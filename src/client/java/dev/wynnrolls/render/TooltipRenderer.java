package dev.wynnrolls.render;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Formatiert Roll-Prozente mit stufenloser RGB-Farbinterpolation.
 *
 * Farbstopps:
 *   0%   → #AA0000 (dunkelrot)
 *   25%  → #FF8800 (orange)
 *   50%  → #FFFF00 (gelb)
 *   75%  → #55FF55 (grün)
 *   100% → #00FFCC (hellcyan/türkis)
 */
public class TooltipRenderer {

    // RGB-Farbstopps: Rot → Orange → Gelb → Grün → Türkis → Cyan
    // pct:   0      25      60      80      95       100
    private static final double[] STOP_PCT   = {  0,    25,     60,     80,     95,      100 };
    private static final int[][]  COLOR_STOPS = {
        {0xAA, 0x00, 0x00},  //   0% dunkelrot
        {0xFF, 0x88, 0x00},  //  25% orange
        {0xFF, 0xA0, 0x00},  //  60% amber (G=160, R>200 und G>200 überlappen sich nicht mehr)
        {0x55, 0xFF, 0x55},  //  80% grün
        {0x00, 0xFF, 0xCC},  //  95% türkis
        {0x00, 0xFF, 0xFF},  // 100% cyan
    };

    private static final int GRAY       = 0x555555;
    private static final int WHITE_BOLD = 0xFFFFFF;

    /**
     * " [85.3%]" — Farbe interpoliert, Klammern grau.
     */
    public static Text formatRoll(double rollPercent) {
        if (rollPercent < 0) return Text.empty();
        int color = interpolateColor(rollPercent);
        return Text.literal(" [")
            .setStyle(Style.EMPTY.withColor(GRAY))
            .append(Text.literal(String.format("%.1f%%", rollPercent))
                .setStyle(Style.EMPTY.withColor(color)))
            .append(Text.literal("]")
                .setStyle(Style.EMPTY.withColor(GRAY)));
    }

    /**
     * "Overall: 81.5%" — fett, Farbe interpoliert. Für Tooltip-Kopf.
     */
    public static Text formatOverall(double overallPercent) {
        if (overallPercent < 0) return Text.empty();
        int color = interpolateColor(overallPercent);
        MutableText label = Text.literal("Overall: ")
            .setStyle(Style.EMPTY.withBold(true).withColor(WHITE_BOLD));
        label.append(Text.literal(String.format("%.1f%%", overallPercent))
            .setStyle(Style.EMPTY.withBold(true).withColor(color)));
        return label;
    }

    /**
     * "Defective Overall: 0.0%" — fett, animiert pulsierendes Rot.
     * Da der Tooltip jeden Frame neu aufgebaut wird, ändert sich die Farbe automatisch.
     */
    public static Text formatDefective() {
        long t = System.currentTimeMillis();
        double pulse = (Math.sin(t / 250.0) + 1.0) / 2.0; // 0..1, ~4x/s
        int r = (int) (0x77 + pulse * (0xFF - 0x77));
        int color = (r << 16); // pulsierendes Rot, kein Grün/Blau
        return Text.literal("Defective Overall: 0.0%")
            .setStyle(Style.EMPTY.withBold(true).withColor(color));
    }

    /**
     * Zyklischer Farbverlauf für die Regenbogenanimation (0–100 = ein voller Loop).
     * Rot → Orange → Gelb → Grün → Türkis → Cyan → Blau → Violett → Dunkelrot
     * Übergang Cyan→Rot läuft weich über Blau/Violett.
     */
    private static final double[] RAINBOW_PCT   = {  0,    16,     38,     50,     60,     67,     75,     83,     92,    100 };
    private static final int[][]  RAINBOW_STOPS = {
        {0xAA, 0x00, 0x00},  //   0% dunkelrot
        {0xFF, 0x88, 0x00},  //  16% orange
        {0xFF, 0xD0, 0x00},  //  38% goldgelb
        {0x55, 0xFF, 0x55},  //  50% grün
        {0x00, 0xFF, 0xCC},  //  60% türkis
        {0x00, 0xFF, 0xFF},  //  67% cyan
        {0x00, 0x88, 0xFF},  //  75% blau
        {0x88, 0x00, 0xFF},  //  83% violett
        {0xCC, 0x00, 0x44},  //  92% dunkelmagenta
        {0xAA, 0x00, 0x00},  // 100% dunkelrot (= 0%, Loop geschlossen)
    };

