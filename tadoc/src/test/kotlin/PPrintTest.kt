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
package org.key_project.core.doc
import de.uka.ilkd.key.nparser.ParsingFacade
import org.junit.jupiter.api.Test
import org.key_project.core.doc.*
import java.io.File

/**
 *
 * @author Alexander Weigl
 * @version 1 (1/26/22)
 */

class PPrintTest {
    @Test
    fun first() {
        val d =
            string("begin") `^^` nest(
                4,
                break1 `^^` string("stmt;")
                    `^^` break1 `^^` string("stmt;")
                    `^^` break1 `^^` string("stmt;")
            ) `^^` break1 `^^` string("end")
        println(pretty(d, 40))
    }

    @Test
    fun mapFile() {
        val ast = ParsingFacade.parseFile(File("src/test/resources/map.key"))
        println("read!")
        val txt = pretty(
            ParsingFacade.getParseRuleContext(ast),
            Index(),
            Symbol("Test", "xxx", type = Symbol.Type.FILE),
            hashMapOf()
        )
        println("TXT: $txt")
    }
}
