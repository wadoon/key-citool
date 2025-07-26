package io.github.wadoon.keycitool

/**
 * Helper and constants for printing ANSI codes on the terminal.
 */
@Suppress("unused")
object Ansi {
    const val ESC = 27.toChar()
    const val RED = 31
    const val GREEN = 32
    const val YELLOW = 33
    const val BLUE = 34
    const val MAGENTA = 35
    const val CYAN = 36
    const val WHITE = 37

    fun color(s: Any, fg: Int, bg: Int) = "$ESC[${fg}m$ESC[${bg + 10}m$s$ESC[0m"
    fun colorfg(s: Any, c: Int) = "$ESC[${c}m$s$ESC[0m"
    fun colorbg(s: Any, c: Int) = "$ESC[${c + 10}m$s$ESC[0m"

    var currentPrintLevel = 0
    var verbose = false
    var useColor = true

    fun printBlock(message: String, f: () -> Unit) {
        info(message)
        currentPrintLevel++
        f()
        currentPrintLevel--
    }

    fun printm(message: String, fg: Int? = null, bg: Int? = null) {
        print("  ".repeat(currentPrintLevel))
        val m =
            when {
                useColor -> message
                fg != null && bg != null -> color(message, fg, bg)
                fg != null -> colorfg(message, fg)
                bg != null -> colorbg(message, bg)
                else -> message
            }
        println(m)
    }

    fun err(message: String) = printm("[ERR ] $message", fg = RED)
    fun fail(message: String) = printm("[FAIL] $message", fg = WHITE, bg = RED)
    fun warn(message: String) = printm("[WARN] $message", fg = YELLOW)
    fun info(message: String) = printm("[FINE] $message", fg = BLUE)
    fun fine(message: String) = printm("[OK  ] $message", fg = GREEN)
    fun debug(message: String) =
        if (verbose) printm("[    ] $message", fg = GREEN) else Unit
}