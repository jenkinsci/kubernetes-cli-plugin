package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import io.fabric8.kubernetes.api.model.Cluster;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.api.model.ConfigFluent;
import io.fabric8.kubernetes.api.model.NamedCluster;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthConfig;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;
import org.jenkinsci.plugins.kubernetes.auth.impl.KubernetesAuthKubeconfig;
import org.jenkinsci.plugins.kubernetes.credentials.Utils;

import javax.annotation.Nonnull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * @author Max Laverse
 */
public class KubeConfigWriter {
    public static final String ENV_VARIABLE_NAME = "KUBECONFIG";

    private static final String DEFAULT_CONTEXTNAME = "k8s";
    private static final String CLUSTERNAME = "k8s";

    private final String serverUrl;
    private final String credentialsId;
    private final String caCertificate;
    private final String clusterName;
    private final String contextName;
    private final String namespace;
    private final boolean skipUseContext;
    private final FilePath workspace;
    private final Launcher launcher;
    private final Run<?, ?> build;

    public KubeConfigWriter(@Nonnull String serverUrl, @Nonnull String credentialsId,
                            String caCertificate, String clusterName, String contextName, String namespace, boolean skipUseContext, FilePath workspace, Launcher launcher, Run<?, ?> build) {
        this.serverUrl = serverUrl;
        this.credentialsId = credentialsId;
        this.caCertificate = caCertificate;
        this.workspace = workspace;
        this.launcher = launcher;
        this.build = build;
        this.clusterName = clusterName;
        this.contextName = contextName;
        this.namespace = namespace;
        this.skipUseContext = skipUseContext;
    }

    private static ConfigBuilder setNamedCluster(ConfigBuilder configBuilder, NamedCluster cluster) {
        return existingOrNewCluster(configBuilder, cluster.getName())
                .withName(cluster.getName())
                .editOrNewClusterLike(cluster.getCluster())
                .endCluster()
                .endCluster();
    }

    private static ConfigBuilder setContextCluster(ConfigBuilder configBuilder, String context, String cluster) {
        return existingOrNewContext(configBuilder, context).editOrNewContext().withCluster(cluster).endContext().endContext();
    }

    private static ConfigBuilder setContextNamespace(ConfigBuilder configBuilder, String context, String namespace) {
        return existingOrNewContext(configBuilder, context).editOrNewContext().withNamespace(namespace).endContext().endContext();
    }

    private static ConfigBuilder setCurrentContext(ConfigBuilder configBuilder, String context) {
        return configBuilder.withNewCurrentContext(context);
    }

    private static ConfigFluent.ContextsNested<ConfigBuilder> existingOrNewContext(ConfigBuilder configBuilder, String context) {
        if (configBuilder.hasMatchingContext(p -> p.getName().equals(context))) {
            return configBuilder.editMatchingContext(p -> p.getName().equals(context));
        } else {
            return configBuilder.addNewContext();
        }
    }

    private static ConfigFluent.ClustersNested<ConfigBuilder> existingOrNewCluster(ConfigBuilder configBuilder, String cluster) {
        if (configBuilder.hasMatchingCluster(p -> p.getName().equals(cluster))) {
            return configBuilder.editMatchingCluster(p -> p.getName().equals(cluster));
        } else {
            return configBuilder.addNewCluster();
        }
    }

