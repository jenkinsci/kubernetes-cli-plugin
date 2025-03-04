package org.jenkinsci.plugins.kubernetes.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildWrapper;

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

    @DataBoundSetter
    public Boolean restrictKubeConfigAccess;

    @DataBoundConstructor
    public KubectlBuildWrapper() {
    }

    @Override
    public void setUp(Context context, Run<?, ?> build,
            FilePath workspace,
            Launcher launcher,
            TaskListener listener,
            EnvVars initialEnvironment) throws IOException, InterruptedException {

        KubectlCredential cred = new KubectlCredential();
        cred.serverUrl = this.serverUrl;
        cred.credentialsId = this.credentialsId;
        cred.caCertificate = this.caCertificate;
        cred.contextName = this.contextName;
        cred.clusterName = this.clusterName;
        cred.namespace = this.namespace;

        List<KubectlCredential> list = new ArrayList<KubectlCredential>();
        list.add(cred);

        MultiKubectlBuildWrapper bw = new MultiKubectlBuildWrapper(list, restrictKubeConfigAccess);
        bw.setUp(context, build, workspace, launcher, listener, initialEnvironment);
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Configure Kubernetes CLI (kubectl) (deprecated, use the multi credentials one instead)";
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@NonNull @AncestorInPath Item item,
                @QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            return CredentialsLister.doFillCredentialsIdItems(item, serverUrl, credentialsId);
        }
    }
}
