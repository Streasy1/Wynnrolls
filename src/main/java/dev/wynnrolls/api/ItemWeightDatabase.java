package dev.wynnrolls.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wynnrolls.util.DebugLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Lädt item_weights.json (Wynntils Nori + WynnPool Scales) aus dem JAR.
 *
 * Format der JSON:
 *   { "wynnpool": { "ItemName": { "ScaleName": { "statKey": weight } } },
 *     "nori":    { ... } }
 *
 * Liefert pro Item eine Liste von ScaleEntry (source, scaleName, weights-Map).
 */
public class ItemWeightDatabase {

    public record ScaleEntry(String source, String scaleName, Map<String, Double> weights) {}

    // Key = lowercase item name → list of scales (all sources)
    private static final Map<String, List<ScaleEntry>> DB = new HashMap<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;

        try (InputStream is = ItemWeightDatabase.class.getResourceAsStream("/item_weights.json")) {
            if (is == null) {
                DebugLogger.error("item_weights.json nicht gefunden in Resources!");
                return;
            }

            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            int totalScales = 0;

            for (Map.Entry<String, JsonElement> sourceEntry : root.entrySet()) {
                String source = sourceEntry.getKey(); // "wynnpool" or "nori"
                JsonObject itemsObj = sourceEntry.getValue().getAsJsonObject();

                for (Map.Entry<String, JsonElement> itemEntry : itemsObj.entrySet()) {
                    String itemName = itemEntry.getKey();
                    String key = itemName.toLowerCase();
                    JsonObject scalesObj = itemEntry.getValue().getAsJsonObject();

                    List<ScaleEntry> entries = DB.computeIfAbsent(key, k -> new ArrayList<>());

                    for (Map.Entry<String, JsonElement> scaleEntry : scalesObj.entrySet()) {
                        String scaleName = scaleEntry.getKey();
                        JsonObject weightsObj = scaleEntry.getValue().getAsJsonObject();

                        Map<String, Double> weights = new LinkedHashMap<>();
                        for (Map.Entry<String, JsonElement> wEntry : weightsObj.entrySet()) {
                            weights.put(wEntry.getKey(), wEntry.getValue().getAsDouble());
                        }

                        entries.add(new ScaleEntry(source, scaleName, weights));
                        totalScales++;
                    }
                }
            }

            loaded = true;
            DebugLogger.log("ItemWeightDatabase geladen: " + DB.size() + " Items, "
                + totalScales + " Scales total");

        } catch (Exception e) {
            DebugLogger.error("Fehler beim Laden der ItemWeightDatabase", e);
        }
    }

    /**
     * Gibt alle Scales für ein Item zurück (aus allen Quellen, z.B. wynnpool + nori).
     * Gibt leere Liste zurück wenn keine Scales vorhanden.
     */
    public static List<ScaleEntry> getScales(String itemName) {
        if (itemName == null) return Collections.emptyList();
        List<ScaleEntry> result = DB.get(itemName.toLowerCase());
        return result != null ? result : Collections.emptyList();
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
