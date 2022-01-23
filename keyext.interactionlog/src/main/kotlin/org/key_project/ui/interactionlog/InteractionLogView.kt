package org.key_project.ui.interactionlog

import bibliothek.gui.dock.common.DefaultMultipleCDockable
import bibliothek.gui.dock.common.NullMultipleCDockableFactory
import de.uka.ilkd.key.core.KeYMediator
import de.uka.ilkd.key.gui.MainWindow
import de.uka.ilkd.key.gui.actions.KeyAction
import de.uka.ilkd.key.gui.extension.api.TabPanel
import de.uka.ilkd.key.gui.fonticons.*
import de.uka.ilkd.key.proof.Proof
import org.key_project.ui.interactionlog.algo.MUProofScriptExport
import org.key_project.ui.interactionlog.algo.MarkdownExport
import org.key_project.ui.interactionlog.algo.toHtml
import org.key_project.ui.interactionlog.api.Interaction
import org.key_project.ui.interactionlog.api.InteractionRecorderListener
import org.key_project.ui.interactionlog.model.InteractionLog
import org.key_project.ui.interactionlog.model.NodeInteraction
import org.key_project.ui.interactionlog.model.UserNoteInteraction
import org.ocpsoft.prettytime.PrettyTime
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.lang.ref.WeakReference
import javax.swing.*
import javax.swing.border.Border
import javax.swing.text.html.HTMLEditorKit


class InteractionLogView(val interactionLog: InteractionLog, private var mediator: KeYMediator) : JPanel(), TabPanel {
    val actionExportProofScript: KeyAction = ExportMUScriptAction()
    val actionKPSExport: KeyAction = ExportKPSAction()
    val actionSave: KeyAction = SaveAction()
    val actionAddUserNote: KeyAction = AddUserNoteAction()
    val actionToggleFavourite: KeyAction = ToggleFavouriteAction()
    val actionJumpIntoTree: KeyAction = JumpIntoTreeAction()
    val actionTryReapply: KeyAction = TryReapplyAction()
    val actionExportMarkdown: KeyAction = ExportMarkdownAction()
    val actionShowExtended: KeyAction = ShowExtendedActionsAction()
    val actionMUCopyClipboard: KeyAction = ExportMUScriptClipboardAction()


    val listInteraction = JList<Interaction>()
    val interactionListModel = DefaultListModel<Interaction>()

    private var currentProof: Proof? = interactionLog.proof.get()
    private var fileChooser: JFileChooser = JFileChooser()
    private val lblCurrentProof = JLabel(currentProof?.name().toString())

