package org.jenkinsci.plugins.kubernetes.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;

import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.jenkinsci.plugins.kubernetes.cli.helpers.TestResourceLoader;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.kubernetes.cli.helpers.JenkinsRuleExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Result;
import hudson.util.ListBoxModel;

/**
 * @author Max Laverse
 */
@ExtendWith(JenkinsRuleExtension.class)
public class KubectlBuildStepTest {
    public final JenkinsRule r = new JenkinsRule();

    @Test
    public void testListedCredentials() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.usernamePasswordCredential("1"));
        store.addCredentials(Domain.global(), DummyCredentials.secretCredential("2"));
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential("3"));
        store.addCredentials(Domain.global(), DummyCredentials.certificateCredential("4"));
        store.addCredentials(Domain.global(), DummyCredentials.tokenCredential("5"));

        KubectlBuildStep.DescriptorImpl d = new KubectlBuildStep.DescriptorImpl();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testUsernamePasswordWithSpace");

        ListBoxModel s = d.doFillCredentialsIdItems(p.asItem(), "", "1");

        assertEquals(6, s.size());
    }

    @Test
    public void testScopedCredentials() throws Exception {
        Folder folder = new Folder(r.jenkins.getItemGroup(), "test-folder");
        CredentialsProvider.lookupStores(folder).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredential("test-credentials"));

        WorkflowJob p = folder.createProject(WorkflowJob.class, "testScopedCredentials");
        p.setDefinition(
                new CpsFlowDefinition(TestResourceLoader
                        .loadAsString("withKubeConfigPipelineEchoPath.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        assertNotNull(b);
        assertBuildStatus(b, Result.SUCCESS);
    }

    @Test
    public void testMissingScopedCredentials() throws Exception {
        Folder folder = new Folder(r.jenkins.getItemGroup(), "test-folder");
        CredentialsProvider.lookupStores(folder).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredential("test-credentials"));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testMissingScopedCredentials");
        p.setDefinition(
                new CpsFlowDefinition(TestResourceLoader
                        .loadAsString("withKubeConfigPipelineEchoPath.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        assertNotNull(b);
        assertBuildStatus(b, Result.FAILURE);
        r.assertLogContains("ERROR: [kubernetes-cli] unable to find credentials with id 'test-credentials'", b);
    }

    @Test
    public void testKubeConfigDisposedAlsoOnFailure() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredentialWithSpace("test-credentials"));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testCleanupOnFailure");
        p.setDefinition(
                new CpsFlowDefinition(
                        TestResourceLoader.loadAsString("withKubeConfigPipelineFailing.groovy"),
                        true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        assertNotNull(b);
        assertBuildStatus(b, Result.FAILURE);
        r.assertLogContains("[kubernetes-cli] kubectl configuration cleaned up", b);
    }

    @Test
    public void testCredentialNotProvided() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testWithEmptyCredentials");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString("withKubeConfigPipelineMissingCredentials.groovy"),
                true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        assertNotNull(b);
        assertBuildStatus(b, Result.SUCCESS);
    }

    @Test
    public void testUnsupportedCredential() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.unsupportedCredential("test-credentials"));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testWithUnsupportedCredentials");
        p.setDefinition(
                new CpsFlowDefinition(TestResourceLoader
                        .loadAsString("withKubeConfigPipelineEchoPath.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        assertNotNull(b);
        assertBuildStatus(b, Result.FAILURE);
        r.assertLogContains(
                "ERROR: [kubernetes-cli] unsupported credentials type org.jenkinsci.plugins.kubernetes.cli.helpers.UnsupportedCredential",
                b);
    }

    @Test
    public void testEnvVariableFormat() throws Exception {
        Folder folder = new Folder(r.jenkins.getItemGroup(), "test-folder");
        CredentialsProvider.lookupStores(folder).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredential("test-credentials"));

        WorkflowJob p = folder.createProject(WorkflowJob.class, "testScopedCredentials");
        p.setDefinition(
                new CpsFlowDefinition(TestResourceLoader
                        .loadAsString("withKubeConfigPipelineEchoPath.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();

        assertNotNull(b);
        assertBuildStatus(b, Result.SUCCESS);
        String regExp = "Using temporary file '(.+).kube(.+)config'";
        Pattern kubeConfigPathRegexp = Pattern.compile(regExp);
        assertTrue(kubeConfigPathRegexp.matcher(JenkinsRule.getLog(b)).find(),
                "No line in the logs matched the regular expression '" + regExp + "': "
                        + JenkinsRule.getLog(b));
    }

    private void assertBuildStatus(WorkflowRun b, Result result) throws Exception {
        r.assertBuildStatus(result, r.waitForCompletion(b));

        // Because else Jenkins would not always show all the logs
        Thread.sleep(1000);
    }
}
