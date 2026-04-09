package dev.wynnrolls.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("WynnRolls");
    private static boolean debugEnabled = false;

    public static void log(String message) {
        if (debugEnabled) {
            LOGGER.info("[WynnRolls] {}", message);
        }
    }

    public static void warn(String message) {
        LOGGER.warn("[WynnRolls] {}", message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.error("[WynnRolls] {}", message, t);
    }

    public static void error(String message) {
        LOGGER.error("[WynnRolls] {}", message);
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
}
