package dev.wynnrolls.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wynnrolls.util.DebugLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lädt die gebündelte items.json (Wynntils Static Storage, gear_expanded Format)
 * beim Start einmalig in den Speicher.
 *
 * Kein Netzwerkzugriff zur Laufzeit.
 */
public class ItemDatabase {

    // Key = lowercase item name → ItemData
    private static final ConcurrentHashMap<String, ItemData> DB = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    /**
     * Lädt items.json aus dem JAR-Resources-Ordner.
     * Muss einmalig beim Mod-Start aufgerufen werden.
     */
    public static void load() {
        if (loaded) return;

        try (InputStream is = ItemDatabase.class.getResourceAsStream("/items.json")) {
            if (is == null) {
                DebugLogger.error("items.json nicht gefunden in Resources!");
                return;
            }

            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            int loaded_count = 0;
            int skipped_no_ids = 0;

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String itemName = entry.getKey();
                JsonObject item = entry.getValue().getAsJsonObject();

                String tier = getStringOrNull(item, "tier");
                String type = getStringOrNull(item, "type");
                String subType = getStringOrNull(item, "subType");

                Map<String, IdentificationData> ids = new HashMap<>();

                if (item.has("identifications") && !item.get("identifications").isJsonNull()) {
                    JsonObject idObj = item.getAsJsonObject("identifications");
                    for (Map.Entry<String, JsonElement> idEntry : idObj.entrySet()) {
                        JsonElement val = idEntry.getValue();
                        // Pre-identified = primitiver int → überspringen
                        if (val.isJsonPrimitive()) continue;
                        // Gerollte Identification = Objekt mit min/max/raw
                        if (val.isJsonObject()) {
                            JsonObject idData = val.getAsJsonObject();
                            if (idData.has("min") && idData.has("max") && idData.has("raw")) {
                                int min = idData.get("min").getAsInt();
                                int max = idData.get("max").getAsInt();
                                int raw = idData.get("raw").getAsInt();
                                // Nur laden wenn ein echter Bereich existiert (min != max)
                                if (min != max) {
                                    ids.put(idEntry.getKey(), new IdentificationData(min, max, raw));
                                }
                            }
                        }
                    }
                }

                if (ids.isEmpty()) {
                    skipped_no_ids++;
                }

                // Auch Items ohne rollbare IDs speichern (für Name-Lookup / Tier-Anzeige)
                DB.put(itemName.toLowerCase(), new ItemData(itemName, tier, type, subType, ids));
                loaded_count++;
            }

            loaded = true;
            DebugLogger.log("ItemDatabase geladen: " + loaded_count + " Items "
                + "(" + skipped_no_ids + " ohne rollbare IDs)");

        } catch (Exception e) {
            DebugLogger.error("Fehler beim Laden der ItemDatabase", e);
        }
    }

    /**
     * Sucht ein Item per Name (case-insensitive).
     * Gibt null zurück wenn nicht gefunden.
     */
    public static ItemData get(String name) {
        return DB.get(name.toLowerCase());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int size() {
        return DB.size();
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
