node{
  label "mocked-kubectl"
  stage('Run') {
    withKubeConfig([credentialsId: 'test-credentials', serverUrl: 'https://localhost:6443']) {
      if (isUnix()) {
        sh 'cp -p "$KUBECONFIG" configCopy'
      }else{
        bat 'echo "not implementeded"; exit 1'
      }
    }
  }
}
