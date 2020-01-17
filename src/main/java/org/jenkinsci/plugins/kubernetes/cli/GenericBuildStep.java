package org.jenkinsci.plugins.kubernetes.cli;

import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriter;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriterFactory;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenericBuildStep extends AbstractStepExecutionImpl {
    private static final long serialVersionUID = 1L;
    private transient List<KubectlCredential> kubectlCredentials;

    public GenericBuildStep(List<KubectlCredential> credentials, StepContext context) {
        super(context);
        this.kubectlCredentials = credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean start() throws Exception {
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
                    getContext());

            configFiles.add(kubeConfigWriter.writeKubeConfig());
        }

        // Prepare a new environment
        String configFileList = String.join(File.pathSeparator, configFiles);
        EnvironmentExpander envExpander = EnvironmentExpander.merge(
                getContext().get(EnvironmentExpander.class),
                new KubeConfigExpander(configFileList));

        // Execute the commands in the body within this environment
        getContext().newBodyInvoker()
                .withContext(envExpander)
                .withCallback(new Callback(configFiles))
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

    private static final class Callback extends BodyExecutionCallback.TailCall {
        private static final long serialVersionUID = 1L;
        private final List<String> configFiles;

        Callback(List<String> configFiles) {
            this.configFiles = configFiles;
        }

        protected void finished(StepContext context) throws Exception {
            for(String configFile : configFiles) {
                context.get(FilePath.class).child(configFile).delete();
            }
            context.get(TaskListener.class).getLogger().println("kubectl configuration cleaned up");
        }

    }
}
