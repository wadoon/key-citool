package org.key_project.ui.interactionlog

import de.uka.ilkd.key.control.AutoModeListener
import de.uka.ilkd.key.control.InteractionListener
import de.uka.ilkd.key.logic.PosInOccurrence
import de.uka.ilkd.key.macros.ProofMacro
import de.uka.ilkd.key.macros.ProofMacroFinishedInfo
import de.uka.ilkd.key.proof.Node
import de.uka.ilkd.key.proof.Proof
import de.uka.ilkd.key.proof.ProofEvent
import de.uka.ilkd.key.proof.mgt.ProofEnvironmentEvent
import de.uka.ilkd.key.proof.mgt.ProofEnvironmentListener
import de.uka.ilkd.key.prover.impl.ApplyStrategyInfo
import de.uka.ilkd.key.rule.BuiltInRule
import de.uka.ilkd.key.rule.IBuiltInRuleApp
import de.uka.ilkd.key.rule.RuleApp
import de.uka.ilkd.key.settings.Settings
import org.key_project.ui.interactionlog.api.Interaction
import org.key_project.ui.interactionlog.api.InteractionRecorderListener
import org.key_project.ui.interactionlog.model.*
import java.io.File
import java.util.*

/**
 * @author Alexander Weigl <weigl@kit.edu>
 */
class InteractionRecorder : InteractionListener, AutoModeListener {
    private val listeners = ArrayList<InteractionRecorderListener>()
    private val instances = arrayListOf<InteractionLog>()
    private val disableSettingsChanges = false

    var isDisableAll = false

    fun register(log: InteractionLog) {
        instances += log
        fireNewInteractionLog(log)
    }

    fun dispose(log: InteractionLog) {
        instances.remove(log)
        fireDisposeInteractionLog(log)
    }

    operator fun get(proof: Proof): InteractionLog {
        return instances.find { it.proof.get() == proof }
                ?: InteractionLog(proof).also { il ->
                    register(il)
                    registerOnSettings(proof)
                    registerDisposeListener(proof)
                    createInitialSettingsEntry(proof)
                }
    }

    private fun fireNewInteractionLog(log: InteractionLog) {
        listeners.forEach { it.onNewInteractionLog(this, log) }
    }


    private fun fireDisposeInteractionLog(log: InteractionLog) {
        listeners.forEach { it.onDisposeInteractionLog(this, log) }
    }

    private fun createInitialSettingsEntry(proof: Proof) {
        settingChanged(proof,
                proof.settings.strategySettings,
                InteractionListener.SettingType.STRATEGY, "Initial Config")
        settingChanged(proof,
                proof.settings.smtSettings,
                InteractionListener.SettingType.SMT, "Initial Config")
        settingChanged(proof,
                proof.settings.choiceSettings,
                InteractionListener.SettingType.CHOICE, "Initial Config")
    }

    private fun registerDisposeListener(proof: Proof) {
        proof.env.addProofEnvironmentListener(object : ProofEnvironmentListener {
            override fun proofRegistered(event: ProofEnvironmentEvent) {

            }

            override fun proofUnregistered(event: ProofEnvironmentEvent) {
                //TODO check how to find out whether proof was removed or a different instance
            }
        })
    }

    fun readInteractionLog(file: File): InteractionLog {
        val log = InteractionLogFacade.readInteractionLog(file)
        register(log)
        return log
    }

    fun registerOnSettings(proof: Proof) {
        proof.settings.strategySettings.addSettingsListener {
            settingChanged(proof,
                    proof.settings.strategySettings,
                    InteractionListener.SettingType.STRATEGY, null)
        }

        proof.settings.choiceSettings.addSettingsListener {
            settingChanged(proof,
                    proof.settings.choiceSettings,
                    InteractionListener.SettingType.CHOICE, null)
        }

        proof.settings.smtSettings.addSettingsListener {
            settingChanged(proof,
                    proof.settings.smtSettings,
                    InteractionListener.SettingType.SMT, null)
        }
    }

    override fun settingChanged(proof: Proof, settings: Settings, type: InteractionListener.SettingType, message: String?) {
        if (disableSettingsChanges) return
        if (isDisableAll) return

        val p = Properties()
        settings.writeSettings(p)
        val sci = SettingChangeInteraction(p, type)
        if (message != null) sci.message = message
        val log = get(proof)

        try {
            //Remove the last interaction if it was a change setting with the same type
            val last = log.interactions.get(log.interactions.size - 1)
            if (last is SettingChangeInteraction) {
                val change = last
                if (change.type === type) {
                    log.remove(log.interactions.size - 1)
                }
            }

        } catch (ex: IndexOutOfBoundsException) {
        } catch (ex: NullPointerException) {
        }

        log.add(sci)
        emit(log, sci)
    }

    override fun runPrune(node: Node) {
        if (isDisableAll) return
        val state = get(node.proof())
        val interaction = PruneInteraction(node)
        state.add(interaction)
        emit(state, interaction)
    }

    override fun runMacro(node: Node, macro: ProofMacro, posInOcc: PosInOccurrence?, info: ProofMacroFinishedInfo) {
        if (isDisableAll) return
        val state = get(node.proof())
        val interaction = MacroInteraction(node, macro, posInOcc, info)
        state.add(interaction)
        emit(state, interaction)
    }

    override fun runBuiltInRule(
        node: Node, app: IBuiltInRuleApp, rule: BuiltInRule,
        pos: PosInOccurrence, forced: Boolean) {
        if (isDisableAll) return
        val state = get(node.proof())
        val interaction = BuiltInRuleInteractionFactory.create(node, app)
        state.add(interaction)
        emit(state, interaction)
    }

    fun addListener(listener: InteractionRecorderListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: InteractionRecorderListener) {
        listeners.remove(listener)
    }

    protected fun emit(log: InteractionLog, interaction: Interaction) {
        listeners.forEach { l -> l.onInteraction(this, log, interaction) }
    }

    override fun runAutoMode(initialGoals: List<Node>, proof: Proof, info: ApplyStrategyInfo) {
        if (isDisableAll) return
        val state = get(proof)
        val interaction = AutoModeInteraction(initialGoals, info)
        state.add(interaction)
        emit(state, interaction)
    }

    override fun runRule(goal: Node, app: RuleApp) {
        if (isDisableAll) return
        val state = get(goal.proof())
        val interaction = RuleInteraction(goal, app)
        state.add(interaction)
        emit(state, interaction)
    }

    override fun autoModeStarted(e: ProofEvent) {

    }

    override fun autoModeStopped(e: ProofEvent) {

    }

    fun prioritize(interactionLog: InteractionLog) {
        instances.remove(interactionLog)
        instances.add(0, interactionLog)
    }
}
