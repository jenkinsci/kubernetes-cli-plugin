// Java versions to test against
String[] unitTestJdkVersions = [21, 25]
String integrationJdkVersions = 21

// Platform to test for
String[] platforms = [
 'windows',
 'linux',
]

// Kubectl versions to test against
String[] kubectlVersions = [
    '1.29.14',
    '1.30.10',
    '1.31.6',
    '1.32.2',
]

// Not sure what this does yet
def buildNumber = BUILD_NUMBER as int; if (buildNumber > 1) milestone(buildNumber - 1); milestone(buildNumber) // JENKINS-43353 / JENKINS-58625
properties([
    durabilityHint('PERFORMANCE_OPTIMIZED'),
    buildDiscarder(logRotator(numToKeepStr: '3')),
])

// Generates a build block that can later be run in parallel
// for which the kubectl version, platform and java version
// can be specified
def integrationTest(platform, jdk, kubectlVersion) {
    node(platform) {
        timeout(15) {
            withEnv(["PATH+KUBECTL=${env.WORKSPACE}/.bin", "KUBECTL_VERSION=v${kubectlVersion}"]){
                stage('checkout') {
                    checkout scm
                }
                stage('preparation') {
                    retry(5) {
                        downloadKubectl(kubectlVersion)
                    }
                }
                stage('tests') {
                    infra.runWithJava('mvn clean -Dgroups=org.jenkinsci.plugins.kubernetes.cli.KubectlIntegrationTest test', jdk)
                }
            }
        }
    }
}

def unitTest(platform, jdk) {
    node(platform) {
        timeout(15) {
            stage('checkout') {
                checkout scm
            }
            stage('tests') {
                infra.runWithJava('mvn clean -Dgroups="! org.jenkinsci.plugins.kubernetes.cli.KubectlIntegrationTest" test spotbugs:check', jdk)
            }
            // stage('coverage') {
            //     infra.runWithJava('mvn jacoco:report coveralls:report')
            // }
        }
    }
}

// Generates a script block to download kubectl
def downloadKubectl(kubectlVersion){
    if (isUnix()) {
        sh """
            mkdir -p .bin
            cd .bin
            curl -LO https://dl.k8s.io/release/v${kubectlVersion}/bin/linux/amd64/kubectl
            chmod +x kubectl
            kubectl version --client 
        """
    }else{
        bat """
            mkdir -p .bin
            cd .bin
            curl -LO https://dl.k8s.io/release/v${kubectlVersion}/bin/windows/amd64/kubectl.exe
            kubectl.exe version --client
        """
    }
}

// Build a step for all combinations we want to test for
def stepsForParallel = [:]
for (int i = 0; i < kubectlVersions.size(); i++) {
    def kubectlVersion = kubectlVersions[i]

    for (int j = 0; j < platforms.size(); j++) {
        def platform = platforms[j]

        def stepName = "integration/${platform}/v${kubectlVersion}"
        stepsForParallel[stepName] = { ->
            integrationTest(platform, integrationJdkVersions, kubectlVersion)
        }
    }
}
for (int j = 0; j < platforms.size(); j++) {
    def platform = platforms[j]

    for (int k = 0; k < unitTestJdkVersions.size(); k++) {
    def jdkVersion = unitTestJdkVersions[k]
        def stepName = "unit/${platform}/${jdkVersion}"
        stepsForParallel[stepName] = { ->
            unitTest(platform, jdkVersion)
        }
    }
}

// Run tests in parallel
parallel stepsForParallel
