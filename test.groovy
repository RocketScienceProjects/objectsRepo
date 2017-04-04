import groovy.xml.*

node('master') {

  deleteDir()

  stage('Checkout') {
    checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false,
    extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'release1'], [path: 'Parameters']]]], submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]
  }

  env.GIT_TAG_NAME = gitTagName()
  env.GIT_TAG_MESSAGE = gitTagMessage()



  /*
  This will generate the manifest xml
  */

String input = "$objectListParameter"
println input
def inputMap = Eval.me("$input")
def objectList=[]
objectList << inputMap
println objectList
println objectList.getClass()
//call the method
generateXML(objectList)

// String input = "$objectListParameter"
// def l = []
// l.removeAll()
// l << input
// println l
// println l.class.name
// generateXML(l)

}

/*

stage('Package') {
xldCreatePackage artifactsPath: 'release1', manifestPath: 'deployit-manifest.xml', darPath: '$JOB_NAME-WF.$BUILD_NUMBER.dar'
}
stage('Publish') {
xldPublishPackage serverCredentials: 'admin', darPath: '$JOB_NAME-WF.$BUILD_NUMBER.dar'
}

stage('Deploy') {
xldDeploy serverCredentials: 'admin', environmentId: 'Environments/informatica_test', packageId: 'Applications/informaticaApp/$BUILD_NUMBER'
}

}
*/


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

@NonCPS
generateXML(List workflows) {
  def writer = new FileWriter("$WORKSPACE/deployit-manifestTAK.xml")
  def builder = new StreamingMarkupBuilder()
  builder.encoding = 'UTF-8'
  writer << builder.bind {
    mkp.xmlDeclaration()
    mkp.declareNamespace(udm :'http://www.w3.org/2001/XMLSchema')
    mkp.declareNamespace(powercenter:'http://www.w3.org/2001/XMLSchema')
    delegate.udm.DeploymentPackage(version:'$BUILD_NUMBER', application: "informaticaApp"){
      delegate.deployables {
        workflows.each { item ->
          delegate.powercenter.PowercenterXml(name:item.name, file:item.file) {
            delegate.scanPlaceholders(true)
            delegate.sourceRepository(item.sourceRepository)
            delegate.folderNameMap {
              item.folderNames.each { name, value ->
                it.entry(key:name, value)
              }
            }
            delegate.objectNames {
              delegate.value(item.objectName)
            }
            delegate.objectTypes {
              delegate.value(item.objectType)
            }
          }
        }
      }
      delegate.dependencyResolution('LATEST')
      delegate.undeployDependencies(false)
    }
  }
}