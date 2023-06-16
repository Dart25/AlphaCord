package me.dartn.alphacord;

import arc.*;
import arc.util.*;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration.*;
import mindustry.world.blocks.storage.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

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

        //cleanup
        Events.on(DisposeEvent.class, event -> {
            sendServerMessage("Server stopped!");
            jda.shutdown();
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

        //listen for a discord chat event
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                //ignore bot/webhook messages
                if (event.getAuthor().isBot() || event.isWebhookMessage()) return;

                Call.sendMessage("[blue][Discord] " + event.getAuthor().getEffectiveName() + "[]: " + event.getMessage().getContentDisplay());
            }
        });

        sendServerMessage("Server started!");
    }

    //util method to send a message to discord from a PlayerChatEvent easily
    private void sendDiscordMessage(PlayerChatEvent event) {
        //avatarUrl is the alpha unit sprite
        sendDiscordMessage(event.player.name, cleanMessage(event.message), "https://files.catbox.moe/1dmf06.png");
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
}
