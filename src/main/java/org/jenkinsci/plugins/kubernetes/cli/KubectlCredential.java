package org.jenkinsci.plugins.kubernetes.cli;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

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

        public ListBoxModel doFillCredentialsIdItems(@Nonnull @AncestorInPath Item item, @QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            return CredentialsLister.doFillCredentialsIdItems(item, serverUrl, credentialsId);
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId) throws IOException, ServletException {
            if (Strings.isNullOrEmpty(credentialsId)) {
                return FormValidation.error("The credentialId cannot be empty");
            }
            return FormValidation.ok();
        }
    }
}
