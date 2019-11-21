package org.jenkinsci.plugins.kubernetes.cli;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriter;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriterFactory;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Max Laverse
 */
public class KubectlBuildStep extends Step {

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
    public KubectlBuildStep() {
    }

    @Override
    public final StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
    }

    public static class ExecutionImpl extends AbstractStepExecutionImpl {

        private static final long serialVersionUID = 1L;
        private transient KubectlBuildStep step;

        public ExecutionImpl(KubectlBuildStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean start() throws Exception {
            KubeConfigWriter kubeConfigWriter = KubeConfigWriterFactory.get(
                    step.serverUrl,
                    step.credentialsId,
                    step.caCertificate,
                    step.clusterName,
                    step.contextName,
                    step.namespace,
                    getContext());

            // Write config
            String configFile = kubeConfigWriter.writeKubeConfig();

            // Prepare a new environment
            EnvironmentExpander envExpander = EnvironmentExpander.merge(
                    getContext().get(EnvironmentExpander.class),
                    new KubeConfigExpander(configFile));

            // Execute the commands in the body within this environment
            getContext().newBodyInvoker()
                    .withContext(envExpander)
                    .withCallback(new Callback(configFile))
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
            return "Configure Kubernetes CLI (kubectl)";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFunctionName() {
            return "withKubeConfig";
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

        public ListBoxModel doFillCredentialsIdItems(@Nonnull @AncestorInPath Item item, @QueryParameter String serverUrl) {
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

    static final class KubeConfigExpander extends EnvironmentExpander {

        private final Map<String, String> envOverride;

        KubeConfigExpander(String kubeConfigPath) {
            this.envOverride = new HashMap<>();
            this.envOverride.put(KubeConfigWriter.ENV_VARIABLE_NAME, kubeConfigPath);
        }

        @Override
        public void expand(EnvVars env) throws IOException, InterruptedException {
            env.overrideAll(envOverride);
        }
    }
}
