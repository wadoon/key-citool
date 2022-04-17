package com.github.wadoon.keytools;

import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.rule.RuleSet;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.strategy.NumberRuleAppCost;
import de.uka.ilkd.key.strategy.RuleAppCost;
import de.uka.ilkd.key.strategy.RuleAppCostCollector;
import de.uka.ilkd.key.strategy.Strategy;
import org.jetbrains.annotations.NotNull;
import org.key_project.util.collection.ImmutableList;

import java.util.*;
import java.util.function.Function;

/**
 * @author Alexander Weigl
 * @version 1 (30.05.18)
 */
public class AdaptableStrategy implements Strategy {
    /**
     *
     */
    protected final Strategy delegate;
    /**
     * Set of forbidden rulesets. A taclet is not applied if it is belongs at least to ONE forbidden
     * rule set.
     */
    protected Set<String> disabledRulesBySet = new HashSet<>();

    /**
     *
     */
    protected Set<String> disabledRulesByName = new HashSet<>();

    protected CostAdapter costAdapter = new NeutralCostAdapter();

    /**
     * @param delegate
     */
    public AdaptableStrategy(Strategy delegate) {
        this.delegate = delegate;
    }

    public static CostAdapter createFromProperties(Properties p) {
        RuleNameAndSetCostAdapter adapter = RuleNameAndSetCostAdapter.createDefault();
        adapter.loadFrom(p);
        return adapter;
    }

    /**
     * @return
     */
    @Override
    public boolean isStopAtFirstNonCloseableGoal() {
        return delegate.isStopAtFirstNonCloseableGoal();
    }

    @Override
    public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
        if (disabledRulesByName.contains(app.rule().name().toString())) {
            return false;
        }

        if (!disabledRulesBySet.isEmpty()) {//guard for performance
            try {
                Taclet t = (Taclet) app.rule();
                boolean hit = t.getRuleSets().stream().anyMatch(rs ->
                        disabledRulesBySet.contains(rs.name().toString()));
                if (hit) {
                    //cache the decision
                    disabledRulesByName.add(t.name().toString());
                    return true;
                }
            } catch (ClassCastException ignored) {

            }
        }

