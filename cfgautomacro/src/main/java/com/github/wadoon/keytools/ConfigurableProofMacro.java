package com.github.wadoon.keytools;

import de.uka.ilkd.key.control.UserInterfaceControl;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.macros.ProofMacro;
import de.uka.ilkd.key.macros.ProofMacroFinishedInfo;
import de.uka.ilkd.key.macros.StrategyProofMacro;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.RuleAppIndex;
import de.uka.ilkd.key.proof.rulefilter.TacletFilter;
import de.uka.ilkd.key.prover.ProverTaskListener;
import de.uka.ilkd.key.rule.RuleSet;
import de.uka.ilkd.key.rule.Taclet;
import org.key_project.util.collection.ImmutableList;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Alexander Weigl
 * @version 1 (30.05.18)
 */
public abstract class ConfigurableProofMacro<M extends ProofMacro> extends StrategyProofMacro {
    /**
     *
     */
    protected final M internal;

    protected AdaptableStrategy strategy;

    protected AdaptableStrategy.RuleNameAndSetCostAdapter costAdapter
            = AdaptableStrategy.RuleNameAndSetCostAdapter.createDefault();

    public ConfigurableProofMacro(M internal) {
        this.internal = internal;
    }

    public AdaptableStrategy.RuleNameAndSetCostAdapter getCostAdapter() {
        return costAdapter;
    }

    private static List<Taclet> findTaclets(Proof p) {
        Goal g = p.openGoals().head();
        Services services = p.getServices();
        TacletFilter filter = new TacletFilter() {
            @Override
            protected boolean filter(Taclet taclet) {
                return true;
            }
        };
        List<Taclet> set = new ArrayList<>();
        RuleAppIndex index = g.ruleAppIndex();
        index.tacletIndex().allNoPosTacletApps().forEach(t ->
                set.add(t.taclet())
        );

/*        index.autoModeStopped();
        for (SequentFormula sf : g.node().sequent().antecedent()) {
            ImmutableList<TacletApp> apps = index.getTacletAppAtAndBelow(filter,
                    new PosInOccurrence(sf, PosInTerm.getTopLevel(), true),
                    services);
            apps.forEach(t -> set.add(t.taclet()));
        }

        try {
            for (SequentFormula sf : g.node().sequent().succedent()) {
                ImmutableList<TacletApp> apps = index.getTacletAppAtAndBelow(filter,
                        new PosInOccurrence(sf, PosInTerm.getTopLevel(), true),
                        services);
                apps.forEach(t -> set.add(t.taclet()));
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
  */
        return set;
    }

    @Override
    protected AdaptableStrategy createStrategy(Proof proof, PosInOccurrence posInOcc) {
        if (this.strategy == null) {
            this.strategy = new AdaptableStrategy(proof.getActiveStrategy());
            this.costAdapter = AdaptableStrategy.RuleNameAndSetCostAdapter.createDefault();
            this.strategy.costAdapter = this.costAdapter;
        }
        return this.strategy;
    }

    @Override
    public boolean canApplyTo(Proof proof, ImmutableList<Goal> goals, PosInOccurrence posInOcc) {
        return internal.canApplyTo(proof, goals, posInOcc);
    }

    @Override
    public boolean canApplyTo(Node node, PosInOccurrence posInOcc) {
        return internal.canApplyTo(node, posInOcc);
    }

