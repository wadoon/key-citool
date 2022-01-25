package org.key_project.core.doc

import org.key_project.core.doc.document.*
import java.io.PrintWriter
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*

/** A point is a pair of a line number and a column number. */
typealias Point = Pair<Int, Int>

typealias int = Int
typealias bool = Boolean

/** A range is a pair of points. */
typealias range = Pair<Point, Point>

/** A type of integers with infinity. Infinity is encoded as [max_int]. */
@JvmInline
value class Requirement(val x: Int) {
    val isInfinity: Boolean
        get() = x == infinity.x


    /** Addition of integers with infinity. */
    operator fun plus(y: Requirement) =
        if (isInfinity || y.isInfinity) infinity
        else Requirement(x + y.x)

    /* Comparison between an integer with infinity and a normal integer. */
    operator fun compareTo(y: Int) = x - y // x<=ys

}
typealias requirement = Requirement

val infinity = Requirement(Integer.MAX_VALUE)


/* ------------------------------------------------------------------------- */
/** A uniform interface for output channels. */
typealias output = PrintWriter


/* ------------------------------------------------------------------------- */

/* Printing blank space. This is used both internally (to emit indentation
characters) and via the public combinator [blank]. */

val blank_length = 80

val blank_buffer = " ".repeat(blank_length)

fun blanks(output: output, n: Int): Unit {
    if (n <= 0) ""
    else if (n <= blank_length)
        output.print(blank_buffer.substring(0, n))
    else {
        output.print(blank_buffer.substring(0, blank_length))
        output.print(" ".repeat(n - blank_length))
    }
}

/** The rendering engine maintains the following internal state. Its structure
is subject to change in future versions of the library. Nevertheless, it is
exposed to the user who wishes to define custom documents. */
data class state(
    /** The line width. This parameter is fixed throughout the execution of
    the renderer. */
    val width: int,
    /** The ribbon width. This parameter is fixed throughout the execution of
    the renderer. */
    val ribbon: int,
    /** The number of blanks that were printed at the beginning of the current
    line. This field is updated (only) when a hardline is emitted. It is
    used (only) to determine whether the ribbon width constraint is
    respected. */
    var last_indent: int,
    /** The current line. This field is updated (only) when a hardline is
    emitted. It is not used by the pretty-printing engine itself. */
    var line: int,
    /** The current column. This field must be updated whenever something is
    sent to the output channel. It is used (only) to determine whether the
    width constraint is respected. */
    var column: int
)


/* ------------------------------------------------------------------------- */

/** [initial rfrac width] creates a fresh initial state. */
fun initial(rfrac: Double, width: Int) = state(
    width = width,
    ribbon = max(0, min(width, (width * rfrac) as Int)),
    last_indent = 0,
    line = 0,
    column = 0
)

/* ------------------------------------------------------------------------- */

/** A custom document is defined by implementing the following methods. */

interface custom {

    /** A custom document must publish the width (i.e., the number of columns)
    that it would like to occupy if it is printed on a single line (that is,
    in flattening mode). The special value [infinity] means that this
    document cannot be printed on a single line; this value causes any
    groups that contain this document to be dissolved. This method should
    in principle work in constant time. */
    fun requirement(): requirement

    /** The method [pretty] is used by the main rendering algorithm. It has
    access to the output channel and to the algorithm's internal state, as
    described above. In addition, it receives the current indentation level
    and the current flattening mode (on or off). If flattening mode is on,
    then the document must be printed on a single line, in a manner that is
    consistent with the requirement that was published ahead of time. If
    flattening mode is off, then there is no such obligation. The state must
    be updated in a manner that is consistent with what is sent to the
    output channel. */
    fun pretty(o: output, s: state, i: int, b: Boolean)

    /** The method [compact] is used by the compact rendering algorithm. It has
    access to the output channel only. */
    fun compact(o: output)
}


/** Here is the algebraic data type of documents. It is analogous to Daan
Leijen's version, but the binary constructor [Union] is replaced with
the unary constructor [Group], and the constant [Line] is replaced with
more general constructions, namely [IfFlat], which provides alternative
forms depending on the current flattening mode, and [HardLine], which
represents a newline character, and causes a failure in flattening mode. */

sealed class document {
    /** [Empty] is the empty document. */
    object Empty : document()

    /** [Char c] is a document that consists of the single character [c]. We
    enforce the invariant that [c] is not a newline character. */

    data class Char(val char: kotlin.Char) : document()

