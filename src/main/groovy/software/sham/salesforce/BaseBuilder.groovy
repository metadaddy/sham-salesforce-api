package software.sham.salesforce

import java.io.InputStream;

import software.sham.http.MockHttpsServer

import groovy.json.JsonSlurper
import groovy.lang.Closure;
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder

import org.springframework.core.io.ClassPathResource

import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

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
    def evaluator = XPathFactory.newInstance().newXPath()
    def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

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

    String buildSoapEnvelope(Closure editClosure) {
        buildXml {
            'env:Envelope'(NAMESPACES.collectEntries {key, value -> ["xmlns:$key", value]}) {
                mkp.yield editClosure
            }
        }
    }
	
	String slurpJson(String path) {
		String json = new ClassPathResource(path).inputStream.getText()
		// def root = new JsonSlurper().parseText(json)
		return json
	}

    def evalXpath(String xpath, String xml, QName resultType) {
        def rootElement = builder.parse(new ByteArrayInputStream(xml.bytes)).documentElement
        evaluator.evaluate(xpath, rootElement, resultType)
    }
}
