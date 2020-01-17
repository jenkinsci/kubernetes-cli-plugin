package org.jenkinsci.plugins.kubernetes.cli;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.FilePath;
import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.jenkinsci.plugins.kubernetes.cli.helpers.TestResourceLoader;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * @author Max Laverse
 */
public class KubectlIntegrationTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    protected static final String CREDENTIAL_ID = "test-credentials";
    protected static final String SECONDARY_CREDENTIAL_ID = "cred9999";
    protected static final String KUBECTL_BINARY = "kubectl";

    protected boolean kubectlPresent() {
        return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
                .map(Paths::get)
                .map(p -> p.resolve(KUBECTL_BINARY))
                .filter(Files::exists)
                .anyMatch(Files::isExecutable);
    }

    @Before
    public void checkKubectlPresence() {
        assumeTrue(kubectlPresent());
    }

    @Test
    public void testSingleKubeConfig() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), DummyCredentials.usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCa");
        p.setDefinition(new CpsFlowDefinition(TestResourceLoader.load("withKubeConfigPipelineConfigDump.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        r.assertLogContains("kubectl configuration cleaned up", b);
        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertThat(configDumpContent, containsString("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:6443\n" +
                "  name: k8s\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: k8s\n" +
                "    user: test-credentials\n" +
                "  name: k8s\n" +
                "current-context: k8s\n" +
                "kind: Config\n" +
                "preferences: {}\n" +
                "users:\n" +
                "- name: test-credentials\n" +
                "  user:\n" +
                "    password: s3cr3t\n" +
                "    username: bob"));
    }

    @Test
    public void testMultiKubeConfig() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential(SECONDARY_CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfig");
        p.setDefinition(new CpsFlowDefinition(TestResourceLoader.load("withKubeCredentialsPipelineConfigDump.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertThat(configDumpContent, containsString("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"\"\n" +
                "  name: cred9999\n" +
                "- cluster:\n" +
                "    server: \"\"\n" +
                "  name: test-sample\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: cred9999\n" +
                "    user: \"\"\n" +
                "  name: cred9999\n" +
                "- context:\n" +
                "    cluster: \"\"\n" +
                "    user: \"\"\n" +
                "  name: minikube\n" +
                "- context:\n" +
                "    cluster: test-sample\n" +
                "    user: \"\"\n" +
                "  name: test-sample\n" +
                "current-context: test-sample\n"));
    }

    @Test
    public void testMultiKubeConfigUsernames() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.secretCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(), DummyCredentials.secretCredential(SECONDARY_CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfigUsernames");
        p.setDefinition(new CpsFlowDefinition(TestResourceLoader.load("withKubeCredentialsPipelineAndUsernames.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertEquals("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:1234\n" +
                "  name: clus1234\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:9999\n" +
                "  name: clus9999\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: clus1234\n" +
                "    user: test-credentials\n" +
                "  name: cont1234\n" +
                "- context:\n" +
                "    cluster: clus9999\n" +
                "    user: cred9999\n" +
                "  name: cont9999\n" +
                "current-context: \"\"\n" +
                "kind: Config\n" +
                "preferences: {}\n" +
                "users:\n" +
                "- name: cred9999\n" +
                "  user:\n" +
                "    token: s3cr3t\n" +
                "- name: test-credentials\n" +
                "  user:\n" +
                "    token: s3cr3t", configDumpContent);
    }

    @Test
    public void testMultiKubeConfigWithServer() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential(SECONDARY_CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfigWithServer");
        p.setDefinition(new CpsFlowDefinition(TestResourceLoader.load("withKubeCredentialsPipelineAndServer.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        assertThat(configDumpContent, containsString("apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:9999\n" +
                "  name: cred9999\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://localhost:1234\n" +
                "  name: test-cluster\n" +
                "- cluster:\n" +
                "    server: \"\"\n" +
                "  name: test-sample\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: cred9999\n" +
                "    user: \"\"\n" +
                "  name: cred9999\n" +
                "- context:\n" +
                "    cluster: \"\"\n" +
                "    user: \"\"\n" +
                "  name: minikube\n" +
                "- context:\n" +
                "    cluster: test-cluster\n" +
                "    user: \"\"\n" +
                "  name: test-sample\n" +
                "current-context: test-sample\n"));
    }
}
