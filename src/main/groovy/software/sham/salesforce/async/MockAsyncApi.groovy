package software.sham.salesforce.async

import software.sham.http.MockHttpsServer
import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.ClassPathResource

import static software.sham.http.matchers.HttpMatchers.matchesPattern
import static software.sham.http.matchers.HttpMatchers.path
import static software.sham.http.matchers.HttpMatchers.path


class MockAsyncApi {
    MockHttpsServer mockHttpsServer
    String version
    LinkedHashMap<String, LinkedHashMap<String, String>> records = new LinkedHashMap<>()

    MockAsyncApi(MockHttpsServer mockHttpsServer) {
        this.mockHttpsServer = mockHttpsServer
        defaults()
    }

    void defaults() {
        // Do everything here for now
        //batchResult().respondSuccess()

        // Create job
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_jobs_create.htm
        def jobPathPattern = '/services/async/(\\d+.\\d)/job/'
        mockHttpsServer.respondTo(path(matchesPattern(jobPathPattern))).withStatus(201).withBody { request ->
            def requestXml = new XmlSlurper().parseText(request.body)
            def version = (request.path =~ jobPathPattern)[0][1]
            slurpAndEditXml('/template/create-job-response.xml') { root ->
                def operation = requestXml.operation.text()
                // return "00MOCK0000query" etc so we know what kind of batch result to return
                root.id = '00MOCK' + operation.padLeft(9, '0')
                root.operation = operation
                root.object = requestXml.object.text()
                root.externalIdFieldName = requestXml.externalIdFieldName.text()
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
                root.apiVersion = version
            }
        }

        // Monitor/change job state - e.g. abort/close job
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_jobs_close.htm
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_jobs_get_details.htm
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_jobs_abort.htm
        def jobInfoPathPattern = '/services/async/(\\d+.\\d)/job/(\\w+)'
        mockHttpsServer.respondTo(path(matchesPattern(jobInfoPathPattern))).withStatus(200).withBody { request ->
            def requestXml = new XmlSlurper().parseText(request.body)
            def version = (request.path =~ jobInfoPathPattern)[0][1]
            def jobId = (request.path =~ jobInfoPathPattern)[0][2]
            slurpAndEditXml('/template/create-job-response.xml') { root ->
                root.id = jobId
                root.operation = 'query'
                root.object = 'Account'
                root.state = requestXml.state.text()
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
                root.apiVersion = version
            }
        }

        // Add a batch to a job
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_batches_create.htm
        def batchPathPattern = '/services/async/\\d+.\\d/job/(\\w+)/batch'
        mockHttpsServer.respondTo(path(matchesPattern(batchPathPattern))).withStatus(201).withBody { request ->
            def jobId = (request.path =~ batchPathPattern)[0][1]
            slurpAndEditXml('/template/create-batch-response.xml') { root ->
                root.id = '00MOCK0000batch'
                root.jobId = jobId
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
            }
        }

        // Get batch info
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_batches_get_info.htm
        def batchInfoPathPattern = '/services/async/\\d+.\\d/job/(\\w+)/batch/(\\w+)'
        mockHttpsServer.respondTo(path(matchesPattern(batchInfoPathPattern))).withStatus(200).withBody { request ->
            def matches = (request.path =~ batchInfoPathPattern)
            def jobId = matches[0][1]
            def batchId = matches[0][2]
            slurpAndEditXml('/template/batch-info-response.xml') { root ->
                root.id = batchId
                root.jobId = jobId
                root.createdDate = formatDate(new Date())
                root.systemModstamp = formatDate(new Date())
            }
        }

        // Get batch results
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_using_bulk_query.htm
        def batchResultPathPattern = '/services/async/\\d+.\\d/job/(\\w+)/batch/(\\w+)/result'
        mockHttpsServer.respondTo(path(matchesPattern(batchResultPathPattern))).withStatus(200).withBody { request ->
            def matches = (request.path =~ batchResultPathPattern)
            def jobId = matches[0][1]
            def batchId = matches[0][2]
            def response
            if (jobId.endsWith('query')) {
                response = slurpAndEditXml('/template/batch-result-list-response.xml') { root ->
                    root.result = '00MOCK000result'
                }
            } else {
                // Default copied from MockBatchResultBuilder
                response = slurpAndEditXml('/template/batch-result-response.xml') { root ->
                    root.result.id = batchId
                    root.result.success = 'true'
                }
            }
        }

        // Get bulk query result
        // https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/asynch_api_using_bulk_query.htm
        def bulkQueryResultsPathPattern = '/services/async/\\d+.\\d/job/\\w+/batch/\\w+/result/\\w+'
        mockHttpsServer.respondTo(path(matchesPattern(bulkQueryResultsPathPattern)))
            .withStatus(200)
            .withHeader("Content-Type", "text/csv").withBody {
            String content = "\"Id\",\"Name\"\n"
            records.each { id, fields ->
                content += "\"" + id + "\",\"" + fields.get("Name") + "\"\n"
            }
            content
        }
    }

    MockBatchResultBuilder batchResult() {
        new MockBatchResultBuilder(mockHttpsServer)
    }


    String slurpAndEditXml(String path, Closure editClosure) {
        slurpAndEditXml(new ClassPathResource(path).inputStream, editClosure)
    }

    String slurpAndEditXml(InputStream xml, Closure editClosure) {
        def root = new XmlSlurper().parse(xml)
        editClosure(root)
        new StreamingMarkupBuilder().bind {
            mkp.declareNamespace("": root.namespaceURI())
            mkp.yield root
        }
    }

    String formatDate(Date date) {
        date.format("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    void clear() {
        this.records.clear()
    }

    void insert(LinkedHashMap<String, LinkedHashMap<String, String>> records) {
        this.records.putAll(records)
    }
}
