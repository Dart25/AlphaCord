package me.dartn.alphacord.gfx;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.math.Mathf;
import arc.math.Rand;
import arc.struct.ObjectIntMap;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class MapRenderer {
    //tile colours, since the game doesn't include them on server builds
    public static ObjectIntMap<String> colours;
    private static final Rand rng;

    static {
        colours = new ObjectIntMap<>();
        rng = new Rand();
    }

    //dispose the pixmap returned after calling this
    public static Pixmap takeMapScreenshot() {
        int w = Vars.world.width();
        int h = Vars.world.height();
        int ho = h - 1;
        Pixmap img = new Pixmap(w, h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Tile t = Vars.world.tile(x, y);
                //flip y since images are top-bottom while gl is bottom-top
                img.set(x, ho - y, colorFor(t));
            }
        }

        return img;
    }

    //mipmaprenderer line 360
    private static Block realBlock(Tile tile) {
        //TODO doesn't work properly until player goes and looks at block
        return tile.build == null ? tile.block() : state.rules.fog && !tile.build.wasVisible ? Blocks.air : tile.block();
    }

    //mipmaprenderer line 365
    private static int colorFor(Tile tile) {
        if (tile == null) return 0;
        Block real = realBlock(tile);
        int bc = real.minimapColor(tile);
        if (bc == 0) {
            bc = colorFor(real, tile.floor(), tile.overlay(), tile.team());
        }

        if (real == Blocks.sand) {
            //add some noise to sand
            int off = rng.nextInt(16);
            int noise = (off << 24) | (off << 16) | (off << 8) | 0xFF;
            bc += noise;
        }

        Color color = Tmp.c1.set(bc);
        color.mul(1f - Mathf.clamp(world.getDarkness(tile.x, tile.y) / 4f));

        if (real == Blocks.air && tile.y < world.height() - 1 && realBlock(world.tile(tile.x, tile.y + 1)).solid){
            color.mul(0.7f);
        } else if (tile.floor().isLiquid && (tile.y >= world.height() - 1 || !world.tile(tile.x, tile.y + 1).floor().isLiquid)){
            color.mul(0.84f, 0.84f, 0.9f, 1f);
        }

        return color.rgba();
    }

    private static int colorFor(Block wall, Block floor, Block overlay, Team team){
        if (wall.synthetic()) {
            return team.color.rgba();
        }

        return (((Floor)overlay).wallOre ? colours.get(overlay.name) : wall.solid ? colours.get(wall.name) : !overlay.useColor ? colours.get(floor.name) : colours.get(overlay.name));
    }
}
