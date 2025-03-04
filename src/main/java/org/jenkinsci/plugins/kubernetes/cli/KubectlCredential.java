package org.jenkinsci.plugins.kubernetes.cli;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;

/**
 * Necessary information for configuring a single registry
 */
public class KubectlCredential extends AbstractDescribableImpl<KubectlCredential> {

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
    public KubectlCredential() {
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KubectlCredential> {
        @Override
        public String getDisplayName() {
            return "";
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@NonNull @AncestorInPath Item item,
                @QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            return CredentialsLister.doFillCredentialsIdItems(item, serverUrl, credentialsId);
        }
    }
}
