package io.github.wadoon.keycitool.junit

import java.util.Date

@Suppress("MemberVisibilityCanBePrivate")
data class TestSuite(
    var name: String = "",
    private val impl: ArrayList<TestCase> = ArrayList()
) :
    MutableList<TestCase> by impl {
    /** Full (class) name of the test for non-aggregated testsuite documents.
    Class name without the package for aggregated testsuites documents. Required */
    /** The total number of tests in the suite, required. */
    fun tests(): Int = size

    /** the total number of disabled tests in the suite. optional */
    fun disabled(): Int = filter { it.disabled }.size

    /** The total number of tests in the suite that errored. An errored test is one that had an unanticipated problem,
    for example an unchecked throwable; or a problem with the implementation of the test. optional */
    fun errors(): Int = filter { it.errors }.size

    /** The total number of tests in the suite that failed. A failure is a test which the code has explicitly failed
    by using the mechanisms for that purpose. e.g., via an assertEquals. optional */
    fun failures(): Int = filter { it.failures }.size

    /** Host on which the tests were executed. 'localhost' should be used if the hostname cannot be determined. optional */
    var hostname = "localhost"

    // /** Starts at 0 for the first testsuite and is incremented by 1 for each following testsuite */
    // var id : Int = 0

    /** Derived from testsuite/@name in the non-aggregated documents. optional */
    var package_: String? = ""

    /** The total number of skipped tests. optional */
    fun skipped() = 0

    /** Time taken (in seconds) to execute the tests in the suite. optional */
    fun time() = 0

    fun writeXml(writer: XmlWriter) {
        writer.element(
            "testsuites",
            "errors" to errors(),
            "disabled" to disabled(),
            "failures" to failures(),
            "tests" to tests(),
            "hostname" to hostname,
            "package" to package_,
            "skipped" to skipped(),
            "timestamp" to timestamp,
            "time" to time()
        ) {
            element("properties") {
                properties.forEach { (k, v) ->
                    element(
                        "property",
                        "name" to k, "value" to v.toString()
                    )
                }
            }
            forEach { it.writeXml(writer) }
        }
    }

    fun newTestCase(name: String) = TestCase(name).also { add(it) }

    /** when the test was executed in ISO 8601 format (2014-01-21T16:17:18). Timezone may not be specified. optional */
    var timestamp: String? = Date().toString()

    /**
    property can appear multiple times. The name and value attributres are required.
     */
    val properties: MutableMap<String, Any?> = hashMapOf()
}