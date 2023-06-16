package me.dartn.alphacord;

import arc.*;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class AlphaCordPlugin extends Plugin {
    private static final Config
            channelIdConf = new Config("channelId", "ID of the Discord channel to send/receive messages in.", "CID_HERE"),
            tokenConf = new Config("discordToken", "Token of the Discord bot to use.", "TOKEN_HERE"),
            webhookUrl = new Config("webhookUrl", "URL of the webhook to send messages through.", "WEBHOOK_URL_HERE");

    private JDA jda;
    private TextChannel channel;
    private WebhookClient webhookClient;

    //called when game initializes
    @Override
    public void init(){
        //make everyone know the plugin's been configured incorrectly if it has been :P
        if (channelIdConf.string() == channelIdConf.defaultValue ||
                tokenConf.string() == tokenConf.defaultValue ||
                webhookUrl.string() == webhookUrl.defaultValue) {
            Events.on(PlayerJoin.class, event -> {
                //send a message telling everyone that the admin should configure the plugin correctly
                Call.sendMessage("[scarlet]ALERT![] AlphaCord has not been configured correctly! The server's owner/administrator should set the channelId, webhookUrl, and discordToken configs. (eg. run \"config channelId 1098728495691083806\" in the server's console)");
            });

            return; //skip all further initialization if the plugin isn't configured correctly, to avoid crashing everything...
        }

        //JDA setup
        JDABuilder jdaBuilder = JDABuilder.createDefault(tokenConf.string());

        jdaBuilder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        jdaBuilder.setActivity(Activity.playing("Animdustry"));

        jda = jdaBuilder.build();
        try {
            jda.awaitReady(); //or getTextChannelById may return null
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channel = jda.getTextChannelById(channelIdConf.string());
        webhookClient = JDAWebhookClient.withUrl(webhookUrl.string());

        //cleanup, this adds a new ApplicationListener because DisposeEvent isn't fired on the server, since
        //fsr it's fired from the Renderer class...
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                sendServerMessage("Server stopped!");

                webhookClient.close();
                jda.shutdownNow();
            }
        });

        //i realise this code is pretty much a duplicate of the above, but :TohruShrug:
        if (channel == null) {
            Events.on(PlayerJoin.class, event -> {
                //send a message telling everyone that the admin should configure the plugin correctly
                Call.sendMessage("[scarlet]ALERT![] AlphaCord has not been configured correctly! The channel set by the server administrator doesn't exist!");
            });

            return; //skip all further initialization if the plugin isn't configured correctly, to avoid crashing everything...
        }

        //listen for a mindustry chat message event
        Events.on(PlayerChatEvent.class, this::sendDiscordMessage);

        //player join + leave messages
        Events.on(PlayerJoin.class, event -> {
            sendServerMessage(event.player.name + " joined.");
        });
        Events.on(PlayerLeave.class, event -> {
            sendServerMessage(event.player.name + " left.");
        });

        //map load + game over messages
        Events.on(PlayEvent.class, event -> {
            sendServerMessage(getCurrentModeName() + " game started on " + Vars.state.map.name() + "!");
        });
        Events.on(GameOverEvent.class, event -> {
            //why oh why does java not have string interpolation?
            String message = String.format(
                    """
                    Game over on %s!
                    %d waves passed,
                    %d enemies destroyed,
                    %d buildings built,
                    %d buildings destroyed,
                    and %d units built.
                    """,
                    Vars.state.map.name(), Vars.state.stats.wavesLasted, Vars.state.stats.enemyUnitsDestroyed,
                    Vars.state.stats.buildingsBuilt, Vars.state.stats.buildingsDestroyed, Vars.state.stats.unitsCreated
            );

            sendServerMessage(message);
        });

        //listen for a discord chat event
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                //ignore bot/webhook messages & messages from the wrong channel
                if (event.getAuthor().isBot() || event.isWebhookMessage() ||
                        !event.getChannel().getId().equals(channelIdConf.string())) return;

                //sorta hacky, String.repeat throws a "cannot find symbol" error because java 8, so we do this
                StringBuilder attBuilder = new StringBuilder();

                for (int i = 0; i < event.getMessage().getAttachments().size(); i++) {
                    attBuilder.append("<attachment> ");
                }

                Core.app.post(() -> { //uE80D is the Discord symbol ingame
                    Call.sendMessage("[blue]\uE80D [" + colourToHex(event.getMember().getColor()) + "]" +
                            event.getMember().getEffectiveName() + ":[white] " +
                            event.getMessage().getContentDisplay() + (attBuilder.length() > 0 ? " " : "") +
                            attBuilder.toString().trim());
                });
            }
        });

        sendServerMessage("Server started!");
    }

    //util method to send a message to discord from a PlayerChatEvent easily
    private void sendDiscordMessage(PlayerChatEvent event) {
        //avatarUrl is the alpha unit sprite
        Unit playerUnit = event.player.unit();

        String avatarUrl = String.format("https://dartn.duckdns.org/Mindustry/teams/team%d/%s.png",
                playerUnit.team.id, playerUnit.type.name);

        // used to default to https://files.catbox.moe/1dmf06.png
        sendDiscordMessage(event.player.name, cleanMessage(event.message), avatarUrl);
    }

    private void sendServerMessage(String message) {
        //avatarUrl is alpha-chan >w< sprite because i couldn't really find something that fits "mindustry server",
        //and just using a core is boring :P
        sendDiscordMessage("Server", message, "https://files.catbox.moe/hd82m4.png");
    }

    private void sendDiscordMessage(String username, String message, String avatarUrl) {
        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();

        msgBuilder.setUsername(username);
        msgBuilder.setAvatarUrl(avatarUrl);
        msgBuilder.setContent(message);

        WebhookMessage msg = msgBuilder.build();

        webhookClient.send(msg);
    }

    //ported from https://github.com/Brandons404/easyDiscordPlugin/blob/master/scripts/main.js#L39 but modified a bit
    private static String cleanMessage(String message) {
        int lastCharCode = message.codePointAt(message.length() - 1);
        int secondLastCharCode = message.codePointAt(message.length() - 2);

        if (lastCharCode >= 0xf80 && lastCharCode <= 0x107f && secondLastCharCode >= 0xf80 && secondLastCharCode <= 0x107f) {
            //If the last two characters are both in the range U+0F80 to U+0x107F, then they were generated by foo's client and should not be displayed
            message = message.substring(0, message.length() - 2); //Remove them
        }

        return message;
    }

    //there has to be a builtin function for this somewhere but i can't find it
    private static String getCurrentModeName() {
        if (Vars.state.rules.pvp) return "PVP";
        if (Vars.state.rules.infiniteResources) return "Sandbox";
        if (Vars.state.rules.attackMode) return "Attack";
        if (Vars.state.rules.editor) return "Editor";

        return "Survival";
    }

    // https://forums.oracle.com/ords/apexds/post/convert-java-awt-color-to-hex-string-8724#comment_323462165417437941337851389448683170665
    private static String colourToHex(Color colour) {
        return String.format("#%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
