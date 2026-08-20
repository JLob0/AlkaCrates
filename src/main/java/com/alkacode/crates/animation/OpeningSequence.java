package com.alkacode.crates.animation;

import org.bukkit.Sound;
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

    /**
     * Sequencia embutida em Java, sem depender de YAML - usada quando config.yml
     * nao tem (ou tem quebrado) `default-animation.opening`, pra nunca deixar a
     * abertura ficar sem nenhuma animacao (item so aparecendo sem mais nada).
     */
    public static OpeningSequence fallback() {
        List<Phase> phases = new ArrayList<>();
        phases.add(new Phase("start", 1.0,
                new Transform(new double[]{0, 0.5, 0}, new double[]{0, 0, 0}, new double[]{1.2, 1.2, 1.2}),
                Easing.EASE_OUT, List.of(), Sound.BLOCK_BEACON_ACTIVATE, PhaseTrigger.NONE));
        phases.add(new Phase("spin", 3.0,
                new Transform(new double[]{0, 0.5, 0}, new double[]{0, 1080, 0}, new double[]{1.5, 1.5, 1.5}),
                Easing.EASE_OUT, List.of(), null, PhaseTrigger.NONE));
        phases.add(new Phase("reveal", 1.5,
                new Transform(new double[]{0, 1.5, 0}, new double[]{0, 1080, 0}, new double[]{2.0, 2.0, 2.0}),
                Easing.BOUNCE, List.of(), Sound.ENTITY_PLAYER_LEVELUP, PhaseTrigger.REWARD_REVEAL));
        return new OpeningSequence(phases);
    }

    public List<Phase> getPhases() { return phases; }
    public double getTotalDuration() { return totalDuration; }
}
