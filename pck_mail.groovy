import groovy.xml.*

node('master') {

  deleteDir()

  stage('Checkout') {
    checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false,
    extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'release1'], [path: 'Parameters']]]], submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]]
  }

  /*
call the method to generate the manifest
  */

generateXML("deploy.xml")


  //stage for packaging



  //Stage to Publish


  //Stage to Deploy


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
  import groovy.xml.*

def generateXML() {

    println "Generating the manifest XML........"

    def workflows = [
    [ name: 'A', file: 'fileA', objectName: 'wf_A', objectType: 'workflow', sourceRepository: 'DEV2', folderNames: [ multifolder: '{{multifolderTST}}', multifolder2: '{{multifolderTST2}}' ]],

    [ name: 'B', file: 'fileB', objectName: 'wf_B', objectType: 'workflow', sourceRepository: 'DEV2', folderNames: [ multifolder3: '{{multifolderTST3}}', multifolder4: '{{multifolderTST4}}']]
    ]

   // new File('./deployit-manifest.xml').withWriter { writer ->
    def builder = new StreamingMarkupBuilder()
    builder.encoding = 'UTF-8'
    new File('file.xml').newWriter() << builder.bind {
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
