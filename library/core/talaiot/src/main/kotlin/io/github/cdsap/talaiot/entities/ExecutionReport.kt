package io.github.cdsap.talaiot.entities

/**
 * Since the user of the plugin might customize metrics provider we have to make almost everything here optional
 *
 * @property beginMs timestamp of gradle execution start
 * @property endMs timestamp of gradle execution finish
 * @property durationMs duration in millis of gradle execution
 * @property configurationDurationMs duration of the configuration phase
 *
 * @property tasks list of executed tasks filtered according to [io.github.cdsap.talaiot.configuration.FilterConfiguration]
 * @property unfilteredTasks list of executed tasks
 * @property requestedTasks gradle tasks requested by user, e.g. "assemble test"
 *
 * @property buildId unique build identifier generated by Talaiot
 * @property buildInvocationId unique build identifier generated by Gradle's [org.gradle.internal.scan.scopeids.BuildScanScopeIds]. It has the same value for multi-stage builds, e.g. wrapper invocation and actual tasks requested. For more info see comments at [io.github.cdsap.talaiot.metrics.GradleBuildInvocationIdMetric]
 *
 * @property rootProject name of the root gradle project
 * @property success true if build finished successfully, false otherwise
 *
 * @property scanLink link to the generated gradle scan
 *
 * @property environment information about the environment of gradle execution
 * @property customProperties custom properties defined in [io.github.cdsap.talaiot.configuration.MetricsConfiguration]
 */
data class ExecutionReport(
    var environment: Environment = Environment(),
    var customProperties: CustomProperties = CustomProperties(),
    var beginMs: String? = null,
    var endMs: String? = null,
    var durationMs: String? = null,
    var configurationDurationMs: String? = null,
    var executionDurationMs: String? = null,
    var tasks: List<TaskLength>? = null,
    var unfilteredTasks: List<TaskLength>? = null,
    var buildId: String? = null,
    var rootProject: String? = null,
    var requestedTasks: String? = null,
    var success: Boolean = false,
    var scanLink: String? = null,
    var buildInvocationId: String? = null,
    var configurationCacheHit: Boolean = false
) : java.io.Serializable {

    /**
     * Cache ratio of the tasks = tasks_from_cache / all_tasks
     */
    val cacheRatio: String?
        get() = unfilteredTasks?.let {
            it.count { taskLength -> taskLength.state == TaskMessageState.FROM_CACHE } / it.size.toDouble()
        }?.toString()
}

data class Environment(
    var cpuCount: String? = null,
    var osVersion: String? = null,
    var maxWorkers: String? = null,
    var javaRuntime: String? = null,
    var javaVmName: String? = null,
    var javaXmsBytes: String? = null,
    var javaXmxBytes: String? = null,
    var javaMaxPermSize: String? = null,
    var locale: String? = null,
    var username: String? = null,
    var defaultChartset: String? = null,
    var ideVersion: String? = null,
    var gradleVersion: String? = null,
    var cacheUrl: String? = null,
    var cacheStore: String? = null,
    var plugins: List<Plugin> = emptyList(),
    var gitBranch: String? = null,
    var gitUser: String? = null,
    var switches: Switches = Switches(),
    var hostname: String? = null,
    var gradleJvmArgs: Map<String, String>? = null,
    var gradleProcessesAvailable: Int? = null,
    var multipleGradleProcesses: Boolean? = null,
    var multipleGradleJvmArgs: Map<String, Map<String, String>>? = null,
    var kotlinJvmArgs: Map<String, String>? = null,
    var kotlinProcessesAvailable: Int? = null,
    var multipleKotlinProcesses: Boolean? = null,
    var multipleKotlinJvmArgs: Map<String, Map<String, String>>? = null,
    var processesStats: Processes = Processes()
) : java.io.Serializable

data class Switches(
    var buildCache: String? = null,
    var configurationOnDemand: String? = null,
    var daemon: String? = null,
    var parallel: String? = null,
    var continueOnFailure: String? = null,
    var dryRun: String? = null,
    var offline: String? = null,
    var rerunTasks: String? = null,
    var refreshDependencies: String? = null,
    var buildScan: String? = null,
    var configurationCache: String? = null
) : java.io.Serializable

data class CustomProperties(
    var buildProperties: MutableMap<String, String> = mutableMapOf(),
    var taskProperties: MutableMap<String, String> = mutableMapOf()
) : java.io.Serializable

/**
 * TODO: figure out how to get the list of current plugins applied to the project
 */
data class Plugin(
    var id: String,
    var mainClass: String,
    var version: String
) : java.io.Serializable