    /** [String s] is a document that consists of just the string [s]. We
    assume, but do not check, that this string does not contain a newline
    character. [String] is a special case of [FancyString], which takes up
    less space in memory. */
    data class String(val s: kotlin.String) : document()


    /** [FancyString (s, ofs, len, apparent_length)] is a (portion of a) string
    that may contain fancy characters: color escape characters, UTF-8 or
    multi-byte characters, etc. Thus, the apparent length (which corresponds
    to what will be visible on screen) differs from the length (which is a
    number of bytes, and is reported by [String.length]). We assume, but do
    not check, that fancystrings do not contain a newline character. */

    data class FancyString(val s: kotlin.String, val ofs: int, val len: int, val apperent_length: int) : document()

    /** [Blank n] is a document that consists of [len] blank characters. */

    data class Blank(val len: int) : document()

    /** When in flattening mode, [IfFlat (d1, d2)] turns into the document
    [d1]. When not in flattening mode, it turns into the document [d2]. */

    data class IfFlat(val doc1: document, val doc2: document) : document()


    /** When in flattening mode, [HardLine] causes a failure, which requires
    backtracking all the way until the stack is empty. When not in flattening
    mode, it represents a newline character, followed with an appropriate
    number of indentation. A common way of using [HardLine] is to only use it
    directly within the right branch of an [IfFlat] construct. */

    object HardLine : document()

    /** The following constructors store their space requirement. This is the
    document's apparent length, if printed in flattening mode. This
    information is computed in a bottom-up manner when the document is
    constructed. */

    /** In other words, the space requirement is the number of columns that the
    document needs in order to fit on a single line. We express this value in
    the set of `integers extended with infinity', and use the value
    [infinity] to indicate that the document cannot be printed on a single
    line. */

    /** Storing this information at [Group] nodes is crucial, as it allows us to
    avoid backtracking and buffering. */

    /** Storing this information at other nodes allows the function [requirement]
    to operate in constant time. This means that the bottom-up computation of
    requirements takes linear time. */

    /** [Cat (req, doc1, doc2)] is the concatenation of the documents [doc1] and
    [doc2]. The space requirement [req] is the sum of the requirements of
    [doc1] and [doc2]. */

    data class Cat(val r: requirement, val doc1: document, val doc2: document) : document()

    /** [Nest (req, j, doc)] is the document [doc], in which the indentation
    level has been increased by [j], that is, in which [j] blanks have been
    inserted after every newline character. The space requirement [req] is
    the same as the requirement of [doc]. */

    data class Nest(val r: requirement, val j: int, val doc: document) : document()

    /** [Group (req, doc)] represents an alternative: it is either a flattened
    form of [doc], in which occurrences of [Group] disappear and occurrences
    of [IfFlat] resolve to their left branch, or [doc] itself. The space
    requirement [req] is the same as the requirement of [doc]. */

    data class Group(val req: requirement, val doc: document) : document()

    /** [Align (req, doc)] increases the indentation level to reach the current
    column.  Thus, the document [doc] is rendered within a box whose upper
    left corner is the current position. The space requirement [req] is the
    same as the requirement of [doc]. */

    data class Align(val req: requirement, val doc: document) : document()

    /** [Range (req, hook, doc)] is printed like [doc]. After it is printed, the
    function [hook] is applied to the range that is occupied by [doc] in the
    output. */

    data class Range(val req: requirement, val fn: (range) -> Unit, val doc: document) : document()

    /** [Custom (req, f)] is a document whose appearance is user-defined. */
//    class Custom of custom: document()

}

/* Retrieving or computing the space requirement of a document. */
fun requirement(x: document): Requirement =
    when (x) {
        is document.Empty -> Requirement(0)
        is document.Char -> Requirement(1)
        is document.String -> Requirement(x.s.length)
        is document.FancyString -> Requirement(x.apperent_length)
        is document.Blank -> Requirement(x.len)
        /* In flattening mode, the requirement of [ifflat x y] is just the
        requirement of its flat version, [x]. */
        /* The smart constructor [ifflat] ensures that [IfFlat] is never nested
        in the left-hand side of [IfFlat], so this recursive call is not a
        problem; the function [requirement] has constant time complexity. */
        is document.IfFlat -> requirement(x.doc1)
        /* A hard line cannot be printed in flattening mode. */
        is HardLine -> infinity

        /* These nodes store their requirement -- which is computed when the
        node is constructed -- so as to allow us to answer in constant time
        here. */
        is document.Cat -> x.r
        is document.Nest -> x.r
        is document.Group -> x.req
        is document.Align -> x.req
        is document.Range -> x.req
        // | Custom c -> c#requirement
        else -> throw IllegalArgumentException()
    }
