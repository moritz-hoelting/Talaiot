package io.github.cdsap.talaiot.publisher.elasticsearch

import io.github.cdsap.talaiot.entities.ExecutionReport
import io.github.cdsap.talaiot.logger.LogTracker
import io.github.cdsap.talaiot.metrics.DefaultBuildMetricsProvider
import io.github.cdsap.talaiot.metrics.DefaultTaskDataProvider
import io.github.cdsap.talaiot.publisher.Publisher
import org.apache.http.HttpHost
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

    private val TAG = "ElasticSearchPublisher"

    override fun publish(report: ExecutionReport) {
        if (validate()) {
            val client = getClient()
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
            applyAuthorization(RequestOptions.DEFAULT)

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
                    applyAuthorization(RequestOptions.DEFAULT)
                )
                logTracker.log(TAG, "Result Task metrics $response")
            } catch (e: java.lang.Exception) {
                logTracker.error(e.message.toString())
            }
        }
    }

    private fun getClient(): RestHighLevelClient {
        return if (elasticSearchPublisherConfiguration.url == "localhost") {
            RestHighLevelClient(RestClient.builder(HttpHost("localhost")))
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
            RestHighLevelClient(restClientBuilder)
        }
    }

    /**
     * Apply Authorization header to the request
     */
    private fun applyAuthorization(options: RequestOptions): RequestOptions {
        val credentials = getBase64Auth()
        if (credentials != null) {
            val optionsBuilder = options.toBuilder()
            optionsBuilder.addHeader("Authorization", "Basic $credentials")
            return optionsBuilder.build()
        } else {
            return options
        }
    }

    /**
     * Get the base64 encoded credentials
     */
    private fun getBase64Auth(): String? {
        if (elasticSearchPublisherConfiguration.username.isEmpty() || elasticSearchPublisherConfiguration.password.isEmpty()) {
            return null
        } else {
            val credentials = "${elasticSearchPublisherConfiguration.username}:${elasticSearchPublisherConfiguration.password}"
            return java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
        }
    }
}
