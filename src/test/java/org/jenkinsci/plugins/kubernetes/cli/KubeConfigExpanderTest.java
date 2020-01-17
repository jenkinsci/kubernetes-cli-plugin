package org.jenkinsci.plugins.kubernetes.cli;

import hudson.EnvVars;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KubeConfigExpanderTest {
    @Test
    public void testKubeConfigVariableIsSet() throws Exception {
        KubeConfigExpander expander = new KubeConfigExpander("a-file-path");
        EnvVars initialEnv = new EnvVars();
        expander.expand(initialEnv);

        assertEquals("a-file-path", initialEnv.get("KUBECONFIG"));
    }

    @Test
    public void testExistingKubeConfigVariableIsOverridden() throws Exception {
        KubeConfigExpander expander = new KubeConfigExpander("a-file-path");
        EnvVars initialEnv = new EnvVars();
        initialEnv.put("KUBECONFIG", "a-wrong-path");
        expander.expand(initialEnv);

        assertEquals("a-file-path", initialEnv.get("KUBECONFIG"));
    }

    @Test
    public void testExistingVariableAreLeftUntouched() throws Exception {
        KubeConfigExpander expander = new KubeConfigExpander("a-file-path");
        EnvVars initialEnv = new EnvVars();
        initialEnv.put("KUBECONFIG", "a-wrong-path");
        initialEnv.put("ANOTHER", "value");
        expander.expand(initialEnv);

        assertEquals("value", initialEnv.get("ANOTHER"));
    }
}
