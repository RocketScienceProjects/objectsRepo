node('master') {

deleteDir()

stage('Checkout') {
  checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false,
  extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'release1'], [path: 'Parameters']]]], submoduleCfg: [],
  userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]
}

env.GIT_TAG_NAME = gitTagName()
env.GIT_TAG_MESSAGE = gitTagMessage()

 stage('Package') {
   xldCreatePackage artifactsPath: 'release1', manifestPath: 'release1/deployit-manifest.xml', darPath: '$JOB_NAME.$BUILD_NUMBER.dar'
 }
 stage('Publish') {
   xldPublishPackage serverCredentials: 'admin', darPath: '$JOB_NAME.$BUILD_NUMBER.dar'
 }

 /* stage('Deploy') {
   xldDeploy serverCredentials: 'admin', environmentId: 'Environments/informatica_test', packageId: 'Applications/FileDeploy/$GIT_TAG_NAME.$BUILD_NUMBER'
 }
 */

}


/** @return The tag name, or `null` if the current commit isn't a tag. */
String gitTagName() {
    commit = getCommit()
    if (commit) {
        desc = sh(script: "git describe --tags ${commit}", returnStdout: true)?.trim()
        if (isTag(desc)) {
            return desc
        }
    }
    return null
}

/** @return The tag message, or `null` if the current commit isn't a tag. */
String gitTagMessage() {
    name = gitTagName()
    msg = sh(script: "git tag -n10000 -l ${name}", returnStdout: true)?.trim()
    if (msg) {
        return msg.substring(name.size()+1, msg.size())
    }
    return null
}

String getCommit() {
    return sh(script: 'git rev-parse HEAD', returnStdout: true)?.trim()
}

@NonCPS
boolean isTag(String desc) {
    match = desc =~ /.+-[0-9]+-g[0-9A-Fa-f]{6,}$/
    result = !match
    match = null // prevent serialisation
    return result
}
