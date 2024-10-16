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
import java.util.concurrent.Executor

class ElasticSearchPublisher(
    /**
     * General configuration for the publisher
     */
    private val elasticSearchPublisherConfiguration: ElasticSearchPublisherConfiguration,
    /**
     * LogTracker to print in console depending on the Mode
     */
    private val logTracker: LogTracker,
    /**
     * Executor to schedule a task in Background
     */
    private val executor: Executor
) : Publisher {

    private val TAG = "ElasticSearchPublisher"

    override fun publish(report: ExecutionReport) {

        if (validate()) {
            val client = getClient()
            executor.execute {
                logTracker.log(TAG, "================")
                logTracker.log(TAG, "ElasticSearchPublisher")
                logTracker.log(
                    TAG,
                    "publishBuildMetrics: ${elasticSearchPublisherConfiguration.publishBuildMetrics}"
                )
                logTracker.log(
                    TAG,
                    "publishTaskMetrics: ${elasticSearchPublisherConfiguration.publishTaskMetrics}"
                )
                logTracker.log(TAG, "================")

                try {
                    if (elasticSearchPublisherConfiguration.publishBuildMetrics) {
                        logTracker.log(TAG, "Sending Build metrics")
                        sendBuildMetrics(report, client)
                    }
                    if (elasticSearchPublisherConfiguration.publishTaskMetrics) {
                        logTracker.log(TAG, "Sending Task metrics")
                        sendTasksMetrics(report, client)
                    }
                } catch (e: Exception) {
                    logTracker.error("ElasticSearchPublisher-Error-Executor Runnable: ${e.message}")
                }
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
        logTracker.log(TAG, "Result Build metrics $response")
    }

    private fun sendTasksMetrics(
        report: ExecutionReport,
        client: RestHighLevelClient
    ) {
        logTracker.log(TAG, "number of tasks report.tasks " + report.tasks?.size)
        report.tasks?.forEach {
            try {
                val response = client.index(
                    IndexRequest(elasticSearchPublisherConfiguration.taskIndexName)
                        .source(DefaultTaskDataProvider(it, report).get()),
                    RequestOptions.DEFAULT
                )
                logTracker.log(TAG, "Result Task metrics $response")
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

            RestClient.builder(
                HttpHost(
                    url.host,
                    url.port,
                    url.protocol
                )
            )
        }

        val defaultCredentialsProvider = if (elasticSearchPublisherConfiguration.username.isNotEmpty() && elasticSearchPublisherConfiguration.password.isNotEmpty()) {
            val credentialsProvider = BasicCredentialsProvider()
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                UsernamePasswordCredentials(elasticSearchPublisherConfiguration.username, elasticSearchPublisherConfiguration.password)
            )
            credentialsProvider
        } else {
            null
        }

        if (defaultCredentialsProvider != null || elasticSearchPublisherConfiguration.ignoreSslCertificates) {
            clientBuilder.setHttpClientConfigCallback { httpClientBuilder ->
                val httpClientBuilder2 = if (defaultCredentialsProvider != null) {
                    httpClientBuilder.setDefaultCredentialsProvider(defaultCredentialsProvider)
                } else {
                    httpClientBuilder
                }

                if (elasticSearchPublisherConfiguration.ignoreSslCertificates) {
                    httpClientBuilder2.setSSLHostnameVerifier( { _, _ -> true } )
                } else {
                    httpClientBuilder2
                }
            }
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
