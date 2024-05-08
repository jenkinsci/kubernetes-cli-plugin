package org.jenkinsci.plugins.kubernetes.cli.kubeconfig;

import java.io.IOException;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;

/**
 * @author Max Laverse
 */
public abstract class KubeConfigWriterFactory {
    public static KubeConfigWriter get(@NonNull String serverUrl, @NonNull String credentialsId,
            String caCertificate, String clusterName, String contextName, String namespace,
            Boolean restrictKubeConfigAccess, FilePath workspace, Launcher launcher, Run<?, ?> build) {
        return new KubeConfigWriter(serverUrl, credentialsId, caCertificate, clusterName, contextName, namespace,
                restrictKubeConfigAccess, workspace, launcher, build);
    }

    public static KubeConfigWriter get(@NonNull String serverUrl, @NonNull String credentialsId,
            String caCertificate, String clusterName, String contextName, String namespace,
            Boolean restrictKubeConfigAccess, StepContext context) throws IOException, InterruptedException {
        Run<?, ?> run = context.get(Run.class);
        FilePath workspace = context.get(FilePath.class);
        Launcher launcher = context.get(Launcher.class);
        return new KubeConfigWriter(serverUrl, credentialsId, caCertificate, clusterName, contextName, namespace,
                restrictKubeConfigAccess, workspace, launcher, run);
    }
}
