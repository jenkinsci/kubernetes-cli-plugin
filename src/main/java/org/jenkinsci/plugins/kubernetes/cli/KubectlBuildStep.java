package org.jenkinsci.plugins.kubernetes.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.util.ListBoxModel;

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

    @DataBoundSetter
    public Boolean restrictKubeConfigAccess;

    @DataBoundConstructor
    public KubectlBuildStep() {
    }

    @Override
    public final StepExecution start(StepContext context) throws Exception {
        KubectlCredential cred = new KubectlCredential();
        cred.serverUrl = this.serverUrl;
        cred.credentialsId = this.credentialsId;
        cred.caCertificate = this.caCertificate;
        cred.contextName = this.contextName;
        cred.clusterName = this.clusterName;
        cred.namespace = this.namespace;

        List<KubectlCredential> list = new ArrayList<KubectlCredential>();
        list.add(cred);

        return new GenericBuildStep(list, restrictKubeConfigAccess, context);
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

        public ListBoxModel doFillCredentialsIdItems(@NonNull @AncestorInPath Item item,
                @QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            return CredentialsLister.doFillCredentialsIdItems(item, serverUrl, credentialsId);
        }
    }

}
