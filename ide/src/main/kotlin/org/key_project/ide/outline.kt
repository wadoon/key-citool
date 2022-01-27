package org.key_project.ide

import javafx.scene.control.TreeView
import tornadofx.View

class FileOutline(ctx: Context) : View("Outline") {
    val outlineTree = TreeView<OutlineEntry>()
    override val root = outlineTree
    init {
        ctx.register(this)
    }
}

class OutlineEntry(
    val title: String,
    val editor: Editor,
    val caretPosition: Int? = null
) {
    override fun toString(): String = title
}