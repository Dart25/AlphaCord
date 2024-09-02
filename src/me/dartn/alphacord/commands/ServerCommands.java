package me.dartn.alphacord.commands;

import arc.graphics.Pixmap;
import arc.struct.Seq;
import me.dartn.alphacord.gfx.GraphicsUtil;
import me.dartn.alphacord.gfx.MapRenderer;
import mindustry.Vars;
import mindustry.entities.EntityGroup;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Locale;

public class ServerCommands extends ListenerAdapter {
    private static final int MAPLIST_PAGE_SIZE = 10;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmdName = event.getName().toLowerCase(Locale.ENGLISH);
        switch (cmdName) {
            //map list
            case "maps":
                int page = event.getOption("page").getAsInt() - 1;
                Seq<Map> maps = Vars.maps.customMaps();

                int pageStart = page * MAPLIST_PAGE_SIZE;
                if (pageStart < 0 || pageStart >= maps.size) {
                    event.reply("Invalid page number.");
                    break;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Map List");
                sb.append(" (page ");
                sb.append(page+1);
                sb.append(")\n\n");

                for (int i = pageStart; i < Math.min(maps.size, pageStart + MAPLIST_PAGE_SIZE); i++) {
                    Map map = maps.get(i);

                    sb.append('#');
                    sb.append(i+1);
                    sb.append(": ");
                    sb.append(map.plainName());
                    sb.append('\n');
                }
                event.reply(sb.toString()).queue();
                break;
            case "map":
                //screenshots probably will take more time to generate, show a "x is thinking..." message until we're done
                event.deferReply().queue();

                Pixmap ss = MapRenderer.takeMapScreenshot();
                byte[] ssPng = GraphicsUtil.pixToPng(ss);
                ss.dispose();

                FileUpload f = FileUpload.fromData(ssPng, "map.png");
                event.getHook().sendFiles(f).queue();
                break;
            case "list":
                EntityGroup<Player> players = Groups.player;

                //handle no players being online nicely
                if (players.isEmpty()) {
                    event.reply("Nobody's online :(").queue();
                    break;
                }

                StringBuilder responseBuilder = new StringBuilder();
                responseBuilder.append("Online players: ");
                for (Player p : players) {
                    responseBuilder.append(p.plainName());
                    responseBuilder.append(", ");
                }
                String response = responseBuilder.toString();
                //strip trailing ", "
                if (response.length() > 2) {
                    response = response.substring(0, response.length() - 2);
                }
                event.reply(response).queue();
                break;
        }
    }
}
