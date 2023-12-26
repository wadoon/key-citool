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
package de.uka.ilkd.key

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class Cli {
    @Test
    fun test_any2json() {
        assertEquals("1", obj2json(1))
        assertEquals("null", obj2json(null))
        assertEquals("\"abc\"", obj2json("abc"))
        assertEquals("1.2", obj2json(1.2))
        assertEquals("2.0E-9", obj2json(0.000000002))
        assertEquals("[1,2,3,4,5]", obj2json(arrayListOf(1, 2, 3, 4, 5)))
        assertEquals("[\"a\",\"b\",\"c\",4,5]", obj2json(arrayListOf("a", "b", "c", 4, 5)))
        assertEquals("{\"abc\" : 2}", obj2json(hashMapOf("abc" to 2)))
        assertEquals("{\"abc\" : {\"abc\" : 2}}", obj2json(hashMapOf("abc" to hashMapOf("abc" to 2))))
        assertEquals("{\"abc\" : 2}", obj2json(hashMapOf("abc" to 2)))
    }

    @Test
    fun empty() {
    }
}
