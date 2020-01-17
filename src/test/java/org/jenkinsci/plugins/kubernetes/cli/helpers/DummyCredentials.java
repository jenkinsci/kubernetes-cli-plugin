package org.jenkinsci.plugins.kubernetes.cli.helpers;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.Secret;
import org.apache.commons.compress.utils.IOUtils;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

import java.io.UnsupportedEncodingException;

/**
 * @author Max Laverse
 */
public class DummyCredentials {
    public static final String PASSPHRASE = "test";
    public static final String USERNAME_WITH_SPACE = "bob with-userspace";
    public static final String USERNAME = "bob";
    public static final String PASSWORD = "s3cr3t";
    public static final String PASSWORD_WITH_SPACE = "s3cr3t with-passwordspace";

    public  static String loadResource(String name) {
        try {
            return new String(IOUtils.toByteArray(DummyCredentials.class.getResourceAsStream(name)));
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }

    public  static String getResourceFile(String name) {
        return DummyCredentials.class.getResource(name).getFile();
    }


    public  static BaseStandardCredentials secretCredential(String credentialId) {
        return new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", Secret.fromString(PASSWORD));
    }

    public  static BaseStandardCredentials secretCredentialWithSpace(String credentialId) {
        return new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", Secret.fromString(PASSWORD_WITH_SPACE));
    }

    public static  BaseStandardCredentials certificateCredential(String credentialId) {
        String storeFile = getResourceFile("/org/jenkinsci/plugins/kubernetes/cli/kubernetes.pkcs12");
        CertificateCredentialsImpl.KeyStoreSource keyStoreSource = new CertificateCredentialsImpl.FileOnMasterKeyStoreSource(storeFile);
        return new CertificateCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", PASSPHRASE, keyStoreSource);
    }

    public  static BaseStandardCredentials brokenCertificateCredential(String credentialId) {
        String storeFile = getResourceFile("/org/jenkinsci/plugins/kubernetes/cli/kubernetes.pkcs12");
        CertificateCredentialsImpl.KeyStoreSource keyStoreSource = new CertificateCredentialsImpl.FileOnMasterKeyStoreSource(storeFile);
        return new CertificateCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", "bad-passphrase", keyStoreSource);
    }

    public  static BaseStandardCredentials usernamePasswordCredential(String credentialId) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", USERNAME, PASSWORD);
    }

    public  static BaseStandardCredentials usernamePasswordCredentialWithSpace(String credentialId) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "sample", USERNAME_WITH_SPACE, PASSWORD_WITH_SPACE);
    }

    public  static FileCredentials fileCredential(String credentialId) throws UnsupportedEncodingException {
        String clusterName = credentialId == "test-credentials" ? "test-sample" : credentialId;
        return new FileCredentialsImpl(CredentialsScope.GLOBAL,
                credentialId,
                "sample",
                "file-name",
                SecretBytes.fromBytes(("---\n" +
                        "apiVersion: \"v1\"\n" +
                        "clusters:\n" +
                        "- name: \"" + clusterName + "\"\n" +
                        "contexts:\n" +
                        "- context:\n" +
                        "    cluster: \"" + clusterName + "\"\n" +
                        "  name: \"" + clusterName + "\"\n" +
                        "- name: \"minikube\"\n" +
                        "current-context: \""+clusterName+"\"\n" +
                        "users: []").getBytes("UTF-8")));
    }

    public  static DummyTokenCredentialImpl tokenCredential(String credentialId) {
        return new DummyTokenCredentialImpl(CredentialsScope.GLOBAL, credentialId, "a-description", USERNAME, PASSWORD);
    }

    public static UnsupportedCredentialImpl unsupportedCredential(String credentialId) {
        return new UnsupportedCredentialImpl(credentialId, "sample");
    }
}
