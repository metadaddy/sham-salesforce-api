package software.sham.salesforce.sforce

import software.sham.http.MockHttpsServer
import groovy.util.logging.Slf4j
import software.sham.salesforce.BaseBuilder

import static software.sham.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

@Slf4j
class MockGetUserInfoBuilder extends BaseBuilder {
    public MockGetUserInfoBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    public MockGetUserInfoResponse returnObject() {
        def response = new MockGetUserInfoResponse()
		def xml = slurpAndEditXmlResource('/template/get-user-info-response.xml') { root ->
			root.Body.getUserInfoResponse.result.userFullName = 'Thaddeus Rafacz'
			root.Body.getUserInfoResponse.result.userId = '005i0000000fWxdAAE'
			root.Body.getUserInfoResponse.result.userEmail = 'trafacz@confluex.com'
		}
		log.debug("in MockGetUserInfoResponse.returnObject, after slurpAndEditXml, xml = $xml")
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/'))
                .and(body(stringHasXPath('/Envelope/Body/getUserInfo')))
        )
            .withStatus(200)
            .withBody(xml)
        return response
    }
}
