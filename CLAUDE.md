# WynnRolls — Fabric Mod für Wynncraft Item Roll Prozente

---

# AKTIVER_MODUS: SIMPEL
# Wechsel → ! bash .claude/simpel.sh   (deaktiviert R2/R5/R7/R8-Agents)
#           → ! bash .claude/komplex.sh (volles Regelwerk)
#
# SIMPEL-MODUS:  Kein Plan, kein Leseabsicht, kein Urteils-Agent. Direkt lesen + editieren.
# KOMPLEX-MODUS: Alle Regeln aktiv (Planung, Hooks, Agents, Review).

---

# ██ ABSOLUTE PFLICHTREGELN — KEINE AUSNAHMEN ██

## R1 — FEHLER = STOPP → REVIEW-AGENT
1. SOFORT aufhören. Kein weiterer Edit.
2. Urteils-Agent starten (MODUS: REVIEW, Datei: `AGENT.md`)
3. Schildern: was versucht · welcher Fehler · deine Lösung
4. GENEHMIGT → weiter | ABGELEHNT → User zeigen, warten
Ausnahme: Tippfehler (Syntax, falsche Variable) → still korrigieren.

## R2 — KEIN EDIT OHNE GENEHMIGTEN PLAN
1. Plan in `plan_aktuell.md` (nummerierte Schritte)
2. Urteils-Agent starten (MODUS: PLAN, Datei: `AGENT.md`)
3. GENEHMIGT → `_state/plan_genehmigt` existiert → editieren
4. ABGELEHNT → überarbeiten → erneut
5. Abschluss: `plan_aktuell.md` + `_state/plan_genehmigt` löschen
Hook blockiert. Umgehen verboten.
Ausnahmen: CLAUDE.md · AGENT.md · plan_aktuell.md · _state/*

## R3 — SELF IMPROVEMENT LOOP
Bug durch falsche Annahme → als `feedback_*.md` ins Memory. Immer.

## R4 — AUTO-BUGFIX
Offensichtlicher Bug in derselben Datei → still mitbeheben, kein separater Plan.

## R5 — GREP VOR READ
1. Was suche ich? Aufschreiben.
2. In CLAUDE.md? → Hier lesen, nicht die Datei.
3. Sonst: Grep → nur ±30 Zeilen lesen.
4. Full-Read nur wenn Review-Agent Notwendigkeit bestätigt.
Hook prüft ob `_state/leseabsicht.md` existiert.
Ausnahmen: CLAUDE.md · AGENT.md · plan_aktuell.md · .claude/*

## R6 — CLAUDE.md SOFORT NACH CODE-ÄNDERUNG AKTUALISIEREN
Neue Dateien, Klassen, Methoden, Signale, Abhängigkeiten → sofort hier eintragen.

## R7 — LESEABSICHT VOR ERSTEM READ
`_state/leseabsicht.md`: Was + warum (max. 5 Zeilen). Nach Aufgabe löschen.
Folge-Reads derselben Aufgabe: keine neue Datei nötig.
Ausnahmen: CLAUDE.md · AGENT.md · plan_aktuell.md · .claude/*

## R8 — ABSCHLUSS-PFLICHT
Alle `- [ ]` → `- [x]` · CLAUDE.md aktualisiert · kurzer Überblick
→ erst dann "abgeschlossen" sagen.

## URTEILS-AGENT STARTEN
```
Agent tool → subagent_type: "general-purpose"
Prompt: MODUS: PLAN/REVIEW · AGENT.md-Inhalt · ±30 Zeilen Code-Kontext · Plan/Problem
```
Foreground. Bei ABGELEHNT: Ergebnis dem User zeigen.

---

# PROJEKTDOKUMENTATION

## Projekt
Fabric Mod · Minecraft 1.21.4 · Java 21 · Gradle (Fabric Loom) · Wynncraft API v3.
Eigenständige Mod — KEINE Abhängigkeit auf Wynntils.
Client-only Mod: Entrypoint implementiert `ClientModInitializer`.

## Zweck
Wynncraft-Item-Tooltips um Roll-Prozente und einen Overall-Score ergänzen.
Ersetzt die Wynntils-Tooltip-Funktionalität, die seit dem Fruma-Update (2.2.0, April 2026) nicht mehr funktioniert.

## Roll-Formel
- Positive Stats: Bereich = `[base * 0.3 .. base * 1.3]`
- Negative Stats: Bereich = `[base * 0.7 .. base * 1.3]`
- Roll% = `(aktueller_wert - min) / (max - min) * 100`, clamped 0–100
- Pre-identified Stats (nur `int` in API, kein min/max-Objekt) → überspringen

## Projektstruktur
```
wynnrolls/
├── CLAUDE.md                         ← diese Datei
├── AGENT.md                          ← Urteils-Agent-Verhalten
├── plan_aktuell.md                   ← aktueller Plan (temporär)
├── _state/                           ← State-Dateien für Hooks
├── build.gradle
├── settings.gradle
├── gradle.properties
├── src/main/java/dev/wynnrolls/
│   ├── tooltip/
│   │   └── StatEntry.java            # Datenklasse: displayName, value, unit, isNegativeStat
│   ├── api/
│   │   ├── ItemData.java             # Datenklasse: name, tier, type, identifications
│   │   ├── IdentificationData.java   # min, max, raw
│   │   ├── ItemDatabase.java         # Lädt items.json aus Resources (kein Netzwerk)
│   │   └── ItemWeightDatabase.java   # Lädt item_weights.json (Nori+WynnPool Scales)
│   ├── calc/
│   │   ├── RollCalculator.java       # Roll-% pro Stat + RollTier enum
│   │   └── OverallScore.java         # Gesamtscore (Durchschnitt)
│   └── util/
│       └── DebugLogger.java          # [WynnRolls] Prefix-Logging
├── src/client/java/dev/wynnrolls/
│   ├── WynnRollsMod.java             # Mod Entrypoint (ClientModInitializer)
│   ├── tooltip/
│   │   ├── TooltipParser.java        # Lore-Zeilen aus ItemStack parsen
│   │   ├── TooltipInjector.java      # Roll-Pipeline: Parse→DB→Calc→Render
│   │   └── StatNameMapper.java       # Tooltip-Name → API-Key Mapping (camelCase + Fallback)
│   ├── render/
│   │   └── TooltipRenderer.java      # Farbcodiertes Tooltip-Rendering
│   ├── config/
│   │   └── WynnRollsConfig.java      # Keybinds, Gewichtung, Toggle  [TODO]
│   └── util/
│       └── ServerDetector.java       # Wynncraft-Server-Erkennung
├── src/main/resources/
│   ├── items.json                    # Gebündelte Item-DB (Wynntils Static Storage, 6.4MB)
└── src/main/resources/
    ├── fabric.mod.json
    └── wynnrolls.mixins.json
```

## Datei-Referenz

**`WynnRollsMod.java`** — Entrypoint (ClientModInitializer). Registriert Events via `ServerDetector.register()` und `TooltipInjector.register()`.
Implementiert `ClientModInitializer`. Liegt in `src/client/`.

**`tooltip/TooltipParser.java`** — Registriert NICHT selbst das Event (das tut TooltipInjector).
Parst `List<Text>` zu `ParseResult` (record mit itemName, rarity="WYNNCRAFT", itemType="UNKNOWN", stats).
Liegt in `src/client/`.

**FRUMA-FORMAT (April 2026+) — WICHTIG:**
- Rarity/Type ist NICHT mehr als Text in Tooltip — nur als Custom-Font-PUA-Zeichen.
- Stat-Zeilen haben KEINEN Doppelpunkt mehr!
- Neues Format: `StatName<Supplementary-PUA-Chars>+/-Wert<Einheit> <Bar-PUA-Chars>`
- Beispiel roh: `Mana Steal󏿑󐁰+5/3s [U+E023]󏿷[U+E023]`
- Nach Stripping: `Mana Steal+5/3s`
- Item-Name in L0 von Supplementary-PUA eingerahmt: `󏀀Titanomachia󏀀`
- Wynncraft-Erkennung: L0 enthält BMP-PUA (U+E000-U+F8FF) oder Surrogate-Chars

**Stripping-Strategie — `stripAll()` (zeichenweise, KEIN regex für Surrogates!):**
- Java's `Pattern.replaceAll()` entfernt Surrogate-Chars NICHT zuverlässig → char-by-char loop
- `§X` → 2 Chars skippen
- BMP PUA `U+E000–U+F8FF` → 1 Char skippen
- High-Surrogate + Low-Surrogate Paar → 2 Chars skippen (Supplementary PUA)
- Orphaned Low-Surrogate → 1 Char skippen

**Stat-Regex** (nach Stripping): `^([A-Za-z][A-Za-z0-9 ]*?)\\s*([+-]\\d[\\d,]*(?:\\.\\d+)?)(/%|/3s|/5s|%| tier)?.*$`
Einheiten: flat, %, /3s, /5s, tier (Attack Speed)
Negative Stats: heal/teleport/meteor/ice snake/spell cost, jump height, healing efficiency
Gefiltert: Skill-Reqs (Strength/Dexterity/Intelligence/Defence/Agility), Combat Level, Quest, Elemental Defences/Damages

**`tooltip/TooltipInjector.java`** — Registriert `ItemTooltipCallback.EVENT`.
Koordiniert die gesamte Pipeline: Parse → ItemDatabase.get() → StatNameMapper → RollCalculator → TooltipRenderer.
Throttle via `lastProcessedItem`. `resetLastLogged()` für Inventory-Close.
Liegt in `src/client/`.

**`tooltip/StatEntry.java`** — Klasse (kein Record!).
Felder: `String displayName`, `double value`, `Unit unit`, `boolean isNegativeStat`, `String rawLine`.
Enum `Unit`: FLAT, PERCENT, PER_3S, PER_5S, TIER.
Liegt in `src/main/`.

**`tooltip/StatNameMapper.java`** — Löst Tooltip-Anzeigenamen auf API-Keys auf.
Strategie: 1) Hardcoded Overrides → 2) camelCase-Konvertierung → 3) "raw"+capitalize Fallback.
Methode: `resolve(displayName, itemData) → IdentificationData`.
Liegt in `src/client/`.

**`api/ItemData.java`** — Datenklasse. Felder: `name`, `tier`, `type`, `subType`, `Map<String, IdentificationData> identifications`.
Liegt in `src/main/`.

**`api/IdentificationData.java`** — `int min, max, raw`. Liegt in `src/main/`.

**`api/ItemDatabase.java`** — Lädt `items.json` aus JAR-Resources beim Start (kein Netzwerk!).
`ConcurrentHashMap`, Key = lowercase name. Methoden: `load()`, `get(name)`, `size()`.
Quelle: Wynntils Static Storage `gear_expanded.json` (6615 Items, 6.4MB).
Liegt in `src/main/`.

**`api/ItemWeightDatabase.java`** — Lädt `item_weights.json` (Wynntils athena, 19KB).
`Map<String, List<ScaleEntry>>`, Key = lowercase item name.
`ScaleEntry` record: `source` (wynnpool/nori), `scaleName`, `weights` (Map statKey → double).
Gewichtete Score-Formel (Wynntils): `weightedSum/sumWeights`, negative weights invertieren (100-pct).
`getScales(itemName) → List<ScaleEntry>`. Liegt in `src/main/`.

**`calc/RollCalculator.java`** — `calculate(stat, idData) → double` (0–100 oder -1).
`getTier(pct) → RollTier`. Negative Stats: gear_expanded hat korrekte min/max (min < max bei neg. Stats).
Liegt in `src/main/`.

**`calc/OverallScore.java`** — `calculateAverage(Collection<Double>) → double`. Werte < 0 ignoriert.
Liegt in `src/main/`.

**`render/TooltipRenderer.java`** — `formatRoll(pct)` → `" §7[§X##.#%§7]"`, `formatOverall(pct)`.
Farbschema: <30% §4, <60% §c, <80% §e, <95% §a, 100% §6. Liegt in `src/client/`.
Default-Gewichtung: alle 1.0. Konfigurierbar über Config.

**`render/TooltipRenderer.java`** — [TODO] Injiziert Roll-% mit Farbcodes in Tooltip-Zeilen.
Farbschema: 0–30% §4 (dunkelrot), 30–60% §c (rot), 60–80% §e (gelb), 80–95% §a (grün), 95–100% §6 (gold).
Format: `§7[§a85.3%§7]` hinter dem Stat-Wert.
Overall Score als letzte Zeile: `§8──────── §7Overall: §a81.5%`

**`util/ServerDetector.java`** — Prüft via `ClientPlayConnectionEvents.JOIN/DISCONNECT`.
Aktiv wenn Server-IP "wynncraft" oder "wynncraft.com" enthält. Reset bei Disconnect.
`forceEnable()` für Dev-Tests ohne echten Server.
Liegt in `src/client/`.

**`util/DebugLogger.java`** — Alle Logs mit `[WynnRolls]` Prefix via SLF4J.
Felder: `boolean debugEnabled` (default: true). Toggle via `setDebugEnabled()`.
Methoden: `log`, `warn`, `error(msg)`, `error(msg, throwable)`.
Liegt in `src/main/`.

**`config/WynnRollsConfig.java`** — [TODO] Keybind: SHIFT = erweiterte Ansicht.
Felder: `boolean debugMode`, `boolean alwaysShow`, `Map<String, Double> statWeights`.

## Wynncraft API v3 — Item-Endpoint

Basis-URL: `https://api.wynncraft.com/v3/item`
Doku: `https://docs.wynncraft.com/docs/modules/item`

**Wichtige Endpunkte:**
- `GET /v3/item/?fullResult` — Alle Items auf einmal (bypass Pagination)
- `GET /v3/item/search?query={name}` — Suche nach Item-Name
- `GET /v3/item/metadata` — Alle verfügbaren Identifications + Filter

**API-Antwort-Struktur (Identifications):**
```json
"identifications": {
    "lifeSteal": { "min": 135, "max": 585, "raw": 450 },
    "rawDefence": 40
}
```
- Objekt mit min/max/raw → gerollte Identification
- Einzelner int → pre-identified, NICHT gerollt → überspringen

**Rate Limit:** 120 requests/minute. Cache-TTL: 1 Stunde.

## Phasen & Status

### Phase 1: Projekt-Setup + Tooltip-Parser
- [x] Fabric-Mod-Projekt mit Gradle aufsetzen
- [x] fabric.mod.json mit mod-id `wynnrolls`
- [x] ServerDetector implementieren
- [x] TooltipParser: ItemTooltipCallback hooken, Lore parsen
- [x] StatEntry + DebugLogger
- [x] Test: Hover über Item → Stats in Konsole ✓ (verifiziert 2026-04-06)

### Phase 2: Datenbank + Item-Erkennung (kein API-Zugriff!)
- [x] items.json gebündelt (Wynntils Static Storage, 6615 Items)
- [x] ItemData + IdentificationData Datenklassen
- [x] ItemDatabase: Lädt aus Resources, kein Netzwerk
- [x] StatNameMapper: camelCase + Fallback-Strategie
- [x] Test: Item hovern → DB-Match + Base-Werte im Log ✓ (verifiziert 2026-04-06)

### Phase 3: Roll-Berechnung
- [x] RollCalculator: Einzelstat-Prozent
- [x] OverallScore: Durchschnitt
- [x] Negative Stats: gear_expanded hat korrekte min/max
- [x] Pre-identified Stats (int in JSON) ausgeschlossen
- [x] Test: Bekanntes Item → erwartete Prozentwerte im Log ✓ (verifiziert 2026-04-06)

### Phase 4: Tooltip-Rendering
- [x] TooltipInjector: Roll-% visuell in Lore-Zeilen einfügen
- [x] TooltipRenderer: RGB-Farbgradient (stufenlos, 0%–100%)
- [x] Overall Score oben im Tooltip
- [x] ItemWeightDatabase: Nori + WynnPool Scales aus item_weights.json
- [x] Gewichtete Skalen-Scores im Tooltip (unterhalb Overall)
- [ ] Keybind-Toggle (Shift = Detail-Ansicht)
- [ ] Test: Visuell korrekt im Spiel (mit Skalen)

## Debug-Output Format
```
[WynnRolls] === Item: Monster (MYTHIC WAND) ===
[WynnRolls] Parsed Stats:
[WynnRolls]   Life Steal: +585 (per3s)
[WynnRolls]   Fire Damage: +32 (percent)
[WynnRolls] API: Monster → FOUND
[WynnRolls]   lifeSteal: base=450, range=[135..585]
[WynnRolls]   rawDefence: 40 (PRE-ID, skip)
[WynnRolls] Rolls:
[WynnRolls]   Life Steal: 585 in [135..585] = 100.0%
[WynnRolls]   Fire Damage: 32 in [10..42] = 68.8%
[WynnRolls] Overall: 84.4% (weighted: 86.1%)
```

## Build-System

### Bauen (EINZIGER funktionierender Weg)
```powershell
powershell.exe -NoProfile -Command '
$env:JAVA_HOME = "C:\Users\YTGBS\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "D:\Games\Minecraft\Self Made mods\wynnrolls"
.\gradlew.bat build 2>&1
'
```
Oder via `build_j21.bat` (kopiert JAR automatisch nach erfolgreichem Build).

### Build-Erkenntnisse (WICHTIG — nicht wieder suchen!)
- **cmd.exe via bash** (`cmd.exe /c ...`) funktioniert NICHT — Output wird komplett geschluckt, gradlew läuft nicht durch.
- **`gradlew.bat` direkt in PowerShell** schlägt fehl wenn JAVA_HOME im äußeren Scope gesetzt wird — MUSS innerhalb des `-Command`-Blocks mit `$env:JAVA_HOME = ...` gesetzt werden.
- **`.\gradlew.bat`** (Backslash) in PowerShell, NICHT `./gradlew.bat` (Forward-Slash).
- **Gradle Wrapper JAR** war veraltet (42KB, kein `IDownload`) → ersetzt durch neue Version (48KB). Nie mehr das alte nutzen.
- **Fabric Loom 1.16-SNAPSHOT** benötigt **Gradle 9.4.1** (nicht 8.x, nicht 9.0). Wrapper-Properties zeigen auf `gradle-9.4.1-bin.zip`.
- **Gradle 9.4.1** ist gecacht unter `C:\Users\YTGBS\.gradle\wrapper\dists\gradle-9.4.1-bin\`.
- **`gradle-9.4-bin.zip`** gibt 404 — immer `gradle-9.4.1-bin.zip` verwenden.
- **Bash-Escaping in PowerShell-Command**: `$variable` in `"..."` wird von bash expandiert → leer. Entweder `'...'` (single-quotes) für den ganzen Block nutzen, oder `\$variable` escapen.
- **JAR-Zielort**: `C:\Users\YTGBS\curseforge\minecraft\Instances\Wynn Fruma\mods\wynnrolls-1.0.0.jar`

### Gradle-Cache-Struktur
```
C:\Users\YTGBS\.gradle\
├── wrapper\dists\
│   └── gradle-9.4.1-bin\   ← einzige aktive Version
└── jdks\
    └── eclipse_adoptium-21-amd64-windows.2\  ← Java 21 für Build
```

## Architektur & Fallstricke
- Java 21 + Fabric API. Kommentare auf Deutsch oder Englisch.
- Client-only Mod: alles unter `src/client/` außer reine Datenklassen (StatEntry, DebugLogger unter `src/main/`).
- API-Calls IMMER async — nie im Render/Main-Thread.
- Gson für JSON-Parsing (als Fabric-Dependency vorhanden).
- Minecraft §-Farbcodes müssen vor dem Stat-Parsen entfernt werden.
- Wynncraft-Tooltips enthalten Unicode-Symbole (❤❋✤✦❉) — vor Stat-Name abschneiden.
- StatNameMapper wird der fehleranfälligste Teil — als editierbare Map bauen.
- Tooltip-Format kann sich mit Wynncraft-Updates ändern — Parser flexibel halten.
- Nicht auf Wynncraft → Mod komplett inaktiv (keine Events, keine API-Calls).
- TooltipParser hat eingebauten Raw-Debug-Log — hilft beim Analysieren des tatsächlichen Tooltip-Formats.

## Gedächtnis-System
CLAUDE.md = Regelwerk & Projektstruktur · AGENT.md = Urteils-Agent-Verhalten · claude-mem = Sitzungshistorie
Vor komplexen Entscheidungen: mem-search für frühere Entscheidungen nutzen.
