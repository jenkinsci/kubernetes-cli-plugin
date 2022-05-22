package org.jenkinsci.plugins.kubernetes.cli;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;

import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.jenkinsci.plugins.kubernetes.cli.helpers.TestResourceLoader;
import org.jenkinsci.plugins.kubernetes.cli.helpers.Version;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.FilePath;
import io.jenkins.cli.shaded.org.apache.commons.lang.SystemUtils;

/**
 * @author Max Laverse
 */
@Category(KubectlIntegrationTest.class)
public class KubectlIntegrationTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    protected static final String CREDENTIAL_ID = "test-credentials";
    protected static final String SECONDARY_CREDENTIAL_ID = "cred9999";

    @Before
    public void checkKubectlPresence() {
        assertThat("The '" + kubectlBinaryName() + "' binary could not be found in the PATH", kubectlPresent());
    }

    @Test
    public void testKubeConfigPermissionsRestrictedRead() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").contains("Windows"));

        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCa");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString(
                        "withKubeConfigPipelineConfigPermissionsRestrictedRead.groovy"),
                true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configCopy = r.jenkins.getWorkspaceFor(p).child("configCopy");
        assertTrue(configCopy.exists());
        assertEquals(384, configCopy.mode());
    }

    @Test
    public void testKubeConfigPermissionsDefault() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").contains("Windows"));

        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCa");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString(
                        "withKubeConfigPipelineConfigPermissionsDefault.groovy"),
                true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configCopy = r.jenkins.getWorkspaceFor(p).child("configCopy");
        assertTrue(configCopy.exists());

        // We can expected any specific value here since it's installation-dependent,
        // but we can
        // assume it's different from owner only accessible.
        assertNotEquals(384, configCopy.mode());
    }

    @Test
    public void testSingleKubeConfig() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.usernamePasswordCredential(CREDENTIAL_ID));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCa");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString("withKubeConfigPipelineConfigDump.groovy"), true));
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
        store.addCredentials(Domain.global(),
                DummyCredentials.fileCredential(SECONDARY_CREDENTIAL_ID, "test-cluster2",
                        "test-user2"));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfig");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString("withKubeCredentialsPipelineConfigDump.groovy"), true));
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
                "    server: https://test-cluster\n" +
                "  name: test-cluster\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://test-cluster2\n" +
                "  name: test-cluster2\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: test-cluster\n" +
                "    user: test-user\n" +
                "  name: test-cluster\n" +
                "- context:\n" +
                "    cluster: test-cluster2\n" +
                "    user: test-user2\n" +
                "  name: test-cluster2\n" +
                "current-context: test-cluster\n" +
                "kind: Config\n" +
                "preferences: {}\n" +
                "users:\n" +
                "- name: test-user\n" +
                "  user: {}\n" +
                "- name: test-user2\n" +
                "  user: {}"));
    }

    @Test
    public void testMultiKubeConfigUsernames() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.secretCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(),
                DummyCredentials.fileCredential(SECONDARY_CREDENTIAL_ID, "test-cluster2",
                        "test-user2"));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfigUsernames");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString("withKubeCredentialsPipelineAndUsernames.groovy"),
                true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        FilePath configDump = r.jenkins.getWorkspaceFor(p).child("configDump");
        assertTrue(configDump.exists());
        String configDumpContent = configDump.readToString().trim();

        String expectedToken = "REDACTED";
        if ((new Version("1.19.0")).compareTo(KubectlVersion()) >= 0) {
            expectedToken = "s3cr3t";
        }
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
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://test-cluster2\n" +
                "  name: test-cluster2\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: clus1234\n" +
                "    user: test-credentials\n" +
                "  name: cont1234\n" +
                "- context:\n" +
                "    cluster: clus9999\n" +
                "    user: \"\"\n" +
                "  name: cont9999\n" +
                "- context:\n" +
                "    cluster: test-cluster2\n" +
                "    user: test-user2\n" +
                "  name: test-cluster2\n" +
                "current-context: cont1234\n" +
                "kind: Config\n" +
                "preferences: {}\n" +
                "users:\n" +
                "- name: test-credentials\n" +
                "  user:\n" +
                "    token: " + expectedToken + "\n" +
                "- name: test-user2\n" +
                "  user: {}", configDumpContent);
    }

    @Test
    public void testMultiKubeConfigWithServer() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        store.addCredentials(Domain.global(), DummyCredentials.fileCredential(CREDENTIAL_ID));
        store.addCredentials(Domain.global(),
                DummyCredentials.fileCredential(SECONDARY_CREDENTIAL_ID, "test-cluster2",
                        "test-user2"));

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "multiKubeConfigWithServer");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString("withKubeCredentialsPipelineAndServer.groovy"), true));
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
                "    server: https://test-cluster\n" +
                "  name: test-cluster\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: https://test-cluster2\n" +
                "  name: test-cluster2\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: test-cluster\n" +
                "    user: test-user\n" +
                "  name: test-cluster\n" +
                "- context:\n" +
                "    cluster: cred9999\n" +
                "    user: test-user2\n" +
                "  name: test-cluster2\n" +
                "current-context: test-cluster\n" +
                "kind: Config\n" +
                "preferences: {}\n" +
                "users:\n" +
                "- name: test-user\n" +
                "  user: {}\n" +
                "- name: test-user2\n" +
                "  user: {}"));
    }

    protected boolean kubectlPresent() {
        return executablePaths()
                .map(p -> p.resolve(kubectlBinaryName()))
                .filter(Files::exists)
                .anyMatch(Files::isExecutable);
    }

    protected Stream<Path> executablePaths() {
        return System.getenv().entrySet().stream()
                .filter(map -> map.getKey().equals("PATH") || map.getKey().startsWith("PATH+"))
                .flatMap(map -> Arrays.stream(map.getValue().split(Pattern.quote(File.pathSeparator))))
                .map(Paths::get);
    }

    protected String kubectlBinaryName() {
        return SystemUtils.IS_OS_WINDOWS ? "kubectl.exe" : "kubectl";
    }

    private Version KubectlVersion() {
        String version = System.getenv("KUBECTL_VERSION");
        if (version == null) {
            return new Version("99.99.99");
        }
        if (version.startsWith("v")) {
            version = version.replaceFirst("^v", "");
        }
        return new Version(version);
    }
}
