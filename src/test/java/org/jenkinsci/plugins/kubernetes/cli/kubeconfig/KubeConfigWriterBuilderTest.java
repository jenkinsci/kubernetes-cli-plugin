package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.impl.KubernetesAuthKubeconfig;
import org.jenkinsci.plugins.kubernetes.auth.impl.KubernetesAuthUsernamePassword;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class KubeConfigWriterBuilderTest {
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

        build = Mockito.mock(AbstractBuild.class);
        EnvVars env = new EnvVars();
        when(build.getEnvironment(any())).thenReturn(env);
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "    as-user-extra: {}\n" +
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    certificate-authority-data: \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCnRlc3QtY2VydGlmaWNhdGUKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\"\n" +
                "    insecure-skip-tls-verify: false\n" +
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
                "    as-user-extra: {}\n" +
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "    as-user-extra: {}\n" +
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "    as-user-extra: {}\n" +
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "    as-user-extra: {}\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithSkipUseContext() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "test-context",
                "",
                true,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthUsernamePassword("test-user", "test-password");

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    password: \"test-password\"\n" +
                "    username: \"test-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigMinimum() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "",
                false,
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    certificate-authority-data: \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCnRlc3QtY2VydGlmaWNhdGUKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\"\n" +
                "    insecure-skip-tls-verify: false\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"k8s\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"k8s\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
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
                "test-namespace",
                false,
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "    namespace: \"test-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithClusterName() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "test-cluster",
                "",
                "",
                false,
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();

        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
        String configDumpContent = dumpBuilder(configBuilder);

        assertEquals("---\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    server: \"https://existing-cluster\"\n" +
                "  name: \"existing-cluster\"\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: \"https://localhost:6443\"\n" +
                "  name: \"test-cluster\"\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: \"test-cluster\"\n" +
                "    namespace: \"existing-namespace\"\n" +
                "  name: \"existing-context\"\n" +
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }

    @Test
    public void kubeConfigWithNewContext() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "test-context",
                "",
                false,
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();
        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "current-context: \"test-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
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
                false,
                workspace, mockLauncher, build);

        KubernetesAuthKubeconfig auth = dummyKubeConfigAuth();
        ConfigBuilder configBuilder = configWriter.getConfigBuilder("test-credential", auth);
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
                "current-context: \"existing-context\"\n" +
                "users:\n" +
                "- name: \"existing-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    password: \"existing-password\"\n" +
                "    username: \"existing-user\"\n", configDumpContent);
    }
}
