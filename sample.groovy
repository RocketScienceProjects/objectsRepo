import groovy.xml.*
import groovy.json.JsonSlurper

node('linux') {
  deleteDir()

  stage('Checkout') {
    checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]])
  }

  env.GIT_TAG_NAME = gitTagName()
  env.GIT_TAG_MESSAGE = gitTagMessage()

  // GenerateXML()
  // println "Generated the manifest XML"
  //readFile encoding: 'UTF-8', file: 'a.txt'
  def fR = readFile encoding: 'UTF-8', file: 'objects.json'
  def fR2 = readFile encoding: 'UTF-8', file: 'param.json'
  def fR3 = readFile encoding: 'UTF-8', file: 'misc.json'

  //parsed json obj


  writeFile file: "zing.XML", text: GenerateXML(fR,fR2)

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
def GenerateXML(Object fileReader, Object fileReader2) {
  def jsonSlurper = new JsonSlurper();
  def parsedData = jsonSlurper.parseText(fileReader)
  def parsedData2 = jsonSlurper.parseText(fileReader2)
  //def parsedData3 = jsonSlurper.parseText(fileReader3)

  def builder = new StreamingMarkupBuilder()
  builder.encoding = 'UTF-8'
  def xml = builder.bind {
    mkp.xmlDeclaration()
    mkp.declareNamespace('udm.DeploymentPackage' :'http://www.w3.org/2001/XMLSchema')
    mkp.declareNamespace('powercenter.PowercenterXml' :'http://www.w3.org/2001/XMLSchema')
    delegate."udm.DeploymentPackage"(version:'$BUILD_NUMBER', application: "informaticaApp"){
      delegate.deployables {
        for (int i = 0; i < parsedData.workflows.size(); i++) {
          it."powercenter.PowercenterXml"(name:parsedData.workflows[i].name, file:parsedData.workflows[i].file) {
            delegate.scanPlaceholders(true)
            delegate.sourceRepository(parsedData.workflows[i].sourceRepository)
            delegate.folderNameMap {
              for (int j = 0; j < parsedData.workflows[i].folderNames.size(); j++ ) {
                it.entry(key:parsedData.workflows[i].folderNames[j].key, parsedData.workflows[i].folderNames[j].value)
              }
            }
            delegate.objectNames {
              delegate.value(parsedData.workflows[i].objectName)
            }
            delegate.objectTypes {
              delegate.value(parsedData.workflows[i].objectType)
            }
          }
        }
        for (int a = 0; a < parsedData2.paramFiles.size(); a++) {
          it."powercenter.PowercenterParamFile"(name:parsedData2.paramFiles[a].name, file:parsedData2.paramFiles[a].file) {
            delegate.scanPlaceholders(true)
            delegate.functionality(parsedData2.paramFiles[a].functionality)
            delegate.targetFile(parsedData2.paramFiles[a].targetFile)
            delegate.preserveExistingFiles(true)
          }
        }
      }

    delegate.dependencyResolution('LATEST')
    delegate.undeployDependencies(false)
    }
  }
  return xml.toString()
}
