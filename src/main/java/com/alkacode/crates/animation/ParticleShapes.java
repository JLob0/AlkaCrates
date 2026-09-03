package com.alkacode.crates.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulas puras (sem dependencia de Bukkit World/Location) que geram os pontos de
 * cada {@link ParticleShape} - portado do DadaCratesPro (auditado 2026-09-03,
 * IdleParticleManager#draw e metodos privados de forma), traduzido pros nossos
 * nomes (points/radius/tick em vez de crate.idleParticle*). Cada ponto e um offset
 * RELATIVO ao centro de emissao (em blocos); "tick" e o contador incremental da
 * timeline (idle loop ou fase de abertura) que faz as formas girarem/pulsarem.
 *
 * <p>COLOR_SWIRL e a unica forma que carrega cor propria (rainbow cycling via
 * {@link Point#rgb()}) - ela sempre usa Particle.DUST independente do "type:"
 * configurado no ParticleEffect (ver AnimationEngine), porque a cor animada so
 * faz sentido nesse tipo de particula. FLAME_CIRCLE, diferente do original deles
 * (que forcava Particle.FLAME na marra), usa o particle TIPO configurado pelo
 * admin - mantem o padrao 100% config-driven do resto do plugin.</p>
 */
final class ParticleShapes {

    private ParticleShapes() {
    }

    record Point(double x, double y, double z, Integer rgb) {
        Point(double x, double y, double z) {
            this(x, y, z, null);
        }
    }

    static List<Point> compute(ParticleShape shape, int points, double radius, long tick) {
        List<Point> out = new ArrayList<>();
        int safePoints = Math.max(1, points);
        switch (shape) {
            case RING -> ring(out, safePoints, radius, tick, 0, 0);
            case DOUBLE_RING -> {
                ring(out, safePoints, radius, tick, 0, -0.2);
                ring(out, safePoints, radius, tick, Math.PI / safePoints, 0.35);
            }
            case SPIRAL -> spiral(out, safePoints, radius, tick, false);
            case HELIX -> spiral(out, safePoints, radius, tick, true);
            case HEART -> heart(out, safePoints, radius);
            case STAR -> star(out, safePoints, radius, tick);
            case ORBIT -> orbit(out, radius, tick);
            case AURA -> aura(out, safePoints, radius, tick);
            case CROWN -> crown(out, safePoints, radius, tick);
            case FOUNTAIN -> fountain(out, safePoints, radius, tick);
            case SIDE_WAVES -> sideWaves(out, safePoints, radius, tick);
            case COLOR_SWIRL -> colorSwirl(out, safePoints, radius, tick);
            case BURST -> burst(out, safePoints, tick);
            case MAGIC_PLATFORM -> magicPlatform(out, safePoints, radius, tick);
            case CORNER_SPARKS -> cornerSparks(out, tick);
            case RUNE_SQUARE -> {
                magicPlatform(out, safePoints, radius, tick);
                runeSquareRunes(out, tick);
            }
            case SPLASH_AURA -> splashAura(out, safePoints, tick);
            case RISING_STARS -> risingStars(out, safePoints, radius, tick);
            case LOW_MIST -> lowMist(out, safePoints, radius, tick);
            case DIAGONAL_CROSS -> diagonalCross(out, radius);
            case BUTTERFLY -> butterfly(out, safePoints, radius);
            case CUBE_FRAME -> cubeFrame(out, radius);
            case FLAME_CIRCLE -> flameCircle(out, safePoints, radius, tick);
            case NONE -> {
            }
        }
        return out;
    }

    private static void ring(List<Point> out, int points, double radius, long tick, double angleOffset, double y) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.08 + angleOffset;
            out.add(new Point(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        }
    }

    private static void spiral(List<Point> out, int points, double radius, long tick, boolean doubleHelix) {
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = progress * Math.PI * 2 + tick * 0.12;
            double y = progress * 1.4 - 0.35;
            out.add(new Point(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
            if (doubleHelix) {
                out.add(new Point(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius));
            }
        }
    }

    private static void heart(List<Point> out, int points, double radius) {
        for (int i = 0; i < points; i++) {
            double t = Math.PI * 2 * i / points;
            double x = 16.0 * Math.pow(Math.sin(t), 3.0) / 18.0;
            double y = (13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t)) / 18.0;
            out.add(new Point(x * radius, y + 0.7, 0.0));
        }
    }

    private static void star(List<Point> out, int points, double radius, long tick) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.06;
            double r = i % 2 == 0 ? radius : radius * 0.45;
            out.add(new Point(Math.cos(angle) * r, 0.25, Math.sin(angle) * r));
        }
    }

    private static void orbit(List<Point> out, double radius, long tick) {
        for (int i = 0; i < 3; i++) {
            double angle = tick * 0.18 + i * Math.PI * 2 / 3;
            out.add(new Point(Math.cos(angle) * radius, 0.3 + Math.sin(angle * 2) * 0.25, Math.sin(angle) * radius));
        }
    }

    private static void aura(List<Point> out, int points, double radius, long tick) {
        int n = Math.max(16, points);
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n + tick * 0.11;
            double wave = Math.sin(tick * 0.25 + i * 0.8) * 0.18;
            double r = radius + wave;
            out.add(new Point(Math.cos(angle) * r, 0.05 + Math.sin(angle * 2 + tick * 0.1) * 0.22, Math.sin(angle) * r));
        }
        int inner = Math.max(1, n / 3);
        for (int i = 0; i < inner; i++) {
            double angle = Math.PI * 2 * i / inner - tick * 0.16;
            out.add(new Point(Math.cos(angle) * radius * 0.55, 0.75, Math.sin(angle) * radius * 0.55));
        }
    }

    private static void crown(List<Point> out, int points, double radius, long tick) {
        ring(out, points, radius, tick, 0, 0.85);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6 + tick * 0.05;
            double bx = Math.cos(angle) * radius;
            double bz = Math.sin(angle) * radius;
            for (int y = 0; y < 4; y++) {
                out.add(new Point(bx, 0.85 + y * 0.12, bz));
            }
        }
    }

    private static void fountain(List<Point> out, int points, double radius, long tick) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.08;
            double progress = (i % 6) / 6.0;
            double r = radius * progress;
            double y = 0.15 + Math.sin(progress * Math.PI) * 0.9;
            out.add(new Point(Math.cos(angle) * r, y, Math.sin(angle) * r));
        }
    }

    private static void sideWaves(List<Point> out, int points, double radius, long tick) {
        int half = Math.max(1, points / 2);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < half; i++) {
                double progress = (double) i / half;
                double z = (progress - 0.5) * 1.8;
                double y = 0.15 + Math.sin(progress * Math.PI * 2 + tick * 0.2) * 0.25;
                out.add(new Point(side * radius, y, z));
            }
        }
    }

    private static void colorSwirl(List<Point> out, int points, double radius, long tick) {
        for (int i = 0; i < points; i++) {
            double progress = (double) i / points;
            double angle = progress * Math.PI * 2 + tick * 0.18;
            int red = clamp((int) (128 + 127 * Math.sin(angle)));
            int green = clamp((int) (128 + 127 * Math.sin(angle + 2.1)));
            int blue = clamp((int) (128 + 127 * Math.sin(angle + 4.2)));
            int rgb = (red << 16) | (green << 8) | blue;
            double y = progress * 1.2 - 0.2;
            out.add(new Point(Math.cos(angle) * radius, y, Math.sin(angle) * radius, rgb));
        }
    }

    private static void burst(List<Point> out, int points, long tick) {
        int n = Math.max(12, points);
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2 * i / n;
            double pulse = 0.35 + (Math.sin(tick * 0.25) + 1.0) * 0.35;
            out.add(new Point(Math.cos(angle) * pulse, 0.25 + Math.sin(i + tick * 0.2) * 0.2, Math.sin(angle) * pulse));
        }
    }

    private static void magicPlatform(List<Point> out, int points, double radius, long tick) {
        double y = -0.55;
        for (double x = -1.05; x <= 1.05; x += 0.22) {
            out.add(new Point(x, y, -1.05));
            out.add(new Point(x, y, 1.05));
        }
        for (double z = -1.05; z <= 1.05; z += 0.22) {
            out.add(new Point(-1.05, y, z));
            out.add(new Point(1.05, y, z));
        }
        ring(out, points, radius, tick, tick * 0.05, -0.55);
    }

    private static void cornerSparks(List<Point> out, long tick) {
        double[][] corners = {{-0.85, -0.85}, {0.85, -0.85}, {-0.85, 0.85}, {0.85, 0.85}};
        for (double[] corner : corners) {
            for (int i = 0; i < 5; i++) {
                double lift = 0.05 + ((tick + i) % 8) * 0.11;
                out.add(new Point(
                        corner[0] + Math.sin(tick * 0.2 + i) * 0.08,
                        lift,
                        corner[1] + Math.cos(tick * 0.2 + i) * 0.08));
            }
        }
    }

    private static void runeSquareRunes(List<Point> out, long tick) {
        for (int i = 0; i < 4; i++) {
            double angle = tick * 0.08 + i * Math.PI / 2;
            out.add(new Point(Math.cos(angle) * 0.55, -0.15 + Math.sin(tick * 0.12) * 0.08, Math.sin(angle) * 0.55));
        }
    }

    private static void splashAura(List<Point> out, int points, long tick) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.12;
            double r = 0.25 + (i % 5) * 0.16;
            double y = -0.35 + Math.sin(tick * 0.18 + i) * 0.35;
            out.add(new Point(Math.cos(angle) * r, y, Math.sin(angle) * r));
        }
        if (tick % 3 == 0) {
            out.add(new Point(0, 0.85, 0));
        }
    }

    private static void risingStars(List<Point> out, int points, double radius, long tick) {
        int half = Math.max(1, points / 2);
        for (int i = 0; i < half; i++) {
            double angle = Math.PI * 2 * i / half + tick * 0.09;
            double y = ((tick + i * 3) % 20) / 20.0 * 1.6 - 0.45;
            double r = radius * (0.45 + (i % 3) * 0.22);
            out.add(new Point(Math.cos(angle) * r, y, Math.sin(angle) * r));
        }
    }

    private static void lowMist(List<Point> out, int points, double radius, long tick) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.04;
            double r = radius * (0.35 + (i % 4) * 0.18);
            out.add(new Point(Math.cos(angle) * r, -0.55 + Math.sin(tick * 0.1 + i) * 0.08, Math.sin(angle) * r));
        }
    }

    private static void diagonalCross(List<Point> out, double radius) {
        for (double t = -1.0; t <= 1.0; t += 0.18) {
            out.add(new Point(t * radius, 0, t * radius));
            out.add(new Point(t * radius, 0, -t * radius));
        }
    }

    private static void butterfly(List<Point> out, int points, double radius) {
        for (int i = 0; i < points; i++) {
            double t = Math.PI * 2 * i / points;
            double wing = Math.sin(t) * Math.cos(t);
            double x = Math.sin(t) * radius;
            double z = wing * 1.4 * radius;
            double y = Math.abs(Math.cos(t)) * 0.55;
            out.add(new Point(x, y, z));
            out.add(new Point(-x, y, z));
        }
    }

    private static void cubeFrame(List<Point> out, double radius) {
        double y1 = -0.45;
        double y2 = 0.75;
        for (double d = -radius; d <= radius; d += 0.25) {
            out.add(new Point(d, y1, -radius));
            out.add(new Point(d, y1, radius));
            out.add(new Point(-radius, y1, d));
            out.add(new Point(radius, y1, d));
            out.add(new Point(d, y2, -radius));
            out.add(new Point(d, y2, radius));
            out.add(new Point(-radius, y2, d));
            out.add(new Point(radius, y2, d));
        }
    }

    private static void flameCircle(List<Point> out, int points, double radius, long tick) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + tick * 0.12;
            double y = Math.sin(tick * 0.2 + i) * 0.12;
            out.add(new Point(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
