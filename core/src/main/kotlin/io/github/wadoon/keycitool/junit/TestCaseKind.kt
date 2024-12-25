package io.github.wadoon.keycitool.junit

sealed class TestCaseKind {
    abstract fun writeXml(writer: XmlWriter)

    object UNKNOWN : TestCaseKind() {
        override fun writeXml(writer: XmlWriter) {
            //no need to write anything to the stream.
        }
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