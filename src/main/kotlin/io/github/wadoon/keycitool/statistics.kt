package io.github.wadoon.keycitool

import de.uka.ilkd.key.control.UserInterfaceControl
import de.uka.ilkd.key.logic.PosInOccurrence
import de.uka.ilkd.key.macros.AutoMacro
import de.uka.ilkd.key.macros.AutoPilotPrepareProofMacro
import de.uka.ilkd.key.macros.ProofMacro
import de.uka.ilkd.key.macros.ProofMacroFinishedInfo
import de.uka.ilkd.key.macros.SequentialProofMacro
import de.uka.ilkd.key.macros.SkipMacro
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.Proof
import de.uka.ilkd.key.prover.ProverTaskListener
import org.key_project.util.collection.ImmutableList

//region Measuring
class MeasuringMacro : SequentialProofMacro() {
    val before = Stats()
    val after = Stats()

    override fun getName() = "MeasuringMacro"
    override fun getCategory() = "ci-only"
    override fun getDescription() = "like auto but with more swag"

    override fun createProofMacroArray(): Array<ProofMacro> {
        return arrayOf(
            AutoPilotPrepareProofMacro(),
            GatherStatistics(before),
            AutoMacro(), // or TryCloseMacro()?
            GatherStatistics(after)
        )
    }
}

data class Stats(var openGoals: Int = 0, var closedGoals: Int = 0)

class GatherStatistics(val stats: Stats) : SkipMacro() {
    override fun getName() = "gather-stats"
    override fun getCategory() = "ci-only"
    override fun getDescription() = "stat purpose"

    override fun canApplyTo(
        proof: Proof?,
        goals: ImmutableList<Goal?>?,
        posInOcc: PosInOccurrence?
    ): Boolean = true

    override fun applyTo(
        uic: UserInterfaceControl?,
        proof: Proof,
        goals: ImmutableList<Goal?>?,
        posInOcc: PosInOccurrence?,
        listener: ProverTaskListener?
    ): ProofMacroFinishedInfo? { // do nothing
        stats.openGoals = proof.openGoals().size()
        stats.closedGoals = proof.getClosedSubtreeGoals(proof.root()).size()
        return super.applyTo(uic, proof, goals, posInOcc, listener)
    }
}
//endregion
