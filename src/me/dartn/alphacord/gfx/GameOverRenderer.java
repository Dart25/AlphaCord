package me.dartn.alphacord.gfx;

import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.math.geom.Point2;
import arc.util.Strings;
import arc.util.io.Streams;
import me.dartn.alphacord.Utils;

import java.io.IOException;
import java.util.zip.Deflater;

public class GameOverRenderer {
    private static final int TEXT_MARGIN = 1;
    private static final int GAME_OVER_TEXT_OFFSET = 24;
    private static final int ALPHA_OFFSET = 4;
    //private static final int ALPHA_WIDTH = 20;
    private static final String gameOverText = "Game over!";
    private static final String infoFormat = """
                                             @ waves passed
                                             @ enemies destroyed
                                             @ buildings built
                                             @ buildings destroyed
                                             and @ units built
                                             on @
                                             with @ @ online.
                                             """;

    private final FontRenderer fontRenderer;
    private final AlphaRenderer alphaRenderer;
    private final int imgWidth;
    private final int imgHeight;

    private String mapName;
    private int wavesPassed;
    private int enemiesKilled;
    private int buildingsBuilt;
    private int buildingsDestroyed;
    private int unitsBuilt;
    private int playersOnline;

    public GameOverRenderer(FontRenderer fontRenderer, AlphaRenderer alpha, int imgWidth, int imgHeight) {
        this.fontRenderer = fontRenderer;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.alphaRenderer = alpha;

        this.mapName = "";
        this.wavesPassed = -1;
        this.enemiesKilled = -1;
        this.buildingsBuilt = -1;
        this.buildingsDestroyed = -1;
        this.unitsBuilt = -1;
        this.playersOnline = -1;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public void setWavesPassed(int wavesPassed) {
        this.wavesPassed = wavesPassed;
    }

    public void setEnemiesKilled(int enemiesKilled) {
        this.enemiesKilled = enemiesKilled;
    }

    public void setBuildingsBuilt(int buildingsBuilt) {
        this.buildingsBuilt = buildingsBuilt;
    }

    public void setBuildingsDestroyed(int buildingsDestroyed) {
        this.buildingsDestroyed = buildingsDestroyed;
    }

    public void setUnitsBuilt(int unitsBuilt) {
        this.unitsBuilt = unitsBuilt;
    }

    public void setPlayersOnline(int playersOnline) {
        this.playersOnline = playersOnline;
    }

    public Pixmap draw() {
        Pixmap pix = new Pixmap(this.imgWidth, this.imgHeight);

        int wh = this.imgWidth / 2;
        int hh = this.imgHeight / 2;

        //bg

        //initial black
        pix.fill(0x000000FF);

        //rotated map bg
        Pixmap mapRender = MapRenderer.takeMapScreenshot();
        //scale up
        Pixmap mapRender2x = new Pixmap(mapRender.width * 2, mapRender.height * 2);
        mapRender2x.draw(mapRender, 0, 0, mapRender.width, mapRender.height, 0, 0, mapRender2x.width, mapRender2x.height, false, false);
        mapRender.dispose();

        //rotate
        Pixmap mapRenderRot = rotate(mapRender2x, 45.0D);
        mapRender2x.dispose();

        //add bg
        int mwh = mapRenderRot.width / 2;
        int mhh = mapRenderRot.height / 2;

        int msx = mwh - wh;
        int msy = mhh - hh;

        pix.draw(mapRenderRot, msx, msy, pix.width, pix.height, 0, 0, pix.width, pix.height);
        mapRenderRot.dispose();

        //text begin
        this.fontRenderer.setScale(3);
        Point2 gameOverTextSize = this.fontRenderer.measureTextSize(gameOverText);
        this.fontRenderer.drawCentredXWithShadow(pix, wh, GAME_OVER_TEXT_OFFSET, gameOverText);

        this.fontRenderer.setScale(2);
        String info = Strings.format(infoFormat,
                this.wavesPassed,
                this.enemiesKilled,
                this.buildingsBuilt,
                this.buildingsDestroyed,
                this.unitsBuilt,
                this.mapName,
                this.playersOnline == 0 ? "no" : this.playersOnline,
                this.playersOnline == 1 ? "person" : "people");
        String[] infoLines = info.split("\n");

        Point2 infoSize = this.fontRenderer.measureTextSize(info);

        //int curY = hh - (infoSize.y / 2);
        /*for (int i = 0; i < infoLines.length; i++) {
            String line = infoLines[i];
            Point2 lineSize = this.fontRenderer.measureTextSize(line);

            this.fontRenderer.drawCentredXWithShadow(pix, wh, curY, line);
            curY += lineSize.y + TEXT_MARGIN;
        }*/
        int texY = hh - (infoSize.y / 2);
        this.fontRenderer.drawLinesCentredXWithShadow(pix, wh, texY, infoLines);

        //alphas
        /*int alx = wh - (gameOverTextSize.x / 2) - this.alpha.width - ALPHA_OFFSET;
        int arx = wh + (gameOverTextSize.x / 2) + (ALPHA_OFFSET / 2);
        int ay = GAME_OVER_TEXT_OFFSET + 4;

        pix.draw(this.alpha, 0, 0, this.alpha.width, this.alpha.height, alx, ay, this.alpha.width, this.alpha.height, false, true);
        pix.draw(this.alpha, 0, 0, this.alpha.width, this.alpha.height, arx, ay, this.alpha.width, this.alpha.height, false, true);*/
        this.alphaRenderer.draw(pix, wh, GAME_OVER_TEXT_OFFSET + 4, gameOverTextSize.x);

        return pix;
    }

    private Pixmap rotate(Pixmap pix, double rot) {
        double rad = Math.toRadians(rot);
        Pixmap out = new Pixmap(pix.width, pix.height);

        double alpha = -Math.tan(rad * 0.5D);
        double beta = Math.sin(rad);

        shear(pix, out, alpha, beta);

        return out;
    }

    //https://www.ocf.berkeley.edu/~fricke/projects/israel/paeth/rotation_by_shearing.html
    private void shear(Pixmap pix, Pixmap out, double alpha, double beta) {
        int cx = pix.width / 2;
        int cy = pix.height / 2;

        for (int y = pix.height - 1; y >= 0; y--) {
            for (int x = pix.width - 1; x >= 0; x--) {
                //int pixel = pix.get(x, y);

                //shear 1
                int nx = x + ((int)Math.floor(alpha * (y - cy + 0.5D)));
                //shear 2
                int ny = y + ((int)Math.floor(beta * (nx - cx + 0.5D)));
                //shear 3
                nx += (int)Math.floor(alpha * (ny - cy + 0.5D));

                if (nx >= 0 && nx < pix.width &&
                    ny >= 0 && ny < pix.height) {
                    //outPix.set(nx, y, pixel);
                    out.set(nx, ny, pix.get(x, y));
                    //pix.set(x, y, 0x000000FF);
                }
            }
        }
    }

    public byte[] drawPng() {
        Pixmap pix = draw();
        byte[] png = Utils.pixToPng(pix);
        pix.dispose();
        return png;
    }
}
