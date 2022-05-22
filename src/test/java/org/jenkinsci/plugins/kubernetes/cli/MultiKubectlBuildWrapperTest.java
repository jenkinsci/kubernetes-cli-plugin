package org.jenkinsci.plugins.kubernetes.cli;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;

import org.jenkinsci.plugins.kubernetes.cli.helpers.DummyCredentials;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;

/**
 * @author Max Laverse
 */
public class MultiKubectlBuildWrapperTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testConfigurationPersistedOnSave() throws Exception {
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(),
                DummyCredentials.secretCredential("test-credentials"));

        FreeStyleProject p = r.createFreeStyleProject();

        KubectlCredential kc = new KubectlCredential();
        kc.credentialsId = "test-credentials";
        List<KubectlCredential> l = new ArrayList();
        l.add(kc);
        MultiKubectlBuildWrapper bw = new MultiKubectlBuildWrapper(l);
        p.getBuildWrappersList().add(bw);

        assertEquals("<?xml version='1.1' encoding='UTF-8'?>\n" +
                "<project>\n" +
                "  <keepDependencies>false</keepDependencies>\n" +
                "  <properties/>\n" +
                "  <scm class=\"hudson.scm.NullSCM\"/>\n" +
                "  <canRoam>false</canRoam>\n" +
                "  <disabled>false</disabled>\n" +
                "  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
                "  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
                "  <triggers/>\n" +
                "  <concurrentBuild>false</concurrentBuild>\n" +
                "  <builders/>\n" +
                "  <publishers/>\n" +
                "  <buildWrappers>\n" +
                "    <org.jenkinsci.plugins.kubernetes.cli.MultiKubectlBuildWrapper>\n" +
                "      <kubectlCredentials>\n" +
                "        <org.jenkinsci.plugins.kubernetes.cli.KubectlCredential>\n" +
                "          <credentialsId>test-credentials</credentialsId>\n" +
                "        </org.jenkinsci.plugins.kubernetes.cli.KubectlCredential>\n" +
                "      </kubectlCredentials>\n" +
                "    </org.jenkinsci.plugins.kubernetes.cli.MultiKubectlBuildWrapper>\n" +
                "  </buildWrappers>\n" +
                "</project>", p.getConfigFile().asString());
    }
}
