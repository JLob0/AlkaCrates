package com.alkacode.crates.animation;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/** Sequencia de abertura multi-fase. */
public final class OpeningSequence {

    private final List<Phase> phases;
    private final double totalDuration;

    public OpeningSequence(List<Phase> phases) {
        this.phases = phases;
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDuration();
        }
        this.totalDuration = total;
    }

    public static OpeningSequence from(ConfigurationSection section) {
        List<Phase> phases = new ArrayList<>();
        ConfigurationSection phasesSection = section.getConfigurationSection("phases");
        if (phasesSection != null) {
            for (String key : phasesSection.getKeys(false)) {
                phases.add(Phase.from(phasesSection.getConfigurationSection(key)));
            }
        }
        return new OpeningSequence(phases);
    }

    public List<Phase> getPhases() { return phases; }
    public double getTotalDuration() { return totalDuration; }
}
