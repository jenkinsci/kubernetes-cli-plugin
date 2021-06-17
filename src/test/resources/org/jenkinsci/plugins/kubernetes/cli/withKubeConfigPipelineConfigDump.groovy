node{
  label "mocked-kubectl"
  stage('Run') {
    withKubeConfig([credentialsId: 'test-credentials', serverUrl: 'https://localhost:6443']) {
      if (isUnix()) {
        sh 'kubectl config view > configDump'
      }else{
        bat 'kubectl.exe config view > configDump'
      }
    }
  }
}
