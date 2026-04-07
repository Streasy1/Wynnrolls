package dev.wynnrolls;

import dev.wynnrolls.api.ItemDatabase;
import dev.wynnrolls.api.ItemWeightDatabase;
import dev.wynnrolls.tooltip.TooltipInjector;
import dev.wynnrolls.util.DebugLogger;
import dev.wynnrolls.util.ServerDetector;
import net.fabricmc.api.ClientModInitializer;

public class WynnRollsMod implements ClientModInitializer {

    public static final String MOD_ID = "wynnrolls";

    @Override
    public void onInitializeClient() {
        DebugLogger.log("WynnRolls initializing...");

        // Datenbanken aus gebündelten JSON-Dateien laden (kein Netzwerk)
        ItemDatabase.load();
        ItemWeightDatabase.load();

        // Register server detection
        ServerDetector.register();

        // Register tooltip callback
        TooltipInjector.register();

        DebugLogger.log("WynnRolls initialized. Join play.wynncraft.com to activate.");

        // DEV NOTE: Uncomment the line below to test without being on Wynncraft:
        // ServerDetector.forceEnable();
    }
}
