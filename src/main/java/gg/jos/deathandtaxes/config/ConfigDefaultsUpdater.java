package gg.jos.deathandtaxes.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Adds newly introduced default configuration paths without replacing administrator-owned values or comments.
 */
public final class ConfigDefaultsUpdater {
    private ConfigDefaultsUpdater() {
    }

    /**
     * Adds any paths present in the bundled config but absent from the server's config.
     *
     * @param plugin plugin that owns the configuration
     * @return whether the configuration file was updated successfully
     */
    public static boolean addMissingDefaults(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try (InputStream resource = plugin.getResource("config.yml")) {
            if (resource == null) {
                plugin.getLogger().severe("Bundled config.yml could not be found.");
                return false;
            }

            YamlConfiguration configured = loadConfiguration(configFile);
            YamlConfiguration defaults = loadDefaults(resource);
            boolean changed = mergeMissingPaths(configured, defaults, plugin, "");

            if (changed) {
                saveAtomically(configured, configFile);
                plugin.getLogger().info("Added missing options from the bundled config.yml without replacing existing values.");
            }

            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not update config.yml with missing defaults: " + exception.getMessage());
            return false;
        }
    }

    private static YamlConfiguration loadDefaults(InputStream resource) throws IOException, InvalidConfigurationException {
        try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = new YamlConfiguration();
            defaults.options().parseComments(true);
            defaults.load(reader);
            return defaults;
        }
    }

    private static YamlConfiguration loadConfiguration(File configFile) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.options().parseComments(true);
        configuration.load(configFile);
        return configuration;
    }

    private static boolean mergeMissingPaths(ConfigurationSection configured, ConfigurationSection defaults, JavaPlugin plugin, String parentPath) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            String path = parentPath.isEmpty() ? key : parentPath + "." + key;
            ConfigurationSection defaultSection = defaults.getConfigurationSection(key);
            if (!configured.contains(key)) {
                copyMissingPath(configured, defaults, key, defaultSection, plugin, path);
                changed = true;
                continue;
            }

            if (defaultSection == null) {
                continue;
            }

            ConfigurationSection configuredSection = configured.getConfigurationSection(key);
            if (configuredSection == null) {
                plugin.getLogger().warning("Skipped bundled config options below '" + path + "' because the existing value is not a section.");
                continue;
            }

            changed |= mergeMissingPaths(configuredSection, defaultSection, plugin, path);
        }

        return changed;
    }

    private static void copyMissingPath(ConfigurationSection configured, ConfigurationSection defaults, String key, ConfigurationSection defaultSection, JavaPlugin plugin, String path) {
        if (defaultSection == null) {
            configured.set(key, defaults.get(key));
        } else {
            ConfigurationSection createdSection = configured.createSection(key);
            mergeMissingPaths(createdSection, defaultSection, plugin, path);
        }

        configured.setComments(key, defaults.getComments(key));
        configured.setInlineComments(key, defaults.getInlineComments(key));
    }

    private static void saveAtomically(YamlConfiguration configuration, File configFile) throws IOException {
        Path configPath = configFile.toPath().toAbsolutePath();
        Path temporaryFile = Files.createTempFile(configPath.getParent(), "config.yml.", ".tmp");
        try {
            configuration.save(temporaryFile.toFile());
            try {
                Files.move(temporaryFile, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
