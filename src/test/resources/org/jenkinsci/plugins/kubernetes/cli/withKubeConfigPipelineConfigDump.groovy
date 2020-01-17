node{
  label "mocked-kubectl"
  stage('Run') {
    withKubeConfig([credentialsId: 'test-credentials', serverUrl: 'https://localhost:6443']) {
      sh "kubectl config view > configDump"
    }
  }
}
