package org.jenkinsci.plugins.kubernetes.cli;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriter;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriterFactory;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiKubectlBuildWrapper extends SimpleBuildWrapper {
    final transient public List<KubectlCredential> kubectlCredentials;

    @DataBoundConstructor
    public MultiKubectlBuildWrapper(List<KubectlCredential> kubectlCredentials) {
        if (kubectlCredentials == null || kubectlCredentials.size() == 0) {
            throw new RuntimeException("Credentials list cannot be empty");
        }
        this.kubectlCredentials = kubectlCredentials;
    }

    @Override
    public void setUp(Context context, Run<?, ?> build,
                      FilePath workspace,
                      Launcher launcher,
                      TaskListener listener,
                      EnvVars initialEnvironment) throws IOException, InterruptedException {

        List<String> configFiles = new ArrayList<String>();
        boolean skipUseContext = this.kubectlCredentials.size() >= 2;
        for(KubectlCredential cred: this.kubectlCredentials) {
            KubeConfigWriter kubeConfigWriter = KubeConfigWriterFactory.get(
                    cred.serverUrl,
                    cred.credentialsId,
                    cred.caCertificate,
                    cred.clusterName,
                    cred.contextName,
                    cred.namespace,
                    skipUseContext,
                    workspace,
                    launcher,
                    build);

            configFiles.add(kubeConfigWriter.writeKubeConfig());
        }

        // Remove it when the build is finished
        context.setDisposer(new CleanupDisposer(configFiles));

        // Set environment for the kubectl calls to find the configuration
        String configFileList = String.join(File.pathSeparator, configFiles);
        context.env(KubeConfigWriter.ENV_VARIABLE_NAME, configFileList);
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Configure Kubernetes CLI (kubectl) with multiple credentials";
        }
    }

    public static class CleanupDisposer extends Disposer {

        private static final long serialVersionUID = 1L;
        private List<String> filesToBeRemoved;

        public CleanupDisposer(List<String > files) {
            this.filesToBeRemoved = files;
        }

        @Override
        public void tearDown(Run<?, ?> build,
                             FilePath workspace,
                             Launcher launcher,
                             TaskListener listener) throws IOException, InterruptedException {
            for(String file : filesToBeRemoved) {
                workspace.child(file).delete();
            }
            listener.getLogger().println("kubectl configuration cleaned up");
        }
    }
}
