package org.jenkinsci.plugins.kubernetes.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.domains.Domain;

import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.jenkinsci.plugins.kubernetes.cli.helpers.TestResourceLoader;
import org.jenkinsci.plugins.kubernetes.cli.helpers.Version;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.kubernetes.cli.helpers.JenkinsRuleExtension;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Fingerprint;
import hudson.FilePath;
import io.jenkins.cli.shaded.org.apache.commons.lang.SystemUtils;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.NamedCluster;
import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.api.model.NamedAuthInfo;

/**
 * @author Max Laverse
 */
@Tag("org.jenkinsci.plugins.kubernetes.cli.KubectlIntegrationTest")
@ExtendWith(JenkinsRuleExtension.class)
public class KubectlIntegrationTest {
    public final JenkinsRule r = new JenkinsRule();

    protected static final String CREDENTIAL_ID = "test-credentials";
    protected static final String SECONDARY_CREDENTIAL_ID = "cred9999";

    @BeforeEach
    public void checkKubectlPresence() {
        assertThat("The '" + kubectlBinaryName() + "' binary could not be found in the PATH", kubectlPresent());
    }

    @Test
    public void testKubeConfigPermissionsRestrictedRead() throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name").contains("Windows"));

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
        Assumptions.assumeFalse(System.getProperty("os.name").contains("Windows"));

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

        Config config = Serialization.unmarshal(configDumpContent, Config.class);
        assertNotNull(config);
        assertEquals("v1", config.getApiVersion());
        assertEquals("Config", config.getKind());
        assertEquals("k8s", config.getCurrentContext());

        NamedCluster cluster = config.getClusters().stream()
                .filter(c -> "k8s".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster);
        assertEquals("https://localhost:6443", cluster.getCluster().getServer());
        assertTrue(cluster.getCluster().getInsecureSkipTlsVerify());

        NamedContext context = config.getContexts().stream()
                .filter(c -> "k8s".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context);
        assertEquals("k8s", context.getContext().getCluster());
        assertEquals("test-credentials", context.getContext().getUser());

        NamedAuthInfo user = config.getUsers().stream()
                .filter(u -> "test-credentials".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user);
        assertEquals("bob", user.getUser().getUsername());
        assertEquals("s3cr3t", user.getUser().getPassword());
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

        Config config = Serialization.unmarshal(configDumpContent, Config.class);
        assertNotNull(config);
        assertEquals("v1", config.getApiVersion());
        assertEquals("Config", config.getKind());
        assertEquals("test-cluster", config.getCurrentContext());

        NamedCluster cluster1 = config.getClusters().stream()
                .filter(c -> "test-cluster".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster1);
        assertEquals("https://test-cluster", cluster1.getCluster().getServer());
        assertTrue(cluster1.getCluster().getInsecureSkipTlsVerify());

        NamedCluster cluster2 = config.getClusters().stream()
                .filter(c -> "test-cluster2".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster2);
        assertEquals("https://test-cluster2", cluster2.getCluster().getServer());
        assertTrue(cluster2.getCluster().getInsecureSkipTlsVerify());

        NamedContext context1 = config.getContexts().stream()
                .filter(c -> "test-cluster".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context1);
        assertEquals("test-cluster", context1.getContext().getCluster());
        assertEquals("test-user", context1.getContext().getUser());

        NamedContext context2 = config.getContexts().stream()
                .filter(c -> "test-cluster2".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context2);
        assertEquals("test-cluster2", context2.getContext().getCluster());
        assertEquals("test-user2", context2.getContext().getUser());

        NamedAuthInfo user1 = config.getUsers().stream()
                .filter(u -> "test-user".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user1);

        NamedAuthInfo user2 = config.getUsers().stream()
                .filter(u -> "test-user2".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user2);
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

        Config config = Serialization.unmarshal(configDumpContent, Config.class);
        assertNotNull(config);
        assertEquals("v1", config.getApiVersion());
        assertEquals("Config", config.getKind());
        assertEquals("cont1234", config.getCurrentContext());

        NamedCluster cluster1 = config.getClusters().stream()
                .filter(c -> "clus1234".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster1);
        assertEquals("https://localhost:1234", cluster1.getCluster().getServer());
        assertTrue(cluster1.getCluster().getInsecureSkipTlsVerify());

