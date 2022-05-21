node{
  label "mocked-kubectl"
  stage('Run') {
    withKubeConfig() {
      echo "example when running inside a Pod with a ServiceAccount tokens"
    }
  }
}
