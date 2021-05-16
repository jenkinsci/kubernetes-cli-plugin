package org.jenkinsci.plugins.kubernetes.cli;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.Result;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.jenkinsci.plugins.kubernetes.cli.helpers.TestResourceLoader;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Max Laverse
 */
public class KubectlCredentialTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testListedCredentials() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.usernamePasswordCredential("1"));
        store.addCredentials(Domain.global(), DummyCredentials.secretCredential("2"));
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential("3"));
        store.addCredentials(Domain.global(), DummyCredentials.certificateCredential("4"));
        store.addCredentials(Domain.global(), DummyCredentials.tokenCredential("5"));

        KubectlCredential.DescriptorImpl d = new KubectlCredential.DescriptorImpl();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testUsernamePasswordWithSpace");

        ListBoxModel s = d.doFillCredentialsIdItems(p.asItem(), "","1");

        assertEquals(6, s.size());
    }
}
