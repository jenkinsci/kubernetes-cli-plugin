node{
  stage('Run') {
    withKubeCredentials([[credentialsId: 'test-credentials'], [credentialsId: 'cred9999']]) {
      if (isUnix()) {
        sh 'kubectl config view > configDump'
      }else{
        bat 'kubectl.exe config view > configDump'
      }
    }
  }
}
