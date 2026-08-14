package com.alkacode.crates.crate.model;

import com.alkacode.crates.animation.Phase;
import com.alkacode.crates.crate.placement.PlacedCrate;
import org.bukkit.entity.Player;

/** Estado de abertura ativo por jogador. */
public final class CrateSession {

    private final Player player;
    private final Crate crate;
    private final PlacedCrate placedCrate;
    private final long startTime;
    private int currentPhase;

    public CrateSession(Player player, Crate crate, PlacedCrate placedCrate) {
        this.player = player;
        this.crate = crate;
        this.placedCrate = placedCrate;
        this.startTime = System.currentTimeMillis();
        this.currentPhase = 0;
    }

    public Player getPlayer() { return player; }
    public Crate getCrate() { return crate; }
    public PlacedCrate getPlacedCrate() { return placedCrate; }
    public long getStartTime() { return startTime; }
    public int getCurrentPhase() { return currentPhase; }

    public Phase nextPhase() {
        if (crate.getOpeningSequence() == null || crate.getOpeningSequence().getPhases().isEmpty()) {
            return null;
        }
        if (currentPhase >= crate.getOpeningSequence().getPhases().size()) {
            return null;
        }
        return crate.getOpeningSequence().getPhases().get(currentPhase++);
    }
}
