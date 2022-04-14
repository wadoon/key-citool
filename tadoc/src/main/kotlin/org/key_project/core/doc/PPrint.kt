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
@file:Suppress("unused", "KDocUnresolvedReference")

package org.key_project.core.doc

import org.key_project.core.doc.Document.Group
import org.key_project.core.doc.Document.HardLine
import org.key_project.core.doc.Document.IfFlat
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*
import kotlin.Boolean
import kotlin.Char
import kotlin.Double
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Pair
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.also
import kotlin.let
import kotlin.require
import kotlin.text.String
import kotlin.text.indexOf
import kotlin.text.repeat
import kotlin.text.split
import kotlin.text.substring
import kotlin.text.toCharArray
import kotlin.to

/** A point is a pair of a line number and a column number. */
typealias Point = Pair<Int, Int>

/** A range is a pair of points. */
typealias PointRange = Pair<Point, Point>

/** A type of integers with infinity. Infinity is encoded as [max_int]. */
@JvmInline
value class Requirement(val value: Int) {
    val isInfinity: Boolean
        get() = value == infinity.value

    /** Addition of integers with infinity. */
    operator fun plus(y: Requirement) =
        if (isInfinity || y.isInfinity) infinity
        else Requirement(value + y.value)

    /* Comparison between an integer with infinity and a normal integer. */
    operator fun compareTo(y: Int) = value - y // x<=ys
}

val infinity = Requirement(Integer.MAX_VALUE)

/* ------------------------------------------------------------------------- */

/* Printing blank space. This is used both internally (to emit indentation
characters) and via the public combinator [blank]. */

const val blank_length = 80

/** The rendering engine maintains the following internal state. Its structure
is subject to change in future versions of the library. Nevertheless, it is
exposed to the user who wishes to define custom documents. */
data class State(
    /** The line width. This parameter is fixed throughout the execution of
    the renderer. */
    val width: Int,
    /** The ribbon width. This parameter is fixed throughout the execution of
    the renderer. */
    val ribbon: Int,
    /** The number of blanks that were printed at the beginning of the current
    line. This field is updated (only) when a hardline is emitted. It is
    used (only) to determine whether the ribbon width constraint is
    respected. */
    var lastIndent: Int = 0,
    /** The current line. This field is updated (only) when a hardline is
    emitted. It is not used by the pretty-printing engine itself. */
    var line: Int = 0,
    /** The current column. This field must be updated whenever something is
    sent to the output channel. It is used (only) to determine whether the
    width constraint is respected. */
    var column: Int = 0
) {
    constructor(width: Int, rfrac: Double) : this(width, max(0, min(width, (width * rfrac).toInt())))
}

/** A custom document is defined by implementing the following methods. */
interface CustomDocument {
    /** A custom document must publish the width (i.e., the number of columns)
    that it would like to occupy if it is printed on a single line (that is,
    in flattening mode). The special value [infinity] means that this
    document cannot be printed on a single line; this value causes any
    groups that contain this document to be dissolved. This method should
    in principle work in constant time. */
    val requirement: Requirement

    /** The method [pretty] is used by the main rendering algorithm. It has
    access to the output channel and to the algorithm's internal state, as
    described above. In addition, it receives the current indentation level
    and the current flattening mode (on or off). If flattening mode is on,
    then the document must be printed on a single line, in a manner that is
    consistent with the Requirement that was published ahead of time. If
    flattening mode is off, then there is no such obligation. The state must
    be updated in a manner that is consistent with what is sent to the
    output channel. */
    fun pretty(o: PrintWriter, s: State, i: Int, b: Boolean)

    /** The method [compact] is used by the compact rendering algorithm. It has
    access to the output channel only. */
    fun compact(o: PrintWriter)
}

