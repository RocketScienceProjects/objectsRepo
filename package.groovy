node("$NODE") {

 stage('checkout')
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
  userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]

 // env.GIT_TAG_NAME = gitTagName()
 // env.GIT_TAG_MESSAGE = gitTagMessage()

 stage('package')  //testing the v6.1.0
  xldCreatePackage artifactsPath: '.', darPath: '$JOB_NAME-output.dar', manifestPath: 'deploy/deployit-manifest.xml'

stage('publish')
  xldPublishPackage darPath: '$JOB_NAME-output.dar', serverCredentials: 'admin'

stage('deploy')
  xldDeploy environmentId: 'Environments/informatica_test', packageId: 'Applications/informaticaApp/$BUILD_NUMBER', serverCredentials: 'admin'


}
