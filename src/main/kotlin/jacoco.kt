package de.uka.ilkd.key

import java.io.PrintWriter
import java.io.StringWriter

class JacocoBuilder() {
    private val sw = StringWriter()
    private val out = PrintWriter(sw)
    fun report(name: String, fn: JacocoReportBuilder.() -> Unit) {
        out.println("<report name=\"$name\">")
        JacocoReportBuilder().fn()
        out.println("</report>")
    }

    inner class JacocoReportBuilder {
        /**
         * 	<!-- session id -->
         * 		<!ATTLIST sessioninfo id CDATA #REQUIRED>
         * 		<!-- start time stamp -->
         * 		<!ATTLIST sessioninfo start CDATA #REQUIRED>
         * 		<!-- dump time stamp -->
         * 		<!ATTLIST sessioninfo dump CDATA #REQUIRED>
         */
        fun sessioninfo(id: String, start: String, dump: String) {
            out.println("<sessioninfo id=\"$id\" start=\"$start\" dump=\"$dump\" />")
        }

        fun group(name: String, fn: JacocoReportBuilder.() -> Unit) {
            out.println("<group name=\"$name\">")
            this.fn()
            out.println("</group>")
        }

        fun `package`() = ""
        fun counter() = ""
    }
}