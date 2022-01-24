package org.key_project.ui.interactionlog.model

import de.uka.ilkd.key.java.Services
import de.uka.ilkd.key.logic.*
import de.uka.ilkd.key.logic.op.SchemaVariable
import de.uka.ilkd.key.macros.scripts.ScriptException
import de.uka.ilkd.key.nparser.KeyIO
import de.uka.ilkd.key.pp.LogicPrinter
import de.uka.ilkd.key.proof.Goal
import de.uka.ilkd.key.proof.rulefilter.TacletFilter
import de.uka.ilkd.key.rule.*
import de.uka.ilkd.key.util.parsing.BuildingExceptions
import org.key_project.util.collection.ImmutableMapEntry

class RuleHelper(
    val goal: Goal,
    val ruleName: String,
    val occId: OccurenceIdentifier? = null,
    val tacletArguments: Map<String, String> = HashMap(),
    val pos: Int? = null,
    val strictSearchStrategy: Boolean = true
) {
    private val proof = goal.proof()
    private val services = proof.services
    private val rulename = Name(ruleName)
    private val pio = occId?.rebuildOn(goal)


    fun makeRuleApp(): RuleApp? {
        val builtInRule: BuiltInRule? = proof.initConfig.profile.standardRules.standardBuiltInRules
            .find { it.name() == rulename }
        if (builtInRule != null) {
            return makeBuiltInRuleApp(builtInRule)
        }

        val taclet = proof.env.initConfigForEnvironment.lookupActiveTaclet(rulename)
        if (taclet != null) {
            val theApp = if (taclet is NoFindTaclet) {
                makeNoFindTacletApp(taclet)
            } else {
                findTacletApp(pos ?: -1)
            }
            return theApp.let { instantiateTacletApp(it) }
        }

        val localApp: NoPosTacletApp? = goal.indexOfTaclets().lookup(rulename)
        if (localApp != null) {
            if (localApp.taclet() is FindTaclet) {
                return findTacletApp()
            }
            return localApp
        }
        return null
    }

    private fun instantiateTacletApp(theApp: TacletApp): TacletApp? {
        var result: TacletApp
        val services = goal.proof().services
        val candidates = theApp.findIfFormulaInstantiations(goal.sequent(), services).toList()
        val assumesCandidates = filterList(candidates)
        if (assumesCandidates.size != 1) {
            throw ScriptException("Not a unique \\assumes instantiation")
        }
        result = assumesCandidates.first()

        var recheckMatchConditions = false
        /*
     * (DS, 2019-01-31): Try to instantiate first, otherwise, we cannot
     * apply taclets with "\newPV", Skolem terms etc.
     */
        result.tryToInstantiateAsMuchAsPossible(
            services.getOverlay(goal.localNamespaces)
        )
            ?.let {
                result = it
                recheckMatchConditions = true
            }
        recheckMatchConditions = recheckMatchConditions and !(result.uninstantiatedVars()?.isEmpty ?: false)
        for (sv: SchemaVariable in result.uninstantiatedVars()) {
            if (result.isInstantiationRequired(sv)) {
                val inst: Term = term(sv)
                    ?: throw ScriptException("missing instantiation for $sv")
                result = result.addInstantiation(sv, inst, true, services)
            }
        }

        // try to instantiate remaining symbols
        val res = result.tryToInstantiate(services.getOverlay(goal.localNamespaces))
            ?: throw ScriptException("Cannot instantiate this rule")
        if (recheckMatchConditions) {
            val appMC = res.taclet().matcher.checkConditions(res.matchConditions(), services)
            return appMC?.let { res.setMatchConditions(it, services) }
        }
        return res
    }

    private fun term(sv: SchemaVariable): Term? {
        try {
            return tacletArguments[sv.toString()]?.let { KeyIO(services).parseExpression(it) }
        } catch (e: BuildingExceptions) {
            e.printStackTrace()
        }
        return null
    }

    fun makeNoFindTacletApp(taclet: Taclet): TacletApp {
        return NoPosTacletApp.createNoPosTacletApp(taclet)
    }

    fun makeBuiltInRuleApp(rule: BuiltInRule): IBuiltInRuleApp {
        val matchingApps = getBuiltInRuleApps(rule)
        if (matchingApps.isEmpty()) throw ScriptException("No matching applications.")

        //get first or die?
        if (matchingApps.size > 1) {
            throw ScriptException("More than one applicable occurrence")
        }
        val builtInRuleApp = matchingApps[0]
        /*} else {
            if (p.occ >= matchingApps.size) {
                throw ScriptException("Occurence ${p.occ} has been specified, but there are only ${matchingApps.size} hits.")
            }
            return matchingApps[p.occ]
        }*/

        if (builtInRuleApp.isSufficientlyComplete) {
            return builtInRuleApp.forceInstantiate(goal)
        }
        return builtInRuleApp
    }

    fun getBuiltInRuleApps(rule: BuiltInRule): List<IBuiltInRuleApp> {
        val matchingApps = findBuiltInRuleApps()
            .filter { r: IBuiltInRuleApp -> (r.rule().name() == rule.name()) }
            .toList()
        return matchingApps
    }

    fun findTacletApp(occ: Int = -1): TacletApp {
        val allApps = findTacletApps()
        val matchingApps = filterList(allApps)
        return if (occ < 0) {
            matchingApps.firstOrNull() ?: throw ScriptException()
        } else {
            matchingApps.getOrNull(occ) ?: throw ScriptException()
        }
    }

    fun findBuiltInRuleApps(): Sequence<IBuiltInRuleApp> {
        val services = goal.proof().services
        assert(services != null)
        val index = goal.ruleAppIndex()
            .builtInRuleAppIndex()

        val antecApp = goal.node().sequent().antecedent()
            .asSequence()
            .filter { isFormulaSearchedFor(it) }
            .flatMap {
                index.getBuiltInRule(goal, PosInOccurrence(it, PosInTerm.getTopLevel(), true))
            }

        val succApp = goal.node().sequent().succedent()
            .asSequence()
            .filter { isFormulaSearchedFor(it) }
            .flatMap {
                index.getBuiltInRule(goal, PosInOccurrence(it, PosInTerm.getTopLevel(), false))
            }
        return antecApp + succApp
    }

    fun findTacletApps(): List<TacletApp> {
        val filter: TacletFilter = TacletPredicate(rulename)
        val index = goal.ruleAppIndex()
        index.autoModeStopped()
        val allApps = ArrayList<TacletApp>()

        if (pio != null) {
            val apps = index.getTacletAppAtAndBelow(filter, pio, services).toList()
            if (apps.isNotEmpty()) {
                return apps
            }
        }

        if (strictSearchStrategy)
            throw ScriptException("Could not find taclet by position")


        for (sf: SequentFormula in goal.node().sequent().antecedent()) {
            if (!isFormulaSearchedFor(sf)) {
                continue
            }
            allApps += index.getTacletAppAtAndBelow(
                filter, PosInOccurrence(sf, PosInTerm.getTopLevel(), true),
                services
            )
        }
        for (sf: SequentFormula in goal.node().sequent().succedent()) {
            if (!isFormulaSearchedFor(sf)) {
                continue
            }
            allApps += index.getTacletAppAtAndBelow(
                filter, PosInOccurrence(sf, PosInTerm.getTopLevel(), false), services
            )
        }
        return allApps
    }

    /**
     * Returns true iff the given [SequentFormula] either matches the
     * [occId] parameter.
     * @param p
     * The [Parameters] object.
     * @param sf
     * The [SequentFormula] to check.
     * @return true if `sf` matches.
     */
    private fun isFormulaSearchedFor(sf: SequentFormula): Boolean {
        val formula = occId?.toplevelFormula
        if (formula != null) {
            val actual = sf.formula().formatTermString(services)
            if (actual == formula) {
                return true
            }
        }
        return false
    }

    /**
     * Filter those apps from a list that are according to the parameters.
     */
    private fun filterList(list: List<TacletApp>): List<TacletApp> =
        list.filterIsInstance<PosTacletApp>()
            .filter { pta ->
                val add = pio != null || pio == pta.posInOccurrence()
                val args = pta.arguments()
                add and (args == tacletArguments)
            }
}

fun TacletApp.arguments(): Map<String, String> = instantiations().pairIterator().asSequence()
    .map { (k, v) ->
        val inst = v.instantiation
        val s = if (inst is Term) {
            LogicPrinter.quickPrintTerm(inst, null)
        } else {
            inst.toString()
        }
        k.toString() to s
    }.toMap()

private operator fun <S, T> ImmutableMapEntry<S, T>.component1() = key()
private operator fun <S, T> ImmutableMapEntry<S, T>.component2() = value()


/**
 * Removes spaces and line breaks from the string representation of a term.
 *
 * @param str
 * The string to "clean up".
 * @return The original without spaces and line breaks.
 */
private fun Term.formatTermString(services: Services? = null): String =
    LogicPrinter.quickPrintTerm(this, services)
        .replace("[\n \t\r]+", " ")


private class TacletPredicate(private val rulename: Name) : TacletFilter() {
    override fun filter(taclet: Taclet): Boolean {
        return (taclet.name() == rulename)
    }
}
