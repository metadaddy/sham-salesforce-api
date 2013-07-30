package com.confluex.test.salesforce

import com.confluex.mule.test.http.MockHttpsServer
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.config.ClientConfig
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.HTTPSProperties
import groovy.xml.StreamingMarkupBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource

import javax.ws.rs.core.MediaType
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

class LoginFunctionalTest {

    MockSalesforceApiServer server
    Client sslClient

    @Before
    void initServer() {
        server = new MockSalesforceApiServer(8090)
    }

    @After
    void stopServer() {
        server.stop()
    }

    @Before
    void initClient() {
        ClientConfig config = new DefaultClientConfig()
        config.properties.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, MockHttpsServer.clientSslContext))
        sslClient = Client.create(config)
    }

    @Test
    void shouldAcceptAnyLoginCredentialsByDefault() {
        def root = new XmlSlurper().parse(new ClassPathResource('login-request.xml').inputStream)
        root.Body.login.username = 'trafacz@confluex.com.prospectmtg.dev'
        root.Body.login.password = 'wioska123qH3KrpON7OsB6MKOTdPWv711T'
        String request = new StreamingMarkupBuilder().bind { mkp.yield root }

        ClientResponse response = sslClient.resource('https://localhost:8090/services/Soap/u/28.0')
            .header('SOAPAction', 'login')
            .entity(request, MediaType.TEXT_XML_TYPE)
            .post(ClientResponse.class)

        def responseBody = response.getEntity(String)
        println responseBody
        assert response.status == 200
        assert MockSalesforceApiServer.DEFAULT_USER_ID == evalXpath('/Envelope/Body/loginResponse/result/userId', responseBody)
        assert MockSalesforceApiServer.DEFAULT_USER_ID == evalXpath('/Envelope/Body/loginResponse/result/userInfo/userId', responseBody)
        assert MockSalesforceApiServer.DEFAULT_ORG_ID + 'MAC' == evalXpath('/Envelope/Body/loginResponse/result/userInfo/organizationId', responseBody)
        assert MockSalesforceApiServer.DEFAULT_SESSION_ID == evalXpath('/Envelope/Body/loginResponse/result/sessionId', responseBody)

        URL metadataUrl = new URL(evalXpath('/Envelope/Body/loginResponse/result/metadataServerUrl', responseBody))
        assert 'localhost' == metadataUrl.host
        assert 8090 == metadataUrl.port
        assert metadataUrl.path ==~ /${MockSalesforceApiServer.METADATA_PATH_PREFIX}.*/
        assert metadataUrl.path ==~ /.*${MockSalesforceApiServer.DEFAULT_ORG_ID}/

        URL serverUrl = new URL(evalXpath('/Envelope/Body/loginResponse/result/serverUrl', responseBody))
        assert 'localhost' == serverUrl.host
        assert 8090 == serverUrl.port
        assert serverUrl.path ==~ /${MockSalesforceApiServer.PATH_PREFIX}.*/
        assert serverUrl.path ==~ /.*${MockSalesforceApiServer.DEFAULT_ORG_ID}/
    }

    def evalXpath(String xpath, String xml) {
        def evaluator = XPathFactory.newInstance().newXPath()
        def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        def rootElement = builder.parse(new ByteArrayInputStream(xml.bytes)).documentElement

        evaluator.evaluate(xpath, rootElement)
    }
}