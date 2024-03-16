package me.dartn.alphacord.gfx;

import arc.func.IntIntf;
import arc.graphics.Pixmap;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Disposable;
import arc.util.Log;
import arc.util.Strings;
import mindustry.maps.Map;

public class FontRenderer implements Disposable, AutoCloseable {
    private static final int NUM_CHARS = 256;
    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 8;
    private static final int START_CHAR = '!';

    //font atlas
    private final Pixmap atlas;
    private final int[] widths;
    //in characters
    private final int atlasCols;
    private final int atlasRows;
    private boolean disposed;
    private int scale;
    private int colour;

    public FontRenderer(Pixmap fontAtlas) {
        this.disposed = false;
        this.scale = 1;
        this.atlas = fontAtlas;
        this.atlasRows = this.atlas.height / CHAR_HEIGHT;
        this.atlasCols = this.atlas.width / CHAR_WIDTH;
        this.widths = new int[NUM_CHARS];
        this.colour = 0xFFFFFFFF;

        loadCharWidths();
        //make black transparent
        this.atlas.replace(new IntIntf() {
            @Override
            public int get(int pix) {
                int r = (pix >> 24) & 0xFF;
                int g = (pix >> 16) & 0xFF;
                int b = (pix >> 8) & 0xFF;
                int a = pix & 0xFF;

                if (r == 0 && g == 0 && b == 0 && a == 255) {
                    a = 0;
                }

                return (r << 24) | (g << 16) | (b << 8) | a;
            }
        });
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    public int getScale() {
        return this.scale;
    }

    public void draw(Pixmap target, int x, int y, String text) {
        int curX = x;
        int curY = y;

        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);

            //newline
            if (c == '\n') {
                curX = x;
                curY += CHAR_HEIGHT * this.scale;
                continue;
            } else if (c == ' ') {
                //space
                curX += (CHAR_WIDTH / 2) * this.scale;
                continue;
            }

            int cw = this.widths[c];

            int oc = c - START_CHAR;
            int px = (oc % this.atlasCols) * CHAR_WIDTH;
            int py = (oc / this.atlasRows) * CHAR_HEIGHT;

            //manual pixel copy to allow for colouring
            for (int srcY = py; srcY < py + CHAR_HEIGHT; srcY++) {
                for (int srcX = px; srcX < px + CHAR_WIDTH; srcX++) {
                    if (this.atlas.get(srcX, srcY) != 0xFFFFFFFF) continue;

                    //optimize for 1px scale, no need to bother drawing a rect there
                    if (this.scale == 1) {
                        int dstX = curX + (srcX - px);
                        int dstY = curY + (srcY - py);
                        target.set(dstX, dstY, this.colour);
                    } else {
                        int pxs = curX + ((srcX - px) * this.scale);
                        int pys = curY + ((srcY - py) * this.scale);
                        target.fillRect(pxs, pys, this.scale, this.scale, this.colour);
                    }
                }
            }

            //target.draw(this.atlas, px, py, CHAR_WIDTH, CHAR_HEIGHT, curX, curY, CHAR_WIDTH * this.scale, CHAR_HEIGHT * this.scale, false, true);

            curX += (cw * this.scale) + this.scale;
        }
    }

    //centres every line properly
    public void drawLinesCentredXWithShadow(Pixmap target, int x, int y, String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Point2 lineSize = measureTextSize(line);

            drawCentredXWithShadow(target, x, y, line);
            y += lineSize.y + this.scale;
        }
    }


    public void drawWithShadow(Pixmap target, int x, int y, String text) {
        int oc = this.colour;
        int dc = ((oc & 0xFCFCFC00) >>> 2) | 0x000000FF;

        setColour(dc);
        draw(target, x + this.scale, y + this.scale, text);
        setColour(oc);
        draw(target, x, y, text);
    }

    public void drawCentredX(Pixmap target, int x, int y, String text) {
        Point2 size = measureTextSize(text);
        draw(target, x - (size.x / 2), y, text);
    }

    public void drawCentredXWithShadow(Pixmap target, int x, int y, String text) {
        Point2 size = measureTextSize(text);
        drawWithShadow(target, x - (size.x / 2), y, text);
    }

    //lobotomized draw function
    public Point2 measureTextSize(String text) {
        if (text.trim().isEmpty()) return new Point2(0, 0);

        int curW = 0;
        int curH = CHAR_HEIGHT * this.scale;
        int w = 0;
        int h = curH;

        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);

            //newline
            if (c == '\n') {
                curW = 0;
                curH += CHAR_HEIGHT * this.scale;

                if (curH > h) h = curH;
                continue;
            } else if (c == ' ') {
                //space
                curW += (CHAR_WIDTH / 2) * this.scale;

                //comment out to make trailing spaces not have an effect on length
                //if (curW > w) w = curW;
                continue;
            }

            curW += (this.widths[c] * this.scale) + this.scale;

            if (curW > w) w = curW;
            //if (curH > h) h = curH;
        }

        return new Point2(w, h);
    }

    private void loadCharWidths() {
        for (int c = START_CHAR; c < NUM_CHARS; c++) {
            int oc = c - START_CHAR;
            int px = (oc % this.atlasCols) * CHAR_WIDTH;
            int py = (oc / this.atlasRows) * CHAR_HEIGHT;
            int w = 0;

            for (int y = py; y < py + CHAR_HEIGHT; y++) {
                int fxo = 0;
                //r2l
                for (int relX = CHAR_WIDTH - 1; relX >= 0; relX--) {
                    int ax = px + relX;
                    int pix = this.atlas.get(ax, y);

                    if (pix == 0xFFFFFFFF) {
                        fxo = relX + 1;
                        break;
                    }
                }

                if (fxo > w) w = fxo;
            }

            Log.info(Strings.format("w(@) = @", ((char)c), w));

            //save
            this.widths[c] = w;
        }
    }

    @Override
    public void dispose() {
        this.atlas.dispose();
        this.disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public void close() throws Exception {
        dispose();
    }
}
