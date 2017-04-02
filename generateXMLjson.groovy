import groovy.xml.*
import groovy.json.JsonSlurper

def GenerateXML(List workflows) {

  def jsonSlurper = new JsonSlurper();
  def fileReader = new BufferedReader(
                  new FileReader("/home/okram/workspace/objectsRepo/objects.json"))

  def parsedData = jsonSlurper.parse(fileReader)


  def writer = new FileWriter("sampleManifest.XML")
  def builder = new StreamingMarkupBuilder()
  builder.encoding = 'UTF-8'
  writer << builder.bind {
    mkp.xmlDeclaration()
    mkp.declareNamespace(udm :'http://www.w3.org/2001/XMLSchema')
    mkp.declareNamespace(powercenter:'http://www.w3.org/2001/XMLSchema')
    delegate.udm.DeploymentPackage(version:'$BUILD_NUMBER', application: "informaticaApp"){
      delegate.deployables {
    //    workflows.each { item ->
     parsedData.each { index, obj ->
          delegate.powercenter.PowercenterXml(name:obj.name, file:obj.file) {
            delegate.scanPlaceholders(true)
            delegate.sourceRepository(obj.sourceRepository)
            delegate.folderNameMap {
        //      item.folderNames.each { name, value ->
            obj.folderNames.each { name, value ->
              //  it.entry(key:name, value)
                "${name}, ${value}"
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
//  return file;
}