    /**
     * Write a configuration file for kubectl to disk.
     *
     * @return path to kubeconfig file
     * @throws IOException          on file operations
     * @throws InterruptedException on file operations
     */
    public String writeKubeConfig() throws IOException, InterruptedException {
        if (!workspace.exists()) {
            launcher.getListener().getLogger().println("creating missing workspace to write kubeconfig");
            workspace.mkdirs();
        }

        // Lookup for the credentials on Jenkins
        final StandardCredentials credentials = CredentialsProvider.findCredentialById(credentialsId, StandardCredentials.class, build, Collections.emptyList());
        if (credentials == null) {
            throw new AbortException("Unable to find credentials with id '" + credentialsId + "'");
        }

        // Convert into Kubernetes credentials
        KubernetesAuth auth = AuthenticationTokens.convert(KubernetesAuth.class, credentials);
        if (auth == null) {
            throw new AbortException("Unsupported credentials type " + credentials.getClass().getName());
        }

        ConfigBuilder configBuilder = getConfigBuilder(credentials.getId(), auth);

        // Write configuration to disk
        FilePath configFile = getTempKubeconfigFilePath();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(configFile.getRemote()), StandardCharsets.UTF_8)) {
            w.write(SerializationUtils.getMapper().writeValueAsString(configBuilder.build()));
        }

        return configFile.getRemote();
    }

    public ConfigBuilder getConfigBuilder(String credentialsId, KubernetesAuth auth) throws IOException, InterruptedException {
        // Build configuration
        ConfigBuilder configBuilder;
        try {
            // Build an initial Kubeconfig builder from the credentials
            KubernetesAuthConfig authConfig = new KubernetesAuthConfig(getServerUrl(), caCertificate, !wasProvided(caCertificate));
            configBuilder = auth.buildConfigBuilder(authConfig, getContextNameOrDefault(), getClusterNameOrDefault(), credentialsId);

            // Set additional values of the Kubeconfig
            if (auth instanceof KubernetesAuthKubeconfig) {
                configBuilder = completeKubeconfigConfigBuilder(configBuilder);
            } else {
                configBuilder = completeConfigBuilder(configBuilder);
            }
        } catch (KubernetesAuthException e) {
            throw new AbortException(e.getMessage());
        }
        return configBuilder;
    }

    private ConfigBuilder completeConfigBuilder(ConfigBuilder configBuilder) {
        if (wasProvided(namespace)) {
            configBuilder = setContextNamespace(configBuilder, getContextNameOrDefault(), namespace);
        }

        if (!skipUseContext) {
            configBuilder = setCurrentContext(configBuilder, getContextNameOrDefault());
        }
        return configBuilder;
    }

    private ConfigBuilder completeKubeconfigConfigBuilder(ConfigBuilder configBuilder) throws IOException, InterruptedException {
        if (wasProvided(getServerUrl())) {
            configBuilder = setNamedCluster(configBuilder, buildNamedCluster());
        }

        String currentContext = configBuilder.getCurrentContext();
        if (wasProvided(serverUrl) || wasProvided(clusterName)) {
            configBuilder = setContextCluster(configBuilder, currentContext, getClusterNameOrDefault());
        }

        if (wasProvided(namespace)) {
            configBuilder = setContextNamespace(configBuilder, currentContext, namespace);
        }

        if (wasProvided(contextName) && !skipUseContext) {
            configBuilder = setCurrentContext(configBuilder, contextName);
        }

        return configBuilder;
    }

    private NamedCluster buildNamedCluster() throws IOException, InterruptedException {
        Cluster cluster = new Cluster();
        cluster.setServer(getServerUrl());
        if (wasProvided(caCertificate)) {
            cluster.setCertificateAuthorityData(Utils.encodeBase64(Utils.wrapCertificate(caCertificate)));
        }
        cluster.setInsecureSkipTlsVerify(!wasProvided(caCertificate));

        NamedCluster namedCluster = new NamedCluster();
        namedCluster.setCluster(cluster);
        namedCluster.setName(getClusterNameOrDefault());
        return namedCluster;
    }

    /**
     * Return whether a non-blank value was provided or not
     *
     * @return true if a value was provided to the plugin.
     */
    private boolean wasProvided(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Returns contextName or its default value
     *
     * @return contextName if provided, else the default value.
     */
    private String getContextNameOrDefault() {
        if (!wasProvided(contextName)) {
            return DEFAULT_CONTEXTNAME;
        }
        return contextName;
    }

    /**
     * Returns clusterName or its default value
     *
     * @return clusterName if provided, else the default value.
     */
    private String getClusterNameOrDefault() {
        if (!wasProvided(clusterName)) {
            return CLUSTERNAME;
        }
        return clusterName;
    }

    /**
     * Returns serverUrl with environment variables interpolated
     *
     * @return serverUrl
     */
    private String getServerUrl() throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(launcher.getListener());
        return env.expand(serverUrl);
    }

    private FilePath getTempKubeconfigFilePath() throws IOException, InterruptedException {
        String tempFolder = workspace.getChannel().call(new ObtainTemporaryFolderCallable());
        FilePath tempPath = new FilePath(workspace.getChannel(), tempFolder);
        if (!tempPath.exists()) {
            launcher.getListener().getLogger().println("creating missing temporary folder to write kube config files");
            tempPath.mkdirs();
        }

        return tempPath.createTempFile("kubernetes-cli-plugin-kube", "config");
    }

    /**
     * Used for obtaining the temporary folder for a node.
     */
    private static class ObtainTemporaryFolderCallable extends MasterToSlaveCallable<String, IOException> {
        private static final String TMPDIR__PROPERTY = "java.io.tmpdir";

        @Override
        public String call() {
            return System.getProperty(TMPDIR__PROPERTY);
        }
    }

}
