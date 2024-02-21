package me.dartn.alphacord.gfx;

import arc.graphics.Pixmap;

public class AlphaRenderer {
    private static final int ALPHA_OFFSET = 4;

    private final Pixmap alpha;

    public AlphaRenderer(Pixmap alpha) {
        this.alpha = alpha;
    }

    public void draw(Pixmap target, int x, int y, int textWidth) {
        //alphas
        int alx = x - (textWidth / 2) - this.alpha.width - ALPHA_OFFSET;
        int arx = x + (textWidth / 2) + (ALPHA_OFFSET / 2);

        target.draw(this.alpha, 0, 0, this.alpha.width, this.alpha.height, alx, y, this.alpha.width, this.alpha.height, false, true);
        target.draw(this.alpha, 0, 0, this.alpha.width, this.alpha.height, arx, y, this.alpha.width, this.alpha.height, false, true);
    }
}
