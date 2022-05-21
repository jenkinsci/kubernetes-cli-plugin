package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CertificateHelperTest {
    @Test
    public void testWrapCertificate() {
        String wrapperCertificate = CertificateHelper.wrapCertificate("a-certificate");
        assertEquals("-----BEGIN CERTIFICATE-----\na-certificate\n-----END CERTIFICATE-----", wrapperCertificate);
    }

    @Test
    public void testWrapAlreadyWrappedCertificate() {
        String wrapperCertificate = CertificateHelper
                .wrapCertificate("-----BEGIN CERTIFICATE-----\na-certificate\n-----END CERTIFICATE-----");
        assertEquals("-----BEGIN CERTIFICATE-----\na-certificate\n-----END CERTIFICATE-----", wrapperCertificate);
    }

    @Test
    public void testWrapPrivateKey() {
        String wrapperCertificate = CertificateHelper.wrapPrivateKey("a-key");
        assertEquals("-----BEGIN PRIVATE KEY-----\na-key\n-----END PRIVATE KEY-----", wrapperCertificate);
    }

    @Test
    public void testWrapAlreadyWrappedPrivateKey() {
        String wrapperCertificate = CertificateHelper
                .wrapPrivateKey("-----BEGIN PRIVATE KEY-----\na-key\n-----END PRIVATE KEY-----");
        assertEquals("-----BEGIN PRIVATE KEY-----\na-key\n-----END PRIVATE KEY-----", wrapperCertificate);
    }
}