        return delegate.isApprovedApp(app, pio, goal);
    }

    @Override
    public void instantiateApp(RuleApp app, PosInOccurrence pio,
                               Goal goal, RuleAppCostCollector collector) {
        delegate.instantiateApp(app, pio, goal, collector);
    }

    @Override
    public Name name() {
        return new Name(getClass().getSimpleName());
    }

    @Override
    public RuleAppCost computeCost(RuleApp app, PosInOccurrence pos, Goal goal) {
        /*// if new cost is zero, then return immediately
        String ruleName = app.rule().name().toString();
        if (factorForRuleNames.getOrDefault(ruleName, -1L) == 0D) {
            return NumberRuleAppCost.getZeroCost();
        }

        long rSF = ruleSetFactor(app);
        if (rSF == 0) {
            return NumberRuleAppCost.getZeroCost();
        } else if (rSF != 1) {
            defaultCost = defaultCost.mul(NumberRuleAppCost.create(rSF));
        }

        if (!factorForRuleNames.containsKey(ruleName)) {
            return defaultCost;
        } else {
            NumberRuleAppCost factorCost = (NumberRuleAppCost) NumberRuleAppCost.create(factorForRuleNames.get(ruleName));
            return factorCost.mul(defaultCost);
        }*/
        RuleAppCost defaultCost = delegate.computeCost(app, pos, goal);
        return costAdapter.computeCost(defaultCost, app, pos, goal);
    }

    public void loadFrom(Properties p) {
        p.forEach((k, v) -> {
            String key = k.toString();
            String t = key.substring(key.indexOf('.'), key.lastIndexOf('.'));
            if (key.startsWith("rule.") && key.endsWith(".disabled") && Boolean.valueOf(v.toString())) {
                disabledRulesByName.add(t);
            } else {
                disabledRulesByName.remove(t);
            }

            if (key.startsWith("ruleset.") && key.endsWith(".disabled") && Boolean.valueOf(v.toString())) {
                disabledRulesBySet.add(t);
            } else {
                disabledRulesBySet.remove(t);
            }
        });
    }

    public void storeInto(Properties p) {
        disabledRulesByName.forEach(s -> p.put(s + ".disabled", "true"));
    }

    interface CostAdapter {
        /**
         * Based on the old cost of the rule given rule application, its position in occurence and the goal
         * should this method return the new cost.
         */
        @NotNull RuleAppCost computeCost(@NotNull RuleAppCost oldCost, @NotNull RuleApp app,
                                         @NotNull PosInOccurrence pos,
                                         @NotNull Goal goal);
    }

    /**
     * This cost adapter is based on sets. You need to give a project function <code>(RuleApp -> Key)</code>,
     * then you can define the new cost in the summandMap and factorMap.
     *
     * @param <T> the lookup key, typically a string.
     */
    public static class SetBasedCostAdapter<T> extends LinearCostAdapater {
        protected Map<T, Long> summandMap = new HashMap<>();
        protected Map<T, Long> factorMap = new HashMap<>();
        protected Function<RuleApp, T> translateApp;

        public SetBasedCostAdapter(Function<RuleApp, T> translate) {
            this.translateApp = translate;

            Function<RuleApp, RuleAppCost> summandNeutral = summand;
            Function<RuleApp, RuleAppCost> factorNeutral = factor;

            this.summand = (RuleApp app) -> {
                T key = this.translateApp.apply(app);
                if (summandMap.containsKey(key)) {
                    return NumberRuleAppCost.create(summandMap.get(key));
                }
                return summandNeutral.apply(app);
            };

            this.factor = (RuleApp app) -> {
                T key = this.translateApp.apply(app);
                if (factorMap.containsKey(key)) {
                    return NumberRuleAppCost.create(factorMap.get(key));
                }
                return factorNeutral.apply(app);
            };
        }

        public Map<T, Long> getSummandMap() {
            return summandMap;
        }

        public Map<T, Long> getFactorMap() {
            return factorMap;
        }
    }

    /**
     * Class bundles two cost adapter: one for the rule name and one for the rulset.
     */
    public static class RuleNameAndSetCostAdapter implements CostAdapter {
        private final SetBasedCostAdapter<String> ruleSet;
        private final SetBasedCostAdapter<String> ruleName;

        public RuleNameAndSetCostAdapter(SetBasedCostAdapter<String> ruleSet, SetBasedCostAdapter<String> ruleName) {
            this.ruleSet = ruleSet;
            this.ruleName = ruleName;
        }

        public static RuleNameAndSetCostAdapter createDefault() {
            return new RuleNameAndSetCostAdapter(
                    new SetBasedCostAdapter<>(app -> app.rule().name().toString()),
                    new SetBasedCostAdapter<>(app -> {
                        try {
                            Taclet t = (Taclet) app.rule();
                            ImmutableList<RuleSet> rs = t.getRuleSets();
                            return rs.head().name().toString();
                        } catch (ClassCastException e) {
                            return "";
                        }
                    })
            );
        }

        public SetBasedCostAdapter<String> getRuleSet() {
            return ruleSet;
        }

        public SetBasedCostAdapter<String> getRuleName() {
            return ruleName;
        }

        @Override
        public RuleAppCost computeCost(@NotNull RuleAppCost oldCost, @NotNull RuleApp app, PosInOccurrence pos, @NotNull Goal goal) {
            return ruleName.computeCost(ruleSet.computeCost(oldCost, app, pos, goal), app, pos, goal);
        }

        public void loadFrom(Properties p) {
            for (Map.Entry<Object, Object> entry : p.entrySet()) {
                Object k = entry.getKey();
                Object v = entry.getValue();
                try {
                    String key = k.toString();

                    Map<String, Long> fMap, sMap;
                    if (key.startsWith("ruleset.")) {
                        key = key.substring(8);
                        fMap = ruleSet.factorMap;
                        sMap = ruleSet.summandMap;
                    } else if (key.startsWith("rule.")) {
                        key = key.substring(5);
                        fMap = ruleName.factorMap;
                        sMap = ruleName.summandMap;
                    } else {
                        continue;
                    }

                    long value = Long.parseLong(v.toString());
                    if (key.endsWith(".factor")) {
                        fMap.put(key.substring(0, key.lastIndexOf('.')), value);
                    }

                    if (key.endsWith(".summand")) {
                        sMap.put(key.substring(0, key.lastIndexOf('.')), value);
                    }

                } catch (Exception e) {

                }
            }
        }

        public void storeInto(Properties p) {
            store(p, "ruleset.", ".factor", ruleSet.factorMap);
            store(p, "ruleset.", ".summand", ruleSet.summandMap);
            store(p, "rule.", ".factor", ruleName.factorMap);
            store(p, "rule.", ".summand", ruleName.summandMap);
        }

        private void store(Properties p, String prefix, String suffix, Map<String, Long> values) {
            values.forEach((k, v) -> p.put(prefix + k + suffix, v.toString()));
        }
    }

    /**
     * This cost adapter changes the cost by using a linear model.
     * <p>
     * The new costs are given by <code>summand + (oldCost * factor)</code>
     */
    public abstract static class LinearCostAdapater implements CostAdapter {
        protected Function<RuleApp, RuleAppCost> summand = (app) -> NumberRuleAppCost.getZeroCost();
        protected Function<RuleApp, RuleAppCost> factor = (app) -> NumberRuleAppCost.create(1);

        public LinearCostAdapater() {
        }

        public LinearCostAdapater(Function<RuleApp, RuleAppCost> summand, Function<RuleApp, RuleAppCost> factor) {
            this.summand = summand;
            this.factor = factor;
        }

        @NotNull
        @Override
        public RuleAppCost computeCost(@NotNull RuleAppCost oldCost, @NotNull RuleApp app,
                                       @NotNull PosInOccurrence pos, @NotNull Goal goal) {
            return oldCost.mul(factor.apply(app)).add(summand.apply(app));
        }
    }

    /**
     * This cost adapter does not change the given costs.
     */
    public static class NeutralCostAdapter implements CostAdapter {
        @NotNull
        @Override
        public RuleAppCost computeCost(@NotNull RuleAppCost oldCost, @NotNull RuleApp app, @NotNull PosInOccurrence pos,
                                       @NotNull Goal goal) {
            return oldCost;
        }
    }
}
