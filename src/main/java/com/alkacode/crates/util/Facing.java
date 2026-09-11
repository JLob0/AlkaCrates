package com.alkacode.crates.util;

import org.bukkit.block.BlockFace;

/**
 * Conversao entre yaw e direcao cardeal, e o parse dos nomes PT-BR aceitos no
 * `/alkacrates place` - antes so dava pra colocar virado pra onde o admin olhava,
 * sem escolher a direcao (norte/sul/leste/oeste) explicitamente.
 */
public final class Facing {

    private Facing() {
    }

    /** null se `arg` nao for um nome de direcao valido (chamador cai no yaw do player). */
    public static Float parseYaw(String arg) {
        if (arg == null) {
            return null;
        }
        return switch (arg.toLowerCase()) {
            case "sul", "s", "south" -> 0f;
            case "oeste", "o", "w", "west" -> 90f;
            case "norte", "n", "north" -> 180f;
            case "leste", "l", "e", "east" -> 270f;
            default -> null;
        };
    }

    /** Convencao do Bukkit: yaw 0=Sul, 90=Oeste, 180=Norte, 270=Leste. */
    public static BlockFace yawToBlockFace(float yaw) {
        float normalized = yaw % 360f;
        if (normalized < 0) {
            normalized += 360f;
        }
        if (normalized >= 315 || normalized < 45) {
            return BlockFace.SOUTH;
        }
        if (normalized < 135) {
            return BlockFace.WEST;
        }
        if (normalized < 225) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }
}
