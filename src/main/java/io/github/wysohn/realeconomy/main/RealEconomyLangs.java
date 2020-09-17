package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public enum RealEconomyLangs implements ILang {

    ;

    private final String[] def;

    RealEconomyLangs(String... def) {
        this.def = def;
    }

    @Override
    public String[] getEngDefault() {
        return def;
    }
}
