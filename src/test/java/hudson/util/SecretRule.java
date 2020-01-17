package hudson.util;

import org.junit.rules.ExternalResource;

public class SecretRule extends ExternalResource {
    private String oldSecret;

    @Override
    protected void before() throws Throwable {
        oldSecret = Secret.SECRET;
        Secret.SECRET = "test";
    }

    @Override
    protected void after() {
        Secret.SECRET = oldSecret;
    }
}

