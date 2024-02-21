package me.dartn.alphacord.gfx;

import arc.graphics.Pixmap;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Strings;
import me.dartn.alphacord.GuiConstants;
import mindustry.Vars;
import mindustry.maps.Map;

import static me.dartn.alphacord.GuiConstants.*;

public class MapListRenderer {
    private static final String HEADING_TEXT = "Maps";
    public static final int MAPS_PER_PAGE = 10;

    private final FontRenderer fontRenderer;
    private final AlphaRenderer alphaRenderer;

    public MapListRenderer(FontRenderer fontRenderer, AlphaRenderer alphaRenderer) {
        this.fontRenderer = fontRenderer;
        this.alphaRenderer = alphaRenderer;
    }

    //page here starts at 1, not 0
    public Pixmap renderMapList(int page) {
        Pixmap pix = new Pixmap(GUI_WIDTH, GUI_HEIGHT);
        pix.fill(0x172947FF);
        Seq<Map> maps = Vars.maps.customMaps();

        //heading
        this.fontRenderer.setScale(3);
        Point2 headingSize = this.fontRenderer.measureTextSize(HEADING_TEXT);
        this.fontRenderer.drawCentredXWithShadow(pix, GUI_WIDTH / 2, HEADING_TEXT_OFFSET, HEADING_TEXT);

        //page #
        this.fontRenderer.setScale(1);
        String pageText = Strings.format("(page @)", page);
        Point2 pageTextSize = this.fontRenderer.measureTextSize(pageText);
        this.fontRenderer.drawCentredXWithShadow(pix, GUI_WIDTH / 2, HEADING_TEXT_OFFSET + headingSize.y + 4, pageText);

        //map listing
        this.fontRenderer.setScale(2);

        int offset = (page - 1) * MAPS_PER_PAGE;
        int drawY = HEADING_TEXT_OFFSET + headingSize.y + 4 + pageTextSize.y;

        drawListLines(pix, GUI_WIDTH / 2, drawY, maps, offset, Math.min(offset + MAPS_PER_PAGE, maps.size));

        //alphas
        this.alphaRenderer.draw(pix, GUI_WIDTH / 2, HEADING_TEXT_OFFSET + 4, headingSize.x);

        return pix;
    }

    public void drawListLines(Pixmap target, int x, int y, Seq<Map> maps, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            String line = Strings.format("@: @", i, maps.get(i).plainName());
            Point2 lineSize = this.fontRenderer.measureTextSize(line);

            this.fontRenderer.drawCentredXWithShadow(target, x, y, line);
            y += lineSize.y + this.fontRenderer.getScale();
        }
    }
}
