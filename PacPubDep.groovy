import groovy.xml.*
import groovy.json.JsonSlurper

node('master') {
  deleteDir()

  stage('Checkout') {
    checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]])
  }

  env.GIT_TAG_NAME = gitTagName()
  env.GIT_TAG_MESSAGE = gitTagMessage()

  GenerateXML()
  println "Generated the manifest XML"

  stage('Package') {
    xldCreatePackage artifactsPath: '.', darPath: 'output1.dar', manifestPath: './sampleManifest123.XML'
  }

  stage('Publish') {
    xldPublishPackage serverCredentials: 'admin', darPath: 'output1.dar'
  }

  stage('Deploy') {
    xldDeploy serverCredentials: 'admin', environmentId: 'Environments/informatica_test', packageId: 'Applications/informaticaApp/$BUILD_NUMBER'
  }

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

@NonCPS
def GenerateXML() {
  /*
  parsing the obj.json file
  */
  def currentws = pwd()
  println currentws
  def jsonSlurper = new JsonSlurper();
  def fileReader = new BufferedReader(
    new FileReader("${currentws}/objects.json"))  //the file location need to change in the actual implementation

    def parsedData = jsonSlurper.parse(fileReader)

    /*
    creating the xml
    */
    String sample = "powercenter.PowercenterXml"
    def writer = new FileWriter("${currentws}/sampleManifest123.XML")
    def builder = new StreamingMarkupBuilder()
    builder.encoding = 'UTF-8'
    writer << builder.bind {
      mkp.xmlDeclaration()
      mkp.declareNamespace('udm.DeploymentPackage' :'http://www.w3.org/2001/XMLSchema')
      mkp.declareNamespace('powercenter.PowercenterXml' :'http://www.w3.org/2001/XMLSchema')
      delegate."udm.DeploymentPackage"(version:'$BUILD_NUMBER', application: "informaticaApp"){
        delegate.deployables {
          parsedData.each { index, obj ->
            println this.class.name
            println owner.class.name
            println delegate.class.name
            it."powercenter.PowercenterXml"(name:obj.name, file:obj.file) {
              delegate.scanPlaceholders(true)
              delegate.sourceRepository(obj.sourceRepository)
              delegate.folderNameMap {
                obj.folderNames.each { name, value ->
                  it.entry(key:name, value)
                }
              }
              delegate.objectNames {
                delegate.value(obj.objectName)
              }
              delegate.objectTypes {
                delegate.value(obj.objectType)
              }
            }
          }
        }
        delegate.dependencyResolution('LATEST')
        delegate.undeployDependencies(false)
      }
    }
  }
