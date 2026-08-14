package com.alkacode.crates.animation;

import com.alkacode.crates.AlkaCrates;
import com.alkacode.crates.crate.model.Crate;
import com.alkacode.crates.crate.model.CrateSession;
import com.alkacode.crates.display.CrateDisplay;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Orquestrador de timelines (idle e abertura). Roda na main thread. */
public final class AnimationEngine {

    private static final int TICK_MS = 50;

    private final AlkaCrates plugin;
    private final Map<UUID, BukkitTask> idleTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> openingTasks = new HashMap<>();

    public AnimationEngine(AlkaCrates plugin) {
        this.plugin = plugin;
    }

    public void startIdle(CrateDisplay display) {
        Crate crate = display.getCrate();
        IdleAnimation idle = crate.getIdleAnimation();
        if (idle == null || idle.getKeyframes().isEmpty()) {
            return;
        }
        UUID id = display.getItemDisplay().getUniqueId();
        stopIdle(display);

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }
                double totalMs = Math.max(1L, (long) (idle.getTotalDuration() * 1000));
                double now = (System.currentTimeMillis() % totalMs) / 1000.0;
                Transform transform = sample(idle.getKeyframes(), now, idle.getTotalDuration(), idle.isLoop());
                display.applyTransform(transform.getOffset(), transform.getRotation(), transform.getScale());
                spawnParticles(display.getLocation(), idle.getParticles());
            }
        };
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 1L);
        idleTasks.put(id, task);
    }

    public void stopIdle(CrateDisplay display) {
        if (display == null || display.getItemDisplay() == null) {
            return;
        }
        BukkitTask task = idleTasks.remove(display.getItemDisplay().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /** Roda a sequencia de abertura phase-a-phase. */
    public void playOpening(CrateSession session, Runnable onComplete) {
        CrateDisplay display = session.getPlacedCrate().getDisplay();
        UUID id = display.getItemDisplay().getUniqueId();
        BukkitTask existing = openingTasks.get(id);
        if (existing != null) {
            existing.cancel();
        }

        BukkitRunnable runnable = new BukkitRunnable() {
            double elapsed = 0;
            Phase current = session.nextPhase();
            double phaseTime = current != null ? current.getDuration() : 0;
            Transform from = Transform.identity();
            Transform to = current != null ? current.getTransform() : Transform.identity();

            @Override
            public void run() {
                if (current == null || !display.isValid()) {
                    finish();
                    return;
                }
                elapsed += TICK_MS / 1000.0;
                double t = Math.min(1.0, elapsed / phaseTime);
                double eased = current.getEasing().apply(t);
                display.applyTransform(
                        lerp(from.getOffset(), to.getOffset(), eased),
                        lerp(from.getRotation(), to.getRotation(), eased),
                        lerp(from.getScale(), to.getScale(), eased));
                spawnParticles(display.getLocation(), current.getParticles());
                if (t >= 1.0) {
                    if (current.getTrigger() == PhaseTrigger.REWARD_REVEAL) {
                        finish();
                        return;
                    }
                    elapsed = 0;
                    from = to;
                    current = session.nextPhase();
                    if (current != null) {
                        to = current.getTransform();
                        phaseTime = current.getDuration();
                    }
                }
            }

            private void finish() {
                openingTasks.remove(id);
                onComplete.run();
                cancel();
            }
        };
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 1L);
        openingTasks.put(id, task);
    }

    public void cancelSession(CrateSession session) {
        if (session == null || session.getPlacedCrate() == null || session.getPlacedCrate().getDisplay() == null
                || session.getPlacedCrate().getDisplay().getItemDisplay() == null) {
            return;
        }
        BukkitTask task = openingTasks.remove(session.getPlacedCrate().getDisplay().getItemDisplay().getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAllSessions() {
        for (BukkitTask task : openingTasks.values()) {
            task.cancel();
        }
        openingTasks.clear();
        for (BukkitTask task : idleTasks.values()) {
            task.cancel();
        }
        idleTasks.clear();
    }

    private Transform sample(List<Keyframe> keyframes, double time, double totalDuration, boolean loop) {
        if (keyframes.isEmpty()) {
            return Transform.identity();
        }
        if (keyframes.size() == 1) {
            return keyframes.get(0).getTransform();
        }
        double t = loop ? time % totalDuration : Math.min(time, totalDuration);
        Keyframe prev = keyframes.get(0);
        Keyframe next = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe a = keyframes.get(i);
            Keyframe b = keyframes.get(i + 1);
            if (t >= a.getTime() && t <= b.getTime()) {
                prev = a;
                next = b;
                break;
            }
        }
        double span = next.getTime() - prev.getTime();
        double p = span <= 0 ? 0 : (t - prev.getTime()) / span;
        double eased = next.getEasing().apply(p);
        return new Transform(
                lerp(prev.getTransform().getOffset(), next.getTransform().getOffset(), eased),
                lerp(prev.getTransform().getRotation(), next.getTransform().getRotation(), eased),
                lerp(prev.getTransform().getScale(), next.getTransform().getScale(), eased));
    }

    private double[] lerp(double[] a, double[] b, double t) {
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            out[i] = a[i] + (b[i] - a[i]) * t;
        }
        return out;
    }

    private void spawnParticles(Location location, List<ParticleEffect> effects) {
        if (effects == null || effects.isEmpty() || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        for (ParticleEffect effect : effects) {
            double[] off = effect.getOffset();
            world.spawnParticle(effect.getParticle(), location.clone().add(0, 0.5, 0),
                    effect.getCount(), off[0], off[1], off[2], effect.getSpeed());
        }
    }
}
