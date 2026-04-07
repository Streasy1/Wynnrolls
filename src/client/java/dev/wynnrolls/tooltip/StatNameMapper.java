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

    // Alle möglichen Spell-Cost-Slots (raw zuerst, da Spell Costs meist flat sind)
    private static final String[] SPELL_COST_SLOTS = {
        "raw1stSpellCost", "raw2ndSpellCost", "raw3rdSpellCost", "raw4thSpellCost",
        "1stSpellCost",    "2ndSpellCost",    "3rdSpellCost",    "4thSpellCost"
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
        // Mage spells
        OVERRIDES.put("heal cost",       "raw1stSpellCost");
        OVERRIDES.put("teleport cost",   "raw2ndSpellCost");
        OVERRIDES.put("meteor cost",     "raw3rdSpellCost");
        OVERRIDES.put("ice snake cost",  "raw4thSpellCost");
        // Warrior spells
        OVERRIDES.put("bash cost",       "raw1stSpellCost");
        OVERRIDES.put("charge cost",     "raw2ndSpellCost");
        OVERRIDES.put("war cry cost",    "raw3rdSpellCost");
        OVERRIDES.put("uppercut cost",   "raw4thSpellCost");
        OVERRIDES.put("haul cost",       "raw4thSpellCost");
        // Archer spells
        OVERRIDES.put("arrow storm cost",  "raw1stSpellCost");
        OVERRIDES.put("escape cost",       "raw2ndSpellCost");
        OVERRIDES.put("arrow shield cost", "raw3rdSpellCost");
        OVERRIDES.put("arrow rain cost",   "raw4thSpellCost");
        // Assassin spells
        OVERRIDES.put("spin attack cost",    "raw1stSpellCost");
        OVERRIDES.put("vanish cost",         "raw2ndSpellCost");
        OVERRIDES.put("smoke bomb cost",     "raw3rdSpellCost");
        OVERRIDES.put("mortal decision cost","raw4thSpellCost");
        // Shaman spells
        OVERRIDES.put("totem cost",      "raw1stSpellCost");
        OVERRIDES.put("haul cost",       "raw2ndSpellCost");
        OVERRIDES.put("war scream cost", "raw3rdSpellCost");
        OVERRIDES.put("uproot cost",     "raw4thSpellCost");
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
            IdentificationData found = tryKey(key, itemData);
            if (found != null) return found;
            // Fallback: ohne "raw"-Prefix versuchen
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
            IdentificationData found = resolveSpellCostByValue(stat.value, itemData);
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
     * Findet den Spell-Cost-Slot dessen raw-Wert mit dem tatsächlichen Tooltip-Wert übereinstimmt.
     * Fallback: gibt den ersten vorhandenen Spell-Cost-Slot zurück.
     */
    private static IdentificationData resolveSpellCostByValue(double actualValue, ItemData itemData) {
        IdentificationData firstFound = null;
        for (String slot : SPELL_COST_SLOTS) {
            IdentificationData id = itemData.identifications.get(slot);
            if (id == null) continue;
            if (firstFound == null) firstFound = id;
            if (id.raw == (int) actualValue) return id;
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
