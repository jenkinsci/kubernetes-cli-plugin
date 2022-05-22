package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.impl.KubernetesAuthKubeconfig;
import org.jenkinsci.plugins.kubernetes.auth.impl.KubernetesAuthUsernamePassword;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

public class KubeConfigWriterBuilderTest {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    FilePath workspace;
    Launcher mockLauncher;
    AbstractBuild build;

    private static String dumpBuilder(ConfigBuilder configBuilder) throws JsonProcessingException {
        return SerializationUtils.getMapper().writeValueAsString(configBuilder.build());
    }

    private static KubernetesAuthKubeconfig dummyKubeConfigAuth() {
        return new KubernetesAuthKubeconfig(
                "---\n" +
                        "clusters:\n" +
                        "- name: \"existing-cluster\"\n" +
                        "  cluster:\n" +
                        "    server: https://existing-cluster\n" +
                        "contexts:\n" +
                        "- context:\n" +
                        "    cluster: \"existing-cluster\"\n" +
                        "    namespace: \"existing-namespace\"\n" +
                        "  name: \"existing-context\"\n" +
                        "- context:\n" +
                        "    cluster: \"existing-cluster\"\n" +
                        "    namespace: \"unused-namespace\"\n" +
                        "  name: \"unused-context\"\n" +
                        "current-context: \"existing-context\"\n" +
                        "users:\n" +
                        "- name: \"existing-credential\"\n" +
                        "  user:\n" +
                        "    password: \"existing-password\"\n" +
                        "    username: \"existing-user\"\n");
    }

    @Before
    public void init() throws IOException, InterruptedException {
        workspace = new FilePath(tempFolder.newFolder("workspace"));

        mockLauncher = Mockito.mock(Launcher.class);
        VirtualChannel mockChannel = Mockito.mock(VirtualChannel.class);
        when(mockLauncher.getChannel()).thenReturn(mockChannel);

        TaskListener mockListener = Mockito.mock(TaskListener.class);
        when(mockListener.getLogger()).thenReturn(new PrintStream(output, true, "UTF-8"));

        when(mockLauncher.getListener()).thenReturn(mockListener);

        build = Mockito.mock(AbstractBuild.class);
        EnvVars env = new EnvVars();
        when(build.getEnvironment(any())).thenReturn(env);
    }

    @Test
    public void inClusterServiceAccountToken() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "",
                "",
                "",
                "",
                "",
                "",
                workspace, mockLauncher, build);

        ConfigBuilder configBuilder = configWriter.getConfigBuilderInCluster();
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters: []\n" +
                "contexts:\n" +
                "- context: {}\n" +
                "  name: \"k8s\"\n" +
                "current-context: \"k8s\"\n" +
                "users: []\n", configDumpContent);
    }

    @Test
    public void inClusterServiceAccountTokenWithNamespace() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "",
                "",
                "",
                "",
                "",
                "test-namespace",
                workspace, mockLauncher, build);

        ConfigBuilder configBuilder = configWriter.getConfigBuilderInCluster();
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters: []\n" +
                "contexts:\n" +
                "- context:\n" +
                "    namespace: \"test-namespace\"\n" +
                "  name: \"k8s\"\n" +
                "current-context: \"k8s\"\n" +
                "users: []\n", configDumpContent);
    }

    @Test
    public void inClusterServiceAccountTokeninClusterServiceAccountTokenWithContextAndNamespace() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "",
                "",
                "",
                "",
                "test-context",
                "test-namespace",
                workspace, mockLauncher, build);

        ConfigBuilder configBuilder = configWriter.getConfigBuilderInCluster();
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters: []\n" +
                "contexts:\n" +
                "- context:\n" +
                "    namespace: \"test-namespace\"\n" +
                "  name: \"test-context\"\n" +
                "current-context: \"test-context\"\n" +
                "users: []\n", configDumpContent);
    }

    @Test
    public void basicConfigMinimum() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "",
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    user: \"test-credential\"\n" +
                "  name: \"k8s\"\n" +
                "current-context: \"k8s\"\n" +
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithCa() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "test-certificate",
                "",
                "",
                "",
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    certificate-authority-data: \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCnRlc3QtY2VydGlmaWNhdGUKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\"\n"
                +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    user: \"test-credential\"\n" +
                "  name: \"k8s\"\n" +
                "current-context: \"k8s\"\n" +
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithCluster() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "test-cluster",
                "",
                "",
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"test-cluster\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"test-cluster\"\n" +
                "    user: \"test-credential\"\n" +
                "  name: \"k8s\"\n" +
                "current-context: \"k8s\"\n" +
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithNamespace() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "test-namespace",
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"test-namespace\"\n" +
                "    user: \"test-credential\"\n" +
                "  name: \"k8s\"\n" +
                "current-context: \"k8s\"\n" +
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithContext() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "test-context",
                "",
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    user: \"test-credential\"\n" +
                "  name: \"test-context\"\n" +
                "current-context: \"test-context\"\n" +
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigMinimum() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "",
                "test-credential",
                "",
                "",
                "",
                "",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        // asserts that:
        // * kubeconfig is simply imported
        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"unused-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithCaCertificate() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "test-certificate",
                "",
                "",
                "",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        // asserts that:
        // * kubeconfig is imported
        // * a new cluster is created with the CA and serverURL
        // * the cluster is used by the existing context
        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    certificate-authority-data: \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCnRlc3QtY2VydGlmaWNhdGUKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\"\n"
                +
                "    insecure-skip-tls-verify: false\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"unused-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithNamespace() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "new-namespace",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        // asserts that:
        // * kubeconfig is imported
        // * a new cluster is created with the serverURL
        // * the cluster is used by the existing context
        // * the namespace is set for the existing context
        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"new-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"unused-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithClusterName() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "new-cluster",
                "",
                "",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        // asserts that:
        // * kubeconfig is imported
        // * a new cluster is created with the serverURL and the clusterName
        // * the cluster is used by the existing context
        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"new-cluster\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"new-cluster\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"unused-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithContextSwitch() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "unused-context",
                "new-namespace",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();
        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        // asserts that:
        // * kubeconfig is imported
        // * context is switched
        // * a new cluster is created with the serverURL
        // * the cluster is used by the context we switched to
        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"new-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "current-context: \"unused-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithSwitchToNonExistentContext() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "non-existent-context",
                "new-namespace",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();
        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        // asserts that:
        // * kubeconfig is imported
        // * non-existent context is created
        // * a new cluster is created with the serverURL
        // * the cluster is used by the context we created
        // * the namespace is set in the context we created
        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"unused-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"new-namespace\"\n" +
                "  name: \"non-existent-context\"\n" +
                "current-context: \"non-existent-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
        assertEquals("[kubernetes-cli] context 'non-existent-context' doesn't exist in kubeconfig",
                output.toString());
    }

    @Test
    public void kubeConfigWithExistingContext() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "existing-context",
                "",
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();
        ConfigBuilder configBuilder = configWriter.getConfigBuilderWithAuth("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "- context:\n" +
                "    cluster: \"existing-cluster\"\n" +
                "    namespace: \"unused-namespace\"\n" +
                "  name: \"unused-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }
}