/* ------------------------------------------------------------------------- */

/* The above algebraic data type is not exposed to the user. Instead, we
expose the following functions. These functions construct a raw document
and compute its requirement, so as to obtain a document. */

val empty = document.Empty
fun char(c: Char) = document.Char(c).also { require(c != '\n') }
val space = document.Blank(1)
fun string(s: String) = document.String(s)
fun fancysubstring(s: String, ofs: int, len: int, apparent_length: int) =
    if (len == 0) empty
    else document.FancyString(s, ofs, len, apparent_length)

fun substring(s: String, ofs: int, len: int) = fancysubstring(s, ofs, len, len)
fun fancystring(s: String, apparent_length: int) = fancysubstring(s, 0, s.length, apparent_length)

/* The following function was stolen from [Batteries]. */
fun utf8_length(s: String): int {
    fun length_aux(s: String, c: int, i: int): int {
        if (i >= s.length) return c
        val n = s[i]
        val k =
            if (n < 0x80.toChar()) 1 else
                if (n < 0xe0.toChar()) 2 else
                    if (n < 0xf0.toChar()) 3 else 4
        return length_aux(s, (c + 1), (i + k))
    }
    return length_aux(s, 0, 0)
}

fun utf8string(s: String) = fancystring(s, utf8_length(s))

//fun utf8format(f: String) = Printf.ksprintf utf8string f
val hardline = document.HardLine
fun blank(n: int) = if (n == 0) empty else document.Blank(n)

fun ifflat(doc1: document, doc2: document) = document.IfFlat(doc1, doc2)
/* Avoid nesting [IfFlat] in the left-hand side of [IfFlat], as this
is redundant.
when( doc1) {
        | IfFlat(doc1, _)
        | doc1
    ->
        IfFlat(doc1, doc2)}*/

fun internal_break(i: int) = ifflat(blank(i), hardline)
val break0 = internal_break(0)
val break1 = internal_break(1)
fun break_(i: int) = when (i) {
    0 -> break0
    1 -> break1
    else -> internal_break(i)
}

fun cat(x: document, y: document) =// ^^
    if (x is Empty) y else if (y is Empty) x else document.Cat(requirement(x) + requirement(y), x, y)


fun nest(i: int, x: document) =
    //assert (i >= 0);
    document.Nest(requirement(x), i, x)

fun group(x: document): document {
    val req = requirement(x)
    /* Minor optimisation: an infinite requirement dissolves a group. */
    return if (req.isInfinity) x else document.Group(req, x)
}


fun align(x: document) = document.Align(requirement(x), x)

fun range(hook: (range) -> Unit, x: document) = document.Range(requirement(x), hook, x)


/*let custom c =
assert (c#requirement >= 0);
Custom c*/



/** This function expresses the following invariant: if we are in flattening
mode, then we must be within bounds, i.e. the width and ribbon width
constraints must be respected. */

fun ok(state: state, flatten: bool) =
    !flatten || state.column <= state.width && state.column <= state.last_indent + state.ribbon


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

sealed class cont
object KNil : cont()
data class KCons(val indent: int, val flatten: bool, val doc: document, val cont: cont) : cont()
data class KRange(val hook: (range) -> Unit, val start: Point, val cont: cont) : cont()


fun __continue(output: output, state: state, x: cont) {
    when (x) {
        is KNil -> Unit
        is KCons -> pretty(output, state, x.indent, x.flatten, x.doc, x.cont)
        is KRange -> {
            val finish: Point = state.line to state.column
            x.hook(x.start to finish)
            __continue(output, state, x.cont)
        }
    }
}