    @Override
    public ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Proof proof,
                                          ImmutableList<Goal> goals, PosInOccurrence posInOcc,
                                          ProverTaskListener listener) throws InterruptedException {
        createStrategy(proof, null);
        updateStrategyDialog(proof);
        proof.setActiveStrategy(strategy);
        ProofMacroFinishedInfo info = null;
        try {
            info = internal.applyTo(uic, proof, goals, posInOcc, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        proof.setActiveStrategy(strategy.delegate);
        return info;
    }

    @Override
    public ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Node node, PosInOccurrence posInOcc,
                                          ProverTaskListener listener) throws Exception {
        updateStrategyDialog(node.proof());
        node.proof().setActiveStrategy(strategy);
        ProofMacroFinishedInfo info = internal.applyTo(uic, node, posInOcc, listener);
        node.proof().setActiveStrategy(strategy.delegate);
        return info;
    }

    private void updateStrategyDialog(Proof proof) {
        createStrategy(proof, null);

        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();

        List<Taclet> taclets = findTaclets(proof);
        List<String> tacletNames = findTaclets(proof).stream()
                .map(Taclet::name)
                .map(Object::toString)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        List<String> ruleSetNames = taclets.stream()
                .flatMap(t -> t.getRuleSets().stream())
                .map(RuleSet::name)
                .map(Object::toString)
                .sorted()
                .distinct()
                .collect(Collectors.toList());


        JTable tacletFactor = new JTable(new TacletCostWithDisableModel(tacletNames,
                costAdapter.getRuleName(),
                strategy.disabledRulesByName));
        tabbedPane.addTab("Taclet Factor", new JScrollPane(tacletFactor));


        JTable ruleSetFactor = new JTable(new TacletCostWithDisableModel(ruleSetNames,
                costAdapter.getRuleSet(), strategy.disabledRulesBySet));
        tabbedPane.addTab("RuleSet Factor", new JScrollPane(ruleSetFactor));

        panel.add(tabbedPane);
        JDialog dialog = new JDialog((Dialog) null, "Change Strategy Settings (locally)", true);
        dialog.setContentPane(panel);
        dialog.setSize(300, 600);

        JPanel pSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnOk = new JButton("Run");
        btnOk.addActionListener(evt -> {
            dialog.setVisible(false);
        });
        pSouth.add(btnOk);
        panel.add(pSouth, BorderLayout.SOUTH);


        JPanel pNorth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save");
        JButton btnLoad = new JButton("Load");
        pNorth.add(btnSave);
        pNorth.add(btnLoad);
        panel.add(pNorth, BorderLayout.NORTH);

        btnLoad.addActionListener(e -> {
            JFileChooser jf = new JFileChooser("Choose a property file...");
            int c = jf.showOpenDialog(dialog);
            if (c == JFileChooser.APPROVE_OPTION) {
                Properties p = new Properties();
                try (Reader reader = new FileReader(jf.getSelectedFile())) {
                    p.load(reader);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                costAdapter.loadFrom(p);
                strategy.loadFrom(p);
                tacletFactor.repaint();
            }
        });

        btnSave.addActionListener(e -> {
            JFileChooser jf = new JFileChooser("Choose a property file to store...");
            int c = jf.showSaveDialog(dialog);
            if (c == JFileChooser.APPROVE_OPTION) {
                Properties p = new Properties();
                costAdapter.storeInto(p);
                strategy.storeInto(p);
                try (Writer writer = new FileWriter(jf.getSelectedFile())) {
                    p.store(writer, "");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                tacletFactor.repaint();
            }
        });

        dialog.setVisible(true);
    }

    private class TacletCostWithDisableModel extends TacletCostModel {
        private final Set<String> disabled;

        public TacletCostWithDisableModel(List<String> keys, AdaptableStrategy.SetBasedCostAdapter<String> ruleSet, Set<String> disabled) {
            super(keys, ruleSet);
            this.disabled = disabled;
        }

        @Override
        public int getColumnCount() {
            return super.getColumnCount() + 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 3) return "Disabled";
            return super.getColumnName(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) return Boolean.class;
            return super.getColumnClass(columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 3 || super.isCellEditable(rowIndex, columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 3) {
                String name = keys.get(rowIndex);
                return disabled.contains(name);
            }
            return super.getValueAt(rowIndex, columnIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 3) {
                String name = keys.get(rowIndex);
                if (Boolean.valueOf(aValue.toString())) {
                    disabled.add(name);
                } else {
                    disabled.remove(name);
                }
                return;
            }
            super.setValueAt(aValue, rowIndex, columnIndex);
        }
    }

    private class TacletCostModel extends AbstractTableModel {
        protected final List<String> keys;
        private final AdaptableStrategy.SetBasedCostAdapter<String> costAdapter;

        public TacletCostModel(List<String> keys, AdaptableStrategy.SetBasedCostAdapter<String> ruleSet) {
            this.keys = keys;
            costAdapter = ruleSet;
        }

        @Override
        public int getRowCount() {
            return keys.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Taclet";
                case 1:
                    return "Cost Factor";
                case 2:
                    return "Cost Summand";
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                case 2:
                    return Integer.class;
                default:
                    return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1 || columnIndex == 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                String name = keys.get(rowIndex);
                switch (columnIndex) {
                    case 0:
                        return name;
                    case 1:
                        return costAdapter.factorMap.getOrDefault(name, 1L);
                    case 2:
                        return costAdapter.summandMap.getOrDefault(name, 0L);
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String name = keys.get(rowIndex);
            long l = Long.parseLong(aValue.toString());
            if (columnIndex == 1) {
                costAdapter.factorMap.put(name, l);
            } else {
                costAdapter.summandMap.put(name, l);
            }
        }
    }
}

