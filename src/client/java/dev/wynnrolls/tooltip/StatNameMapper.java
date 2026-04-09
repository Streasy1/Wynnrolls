package dev.wynnrolls.tooltip;

import dev.wynnrolls.api.IdentificationData;
import dev.wynnrolls.api.ItemData;

import java.util.HashMap;
import java.util.Map;

/**
 * Wandelt Tooltip-Anzeigenamen in API-Keys um.
 *
 * Strategie (in Reihenfolge):
 * 1. Hardcoded Ausnahmen (Sonderfälle)
 * 2. Spell-Cost-Fallback: "*cost" → Wert-Matching gegen raw1st-4thSpellCost
 * 3. camelCase: "Life Steal" → "lifeSteal"
 * 4. raw-Prefix: "lifeSteal" → "rawLifeSteal"
 */
public class StatNameMapper {

    // Spell-Cost-Slots: flache (raw) und prozentuale Varianten getrennt
    private static final String[] SPELL_COST_SLOTS_RAW = {
        "raw1stSpellCost", "raw2ndSpellCost", "raw3rdSpellCost", "raw4thSpellCost"
    };
    private static final String[] SPELL_COST_SLOTS_PCT = {
        "1stSpellCost", "2ndSpellCost", "3rdSpellCost", "4thSpellCost"
    };

    private static final Map<String, String> OVERRIDES = new HashMap<>();

    static {
        // Anzeigename weicht von camelCase ab
        OVERRIDES.put("health", "rawHealth");
        // XP Bonus: Tooltip zeigt "XP Bonus", DB-Key ist "combatExperience"
        OVERRIDES.put("xp bonus", "combatExperience");
        OVERRIDES.put("combat experience", "combatExperience");
        // Loot
        OVERRIDES.put("loot bonus", "lootBonus");
        OVERRIDES.put("loot", "lootBonus");
        OVERRIDES.put("loot quality", "lootQuality");
        // Sonstige
        OVERRIDES.put("soul point regen", "soulPointRegen");
        OVERRIDES.put("gather xp bonus", "gatherXpBonus");
        OVERRIDES.put("gather speed", "gatherSpeed");
        OVERRIDES.put("leveled xp bonus", "leveledXpBonus");
        OVERRIDES.put("leveled loot bonus", "leveledLootBonus");
        OVERRIDES.put("1st spell cost", "1stSpellCost");
        OVERRIDES.put("2nd spell cost", "2ndSpellCost");
        OVERRIDES.put("3rd spell cost", "3rdSpellCost");
        OVERRIDES.put("4th spell cost", "4thSpellCost");
        OVERRIDES.put("attack speed", "rawAttackSpeed");
        OVERRIDES.put("critical damage", "criticalDamageBonus");
        OVERRIDES.put("critical hit damage", "criticalDamageBonus");
        OVERRIDES.put("max mana", "rawMaxMana");
        OVERRIDES.put("maximum mana", "rawMaxMana");
        OVERRIDES.put("main attack range", "mainAttackRange");
        OVERRIDES.put("elemental damage", "elementalDamage");
        OVERRIDES.put("elemental defence", "elementalDefence");
        OVERRIDES.put("neutral damage", "neutralDamage");
        OVERRIDES.put("damage", "damage");
        OVERRIDES.put("healing efficiency", "healingEfficiency");
        OVERRIDES.put("health regen", "healthRegen");
        OVERRIDES.put("walk speed", "walkSpeed");
        OVERRIDES.put("sprint regen", "sprintRegen");
        // Element-Defences
        OVERRIDES.put("fire defence", "fireDefence");
        OVERRIDES.put("water defence", "waterDefence");
        OVERRIDES.put("air defence", "airDefence");
        OVERRIDES.put("thunder defence", "thunderDefence");
        OVERRIDES.put("earth defence", "earthDefence");
        // Spell Costs — alle zeigen auf "raw"-Variante; % vs flat wird in resolve() über Unit gesteuert.
        // Mage / Dark Wizard spells
        OVERRIDES.put("heal cost",         "raw1stSpellCost");
        OVERRIDES.put("teleport cost",     "raw2ndSpellCost");
        OVERRIDES.put("meteor cost",       "raw3rdSpellCost");
        OVERRIDES.put("ice snake cost",    "raw4thSpellCost");
        // Warrior / Knight spells
        OVERRIDES.put("bash cost",         "raw1stSpellCost");
        OVERRIDES.put("charge cost",       "raw2ndSpellCost");
        OVERRIDES.put("uppercut cost",     "raw3rdSpellCost");
        OVERRIDES.put("war scream cost",   "raw4thSpellCost");
        // Archer / Hunter spells
        OVERRIDES.put("arrow storm cost",  "raw1stSpellCost");
        OVERRIDES.put("escape cost",       "raw2ndSpellCost");
        OVERRIDES.put("arrow bomb cost",   "raw3rdSpellCost");
        OVERRIDES.put("arrow shield cost", "raw4thSpellCost");
        // Assassin / Ninja spells
        OVERRIDES.put("spin attack cost",  "raw1stSpellCost");
        OVERRIDES.put("multihit cost",     "raw2ndSpellCost");
        OVERRIDES.put("vanish cost",       "raw3rdSpellCost");
        OVERRIDES.put("dash cost",         "raw3rdSpellCost");  // Ninja-Variante von Vanish
        OVERRIDES.put("smoke bomb cost",   "raw4thSpellCost");
        // Shaman / Skyseer spells
        OVERRIDES.put("totem cost",        "raw1stSpellCost");
        OVERRIDES.put("haul cost",         "raw2ndSpellCost");
        OVERRIDES.put("aura cost",         "raw3rdSpellCost");
        OVERRIDES.put("uproot cost",       "raw4thSpellCost");
    }

