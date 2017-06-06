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

    def fileReader2 = new BufferedReader(
      new FileReader("${currentws}/param.json"))  //the file location need to change in the actual implementation
      def parsedData2 = jsonSlurper.parse(fileReader2)

    def fileReader3 = new BufferedReader(
      new FileReader("${currentws}/misc.json"))  //the file location need to change in the actual implementation
      def parsedData3 = jsonSlurper.parse(fileReader3)


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
          parsedData2.each { index2, obj2 ->
            it."powercenter.PowercenterParamFile"(name:obj2.name, file:obj2.file) {
                delegate.scanPlaceholders(true)
                delegate.functionality(obj2.functionality)
                delegate.targetFile(obj2.targetFile)
                delegate.preserveExistingFiles(true)
            }
          }
         parsedData3.each { index3, obj3 ->
            it."powercenter.PowercenterMiscFile"(name:obj3.name, file:obj3.file) {
                 delegate.scanPlaceholders(true)
                 delegate.textFileNamesRegex(obj3.textFileNamesRegex)
                 delegate.functionality(obj3.functionality)
                 delegate.targetFile(obj3.targetFile)
                 delegate.filePermissions(obj3.filePermissions)
                 delegate.preserveExistingFiles(true)
            }
         }
        }
        delegate.dependencyResolution('LATEST')
        delegate.undeployDependencies(false)
      }
    }
  }
