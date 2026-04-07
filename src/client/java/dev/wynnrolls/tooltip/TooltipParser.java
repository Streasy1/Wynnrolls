package dev.wynnrolls.tooltip;

import dev.wynnrolls.util.DebugLogger;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Wynncraft item tooltips (Fruma format, April 2026+).
 *
 * Fruma tooltip format changes vs. pre-Fruma:
 * - Rarity/type is NO LONGER plain text — encoded as Custom Font (PUA) characters only.
 * - Stat lines no longer use a colon separator.
 *   New format: "StatName<PUA_chars>+/-Value<unit> <bar_PUA_chars>"
 *   Example raw: "Mana Steal󏿑󐁰+5/3s [U+E023]󏿷[U+E023]"
 * - Item name in L0 is wrapped by Supplementary PUA chars: 󏀀ItemName󏀀
 * - All decorative/indicator chars are Supplementary PUA (surrogate pairs in Java).
 */
public class TooltipParser {

    // Removes Minecraft §-color and formatting codes
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    // Removes BMP Private Use Area chars (U+E000–U+F8FF, used by Wynncraft custom font)
    private static final Pattern BMP_PUA_PATTERN = Pattern.compile("[\\uE000-\\uF8FF]");

    // Removes Unicode surrogate halves (D800–DFFF) which encode Supplementary PUA chars
    // (Wynncraft uses plane-15/16 PUA for roll bars, rarity icons, etc.)
    private static final Pattern SURROGATE_PATTERN = Pattern.compile("[\\uD800-\\uDFFF]");

    /**
     * Fruma stat line pattern (applied AFTER stripping PUA + surrogates):
     * Group 1: stat name (letters + spaces, must start with letter)
     * Group 2: signed numeric value (may contain comma thousands-separator)
     * Group 3: unit (%, /3s, /5s, tier) — optional
     *
     * After stripping, a line like "Mana Steal+5/3s" or "Walk Speed-27% " is produced.
     */
    private static final Pattern STAT_LINE_PATTERN = Pattern.compile(
        "^([A-Za-z][A-Za-z0-9 ]*?)\\s*([+-]\\d[\\d,]*(?:\\.\\d+)?)(/%|/3s|/5s|%| tier)?.*$"
    );

    // Stats where a lower value is better
    private static final List<String> NEGATIVE_STATS = List.of(
        "heal cost", "teleport cost", "meteor cost", "ice snake cost",
        "spell cost", "spell cost 1st", "spell cost 2nd",
        "spell cost 3rd", "spell cost 4th", "jump height",
        "healing efficiency"
    );

    // Skill-point requirement stat names — these are NOT identifications, skip them
    private static final List<String> SKILL_REQS = List.of(
        "strength", "dexterity", "intelligence", "defence", "agility"
    );

    public record ParseResult(
        String itemName,
        String rarity,   // "WYNNCRAFT" (actual rarity comes from API)
        String itemType, // "UNKNOWN" until API lookup
        List<StatEntry> stats
    ) {}

    /**
     * Parses a full tooltip into a ParseResult.
     * Returns null if this doesn't look like a Wynncraft item.
     */
    public static ParseResult parse(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) return null;

        List<String> lines = tooltip.stream()
            .map(Text::getString)
            .toList();

        // Wynncraft items have Supplementary PUA chars (surrogate pairs) in L0.
        // Non-Wynncraft items (vanilla, other mods) won't have these.
        String l0 = lines.get(0);
        if (!containsPUA(l0)) return null;

        // Extract item name: strip all PUA + colors from L0
        String itemName = stripAll(l0).trim();
        if (itemName.isBlank()) return null;

        List<StatEntry> stats = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            String stripped = stripAll(raw).trim();
            if (stripped.isBlank()) continue;

            // Unidentified items (pre-Fruma): enthält Wort "Unidentified"
            if (stripped.equalsIgnoreCase("Unidentified")
                    || raw.toLowerCase().contains("unidentified")) return null;

            // Unidentified items (Fruma): zeigt Range-Format "StatName+min to +max"
            // z.B. "Max Mana+24 to +104" oder "Knockback-195% to -105%"
            if (stripped.contains(" to +") || stripped.contains(" to -")) return null;