    private static int interpolateRainbow(double pct) {
        pct = ((pct % 100.0) + 100.0) % 100.0;
        int idx = 0;
        for (int i = 0; i < RAINBOW_PCT.length - 2; i++) {
            if (pct >= RAINBOW_PCT[i + 1]) idx = i + 1;
        }
        double segLen = RAINBOW_PCT[idx + 1] - RAINBOW_PCT[idx];
        double t = (pct - RAINBOW_PCT[idx]) / segLen;
        int[] from = RAINBOW_STOPS[idx];
        int[] to   = RAINBOW_STOPS[idx + 1];
        int r = (int) Math.round(from[0] + t * (to[0] - from[0]));
        int g = (int) Math.round(from[1] + t * (to[1] - from[1]));
        int b = (int) Math.round(from[2] + t * (to[2] - from[2]));
        return (r << 16) | (g << 8) | b;
    }

    /**
     * "Perfect Overall: 100.0%" — fett, animierter Regenbogen mit weichem Loop.
     */
    public static Text formatPerfect() {
        String text = "Perfect Overall: 100.0%";
        long t = System.currentTimeMillis();
        double baseOffset = (t % 3000) / 30.0; // 0..100 in 3s
        MutableText result = null;
        for (int i = 0; i < text.length(); i++) {
            double pct = baseOffset + (double) i / (text.length() - 1) * 100.0;
            int color = interpolateRainbow(pct);
            MutableText ch = Text.literal(String.valueOf(text.charAt(i)))
                .setStyle(Style.EMPTY.withBold(true).withColor(color));
            if (result == null) result = ch;
            else result.append(ch);
        }
        return result != null ? result : Text.empty();
    }

    /**
     * "[WynnPool] Main: 85.1%" — Quelle grau, Skalenname weiß, Prozent farbcodiert.
     */
    public static Text formatScale(String source, String scaleName, double scorePercent) {
        if (scorePercent < 0) return Text.empty();
        int color = interpolateColor(scorePercent);
        String displaySource = formatSource(source);
        MutableText line = Text.literal("[" + displaySource + "] ")
            .setStyle(Style.EMPTY.withColor(GRAY));
        line.append(Text.literal(scaleName + ": ")
            .setStyle(Style.EMPTY.withColor(WHITE_BOLD)));
        line.append(Text.literal(String.format("%.1f%%", scorePercent))
            .setStyle(Style.EMPTY.withColor(color)));
        return line;
    }

    private static String formatSource(String source) {
        return switch (source.toLowerCase()) {
            case "wynnpool" -> "WynnPool";
            case "nori"     -> "Nori";
            default         -> source;
        };
    }

    /**
     * Interpoliert eine RGB-Farbe stufenlos zwischen den Farbstopps.
     */
    public static int interpolateColor(double pct) {
        pct = Math.max(0, Math.min(100, pct));

        int idx = 0;
        for (int i = 0; i < STOP_PCT.length - 2; i++) {
            if (pct >= STOP_PCT[i + 1]) idx = i + 1;
        }

        double segLen = STOP_PCT[idx + 1] - STOP_PCT[idx];
        double t = (pct - STOP_PCT[idx]) / segLen;

        int[] from = COLOR_STOPS[idx];
        int[] to   = COLOR_STOPS[idx + 1];

        int r = (int) Math.round(from[0] + t * (to[0] - from[0]));
        int g = (int) Math.round(from[1] + t * (to[1] - from[1]));
        int b = (int) Math.round(from[2] + t * (to[2] - from[2]));

        return (r << 16) | (g << 8) | b;
    }

}
