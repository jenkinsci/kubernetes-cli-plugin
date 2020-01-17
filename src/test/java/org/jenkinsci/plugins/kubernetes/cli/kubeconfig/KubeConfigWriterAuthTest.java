package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;
import hudson.util.SecretRule;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import jenkins.security.ConfidentialStoreRule;
import org.jenkinsci.plugins.kubernetes.auth.KubernetesAuth;
import org.jenkinsci.plugins.kubernetes.auth.impl.*;
import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyTokenCredentialImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class KubeConfigWriterAuthTest {

    /*
    The SecretRule and ConfidentialStoreRule are hacks to avoid
    having to use a JenkinsRule in order to read secure credentials,
    as it is way slower to load
     */
    @Rule
    public SecretRule secretRule = new SecretRule();

    @Rule
    public ConfidentialStoreRule confidentialStoreRule = new ConfidentialStoreRule();

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

    private static KeyStore loadKeyStore(InputStream inputStream, char[] password) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(inputStream, password);
        return keyStore;
    }

    @Test
    public void KubernetesAuthUsernamePassword() throws Exception {
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
    public void basicConfigWithAuthCertificate() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "",
                true,
                workspace, mockLauncher, build);

        String cert = "-----BEGIN CERTIFICATE-----\nMIICazCCAdQCCQDVtVxaHvqqtzANBgkqhkiG9w0BAQUFADB6MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEQMA4GA1UEChMHSmVua2luczEaMBgGA1UEAxMRS3ViZXJuZXRlcy1Vc2VyLTExKDAmBgkqhkiG9w0BCQEWGWt1YmVybmV0ZXMtdXNlci0xQGplbmtpbnMwHhcNMTcxMDAzMTI1NzU5WhcNMTgxMDAzMTI1NzU5WjB6MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEQMA4GA1UEChMHSmVua2luczEaMBgGA1UEAxMRS3ViZXJuZXRlcy1Vc2VyLTExKDAmBgkqhkiG9w0BCQEWGWt1YmVybmV0ZXMtdXNlci0xQGplbmtpbnMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALKEbz2+ljp7wMLFXrGaTFx3nGQA4sWlXkKpgjb6+wU7e7XT1n8qh8jDySHL4GUJuN5TMCN56NCx7cMwHwXfdrRXGdPtRLYqGAI+D6qYZTlC8sHSrLVWSVYCMHhIHdFzBlI7kwEXvEmIqR/1RWKgG0mlBxiB5fnlWnja0OTt4ichAgMBAAEwDQYJKoZIhvcNAQEFBQADgYEAFHvKqMNou+idNZCaJJ6x2u0xrkxBG01UbsmxyVwT5uiCrOzsw/xi9IW4vjFFkJezM2RqsCGhFoDP4i64SK++CXmrzURxQJIb/qxGjEC8H4yAU6tk7a+hzYXUkxnvl+Ay9g9ZpVGvykY+lyF4BdvyXgb9heAljwk4mtth6gUywZE=\n-----END CERTIFICATE-----";
        String key = "-----BEGIN PRIVATE KEY-----\nMIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBALKEbz2+ljp7wMLFXrGaTFx3nGQA4sWlXkKpgjb6+wU7e7XT1n8qh8jDySHL4GUJuN5TMCN56NCx7cMwHwXfdrRXGdPtRLYqGAI+D6qYZTlC8sHSrLVWSVYCMHhIHdFzBlI7kwEXvEmIqR/1RWKgG0mlBxiB5fnlWnja0OTt4ichAgMBAAECgYAGE7oRuQY2MXZLaxqhIyaMU0oQoXMW1V1TGaAkLQEUmYTJmM+JfrImpHuZWe5moiEX+G8AFitVx2jXpzC3K3dH98FB9rkrfFjbZXJP8mdhuTQz5yQ0VFysX/E+sf/YtNl63qwgCAMO8E8NDXRp741pMjrEp6py5wRVDz7h7gcwAQJBAOd4LWJ9iCOC9jPBduAVWqRl1o5owCGtVpyBWnN0vD4VQ2NYfJ6YPVavkx2LSZzxGF39eX1BzdEUQ/LrQY8H5qECQQDFb6c7lmNwOYIxz9IaYfwoI+nZp0ZENu+bMx3q2/MMEgXDHaKiyJmixaSm5/Ob2luLqQSE6o+9nQ+pXd9ksPCBAkALCmpvxjkWKIsB0PqQmbQnH0xqooh3ksMM2AaudyT7eRwrwu6+ydgzKFDGGfy65a0Z3ptK5DajAGp1Tc9kuSXBAkASoT5+eOpZJJQMbze8FZLdlsXyK76NoUFqu6APEUIV2X2Bs8Is6hDVMyEePrTV9/y7aO9sO1Xk5nUb3ie+MJQBAkBx+5dVLxuQRwaFU92lCk2GjyFOW7C/2NylYJRWe47u6Dj8+zGCOnVEhiMBZI0zimgQX9UhudOSRB+9c4XXSEO5\n-----END PRIVATE KEY----\n";

        KubernetesAuth auth = new KubernetesAuthCertificate(cert, key);

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
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    client-certificate-data: \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNhekNDQWRRQ0NRRFZ0VnhhSHZxcXR6QU5CZ2txaGtpRzl3MEJBUVVGQURCNk1Rc3dDUVlEVlFRR0V3SkJWVEVUTUJFR0ExVUVDQk1LVTI5dFpTMVRkR0YwWlRFUU1BNEdBMVVFQ2hNSFNtVnVhMmx1Y3pFYU1CZ0dBMVVFQXhNUlMzVmlaWEp1WlhSbGN5MVZjMlZ5TFRFeEtEQW1CZ2txaGtpRzl3MEJDUUVXR1d0MVltVnlibVYwWlhNdGRYTmxjaTB4UUdwbGJtdHBibk13SGhjTk1UY3hNREF6TVRJMU56VTVXaGNOTVRneE1EQXpNVEkxTnpVNVdqQjZNUXN3Q1FZRFZRUUdFd0pCVlRFVE1CRUdBMVVFQ0JNS1UyOXRaUzFUZEdGMFpURVFNQTRHQTFVRUNoTUhTbVZ1YTJsdWN6RWFNQmdHQTFVRUF4TVJTM1ZpWlhKdVpYUmxjeTFWYzJWeUxURXhLREFtQmdrcWhraUc5dzBCQ1FFV0dXdDFZbVZ5Ym1WMFpYTXRkWE5sY2kweFFHcGxibXRwYm5Nd2daOHdEUVlKS29aSWh2Y05BUUVCQlFBRGdZMEFNSUdKQW9HQkFMS0ViejIrbGpwN3dNTEZYckdhVEZ4M25HUUE0c1dsWGtLcGdqYjYrd1U3ZTdYVDFuOHFoOGpEeVNITDRHVUp1TjVUTUNONTZOQ3g3Y013SHdYZmRyUlhHZFB0UkxZcUdBSStENnFZWlRsQzhzSFNyTFZXU1ZZQ01IaElIZEZ6QmxJN2t3RVh2RW1JcVIvMVJXS2dHMG1sQnhpQjVmbmxXbmphME9UdDRpY2hBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUZCUUFEZ1lFQUZIdktxTU5vdStpZE5aQ2FKSjZ4MnUweHJreEJHMDFVYnNteHlWd1Q1dWlDck96c3cveGk5SVc0dmpGRmtKZXpNMlJxc0NHaEZvRFA0aTY0U0srK0NYbXJ6VVJ4UUpJYi9xeEdqRUM4SDR5QVU2dGs3YStoellYVWt4bnZsK0F5OWc5WnBWR3Z5a1krbHlGNEJkdnlYZ2I5aGVBbGp3azRtdHRoNmdVeXdaRT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\"\n" +
                "    client-key-data: \"LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JSUNkUUlCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQWw4d2dnSmJBZ0VBQW9HQkFMS0ViejIrbGpwN3dNTEZYckdhVEZ4M25HUUE0c1dsWGtLcGdqYjYrd1U3ZTdYVDFuOHFoOGpEeVNITDRHVUp1TjVUTUNONTZOQ3g3Y013SHdYZmRyUlhHZFB0UkxZcUdBSStENnFZWlRsQzhzSFNyTFZXU1ZZQ01IaElIZEZ6QmxJN2t3RVh2RW1JcVIvMVJXS2dHMG1sQnhpQjVmbmxXbmphME9UdDRpY2hBZ01CQUFFQ2dZQUdFN29SdVFZMk1YWkxheHFoSXlhTVUwb1FvWE1XMVYxVEdhQWtMUUVVbVlUSm1NK0pmckltcEh1WldlNW1vaUVYK0c4QUZpdFZ4MmpYcHpDM0szZEg5OEZCOXJrcmZGamJaWEpQOG1kaHVUUXo1eVEwVkZ5c1gvRStzZi9ZdE5sNjNxd2dDQU1POEU4TkRYUnA3NDFwTWpyRXA2cHk1d1JWRHo3aDdnY3dBUUpCQU9kNExXSjlpQ09DOWpQQmR1QVZXcVJsMW81b3dDR3RWcHlCV25OMHZENFZRMk5ZZko2WVBWYXZreDJMU1p6eEdGMzllWDFCemRFVVEvTHJRWThINXFFQ1FRREZiNmM3bG1Od09ZSXh6OUlhWWZ3b0krblpwMFpFTnUrYk14M3EyL01NRWdYREhhS2l5Sm1peGFTbTUvT2IybHVMcVFTRTZvKzluUStwWGQ5a3NQQ0JBa0FMQ21wdnhqa1dLSXNCMFBxUW1iUW5IMHhxb29oM2tzTU0yQWF1ZHlUN2VSd3J3dTYreWRnektGREdHZnk2NWEwWjNwdEs1RGFqQUdwMVRjOWt1U1hCQWtBU29UNStlT3BaSkpRTWJ6ZThGWkxkbHNYeUs3Nk5vVUZxdTZBUEVVSVYyWDJCczhJczZoRFZNeUVlUHJUVjkveTdhTzlzTzFYazVuVWIzaWUrTUpRQkFrQngrNWRWTHh1UVJ3YUZVOTJsQ2syR2p5Rk9XN0MvMk55bFlKUldlNDd1NkRqOCt6R0NPblZFaGlNQlpJMHppbWdRWDlVaHVkT1NSQis5YzRYWFNFTzUKLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tCg==\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithAuthToken() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "",
                true,
                workspace, mockLauncher, build);

        KubernetesAuth auth = new KubernetesAuthToken(new DummyTokenCredentialImpl(CredentialsScope.GLOBAL, "test", "test", "test", "test"));

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
                "users:\n" +
                "- name: \"test-credential\"\n" +
                "  user:\n" +
                "    as-user-extra: {}\n" +
                "    token: \"faketoken:test:test\"\n", configDumpContent);
    }

    @Test
    public void basicConfigWithAuthKeystore() throws Exception {
        KubeConfigWriter configWriter = new KubeConfigWriter(
                "https://localhost:6443",
                "test-credential",
                "",
                "",
                "",
                "",
                true,
                workspace, mockLauncher, build);

        try (InputStream resourceAsStream = getClass().getResourceAsStream("../kubernetes.pkcs12")) {
            KeyStore keyStore = loadKeyStore(resourceAsStream, "test".toCharArray());

            KubernetesAuth auth = new KubernetesAuthKeystore(keyStore, "test");

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
                    "users:\n" +
                    "- name: \"test-credential\"\n" +
                    "  user:\n" +
                    "    as-user-extra: {}\n" +
                    "    client-certificate-data: \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNhekNDQWRRQ0NRRFZ0VnhhSHZxcXR6QU5CZ2txaGtpRzl3MEJBUVVGQURCNk1Rc3dDUVlEVlFRR0V3SkJWVEVUTUJFR0ExVUVDQk1LVTI5dFpTMVRkR0YwWlRFUU1BNEdBMVVFQ2hNSFNtVnVhMmx1Y3pFYU1CZ0dBMVVFQXhNUlMzVmlaWEp1WlhSbGN5MVZjMlZ5TFRFeEtEQW1CZ2txaGtpRzl3MEJDUUVXR1d0MVltVnlibVYwWlhNdGRYTmxjaTB4UUdwbGJtdHBibk13SGhjTk1UY3hNREF6TVRJMU56VTVXaGNOTVRneE1EQXpNVEkxTnpVNVdqQjZNUXN3Q1FZRFZRUUdFd0pCVlRFVE1CRUdBMVVFQ0JNS1UyOXRaUzFUZEdGMFpURVFNQTRHQTFVRUNoTUhTbVZ1YTJsdWN6RWFNQmdHQTFVRUF4TVJTM1ZpWlhKdVpYUmxjeTFWYzJWeUxURXhLREFtQmdrcWhraUc5dzBCQ1FFV0dXdDFZbVZ5Ym1WMFpYTXRkWE5sY2kweFFHcGxibXRwYm5Nd2daOHdEUVlKS29aSWh2Y05BUUVCQlFBRGdZMEFNSUdKQW9HQkFMS0ViejIrbGpwN3dNTEZYckdhVEZ4M25HUUE0c1dsWGtLcGdqYjYrd1U3ZTdYVDFuOHFoOGpEeVNITDRHVUp1TjVUTUNONTZOQ3g3Y013SHdYZmRyUlhHZFB0UkxZcUdBSStENnFZWlRsQzhzSFNyTFZXU1ZZQ01IaElIZEZ6QmxJN2t3RVh2RW1JcVIvMVJXS2dHMG1sQnhpQjVmbmxXbmphME9UdDRpY2hBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUZCUUFEZ1lFQUZIdktxTU5vdStpZE5aQ2FKSjZ4MnUweHJreEJHMDFVYnNteHlWd1Q1dWlDck96c3cveGk5SVc0dmpGRmtKZXpNMlJxc0NHaEZvRFA0aTY0U0srK0NYbXJ6VVJ4UUpJYi9xeEdqRUM4SDR5QVU2dGs3YStoellYVWt4bnZsK0F5OWc5WnBWR3Z5a1krbHlGNEJkdnlYZ2I5aGVBbGp3azRtdHRoNmdVeXdaRT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQ==\"\n" +
                    "    client-key-data: \"LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JSUNkUUlCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQWw4d2dnSmJBZ0VBQW9HQkFMS0ViejIrbGpwN3dNTEZYckdhVEZ4M25HUUE0c1dsWGtLcGdqYjYrd1U3ZTdYVDFuOHFoOGpEeVNITDRHVUp1TjVUTUNONTZOQ3g3Y013SHdYZmRyUlhHZFB0UkxZcUdBSStENnFZWlRsQzhzSFNyTFZXU1ZZQ01IaElIZEZ6QmxJN2t3RVh2RW1JcVIvMVJXS2dHMG1sQnhpQjVmbmxXbmphME9UdDRpY2hBZ01CQUFFQ2dZQUdFN29SdVFZMk1YWkxheHFoSXlhTVUwb1FvWE1XMVYxVEdhQWtMUUVVbVlUSm1NK0pmckltcEh1WldlNW1vaUVYK0c4QUZpdFZ4MmpYcHpDM0szZEg5OEZCOXJrcmZGamJaWEpQOG1kaHVUUXo1eVEwVkZ5c1gvRStzZi9ZdE5sNjNxd2dDQU1POEU4TkRYUnA3NDFwTWpyRXA2cHk1d1JWRHo3aDdnY3dBUUpCQU9kNExXSjlpQ09DOWpQQmR1QVZXcVJsMW81b3dDR3RWcHlCV25OMHZENFZRMk5ZZko2WVBWYXZreDJMU1p6eEdGMzllWDFCemRFVVEvTHJRWThINXFFQ1FRREZiNmM3bG1Od09ZSXh6OUlhWWZ3b0krblpwMFpFTnUrYk14M3EyL01NRWdYREhhS2l5Sm1peGFTbTUvT2IybHVMcVFTRTZvKzluUStwWGQ5a3NQQ0JBa0FMQ21wdnhqa1dLSXNCMFBxUW1iUW5IMHhxb29oM2tzTU0yQWF1ZHlUN2VSd3J3dTYreWRnektGREdHZnk2NWEwWjNwdEs1RGFqQUdwMVRjOWt1U1hCQWtBU29UNStlT3BaSkpRTWJ6ZThGWkxkbHNYeUs3Nk5vVUZxdTZBUEVVSVYyWDJCczhJczZoRFZNeUVlUHJUVjkveTdhTzlzTzFYazVuVWIzaWUrTUpRQkFrQngrNWRWTHh1UVJ3YUZVOTJsQ2syR2p5Rk9XN0MvMk55bFlKUldlNDd1NkRqOCt6R0NPblZFaGlNQlpJMHppbWdRWDlVaHVkT1NSQis5YzRYWFNFTzUKLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLQ==\"\n", configDumpContent);
        }
    }
}
