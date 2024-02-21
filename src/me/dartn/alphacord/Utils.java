package me.dartn.alphacord;

import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.io.Streams;
import me.dartn.alphacord.gfx.MapListRenderer;
import mindustry.Vars;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.util.zip.Deflater;

public class Utils {
    public static byte[] pixToPng(Pixmap pix) {
        try (Streams.OptimizedByteArrayOutputStream out = new Streams.OptimizedByteArrayOutputStream(16384)) {
            PixmapIO.PngWriter writer = new PixmapIO.PngWriter();
            writer.setFlipY(false);
            writer.setCompression(Deflater.BEST_COMPRESSION);
            writer.write(out, pix);
            writer.dispose(); //fsr doesn't impl autodisposable

            return out.toByteArray();

            //FileUpload f = FileUpload.fromData(out.toByteArray(), "map.png");

            //reply
            //event.getHook().sendFiles(f).queue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //page is 0-indexed
    public static boolean isPageValid(int page) {
        if (page < 0) return false;
        return page * MapListRenderer.MAPS_PER_PAGE < Vars.maps.customMaps().size;
    }
}
