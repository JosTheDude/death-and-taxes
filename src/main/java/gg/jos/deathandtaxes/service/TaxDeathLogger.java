package gg.jos.deathandtaxes.service;

import gg.jos.deathandtaxes.config.TaxLoggingSettings;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

/**
 * Writes successful death-tax collections to the configured server log destinations.
 */
public final class TaxDeathLogger implements AutoCloseable {
    private static final String LOG_FILE_NAME = "death-taxes.log";

    private final JavaPlugin plugin;
    private final Logger fileLogger;
    private FileHandler fileHandler;
    private boolean consoleEnabled;

    public TaxDeathLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fileLogger = Logger.getLogger(TaxDeathLogger.class.getName());
        this.fileLogger.setUseParentHandlers(false);
    }

    /**
     * Applies logging settings, replacing the file handler when configuration changes.
     *
     * @param settings current logging configuration
     */
    public void reload(TaxLoggingSettings settings) {
        consoleEnabled = settings.consoleEnabled();
        closeFileHandler();
        if (!settings.fileEnabled()) {
            return;
        }

        try {
            File logFile = new File(plugin.getDataFolder(), LOG_FILE_NAME);
            fileHandler = new FileHandler(logFile.getPath(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileLogger.addHandler(fileHandler);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not open " + LOG_FILE_NAME + " for death-tax logging: " + exception.getMessage());
        }
    }

    /**
     * Records a successful tax collection.
     *
     * @param playerName player that died
     * @param playerId unique ID of the player that died
     * @param taxes taxes actually retained by each economy
     * @param formatter formatter for currency amounts
     */
    public void log(String playerName, UUID playerId, Map<Economy, Double> taxes, CurrencyFormatter formatter) {
        if (taxes.isEmpty()) {
            return;
        }

        String amounts = taxes.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(entry -> entry.getKey().getName() + "=" + formatter.format(entry.getValue()))
                .collect(Collectors.joining(", "));
        String message = "Death tax collected | timestamp=" + Instant.now() + " | player=" + playerName + " (" + playerId + ") | amount=" + amounts;

        if (consoleEnabled) {
            plugin.getLogger().info(message);
        }

        if (fileHandler != null) {
            fileLogger.info(message);
        }
    }

    @Override
    public void close() {
        closeFileHandler();
    }

    private void closeFileHandler() {
        if (fileHandler == null) {
            return;
        }

        fileLogger.removeHandler(fileHandler);
        fileHandler.close();
        fileHandler = null;
    }
}
