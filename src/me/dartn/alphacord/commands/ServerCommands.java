package me.dartn.alphacord.commands;

import arc.graphics.Pixmap;
import arc.util.Strings;
import me.dartn.alphacord.Utils;
import me.dartn.alphacord.gfx.MapListRenderer;
import mindustry.Vars;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Locale;

public class ServerCommands extends ListenerAdapter {
    private final MapListRenderer mapListRenderer;

    public ServerCommands(MapListRenderer mapListRenderer) {
        this.mapListRenderer = mapListRenderer;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase(Locale.UK);

        switch (name) {
            //map list
            case "maps":
                int page = event.getOption("page").getAsInt();
                int zeroPage = page - 1;

                if (!Utils.isPageValid(zeroPage)) {
                    event.reply("Invalid page number!").queue();
                    return;
                }

                event.deferReply().queue();

                Pixmap mapListPix = this.mapListRenderer.renderMapList(page);
                byte[] png = Utils.pixToPng(mapListPix);
                mapListPix.dispose();

                FileUpload f = FileUpload.fromData(png, "map.png");
                event.getHook().sendFiles(f).queue();
                break;
        }
    }
}
