/* key-tools are extension for the KeY theorem prover.
 * Copyright (C) 2021  Alexander Weigl
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the complete terms of the GNU General Public License, please see this URL:
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package io.github.wadoon.keycitool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.google.gson.GsonBuilder
import de.uka.ilkd.key.api.KeYApi
import de.uka.ilkd.key.api.ProofManagementApi
import de.uka.ilkd.key.control.AbstractProofControl
import de.uka.ilkd.key.control.AbstractUserInterfaceControl
import de.uka.ilkd.key.control.KeYEnvironment
import de.uka.ilkd.key.java.Position
import de.uka.ilkd.key.macros.scripts.ProofScriptEngine
import de.uka.ilkd.key.parser.Location
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.Node
import de.uka.ilkd.key.proof.Proof
import de.uka.ilkd.key.proof.Statistics
import de.uka.ilkd.key.settings.ChoiceSettings
import de.uka.ilkd.key.settings.ProofSettings
import de.uka.ilkd.key.speclang.Contract
import de.uka.ilkd.key.util.KeYConstants
import de.uka.ilkd.key.util.MiscTools
import io.github.wadoon.keycitool.Ansi.BLUE
import io.github.wadoon.keycitool.Ansi.GREEN
import io.github.wadoon.keycitool.Ansi.RED
import io.github.wadoon.keycitool.Ansi.colorfg
import io.github.wadoon.keycitool.Ansi.debug
import io.github.wadoon.keycitool.Ansi.fail
import io.github.wadoon.keycitool.Ansi.fine
import io.github.wadoon.keycitool.Ansi.info
import io.github.wadoon.keycitool.Ansi.printBlock
import io.github.wadoon.keycitool.Ansi.printm
import io.github.wadoon.keycitool.Ansi.warn
import io.github.wadoon.keycitool.junit.TestCaseKind
import io.github.wadoon.keycitool.junit.TestSuites
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

/**
 * A small interface for a checker scripts
 * @author Alexander Weigl
 * @version 1 (21.11.19)
 */
class Checker : CliktCommand() {
    private val statistics = TreeMap<String, Any>()

    enum class ColorMode { YES, NO, AUTO }

    val color by option("--color").enum<ColorMode>().default(ColorMode.AUTO)

    val junitXmlOutput by option("--xml-output").file()

    val enableMeasuring: Boolean by option(
        "--measuring",
        help = "try to measure proof coverage"
    ).flag()

    val includes by option(
        help = "defines an additional key file to be included (can be repeated)"
    ).multiple()

    val autoModeStep by option(
        "--auto-mode-max-step", metavar = "INT",
        help = "maximal amount of steps in auto-mode [default:10000]"
    )
        .int().default(10000)

    val verbose by option("-v", "--verbose", help = "verbose output")
        .flag("--no-verbose")

    val debug by option("-d", "--debug", help = "more verbose output")
        .flag("--no-debug")

    val readContractNames by option(
        "--read-contract-names-from-file",
        help = "if set, the contract names are read from the proof file contents"
    )
        .flag()

    val disableAutoMode by option(
        "--no-auto-mode",
        help = "If set, only contracts with a proof script or proof are replayed."
    )
        .flag()

    val statisticsFile: File? by option(
        "-s",
        "--statistics",
        help = "if set, JSON files with proof statistics are written"
    ).file()

    val appendStatistics by option(
        "--append-stat",
        help = "Normally, the `statisticsFile' is overriden by the ci-too. " +
                "If set the statistics are appended to the JSON data structure."
    ).flag()

    val dryRun by option(
        "--dry-run",
        help = "skipping the proof reloading, scripts execution and auto mode." +
                " Useful for finding the contract names"
    ).flag()

    val classpath by option(
        "--classpath", "-cp",
        help = "additional classpaths"
    ).multiple()

    val bootClassPath by option(
        "--bootClassPath", "-bcp",
        help = "set the bootclasspath"
    )

    val onlyContracts by option(
        "--contract",
        help = "include a contract by name (can be repeated)"
    )
        .multiple()

    val forbidContracts by option(
        "--forbid-contract",
        help = "exclude a contract by name (can be repeated)"
    )
        .multiple()

    val inputFile by argument(
        "JAVA-KEY-FILE",
        help = "key, java or a folder"
    )
        .multiple(true)

