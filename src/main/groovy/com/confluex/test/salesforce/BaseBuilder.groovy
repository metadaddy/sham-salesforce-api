package com.confluex.test.salesforce

import com.confluex.mock.http.MockHttpsServer
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.ClassPathResource

class BaseBuilder {
    static final Map<String, String> NAMESPACES = [
            env: 'http://schemas.xmlsoap.org/soap/envelope/',
            xsd: 'http://www.w3.org/2001/XMLSchema',
            xsi: 'http://www.w3.org/2001/XMLSchema-instance',
            sf: 'urn:partner.soap.sforce.com',
            so: 'urn:sobject.partner.soap.sforce.com',
            sd: 'http://www.force.com/2009/06/asyncapi/dataload'
    ]

    MockHttpsServer mockHttpsServer

    BaseBuilder(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
    }

    String slurpAndEditXmlResource(String path, Closure editClosure) {
        slurpAndEditXml(new ClassPathResource(path).inputStream, editClosure)
    }

    String slurpAndEditXml(String xml, Closure editClosure) {
        slurpAndEditXml(new ByteArrayInputStream(xml.bytes), editClosure)
    }

    String slurpAndEditXml(InputStream xml, Closure editClosure) {
        def root = new XmlSlurper().parse(xml)
        editClosure(root)
        buildXml {
            mkp.yield root
        }
    }

   String buildXml(Closure editClosure) {
       new StreamingMarkupBuilder().bind {
           NAMESPACES.each {
               mkp.declareNamespace it.key, it.value
           }
           mkp.yield editClosure
       }
   }
}