/** Here is the algebraic data type of documents. It is analogous to Daan
Leijen's version, but the binary constructor [Union] is replaced with
the unary constructor [Group], and the constant [Line] is replaced with
more general constructions, namely [IfFlat], which provides alternative
forms depending on the current flattening mode, and [HardLine], which
represents a newline character, and causes a failure in flattening mode. */

sealed class Document {
    /** [Empty] is the empty document. */
    object Empty : Document()

    /** [Char c] is a document that consists of the single character [c]. We
    enforce the invariant that [c] is not a newline character. */
    data class Char(val char: kotlin.Char) : Document()

    /** [String s] is a document that consists of just the string [s]. We
    assume, but do not check, that this string does not contain a newline
    character. [String] is a special case of [FancyString], which takes up
    less space in memory. */
    data class String(val s: kotlin.String) : Document()

    /** [FancyString (s, ofs, len, apparent_length)] is a (portion of a) string
    that may contain fancy characters: color escape characters, UTF-8 or
    multi-byte characters, etc. Thus, the apparent length (which corresponds
    to what will be visible on screen) differs from the length (which is a
    number of bytes, and is reported by [String.length]). We assume, but do
    not check, that fancystrings do not contain a newline character. */
    data class FancyString(
        val s: kotlin.String,
        val ofs: Int,
        val len: Int,
        val apperentLength: Int
    ) : Document() {
        constructor(s: kotlin.String, apparentLength: Int) : this(s, 0, s.length, apparentLength)
    }

    /** [Blank n] is a document that consists of [len] blank characters. */
    data class Blank(val len: Int) : Document()

    /** When in flattening mode, [IfFlat (d1, d2)] turns into the document
    [d1]. When not in flattening mode, it turns into the document [d2]. */
    data class IfFlat(val doc1: Document, val doc2: Document) : Document()

    /** When in flattening mode, [HardLine] causes a failure, which requires
    backtracking all the way until the stack is empty. When not in flattening
    mode, it represents a newline character, followed with an appropriate
    number of indentation. A common way of using [HardLine] is to only use it
    directly within the right branch of an [IfFlat] construct. */
    object HardLine : Document()

    /** The following constructors store their space Requirement. This is the
    document's apparent length, if printed in flattening mode. This
    information is computed in a bottom-up manner when the document is
    constructed. */

    /** In other words, the space Requirement is the number of columns that the
    document needs in order to fit on a single line. We express this value in
    the set of `integers extended with infinity', and use the value
    [infinity] to indicate that the document cannot be printed on a single
    line. */

    /** Storing this information at [Group] nodes is crucial, as it allows us to
    avoid backtracking and buffering. */

    /** Storing this information at other nodes allows the function [Requirement]
    to operate in constant time. This means that the bottom-up computation of
    requirements takes linear time. */

    /** [Cat (req, doc1, doc2)] is the concatenation of the documents [doc1] and
    [doc2]. The space Requirement [req] is the sum of the requirements of
    [doc1] and [doc2]. */

    data class Cat(val req: Requirement, val doc1: Document, val doc2: Document) : Document()

    /** [Nest (req, j, doc)] is the document [doc], in which the indentation
    level has been increased by [j], that is, in which [j] blanks have been
    inserted after every newline character. The space Requirement [req] is
    the same as the Requirement of [doc]. */

    data class Nest(val req: Requirement, val j: Int, val doc: Document) : Document()

    /** [Group (req, doc)] represents an alternative: it is either a flattened
    form of [doc], in which occurrences of [Group] disappear and occurrences
    of [IfFlat] resolve to their left branch, or [doc] itself. The space
    Requirement [req] is the same as the Requirement of [doc]. */

    data class Group(val req: Requirement, val doc: Document) : Document()

    /** [Align (req, doc)] increases the indentation level to reach the current
    column.  Thus, the document [doc] is rendered within a box whose upper
    left corner is the current position. The space Requirement [req] is the
    same as the Requirement of [doc]. */
    data class Align(val req: Requirement, val doc: Document) : Document()

