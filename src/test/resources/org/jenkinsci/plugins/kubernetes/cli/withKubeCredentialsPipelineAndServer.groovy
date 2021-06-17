node{
  stage('Run') {
    withKubeCredentials([
      [credentialsId: 'test-credentials', clusterName: 'test-cluster', serverUrl: 'https://localhost:1234'],
      [credentialsId: 'cred9999', clusterName: 'cred9999', serverUrl: 'https://localhost:9999']
    ]) {
      if (isUnix()) {
        sh 'kubectl config view > configDump'
      }else{
        bat 'kubectl.exe config view > configDump'
      }
    }
  }
}
