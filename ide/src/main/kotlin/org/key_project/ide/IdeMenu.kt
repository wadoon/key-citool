package org.key_project.ide

import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem

class IdeMenu(val ctx: Context) {
    val file = Menu("File")
    val edit = Menu("Edit")
    val view = Menu("View")
    val tools = Menu("Tools")
    val recentFiles = Menu("Recent files")
    val ui = MenuBar(file, edit, view, tools)

    val main by ctx.ref<MainView>()

    init {
        val rf = ctx.get<RecentFiles>().files
        rf.addListener(ListChangeListener { updateRecentFiles() })
        updateRecentFiles()

        val config = ctx.get<UserConfig>()

        val actionSaveAs = config.createItem("save-as") { main.saveAs() }
        val actionSave = config.createItem("save") { main.save() }
        val actionNew = config.createItem("new") { main.createCodeEditor() }
        val actionRun = config.createItem("run") { }
        val actionOpenConfig = config.createItem("open-config") {
            main.open(ConfigurationPaths.userConfig)
        }
        val actionNewWindow = config.createItem("new-window") { main.newWindow() }
        val actionEditorToNewWindow = config.createItem("new-window-editor") { main.newWindow(main.currentEditor) }
        val actionOpen = config.createItem("open") { main.open() }
        val actionClose = config.createItem("close") { main.close() }
        val actionIncrFontSize = config.createItem("incr-font-size") { main.increaseFontSize() }
        val actionDecrFontSize = config.createItem("decr-font-size") { main.decreaseFontSize() }
        val actionMoveEditorToLeft = config.createItem("editor-move-left") { main.editorToTheLeft() }
        val actionMoveEditorToRight = config.createItem("editor-move-right") { main.editorToTheRight() }

        file.items.setAll(
            actionNew,
            actionOpen,
            recentFiles,
            SeparatorMenuItem(),
            actionSave,
            actionSaveAs,
            SeparatorMenuItem(),
            actionClose,
        )
        view.items.setAll(
            actionIncrFontSize,
            actionDecrFontSize,
            SeparatorMenuItem(),
            actionNewWindow,
            SeparatorMenuItem(),
            actionMoveEditorToLeft,
            actionMoveEditorToRight,
            actionEditorToNewWindow
        )
        tools.items.setAll(actionRun, actionOpenConfig)
    }

    private fun updateRecentFiles() {
        val rf = ctx.get<RecentFiles>().files
        recentFiles.items.setAll(
            rf.map { p ->
                val mi = MenuItem(p.fileName.toString())
                mi.onAction = EventHandler { main.open(p) }
                mi
            }
        )
    }
}
