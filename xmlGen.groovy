
import groovy.xml.*

//declare number of workflows to Loop
//def wf_count = $wf_count

def workflows = [[ name: 'A' , file: 'fileA' , [srcFolder1: "TgtFolder1", srcFolder2: "TgtFolder2"], objectName: 'wf_A' , objectType: 'workflow', sourceRepository: 'DEV2'],
[ name: 'B' , file: 'fileB' , [srcFolder4: "TgtFolder4", srcFolder3: "TgtFolder3"], objectName: 'wf_B' , objectType: 'workflow', sourceRepository: 'DEV2']]




StreamingMarkupBuilder builder = new StreamingMarkupBuilder()
builder.encoding = 'UTF-8'
String filterSoapMessage = builder.bind {
  mkp.xmlDeclaration()
  'udm.DeploymentPackage'(version:'$BUILD_NUMBER', application: "informaticaApp"){
    deployables {
      workflows.each { item ->
      'powercenter.PowercenterXml'(name:"item.name", file:"item.file"){
        scanPlaceholders{ mkp.yield(true) }
        sourceRepository{ mkp.yield("item.sourceRepository") }
        'folderNameMap' {
          entry( key:'multifolder', "{{multifolderTST}}" )
        }
        'objectNames' {
          value { mkp.yield('item.objectName') }
        }
        'objectTypes' {
          value {  mkp.yield('item.objectType') }
        }
      }
    }
  }
    dependencyResolution{ mkp.yield('LATEST') }
    undeployDependencies{ mkp.yield(false) }
  }
}

println XmlUtil.serialize(filterSoapMessage)
