package de.uka.ilkd.key

import java.io.PrintWriter


data class Report(
    val name: String,
    val sessionInfos: List<SessionInfo>,
    val groups: List<Group>,
    val packages: List<Package>,
    val counters: List<Counter>,
) {
    fun write(out: PrintWriter) {
        out.println("<report name=\"$name\">")
        sessionInfos.forEach { it.write(out) }
        groups.forEach { it.write(out) }
        packages.forEach { it.write(out) }
        counters.forEach { it.write(out) }
        out.println("</report>")
    }
}

/**information about a session which contributed execution data*/
data class SessionInfo(
    val id: String,
    /**start time stamp */
    val start: String,
    /**dump time stamp*/
    val dump: String
) {
    fun write(out: PrintWriter) {
        out.println("<sessioninfo id=\"$id\" start=\"$start\" dump=\"$dump\" />")
    }
}

/** representation of a group */
data class Group(
    /** group name */
    val name: String,
    val groups: List<Group>,
    val packages: List<Package>,
    val counters: List<Counter>
) {
    fun write(out: PrintWriter) {
        out.println("<group name=\"$name\">")
        groups.forEach { it.write(out) }
        packages.forEach { it.write(out) }
        counters.forEach { it.write(out) }
        out.println("</group>")
    }
}

/** representation of a package */
data class Package(
    /** package name in VM notation */
    val name: String,
    val classes: List<Class>,
    val sourceFiles: List<Sourcefile>,
    val counters: List<Counter>
) {
    fun write(out: PrintWriter) {
        out.println("<package name=\"$name\">")
        classes.forEach { it.write(out) }
        sourceFiles.forEach { it.write(out) }
        counters.forEach { it.write(out) }
        out.println("</package>")
    }
}

/** representation of a class */
data class Class(
    /** fully qualified VM name */
    val name: String,
    /** name of the corresponding source file */
    val sourceFilename: String,
    val methods: List<method>,
    val counters: List<Counter>
) {
    fun write(out: PrintWriter) {
        out.println("<class name=\"$name\" sourcefilename=\"$sourceFilename\">")
        methods.forEach { it.write(out) }
        counters.forEach { it.write(out) }
        out.println("</class>")
    }
}

/** representation of a method */
data class method(
    /** method name */
    val name: String,
    /** method descriptor */
    val desc: String,
    /** first source line number of this method */
    val line: String,
    val counters: List<Counter>
) {
    fun write(out: PrintWriter) {
        out.println("<method name=\"$name\" desc=\"$desc\" line=\"$line\">")
        counters.forEach { it.write(out) }
        out.println("</class>")
    }
}

/** representation of a source file */
data class Sourcefile(
    /** local source file name */
    val name: String,
    val lines: List<Line>,
    val counters: List<Counter>
) {
    fun write(out: PrintWriter) {
        out.println("<sourcefile name=\"$name\">")
        lines.forEach { it.write(out) }
        counters.forEach { it.write(out) }
        out.println("</sourcefile>")
    }
}

/** representation of a source line */
data class Line(
    /** line number */
    val nr: Int,
    /** number of missed instructions */
    val mi: Int,
    /** number of covered instructions */
    val ci: Int,
    /** number of missed branches */
    val mb: Int,
    /** number of covered branches */
    val cb: Int
) {
    fun write(out: PrintWriter) {
        out.println("<counter nr=\"$nr\" mi=\"$mi\" ci=\"$ci\" mb=\"$mb\" cb=\"$cb\" />")
    }
}

/** coverage data counter for different metrics */
data class Counter(
    /** metric type */
    val type: CounterKind,
    /** number of missed items */
    val missed: Int,
    /** number of covered items */
    val covered: Int
) {
    fun write(out: PrintWriter) {
        out.println("<counter type=\"$type\" missed=\"$missed\" covered=\"$covered\" />")
    }
}

enum class CounterKind {
    INSTRUCTION, BRANCH, LINE, COMPLEXITY, METHOD, CLASS
}