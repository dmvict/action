package net.bdew.wurm.action;

import java.util.Optional;

public enum Target {
    HOVER,
    BODY,
    TILE,
    TILE_N,
    TILE_W,
    TILE_NW,
    TILE_NE,
    TILE_S,
    TILE_E,
    TILE_SE,
    TILE_SW,
    TOOL,
    SELECTED,
    AREA,
    TOOLBELT,
    TB,
    EQ,
    NEARBY;

    public static Optional<Target> parseTargetSafe(String input) {
        if (input == null) return Optional.empty();
        String src = input.trim();;
        try {
            if (src.startsWith("@tb")) {
                return Optional.of(TB);
            } else if (src.startsWith("@eq")) {
                return Optional.of(EQ);
            } else if (src.startsWith("@nearby")) {
                return Optional.of(NEARBY);
            } else {
                return Optional.of(Target.valueOf(src.toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}