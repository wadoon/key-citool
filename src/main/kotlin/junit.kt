/* key-tools are extension for the KeY theorem prover.
 * Copyright (C) 2021  Alexander Weigl
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the complete terms of the GNU General Public License, please see this URL:
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package de.uka.ilkd.key

import java.io.Writer
import java.util.*
import kotlin.collections.ArrayList

class XmlWriter(private val stream: Writer) {
    fun write(s: String) = stream.write(s)
    fun attr(key: String, value: Any?) =
        value?.let {
            stream.write(" $key = \"$it\"")
        }

    fun element(
        s: String,
        vararg attrs: Pair<String, Any?>,
        function: XmlWriter.() -> Unit = {}
    ) {
        write("<$s")
        attrs.forEach { (k, v) -> attr(k, v) }
        function()
        write("</$s>")
    }

    fun cdata(it: String) {
        stream.write("<![CDATA[")
        stream.write(it)
        stream.write("]]>")
    }
}

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

data class TestCase(
    /** Name of the test method, required. */
    var name: String,
    /** Full class name for the class the test method is in. required */
    var classname: String = ""
) {
    /** number of assertions in the test case. optional */
    var assertions: Int? = null

    /** Time taken (in seconds) to execute the test. optional */
    var time: Int? = null

    /** Unknown*/
    var status: String? = null

    var result: TestCaseKind = TestCaseKind.UNKNOWN

    /** Data that was written to standard out while the test suite was executed. optional */
    var sysout: String? = null

    /** Data that was written to standard error while the test suite was executed. optional */
    var syserr: String? = null

    val disabled: Boolean
        get() = result is TestCaseKind.Skipped

    val failures: Boolean
        get() = result is TestCaseKind.Failure

    val errors: Boolean
        get() = result is TestCaseKind.Error

    fun writeXml(writer: XmlWriter) {
        writer.element(
            "testcase",
            "name" to name,
            "assertions" to assertions,
            "classname" to classname,
            "status" to status,
            "time" to time
        ) {

            result.writeXml(writer)

            syserr?.let {
                writer.element("system-err") {
                    cdata(it)
                }
            }

            sysout?.let {
                writer.element("system-out") {
                    cdata(it)
                }
            }
        }
    }
}

sealed class TestCaseKind {
    abstract fun writeXml(writer: XmlWriter)

    object UNKNOWN : TestCaseKind() {
        override fun writeXml(writer: XmlWriter) {}
    }

    /**
     *
     * If the test was not executed or failed, you can specify one
     * the skipped, error or failure elements.
     * skipped can appear 0 or once. optional
     * message/description string why the test case was skipped. optional */
    data class Skipped(val message: String?) : TestCaseKind() {
        override fun writeXml(writer: XmlWriter) {
            writer.element("skipped", "message" to message)
        }
    }

    /** Indicates that the test errored. An errored test is one
    that had an unanticipated problem. For example an unchecked
    throwable or a problem with the implementation of the
    test. Contains as a text node relevant data for the error,
    for example a stack trace. optional */
    data class Error(
        /** The error message. e.g., if a java exception is thrown, the return value of getMessage() */
        val message: String?,
        /** full class name of the exception */
        val type: String?
    ) : TestCaseKind() {
        override fun writeXml(writer: XmlWriter) {
            writer.element("error", "message" to message, "type" to type)
        }
    }

    /** Indicates that the test failed. A failure is a test which
    the code has explicitly failed by using the mechanisms for
    that purpose. For example via an assertEquals. Contains as
    a text node relevant data for the failure, e.g., a stack
    trace. optional */
    data class Failure(
        /** The message specified in the assert. */
        val message: String?,
        /** The type of the assert. */
        val type: String = ""
    ) : TestCaseKind() {
        override fun writeXml(writer: XmlWriter) {
            writer.element("failure", "message" to message)
        }
    }
}
