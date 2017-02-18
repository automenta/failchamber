package nars.failchamber;

import processing.core.PApplet;

/**
 * overlay HUD (heads up display)
 */
public class HUD {

    public static void drawCursorCrossHair(PApplet a, int mx, int my, String label) {
        a.stroke(255, 150, 0, 50);
        a.strokeWeight(10f);
        a.line(mx, 0, mx, a.height);
        a.line(0, my, a.width, my);
        a.noStroke();

        if (label != null && !label.isEmpty()) {
            a.textSize(24f);
            a.fill(150, 0, 255, 150);
            a.text(label, mx, my);
        }
    }
}
