package com.alkacode.crates.crate.placement;

import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.display.CrateDisplay;
import org.bukkit.Location;

/** Instancia de uma crate colocada no mundo. */
public final class PlacedCrate {

    private final Crate crate;
    private final Location location;
    private CrateDisplay display;

    public PlacedCrate(Crate crate, Location location, CrateDisplay display) {
        this.crate = crate;
        this.location = location;
        this.display = display;
    }

    public void remove() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    public Crate getCrate() { return crate; }
    public Location getLocation() { return location; }
    public CrateDisplay getDisplay() { return display; }
}
