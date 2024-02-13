package me.dartn.alphacord.commands;

import mindustry.entities.EntityGroup;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Locale;

public class PlayerCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmdName = event.getName().toLowerCase(Locale.UK);

        switch (cmdName) {
            case "list":
                EntityGroup<Player> players = Groups.player;

                //handle no players being online nicely
                if (players.isEmpty()) {
                    event.reply("Nobody's online :(").queue();
                    break;
                }

                StringBuilder responseBuilder = new StringBuilder();
                responseBuilder.append("Currently online players: ");
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
