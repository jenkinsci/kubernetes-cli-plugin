node{
  label "mocked-kubectl"
  stage('Run') {
    withKubeConfig([credentialsId: '', serverUrl: 'https://localhost:6443']) {
      echo "This should never be displayed as the plugin should fail"
    }
  }
}
