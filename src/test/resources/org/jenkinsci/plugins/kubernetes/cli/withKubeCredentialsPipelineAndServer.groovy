node{
  stage('Run') {
    withKubeCredentials([
      [credentialsId: 'test-credentials', clusterName: 'test-cluster', serverUrl: 'https://localhost:1234'],
      [credentialsId: 'cred9999', clusterName: 'cred9999', serverUrl: 'https://localhost:9999']
    ]) {
      sh 'kubectl config view > configDump'
    }
  }
}