    /** [Range (req, hook, doc)] is printed like [doc]. After it is printed, the
    function [hook] is applied to the range that is occupied by [doc] in the
    output. */
    data class Range(val req: Requirement, val fn: (PointRange) -> Unit, val doc: Document) : Document()

    /** [Custom (req, f)] is a document whose appearance is user-defined. */
    data class Custom(val doc: CustomDocument) : Document()
}

/* Retrieving or computing the space Requirement of a document. */
tailrec fun Requirement(x: Document): Requirement =
    when (x) {
        is Document.Empty -> Requirement(0)
        is Document.Char -> Requirement(1)
        is Document.String -> Requirement(x.s.length)
        is Document.FancyString -> Requirement(x.apperentLength)
        is Document.Blank -> Requirement(x.len)
        /* In flattening mode, the Requirement of [ifflat x y] is just the
        Requirement of its flat version, [x]. */
        /* The smart constructor [ifflat] ensures that [IfFlat] is never nested
        in the left-hand side of [IfFlat], so this recursive call is not a
        problem; the function [Requirement] has constant time complexity. */
        is Document.IfFlat -> Requirement(x.doc1)
        /* A hard line cannot be printed in flattening mode. */
        is Document.HardLine -> infinity

        /* These nodes store their Requirement -- which is computed when the
        node is constructed -- so as to allow us to answer in constant time
        here. */
        is Document.Cat -> x.req
        is Document.Nest -> x.req
        is Document.Group -> x.req
        is Document.Align -> x.req
        is Document.Range -> x.req
        // | Custom c -> c#Requirement
        else -> throw IllegalArgumentException()
    }
/* ------------------------------------------------------------------------- */

/* The above algebraic data type is not exposed to the user. Instead, we
expose the following functions. These functions construct a raw document
and compute its Requirement, so as to obtain a document. */

val empty = Document.Empty
fun char(c: Char) = Document.Char(c).also { require(c != '\n') }
val space = Document.Blank(1)
fun string(s: String) = Document.String(s)
fun fancysubstring(s: String, ofs: Int, len: Int, apparentLength: Int) =
    if (len == 0) empty
    else Document.FancyString(s, ofs, len, apparentLength)

fun substring(s: String, ofs: Int, len: Int) = fancysubstring(s, ofs, len, len)
fun fancystring(s: String, apparentLength: Int) = fancysubstring(s, 0, s.length, apparentLength)

/* The following function was stolen from [Batteries]. */
fun utf8_length(s: String): Int {
    fun length_aux(s: String, c: Int, i: Int): Int {
        if (i >= s.length) return c
        val n = s[i]
        val k =
            if (n < 0x80.toChar()) 1 else if (n < 0xe0.toChar()) 2 else if (n < 0xf0.toChar()) 3 else 4
        return length_aux(s, (c + 1), (i + k))
    }
    return length_aux(s, 0, 0)
}

fun utf8string(s: String) = fancystring(s, utf8_length(s))

// fun utf8format(f: String) = Printf.ksprintf utf8string f
val hardline = HardLine
fun blank(n: Int) = if (n == 0) empty else Document.Blank(n)

fun ifflat(doc1: Document, doc2: Document) = IfFlat(doc1, doc2)
/* Avoid nesting [IfFlat] in the left-hand side of [IfFlat], as this
is redundant.
when( doc1) {
        | IfFlat(doc1, _)
        | doc1
    ->
        IfFlat(doc1, doc2)}*/

fun internal_break(i: Int) = ifflat(blank(i), hardline)
val break0 = internal_break(0)
val break1 = internal_break(1)
fun break_(i: Int) = when (i) {
    0 -> break0
    1 -> break1
    else -> internal_break(i)
}

fun cat(x: Document, y: Document) = // ^^
    if (x is Document.Empty) y else if (y is Document.Empty) x else Document.Cat(Requirement(x) + Requirement(y), x, y)

