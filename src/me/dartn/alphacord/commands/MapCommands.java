package me.dartn.alphacord.commands;

import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.util.Strings;
import arc.util.io.Streams;
import me.dartn.alphacord.Utils;
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
                this.fontRenderer.setScale(Math.max(Math.min(state.map.width / 200, state.map.height / 200), 1));
                this.fontRenderer.drawWithShadow(pix, 6, 5, Strings.format("Map: \"@\" by @", state.map.plainName(), state.map.plainAuthor()));

                byte[] png = Utils.pixToPng(pix);
                pix.dispose();

                FileUpload f = FileUpload.fromData(png, "map.png");
                event.getHook().sendFiles(f).queue();
                break;
        }
    }
}
