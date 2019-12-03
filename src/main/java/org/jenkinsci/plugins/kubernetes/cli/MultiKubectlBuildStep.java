package org.jenkinsci.plugins.kubernetes.cli;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriter;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriterFactory;
import org.jenkinsci.plugins.kubernetes.credentials.TokenProducer;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class MultiKubectlBuildStep extends Step {
    final public List<KubectlCredential> kubectlCredentials;

    @DataBoundConstructor
    public MultiKubectlBuildStep(List<KubectlCredential> credentials) {
        if (credentials == null || credentials.size() == 0) {
            throw new RuntimeException("Credentials list should cannot be empty");
        }
        this.kubectlCredentials = credentials;
    }

    @Override
    public final StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
    }

    public static class ExecutionImpl extends AbstractStepExecutionImpl {

        private static final long serialVersionUID = 1L;
        private transient MultiKubectlBuildStep step;

        public ExecutionImpl(MultiKubectlBuildStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean start() throws Exception {
            List<String> configFiles = new ArrayList<String>();

            for(KubectlCredential cred: step.kubectlCredentials) {
                KubeConfigWriter kubeConfigWriter = KubeConfigWriterFactory.get(
                        cred.serverUrl,
                        cred.credentialsId,
                        cred.caCertificate,
                        cred.clusterName,
                        cred.contextName,
                        cred.namespace,
                        getContext());

                configFiles.add(kubeConfigWriter.writeKubeConfig());
            }

            String configFileList = String.join(File.pathSeparator, configFiles);

            // Prepare a new environment
            EnvironmentExpander envExpander = EnvironmentExpander.merge(
                    getContext().get(EnvironmentExpander.class),
                    new KubeConfigExpander(configFileList));

            // Execute the commands in the body within this environment
            getContext().newBodyInvoker()
                    .withContext(envExpander)
                    .withCallback(new Callback(configFileList))
                    .start();

            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            getContext().onFailure(cause);
        }
    }

    private static final class Callback extends BodyExecutionCallback.TailCall {
        private static final long serialVersionUID = 1L;
        private final String configFile;

        Callback(String configFile) {
            this.configFile = configFile;
        }

        protected void finished(StepContext context) throws Exception {
            context.get(FilePath.class).child(configFile).delete();
            context.get(TaskListener.class).getLogger().println("kubectl configuration cleaned up");
        }

    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Configure Kubernetes CLI (kubectl) with multiple credentials";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFunctionName() {
            return "withMultiKubeConfigs";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>();
        }

    }
}
