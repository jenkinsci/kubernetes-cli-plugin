package org.jenkinsci.plugins.kubernetes.cli;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriter;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriterFactory;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class KubectlBuildWrapper extends SimpleBuildWrapper {

    @DataBoundSetter
    public String serverUrl;

    @DataBoundSetter
    public String credentialsId;

    @DataBoundSetter
    public String caCertificate;

    @DataBoundSetter
    public String contextName;

    @DataBoundSetter
    public String clusterName;

    @DataBoundSetter
    public String namespace;

    @DataBoundConstructor
    public KubectlBuildWrapper() {
    }

    @Override
    public void setUp(Context context, Run<?, ?> build,
                      FilePath workspace,
                      Launcher launcher,
                      TaskListener listener,
                      EnvVars initialEnvironment) throws IOException, InterruptedException {
        KubeConfigWriter kubeConfigWriter = KubeConfigWriterFactory.get(
                this.serverUrl,
                this.credentialsId,
                this.caCertificate,
                this.clusterName,
                this.contextName,
                this.namespace,
                workspace,
                launcher,
                build);

        // Write the kubeconfig file
        String configFile = kubeConfigWriter.writeKubeConfig();

        // Remove it when the build is finished
        context.setDisposer(new CleanupDisposer(configFile));

        // Set environment for the kubectl calls to find the configuration
        context.env(KubeConfigWriter.ENV_VARIABLE_NAME, configFile);
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Configure Kubernetes CLI (kubectl)";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String serverUrl) {
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            item,
                            StandardCredentials.class,
                            URIRequirementBuilder.fromUri(serverUrl).build(),
                            AuthenticationTokens.matcher(KubernetesAuth.class));
        }
    }

    /**
     * @author Max Laverse
     */
    public static class CleanupDisposer extends Disposer {

        private static final long serialVersionUID = 1L;
        private String fileToBeRemoved;

        public CleanupDisposer(String file) {
            this.fileToBeRemoved = file;
        }

        @Override
        public void tearDown(Run<?, ?> build,
                             FilePath workspace,
                             Launcher launcher,
                             TaskListener listener) throws IOException, InterruptedException {
            workspace.child(fileToBeRemoved).delete();
            listener.getLogger().println("kubectl configuration cleaned up");
        }
    }
}
