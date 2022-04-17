package com.github.wadoon.keytools;

import de.uka.ilkd.key.macros.FullAutoPilotProofMacro;

/**
 * @author Alexander Weigl
 * @version 1 (30.05.18)
 */
public class AutoConfigurableMacro extends ConfigurableProofMacro<FullAutoPilotProofMacro> {
    public AutoConfigurableMacro() {
        super(new FullAutoPilotProofMacro());
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getCategory() {
        return internal.getCategory();
    }

    @Override
    public String getDescription() {
        return internal.getDescription();
    }
}