    val proofPath by option(
        "--proof-path",
        help = "folder to look for proofs and script files (can be repeated)"
    )
        .multiple()

    val defaultScript by option(
        "--default-script", help = "A file holding a default script. Note, this option will disable " +
                "the full-auto-macro as the default fallback."
    )
        .file(mustExist = true, canBeDir = false)


    private var choiceSettings: ChoiceSettings? = null

    var errors = 0

    var testSuites = TestSuites()

    override fun run() {
        Ansi.useColor = when (color) {
            ColorMode.YES -> true
            ColorMode.AUTO -> System.console() != null || System.getenv("GIT_PAGER_IN_USE") != null
            ColorMode.NO -> false
        }

        printm("KeY version: ${KeYConstants.VERSION}")
        printm("KeY internal: ${KeYConstants.INTERNAL_VERSION}")
        printm("Copyright: ${KeYConstants.COPYRIGHT}")
        printm("More information at: https://formal.iti.kit.edu/weigl/ci-tool/")

        if (debug) {
            printm("Proof files and Sripts found: ")
            proofFileCandidates.forEach {
                printm(it.absolutePath)
            }
        }

        testSuites.name = inputFile.joinToString(" ")

        inputFile.forEach { run(it) }

        statisticsFile?.let { statisticsFile ->
            val gson = GsonBuilder().disableJdkUnsafe().serializeNulls().setPrettyPrinting().create()

            if (appendStatistics) {
                val stat = gson.fromJson(statisticsFile.readText(), TreeMap::class.java) as TreeMap<String, Any>
                stat.putAll(statistics)
                statisticsFile.bufferedWriter().use {
                    gson.toJson(stat, it)
                }
            } else {
                statisticsFile.bufferedWriter().use {
                    gson.toJson(statistics, it)
                }
            }
        }

        junitXmlOutput?.let { file ->
            file.bufferedWriter().use {
                testSuites.writeXml(it)
            }
        }

        exitProcess(errors)
    }


    fun run(inputFile: String) {
        printBlock("Start with `$inputFile`") {
            val pm = KeYApi.loadProof(
                File(inputFile),
                classpath.map { File(it) },
                bootClassPath?.let { File(it) },
                includes.map { File(it) }
            )

            val contracts = pm.proofContracts
                .filter { it.name in onlyContracts || onlyContracts.isEmpty() }

            info("Found: ${contracts.size}")
            var successful = 0
            var ignored = 0
            var failure = 0
            var error = 0

            val testSuite = testSuites.newTestSuite(inputFile)
            ProofSettings.DEFAULT_SETTINGS.configuration.entries.forEach { (t, u) ->
                testSuite.properties[t.toString()] = u
            }

            for (c in contracts) {
                val testCase = testSuite.newTestCase(c.name)
                printBlock("[INFO] Contract: `${c.name}`") {
                    when {
                        c.name in forbidContracts -> {
                            printm(" [INFO] Contract excluded by `--forbid-contract`")
                            testCase.result = TestCaseKind.Skipped("Contract excluded by `--forbid-contract`.")
                            ignored++
                        }

                        dryRun -> {
                            printm("[INFO] Contract skipped by `--dry-run`")
                            testCase.result = TestCaseKind.Skipped("Contract skipped by `--dry-run`.")
                            ignored++
                        }

                        else -> {
                            when (runContract(pm, c)) {
                                ProofState.Success -> successful++
                                ProofState.Failed -> {
                                    testCase.result = TestCaseKind.Failure("Proof not closeable.")
                                    failure++
                                }

                                ProofState.Skipped -> ignored++
                                ProofState.Error -> error++
                            }
                        }
                    }
                }
            }
            printm(
                "[INFO] Summary for $inputFile: " +
                        "(successful/ignored/failure) " +
                        "(${colorfg(successful, GREEN)}/${colorfg(ignored, BLUE)}/${colorfg(failure, RED)})"
            )
            if (failure != 0)
                error("$inputFile failed!")
        }
    }

