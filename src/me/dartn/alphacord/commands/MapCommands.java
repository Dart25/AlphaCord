package me.dartn.alphacord.commands;

import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.Strings;
import arc.util.io.Streams;
import me.dartn.alphacord.gfx.FontRenderer;
import me.dartn.alphacord.gfx.MapRenderer;
import mindustry.Vars;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.util.Locale;
import java.util.zip.Deflater;

import static mindustry.Vars.state;

public class MapCommands extends ListenerAdapter {
    private final FontRenderer fontRenderer;

    public MapCommands(FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmdName = event.getName().toLowerCase(Locale.UK);

        switch (cmdName) {
            //mapname cmd
            case "mapname":
                event.reply("The current map is \"" + Vars.state.map.name() + "\".").queue();
                break;
            //map screenshot cmd
            case "map":
                //screenshots probably will take more time to generate, show a "x is thinking..." message until we're done
                event.deferReply().queue();

                Pixmap pix = MapRenderer.takeMapScreenshot();
                this.fontRenderer.setScale(1);
                this.fontRenderer.draw(pix, 6, 5, Strings.format("Map: \"@\" by @", state.map.plainName(), state.map.plainAuthor()));

                try (Streams.OptimizedByteArrayOutputStream out = new Streams.OptimizedByteArrayOutputStream(65536)) {
                    PixmapIO.PngWriter writer = new PixmapIO.PngWriter();
                    writer.setFlipY(false);
                    writer.setCompression(Deflater.BEST_COMPRESSION);
                    writer.write(out, pix);
                    writer.dispose(); //fsr doesn't impl autodisposable

                    FileUpload f = FileUpload.fromData(out.toByteArray(), "map.png");

                    //reply
                    event.getHook().sendFiles(f).queue();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                pix.dispose();
                break;
        }
    }
}
