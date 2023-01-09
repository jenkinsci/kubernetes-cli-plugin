CHANGELOG
=========

1.11.0
-----
* upgrade kubernetes-client to 6.x [#101](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/101)
* add support for making kubeconfig only accessible by owner [#96](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/96)
* fix incluster config without namespace [#97](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/97)
* separate unit and integration tests [#95](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/95)
* stop testing EOL versions [#94](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/94)
* run tests for 1.22 and 1.23 [#90](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/90)
* fix typo in README [#88](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/88)
* move from Travis to Jenkins CI [#87](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/87)
* compile against kubernetes-client-api-5.4.1 [#86](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/86)

1.10.3
-----
* compile against kubernetes-client-api 5.4.1 [#86](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/86)

1.10.2
-----
* test with jdk11 [#83](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/83)
* upgrade kubernetes-credentials to 0.9.0 [#84](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/84)

1.10.1
-----
* test for Kubernetes 1.21 [#81](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/81)
* fix permission check [#82](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/82)

1.10.0
-----
* test against 1.20, drop 1.16 [#78](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/78)
* use kubernetes-credentials 0.8.0 [#79](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/79)

1.9.0
-----
* use kubernetes-credentials 0.7.0 [#65](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/65)
* add environment interpolation for more parameters [#74](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/74)
* test against new kubectl versions [#75](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/75)

1.8.3
-----
* Fix UI job configuration for multiple credentials not being saved [#61](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/61)

1.8.2
-----
* Fix raw kubeconfig context switch [#52](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/52)
* Fix creation of new contextes and add tests [#53](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/53)

1.8.1
-----
* Write kubeconfig back in the workspace [#50](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/50)

1.8.0
-----
* Fix some warnings when running the tests [#48](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/48)
* Speed up testsr [#47](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/47)
* Apply Jenkins infrastructure changes to pom.xml [#46](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/46)
* Add a quick implementation overview in the README [#45](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/45)
* Add support for new kubectl versions and drop olders [#44](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/44)
* Don't use kubectl to set credentials up [#43](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/43)
* Support configuration of multiple kube credentials at the same time [#40](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/40)
* Remove duplicated KubeConfigExpander [#39](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/39)
* Fix escaping of certificate-authority parameter [#38](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/38)
* Incrementals [#35](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/35)
* Tests were not passing on my machine because paths were not quoted [#34](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/34)
* Fix README when setting up ServiceAccount in non-default namespace [#33](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/33)
* Corrected decode flag syntax in README [#32](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/32)

1.7.0
-----
* Update credentials plugin from 2.1.7 to 2.1.19 (2.2.0) CVE-2019-10320 [#30](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/30)
* Drop support for 1.8,1.9 - Test for 1.15 [#31](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/31)

1.6.0
-----
* Resolve environment variables in serverUrl for Freestyle jobs [#26](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/26)

1.5.0
-----
* Add support for namespace [#23](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/23)
* Other changes [#24](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/24)
* Fix plain kubeconfig setup [#25](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/25)

1.4.0
-----
* Added support for clusterName option [#19](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/19)
* Upgrade kubectl versions [#20](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/20)
* Add tests for kubectl 1.13 [#21](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/21)

1.3.0
-----
* Add tests for kubectl 1.11 [#11](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/11)
* Test listing credentials [#12](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/12)
* Add support for 1.12.0 [#13](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/13)
* Use latest kubectl versions for tests [#14](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/14)
* Depend on apache-httpcomponent plugin [#15](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/15)
* Missing comma in documentation [#16](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/16)
* Base64 Decode that CA certificates [#17](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/17)
* Don't require ca certificates to be base64 encoded [#18](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/18)

1.2.0
-----
* Add tests for kubectl 1.11 [#10](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/10)
* Add support for kubectl 1.10.5 [#8](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/8)
* Add support for contextName [#4](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/4)

1.1.0
-----
* Upgrade kubernetes-credentials to 0.3.1 [#1](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/1)
* Add support for FileCredentials [#2](https://github.com/jenkinsci/kubernetes-cli-plugin/pull/2)

1.0.0
-----
* Fix sporadic execution error when running kubectl concurrently

0.1.0
-----
* Import authentication feature from the kubernetes plugin
* Add support for StringCredentials
* Add support for scoped credentials
* Add support for tokens and password with spaces
* Embed certificates into the kubeconfig
* Fix the masks used on the commands to prevent tokens from leaking into the logs
* Display kubectl output in the logs