    init {
        // register the recorder in the proof control
        listInteraction.model = interactionListModel
        listInteraction.cellRenderer = InteractionCellRenderer()

        val panelButtons = JToolBar()
        val btnSave = JButton(actionSave)
        val btnExport = JButton(actionExportProofScript)
        val btnAddNote = JButton(actionAddUserNote)

        btnExport.hideActionText = true
        btnSave.hideActionText = true
        btnAddNote.hideActionText = true

        panelButtons.add(btnSave)
        panelButtons.add(Box.createHorizontalGlue())
        panelButtons.add(actionAddUserNote)
        panelButtons.addSeparator()
        panelButtons.add(actionShowExtended)

        val popup = JPopupMenu()
        popup.add(JMenuItem(actionMUCopyClipboard))
        popup.addSeparator()
        popup.add(JMenuItem(actionToggleFavourite))
        popup.add(JMenuItem(actionJumpIntoTree))
        popup.add(JMenuItem(actionTryReapply))
        popup.addSeparator()
        popup.add(JMenuItem(actionAddUserNote))
        listInteraction.componentPopupMenu = popup


        listInteraction.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent?) {
                val l = e!!.source as JList<*>
                val index = l.locationToIndex(e.point)
                if (index > -1) {
                    //val inter = m.getElementAt(index) as Interaction
                    // weigl: disabled on my system, the tooltips lead to a crash of the window manager
                    // l.toolTipText = "<html>" + MarkdownExport.getHtml(inter) + "</html>"
                    l.toolTipText = "Tooltips are deactivated due to a Swing bug."
                }
            }
        })

        DropTarget(listInteraction, 0, object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                val tr = dtde.transferable
                try {
                    val textFlavour = tr.getTransferData(DataFlavor.stringFlavor)
                    dtde.acceptDrop(dtde.dropAction)
                    dtde.dropComplete(true)
                    SwingUtilities.invokeLater { (actionAddUserNote as AddUserNoteAction).doAddNote(textFlavour.toString()) }
                    return
                } catch (e: UnsupportedFlavorException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                dtde.rejectDrop()
            }
        })

        layout = BorderLayout()
        add(panelButtons, BorderLayout.NORTH)

        val txtDetails = JEditorPane().apply {
            isEditable = false
            editorKit = HTMLEditorKit()
        }

        val txtScript = JEditorPane().apply {
            isEditable = false
        }

        val tabDetails = JTabbedPane().apply {
            //border = BorderFactory.createTitledBorder("Details")
            addTab("Details", JScrollPane(txtDetails))
            addTab("MU Script", JScrollPane(txtScript))
        }

        val splitPane = JSplitPane(
            JSplitPane.VERTICAL_SPLIT, true,
            JScrollPane(listInteraction),
            tabDetails
        )
        splitPane.isOneTouchExpandable = true
        add(splitPane)

        listInteraction.addListSelectionListener {
            listInteraction.selectedValuesList?.apply {
                txtDetails.text = "<html>${joinToString("<hr>") { it.toHtml() }}</html>"
                txtScript.text = joinToString("\n") { it.proofScriptRepresentation }
            }
        }


        val pSouth = JPanel()
        pSouth.add(JLabel("Proof: "))
        pSouth.add(lblCurrentProof)
        pSouth.add(JButton(JumpToProofAction()))
        pSouth.add(JButton(BindToCurrentProofAction()))
        add(pSouth, BorderLayout.SOUTH)

        border = BorderFactory.createTitledBorder("Interactions")

        InteractionLogExt.recorder.addListener(object : InteractionRecorderListener {
            override fun onInteraction(recorder: InteractionRecorder, log: InteractionLog, event: Interaction) {
                if (interactionLog == log) {
                    updateList()
                }
            }
        })
        updateList()
    }

    fun updateList() {
        interactionListModel.clear()
        interactionLog.interactions.forEach { interactionListModel.addElement(it) }
    }

    private fun getFileChooser(): JFileChooser {
        if (currentProof != null) {
            val file = currentProof!!.proofFile
            if (file != null)
                fileChooser.currentDirectory = file.parentFile
        }
        return fileChooser
    }

    override fun getTitle(): String {
        return interactionLog.name
    }

    override fun getIcon(): Icon? {
        return INTERACTION_LOG_ICON
    }

    override fun getComponent(): JComponent {
        return this
    }

    val dockable by lazy {
        val d = DefaultMultipleCDockable(NullMultipleCDockableFactory.NULL, icon, title, this, permissions)
        d.isCloseable = true
        d
    }


    private inner class BindToCurrentProofAction : KeyAction() {
        init {
            name = "Rebind"
        }

        override fun actionPerformed(e: ActionEvent?) {
            val proof = mediator.selectionModel.selectedProof
            interactionLog.proof = WeakReference(proof)
            InteractionLogExt.recorder.prioritize(interactionLog)
            lblCurrentProof.text = proof.name().toString()
        }
    }

    private inner class JumpToProofAction : KeyAction() {

        init {
            name = "Jump to proof"
        }

        override fun actionPerformed(e: ActionEvent?) {
            currentProof?.also {
                mediator.selectionModel.selectedProof = it
            }
        }
    }

    private inner class ExportMUScriptAction() : AbstractFileSaveAction() {
        init {
            name = "Export as Proof Script"
            setIcon(IconFactory.EXPORT_MU_SCRIPT.get())
            menuPath = MENU_ILOG_EXPORT
            lookupAcceleratorKey()
        }

        override fun save(selectedFile: File) {
            val current = interactionLog
            val script = MUProofScriptExport.getScript(current)
            try {
                FileWriter(selectedFile).use { fw -> fw.write(script) }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private inner class ExportMUScriptClipboardAction() : KeyAction() {
        init {
            name = "Copy MUScript"
            smallIcon = IconFactory.EXPORT_MU_SCRIPT_CLIPBOARD.get()
            menuPath = MENU_ILOG_EXPORT
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            val text = (listInteraction.selectedValue as Interaction).proofScriptRepresentation
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val contents = StringSelection(text)
            clipboard.setContents(contents, contents)
        }
    }

    private inner class SaveAction() : KeyAction() {
        init {
            name = "Save"
            setIcon(INTERLOG_SAVE.get())
            menuPath = MENU_ILOG
            priority = 1
            lookupAcceleratorKey()
        }


        override fun actionPerformed(e: ActionEvent) {
            val fileChooser = getFileChooser()
            val returnValue = fileChooser.showSaveDialog(null)
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                val activeInteractionLog = interactionLog
                try {
                    InteractionLogFacade.storeInteractionLog(
                        activeInteractionLog,
                        fileChooser.selectedFile
                    )
                    activeInteractionLog.savePath = fileChooser.selectedFile
                } catch (exception: Exception) {
                    JOptionPane.showMessageDialog(
                        MainWindow.getInstance(),
                        exception.message,
                        "IOException",
                        JOptionPane.WARNING_MESSAGE
                    )
                    exception.printStackTrace()
                }

            }
        }
    }

    private inner class AddUserNoteAction() : KeyAction() {
        init {
            name = "Add Note"
            setIcon(ICON_ADD_USER_ACTION.get(SMALL_ICON_SIZE))
            //new ImageIcon(getClass().getResource("/de/uka/ilkd/key/gui/icons/book_add.png")));
            menuPath = MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            doAddNote("")
        }

        fun doAddNote(prefilled: String) {
            MultiLineInputPrompt(this@InteractionLogView, prefilled)
                .show()?.let { note ->
                    val interaction = UserNoteInteraction(note)
                    val interactionLog = interactionLog
                    interactionLog.add(interaction)
                }
        }
    }

    private inner class ToggleFavouriteAction() : KeyAction() {
        init {
            name = "Toggle Fav"
            putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F)
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK))
            setIcon(ICON_TOGGLE_FAVOURITE.get(SMALL_ICON_SIZE))
            menuPath = MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            if (listInteraction.selectedValue != null) {
                listInteraction.selectedValue.isFavoured = !listInteraction.selectedValue.isFavoured
            }
        }
    }

    private inner class JumpIntoTreeAction() : KeyAction() {
        init {
            name = "Jump into tree"
            putValue(Action.SMALL_ICON, JUMP_INTO_TREE.get())
            menuPath = MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            try {
                val current = listInteraction.selectedValue as? NodeInteraction
                if (current != null) {
                    val node = current.getNode(mediator.selectedProof)
                    mediator.selectionModel.selectedNode = node
                }
            } catch (ex: ClassCastException) {
                //ignore
            }

        }
    }

    private inner class TryReapplyAction() : KeyAction() {
        init {
            putValue(Action.NAME, "Re-apply action")
            putValue(Action.SMALL_ICON, INTERLOG_TRY_APPLY.get())
            menuPath = MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            val inter = listInteraction.selectedValue
            try {
                //Reapplication should be ignored by the logging.
                InteractionLogExt.disableLogging()
                inter.reapplyStrict(mediator.ui, mediator.selectedGoal)
            } catch (ex: UnsupportedOperationException) {
                JOptionPane.showMessageDialog(
                    null,
                    String.format(
                        "<html>Reapplication of %s is not implemented<br>If you know how to do it, then override the corresponding method in %s.</html>",
                        inter.javaClass
                    ), "A very expected error.", JOptionPane.ERROR_MESSAGE
                )
            } catch (e1: Exception) {
                e1.printStackTrace()
            } finally {
                InteractionLogExt.enableLogging()
            }
        }
    }

    private abstract inner class AbstractFileSaveAction : KeyAction() {
        override fun actionPerformed(e: ActionEvent) {
            val fc = getFileChooser()
            val choice = fc.showSaveDialog(e.source as Component)
            if (choice == JFileChooser.APPROVE_OPTION) {
                save(fc.selectedFile)
            }
        }

        abstract fun save(selectedFile: File)
    }

    private inner class ExportKPSAction : AbstractFileSaveAction() {
        init {
            name = "Export as KPS …"
            putValue(Action.SHORT_DESCRIPTION, "Export the current log into the KPS format.")
            setIcon(INTERLOG_EXPORT_KPS.get())
            menuPath = MENU_ILOG_EXPORT
            lookupAcceleratorKey()
        }

        override fun save(selectedFile: File) {
            try {
                FileWriter(selectedFile).use { fw -> MarkdownExport(interactionLog, PrintWriter(fw)).run() }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private inner class ExportMarkdownAction : AbstractFileSaveAction() {
        init {
            name = "Export as markdown …"
            putValue(Action.SHORT_DESCRIPTION, "Export the current log into a markdown file.")
            setIcon(ICON_MARKDOWN.get(SMALL_ICON_SIZE))
            menuPath = MENU_ILOG_EXPORT
            lookupAcceleratorKey()
        }

        override fun save(selectedFile: File) {
            try {
                FileWriter(selectedFile).use { fw -> MarkdownExport(interactionLog, PrintWriter(fw)).run() }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private inner class ShowExtendedActionsAction : KeyAction() {
        init {
            name = "More …"
            putValue(Action.SHORT_DESCRIPTION, "Shows further options")
            setIcon(INTERLOW_EXTENDED_ACTIONS.get())
            lookupAcceleratorKey()
        }

        fun createMenu(): JPopupMenu {
            val menu = JPopupMenu(name)
            menu.add(actionExportMarkdown)
            menu.add(actionExportProofScript)
            menu.add(actionKPSExport)
            return menu
        }

        override fun actionPerformed(e: ActionEvent) {
            val btn = e.source as JComponent
            val menu = createMenu()
            //val pi = MouseInfo.getPointerInfo()
            menu.show(btn, 0, 0)
            //pi.getLocation().x, pi.getLocation().y);
        }
    }

    companion object {
        val ICON_AUTO_SAVE_ENABLED = IconFontProvider(FontAwesomeSolid.MAGIC)
        val ICON_AUTO_SAVE_DISABLED = IconFontProvider(FontAwesomeSolid.MAGIC, Color.gray)
        val ICON_LOAD = IconFontProvider(FontAwesomeSolid.TRUCK_LOADING)
        val ICON_ADD_USER_ACTION = IconFontProvider(FontAwesomeRegular.STICKY_NOTE)
        val ICON_TOGGLE_FAVOURITE = IconFontProvider(FontAwesomeSolid.HEART, Color.red)
        val ICON_MARKDOWN = IconFontProvider(FontAwesomeBrands.MARKDOWN)
        val ICON_SHOW = IconFontProvider(FontAwesomeSolid.BOOK_OPEN)

        val INTERLOG_LOAD: IconProvider = IconFontProvider(FontAwesomeSolid.TRUCK_LOADING)
        val INTERLOG_SAVE: IconProvider = IconFontProvider(FontAwesomeRegular.SAVE)
        val INTERLOG_ADD_USER_NOTE: IconProvider = IconFontProvider(FontAwesomeRegular.STICKY_NOTE)
        val INTERLOG_TOGGLE_FAV: IconProvider = IconFontProvider(FontAwesomeSolid.HEART)
        val JUMP_INTO_TREE: IconProvider = IconFontProvider(FontAwesomeSolid.MAP_MARKED)
        val INTERLOG_TRY_APPLY: IconProvider = IconFontProvider(FontAwesomeSolid.REDO)
        val INTERLOG_EXPORT_KPS: IconProvider = IconFontProvider(FontAwesomeSolid.FILE_EXPORT)
        val INTERLOG_EXPORT_MARKDOWN: IconProvider = IconFontProvider(FontAwesomeSolid.FILE_EXPORT)
        val INTERLOW_EXTENDED_ACTIONS: IconProvider = IconFontProvider(FontAwesomeSolid.WRENCH)
        val INTERLOG_RESUME: IconProvider = IconFontProvider(FontAwesomeSolid.PAUSE_CIRCLE)
        val INTERLOG_PAUSE: IconProvider = IconFontProvider(FontAwesomeSolid.PLAY_CIRCLE)
        val INTERLOG_ICON: IconProvider = IconFontProvider(FontAwesomeSolid.BOOK)
        val ICON_SAVE_AS: IconProvider = IconFontProvider(FontAwesomeSolid.FILE_CODE)

        val INTERACTION_LOG_ICON = INTERLOG_ICON.get()
        val SMALL_ICON_SIZE = 16f
        val MENU_ILOG = "Interaction Logging"
        val MENU_ILOG_EXPORT = "$MENU_ILOG.Export"
    }
}

internal class MultiLineInputPrompt(private var parent: JComponent?, private val text: String) {
    val dialog: JDialog by lazy {
        val d = JDialog(JOptionPane.getFrameForComponent(parent))
        d.isModal = true
        d.title = "Enter note..."

        val root = JPanel(BorderLayout(10, 10))
        val box = JPanel(FlowLayout(FlowLayout.CENTER))
        root.add(box, BorderLayout.SOUTH)
        val area = JTextArea(text)
        val btnOk = JButton("OK")
        val btnCancel = JButton("Cancel")
        box.add(btnOk)
        box.add(btnCancel)

        btnOk.addActionListener { accept(area.text) }
        btnCancel.addActionListener { cancel() }
        d.defaultCloseOperation = JDialog.HIDE_ON_CLOSE
        d.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                cancel()
            }
        })
        d.contentPane = root

        root.add(JScrollPane(area), BorderLayout.CENTER)
        d.setSize(300, 200)
        d.setLocationRelativeTo(parent)
        d
    }

    private var acceptedAnswer: String? = null

    private fun cancel() {
        acceptedAnswer = null
        dialog.isVisible = false
    }

    private fun accept(text: String) {
        acceptedAnswer = text
        dialog.isVisible = false
    }

    fun show(): String? {
        dialog.isVisible = true
        return acceptedAnswer
    }
}

internal class InteractionCellRenderer : JPanel(), ListCellRenderer<Interaction> {
    private val lblIconLeft = JLabel()
    private val lblIconRight = JLabel()
    private val lblText = JLabel()
    private val iconHeart = InteractionLogView.INTERLOG_TOGGLE_FAV.get()
    private val prettyTime = PrettyTime()


    init {
        layout = BorderLayout()
        add(lblIconLeft, BorderLayout.WEST)
        add(lblIconRight, BorderLayout.EAST)
        add(lblText)
    }

    override fun getListCellRendererComponent(
        list: JList<out Interaction>,
        value: Interaction,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        //df.format(value.created)

        lblText.text = value.toString()
        lblIconRight.text = prettyTime.format(value.created)
        lblIconRight.icon = if (value.isFavoured) iconHeart else null

        border = if (value.isFavoured)
            BorderFactory.createLineBorder(COLOR_FAVOURED) else null

        componentOrientation = list.componentOrientation

        if (isSelected) {
            background = list.selectionBackground
            foreground = list.selectionForeground
        } else {
            background = if (value.graphicalStyle.backgroundColor != null)
                value.graphicalStyle.backgroundColor
            else
                list.background

            foreground = if (value.graphicalStyle.foregroundColor != null)
                value.graphicalStyle.foregroundColor
            else
                list.foreground
        }

        lblIconRight.foreground = foreground
        lblIconLeft.foreground = foreground
        lblText.foreground = foreground

        lblIconRight.background = background
        lblIconLeft.background = background
        lblText.background = background


        isEnabled = list.isEnabled
        font = list.font

        var border: Border? = null
        if (cellHasFocus) {
            if (isSelected) {
                border = UIManager.getDefaults().getBorder("List.focusSelectedCellHighlightBorder")
            }
            if (border == null) {
                border = UIManager.getDefaults().getBorder("List.focusCellHighlightBorder")
            }
        }
        if (border != null) {
            this.border = border
        }
        return this
    }

    companion object {
        //private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private val COLOR_FAVOURED = Color(0xFFD373)
    }
}
