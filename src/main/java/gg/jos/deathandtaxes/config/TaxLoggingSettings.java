package gg.jos.deathandtaxes.config;

/**
 * Controls where successfully collected death taxes are recorded.
 */
public record TaxLoggingSettings(boolean consoleEnabled, boolean fileEnabled) {
}
