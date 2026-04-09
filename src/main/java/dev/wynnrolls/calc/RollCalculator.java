package dev.wynnrolls.calc;

import dev.wynnrolls.api.IdentificationData;
import dev.wynnrolls.tooltip.StatEntry;

/**
 * Berechnet Roll-Prozente für einzelne Stats.
 *
 * Formel (aus CLAUDE.md):
 * - Positive Stats: range = [base * 0.3 .. base * 1.3]
 * - Negative Stats: range = [base * 0.7 .. base * 1.3]  (aber in gear_expanded ist min/max schon korrekt)
 * - Roll% = (actual - min) / (max - min) * 100, clamped 0–100
 *
 * Da gear_expanded.json bereits korrekte min/max-Werte enthält,
 * nutzen wir diese direkt ohne nochmals die Formel anzuwenden.
 *
 * Negative Stats (wo niedriger = besser): Roll% wird invertiert.
 */
public class RollCalculator {

    /**
     * Berechnet den Roll-Prozentsatz für einen Stat.
     *
     * @param stat    Geparster Stat aus dem Tooltip (enthält actual value + isNegative)
     * @param idData  Identification-Daten aus der Datenbank (min/max/raw)
     * @return        Roll-% zwischen 0.0 und 100.0, oder -1 wenn nicht berechenbar
     */
    public static double calculate(StatEntry stat, IdentificationData idData) {
        if (idData == null) return -1;

        double actual = stat.value;
        double min = idData.min;
        double max = idData.max;

        // Sicherheit: wenn Bereich 0 ist, kein sinnvoller Roll berechenbar
        if (max == min) return -1;

        // gear_expanded Konvention: min = schlechtester Roll (0%), max = bester Roll (100%)
        // Auch bei Spell Costs wo min > max numerisch (min=-1, max=-4 → -1 ist worst, -4 ist best)
        double roll = (actual - min) / (max - min) * 100.0;

        // Clamp auf 0–100
        return Math.max(0.0, Math.min(100.0, roll));
    }

}
