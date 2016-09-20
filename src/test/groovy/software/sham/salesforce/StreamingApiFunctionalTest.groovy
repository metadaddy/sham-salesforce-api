package software.sham.salesforce

import software.sham.http.MockHttpsServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import software.sham.salesforce.util.SfdcStreamingClient

import javax.net.ssl.HttpsURLConnection

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

        client.subscribeTopic('SampleTopic') { message ->
            received = message
        }

        assert null == received
        Thread.sleep 500

        server.streamingApi().publish('SomeOtherTopic', [id: '00321'])
        assert null == received

        Thread.sleep 500
        server.streamingApi().publish('SampleTopic', [id: '00123'])
        Thread.sleep 500
        assert null != received
    }
}
