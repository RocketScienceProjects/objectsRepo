
/*
This will generate the manifest xml
*/

import groovy.xml.*

def workflows = [
    [ name: 'A', file: 'fileA', objectName: 'wf_A', objectType: 'workflow', sourceRepository: 'DEV2', folderNames: [ multifolder: '{{multifolderTST}}',
                     multifolder2: '{{multifolderTST2}}' ]],
    [ name: 'B',
      file: 'fileB',
      objectName: 'wf_B',
      objectType: 'workflow',
      sourceRepository: 'DEV2',
      folderNames: [ multifolder3: '{{multifolderTST3}}',
                     multifolder4: '{{multifolderTST4}}' ]]
]

def writer = new FileWriter('/tmp/file.xml')
def builder = new StreamingMarkupBuilder()
builder.encoding = 'UTF-8'
writer << builder.bind {
  mkp.xmlDeclaration()
  'udm.DeploymentPackage'(version:'$BUILD_NUMBER', application: "informaticaApp"){
    deployables {
      workflows.each { item ->
        'powercenter.PowercenterXml'(name:item.name, file:item.file) {
          scanPlaceholders(true)
          sourceRepository(item.sourceRepository)
          folderNameMap {
            item.folderNames.each { name, value ->
              entry(key:name, value)
            }
          }
          objectNames {
            value(item.objectName)
          }
          objectTypes {
            value(item.objectType)
          }
        }
      }
    }
    dependencyResolution('LATEST')
    undeployDependencies(false)
  }
}
