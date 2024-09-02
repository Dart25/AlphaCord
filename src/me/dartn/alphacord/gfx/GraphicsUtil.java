package me.dartn.alphacord.gfx;

import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.io.Streams;

import java.io.IOException;
import java.util.zip.Deflater;

public class GraphicsUtil {
    private GraphicsUtil(){}

    public static byte[] pixToPng(Pixmap pix) {
        try (Streams.OptimizedByteArrayOutputStream out = new Streams.OptimizedByteArrayOutputStream(16384)) {
            PixmapIO.PngWriter writer = new PixmapIO.PngWriter();
            writer.setFlipY(false);
            writer.setCompression(Deflater.BEST_COMPRESSION);
            writer.write(out, pix);
            writer.dispose(); //fsr doesn't impl autodisposable

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
