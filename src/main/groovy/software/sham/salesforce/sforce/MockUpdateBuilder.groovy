package software.sham.salesforce.sforce

import software.sham.http.ClientRequest
import software.sham.http.MockHttpsServer
import software.sham.salesforce.BaseBuilder

import javax.xml.xpath.XPathConstants

import static software.sham.http.matchers.HttpMatchers.body
import static software.sham.http.matchers.HttpMatchers.path
import static software.sham.http.matchers.HttpMatchers.stringHasXPath
import static org.hamcrest.Matchers.startsWith

class MockUpdateBuilder extends BaseBuilder {
    MockUpdateBuilder(MockHttpsServer mockHttpsServer) {
        super(mockHttpsServer)
    }

    SforceObjectRequest capture(ClientRequest httpRequest) {
        def request = new SforceObjectRequest( httpRequest.body )
        evalXpath('/Envelope/Body/update/sObjects/*', httpRequest.body, XPathConstants.NODESET).each {
            request.fields[it.name.split(':')[1] ?: it] = it.textContent
        }
        request
    }

    MockUpdateResponse returnSuccess() {
        def response = new MockUpdateResponse()

        mockHttpsServer.respondTo(path(startsWith('/services/Soap/u/'))
                .and(body(stringHasXPath('/Envelope/Body/update')))
        )
                .withStatus(200)
                .withBody() { ClientRequest request ->
                    def requestDoc = new XmlSlurper().parseText(request.body)
                    def id = requestDoc.Body.update.sObjects.Id.text()
                    buildSoapEnvelope {
                        'env:Body' {
                            'sf:updateResponse' {
                                'sf:result' {
                                    'sf:id'(id)
                                    'sf:success'('true')
                                }
                            }
                        }
                    }
                }
        return response
    }
}
