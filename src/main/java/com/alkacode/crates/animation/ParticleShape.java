package com.alkacode.crates.animation;

import java.util.Locale;

/**
 * Forma geometrica de uma emissao de particula, calculada AO VIVO por formula
 * trigonometrica (ver {@link ParticleShapes}) em vez de exigir keyframe manual por
 * ponto - portado do padrao de shapes do DadaCratesPro (auditado 2026-09-03),
 * adaptado pro nosso ParticleEffect/AnimationEngine (config-driven, R8). NONE
 * preserva o comportamento antigo (burst simples num offset fixo).
 */
public enum ParticleShape {
    NONE,
    RING,
    DOUBLE_RING,
    SPIRAL,
    HELIX,
    HEART,
    STAR,
    ORBIT,
    AURA,
    CROWN,
    FOUNTAIN,
    SIDE_WAVES,
    COLOR_SWIRL,
    BURST,
    MAGIC_PLATFORM,
    CORNER_SPARKS,
    RUNE_SQUARE,
    SPLASH_AURA,
    RISING_STARS,
    LOW_MIST,
    DIAGONAL_CROSS,
    BUTTERFLY,
    CUBE_FRAME,
    FLAME_CIRCLE;

    public static ParticleShape parse(String value) {
        if (value == null) {
            return NONE;
        }
        try {
            return ParticleShape.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