fun nest(i: Int, x: Document) =
    // assert (i >= 0);
    Document.Nest(Requirement(x), i, x)

fun group(x: Document): Document {
    val req = Requirement(x)
    /* Minor optimisation: an infinite Requirement dissolves a group. */
    return if (req.isInfinity) x else Group(req, x)
}

fun align(x: Document) = Document.Align(Requirement(x), x)

fun range(hook: (PointRange) -> Unit, x: Document) = Document.Range(Requirement(x), hook, x)

/*let custom c =
assert (c#Requirement >= 0);
Custom c*/

/** This function expresses the following invariant: if we are in flattening
mode, then we must be within bounds, i.e. the width and ribbon width
constraints must be respected. */
fun ok(state: State, flatten: Boolean) =
    !flatten || state.column <= state.width && state.column <= state.lastIndent + state.ribbon

/* ------------------------------------------------------------------------- */

/* The pretty rendering engine. */

/* The renderer is supposed to behave exactly like Daan Leijen's, although its
implementation is quite radically different, and simpler. Our documents are
constructed eagerly, as opposed to lazily. This means that we pay a large
space overhead, but in return, we get the ability of computing information
bottom-up, as described above, which allows to render documents without
backtracking or buffering. */

/* The [state] record is never copied; it is just threaded through. In
addition to it, the parameters [indent] and [flatten] influence the
manner in which the document is rendered. */

/* The code is written in tail-recursive style, so as to avoid running out of
stack space if the document is very deep. Each [KCons] cell in a
continuation represents a pending call to [pretty]. Each [KRange] cell
represents a pending call to a user-provided range hook. */

sealed class Continuation
object KNil : Continuation()
data class KCons(val indent: Int, val flatten: Boolean, val doc: Document, val cont: Continuation) : Continuation()
data class KRange(val hook: (PointRange) -> Unit, val start: Point, val cont: Continuation) : Continuation()

tailrec fun proceed(output: PrintWriter, state: State, x: Continuation) {
    when (x) {
        is KNil -> Unit
        is KCons -> pretty(output, state, x.indent, x.flatten, x.doc, x.cont)
        is KRange -> {
            var y = x
            while (y is KRange) {
                val finish: Point = state.line to state.column
                y.hook(y.start to finish)
                y = y.cont
            }
            if (y is KCons) {
                pretty(output, state, y.indent, y.flatten, y.doc, y.cont)
            }
        }
    }
}

