package io.github.wadoon.keycitool.junit

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