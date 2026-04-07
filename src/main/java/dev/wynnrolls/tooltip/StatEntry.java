package dev.wynnrolls.tooltip;

/**
 * Represents a single parsed identification stat from a Wynncraft item tooltip.
 */
public class StatEntry {

    public enum Unit {
        FLAT,       // e.g. +40
        PERCENT,    // e.g. +32%
        PER_3S,     // e.g. +585/3s
        PER_5S,     // e.g. +X/5s
        TIER        // powder/tier values
    }

    /** Display name as shown in the tooltip, e.g. "Life Steal" */
    public final String displayName;

    /** Numeric value parsed from the tooltip */
    public final double value;

    /** Unit of the stat */
    public final Unit unit;

    /**
     * True if a lower value is better (e.g. "Heal Cost", "Spell Cost", negative stats).
     * These stats are shown with a negative base in the API, or have inverted scoring.
     */
    public final boolean isNegativeStat;

    /** The raw tooltip line (for debugging) */
    public final String rawLine;

    /** Index in the original tooltip List<Text> — used for direct injection without string matching */
    public final int lineIndex;

    public StatEntry(String displayName, double value, Unit unit, boolean isNegativeStat, String rawLine, int lineIndex) {
        this.displayName = displayName;
        this.value = value;
        this.unit = unit;
        this.isNegativeStat = isNegativeStat;
        this.rawLine = rawLine;
        this.lineIndex = lineIndex;
    }

    @Override
    public String toString() {
        String unitStr = switch (unit) {
            case FLAT -> "flat";
            case PERCENT -> "percent";
            case PER_3S -> "per3s";
            case PER_5S -> "per5s";
            case TIER -> "tier";
        };
        return String.format("%s: %+.0f (%s)%s", displayName, value, unitStr, isNegativeStat ? " [negative]" : "");
    }
}
