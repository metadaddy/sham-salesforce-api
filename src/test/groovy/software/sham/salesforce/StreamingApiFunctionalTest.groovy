package software.sham.salesforce

import groovy.util.logging.Slf4j
import software.sham.http.MockHttpsServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import software.sham.salesforce.util.SfdcStreamingClient

import javax.net.ssl.HttpsURLConnection

@Slf4j
class StreamingApiFunctionalTest extends AbstractFunctionalTest {
    SfdcStreamingClient client

    @Before
    void trustMockHttpsCertificateForSsl() {
        HttpsURLConnection.setDefaultSSLSocketFactory(MockHttpsServer.clientSslContext.socketFactory)
    }

    @Before
    void initClient() {
        client = new SfdcStreamingClient('', '', '', "https://localhost:${server.port}/services/Soap/u/28.0")
        client.login()
        assert null != client.sessionId && client.sessionId.length() > 0
    }

    @After
    void stopClient() {
        client.stop()
        Thread.sleep 500
    }

    @Test
    void shouldPublishEventsToTopics() {
        def received = null

        log.debug("Subscribing to SampleTopic")
        client.subscribeTopic('SampleTopic') { message ->
            log.debug("Received message on SampleTopic")
            received = message
        }

        assert null == received
        Thread.sleep 500

        log.debug("Publishing message to SomeOtherTopic")
        server.streamingApi().publish('SomeOtherTopic', [id: '00321'])
        assert null == received

        Thread.sleep 500
        log.debug("Publishing message to SampleTopic")
        server.streamingApi().publish('SampleTopic', [id: '00123'])
        Thread.sleep 500
        assert null != received
    }
}
