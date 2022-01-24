package org.key_project.ui.interactionlog.model

import de.uka.ilkd.key.gui.WindowUserInterfaceControl
import de.uka.ilkd.key.logic.PosInOccurrence
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.Node
import de.uka.ilkd.key.rule.*
import de.uka.ilkd.key.rule.merge.MergeRuleBuiltInRuleApp
import de.uka.ilkd.key.smt.RuleAppSMT
import de.uka.ilkd.key.ui.AbstractMediatorUserInterfaceControl

object BuiltInRuleInteractionFactory {
    fun <T : IBuiltInRuleApp> create(node: Node, app: T): BuiltInRuleInteraction {
        return when (app) {
            is OneStepSimplifierRuleApp -> OSSBuiltInRuleInteraction(app, node)
            is ContractRuleApp -> ContractBuiltInRuleInteraction(app, node)
            is UseDependencyContractApp -> UseDependencyContractBuiltInRuleInteraction(app, node)
            is LoopContractInternalBuiltInRuleApp -> LoopContractInternalBuiltInRuleInteraction(app, node)
            is LoopInvariantBuiltInRuleApp -> LoopInvariantBuiltInRuleInteraction(app, node)
            is MergeRuleBuiltInRuleApp -> MergeRuleBuiltInRuleInteraction(app, node)
            is RuleAppSMT -> SMTBuiltInRuleInteraction(app, node)
            else -> throw IllegalArgumentException()
        }
    }
}


sealed class BuiltInRuleInteraction() : NodeInteraction() {
    var ruleName: String? = null
    var nodeIdentifier: NodeIdentifier? = null
    var occurenceIdentifier: OccurenceIdentifier? = null

    constructor(node: Node, pio: PosInOccurrence) : this() {
        this.nodeIdentifier = NodeIdentifier.create(node)
        this.occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), pio)
    }
}

/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class ContractBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    var contractType: String? = null
    var contractName: String? = null

    constructor(app: ContractRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        contractName = app.instantiation.name
        contractType = app.instantiation.typeName
    }

    override fun toString() = "Contract ${contractName} applied"

    override val proofScriptRepresentation: String
        get() = "contract $contractName"

    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        super.reapplyStrict(uic, goal)
    }

    override fun reapplyRelaxed(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        super.reapplyRelaxed(uic, goal)
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class LoopContractInternalBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    var displayName: String? = null
    var contractName: String? = null

    constructor(app: LoopContractInternalBuiltInRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        contractName = app.contract.name
        displayName = app.contract.displayName
        println(app.statement)
        println(app.executionContext)
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class LoopInvariantBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    var displayName: String? = null
    var contractName: String? = null

    constructor(app: LoopInvariantBuiltInRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        println(app.loopStatement)
        println(app.executionContext)
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class MergeRuleBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    constructor(app: MergeRuleBuiltInRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class OSSBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    override val markdown: String
        get() = String.format(
            "## One step simplification%n" + "* applied on %n  * Term:%s%n  * Toplevel %s%n",
            occurenceIdentifier?.term,
            occurenceIdentifier?.toplevelFormula
        )

    override val proofScriptRepresentation: String
        get() = String.format(
            "one_step_simplify %n" +
                    "\t     on = \"%s\"%n" +
                    "\tformula = \"%s\"%n;%n",
            occurenceIdentifier?.term,
            occurenceIdentifier?.toplevelFormula
        )

    constructor(app: OneStepSimplifierRuleApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
    }

    override fun toString(): String {
        return "one step simplification on" + occurenceIdentifier?.term
    }

    @Throws(Exception::class)
    fun reapply(uic: WindowUserInterfaceControl, goal: Goal) {
        val oss = OneStepSimplifier()
        val pio = occurenceIdentifier!!.rebuildOn(goal)
        val app = oss.createApp(pio, goal.proof().services)
        goal.apply(app)
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class SMTBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    constructor(app: RuleAppSMT, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
        println(app.ifInsts())
    }

    override val proofScriptRepresentation: String
        get() = "smt"

    override fun reapplyStrict(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        super.reapplyStrict(uic, goal)
    }

    override fun reapplyRelaxed(uic: AbstractMediatorUserInterfaceControl, goal: Goal) {
        super.reapplyRelaxed(uic, goal)
    }
}


/**
 * @author Alexander Weigl
 * @version 1 (09.12.18)
 */
class UseDependencyContractBuiltInRuleInteraction() : BuiltInRuleInteraction() {
    constructor(app: UseDependencyContractApp, node: Node) : this() {
        nodeIdentifier = NodeIdentifier.create(node)
        occurenceIdentifier = OccurenceIdentifier.create(node.sequent(), app.posInOccurrence())
    }
}
