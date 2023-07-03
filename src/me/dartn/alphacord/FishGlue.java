package me.dartn.alphacord;

import arc.util.Log;
import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.mod.Scripts;
import rhino.Context;
import rhino.ScriptableObject;

public class FishGlue {
    private static final String glueScriptName = "alphaCordGlue.js";
    private static final String script = """
            const players = require("players");
            return players.FishPlayer.getById(alphaCordUserId).muted;""";
    private static final String idVarName = "alphaCordUserId";

    public static boolean isPlayerMuted(String playerId) {
        if (!Vars.mods.hasScripts()) throw new RuntimeException("Scripts not loaded");

        Mods.LoadedMod fishCommands = Vars.mods.getMod("fish");
        if (fishCommands == null) throw new RuntimeException("fish-commands not loaded");

        Scripts scripts = Vars.mods.getScripts();

        //hmmm
        try {
            //set the player id to check
            Object wrappedId = Context.javaToJS(playerId, scripts.scope);
            ScriptableObject.putProperty(scripts.scope, idVarName, wrappedId);

            //inject script info into file
            scripts.context.evaluateString(scripts.scope, "modName = \"" + fishCommands.name + "\"\nscriptName = \"" + glueScriptName + "\"", "initscript.js", 1);
            Object result = scripts.context.evaluateString(scripts.scope,
                    "(function(){'use strict';\n" + script + "\n})();",
                    glueScriptName, 0);

            //delete the temporary player id var
            ScriptableObject.deleteProperty(scripts.scope, idVarName);
            return result == "true";
        } catch(Throwable t) {
            Log.err("Failed to run glue code to determine if player is muted!", t);
            return false;
        }
    }
}