tailrec fun prettyQ(
    output: PrintWriter,
    state: State,
    indent: Int,
    flatten: Boolean,
    doc: Document
) {
    val queue = ArrayDeque<Continuation>(1024)
    queue.add(KCons(indent, flatten, doc, KNil))

    fun proceed(x: Continuation) {
        queue.push(x)
    }

    fun handle(indent: Int, flatten: Boolean, doc: Document, cont: Continuation) =
        when (doc) {
            is Document.Empty -> {}
            is Document.Char -> {
                output.print(doc.char)
                state.column = state.column + 1
                proceed(cont)
            }
            is Document.String -> {
                output.print(doc.s.substring(0, doc.s.length))
                state.column = state.column + doc.s.length
                /* assert (ok state flatten); */
                proceed(cont)
            }
            is Document.FancyString -> {
                output.print(doc.s.substring(doc.ofs, doc.len))
                state.column = state.column + doc.apperentLength
                /* assert (ok state flatten); */
                proceed(cont)
            }
            is Document.Blank -> {
                output.print(" ".repeat(doc.len))
                state.column = state.column + doc.len
                /* assert (ok state flatten); */
                proceed(cont)
            }
            is Document.HardLine -> {
                /* We cannot be in flattening mode, because a hard line has an [infinity]
                   Requirement, and we attempt to render a group in flattening mode only
                   if this group's Requirement is met. */
                require(!flatten)
                /* Emit a hardline. */
                output.print("\n")
                output.print(" ".repeat(indent))
                state.line = state.line + 1
                state.column = indent
                state.lastIndent = indent
                proceed(cont)
            }
            is Document.IfFlat -> {
                /* Pick an appropriate sub-document, based on the current flattening mode. */
                proceed(KCons(indent, flatten, if (flatten) doc.doc1 else doc.doc2, cont))
            }
            is Document.Cat ->
                /* Push the second document onto the continuation. */
                proceed(
                    KCons(
                        indent, flatten, doc.doc1,
                        KCons(indent, flatten, doc.doc2, cont)
                    )
                )

            is Document.Nest ->
                proceed(KCons(indent + doc.j, flatten, doc.doc, cont))

            is Document.Group -> {
                /* If we already are in flattening mode, stay in flattening mode; we
                are committed to it. If we are not already in flattening mode, we
                have a choice of entering flattening mode. We enter this mode only
                if we know that this group fits on this line without violating the
                width or ribbon width constraints. Thus, we never backtrack. */
                val column = Requirement(state.column) + doc.req
                val flatten2 = flatten || column <= state.width && column <= state.lastIndent + state.ribbon
                proceed(KCons(indent, flatten2, doc.doc, cont))
            }
            is Document.Align ->
                /* The effect of this combinator is to set [indent] to [state.column].
                Usually [indent] is equal to [state.last_indent], hence setting it
                to [state.column] increases it. However, if [nest] has been used
                since the current line began, then this could cause [indent] to
                decrease. */
                /* assert (state.column > state.last_indent); */
                proceed(KCons(state.column, flatten, doc.doc, cont))

            is Document.Range -> {
                val start: Point = state.line to state.column
                proceed(KCons(state.column, flatten, doc.doc, KRange(doc.fn, start, cont)))
            }
            is Document.Custom -> {
                /* Invoke the document's custom rendering function. */
                doc.doc.pretty(output, state, indent, flatten)
                /* Sanity check. */
                // assert(ok state flatten);
                /* __continue. */
                proceed(cont)
            }
        }

    while (queue.isNotEmpty()) {
        val x = queue.pop()
        when (x) {
            is KNil -> return
            is KCons -> {
                handle(x.indent, x.flatten, x.doc, x.cont)
                // queue.addLast(x.cont)
            }
            is KRange -> {
                val finish: Point = state.line to state.column
                x.hook(x.start to finish)
                proceed(x.cont)
            }
        }
    }
}

