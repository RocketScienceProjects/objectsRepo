node("$NODE") {

 stage('checkout')
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
  userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]

 env.GIT_TAG_NAME = gitTagName()
 env.GIT_TAG_MESSAGE = gitTagMessage()

 stage('package')
  xldCreatePackage artifactsPath: 'release1/DataIntegration/Workflow', darPath: 'output.dar', manifestPath: 'deploy/zing_working411.XML'

stage('publish')
  xldPublishPackage darPath: 'output.dar', serverCredentials: 'admin'

stage('deploy')
  xldDeploy environmentId: 'Environments/informatica_test', packageId: 'Applications/informaticaApp/$GIT_TAG_NAME.$BUILD_NUMBER', serverCredentials: 'admin'  


}