            StatEntry entry = tryParseStat(stripped, raw, i);
            if (entry != null) {
                stats.add(entry);
            }
        }

        return new ParseResult(itemName, "WYNNCRAFT", "UNKNOWN", stats);
    }

    /**
     * Returns true if the string contains BMP PUA or surrogate chars
     * (= Wynncraft custom font usage).
     */
    private static boolean containsPUA(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0xE000 && c <= 0xF8FF) || Character.isSurrogate(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Strips §-color codes, BMP PUA chars (U+E000–U+F8FF), and Supplementary PUA chars
     * (encoded as surrogate pairs in Java's UTF-16 strings) from a string.
     *
     * NOTE: Java's regex engine does NOT reliably remove surrogate chars via replaceAll —
     * so we process character-by-character instead.
     */
    private static String stripAll(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                // §X color code — skip both chars
                i += 2;
            } else if (c >= 0xE000 && c <= 0xF8FF) {
                // BMP Private Use Area — skip
                i++;
            } else if (Character.isHighSurrogate(c) && i + 1 < text.length()
                    && Character.isLowSurrogate(text.charAt(i + 1))) {
                // Supplementary character (Surrogate pair) = Wynncraft custom font — skip both
                i += 2;
            } else if (Character.isLowSurrogate(c)) {
                // Orphaned low surrogate — skip
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static StatEntry tryParseStat(String strippedLine, String rawLine, int lineIndex) {
        Matcher m = STAT_LINE_PATTERN.matcher(strippedLine);
        if (!m.matches()) return null;

        String name = m.group(1).trim();
        String valueStr = m.group(2).replace(",", ""); // remove thousand separators
        String unitStr = m.group(3);

        // Skip skill-point requirement lines (Strength+12 etc. are reqs, not identifications)
        if (SKILL_REQS.contains(name.toLowerCase())) return null;

        // Skip obvious non-stat lines
        if (name.equalsIgnoreCase("Combat Level") ||
            name.equalsIgnoreCase("Quest") ||
            name.equalsIgnoreCase("Elemental Defences") ||
            name.equalsIgnoreCase("Elemental Damages")) {
            return null;
        }

        // Parse value
        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return null;
        }

        // Determine unit
        StatEntry.Unit unit;
        if (unitStr == null || unitStr.isBlank()) {
            unit = StatEntry.Unit.FLAT;
        } else {
            unit = switch (unitStr.trim()) {
                case "%" -> StatEntry.Unit.PERCENT;
                case "/3s" -> StatEntry.Unit.PER_3S;
                case "/5s" -> StatEntry.Unit.PER_5S;
                case "tier" -> StatEntry.Unit.TIER;
                default -> StatEntry.Unit.FLAT;
            };
        }

        // Determine if negative stat
        boolean isNegative = NEGATIVE_STATS.contains(name.toLowerCase()) || value < 0;

        return new StatEntry(name, value, unit, isNegative, rawLine, lineIndex);
    }

    public static String stripColors(String text) {
        return COLOR_CODE_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Logs all raw tooltip lines with unicode codepoints visible.
     * Call once per unique item (before parse) to inspect the actual tooltip format.
     */
    public static void logRawLines(List<Text> tooltip) {
        DebugLogger.log("=== RAW TOOLTIP (" + tooltip.size() + " lines) ===");
        for (int i = 0; i < tooltip.size(); i++) {
            String raw = tooltip.get(i).getString();
            StringBuilder sb = new StringBuilder();
            for (char c : raw.toCharArray()) {
                if (c < 32 || (c > 126 && c < 160) || (c >= 0xE000 && c <= 0xF8FF) || Character.isSurrogate(c)) {
                    sb.append(String.format("[U+%04X]", (int) c));
                } else {
                    sb.append(c);
                }
            }
            DebugLogger.log("  L" + i + ": [" + sb + "]");
        }
        DebugLogger.log("=== END RAW ===");
    }

    /**
     * Logs a ParseResult in the expected debug format.
     */
    public static void logParseResult(ParseResult result) {
        DebugLogger.log("=== Item erkannt: " + result.itemName() + " ===");
        if (result.stats().isEmpty()) {
            DebugLogger.log("  (keine Stats geparst)");
        } else {
            for (StatEntry stat : result.stats()) {
                DebugLogger.log("  " + stat.toString());
            }
        }
        DebugLogger.log("=== Ende ===");
    }
}
