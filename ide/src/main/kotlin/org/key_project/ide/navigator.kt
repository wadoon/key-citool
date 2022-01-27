package org.key_project.ide

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTreeCell
import javafx.util.StringConverter
import tornadofx.View
import tornadofx.borderpane
import tornadofx.getValue
import tornadofx.setValue
import java.awt.Desktop
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileNavigator(ctx: Context) : View("Files") {
    private val config0 = ctx.get<UserConfig>()
    private val main by ctx.ref<MainView>()

    override val root = borderpane {
        center = treeFile
    }

    //properties
    val rootFileProperty = SimpleObjectProperty(this, "rootFile", Paths.get(".").normalize().toAbsolutePath())
    var rootFile by rootFileProperty
    //end properties

    //ui
    val treeFile = TreeView(SimpleFileTreeItem(rootFile))
    val contextMenu: ContextMenu = ContextMenu()
    //end

    private fun markFolderUnderMouse(event: ActionEvent) {}

    private val actionOpenFile = config0.createItem("tree-open-file") {
        markFolderUnderMouse(it)
        treeFile.selectionModel.selectedItem?.let {
            main.open(it.value)
        }
    }
    private val actionNewFile = config0.createItem("tree-new-file") {
        markFolderUnderMouse(it)
        treeFile.selectionModel.selectedItem?.let { item ->
            val path = item.value!!
            val dialog = TextInputDialog()
            dialog.contentText = "File name:"
            val name = dialog.showAndWait()
            name.ifPresent {
                val newFile = path.resolve(it)
                try {
                    Files.createFile(newFile)
                    main.open(newFile)
                    refresh()
                } catch (e: IOException) {
                    val a = Alert(Alert.AlertType.ERROR)
                    a.contentText = e.message
                    a.show()
                }
            }
        }
    }
    private val actionNewDirectory = config0.createItem("tree-new-directory") {
        markFolderUnderMouse(it)
        treeFile.selectionModel.selectedItem?.let { item ->
            val path = item.value!!
            val dialog = TextInputDialog()
            dialog.contentText = "Folder name:"
            val name = dialog.showAndWait()
            name.ifPresent {
                val newFile = path.resolve(it)
                try {
                    Files.createDirectory(newFile)
                    main.open(newFile)
                    refresh()
                } catch (e: IOException) {
                    val a = Alert(Alert.AlertType.ERROR)
                    a.contentText = e.message
                    a.show()
                }
            }
        }
    }
    private val actionRenameFile = config0.createItem("tree-rename-file") { }
    private val actionDeleteFile = config0.createItem("tree-delete-file") {}
    private val actionGoUp = config0.createItem("go-up") {
        markFolderUnderMouse(it)
        treeFile.root.value?.let { file ->
            treeFile.root = SimpleFileTreeItem(file.parent.toAbsolutePath())
            treeFile.root.isExpanded = true
        }
    }
    private val actionGoInto = config0.createItem("go-into") {
        markFolderUnderMouse(it)
        (treeFile.selectionModel.selectedItem)?.let { file ->
            treeFile.root = SimpleFileTreeItem(file.value.toAbsolutePath())
            treeFile.root.isExpanded = true
        }
    }
    private val actionRefresh = config0.createItem("refresh") { refresh() }

    private fun refresh() {}

    private val actionExpandSubTree = config0.createItem("expand-tree") {
        markFolderUnderMouse(it)
    }
    private val actionOpenExplorer = config0.createItem("open-in-explorer", this::openExplorer)

    fun openExplorer(evt: ActionEvent) {
        markFolderUnderMouse(evt)
        (treeFile.selectionModel.selectedItem)?.let { file ->
            try {
                Desktop.getDesktop()?.browseFileDirectory(file.value.toFile())
            } catch (e: UnsupportedOperationException) {
                ProcessBuilder("explorer", "/select,${file.value}").start()
            }
        }
    }

    private val actionOpenSystem = config0.createItem("xdg-open", this::openSystem)

    fun openSystem(evt:ActionEvent): (ActionEvent) -> Unit = {
            markFolderUnderMouse(evt)
            (treeFile.selectionModel.selectedItem)?.let { file ->
                try {
                    Desktop.getDesktop()?.open(file.value.toFile())
                } catch (e: UnsupportedOperationException) {
                    ProcessBuilder("explorer", "/select,${file.value}").start()
                }
            }
        }

    init {
        contextMenu.items.setAll(
            actionOpenFile,
            SeparatorMenuItem(),
            actionNewFile,
            actionNewDirectory,
            actionRenameFile,
            actionDeleteFile,
            SeparatorMenuItem(),
            actionGoUp,
            actionGoInto,
            SeparatorMenuItem(),
            actionExpandSubTree,
            actionRefresh,
            SeparatorMenuItem(),
            actionOpenExplorer,
            actionOpenSystem
        )


        treeFile.contextMenu = contextMenu
        treeFile.isEditable = false
        treeFile.isShowRoot = true
        rootFileProperty.addListener { _, _, new ->
            treeFile.root = SimpleFileTreeItem(new)
        }

        treeFile.setCellFactory { tv ->
            TextFieldTreeCell(object : StringConverter<Path>() {
                override fun toString(obj: Path?): String = obj?.fileName.toString() ?: ""
                override fun fromString(string: String?): Path = Paths.get(string)
            })
        }
        treeFile.root.isExpanded = true
    }
}

class SimpleFileTreeItem(f: Path) : TreeItem<Path>(f) {
    private var isFirstTimeChildren = true
    private var isFirstTimeLeaf = true
    private var isLeaf = false

    override fun getChildren(): ObservableList<TreeItem<Path>> {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false
            super.getChildren().setAll(buildChildren(this))
        }
        return super.getChildren()
    }

    override fun isLeaf(): Boolean {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false
            val f = value as Path
            isLeaf = Files.isRegularFile(f)
        }
        return isLeaf
    }

    private fun buildChildren(node: TreeItem<Path>): ObservableList<TreeItem<Path>> {
        val f = node.value
        if (f != null && Files.isDirectory(f)) {
            val children: ObservableList<TreeItem<Path>> = FXCollections.observableArrayList()
            Files.list(f).sorted().forEach {
                children.add(SimpleFileTreeItem(it))
            }
            return children
        }
        return FXCollections.emptyObservableList()
    }
}

