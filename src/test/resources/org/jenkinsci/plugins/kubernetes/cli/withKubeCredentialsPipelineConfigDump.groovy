node{
  stage('Run') {
    withKubeCredentials([[credentialsId: 'test-credentials'], [credentialsId: 'cred9999']]) {
      sh 'kubectl config view > configDump'
    }
  }
}