        NamedCluster cluster2 = config.getClusters().stream()
                .filter(c -> "clus9999".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster2);
        assertEquals("https://localhost:9999", cluster2.getCluster().getServer());
        assertTrue(cluster2.getCluster().getInsecureSkipTlsVerify());

        NamedCluster cluster3 = config.getClusters().stream()
                .filter(c -> "test-cluster2".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster3);
        assertEquals("https://test-cluster2", cluster3.getCluster().getServer());
        assertTrue(cluster3.getCluster().getInsecureSkipTlsVerify());

        NamedContext context1 = config.getContexts().stream()
                .filter(c -> "cont1234".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context1);
        assertEquals("clus1234", context1.getContext().getCluster());
        assertEquals("test-credentials", context1.getContext().getUser());

        NamedContext context2 = config.getContexts().stream()
                .filter(c -> "cont9999".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context2);
        assertEquals("clus9999", context2.getContext().getCluster());
        assertEquals("", context2.getContext().getUser());

        NamedContext context3 = config.getContexts().stream()
                .filter(c -> "test-cluster2".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context3);
        assertEquals("test-cluster2", context3.getContext().getCluster());
        assertEquals("test-user2", context3.getContext().getUser());

        NamedAuthInfo user1 = config.getUsers().stream()
                .filter(u -> "test-credentials".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user1);
        String expectedToken = "REDACTED";
        if ((new Version("1.19.0")).compareTo(KubectlVersion()) >= 0) {
            expectedToken = "s3cr3t";
        }
        assertEquals(expectedToken, user1.getUser().getToken());

        NamedAuthInfo user2 = config.getUsers().stream()
                .filter(u -> "test-user2".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user2);
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

        Config config = Serialization.unmarshal(configDumpContent, Config.class);
        assertNotNull(config);
        assertEquals("v1", config.getApiVersion());
        assertEquals("Config", config.getKind());
        assertEquals("test-cluster", config.getCurrentContext());

        NamedCluster cluster1 = config.getClusters().stream()
                .filter(c -> "cred9999".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster1);
        assertEquals("https://localhost:9999", cluster1.getCluster().getServer());
        assertTrue(cluster1.getCluster().getInsecureSkipTlsVerify());

        NamedCluster cluster2 = config.getClusters().stream()
                .filter(c -> "test-cluster".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster2);
        assertEquals("https://test-cluster", cluster2.getCluster().getServer());
        assertTrue(cluster2.getCluster().getInsecureSkipTlsVerify());

        NamedCluster cluster3 = config.getClusters().stream()
                .filter(c -> "test-cluster2".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(cluster3);
        assertEquals("https://test-cluster2", cluster3.getCluster().getServer());
        assertTrue(cluster3.getCluster().getInsecureSkipTlsVerify());

        NamedContext context1 = config.getContexts().stream()
                .filter(c -> "test-cluster".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context1);
        assertEquals("test-cluster", context1.getContext().getCluster());
        assertEquals("test-user", context1.getContext().getUser());

        NamedContext context2 = config.getContexts().stream()
                .filter(c -> "test-cluster2".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(context2);
        assertEquals("cred9999", context2.getContext().getCluster());
        assertEquals("test-user2", context2.getContext().getUser());

        NamedAuthInfo user1 = config.getUsers().stream()
                .filter(u -> "test-user".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user1);

        NamedAuthInfo user2 = config.getUsers().stream()
                .filter(u -> "test-user2".equals(u.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(user2);
    }

    @Test
    public void testTracking() throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(r.jenkins).iterator().next();
        Credentials credentials = DummyCredentials.fileCredential(CREDENTIAL_ID);
        store.addCredentials(Domain.global(), credentials);

        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "testBasicWithCa");
        p.setDefinition(new CpsFlowDefinition(
                TestResourceLoader.loadAsString("withKubeConfigPipelineConfigDump.groovy"), true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        assertNotNull(b);
        r.assertBuildStatusSuccess(r.waitForCompletion(b));

        Fingerprint something = CredentialsProvider.getFingerprintOf(credentials);
        assertNotNull(something);
        assertNotNull(something.getUsages().get("testBasicWithCa"));
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
