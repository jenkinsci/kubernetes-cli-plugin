package org.jenkinsci.plugins.kubernetes.cli.helpers;

import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

public class UnsupportedCredentialImpl extends BaseStandardCredentials {

    public UnsupportedCredentialImpl(String id, String description) {
        super(id, description);
    }
}
