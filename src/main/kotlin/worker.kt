package org.key_project.web

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import de.uka.ilkd.key.control.DefaultUserInterfaceControl
import de.uka.ilkd.key.control.KeYEnvironment
import de.uka.ilkd.key.proof.Proof
import de.uka.ilkd.key.proof.init.ProofInputException
import de.uka.ilkd.key.proof.io.ProblemLoaderException
import de.uka.ilkd.key.settings.ProofSettings
import de.uka.ilkd.key.speclang.Contract
import de.uka.ilkd.key.util.KeYConstants
import de.uka.ilkd.key.util.KeYTypeUtil
import de.uka.ilkd.key.util.MiscTools
import java.io.File
import java.util.*
import kotlin.collections.HashMap

object Worker {
    @JvmStatic
    fun main(args: Array<String>) {
        WorkerImpl().main(args)
    }
}

private class WorkerImpl : CliktCommand() {
    val classPaths: List<File> by option("-cp").file().multiple()
    val bootClassPath by option("--bootclasspath").file()
    val includes: List<File> by option("-I").file().multiple()
    val input by argument("input").file()

    val choiceSettings by option("-C").multiple()
    val strategySettings by option("-S").multiple()

    val out = System.out

    override fun run() {
        out.println("KeY Version " + KeYConstants.VERSION)
        out.println(KeYConstants.COPYRIGHT + "\nKeY is protected by the " +
                "GNU General Public License\n")

        try {
            ensureChoiceSettings()
            setTacletOptions()

            // Load source code
            val env = KeYEnvironment.load(input, classPaths, bootClassPath, includes)

            try {
                val proofContracts = getContracts(env)
                if (proofContracts.isEmpty()) {
                    out.error("No contract found!")
                } else {
                    proofContracts.forEach { check(env, it) }
                }
            } finally {
                env.dispose() // Ensure always that all instances of KeYEnvironment are disposed
            }
        } catch (e: ProblemLoaderException) {
            out.println("<div class=\"error\">")
            println("Exception at '$input':")
            e.printStackTrace()
            out.println("</div>")
        }
    }

    private fun check(env: KeYEnvironment<*>, contract: Contract) {
        var proof: Proof? = null
        try {
            // Create proof
            proof = env.createProof(contract.createProofObl(env.initConfig, contract))
            // Set proof strategy options
            setStrategyOptions(proof)
            // Start auto mode
            env.ui.proofControl.startAndWaitForAutoMode(proof)
            // Show proof result
            val closed = proof.openGoals().isEmpty
            out.info("""Contract '${contract.displayName}' of ${contract.target} is ${if (closed) "verified" else "still open"}.""")
        } catch (e: ProofInputException) {
            out.println("<div class=\"error\">")
            out.println("""Exception at '${contract.displayName}' of ${contract.target}:<br>""")
            e.printStackTrace()
            out.println("</div>")
        } finally {
            proof?.dispose()
        }
    }

    private fun setStrategyOptions(proof: Proof) {
        val sp = proof.settings.strategySettings.activeStrategyProperties
        /*sp.setProperty(StrategyProperties.METHOD_OPTIONS_KEY, StrategyProperties.METHOD_CONTRACT)
        sp.setProperty(StrategyProperties.DEP_OPTIONS_KEY, StrategyProperties.DEP_ON)
        sp.setProperty(StrategyProperties.QUERY_OPTIONS_KEY, StrategyProperties.QUERY_ON)
        sp.setProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY, StrategyProperties.NON_LIN_ARITH_DEF_OPS)
        sp.setProperty(StrategyProperties.STOPMODE_OPTIONS_KEY, StrategyProperties.STOPMODE_NONCLOSE) */
        proof.settings.strategySettings.activeStrategyProperties = sp

        strategySettings.forEach {
            try {
                val (key, value) = it.split("=", limit = 1)
                sp[key] = value
            } catch (ignored: Exception) {
            }
        }

        // Make sure that the new options are used
        val maxSteps = 10000
        ProofSettings.DEFAULT_SETTINGS.strategySettings.maxSteps = maxSteps
        ProofSettings.DEFAULT_SETTINGS.strategySettings.activeStrategyProperties = sp
        proof.settings.strategySettings.maxSteps = maxSteps
        proof.activeStrategy = proof.services.profile.defaultStrategyFactory.create(proof, sp)
    }


    private fun getContracts(env: KeYEnvironment<DefaultUserInterfaceControl>): List<Contract> {
        // List all specifications of all types in the source location (not classPaths and bootClassPath)
        val proofContracts = LinkedList<Contract>()
        val kjts = env.javaInfo.allKeYJavaTypes
        for (type in kjts) {
            if (!KeYTypeUtil.isLibraryClass(type)) {
                val targets = env.specificationRepository.getContractTargets(type)
                for (target in targets) {
                    val contracts = env.specificationRepository.getContracts(type, target)
                    for (contract in contracts) {
                        proofContracts.add(contract)
                    }
                }
            }
        }
        return proofContracts
    }

    private fun setTacletOptions() {
        // Set Taclet options
        val choiceSettings = ProofSettings.DEFAULT_SETTINGS.choiceSettings
        val oldSettings = choiceSettings.defaultChoices
        val newSettings = HashMap<String, String>(oldSettings)
        newSettings.putAll(MiscTools.getDefaultTacletOptions())

        this.choiceSettings.forEach {
            try {
                val (key, value) = it.split("=", limit = 1)
                newSettings[key] = value
            } catch (ignored: Exception) {
            }
        }

        choiceSettings.defaultChoices = newSettings
    }

    private fun ensureChoiceSettings() {
        // Ensure that Taclets are parsed
        if (!ProofSettings.isChoiceSettingInitialised()) {
            val env = KeYEnvironment.load(input, classPaths, bootClassPath, includes)
            env.dispose()
        }
    }
}

private fun Appendable.info(str: String) {
    append("<div class=\"info\">$str</div>")
}

private fun Appendable.error(str: String) {
    append("<div class=\"error\">$str</div>")
}
