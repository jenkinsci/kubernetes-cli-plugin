package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthConfig;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuthException;
import org.jenkinsci.plugins.kubernetes.auth.impl.KubernetesAuthKubeconfig;
import org.jenkinsci.plugins.kubernetes.credentials.Utils;

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
    private final FilePath workspace;
    private final Launcher launcher;
    private final Run<?, ?> build;

    public KubeConfigWriter(@Nonnull String serverUrl, String credentialsId,
            String caCertificate, String clusterName, String contextName, String namespace, FilePath workspace,
            Launcher launcher, Run<?, ?> build) {
        this.serverUrl = serverUrl;
        this.credentialsId = credentialsId;
        this.caCertificate = caCertificate;
        this.workspace = workspace;
        this.launcher = launcher;
        this.build = build;
        this.clusterName = clusterName;
        this.contextName = contextName;
        this.namespace = namespace;
    }

    private static ConfigBuilder setNamedCluster(ConfigBuilder configBuilder, NamedCluster cluster) {
        return existingOrNewCluster(configBuilder, cluster.getName())
                .withName(cluster.getName())
                .editOrNewClusterLike(cluster.getCluster())
                .endCluster()
                .endCluster();
    }

    private static ConfigBuilder setContextCluster(ConfigBuilder configBuilder, String context, String cluster) {
        return existingOrNewContext(configBuilder, context).editOrNewContext().withCluster(cluster).endContext()
                .endContext();
    }

    private static ConfigBuilder setContextNamespace(ConfigBuilder configBuilder, String context, String namespace) {
        return existingOrNewContext(configBuilder, context).editOrNewContext().withNamespace(namespace).endContext()
                .endContext();
    }

    private static ConfigBuilder setCurrentContext(ConfigBuilder configBuilder, String context) {
        return configBuilder.withNewCurrentContext(context);
    }

    private static ConfigFluent.ContextsNested<ConfigBuilder> existingOrNewContext(ConfigBuilder configBuilder,
            String context) {
        if (hasContext(configBuilder, context)) {
            return configBuilder.editMatchingContext(p -> context.equals(p.getName()));
        } else {
            return configBuilder.addNewContext().withName(context);
        }
    }

    private static boolean hasContext(ConfigBuilder configBuilder, String context) {
        return configBuilder.hasMatchingContext(p -> context.equals(p.getName()));
    }

    private static ConfigFluent.ClustersNested<ConfigBuilder> existingOrNewCluster(ConfigBuilder configBuilder,
            String cluster) {
        if (configBuilder.hasMatchingCluster(p -> cluster.equals(p.getName()))) {
            return configBuilder.editMatchingCluster(p -> cluster.equals(p.getName()));
        } else {
            return configBuilder.addNewCluster().withName(cluster);
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
        ConfigBuilder configBuilder;

        if (credentialsId == null || credentialsId.isEmpty()) {
            configBuilder = getConfigBuilderInCluster();
        } else {
            // Lookup for the credentials on Jenkins
            final StandardCredentials credentials = CredentialsProvider.findCredentialById(credentialsId,
                    StandardCredentials.class, build, Collections.emptyList());
            if (credentials == null) {
                throw new AbortException("[kubernetes-cli] unable to find credentials with id '" + credentialsId + "'");
            }

            // Convert into Kubernetes credentials
            KubernetesAuth auth = AuthenticationTokens.convert(KubernetesAuth.class, credentials);
            if (auth == null) {
                throw new AbortException(
                        "[kubernetes-cli] unsupported credentials type " + credentials.getClass().getName());
            }

            configBuilder = getConfigBuilderWithAuth(credentials.getId(), auth);
        }

        // Write configuration to disk
        FilePath configFile = getTempKubeconfigFilePath();
        configFile.write(SerializationUtils.getMapper().writeValueAsString(configBuilder.build()),
                String.valueOf(StandardCharsets.UTF_8));

        return configFile.getRemote();
    }

    // getConfigBuilderInCluster() starts an empty configBuilder
    public ConfigBuilder getConfigBuilderInCluster() throws IOException, InterruptedException {
        ConfigBuilder configBuilder = new io.fabric8.kubernetes.api.model.ConfigBuilder();
        return completeConfigBuilder(configBuilder);
    }

    public ConfigBuilder getConfigBuilderWithAuth(String credentialsId, KubernetesAuth auth)
            throws IOException, InterruptedException {
        // Build configuration
        ConfigBuilder configBuilder;
        try {
            // Build an initial Kubeconfig builder from the credentials
            KubernetesAuthConfig authConfig = new KubernetesAuthConfig(getServerUrl(), caCertificate,
                    !wasProvided(caCertificate));
            configBuilder = auth.buildConfigBuilder(authConfig, getContextNameOrDefault(), getClusterNameOrDefault(),
                    credentialsId);

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

    private ConfigBuilder completeConfigBuilder(ConfigBuilder configBuilder) throws IOException, InterruptedException {
        configBuilder = existingOrNewContext(configBuilder, getContextNameOrDefault()).editOrNewContext().endContext()
                .endContext();
        if (wasProvided(namespace)) {
            configBuilder = setContextNamespace(configBuilder, getContextNameOrDefault(), getNamespace());
        }
        configBuilder = setCurrentContext(configBuilder, getContextNameOrDefault());
        return configBuilder;
    }

    private ConfigBuilder completeKubeconfigConfigBuilder(ConfigBuilder configBuilder)
            throws IOException, InterruptedException {

        String currentContext;

        if (wasProvided(contextName)) {
            currentContext = getContextName();
            if (!hasContext(configBuilder, currentContext)) {
                // There is not much sense to create a new context in a raw kubeconfig file as
                // it would have no
                // configured credentials. Print a warning
                launcher.getListener().getLogger().printf("[kubernetes-cli] context '%s' doesn't exist in kubeconfig",
                        currentContext);
            }
            configBuilder = setCurrentContext(configBuilder, currentContext);
        } else {
            currentContext = configBuilder.getCurrentContext();
        }

        if (wasProvided(serverUrl)) {
            configBuilder = setNamedCluster(configBuilder, buildNamedCluster());
        }

        if (wasProvided(serverUrl) || wasProvided(clusterName)) {
            configBuilder = setContextCluster(configBuilder, currentContext, getClusterNameOrDefault());
        }

        if (wasProvided(namespace)) {
            configBuilder = setContextNamespace(configBuilder, currentContext, getNamespace());
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
     * Returns the namespace after environment variable interpolation.
     *
     * @return namespace.
     */
    private String getNamespace() throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(launcher.getListener());
        return env.expand(namespace);
    }

    /**
     * Returns contextName or its default value
     *
     * @return contextName if provided, else the default value.
     */
    private String getContextNameOrDefault() throws IOException, InterruptedException {
        if (!wasProvided(contextName)) {
            return DEFAULT_CONTEXTNAME;
        }
        return getContextName();
    }

    /**
     * Returns the contextName after environment variable interpolation.
     *
     * @return contextName.
     */
    private String getContextName() throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(launcher.getListener());
        return env.expand(contextName);
    }

    /**
     * Returns clusterName or its default value
     *
     * @return clusterName if provided, else the default value.
     */
    private String getClusterNameOrDefault() throws IOException, InterruptedException {
        if (!wasProvided(clusterName)) {
            return CLUSTERNAME;
        }
        return getClusterName();
    }

    /**
     * Returns the clusterName after environment variable interpolation.
     *
     * @return clusterName.
     */
    private String getClusterName() throws IOException, InterruptedException {
        final EnvVars env = build.getEnvironment(launcher.getListener());
        return env.expand(clusterName);
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
        if (!workspace.exists()) {
            launcher.getListener().getLogger()
                    .println("[kubernetes-cli] creating missing workspace to write temporary kubeconfig");
            workspace.mkdirs();
        }

        return workspace.createTempFile(".kube", "config");
    }
}
