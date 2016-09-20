package software.sham.salesforce.integration

import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import software.sham.salesforce.util.SfdcStreamingClient

@Slf4j @Ignore
class StreamingApi {
    Properties config

    @Before
    void initConfig() {
        config = new Properties()
        config.load(new ClassPathResource('config-local.properties').inputStream)
    }

    @Test
    void doIt() {
        def client = new SfdcStreamingClient(config['sfdc.username'], config['sfdc.password'], config['sfdc.securityToken'], config['sfdc.url'])
        client.login()
        assert null != client.sessionId && client.sessionId.length() > 0
        client.subscribeTopic('RyanContactUpdates') { message ->
            log.info "Whoopee a message! $message"
        }
        Thread.sleep(300000)
    }

}
