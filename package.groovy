node("$NODE") {

 stage('checkout')
  //heckout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
  //userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/tags/$tag']], doGenerateSubmoduleConfigurations: false,
                              extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', refspec: '+refs/tags/*:refs/remotes/origin/tags/*', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]



  env.GIT_TAG_NAME = gitTagName()
  //env.GIT_TAG_MESSAGE = gitTagMessage()

 stage('package')  //testing the v6.1.0
  xldCreatePackage artifactsPath: '.', darPath: '$JOB_NAME-output.dar', manifestPath: 'deploy/deployit-manifest.xml'

stage('publish')
  xldPublishPackage darPath: '$JOB_NAME-output.dar', serverCredentials: 'admin'

stage('deploy')
  xldDeploy environmentId: 'Environments/informatica_test', packageId: 'Applications/informaticaApp/$GIT_TAG_NAME.$BUILD_NUMBER', serverCredentials: 'admin'


}


/** @return The tag name, or `null` if the current commit isn't a tag. */
String gitTagName() {
    commit = getCommit()
    println commit
    if (commit) {
        desc = sh "git describe --tags ${commit}"
        println desc
        if (isTag(desc)) {
            return desc
        }
    }
    return null
}

String getCommit() {
    commits = sh 'git rev-parse HEAD'
    return commits
}

@NonCPS
boolean isTag(String desc) {
    match = desc =~ /.+-[0-9]+-g[0-9A-Fa-f]{6,}$/
    result = !match
    println result
    match = null // prevent serialisation
    return result
}