fun pretty(
    output: output, state: state,
    indent: int, flatten: bool, doc: document,
    cont: cont
): Unit {
    when (doc) {
        is Empty -> __continue(output, state, cont)
        is document.Char
        -> {
            output.print(doc.char)
            state.column = state.column + 1
            /* assert (ok state flatten); */
            __continue(output, state, cont)
        }
        is document.String -> {
            output.print(doc.s.substring(0, doc.s.length))
            state.column = state.column + doc.s.length
            /* assert (ok state flatten); */
            __continue(output, state, cont)
        }
        is document.FancyString -> {
            output.print(doc.s.substring(doc.ofs, doc.len))
            state.column = state.column + doc.apperent_length
            /* assert (ok state flatten); */
            __continue(output, state, cont)
        }
        is Blank -> {
            output.print(" ".repeat(doc.len))
            state.column = state.column + doc.len
            /* assert (ok state flatten); */
            __continue(output, state, cont)
        }
        is HardLine -> {
            /* We cannot be in flattening mode, because a hard line has an [infinity]
               requirement, and we attempt to render a group in flattening mode only
               if this group's requirement is met. */
            require(!flatten)
            /* Emit a hardline. */
            output.print("\n")
            output.print(" ".repeat(indent))
            state.line = state.line + 1
            state.column = indent
            state.last_indent = indent
            __continue(output, state, cont)
        }
        is IfFlat -> {
            /* Pick an appropriate sub-document, based on the current flattening
            mode. */
            pretty(output, state, indent, flatten, if (flatten) doc.doc1 else doc.doc2, cont)
        }
        is Cat ->
            /* Push the second document onto the continuation. */
            pretty(
                output, state, indent, flatten, doc.doc1,
                KCons(indent, flatten, doc.doc2, cont)
            )

        is Nest ->
            pretty(output, state, indent + doc.j, flatten, doc, cont)

        is Group ->
            /* If we already are in flattening mode, stay in flattening mode; we
            are committed to it. If we are not already in flattening mode, we
            have a choice of entering flattening mode. We enter this mode only
            if we know that this group fits on this line without violating the
            width or ribbon width constraints. Thus, we never backtrack. */ {
            val column = Requirement(state.column) + doc.req
            val flatten2 = flatten || column <= state.width && column <= state.last_indent + state.ribbon
            pretty(output, state, indent, flatten2, doc, cont)
        }
        is Align ->
            /* The effect of this combinator is to set [indent] to [state.column].
            Usually [indent] is equal to [state.last_indent], hence setting it
            to [state.column] increases it. However, if [nest] has been used
            since the current line began, then this could cause [indent] to
            decrease. */
            /* assert (state.column > state.last_indent); */
            pretty(output, state, state.column, flatten, doc, cont)

        is Range -> {
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
        //}
    }
}

/* Publish a version of [pretty] that does not take an explicit continuation.
This function may be used by authors of custom documents. We do not expose
the internal [pretty] -- the one that takes a continuation -- because we
wish to simplify the user's life. The price to pay is that calls that go
through a custom document cannot be tail calls. */

fun pretty(output: output, state: state, indent: int, flatten: Boolean, doc: document) =
    pretty(output, state, indent, flatten, doc, KNil)


fun __continue_compact(output: output, cont: List<document>) {
    if (cont.isEmpty()) return
    cont.first().let {
        compact(output, cont.first(), cont.subList(1, cont.lastIndex))//inclusive?
    }
}

fun compact(output: output, doc: document) = compact(output, doc, listOf())

tailrec fun compact(output: output, doc: document, cont: List<document>) {
    when (doc) {
        is document.Empty -> __continue_compact(output, cont)
        is document.Char -> {
            output.print(doc.char)
            __continue_compact(output, cont)
        }
        is document.String -> {
            val len = doc.s.length
            output.print(doc.s.substring(0, len))
            __continue_compact(output, cont)
        }
        is document.FancyString -> {
            output.print(doc.s.substring(doc.ofs, doc.len))
            __continue_compact(output, cont)
        }
        is document.Blank -> {
            output.print(" ".repeat(doc.len))
            __continue_compact(output, cont)
        }
        is HardLine -> {
            output.print('\n')
            __continue_compact(output, cont)
        }
        is document.Cat ->
            __continue_compact(output, listOf(doc.doc1, doc.doc2) + cont)
        is IfFlat -> compact(output, doc.doc1, cont)
        is document.Nest -> compact(output, doc.doc, cont)
        is document.Group -> compact(output, doc.doc, cont)
        is document.Align -> compact(output, doc.doc, cont)
        is document.Range -> compact(output, doc.doc, cont)
        //is document.Custom ->
        //    /* Invoke the document's custom rendering function. */
        //    c#compact output;
        //continue output cont
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

fun twice(doc: document) = cat(doc, doc)

fun repeat(n: int, doc: document) =
    when (n) {
        0 -> empty
        1 -> doc
        else -> (1..n).map { doc }.fold(empty, ::cat)
    }


fun precede(l: document, x: document) = cat(l, x)
fun terminate(r: document, x: document) = cat(x, r)
fun enclose(l: document, r: document, x: document) = cat(cat(l, x), r)
fun squotes(x: document) = enclose(squote, x, squote)
fun dquotes(x: document) = enclose(dquote, x, dquote)
fun bquotes(x: document) = enclose(bquote, x, bquote)
fun braces(x: document) = enclose(lbrace, x, rbrace)
fun parens(x: document) = enclose(lparen, x, rparen)
fun angles(x: document) = enclose(langle, x, rangle)
fun brackets(x: document) = enclose(lbracket, x, rbracket)


/** A variant of [fold_left] that keeps track of the element index. */
fun <A, B> foldli(f: (int, B, A) -> B, accu: B, xs: List<A>): B {
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
fun concat(docs: List<document>) = docs.fold(empty, ::cat)

fun separate(sep: document, docs: List<document>): document =
    foldli({ i, accu: document, doc: document -> if (i == 0) doc else cat(cat(accu, sep), doc) }, empty, docs)

fun <T> concat_map(f: (T) -> document, xs: List<T>) = xs.map(f).reduce(::cat)

fun <T> separate_map(sep: document, f: (T) -> document, xs: List<T>) =
    foldli(
        { i, accu: document, x: T ->
            if (i == 0) f(x) else cat(cat(accu, sep), f(x))
        },
        empty, xs
    )

fun separate2(sep: document, last_sep: document, docs: List<document>) =
    foldli(
        { i, accu: document, doc: document ->
            if (i == 0) doc
            else cat(accu, cat(if (i < docs.size - 1) sep else last_sep, doc))
        },
        empty, docs
    )

fun <T> optional(f: (T) -> document, x: Optional<T>) = x.map(f).orElse(empty)

/* This variant of [String.index_from] returns an option. */
fun index_from(s: String, i: int, c: Char) = s.indexOf(c, i)


/* [lines s] chops the string [s] into a list of lines, which are turned
into documents. */
fun lines(s: String) = s.split("\n").map { string(it) }

fun arbitrary_string(s: String) = separate(break1, lines(s))


/** [split ok s] splits the string [s] at every occurrence of a character
that satisfies the predicate [ok]. The substrings thus obtained are
turned into documents, and a list of documents is returned. No information
is lost: the concatenation of the documents yields the original string.
This code is not UTF-8 aware. */
fun split(chars: (Char) -> Boolean, s: String): List<document> {
    val d = arrayListOf<document>();
    var lastIndex = 0;
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


fun <T> flow_map(sep: document, f: (T) -> document, docs: List<T>) =
    foldli(
        { i: int, accu: document, doc: T ->
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

fun flow(sep: document, docs: List<document>) = flow_map(sep, { it }, docs)
fun url(s: String) = flow(break_(0), split({ it == '/' || it == '.' }, s))

/* -------------------------------------------------------------------------- */
/* Alignment and indentation. */

fun hang(i: int, d: document) = align(nest(i, d))


infix fun document.slash(y: document) = cat(cat(this, break1), y)
infix fun document.`^^`(y: document) = cat(this, y)
fun prefix(n: int, b: int, x: document, y: document) = group(x `^^` nest(n, (break_(b) `^^` y)))
infix fun document.prefixed(y: document) = prefix(2, 1, this, y)//^//^
fun jump(n: int, b: int, y: document) = group(nest(n, break_(b) `^^` y))

fun `infix`(n: int, b: int, op: document, x: document, y: document) = prefix(n, b, x `^^` blank(b) `^^` op, y)

fun surround(n: int, b: int, opening: document, contents: document, closing: document) =
    group(opening `^^` nest(n, (break_(b) `^^` contents) `^^` break_(b) `^^` closing))

fun soft_surround(n: int, b: int, opening: document, contents: document, closing: document) =
    group(
        opening `^^` nest(n, group(break_(b) `^^` contents) `^^` group((break_(b) `^^` closing)))
    )

fun surround_separate(
    n: int,
    b: int,
    `void`: document,
    opening: document,
    sep: document,
    closing: document,
    docs: List<document>
) =
    if (docs.isEmpty()) `void`
    else surround(n, b, opening, separate(sep, docs), closing)

fun <T> surround_separate_map(
    n: int, b: int, `void`: document, opening: document, sep: document, closing: document,
    f: (T) -> document, xs: List<T>
) = if (xs.isEmpty()) `void` else surround(n, b, opening, separate_map(sep, f, xs), closing)
