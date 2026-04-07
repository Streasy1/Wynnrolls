package dev.wynnrolls.calc;

import java.util.Collection;

/**
 * Berechnet den Gesamt-Score eines Items aus einzelnen Roll-Prozenten.
 */
public class OverallScore {

    /**
     * Einfacher Durchschnitt aller Roll-Prozente.
     * Werte < 0 (unbekannt/pre-id) werden ignoriert.
     *
     * @return Durchschnitt 0–100, oder -1 wenn keine gültigen Rolls vorhanden
     */
    public static double calculateAverage(Collection<Double> rolls) {
        double sum = 0;
        int count = 0;
        for (double roll : rolls) {
            if (roll >= 0) {
                sum += roll;
                count++;
            }
        }
        return count > 0 ? sum / count : -1;
    }
}
