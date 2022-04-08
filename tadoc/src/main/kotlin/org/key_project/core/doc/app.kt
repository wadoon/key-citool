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
package org.key_project.core.doc.org.key_project.core.doc

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import de.uka.ilkd.key.nparser.KeYLexer
import de.uka.ilkd.key.nparser.KeYParser
import de.uka.ilkd.key.nparser.ParsingFacade
import org.antlr.v4.runtime.CharStreams
import org.key_project.core.doc.*
import org.key_project.doc.scripts.ScriptDoc
import java.io.File

object App {
    private const val logFormat = "[%5d] %s%s%s"
    private val startTime = System.currentTimeMillis()

    @JvmStatic
    fun main(args: Array<String>) {
        GenDoc().main(args)
    }

    fun putln(s: String, colorOn: String = "", colorOff: String = "") =
        println(String.format(logFormat, (System.currentTimeMillis() - startTime), colorOn, s, colorOff))

    const val ESC = 27.toChar()
    fun putln(s: String, color: Int) = putln(s, "$ESC[${color}m", "$ESC[0m")
    fun errorln(s: String) = putln(s, 33)
    private var printedErrors = mutableSetOf<String>()
    fun errordpln(s: String) {
        if (s !in printedErrors) {
            printedErrors.add(s); errorln(s)
        }
    }
}

interface GenDocStep {
    fun prepare()
    fun manifest()
}

class GenDoc : CliktCommand() {
    val outputFolder by option("-o", "--output", help = "output folder", metavar = "FOLDER")
        .file().default(File("target"))

    val inputFiles by argument("taclet-file", help = "")
        .file().multiple(required = true)

    val tacletFiles by lazy {
        inputFiles.flatMap { file ->
            when {
                file.isDirectory ->
                    file.walkTopDown().filter { it.name.endsWith(".key") }.toList()
                else -> listOf(file)
            }
        }
    }

    private val usageIndex: UsageIndex = HashMap()

    private val symbols = Index().also {
        val l = KeYLexer(CharStreams.fromString(""))
        (0..l.vocabulary.maxTokenType)
            .filter { l.vocabulary.getLiteralName(it) != null }
            .forEach { t ->
                l.vocabulary.getSymbolicName(t)?.let { name ->
                    it += Symbol.token(name, t)
                }
            }
    }

    override fun run() {
        outputFolder.mkdirs()
        copyStaticFiles()
        tacletFiles.map(::index).zip(tacletFiles).forEach { (ctx, f) -> run(ctx, f) }
        ScriptDoc(symbols)
        generateIndex()
    }

    private fun copyStaticFiles() {
        copyStaticFile("style.css")
    }

    private fun copyStaticFile(s: String) {
        javaClass.getResourceAsStream("/static/$s")?.use { input ->
            File(outputFolder, s).outputStream().use { out ->
                input.copyTo(out)
            }
        }
    }

    private fun index(f: File): KeYParser.FileContext {
        App.putln("Parsing $f")
        val ast = ParsingFacade.parseFile(f)
        val ctx = ParsingFacade.getParseRuleContext(ast)
        val self = f.nameWithoutExtension + ".html"
        App.putln("Indexing $f")
        ctx.accept(Indexer(self, symbols))
        return ctx
    }

    fun run(ctx: KeYParser.FileContext, f: File) {
        try {
            App.putln("Analyze: $f")
            val target = File(outputFolder, f.nameWithoutExtension + ".html")
            DocumentationFile(target, f, ctx, symbols, usageIndex).manifest()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateIndex() {
        val f = File(outputFolder, "index.html")
        IndexPage(f, symbols).manifest()

        val uif = File(outputFolder, "usage.html")
        UsageIndexFile(uif, symbols, usageIndex).manifest()
    }
}
