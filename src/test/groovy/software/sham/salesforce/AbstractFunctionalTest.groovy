package software.sham.salesforce

import software.sham.http.MockHttpsServer
import org.junit.After
import org.junit.Before

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class AbstractFunctionalTest {
    protected MockSalesforceApiServer server
    protected Client sslClient
    protected XPath evaluator
    protected DocumentBuilder builder

    @Before
    void initServer() {
        server = new MockSalesforceApiServer(8081)
    }

    @After
    void stopServer() {
        server.stop()
    }

    @Before
    void initClient() {
        sslClient = ClientBuilder.newBuilder()
                .sslContext(MockHttpsServer.clientSslContext)
                .build()
    }

    @Before
    void initXml() {
        evaluator = XPathFactory.newInstance().newXPath()
        evaluator.setNamespaceContext([
                getNamespaceURI: { prefix ->
                    BaseBuilder.NAMESPACES[prefix]
                }
        ] as NamespaceContext)

        def factory = DocumentBuilderFactory.newInstance()
        factory.namespaceAware = true
        builder = factory.newDocumentBuilder()
        assert builder.namespaceAware
    }

    def evalXpath(String xpath, String xml) {
        evalXpath(xpath, xml, XPathConstants.STRING)
    }

    def evalXpath(String xpath, String xml, QName resultType) {
        def rootElement = builder.parse(new ByteArrayInputStream(xml.bytes)).documentElement
        evaluator.evaluate(xpath, rootElement, resultType)
    }
}
