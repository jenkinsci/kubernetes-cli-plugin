package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import hudson.AbortException;
import hudson.FilePath;
import hudson.util.Secret;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * @author Max Laverse
 */
public abstract class CertificateHelper {
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    public static String wrapPrivateKey(String encodedBody) {
        return wrapWithMarker(BEGIN_PRIVATE_KEY, END_PRIVATE_KEY, encodedBody);
    }

    public static String wrapCertificate(String encodedBody) {
        return wrapWithMarker(BEGIN_CERTIFICATE, END_CERTIFICATE, encodedBody);
    }

    private static String wrapWithMarker(String begin, String end, String encodedBody) {
        if (encodedBody.startsWith(begin)) {
            return encodedBody;
        }
        return new StringBuilder(begin).append("\n")
                .append(encodedBody).append("\n")
                .append(end)
                .toString();
    }
}
