package dev.wynnrolls.api;

import java.util.Collections;
import java.util.Map;

/**
 * Datenklasse für ein Wynncraft-Item aus der gebündelten Datenbank (items.json).
 * Enthält nur die für Roll-Berechnung relevanten Felder.
 */
public class ItemData {

    public final String name;
    public final String tier;   // unique, rare, legendary, fabled, mythic, set, crafted
    public final String type;   // weapon, armour, accessory
    public final String subType;

    /**
     * Rollbare Identifications: API-Key → {min, max, raw}.
     * Pre-identified Stats (nur int im JSON) sind hier NICHT enthalten.
     */
    public final Map<String, IdentificationData> identifications;

    public ItemData(String name, String tier, String type, String subType,
                    Map<String, IdentificationData> identifications) {
        this.name = name;
        this.tier = tier;
        this.type = type;
        this.subType = subType;
        this.identifications = Collections.unmodifiableMap(identifications);
    }
}
