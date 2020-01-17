node{
  stage('Run') {
    withKubeConfig([credentialsId: 'test-credentials', serverUrl: 'https://localhost:6443']) {
      echo "Using temporary file ${env.KUBECONFIG}"
    }
  }
}
