package org.jenkinsci.plugins.kubernetes.cli.helpers;

import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

public class UnsupportedCredentialImpl extends BaseStandardCredentials {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnsupportedCredentialImpl(String id, String description) {
        super(id, description);
    }
}