tailrec fun pretty(
    output: PrintWriter,
    state: State,
    indent: Int,
    flatten: Boolean,
    doc: Document,
    cont: Continuation
) {
    when (doc) {
        is Document.Empty -> proceed(output, state, cont)
        is Document.Char
        -> {
            output.print(doc.char)
            state.column = state.column + 1
            /* assert (ok state flatten); */
            proceed(output, state, cont)
        }
        is Document.String -> {
            output.print(doc.s.substring(0, doc.s.length))
            state.column = state.column + doc.s.length
            /* assert (ok state flatten); */
            proceed(output, state, cont)
        }
        is Document.FancyString -> {
            output.print(doc.s.substring(doc.ofs, doc.len))
            state.column = state.column + doc.apperentLength
            /* assert (ok state flatten); */
            proceed(output, state, cont)
        }
        is Document.Blank -> {
            output.print(" ".repeat(doc.len))
            state.column = state.column + doc.len
            /* assert (ok state flatten); */
            proceed(output, state, cont)
        }
        is Document.HardLine -> {
            /* We cannot be in flattening mode, because a hard line has an [infinity]
               Requirement, and we attempt to render a group in flattening mode only
               if this group's Requirement is met. */
            require(!flatten)
            /* Emit a hardline. */
            output.print("\n")
            output.print(" ".repeat(indent))
            state.line = state.line + 1
            state.column = indent
            state.lastIndent = indent
            proceed(output, state, cont)
        }
        is IfFlat -> {
            /* Pick an appropriate sub-document, based on the current flattening
            mode. */
            pretty(output, state, indent, flatten, if (flatten) doc.doc1 else doc.doc2, cont)
        }
        is Document.Cat ->
            /* Push the second document onto the continuation. */
            pretty(
                output, state, indent, flatten, doc.doc1,
                KCons(indent, flatten, doc.doc2, cont)
            )

        is Document.Nest ->
            pretty(output, state, indent + doc.j, flatten, doc.doc, cont)

        is Group ->
            /* If we already are in flattening mode, stay in flattening mode; we
            are committed to it. If we are not already in flattening mode, we
            have a choice of entering flattening mode. We enter this mode only
            if we know that this group fits on this line without violating the
            width or ribbon width constraints. Thus, we never backtrack. */ {
            val column = Requirement(state.column) + doc.req
            val flatten2 = flatten || column <= state.width && column <= state.lastIndent + state.ribbon
            pretty(output, state, indent, flatten2, doc.doc, cont)
        }
        is Document.Align ->
            /* The effect of this combinator is to set [indent] to [state.column].
            Usually [indent] is equal to [state.last_indent], hence setting it
            to [state.column] increases it. However, if [nest] has been used
            since the current line began, then this could cause [indent] to
            decrease. */
            /* assert (state.column > state.last_indent); */
            pretty(output, state, state.column, flatten, doc.doc, cont)

        is Document.Range -> {
            val start: Point = state.line to state.column
            pretty(output, state, indent, flatten, doc.doc, KRange(doc.fn, start, cont))
        }
        //        is Custom c
        //      -> {
        /* Invoke the document's custom rendering function. */
        //        c#pretty output state indent flatten;
        /* Sanity check. */
        //      assert(ok state flatten);
        /* __continue. */
        //    __continue(output, state, cont)
        // }
    }
}

/* Publish a version of [pretty] that does not take an explicit continuation.
This function may be used by authors of custom documents. We do not expose
the internal [pretty] -- the one that takes a continuation -- because we
wish to simplify the user's life. The price to pay is that calls that go
through a custom document cannot be tail calls. */

fun pretty(output: PrintWriter, state: State, indent: Int, flatten: Boolean, doc: Document) =
    pretty(output, state, indent, flatten, doc, KNil)

fun pretty(doc: Document, width: Int = 80, rfrac: Double = 0.2, indent: Int = 0, flatten: Boolean = false): String =
    pretty(doc, State(width, rfrac), indent, flatten)

fun pretty(doc: Document, state: State, indent: Int = 0, flatten: Boolean = false): String {
    val sw = StringWriter()
    val output = PrintWriter(sw)
    pretty(output, state, indent, flatten, doc, KNil)
    return sw.toString()
}

fun proceedc(output: PrintWriter, cont: List<Document>) {
    if (cont.isEmpty()) return
    cont.first().let {
        compact(output, cont.first(), cont.subList(1, cont.lastIndex)) // inclusive?
    }
}

fun compact(output: PrintWriter, doc: Document) = compact(output, doc, listOf())

tailrec fun compact(output: PrintWriter, doc: Document, cont: List<Document>) {
    when (doc) {
        is Document.Empty -> proceedc(output, cont)
        is Document.Char -> {
            output.print(doc.char)
            proceedc(output, cont)
        }
        is Document.String -> {
            val len = doc.s.length
            output.print(doc.s.substring(0, len))
            proceedc(output, cont)
        }
        is Document.FancyString -> {
            output.print(doc.s.substring(doc.ofs, doc.len))
            proceedc(output, cont)
        }
        is Document.Blank -> {
            output.print(" ".repeat(doc.len))
            proceedc(output, cont)
        }
        is HardLine -> {
            output.print('\n')
            proceedc(output, cont)
        }
        is Document.Cat ->
            proceedc(output, listOf(doc.doc1, doc.doc2) + cont)
        is IfFlat -> compact(output, doc.doc1, cont)
        is Document.Nest -> compact(output, doc.doc, cont)
        is Group -> compact(output, doc.doc, cont)
        is Document.Align -> compact(output, doc.doc, cont)
        is Document.Range -> compact(output, doc.doc, cont)
        // is document.Custom ->
        //    /* Invoke the document's custom rendering function. */
        //    c#compact output;
        // continue output cont
    }
}

