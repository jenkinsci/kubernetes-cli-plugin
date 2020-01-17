package org.jenkinsci.plugins.kubernetes.cli;

import hudson.EnvVars;
import org.jenkinsci.plugins.kubernetes.cli.kubeconfig.KubeConfigWriter;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Max Laverse
 */
final class KubeConfigExpander extends EnvironmentExpander {

    private static final long serialVersionUID = 1;

    private final Map<String, String> overrides;

    KubeConfigExpander(String path) {
        this.overrides = new HashMap<>();
        this.overrides.put(KubeConfigWriter.ENV_VARIABLE_NAME, path);
    }

    @Override
    public void expand(EnvVars env) throws IOException, InterruptedException {
        env.overrideAll(overrides);
    }
}
