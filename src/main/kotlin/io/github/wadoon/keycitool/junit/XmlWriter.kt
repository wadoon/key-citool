package io.github.wadoon.keycitool.junit

import java.io.Writer

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