val lparen = char('(')
val rparen = char(')')
val langle = char('<')
val rangle = char('>')
val lbrace = char('{')
val rbrace = char('}')
val lbracket = char('[')
val rbracket = char(']')
val squote = char('\'')
val dquote = char('"')
val bquote = char('`')
val semi = char(';')
val colon = char(':')
val comma = char(',')
val dot = char('.')
val sharp = char('#')
val slash = char('/')
val backslash = char('\\')
val equals = char('=')
val qmark = char('?')
val tilde = char('~')
val at = char('@')
val percent = char('%')
val dollar = char('$')
val caret = char('^')
val ampersand = char('&')
val star = char('*')
val plus = char('+')
val minus = char('-')
val underscore = char('_')
val bang = char('!')
val bar = char('|')

fun twice(doc: Document) = cat(doc, doc)

fun repeat(n: Int, doc: Document) =
    when (n) {
        0 -> empty
        1 -> doc
        else -> (1..n).map { doc }.fold(empty, ::cat)
    }

fun precede(l: Document, x: Document) = cat(l, x)
fun precede(l: String, x: Document) = cat(string(l), x)
fun terminate(r: Document, x: Document) = cat(x, r)
fun enclose(l: Document, r: Document, x: Document) = cat(cat(l, x), r)
fun squotes(x: Document) = enclose(squote, x, squote)
fun dquotes(x: Document) = enclose(dquote, x, dquote)
fun bquotes(x: Document) = enclose(bquote, x, bquote)
fun braces(x: Document) = enclose(lbrace, x, rbrace)
fun parens(x: Document) = enclose(lparen, x, rparen)
fun angles(x: Document) = enclose(langle, x, rangle)
fun brackets(x: Document) = enclose(lbracket, x, rbracket)

/** A variant of [fold_left] that keeps track of the element index. */
fun <A, B> foldli(f: (Int, B, A) -> B, accu: B, xs: List<A>): B {
    return xs.foldIndexed(accu, f)
}

/* Working with lists of documents. */

/** We take advantage of the fact that [^^] operates in constant
time, regardless of the size of its arguments. The document
that is constructed is essentially a reversed list (i.e., a
tree that is biased towards the left). This is not a problem;
when pretty-printing this document, the engine will descend
along the left branch, pushing the nodes onto its stack as
it goes down, effectively reversing the list again. */
fun concat(docs: List<Document>) = docs.fold(empty, ::cat)

fun separate(sep: Document, docs: List<Document>): Document =
    foldli({ i, accu: Document, doc: Document -> if (i == 0) doc else cat(cat(accu, sep), doc) }, empty, docs)

fun <T> concat_map(f: (T) -> Document, xs: List<T>) = xs.map(f).reduce(::cat)

fun <T> concat_map(xs: List<T>, f: (T) -> Document) = xs.map(f).reduce(::cat)

fun <T> separate_map(sep: Document, xs: List<T>, f: (T) -> Document) = separate_map(sep, f, xs)

fun <T> separate_map(sep: Document, f: (T) -> Document, xs: List<T>) =
    foldli(
        { i, accu: Document, x: T ->
            if (i == 0) f(x) else cat(cat(accu, sep), f(x))
        },
        empty, xs
    )

fun separate2(sep: Document, last_sep: Document, docs: List<Document>) =
    foldli(
        { i, accu: Document, doc: Document ->
            if (i == 0) doc
            else cat(accu, cat(if (i < docs.size - 1) sep else last_sep, doc))
        },
        empty, docs
    )

