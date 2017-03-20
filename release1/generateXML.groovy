import groovy.xml.*

 println "generatexml loaded" 
  
def GenerateXML(String file) {
  def workflows = [
  [ name: 'A', file: 'fileA', objectName: 'wf_A', objectType: 'workflow', sourceRepository: 'DEV2', folderNames: [ multifolder: '{{multifolderTST}}', multifolder2: '{{multifolderTST2}}' ]],

  [ name: 'B',
  file: 'fileB',
  objectName: 'wf_B',
  objectType: 'workflow',
  sourceRepository: 'DEV2',
  folderNames: [ multifolder3: '{{multifolderTST3}}',
  multifolder4: '{{multifolderTST4}}' ]]
  ]

  def writer = new FileWriter(file)
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
  return file;
}
