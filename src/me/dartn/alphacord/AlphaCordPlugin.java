package me.dartn.alphacord;

import arc.*;
import arc.func.ConsT;
import arc.util.*;
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
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Properties;

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
            .withParseRoles(false);

    private JDA jda;
    private WebhookClient webhookClient;
    private TextChannel adminLogChannel;

    //emote replacement range
    private static final int emoteRangeStart = 0xF675;
    private static final int emoteRangeEnd = 0xF8FF;

    //icon substitutions
    private static final CharReplacement[] rankReplacements = new CharReplacement[] {
        CharReplacement.rankPrefix(Iconc.add, 'T'), //trusted
        CharReplacement.rankPrefix(Iconc.hammer, 'M'), //mod
        CharReplacement.rankPrefix(Iconc.admin, 'A'), //admin
        CharReplacement.rankPrefix(Iconc.logic, 'D'), //dev
        CharReplacement.rankPrefix(Iconc.star, 'F'), //fish member
        CharReplacement.rankPrefix(Iconc.eye, 'G') //manager
    };
    //special indexed mindy -> dc emote array
    private static final String[] emoteReplacements = new String[emoteRangeEnd - emoteRangeStart + 1];
    //arc.struct.

    static {
        loadEmoteDatabase();
    }

    //Called when the game initializes
    @Override
    public void init(){
        
        //Config check
        if (channelIdConf.string().equals(channelIdConf.defaultValue) ||
                tokenConf.string().equals(tokenConf.defaultValue) ||
                webhookUrl.string().equals(webhookUrl.defaultValue)) {
            Log.err("[AlphaCord] Configuration not complete. Set the channelId, webhookUrl, and discordToken configs.");
            return; //Skip all further initialization if the plugin isn't configured correctly, to avoid crashing everything...
        }

        //Cleanup, uses ApplicationListener because DisposeEvent isn't fired on the server, since
        //for some reason it's fired from the Renderer class...
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                sendServerMessage("Server stopped!");

                if(webhookClient != null) webhookClient.close();
                if(jda != null) jda.shutdownNow();
            }
        });

        //JDA setup
        try {
            jda = JDABuilder.createDefault(tokenConf.string())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("on the Fish Mindustry server"))
                .build();

            webhookClient = JDAWebhookClient.withUrl(webhookUrl.string());
        } catch (Exception e){
            e.printStackTrace();
            return;
        }
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                if (!adminLogChannelId.string().equals(adminLogChannelId.defaultValue)) {
                    adminLogChannel = jda.getTextChannelById(adminLogChannelId.string());
                }
                if (jda.getTextChannelById(channelIdConf.string()) == null) {
                    Log.err("[AlphaCord] Configuration error: Configured channel @ does not exist, or the bot does not have access to it.", channelIdConf.string());
                    return; //Skip all further initialization if the plugin isn't configured correctly, to avoid crashing everything...
                }
            }
        });



        //Listen for a Mindustry chat message event
        Events.on(PlayerChatEvent.class, this::onPlayerChat);

        //Player join + leave messages
        Events.on(PlayerJoin.class, event -> {
            sendServerMessage(Strings.format("**@** joined.", cleanNameToDiscord(event.player.name)));//!
        });
        Events.on(PlayerLeave.class, event -> {
            sendServerMessage(Strings.format("**@** left.", cleanNameToDiscord(event.player.name)));//!
        });

        //Map load + Game Over messages
        Events.on(PlayEvent.class, event -> {
            sendServerMessage("New game started on " + cleanTextToDiscord(Vars.state.map.name()) + "!");//!
        });
        Events.on(GameOverEvent.class, event -> {
            //Why oh why does Java not have string interpolation?
            sendServerMessage(Strings.format(//!
                    """
                    Game over on **@**!
                    `@` waves passed,
                    `@` enemies destroyed,
                    `@` buildings built,
                    `@` buildings destroyed,
                    and `@` units built,
                    with `@` people online.
                    """, //TODO make message include winning team, and gamemode specific (eg say game over on survival, defeat/victory on attack, something else on pvp)
                    cleanTextToDiscord(Vars.state.map.name()), Vars.state.stats.wavesLasted, Vars.state.stats.enemyUnitsDestroyed,
                    Vars.state.stats.buildingsBuilt, Vars.state.stats.buildingsDestroyed, Vars.state.stats.unitsCreated,
                    Groups.player.size()
            ));
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
                //!
                Core.app.post(() -> { //uE80D is the Discord symbol ingame
                    Call.sendMessage("[blue]\uE80D [" + colourToHex(event.getMember().getColor()) + "]" +
                            event.getMember().getEffectiveName() + ":[white] " +
                            event.getMessage().getContentDisplay() + (attBuilder.length() < 1 ? "" : " ") +
                            attBuilder.toString().trim());
                    Log.info("(Discord) &fi@:, @", "&lc" + event.getMember().getEffectiveName(), event.getMessage().getContentDisplay());
                });
            }
        });

        sendServerMessage("Server started!");
    }

    private void onPlayerChat(PlayerChatEvent event) { //!
        //ignore messages from muted players
        if (FishGlue.isPlayerMuted(event.player.uuid())) return;

        Unit playerUnit = event.player.unit();
        String avatarUrl = Strings.format("https://dartn.duckdns.org/Mindustry/teams/team@/@.png",
                playerUnit.team.id, playerUnit.type.name);

        String filteredMessage = cleanMessage(event.message);
        
        //admin log messages are in the old format which is easier to search, this is also uncensored & colours aren't removed
        sendAdminLogMessage(Strings.format("**@**: @", Strings.stripColors(event.player.name), filteredMessage));


        if (msgIsSpam(event.player, filteredMessage)) return;

        //don't send commands (but send them to log)
        if (filteredMessage.startsWith(Vars.netServer.clientCommands.getPrefix())) return;

        //spam filter is always index 0, we skip it because we have our own impl
        for (int i = 1; i < Vars.netServer.admins.chatFilters.size; i++) {
            filteredMessage = Vars.netServer.admins.chatFilters.get(i).filter(event.player, filteredMessage);
            if (filteredMessage == null) return;
        }

        //used to default to https://files.catbox.moe/1dmf06.png
        sendDiscordMessage(cleanNameToDiscord(event.player.name), cleanTextToDiscord(filteredMessage), avatarUrl);
    }

    private void sendServerMessage(String message) {
        //avatarUrl is the alpha-chan >w< sprite because I couldn't really find something that fits "Mindustry server",
        //and just using a core is boring :P
        try {
            sendAdminLogMessage(message);
            sendDiscordMessage("Server", message, "https://dartn.duckdns.org/Mindustry/alpha.png");
        } catch (Exception ignored) { }
    }


    private static String fixRankEmojis(String text){
        for (int i = 0; i < rankReplacements.length; i++) {
            text = rankReplacements[i].process(text);
        }
        return text;
    }

    private static String fixMessageEmojis(String msg) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c >= emoteRangeStart && c <= emoteRangeEnd) {
                //fix c
                int idx = (int)c - emoteRangeStart;
                String replacement = emoteReplacements[idx];
                //ignore if c isn't in the database
                if (replacement == null) {
                    result.append(c);
                    continue;
                }

                //append the replacement instead of the char
                result.append(replacement);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String escapeTextDiscord(String text){
        return text.replaceAll("([\\\\*_~`|:])", "\\\\$1");
    }
    private static String cleanNameToDiscord(String text){
        return fixRankEmojis(escapeTextDiscord(Strings.stripColors(text)));
    }
    private static String cleanTextToDiscord(String text){
        return fixMessageEmojis(escapeTextDiscord(Strings.stripColors(text)));
    }

    private void sendDiscordMessage(String username, String message, String avatarUrl) {
        if (username.isEmpty() || message.isEmpty()) {
            /*Log.info("FAIL!");
            Log.info(username);
            Log.info(message);*/
            return;
        }

        webhookClient.send(
            new WebhookMessageBuilder()
                .setUsername(username)
                .setAvatarUrl(avatarUrl)
                .setContent(message)
                .setAllowedMentions(allowedMentions)
                .build()
        );
    }

    private void sendAdminLogMessage(String message){
        try {
            if(adminLogChannel != null) adminLogChannel.sendMessage(message).queue();
        } catch(Exception ignored){}
    }

    //https://github.com/Anuken/Mindustry/blob/93daa7a5dcc3fac9e5f40c3375e9f57ae4720ff4/core/src/mindustry/net/Administration.java#L36
    private static boolean msgIsSpam(Player player, String message) {
        long resetTime = Config.messageRateLimit.num() * 1000L;
        if (Config.antiSpam.bool() && !player.isLocal() && !player.admin){
            //prevent people from spamming messages quickly
            if (resetTime > 0 && Time.timeSinceMillis(player.getInfo().lastMessageTime) < resetTime){
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

    private static void loadEmoteDatabase() {
        //download the emote db from my server
        Log.info("Downloading emote database...");
        Http.get("https://dartn.duckdns.org/Mindustry/emdb.xml", res -> {
            //parse and load
            Properties props = new Properties();
            props.loadFromXML(res.getResultAsStream());
            props.forEach((key, value) -> {
                //Safe cast, props is clean
                emoteReplacements[Integer.parseInt((String)key, 16) - emoteRangeStart] = (String)value;
            });
            Log.info("Downloaded emote database.");
        }, e -> Log.err("EDB load failed! Emote fixer won't work :/"));
    }

    //https://forums.oracle.com/ords/apexds/post/convert-java-awt-color-to-hex-string-8724#comment_323462165417437941337851389448683170665
    public static String colourToHex(Color colour) {
        if (colour == null) return "#FFFFFF";
        return String.format("#%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue());
    }
}
