import groovy.xml.*

StreamingMarkupBuilder builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'
        String filterSoapMessage = builder.bind {
            mkp.xmlDeclaration()
            'udm.DeploymentPackage'(version:'$BUILD_NUMBER', application: "informaticaApp"){
             deployables {
               'powercenter.PowercenterXml'(name:"/wf_multifolder", file:"wf_multifolder.XML"){
                 scanPlaceholders{ mkp.yield(true) }
                 sourceRepository{ mkp.yield("DEV2") }
                 folderNameMap{ mkp.yield("this will a map") }
                 'objectNames' {
                   value { mkp.yield('wf_multifolder.XML') }
                 }
                 'objectTypes' {
                   value {  mkp.yield('workflow') }
                 }
                }
             }

            dependencyResolution{ mkp.yield('LATEST') }
            undeployDependencies{ mkp.yield(false) }
            }
        }

 println XmlUtil.serialize(filterSoapMessage)
