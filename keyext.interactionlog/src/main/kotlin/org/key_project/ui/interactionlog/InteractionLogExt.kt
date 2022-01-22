package org.key_project.ui.interactionlog

import bibliothek.gui.dock.common.CLocation
import de.uka.ilkd.key.core.KeYSelectionEvent
import de.uka.ilkd.key.core.KeYSelectionListener
import de.uka.ilkd.key.gui.MainWindow
import de.uka.ilkd.key.gui.actions.KeyAction
import de.uka.ilkd.key.gui.actions.MainWindowAction
import de.uka.ilkd.key.gui.extension.api.KeYGuiExtension
import org.key_project.ui.BoundsPopupMenuListener
import org.key_project.ui.interactionlog.api.Interaction
import org.key_project.ui.interactionlog.api.InteractionRecorderListener
import org.key_project.ui.interactionlog.model.InteractionLog
import java.awt.Component
import java.awt.event.ActionEvent
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * @author Alexander Weigl
 * @version 1 (13.02.19)
 */
@KeYGuiExtension.Info(name = "Interaction Logging", optional = true, experimental = true, priority = 10000)
class InteractionLogExt : KeYGuiExtension, KeYGuiExtension.MainMenu, KeYGuiExtension.Toolbar,
    InteractionRecorderListener {
    companion object {
        val recorder = InteractionRecorder()

        fun disableLogging() {
            recorder.isDisableAll = true
        }

        fun enableLogging() {
            recorder.isDisableAll = false
        }
    }

    private var toolbar = JToolBar("interaction logging")
    private val interactionLogSelection = JComboBox<InteractionLog>()
    private var autoSaveEnabled: Boolean = false

    init {
        val listener = BoundsPopupMenuListener(true, false)

        interactionLogSelection.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val v = value ?: "No log loaded."
                return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus)
            }
        }

        interactionLogSelection.addPopupMenuListener(listener)
        interactionLogSelection.prototypeDisplayValue = InteractionLog("12345678901234567890")

        toolbar.add(LoadAction())
        toolbar.add(PauseLoggingAction())
        toolbar.add(interactionLogSelection)

        recorder.addListener(object : InteractionRecorderListener {
            override fun onInteraction(recorder: InteractionRecorder, log: InteractionLog, event: Interaction) {
                if (autoSaveEnabled) {
                    val path = log.savePath ?: File(log.name + ".json").also {
                        log.savePath = it
                    }

                    InteractionLogFacade.storeInteractionLog(log, path)
                    MainWindow.getInstance().setStatusLine("Interaction log ${log.name} saved.")
                }
            }

            override fun onNewInteractionLog(recorder: InteractionRecorder, log: InteractionLog) {
                interactionLogSelection.addItem(log)
                //create a log message
            }

            override fun onDisposeInteractionLog(recorder: InteractionRecorder, log: InteractionLog) {
                interactionLogSelection.removeItem(log)
                //create a log message
            }
        })

        toolbar.add(JToggleButton(AutoSaveAction()))
        toolbar.add(SaveAsAction())
    }


    override fun getToolbar(mainWindow: MainWindow): JToolBar {
        mainWindow.userInterface.proofControl.addInteractionListener(recorder)
        mainWindow.userInterface.proofControl.addAutoModeListener(recorder)
        mainWindow.mediator.addKeYSelectionListener(object : KeYSelectionListener {
            override fun selectedNodeChanged(e: KeYSelectionEvent?) {}

            override fun selectedProofChanged(e: KeYSelectionEvent?) {
                recorder.get(mainWindow.mediator.selectedProof)
            }
        })
        toolbar.add(ShowLogAction(mainWindow))
        return toolbar
    }

    override fun getMainMenuActions(mainWindow: MainWindow): List<Action> {
        return Arrays.asList(
            /*ilv.actionAddUserNote,
                ilv.actionExportMarkdown,
                ilv.actionJumpIntoTree,
                ilv.actionLoad,
                ilv.actionSave,
                ilv.actionTryReapply,
                ilv.actionKPSExport,
                ilv.actionToggleFavourite,
                ilv.actionExportMarkdown,
                ilv.actionMUCopyClipboard,
                ilv.actionPauseLogging*/
        )
    }


    private inner class PauseLoggingAction : KeyAction() {
        init {
            isSelected = recorder.isDisableAll
            priority = -1
            menuPath = InteractionLogView.MENU_ILOG
            putValue(Action.SHORT_DESCRIPTION, "Activation or Deactivation of interaction logging")

            update()
            addPropertyChangeListener { evt ->
                if (evt.propertyName == Action.SELECTED_KEY)
                    update()
            }
            lookupAcceleratorKey()
        }

        private fun update() {
            name = if (!isSelected) {
                setIcon(InteractionLogView.INTERLOG_PAUSE.get())
                "Pause Interaction Logging"
            } else {
                setIcon(InteractionLogView.INTERLOG_RESUME.get())
                "Resume Interaction Logging"
            }
        }

        override fun actionPerformed(e: ActionEvent) {
            isSelected = !isSelected
            recorder.isDisableAll = isSelected
        }
    }


    private inner class LoadAction() : KeyAction() {
        init {
            name = "Load"
            putValue(Action.SHORT_DESCRIPTION, "Load Interaction Log")
            setIcon(InteractionLogView.ICON_LOAD.get(InteractionLogView.SMALL_ICON_SIZE))
            priority = 0
            menuPath = InteractionLogView.MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            val fileChooser = JFileChooser()
            fileChooser.fileFilter = FileNameExtensionFilter(
                "InteractionLog", "json"
            )
            val returnValue = fileChooser.showOpenDialog(null)
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                try {
                    val file = fileChooser.selectedFile
                    recorder.readInteractionLog(file)
                    //addInteractionLog(importedLog);
                } catch (exception: Exception) {
                    JOptionPane.showMessageDialog(
                        null,
                        exception.cause,
                        "IOException",
                        JOptionPane.WARNING_MESSAGE
                    )
                    exception.printStackTrace()
                }

            }
        }
    }

    private inner class ShowLogAction(window: MainWindow) : MainWindowAction(window) {
        init {
            name = "Load"
            tooltip = "Show the interaction log"
            smallIcon = InteractionLogView.ICON_SHOW.get(InteractionLogView.SMALL_ICON_SIZE)
            priority = 0
            menuPath = InteractionLogView.MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent?) {
            showLog()
        }
    }

    private val map = HashMap<InteractionLog, InteractionLogView>()
    private fun showLog(log: InteractionLog? = null) {
        val l = log ?: interactionLogSelection.selectedItem as? InteractionLog
        if (l != null) {
            val view = map.computeIfAbsent(l) {
                InteractionLogView(l, MainWindow.getInstance().mediator)
            }
            MainWindow.getInstance().dockControl.addDockable(view.dockable)
            view.dockable.isVisible = true
            view.dockable.setLocation(CLocation.base().normalEast(.3))
        } else {
            MainWindow.getInstance().setStatusLine("No interaction is loaded or selected.")
        }
    }

    inner class AutoSaveAction : KeyAction() {
        init {
            name = "Enable/Disable Auto Save"
            putValue(Action.SHORT_DESCRIPTION, "If enabled, interaction log are stored on every interaction.")
            priority = 0
            menuPath = InteractionLogView.MENU_ILOG
            lookupAcceleratorKey()
            addPropertyChangeListener { evt ->
                if (evt.propertyName == Action.SELECTED_KEY) {
                    onAutoSaveChange(isSelected)
                    update()
                }
            }
            isSelected = false
        }

        private fun update() {
            setIcon(
                (if (isSelected)
                    InteractionLogView.ICON_AUTO_SAVE_ENABLED
                else
                    InteractionLogView.ICON_AUTO_SAVE_DISABLED)
                    .get(InteractionLogView.SMALL_ICON_SIZE)
            )
        }

        override fun actionPerformed(e: ActionEvent?) {
            onAutoSaveChange(isSelected)
            update()
        }
    }

    private fun onAutoSaveChange(active: Boolean) {
        autoSaveEnabled = active
    }

    inner class SaveAsAction : KeyAction() {
        init {
            name = "Save As ..."
            putValue(Action.SHORT_DESCRIPTION, "Save the current selected interaction into a file.")
            setIcon(InteractionLogView.ICON_SAVE_AS.get(InteractionLogView.SMALL_ICON_SIZE))
            priority = 0
            menuPath = InteractionLogView.MENU_ILOG
            lookupAcceleratorKey()
        }

        override fun actionPerformed(e: ActionEvent) {
            (interactionLogSelection.selectedItem as? InteractionLog)?.let {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter(
                    "InteractionLog", "json"
                )
                val returnValue = fileChooser.showSaveDialog(MainWindow.getInstance())
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    try {
                        val file = fileChooser.selectedFile
                        InteractionLogFacade.storeInteractionLog(it, file)
                        it.savePath = file
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
    }
}
