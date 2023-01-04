node{
  label "mocked-kubectl"
  stage('Run') {
    withKubeConfig([credentialsId: 'test-credentials', serverUrl: 'https://localhost:6443']) {
      if (isUnix()) {
        sh 'kubectl config view --raw > configDump'
      }else{
        bat 'kubectl.exe config view --raw > configDump'
      }
    }
  }
}
