package org.key_project.web

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.http.content.staticRootFolder
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import kotlinx.html.div
import kotlinx.html.dom.create
import kotlinx.html.dom.document
import java.io.File
import java.io.PrintWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

val TIMEOUT_IN_SECONDS = System.getenv().getOrDefault("TIMEOUT", "60").toLong()
val TEMP_DIR = System.getenv().getOrDefault("TEMP_DIR", "tmp")
val WEB_DIR = System.getenv().getOrDefault("WEB_DIR", "src/web")
val TEMP_PATH = Paths.get(TEMP_DIR)

val compute = newFixedThreadPoolContext(4, "compute")


/**
 *
 * @author Alexander Weigl
 * @version 1 (14.05.19)
 */
object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, port = 8081, module = Application::mainModule).start(wait = true)
    }
}

fun Application.mainModule() {
    install(CallLogging)
    install(AutoHeadResponse)


    routing {
        static("/") {
            staticRootFolder = File(WEB_DIR).absoluteFile
            println(staticRootFolder)
            files(".")
            default("index.html")
        }

        post("/run") {
            val jmlInput = call.receiveText()
            if (jmlInput.isBlank()) {
                val myDiv = document {
                    create.div("error") {
                        +"No input provided."
                    }
                }
                call.respondText(myDiv.textContent, ContentType.Text.Html, HttpStatusCode.OK)
            } else {
                call.respondTextWriter(ContentType.Text.Html) {
                    val w = this
                    withContext(compute) {
                        runKey(jmlInput, w)
                    }
                }
            }
        }
    }
}

fun runKey(input: String, writer: Writer) {
    val out = PrintWriter(writer)
    try {
        out.run {
            val folder = Files.createTempDirectory(TEMP_PATH, "keyquickweb")
            info("Run in folder $folder")
            val className = findClassName(input)
            val file = folder.resolve("$className.java")
            Files.createFile(file)
            Files.newBufferedWriter(file).use {
                it.write(input)
            }
            startKey(this, file.toFile())
        }
    } catch (e: Exception) {
        out.append("<div class=\"error\"><pre>")
        e.printStackTrace(out)
        e.printStackTrace()
        out.append("</pre></div>")
    }
}


fun findClassName(input: String): String {
    val p = "public class (\\w+)".toRegex()
    return p.find(input)?.groupValues?.get(1)
            ?: throw RuntimeException("Could not find the public class name. Used pattern: ${p.pattern}.")
}

/**
 * Spawn a second process in which key [Worker] runs.
 */
private fun startKey(out: PrintWriter, input: File, vararg options: String) {
    val javaHome = System.getProperty("java.home")
    val javaBin = "$javaHome${File.separator}bin${File.separator}java"
    val classpath = System.getProperty("java.class.path")
            .splitToSequence(":")
            .joinToString(":") { File(it).absolutePath }

    val className = Worker.javaClass.name

    val builder = ProcessBuilder(
            javaBin, "-cp", classpath, className, *options, input.absolutePath)
            .directory(input.parentFile)
            .redirectErrorStream(true)

    println("Commands: ${builder.command()}")

    val ms = measureTimeMillis {
        val process = builder.start()
        val x = process.waitFor(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
        if (!x) {
            process.destroyForcibly()
            out.info("KeY exceeded timeout")
        }
        val exit = process.exitValue()
        if (exit != 0) {
            out.error("KeY exited with $exit")
        }
        val text = process.inputStream.bufferedReader().readText()
        out.println(text)
    }
    out.info("Request took: $ms milliseconds.")
}

private fun Writer.info(s: String) {
    append("<div class=\"info\">$s</div>")
}

private fun Writer.error(s: String) {
    append("<div class=\"error\">$s</div>")
}
