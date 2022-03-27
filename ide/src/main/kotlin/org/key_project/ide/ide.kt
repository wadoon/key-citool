package org.key_project.ide

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.stage.FileChooser
import javafx.stage.Stage
import tornadofx.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.writeText

class MainView() : View() {
    val context = ROOT_CONTEXT

    //region controllers
    val menu = IdeMenu(context)
    val fileNavigator = FileNavigator(context)
    val outline = FileOutline(context)
    val statusBar = StatusBar(context)
    val problems = IssueList(context)
    //endregion

    val editors = SplitPane()

    val paneNavigation = drawer(multiselect = true) {
        item("Files", null, expanded = true, showHeader = true) {
            this.add(fileNavigator)
        }

        item("Outline") {
            this.add(outline)
        }
    }

    val paneTool = drawer(side = Side.BOTTOM) {

    }


    override val root = borderpane {
        center = editors
        left = paneNavigation
        bottom = paneTool
    }

    val openEditors = arrayListOf<Editor>()
    val editorTabPanes: List<TabPane>
        get() = editors.items.filterIsInstance<TabPane>()

    val currentEditorProperty = SimpleObjectProperty<Editor>(this, "currentEditor", null)
    var currentEditor by currentEditorProperty

    init {
        context.register(this)

        editors.orientation = Orientation.HORIZONTAL

        //paneNavigation.tabs.setAll(outline.tab, fileNavigator.tab)
        editors.items.addAll(editorTabPanes)


        root.top = menu.ui
        root.bottom = statusBar.ui
        //paneNavigation.content = paneTool.ui
        //paneTool.content = editors
        //root.center = paneNavigation.ui

        val appData = context.get<ApplicationData>()
        fileNavigator.rootFile = Paths.get(appData.lastNavigatorPath)
        editors.items.add(createEditorTabPane())

        //paneTool.tabClosingPolicy = TabPane.TabClosingPolicy.ALL_TABS
        /*paneTool.tabs.addListener(ChangeListener({ _, _, _ ->
            vSplit.setDividerPosition(0,1)
        })*/

//        paneTool.ui.centerProperty().addListener()

        //addToolPanel(problems)
    }

    private fun addToolPanel() {
        //paneTool.tabs.add(tab.tab)
    }

    fun publishMessage(status: String, graphic: Node? = null) {
        statusBar.message = status
        statusBar.graphic = graphic
    }

    private fun createEditorTabPane(): TabPane = TabPane().also { tabPane ->
        tabPane.tabs.addListener(ListChangeListener { onHandleEmptyTabs(tabPane) })
    }

    fun createCodeEditor() {
        addEditorTab(Editor(context))
    }

    fun addEditorTab(editor: Editor) {
        val tabPanel = editorTabPanes.last()
        addEditorTab(editor, tabPanel)
    }

    fun addEditorTab(editor: Editor, tabPanel: TabPane) {
        val tab = Tab(editor.title.value, editor.ui)
        tab.onCloseRequest = EventHandler { evt -> onTabCloseRequest(evt, editor) }
        tabPanel.tabs.add(tab)
        editor.title.addListener { _, _, new -> tab.text = new }
        openEditors.add(editor)
        editor.ui.focusedProperty().addListener { _, _, new -> if (new) currentEditor = editor }
        currentEditor = editor
        editor.ui.requestFocus()
    }

    private fun onTabCloseRequest(evt: Event, editor: Editor) {
        if (editor.dirty && !showTabCloseConfirmation()) {
            evt.consume()
        }
    }

