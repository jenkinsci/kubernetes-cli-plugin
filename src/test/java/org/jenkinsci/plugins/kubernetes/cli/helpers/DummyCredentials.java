package org.jenkinsci.plugins.kubernetes.cli.helpers;

import java.nio.charset.StandardCharsets;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

import hudson.util.Secret;

/**
 * @author Max Laverse
 */
public class DummyCredentials {
    public static final String PASSPHRASE = "test";
    public static final String USERNAME_WITH_SPACE = "bob with-userspace";
    public static final String USERNAME = "bob";
    public static final String PASSWORD = "s3cr3t";
    public static final String PASSWORD_WITH_SPACE = "s3cr3t with-passwordspace";

    public static BaseStandardCredentials secretCredential(String credentialId) {
        return new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", Secret.fromString(PASSWORD));
    }

    public static BaseStandardCredentials certificateCredential(String credentialId) {
        byte[] storeFile = TestResourceLoader.loadAsByteArray("kubernetes.pkcs12");
        CertificateCredentialsImpl.KeyStoreSource keyStoreSource = new CertificateCredentialsImpl.UploadedKeyStoreSource(null,
                SecretBytes.fromBytes(storeFile));
        return new CertificateCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", PASSPHRASE,
                keyStoreSource);
    }

    public static BaseStandardCredentials brokenCertificateCredential(String credentialId) {
        byte[] storeFile = TestResourceLoader.loadAsByteArray("kubernetes.pkcs12");
        CertificateCredentialsImpl.KeyStoreSource keyStoreSource = new CertificateCredentialsImpl.UploadedKeyStoreSource(null,
                SecretBytes.fromBytes(storeFile));
        return new CertificateCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", "bad-passphrase",
                keyStoreSource);
    }

    public static BaseStandardCredentials usernamePasswordCredential(String credentialId) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", USERNAME, PASSWORD);
    }

    public static BaseStandardCredentials usernamePasswordCredentialWithSpace(String credentialId) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", USERNAME_WITH_SPACE,
                PASSWORD_WITH_SPACE);
    }

    public static FileCredentials fileCredential(String credentialId) {
        return fileCredential(credentialId, "test-cluster", "test-user");
    }

    public static FileCredentials fileCredential(String credentialId, String clusterName, String userName) {
        return new FileCredentialsImpl(CredentialsScope.GLOBAL,
                credentialId,
                "sample",
                "file-name",
                SecretBytes.fromBytes(("---\n" +
                        "apiVersion: \"v1\"\n" +
                        "clusters:\n" +
                        "- cluster:\n" +
                        "    insecure-skip-tls-verify: true\n" +
                        "    server: https://" + clusterName + "\n" +
                        "  name: \"" + clusterName + "\"\n" +
                        "contexts:\n" +
                        "- context:\n" +
                        "    cluster: \"" + clusterName + "\"\n" +
                        "    user: \"" + userName + "\"\n" +
                        "  name: \"" + clusterName + "\"\n" +
                        "current-context: \"" + clusterName + "\"\n" +
                        "users:\n" +
                        "- name: \"" + userName + "\"\n").getBytes(StandardCharsets.UTF_8)));
    }

    public static DummyTokenCredentialImpl tokenCredential(String credentialId) {
        return new DummyTokenCredentialImpl(CredentialsScope.GLOBAL, credentialId, "a-description", USERNAME, PASSWORD);
    }

    public static UnsupportedCredentialImpl unsupportedCredential(String credentialId) {
        return new UnsupportedCredentialImpl(credentialId, "sample");
    }
}
