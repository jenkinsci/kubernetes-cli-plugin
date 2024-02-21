package org.jenkinsci.plugins.kubernetes.cli;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import org.jenkinsci.plugins.kubernetes.credentials.TokenProducer;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public abstract class CredentialsLister {

    // List of supported credentials
    public static final CredentialsMatcher supportedCredentials = CredentialsMatchers.anyOf(
            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
            CredentialsMatchers.instanceOf(TokenProducer.class),
            CredentialsMatchers.instanceOf(StringCredentials.class),
            CredentialsMatchers.instanceOf(StandardCertificateCredentials.class),
            CredentialsMatchers.instanceOf(FileCredentials.class));

    public static ListBoxModel doFillCredentialsIdItems(@Nonnull @AncestorInPath Item item,
            @QueryParameter String serverUrl, @QueryParameter String credentialsId) {
        if (item == null
                ? !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
                : !item.hasPermission(Item.EXTENDED_READ)) {
            return new StandardListBoxModel().includeCurrentValue(credentialsId);
        }
        return new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM2,
                        item,
                        StandardCredentials.class,
                        URIRequirementBuilder.fromUri(serverUrl).build(),
                        CredentialsLister.supportedCredentials);
    }
}
