// Java versions to test against
String[] jdkVersions = [8, 11]

// Platform to test for
String[] platforms = [
 'windows',
 'linux',
]

// Kubectl versions to test against
String[] kubectlVersions = [
    '1.18.20',
    '1.19.16',
    '1.20.15',
    '1.21.10',
    '1.22.7',
    '1.23.4'
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
def buildPlugin(kubectlVersion, platform, jdk) {
    node(platform) {
        timeout(15) {
            withEnv(["PATH+KUBECTL=${env.WORKSPACE}/.bin", "KUBECTL_VERSION=v${kubectlVersion}"]){
                stage('checkout') {
                    checkout scm
                }
                stage('preparation') {
                    downloadKubectl(kubectlVersion)
                }
                stage('tests') {
                    infra.runWithJava('mvn clean test findbugs:check', jdk)
                }
                // stage('coverage') {
                //     infra.runWithJava('mvn jacoco:report coveralls:report')
                // }
            }
        }
    }
}

// Generates a script block to download kubectl
def downloadKubectl(kubectlVersion){
    if (isUnix()) {
        sh """
            mkdir -p .bin
            cd .bin
            curl -LO https://storage.googleapis.com/kubernetes-release/release/v${kubectlVersion}/bin/linux/amd64/kubectl
            chmod +x kubectl
            kubectl version --client 
        """
    }else{
        bat """
            mkdir -p .bin
            cd .bin
            curl -LO https://storage.googleapis.com/kubernetes-release/release/v${kubectlVersion}/bin/windows/amd64/kubectl.exe
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

        for (int k = 0; k < jdkVersions.size(); k++) {
        def jdkVersion = jdkVersions[k]
            def stepName = "${platform}/v${kubectlVersion}/${jdkVersion}"
            stepsForParallel[stepName] = { ->
                buildPlugin(kubectlVersion, platform, jdkVersion)
            }
        }
    }
}

// Run tests in parallel
parallel stepsForParallel
