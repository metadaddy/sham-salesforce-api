package software.sham.salesforce.sforce

import software.sham.http.ClientRequest
import software.sham.http.MockHttpsServer
import groovy.util.logging.Slf4j
import software.sham.salesforce.BaseBuilder

import static software.sham.http.matchers.HttpMatchers.*
import static org.hamcrest.Matchers.*

@Slf4j
class MockRetrieveBuilder extends BaseBuilder {

    public MockRetrieveBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    public MockRetrieveResponse returnObject() {
        def response = new MockRetrieveResponse()
        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/'))
                .and(body(stringHasXPath('/Envelope/Body/retrieve')))
        )
            .withStatus(200)
            .withBody() { ClientRequest request ->
                def requestDoc = new XmlSlurper().parseText(request.body)
                def ids = requestDoc.Body.retrieve.ids.text().split(',').toList()
                def fields = requestDoc.Body.retrieve.fieldList.text().split(',').toList().minus('Id')
                buildSoapEnvelope {
                    'env:Body' {
                        'sf:retrieveResponse' {
                            ids.each { id ->
                                'sf:result' {
                                    'so:type'(requestDoc.Body.retrieve.sObjectType.text())
                                    'so:Id'(id)
                                    response.buildFieldElements(mkp, fields)
                                }
                            }
                        }
                    }
                }
            }
        return response
    }
}