    private fun runContract(pm: ProofManagementApi, contract: Contract): ProofState {
        val proofApi = pm.startProof(contract)
        val proof = proofApi.proof
        require(proof != null)
        proof.settings?.strategySettings?.maxSteps = autoModeStep
        ProofSettings.DEFAULT_SETTINGS.strategySettings.maxSteps = autoModeStep

        val proofFile = findProofFile(contract.name)
        val scriptFile = findScriptFile(contract.name)
        val ui = proofApi.env.ui as AbstractUserInterfaceControl
        val pc = proofApi.env.proofControl as AbstractProofControl

        val closed = when {
            proofFile != null -> {
                info("Proof found: $proofFile. Try loading.")
                loadProof(proofFile)
            }

            scriptFile != null -> {
                info("Script found: $scriptFile. Try proving.")
                loadScript(ui, proof, scriptFile)
            }

            else -> {
                if (verbose)
                    info("No proof or script found. Fallback to auto-mode.")
                if (disableAutoMode) {
                    warn("Proof skipped because `--no-auto-mode' switch is set.")
                    ProofState.Skipped
                } else {
                    runDefaultFallback(ui, pc, proof)
                }
            }
        }

        when (closed) {
            ProofState.Success -> fine("✔ Proof closed.")
            ProofState.Skipped -> warn("! Proof skipped.")
            ProofState.Failed -> {
                errors++
                error("✘ Proof open.")
                debug("${proof.openGoals().size()} remains open")
            }

            ProofState.Error -> fail("Could not load proof due to exception in KeY.")
        }
        proof.dispose()
        return closed
    }

    private fun runDefaultFallback(ui: AbstractUserInterfaceControl, pc: AbstractProofControl, proof: Proof)
            : ProofState = defaultScript?.let { script ->
        info("Using default script for fallback: $script. Try proving.")
        loadScript(ui, proof, script)
    } ?: runAutoMode(pc, proof)

    private fun runAutoMode(proofControl: AbstractProofControl, proof: Proof): ProofState {
        val time = measureTimeMillis {
            if (enableMeasuring) {
                val mm = MeasuringMacro()
                proofControl.runMacro(proof.root(), mm, null)
                proofControl.waitWhileAutoMode()
                info("Proof has open/closed before: ${mm.before}")
                info("Proof has open/closed after: ${mm.after}")
            } else {
                try {
                    proofControl.startAndWaitForAutoMode(proof)
                } catch (e: Exception) {
                    fail("Error in KeY during auto mode. ${e.javaClass} : ${e.message}")
                    return ProofState.Error
                }
            }
        }
        if (verbose) {
            fine("Auto-mode took ${time / 1000.0} seconds.")
        }
        printStatistics(proof)
        return if (proof.closed()) ProofState.Success else ProofState.Failed
    }

    private fun loadScript(ui: AbstractUserInterfaceControl, proof: Proof, scriptFile: File): ProofState {
        val script = scriptFile.readText()
        val engine = ProofScriptEngine(script, Location(scriptFile.toURI(), Position.newOneBased(1, 1)))
        return try {
            val time = measureTimeMillis {
                engine.execute(ui, proof)
            }
            info("Script execution took ${time / 1000.0} seconds.")
            printStatistics(proof)
            if (proof.closed()) ProofState.Success else ProofState.Failed
        } catch (e: Exception) {
            fail("Error in KeY during auto mode. ${e.javaClass} : ${e.message}")
            ProofState.Error
        }
    }

    private fun loadProof(keyFile: File): ProofState {
        val env = try {
            KeYEnvironment.load(keyFile)
        } catch (e: Exception) {
            error("Error during loading the KeY file. Exception: ${e.javaClass} ${e.message}")
            return ProofState.Error
        }

        val script = env.proofScript
        if (script != null) {
            info("Executing script from key file.")
            val pse = ProofScriptEngine(script.first, script.second)
            pse.execute(env.ui, env.loadedProof);
        }

        try {
            val proof = env?.loadedProof
            try {
                if (proof == null) {
                    fail("No proof found in given KeY-file.")
                    return ProofState.Failed
                }
                printStatistics(proof)
                return if (proof.closed()) ProofState.Success else ProofState.Failed
            } finally {
                proof?.dispose()
            }
        } finally {
            env.dispose()
        }
    }

