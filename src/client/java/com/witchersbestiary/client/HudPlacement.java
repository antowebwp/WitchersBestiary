package com.witchersbestiary.client;

/** Fits a panel beside an existing HUD when they share the same screen column. */
public final class HudPlacement {
    private HudPlacement() { }

    public static void avoidOverlap(int[] panel, int[] occupied, int width, int height,
                                    int logicalWidth, int logicalHeight) {
        if (panel[0] + panel[2] <= occupied[0] || occupied[0] + occupied[2] <= panel[0]
            || panel[1] + panel[3] <= occupied[1] || occupied[1] + occupied[3] <= panel[1]) return;
        int above = Math.max(0, occupied[1] - 4);
        int belowStart = occupied[1] + occupied[3] + 4;
        int below = Math.max(0, height - 48 - belowStart);
        boolean placeBelow = below >= panel[3] || above < panel[3] && below >= above;
        int available = placeBelow ? below : above;
        // In a viewport too small for both, leave the occupied HUD readable.
        if (available == 0) { panel[4] = 0; return; }
        int scale = Math.min(panel[4], available * 10000 / logicalHeight);
        int right = panel[0] + panel[2];
        panel[2] = (int)Math.ceil(logicalWidth * scale / 10000.0);
        panel[3] = (int)Math.ceil(logicalHeight * scale / 10000.0);
        if (right > width / 2) panel[0] = Math.max(0, right - panel[2]);
        panel[1] = placeBelow ? belowStart : above - panel[3];
        panel[4] = scale;
    }
}
