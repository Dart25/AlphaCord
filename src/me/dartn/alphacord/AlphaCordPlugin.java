package me.dartn.alphacord;

import arc.*;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
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
            webhookUrl = new Config("webhookUrl", "URL of the webhook to send messages through.", "WEBHOOK_URL_HERE"),
            adminLogChannelId = new Config("adminLogChannelId", "ID of the Discord channel to send messages in. In the old format, to make it easier to search for messages from a specific player.", "ADMIN_CID_HERE");

    //Make the last 2 false to disable pinging Discord users from Mindustry
    private static final AllowedMentions allowedMentions = new AllowedMentions()
            .withParseEveryone(false)
            .withParseUsers(true)
            .withParseRoles(true);

    private JDA jda;
    private WebhookClient webhookClient;
    private boolean adminLogEnabled;
    private TextChannel adminLogChannel;

    //Called when the game initializes
    @Override
    public void init(){
        //Make everyone know the plugin's been configured incorrectly if it has been :P
        if (channelIdConf.string() == channelIdConf.defaultValue ||
                tokenConf.string() == tokenConf.defaultValue ||
                webhookUrl.string() == webhookUrl.defaultValue) {
            Events.on(PlayerJoin.class, event -> {
                //Send a message telling everyone that the admin should configure the plugin correctly
                Call.sendMessage("[scarlet]ALERT![] AlphaCord has not been configured correctly! The server's owner/administrator should set the channelId, webhookUrl, and discordToken configs. (eg. run \"config channelId 1098728495691083806\" in the server's console)");
            });

            return; //Skip all further initialization if the plugin isn't configured correctly, to avoid crashing everything...
        }

        adminLogEnabled = adminLogChannelId.string() != adminLogChannelId.defaultValue;

        //JDA setup
        JDABuilder jdaBuilder = JDABuilder.createDefault(tokenConf.string());

        jdaBuilder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        jdaBuilder.setActivity(Activity.playing("Animdustry"));

        jda = jdaBuilder.build();
        try {
            jda.awaitReady(); //Or getTextChannelById may return null
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        webhookClient = JDAWebhookClient.withUrl(webhookUrl.string());

        if (adminLogEnabled) {
            adminLogChannel = jda.getTextChannelById(adminLogChannelId.string());
        }

        //Cleanup, this adds a new ApplicationListener because DisposeEvent isn't fired on the server, since
        //for some reason it's fired from the Renderer class...
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                sendServerMessage("Server stopped!");

                webhookClient.close();
                jda.shutdownNow();
            }
        });

        //I realise this code is pretty much a duplicate of the above, but :TohruShrug:
        if (jda.getTextChannelById(channelIdConf.string()) == null) {
            Events.on(PlayerJoin.class, event -> {
                //Send a message telling everyone that the admin should configure the plugin correctly
                Call.sendMessage("[scarlet]ALERT![] AlphaCord has not been configured correctly! The channel set by the server administrator doesn't exist!");
            });

            return; //Skip all further initialization if the plugin isn't configured correctly, to avoid crashing everything...
        }

        //Listen for a Mindustry chat message event
        Events.on(PlayerChatEvent.class, this::sendDiscordMessage);

        //Player join + leave messages
        Events.on(PlayerJoin.class, event -> {
            sendServerMessage(Strings.stripColors(event.player.name) + " joined.");
        });
        Events.on(PlayerLeave.class, event -> {
            sendServerMessage(Strings.stripColors(event.player.name) + " left.");
        });
        Events.on(PlayerConnect.class, event -> {
            System.out.println(event.player.name);
        });

        //Map load + Game Over messages
        Events.on(PlayEvent.class, event -> {
            //toString seems to be recommended but doesn't return the correct results (?, probably just me being stupid)
            String modeName = Strings.capitalize(Vars.state.rules.mode().name());

            sendServerMessage(modeName + " game started on " + Vars.state.map.name() + "!");
        });
        Events.on(GameOverEvent.class, event -> {
            //Why oh why does Java not have string interpolation?
            String message = Strings.format(
                    """
                    Game over on @!
                    @ waves passed,
                    @ enemies destroyed,
                    @ buildings built,
                    @ buildings destroyed,
                    and @ units built,
                    with @ people online.
                    """,
                    Vars.state.map.name(), Vars.state.stats.wavesLasted, Vars.state.stats.enemyUnitsDestroyed,
                    Vars.state.stats.buildingsBuilt, Vars.state.stats.buildingsDestroyed, Vars.state.stats.unitsCreated,
                    Groups.player.size()
            );

            sendServerMessage(message);
        });

        //Listen for a Discord chat event
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                //Ignore bot/webhook messages & messages from the wrong channel
                if (event.getAuthor().isBot() || event.isWebhookMessage() ||
                        !event.getChannel().getId().equals(channelIdConf.string())) return;

                //Sorta hacky, String.repeat throws a "cannot find symbol" error because Java 8, so we do this
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

    //Util method to send a message to Discord from a PlayerChatEvent easily
    private void sendDiscordMessage(PlayerChatEvent event) {
        Unit playerUnit = event.player.unit();

        String avatarUrl = Strings.format("https://dartn.duckdns.org/Mindustry/teams/team@/@.png",
                playerUnit.team.id, playerUnit.type.name);

        //easier to search for messages from a specific player in the old format, this is also uncensored & colours aren't removed
        if (adminLogEnabled) {
            adminLogChannel.sendMessage(Strings.format("**@**: @", event.player.name, cleanMessage(event.message))).queue();
        }

        String filteredMessage = cleanMessage(event.message);

        //spam filter
        if (msgIsSpam(event.player, filteredMessage)) return;

        //spam filter is always index 0, we skip it because we have our own impl
        for (int i = 1; i < Vars.netServer.admins.chatFilters.size; i++) {
            filteredMessage = Vars.netServer.admins.chatFilters.get(i).filter(event.player, filteredMessage);
            if (filteredMessage == null) return;
        }

        //used to default to https://files.catbox.moe/1dmf06.png
        sendDiscordMessage(Strings.stripColors(event.player.name), Strings.stripColors(filteredMessage), avatarUrl);
    }

    private void sendServerMessage(String message) {
        //avatarUrl is the alpha-chan >w< sprite because I couldn't really find something that fits "Mindustry server",
        //and just using a core is boring :P
        if (adminLogEnabled) {
            adminLogChannel.sendMessage(message).queue();
        }
        sendDiscordMessage("Server", message, "https://dartn.duckdns.org/Mindustry/alpha.png");
    }

    private void sendDiscordMessage(String username, String message, String avatarUrl) {
        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();

        msgBuilder.setUsername(username);
        msgBuilder.setAvatarUrl(avatarUrl);
        msgBuilder.setContent(message);
        msgBuilder.setAllowedMentions(allowedMentions);

        WebhookMessage msg = msgBuilder.build();

        webhookClient.send(msg);
    }

    //https://github.com/Anuken/Mindustry/blob/93daa7a5dcc3fac9e5f40c3375e9f57ae4720ff4/core/src/mindustry/net/Administration.java#L36
    private static boolean msgIsSpam(Player player, String message) {
        long resetTime = Config.messageRateLimit.num() * 1000L;
        if(Config.antiSpam.bool() && !player.isLocal() && !player.admin){
            //prevent people from spamming messages quickly
            if(resetTime > 0 && Time.timeSinceMillis(player.getInfo().lastMessageTime) < resetTime){
                return true;
            }

            //prevent players from sending the same message twice in the span of 10 seconds
            return message.equals(player.getInfo().lastSentMessage) && Time.timeSinceMillis(player.getInfo().lastMessageTime) < 1000 * 10;
        }

        return false;
    }

    //Ported from https://github.com/Brandons404/easyDiscordPlugin/blob/master/scripts/main.js#L39 but modified a bit.
    private static String cleanMessage(String message) {
        if (message.length() < 2) return message;

        int lastCharCode = message.codePointAt(message.length() - 1);
        int secondLastCharCode = message.codePointAt(message.length() - 2);

        if (lastCharCode >= 0xf80 && lastCharCode <= 0x107f && secondLastCharCode >= 0xf80 && secondLastCharCode <= 0x107f) {
            //If the last two characters are both in the range U+0F80 to U+0x107F, then they were generated by foo's client and should not be displayed
            message = message.substring(0, message.length() - 2); //Remove them
        }

        return message;
    }

    //https://forums.oracle.com/ords/apexds/post/convert-java-awt-color-to-hex-string-8724#comment_323462165417437941337851389448683170665
    private static String colourToHex(Color colour) {
        return String.format("#%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
