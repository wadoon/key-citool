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

import org.key_project.core.doc.execute
import java.io.File

object GitBlameService {
    private val map = HashMap<String, List<BlameInfo>>()
    private val blameLineRegex =
        "^(?<sha>[a-f0-9]+) \\((?<author>[^0-9]+)\\s*(?<ts>[0-9]+) (?<tz>\\+\\d\\d\\d\\d) (?<lno>\\d+)\\).*\$".toRegex()

    fun getCompleteBlame(file: String) =
        map.computeIfAbsent(file) { parse(execute("git", "blame", "-t", File(it).absolutePath)) }

    private fun parse(execute: String): List<BlameInfo> {
        // 1cfd130d7de (Alexander Weigl   1574945363 +0100 293) // Generation of a JavaDoc across sub projects.

        fun parseLine(it: String) =
            blameLineRegex.matchEntire(it)?.groups?.let { g ->
                BlameInfo(
                    g["sha"]!!.value,
                    g["author"]!!.value,
                    g["ts"]!!.value.toInt(),
                    g["lno"]!!.value.toInt()
                )
            }

        return execute.splitToSequence('\n')
            .map { parseLine(it) }
            .filterNotNull()
            .toList()
    }

    fun getLastAuthorsWithDates(file: String?, lineStart: Int, lineStop: Int): Sequence<BlameInfo> {
        if (file == null) return emptySequence()
        val infos = getCompleteBlame(file)
        return infos.asSequence().filter { it.line in lineStart..lineStop }
    }

    fun lastUpdated(file: String): BlameInfo {
        val text = execute("git", "log", "--decorate=no", "--no-notes", "--date=unix", "--name-only", "--", file)
        return lastUpdatedParse(text)
    }

    fun lastUpdated(file: String, lineStart: Int, lineStop: Int): BlameInfo {
        val text = execute(
            "git", "log", "--no-patch", "--decorate=no", "--no-notes", "--date=unix",
            "-L", "$lineStart,$lineStop:$file", "--", file
        )
        return lastUpdatedParse(text)
    }

    fun lastUpdatedParse(text: String): BlameInfo {
        var commit = ""
        var author = ""
        var date = ""
        for (it in text.splitToSequence('\n')) {
            if (it.startsWith("commit:")) commit = it.substring(7)
            if (it.startsWith("Author:")) author = it.substring(7)
            if (it.startsWith("Date:")) date = it.substring(4)

            if (!commit.isBlank() && !author.isBlank() && !date.isBlank()) {
                break
            }
        }

        return BlameInfo(commit.trim(), author.trim(), date.toIntOrNull() ?: -1, 0)
    }
}

data class BlameInfo(
    val gitCommit: String,
    val author: String,
    val timestamp: Int,
    val line: Int
)
