package io.github.wadoon.keycitool.junit

import java.io.Writer

/**
 *
 * @author Alexander Weigl
 * @version 1 (3/10/20)
 */
data class TestSuites(
    var name: String = "",
    private val impl: ArrayList<TestSuite> = ArrayList()
) :
    MutableList<TestSuite> by impl {

    /** total number of successful tests from all testsuites. */
    fun tests(): Int = sumOf { it.size }

    /** total number of disabled tests from all testsuites. */
    fun disabled(): Int = sumOf { it.disabled() }

    /**total number of tests with error result from all testsuites.*/
    fun errors(): Int = sumOf { it.errors() }

    /** total number of failed tests from all testsuites. */
    fun failures(): Int = sumOf { it.failures() }

    /** time in seconds to execute all test suites. */
    fun time(): Int = sumOf { it.time() }

    fun writeXml(writer: Writer) = writeXml(XmlWriter(writer))
    fun writeXml(writer: XmlWriter) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        writer.element(
            "testsuites",
            "errors" to errors(),
            "disabled" to disabled(),
            "failures" to failures(),
            "tests" to tests(),
            "time" to time()
        ) {
            forEach { it.writeXml(writer) }
        }
    }

    fun newTestSuite(name: String): TestSuite =
        TestSuite(name).also { add(it) }
}