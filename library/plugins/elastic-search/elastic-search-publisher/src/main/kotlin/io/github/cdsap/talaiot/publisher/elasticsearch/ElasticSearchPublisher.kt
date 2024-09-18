package io.github.cdsap.talaiot.publisher.elasticsearch

import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.logger.LogTracker
import io.github.cdsap.talaiot.metrics.DefaultBuildMetricsProvider
import io.github.cdsap.talaiot.metrics.DefaultTaskDataProvider
import io.github.cdsap.talaiot.publisher.Publisher
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import java.net.URL

class ElasticSearchPublisher(
    /**
     * General configuration for the publisher
     */
    private val elasticSearchPublisherConfiguration: ElasticSearchPublisherConfiguration,
    /**
     * LogTracker to print in console depending on the Mode
     */
    private val logTracker: LogTracker
) : Publisher, java.io.Serializable {

    private val tag = "ElasticSearchPublisher"

    override fun publish(report: ExecutionReport) {
        if (validate()) {
            val client = getClient()
            logTracker.log(tag, "================")
            logTracker.log(tag, "ElasticSearchPublisher")
            logTracker.log(
                tag,
                "publishBuildMetrics: ${elasticSearchPublisherConfiguration.publishBuildMetrics}"
            )
            logTracker.log(
                tag,
                "publishTaskMetrics: ${elasticSearchPublisherConfiguration.publishTaskMetrics}"
            )
            logTracker.log(tag, "================")

            try {
                if (elasticSearchPublisherConfiguration.publishBuildMetrics) {
                    logTracker.log(tag, "Sending Build metrics")
                    sendBuildMetrics(report, client)
                }
                if (elasticSearchPublisherConfiguration.publishTaskMetrics) {
                    logTracker.log(tag, "Sending Task metrics")
                    sendTasksMetrics(report, client)
                }
            } catch (e: Exception) {
                logTracker.error("ElasticSearchPublisher-Error-Executor Runnable: ${e.message}")
            }
        }
    }

    private fun validate(): Boolean {
        if (elasticSearchPublisherConfiguration.url.isEmpty() ||
            elasticSearchPublisherConfiguration.taskIndexName.isEmpty() ||
            elasticSearchPublisherConfiguration.buildIndexName.isEmpty()
        ) {
            logTracker.error(
                "ElasticSearchPublisher not executed. Configuration requires url, taskIndexName and buildIndexName: \n" +
                        "elasticSearchPublisher {\n" +
                        "            url = \"http://localhost:8086\"\n" +
                        "            buildIndexName = \"build\"\n" +
                        "            taskIndexName = \"task\"\n" +
                        "}\n" +
                        "Please update your configuration"
            )
            return false
        } else if (elasticSearchPublisherConfiguration.username.isEmpty() != elasticSearchPublisherConfiguration.password.isEmpty()) {
            logTracker.error(
                "ElasticSearchPublisher not executed. Configuration requires both username and password or none of them: \n" +
                        "elasticSearchPublisher {\n" +
                        "            url = \"http://localhost:8086\"\n" +
                        "            buildIndexName = \"build\"\n" +
                        "            taskIndexName = \"task\"\n" +
                        "            username = \"username\"\n" +
                        "            password = \"password\"\n" +
                        "}\n" +
                        "Please update your configuration"
            )
            return false
        }
        return true
    }

    private fun sendBuildMetrics(report: ExecutionReport, client: RestHighLevelClient) {
        val metrics = DefaultBuildMetricsProvider(report).get()
        val response = client.index(
            IndexRequest(elasticSearchPublisherConfiguration.buildIndexName).source(metrics),
            RequestOptions.DEFAULT
        )
        logTracker.log(tag, "Result Build metrics $response")
    }

    private fun sendTasksMetrics(
        report: ExecutionReport,
        client: RestHighLevelClient
    ) {
        logTracker.log(tag, "number of tasks report.tasks " + report.tasks?.size)
        report.tasks?.forEach {
            try {
                val response = client.index(
                    IndexRequest(elasticSearchPublisherConfiguration.taskIndexName)
                        .source(DefaultTaskDataProvider(it, report).get()),
                    RequestOptions.DEFAULT
                )
                logTracker.log(tag, "Result Task metrics $response")
            } catch (e: java.lang.Exception) {
                logTracker.error(e.message.toString())
            }
        }
    }

    /**
     * Get the RestHighLevelClient with the ElasticSearchPublisherConfiguration of this object applied
     */
    private fun getClient(): RestHighLevelClient {
        val clientBuilder = if (elasticSearchPublisherConfiguration.url == "localhost") {
            RestClient.builder(HttpHost("localhost"))
        } else {
            val url = URL(elasticSearchPublisherConfiguration.url)

            val restClientBuilder =
                RestClient.builder(
                    HttpHost(
                        url.host,
                        url.port,
                        url.protocol
                    )
                )
            restClientBuilder
        }
        if (elasticSearchPublisherConfiguration.username.isNotEmpty() && elasticSearchPublisherConfiguration.password.isNotEmpty()) {
            val credentialsProvider = BasicCredentialsProvider()
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                UsernamePasswordCredentials(
                    elasticSearchPublisherConfiguration.username,
                    elasticSearchPublisherConfiguration.password
                )
            )
            clientBuilder.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(
                    credentialsProvider
                )
            }
        }
        if (elasticSearchPublisherConfiguration.ignoreSslCertificates) {
            clientBuilder.setHttpClientConfigCallback { httpClientBuilder -> httpClientBuilder.setSSLHostnameVerifier { _, _ -> true } }
        }
        if (elasticSearchPublisherConfiguration.connectTimeout != null || elasticSearchPublisherConfiguration.socketTimeout != null) {
            clientBuilder.setRequestConfigCallback { requestConfigCallback ->
                val connectTimeout = elasticSearchPublisherConfiguration.connectTimeout
                if (connectTimeout != null) {
                    requestConfigCallback.setConnectTimeout(connectTimeout)
                }
                val socketTimeout = elasticSearchPublisherConfiguration.socketTimeout
                if (socketTimeout != null) {
                    requestConfigCallback.setSocketTimeout(socketTimeout)
                }
                requestConfigCallback
            }
        }
        return RestHighLevelClient(clientBuilder)
    }
}
