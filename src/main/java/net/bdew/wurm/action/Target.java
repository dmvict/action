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
    ACTIVATED,
    SELECTED,
    AREA,
    TOOLBELT,
    TB(1),
    EQ(1),
    NEARBY(1);

    private int id;

    private Target() {
        this.id = Integer.MIN_VALUE; 
    }

    private Target(final int id) {
        this.id = id; 
    }

    private void setId(final int id) {
        this.id = id; 
    }

    public int getId() {
        return this.id;
    }

    public static Optional<Target> parseTargetSafe(String input) {
        if (input == null) return Optional.empty();
        String src = input.trim();;
        try {
            if (src.startsWith("@tb")) {
                int slotT = Integer.parseInt(input.substring(3));
                Target tb = TB;
                tb.setId(slotT);
                return Optional.of(tb);
            } else if (src.startsWith("@eq")) {
                int slotE = Integer.parseInt(input.substring(3));
                Target eq = EQ;
                eq.setId(slotE);
                return Optional.of(eq);
            } else if (src.startsWith("@nearby")) {
                int range = Integer.parseInt(input.substring(7));
                Target nearby = NEARBY;
                nearby.setId(range);
                return Optional.of(nearby);
            } else {
                return Optional.of(Target.valueOf(src.toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}