    private fun printStatistics(proof: Proof) {
        if (statisticsFile != null) {
            statistics[proof.name().toString()] = generateSummary(proof)
        }
        if (verbose) {
            proof.statistics.summary.forEach { p -> debug("${p.first} = ${p.second}") }
        }
        if (enableMeasuring) {
            val closedGoals = proof.getClosedSubtreeGoals(proof.root())
            val visitLineOnClosedGoals = HashSet<Pair<URI, Int>>()
            closedGoals.forEach {
                it.pathToRoot.forEach {
                    val p = it.nodeInfo.activeStatement?.positionInfo
                    if (p != null) {
                        visitLineOnClosedGoals.add(p.uri.get() to p.startPosition.line())
                    }
                }
            }
            info("Visited lines:\n${visitLineOnClosedGoals.joinToString("\n")}")
        }
    }

    val proofFileCandidates: List<File> by lazy {
        proofPath.asSequence()
            .flatMap { File(it).walkTopDown().asSequence() }
            .filter { it.isFile }
            .filter { it.name.endsWith(".proof") || it.name.endsWith(".proof.gz") }
            .toList()
            .sorted()
    }

    val contractNameToProofFile by lazy {
        if (readContractNames) {
            proofFileCandidates.associateBy { extractContractName(it) }
        } else {
            hashMapOf()
        }
    }

    private fun findProofFile(contractName: String): File? =
        if (contractName in contractNameToProofFile) {
            contractNameToProofFile[contractName]
        } else {
            val filename = MiscTools.toValidFileName(contractName)
            proofFileCandidates.find {
                val name = it.name
                name.startsWith(filename) && (name.endsWith(".proof") || name.endsWith(".proof.gz"))
            }
        }

    private fun findScriptFile(filename: String): File? =
        proofFileCandidates.find {
            val name = it.name
            name.startsWith(filename) && (name.endsWith(".txt") || name.endsWith(".pscript"))
        }
}

fun main(args: Array<String>) = Checker().main(args)

private val Goal.pathToRoot: Sequence<Node>
    get() {
        return generateSequence(node()) { it.parent() }
    }

private fun Proof.openClosedProgramBranches(): Pair<Int, Int> {
    val branchingNodes = this.root().subtreeIterator().asSequence()
        .filter { it.childrenCount() > 1 }
    val programBranchingNodes = branchingNodes.filter {
        val childStmt = it.childrenIterator().asSequence().map { child ->
            child.nodeInfo.activeStatement
        }
        childStmt.any { c -> c != it.nodeInfo.activeStatement }
    }

    val diverseProgramBranches = programBranchingNodes.filter { parent ->
        !parent.isClosed && parent.childrenIterator().asSequence().any { it.isClosed }
    }

    return diverseProgramBranches.count() to programBranchingNodes.count()
}

/**
 * Copied from KeY, but provide a better map
 */
private fun generateSummary(proof: Proof): HashMap<String, Any> {
    val result = HashMap<String, Any>()
    val stat: Statistics = proof.statistics
    result["Nodes"] = stat.nodes
    result["Branches"] = stat.branches
    result["Interactive steps"] = stat.interactiveSteps
    result["Symbolic execution steps"] = stat.symbExApps
    result["Automode time"] = proof.autoModeTime
    result["Avg. time per step"] = stat.timePerStepInMillis
    result["Quantifier instantiations"] = stat.quantifierInstantiations
    result["One-step Simplifier apps"] = stat.ossApps
    result["SMT solver apps"] = stat.smtSolverApps
    result["Dependency Contract apps"] = stat.dependencyContractApps
    result["Operation Contract apps"] = stat.operationContractApps
    result["Block/Loop Contract apps"] = stat.blockLoopContractApps
    result["Loop invariant apps"] = stat.loopInvApps
    result["Merge Rule apps"] = stat.mergeRuleApps
    result["Total rule apps"] = stat.totalRuleApps
    result["Interactive Rule Apps"] = stat.interactiveAppsDetails
    return result
}

internal fun extractContractName(it: File): String? {
    val input = if (it.name.endsWith(".gz")) {
        GZIPInputStream(FileInputStream(it))
    } else {
        FileInputStream(it)
    }
    input.bufferedReader().use { incoming ->
        val contractLine = incoming.lineSequence().find { it.startsWith("name=") }
        return contractLine?.trim()?.substring("name=".length)
    }
}

/**
 * State of a proof after execution of KeY.
 */
enum class ProofState {
    /** Execution was successfully finished, i.e., proof is closed. */
    Success,

    /** Proof could not be closed, no exception appeared. */
    Failed,

    /** Proof was skipped due to user options. */
    Skipped,

    /** Loading and proving resulted into an error. */
    Error
}
