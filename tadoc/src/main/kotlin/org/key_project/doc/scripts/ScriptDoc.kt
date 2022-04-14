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
package org.key_project.doc.scripts

import de.uka.ilkd.key.api.KeYApi
import de.uka.ilkd.key.macros.ProofMacro
import de.uka.ilkd.key.macros.scripts.ProofScriptCommand
import de.uka.ilkd.key.proof.io.ProblemLoaderException
import de.uka.ilkd.key.rule.Taclet
import kotlinx.html.*
import org.key_project.core.doc.DefaultPage
import org.key_project.core.doc.Index
import java.io.File
import java.util.*
import java.util.stream.Collectors

/**
 * @author Alexander Weigl
 * @version 1 (11.09.17)
 */
class ScriptDoc(index: Index) : DefaultPage(File("script.html"), "Reference for Proof Scripts", index) {
    override fun content(div: DIV) {
        writePreamble(div)
        writeCommand(div)
        writeMacros(div)
        writeTacletDocumentation(div)
    }

    private fun writePreamble(stream: DIV) {
        stream.h1 { +"Reference for Proof Scripts" }
        stream.p { +"*Generated on ${Date()}" }
    }

    private val FORBBIDEN_COMMANDS = setOf("exit", "focus", "javascrpt", "leave", "let")

    private val basedir = File("..")

    private val dummyFile = File(
        ".", "key.ui/examples/standard_key/prop_log/contraposition.key"
    )

    @get:Throws(ProblemLoaderException::class)
    private val taclets: List<Taclet> by lazy {
        println("Use dummy file: " + dummyFile.absolutePath)
        val env = KeYApi.loadFromKeyFile(dummyFile).loadedProof.env
        val a = env.initConfig.taclets
        a.stream()
            .sorted(Comparator.comparing { obj: Taclet -> obj.name() })
            .collect(Collectors.toList())
    }

    private fun writeTacletDocumentation(stream: DIV) {
        stream.section {
            h2 { +"Taclets" }
            div {
                for (t in taclets) {
                    div {
                        +"[rule] "
                        span { +t.displayName() }
                        +" "
                        t.ifFindVariables.forEach {
                            span { +"inst_$it" }
                        }
                    }
                }
            }
        }
    }

    private fun writeMacros(stream: DIV) {
        val macros = ArrayList(KeYApi.getMacroApi().macros)
        macros.sortWith(Comparator.comparing { obj: ProofMacro -> obj.scriptCommandName })
        stream.section {
            h2 { +"Macros" }
            for (t in macros) {
                div {
                    span("name macro") { +t.scriptCommandName }
                    p {
                        +"Original name ${t.name} in ${t.category}"
                    }
                    p {
                        +t.description
                    }
                }
            }
        }
    }

    private fun P.helpForCommand(c: ProofScriptCommand<*>) {
        h3 { +c.name }
        div {
            +"> Synopsis: "
            span {
                +c.name
                for (a in c.arguments) {
                    +" "
                    if (a.isFlag) {
                        +"[${a.name}]"
                    } else {
                        val arg =
                            if (a.name.startsWith("#")) // positional argument
                                "<${a.type.simpleName.uppercase(Locale.getDefault())}>"
                            else
                                "${a.name}=<${a.type.simpleName.uppercase(Locale.getDefault())}>"
                        if (!a.isRequired) +"[$arg]"
                        else +arg
                    }
                }
            }
        }
        p { +c.documentation }
        p {
            h4 { +"Arguments:" }
            ul {
                for (a in c.arguments) {
                    li {
                        +"${a.name} : ${a.type.simpleName.uppercase()}"

                        if (a.isRequired) {
                            +" ("
                            b { +"required" }
                            +")"
                        }
                        +(a.documentation ?: " not available")
                    }
                }
            }
        }
    }

    private fun writeCommand(stream: DIV) {
        val commands = ArrayList(KeYApi.getScriptCommandApi().scriptCommands)
        commands.sortWith(Comparator.comparing { obj: ProofScriptCommand<*> -> obj.name })
        stream.div {
            h2 { +"Commands" }
            for (t in commands) {
                if (t.name !in FORBBIDEN_COMMANDS)
                    p { this.helpForCommand(t) }
            }
        }
    }
}