fun <T> optional(f: (T) -> Document, x: Optional<T>) = x.map(f).orElse(empty)

/* This variant of [String.index_from] returns an option. */
fun index_from(s: String, i: Int, c: Char) = s.indexOf(c, i)

/* [lines s] chops the string [s] into a list of lines, which are turned
into documents. */
fun lines(s: String) = s.split("\n").map { string(it) }

fun arbitrary_string(s: String) = separate(break1, lines(s))

/** [split ok s] splits the string [s] at every occurrence of a character
that satisfies the predicate [ok]. The substrings thus obtained are
turned into documents, and a list of documents is returned. No information
is lost: the concatenation of the documents yields the original string.
This code is not UTF-8 aware. */
fun split(chars: (Char) -> Boolean, s: String): List<Document> {
    val d = arrayListOf<Document>()
    var lastIndex = 0
    s.toCharArray().forEachIndexed { idx, c ->
        if (chars(c)) {
            d.add(substring(s, lastIndex, idx))
            lastIndex = idx
        }
    }
    if (lastIndex != s.length - 1) {
        d.add(substring(s, lastIndex, s.length))
    }
    return d
}

/** [words s] chops the string [s] into a list of words, which are turned
into documents. */
fun words(s: String) = s.split("\\s").map { it.strip() }.map { ::string }

fun <T> flow_map(sep: Document, docs: List<T>, f: (T) -> Document) = flow_map(sep, f, docs)

fun <T> flow_map(sep: Document, f: (T) -> Document, docs: List<T>) =
    foldli(
        { i: Int, accu: Document, doc: T ->
            if (i == 0) f(doc)
            else cat(
                accu,
                /* This idiom allows beginning a new line if [doc] does not
                fit on the current line. */
                group(
                    cat(sep, f(doc))
                )
            )
        }, empty, docs
    )

fun flow(sep: Document, docs: List<Document>) = flow_map(sep, { it }, docs)
fun url(s: String) = flow(break_(0), split({ it == '/' || it == '.' }, s))

/* -------------------------------------------------------------------------- */
/* Alignment and indentation. */

fun hang(i: Int, d: Document) = align(nest(i, d))

infix fun Document.slash(y: Document) = cat(cat(this, break1), y)
infix fun Document.`^^`(y: Document) = cat(this, y)
fun prefix(n: Int, b: Int, x: Document, y: Document) = group(x `^^` nest(n, (break_(b) `^^` y)))
infix fun Document.prefixed(y: Document) = prefix(2, 1, this, y) // ^//^
fun jump(n: Int, b: Int, y: Document) = group(nest(n, break_(b) `^^` y))

fun `infix`(n: Int, b: Int, op: Document, x: Document, y: Document) = prefix(n, b, x `^^` blank(b) `^^` op, y)

fun surround(n: Int, b: Int, opening: Document, contents: Document, closing: Document) =
    group(opening `^^` nest(n, (break_(b) `^^` contents) `^^` break_(b) `^^` closing))

fun soft_surround(n: Int, b: Int, opening: Document, contents: Document, closing: Document) =
    group(
        opening `^^` nest(n, group(break_(b) `^^` contents) `^^` group((break_(b) `^^` closing)))
    )

fun surround_separate(
    n: Int,
    b: Int,
    `void`: Document,
    opening: Document,
    sep: Document,
    closing: Document,
    docs: List<Document>
) =
    if (docs.isEmpty()) `void`
    else surround(n, b, opening, separate(sep, docs), closing)

fun <T> surround_separate_map(
    n: Int,
    b: Int,
    `void`: Document,
    opening: Document,
    sep: Document,
    closing: Document,
    xs: List<T>,
    f: (T) -> Document
) = if (xs.isEmpty()) `void` else surround(n, b, opening, separate_map(sep, f, xs), closing)
