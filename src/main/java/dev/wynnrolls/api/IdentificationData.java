package dev.wynnrolls.api;

/**
 * Repräsentiert eine rollbare Identification aus der Item-Datenbank.
 * Pre-identified Stats (nur int in JSON) werden separat gehandhabt.
 */
public class IdentificationData {

    public final int min;
    public final int max;
    public final int raw;

    public IdentificationData(int min, int max, int raw) {
        this.min = min;
        this.max = max;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "range=[" + min + ".." + max + "], base=" + raw;
    }
}