    private fun showTabCloseConfirmation(): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.contentText = "Text is edited and not saved. Close anway?"
        val resp = alert.showAndWait()
        val cancel = resp.isEmpty || (resp.isPresent && resp.get() != ButtonType.OK)
        return !cancel
    }

    private fun onHandleEmptyTabs(tabPane: TabPane) {
        if (tabPane.tabs.isEmpty() && editorTabPanes.size > 2) {
            editors.items.remove(tabPane)
        }
    }

    fun closeEditorTab(editor: Editor? = currentEditor) {}

    fun newWindow(): MainView {
        val stage = Stage()
        val ctx = Context()

        ctx.register(context.get<UserConfig>())
        ctx.register(context.get<ApplicationData>())
        ctx.register(context.get<RecentFiles>())

        val main = MainView()
        main.root.styleClass.addAll("root", context.get<UserConfig>().theme)
        val scene = Scene(main.root, 400.0, 400.0)
        context.get<ThemeManager>().installCss(scene)
        stage.hookGlobalShortcuts()
        stage.scene = scene
        stage.show()
        return main
    }

    fun saveAs(editor: Editor? = currentEditor) =
        editor?.also {
            val fileChooser = FileChooser()
            fileChooser.showSaveDialog(currentWindow)?.let { new ->
                editor.filename = new.toPath()
                save(editor)
            }
        }

    fun save(editor: Editor? = currentEditor) {
        editor?.also { editor ->
            editor.filename?.also { filename ->
                filename.writeText(editor.editor.text)
            }
        }
    }

    fun open() {
        val fc = FileChooser()
        fc.showOpenDialog(currentWindow)?.let { file ->
            open(file.toPath())
        }
    }

    val recentFiles get() = context.get<RecentFiles>().files

    fun open(f: Path) {
        if (f !in recentFiles) {
            recentFiles.add(f)
        }
        val editor = Editor(context)
        editor.filename = f
        editor.editor.insertText(0, f.readText())
        editor.dirty = false
        addEditorTab(editor)
        publishMessage("Open $f")
    }

    fun addEditorTabPane(): TabPane {
        val oldDividers = editors.dividerPositions.copyOf(editors.dividerPositions.size + 1)
        val pane = createEditorTabPane().also { editors.items.add(it) }
        val newDividiers = editors.dividerPositions.copyOf()
        oldDividers[oldDividers.lastIndex] = newDividiers.last()
        editors.setDividerPositions(*oldDividers)
        return pane
    }

    fun editorToTheRight(editor: Editor? = currentEditor) {
        val (tabPane, tab) = getTabPane(editor)
        if (tabPane != null) {
            val tabIndex = editorTabPanes.indexOf(tabPane)
            if (tabPane == editorTabPanes.last()) {
                addEditorTabPane()
            }
            val target = editorTabPanes[tabIndex + 1]
            tabPane.tabs.remove(tab)
            target.tabs.add(tab)
        }
    }

    fun editorToTheLeft(editor: Editor? = currentEditor) {
        val (tabPane, tab) = getTabPane(editor)
        if (tabPane != null) {
            val tabIndex = editorTabPanes.indexOf(tabPane)
            if (tabPane == editorTabPanes.first()) {
                createEditorTabPane().also { editors.items.add(1, it) }
            }
            val target = editorTabPanes[tabIndex - 1]
            tabPane.tabs.remove(tab)
            target.tabs.add(tab)
        }
    }

    private fun getTabPane(editor: Editor?): Pair<TabPane?, Tab?> {
        editorTabPanes.forEach { pane ->
            pane.tabs.forEach { tab ->
                if (tab.content == editor?.ui) {
                    return pane to tab
                }
            }
        }
        return null to null
    }

    val currentFontSizeProperty = SimpleDoubleProperty(this, "currentFontSize", 12.0)
        .also {
            root.styleProperty().bind(Bindings.format("-fx-font-size: %.2fpt;", it))
        }
    var currentFontSize by currentFontSizeProperty

    fun increaseFontSize(step: Double = 2.0) {
        currentFontSize += step
    }

    fun decreaseFontSize(step: Double = 2.0) {
        currentFontSize -= step
    }

    fun newWindow(currentEditor: Editor?) {
        val mainScene = newWindow()
        if (currentEditor != null) {
            val (p, t) = getTabPane(currentEditor)
            p?.tabs?.remove(t)
            mainScene.addEditorTab(currentEditor)
        }
    }
}

class StatusBar(context: Context) {
    val lblMessage = Label()
    val lblLineRow = Label()
    val lblError = Label()
    val ui: HBox = HBox(10.0, lblMessage, lblLineRow, lblError)

    val messageProperty = lblMessage.textProperty()
    var message: String by messageProperty

    val graphicProperty = lblMessage.graphicProperty()
    var graphic by graphicProperty

    init {
        context.register<StatusBar>(this)
    }
}

open class TitledPanel(header: String) {
    val ui = BorderPane()
    var buttonBox = HBox()
    val lblHeader = createHeaderLabel(header)

    init {
        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        ui.top = HBox(lblHeader, spacer, buttonBox)
        ui.styleClass.add("titled-panel")
    }

    protected fun createHeaderLabel(text: String) = Label(text).also {
        it.styleClass.add("title")
    }
}