    public static IdentificationData resolve(StatEntry stat, ItemData itemData) {
        if (stat == null || itemData == null || itemData.identifications.isEmpty()) {
            return null;
        }

        String displayName = stat.displayName;
        boolean isPercent = stat.unit == StatEntry.Unit.PERCENT;
        String lower = displayName.toLowerCase();

        // 1. Hardcoded Override (inkl. Spell-Cost-Namen aller Klassen)
        if (OVERRIDES.containsKey(lower)) {
            String key = OVERRIDES.get(lower);
            if (isPercent && key.startsWith("raw")) {
                // PERCENT-Einheit → non-raw Variante zuerst (z.B. 1stSpellCost statt raw1stSpellCost)
                String nonRaw = key.substring(3, 4).toLowerCase() + key.substring(4);
                IdentificationData found = tryKey(nonRaw, itemData);
                if (found != null) return found;
            } else if (!isPercent && !key.startsWith("raw")) {
                // FLAT-Einheit → "Raw"-Varianten zuerst versuchen (z.B. healthRegenRaw / rawHealthRegen)
                // verhindert dass "Health Regen" (flat) fälschlich auf healthRegen (%) gemappt wird
                IdentificationData found = tryKey(key + "Raw", itemData);
                if (found != null) return found;
                found = tryKey("raw" + capitalize(key), itemData);
                if (found != null) return found;
            }
            IdentificationData found = tryKey(key, itemData);
            if (found != null) return found;
            // Gegenteil: ohne "raw"-Prefix / mit Suffix versuchen
            if (key.startsWith("raw")) {
                found = tryKey(key.substring(3, 4).toLowerCase() + key.substring(4), itemData);
                if (found != null) return found;
            } else {
                found = tryKey("raw" + capitalize(key), itemData);
                if (found != null) return found;
            }
        }

        // 2. Spell-Cost-Fallback: unbekannte "X Cost" Stats per Wert-Matching auflösen
        //    Findet den richtigen Slot auch wenn der Spell-Name unbekannt ist.
        if (lower.endsWith(" cost")) {
            IdentificationData found = resolveSpellCostByValue(stat.value, isPercent, itemData);
            if (found != null) return found;
        }

        // 3. camelCase aus Display-Name
        String camel = toCamelCase(displayName);

        if (isPercent) {
            IdentificationData found = tryKey(camel, itemData);
            if (found != null) return found;
            found = tryKey("raw" + capitalize(camel), itemData);
            if (found != null) return found;
        } else {
            IdentificationData found = tryKey("raw" + capitalize(camel), itemData);
            if (found != null) return found;
            found = tryKey(camel, itemData);
            if (found != null) return found;
        }

        // Letzter Fallback: camelCase + "Raw"-Suffix (z.B. "healthRegenRaw")
        // Wynncraft-API nutzt bei manchen Stats den Suffix statt des Prefix
        IdentificationData suffixFound = tryKey(camel + "Raw", itemData);
        if (suffixFound != null) return suffixFound;

        return null;
    }

    /**
     * Findet den Spell-Cost-Slot in dessen [min..max]-Range der Tooltip-Wert fällt.
     * isPercent=true → prüft erst PCT-Slots, dann RAW als Fallback.
     * isPercent=false → prüft erst RAW-Slots, dann PCT als Fallback.
     * Range-Check: actualValue muss innerhalb [min..max] liegen.
     * Fallback: gibt den ersten vorhandenen Slot des bevorzugten Typs zurück.
     */
    private static IdentificationData resolveSpellCostByValue(double actualValue, boolean isPercent, ItemData itemData) {
        String[] preferred = isPercent ? SPELL_COST_SLOTS_PCT : SPELL_COST_SLOTS_RAW;
        String[] fallback  = isPercent ? SPELL_COST_SLOTS_RAW : SPELL_COST_SLOTS_PCT;

        IdentificationData firstFound = null;
        // Erst bevorzugten Typ prüfen
        for (String slot : preferred) {
            IdentificationData id = itemData.identifications.get(slot);
            if (id == null) continue;
            if (firstFound == null) firstFound = id;
            double lo = Math.min(id.min, id.max);
            double hi = Math.max(id.min, id.max);
            if (actualValue >= lo && actualValue <= hi) return id;
        }
        // Dann Fallback-Typ
        for (String slot : fallback) {
            IdentificationData id = itemData.identifications.get(slot);
            if (id == null) continue;
            if (firstFound == null) firstFound = id;
            double lo = Math.min(id.min, id.max);
            double hi = Math.max(id.min, id.max);
            if (actualValue >= lo && actualValue <= hi) return id;
        }
        return firstFound;
    }

    public static String toCamelCase(String displayName) {
        if (displayName == null || displayName.isBlank()) return "";
        String[] words = displayName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            if (i == 0) {
                sb.append(word.substring(0, 1).toLowerCase());
                if (word.length() > 1) sb.append(word.substring(1));
            } else {
                sb.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    private static IdentificationData tryKey(String key, ItemData itemData) {
        return itemData.identifications.get(key);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
