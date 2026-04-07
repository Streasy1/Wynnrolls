package dev.wynnrolls.tooltip;

import dev.wynnrolls.api.IdentificationData;
import dev.wynnrolls.api.ItemData;
import dev.wynnrolls.api.ItemDatabase;
import dev.wynnrolls.api.ItemWeightDatabase;
import dev.wynnrolls.calc.OverallScore;
import dev.wynnrolls.calc.RollCalculator;
import dev.wynnrolls.render.TooltipRenderer;
import dev.wynnrolls.util.DebugLogger;
import dev.wynnrolls.util.ServerDetector;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registriert den Tooltip-Callback.
 *
 * Tooltip-Listen werden jeden Frame neu aufgebaut — deshalb muss die Injektion
 * JEDEN Frame passieren. Nur das Logging ist gedrosselt (einmal pro Item).
 *
 * Ablauf pro Frame:
 * 1. Parse → item name
 * 2. DB-Lookup → ItemData (billiger HashMap-Lookup, ok jeden Frame)
 * 3. Roll-Berechnung → List<Double>
 * 4. Visuell in lines einfügen
 */
public class TooltipInjector {

    // Throttle NUR für Logging
    private static String lastLoggedItem = null;

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!ServerDetector.isOnWynncraft()) return;
            if (lines == null || lines.isEmpty()) return;

            // Parse Tooltip
            TooltipParser.ParseResult result = TooltipParser.parse(lines);
            if (result == null) return;

            // DB-Lookup — "Shiny X" Prefix entfernen (Shiny Items haben gleiche Stats wie Original)
            String lookupName = result.itemName();
            if (lookupName.startsWith("Shiny ")) {
                lookupName = lookupName.substring(6);
            }
            final String finalLookupName = lookupName;
            ItemData itemData = ItemDatabase.get(lookupName);
            if (itemData == null) {
                logOnce(result.itemName(), () -> {
                    DebugLogger.log("API: " + result.itemName() + " → NICHT IN DATENBANK");
                    TooltipParser.logParseResult(result);
                });
                return;
            }

            // Roll-Berechnung
            List<Double> rolls = new ArrayList<>();
            Map<String, Double> rollsByApiKey = new HashMap<>();

            for (StatEntry stat : result.stats()) {
                IdentificationData idData = StatNameMapper.resolve(stat, itemData);
                double roll = RollCalculator.calculate(stat, idData);
                rolls.add(roll);

                // API-Key rückverfolgen für Skalen-Berechnung
                if (roll >= 0 && idData != null) {
                    String apiKey = resolveApiKey(stat, itemData, idData);
                    if (apiKey != null) {
                        rollsByApiKey.put(apiKey, roll);
                    }
                }
            }

            double overall = OverallScore.calculateAverage(rolls);
            List<ItemWeightDatabase.ScaleEntry> scales =
                ItemWeightDatabase.getScales(finalLookupName);

            // Defective / Perfect erkennen
            long validCount = rolls.stream().filter(r -> r >= 0).count();
            boolean isDefective = validCount >= 1 && rolls.stream().filter(r -> r >= 0).allMatch(r -> r == 0.0);
            boolean isPerfect   = validCount >= 1 && rolls.stream().filter(r -> r >= 0).allMatch(r -> r == 100.0);

            // Einmaliges Logging
            logOnce(result.itemName(), () -> {
                DebugLogger.log("API: " + result.itemName()
                    + " → GEFUNDEN (" + itemData.tier + " " + itemData.type + ")");
                List<StatEntry> stats = result.stats();
                for (int i = 0; i < stats.size(); i++) {
                    StatEntry stat = stats.get(i);
                    IdentificationData idData = StatNameMapper.resolve(stat, itemData);
                    double roll = rolls.get(i);
                    if (roll >= 0) {
                        DebugLogger.log("  " + stat.displayName + ": " + stat.value
                            + " in [" + idData.min + ".." + idData.max + "] = "
                            + String.format("%.1f%%", roll));
                    } else {
                        DebugLogger.log("  " + stat.displayName + ": KEIN MATCH in DB");
                    }
                }
                if (overall >= 0) {
                    DebugLogger.log("Overall: " + String.format("%.1f%%", overall));
                }
                if (!scales.isEmpty()) {
                    DebugLogger.log("Skalen (" + scales.size() + "):");
                    for (ItemWeightDatabase.ScaleEntry scale : scales) {
                        double score = computeWeightedScore(rollsByApiKey, scale);
                        DebugLogger.log("  [" + scale.source() + "] " + scale.scaleName()
                            + ": " + (score >= 0 ? String.format("%.1f%%", score) : "N/A"));
                    }
                }
            });

            // Visuell injizieren — JEDEN Frame (Liste wird immer neu aufgebaut)
            injectRolls(lines, result.stats(), rolls, overall, scales, rollsByApiKey, isDefective, isPerfect);
        });

        DebugLogger.log("TooltipInjector registered.");
    }

    /**
     * Hängt Roll-%-Labels an die entsprechenden Stat-Zeilen an
     * und fügt Overall Score + Skalen oben ein.
     */
    private static void injectRolls(List<Text> lines,
                                     List<StatEntry> stats,
                                     List<Double> rolls,
                                     double overall,
                                     List<ItemWeightDatabase.ScaleEntry> scales,
                                     Map<String, Double> rollsByApiKey,
                                     boolean isDefective,
                                     boolean isPerfect) {
        // Stat-Roll-Labels via stripped Content-Matching.
        // Vergleich nach PUA-Strip damit animierte Wynncraft-Balken (die sich frame-zu-frame
        // ändern) den Match nicht verhindern.
        int lastStatLineIdx = -1;
        for (int s = 0; s < stats.size(); s++) {
            double roll = rolls.get(s);
            StatEntry stat = stats.get(s);
            String strippedTarget = stripPUA(stat.rawLine);
            for (int i = 0; i < lines.size(); i++) {
                if (stripPUA(lines.get(i).getString()).equals(strippedTarget)) {
                    if (roll >= 0) {
                        MutableText newLine = lines.get(i).copy();
                        newLine.append(TooltipRenderer.formatRoll(roll));
                        lines.set(i, newLine);
                    }
                    if (i > lastStatLineIdx) lastStatLineIdx = i;
                    break;
                }
            }
        }

        // Overall-Block nach der ERSTEN Leerzeile einfügen (stabile Position, vor animated Bars).
        // "Nach letzter Stat-Zeile" springt bei Items mit animierten Wynncraft-Balken.
        int insertAt = findBlankInsertAt(lines);

        // Skalen zusammenbauen
        List<Text> scaleLines = new ArrayList<>();
        for (ItemWeightDatabase.ScaleEntry scale : scales) {
            double score = computeWeightedScore(rollsByApiKey, scale);
            if (score >= 0) {
                scaleLines.add(TooltipRenderer.formatScale(scale.source(), scale.scaleName(), score));
            }
        }

        // Leere Trennzeile nach unserem Block (Abstand zu den Stats)
        lines.add(insertAt, Text.empty());

        // Skalen rückwärts einfügen (Reihenfolge bleibt erhalten)
        for (int i = scaleLines.size() - 1; i >= 0; i--) {
            lines.add(insertAt, scaleLines.get(i));
        }

        // Overall ganz oben unseres Blocks
        if (overall >= 0) {
            Text overallLine;
            if (isDefective) overallLine = TooltipRenderer.formatDefective();
            else if (isPerfect) overallLine = TooltipRenderer.formatPerfect();
            else overallLine = TooltipRenderer.formatOverall(overall);
            lines.add(insertAt, overallLine);
        }
    }

    /** Entfernt PUA- und Surrogate-Zeichen (animierte Wynncraft-Chars) für stabilen Vergleich. */
    private static String stripPUA(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) { i += 2; }
            else if (c >= 0xE000 && c <= 0xF8FF) { i++; }
            else if (Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i+1))) { i += 2; }
            else if (Character.isLowSurrogate(c)) { i++; }
            else { sb.append(c); i++; }
        }
        return sb.toString();
    }

    /** Fallback-Einfügeposition: nach der ersten Leerzeile in den ersten 10 Zeilen. */
    private static int findBlankInsertAt(List<Text> lines) {
        for (int i = 1; i < Math.min(lines.size(), 10); i++) {
            if (lines.get(i).getString().isBlank()) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Berechnet den gewichteten Score für eine Skala.
     * Formel aus Wynntils ItemWeightService:
     *   - weight < 0 → Prozent invertieren (100 - pct)
     *   - weightedSum / sumWeights
     */
    private static double computeWeightedScore(Map<String, Double> rollsByApiKey,
                                                ItemWeightDatabase.ScaleEntry scale) {
        double weightedSum = 0;
        double sumWeights = 0;

        for (Map.Entry<String, Double> entry : scale.weights().entrySet()) {
            Double roll = rollsByApiKey.get(entry.getKey());
            if (roll == null || roll < 0) continue;
            double w = entry.getValue();
            double pct = w < 0 ? 100.0 - roll : roll;
            weightedSum += pct * Math.abs(w);
            sumWeights += Math.abs(w);
        }

        return sumWeights > 0 ? weightedSum / sumWeights : -1;
    }

    /**
     * Findet den API-Key für eine StatEntry, indem wir die IdentificationData-Referenz
     * mit dem identifications-Map der ItemData abgleichen.
     */
    private static String resolveApiKey(StatEntry stat, ItemData itemData, IdentificationData idData) {
        for (Map.Entry<String, IdentificationData> entry : itemData.identifications.entrySet()) {
            if (entry.getValue() == idData) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void logOnce(String itemName, Runnable action) {
        if (!itemName.equals(lastLoggedItem)) {
            lastLoggedItem = itemName;
            action.run();
        }
    }

    public static void resetLastLogged() {
        lastLoggedItem = null;
    }
}
