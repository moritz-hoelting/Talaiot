package io.github.cdsap.talaiot.publisher.elasticsearch

import io.github.cdsap.talaiot.configuration.PublisherConfiguration

/**
 * Configuration for the [ElasticSearchPublisher]. It belongs to the Publisher configurations
 *
 * elasticSearchPublisher {
 *    url = "url"
 *    taskIndexName = "task"
 *    buildIndexName = "build"
 *    username = "elastic-user"
 *    password = "elastic-password"
 *    ignoreSslCertificates = true
 *    connectTimeout = 5000
 *    socketTimeout = 60000
 * }
 */
class ElasticSearchPublisherConfiguration : PublisherConfiguration, java.io.Serializable {

    /**
     * name of the publisher
     */
    override var name: String = "elasticSearch"

    override var publishBuildMetrics: Boolean = true
    override var publishTaskMetrics: Boolean = true

    /**
     * url from the Elasticsearch instance required to send the measurements. For instance http://localhost:9200
     */
    var url: String = ""

    /**
     * name to identify the task index measurement in Elasticsearch
     */
    var taskIndexName: String = "task"

    /**
     * name to identify the build index measurement in Elasticsearch
     */
    var buildIndexName: String = "build"

    /**
     * optional username for authentication
     */
    var username: String = ""

    /**
     * optional password for authorization
     */
    var password: String = ""

    /**
     * whether to check SSL certificates
     */
    var ignoreSslCertificates: Boolean = false

    /**
     * timeout for the connection in milliseconds (defaults to 1s)
     */
    val connectTimeout: Int? = null

    /**
     * timeout for the socket in milliseconds (defaults to 30s)
     */
    val socketTimeout: Int? = null
}
