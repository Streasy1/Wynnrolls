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

        // Bei negativen Stats: min ist negativer als max (z.B. min=-200, max=-108)
        // Der Roll-% gibt an wie nahe wir am "besseren" Ende sind.
        // Für negative Stats: näher an max (weniger negativ) = besser = höherer Roll%
        // Das ist dieselbe Formel — bei negativen Stats ist "höher" bereits besser
        // weil max > min (z.B. -108 > -200).
        double roll = (actual - min) / (max - min) * 100.0;

        // Clamp auf 0–100
        return Math.max(0.0, Math.min(100.0, roll));
    }

    /**
     * Farb-Kategorie für einen Roll-Prozentsatz.
     */
    public static RollTier getTier(double rollPercent) {
        if (rollPercent < 0) return RollTier.UNKNOWN;
        if (rollPercent < 30) return RollTier.TERRIBLE;
        if (rollPercent < 60) return RollTier.BAD;
        if (rollPercent < 80) return RollTier.MEDIUM;
        if (rollPercent < 95) return RollTier.GOOD;
        return RollTier.PERFECT;
    }

    public enum RollTier {
        UNKNOWN,
        TERRIBLE,  // 0–30%
        BAD,       // 30–60%
        MEDIUM,    // 60–80%
        GOOD,      // 80–95%
        PERFECT    // 95–100%
    }
}
