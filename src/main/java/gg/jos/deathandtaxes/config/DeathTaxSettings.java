package gg.jos.deathandtaxes.config;

import gg.jos.deathandtaxes.service.CurrencyFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Immutable representation of the plugin's configurable tax behaviour.
 */
public final class DeathTaxSettings {
    private final List<Economy> economies;
    private final TaxMode taxMode;
    private final double taxValue;
    private final double minimumBalance;
    private final int decimalPlaces;
    private final List<String> worlds;
    private final boolean worldsBlacklist;
    private final String deathMessage;

    private DeathTaxSettings(List<Economy> economies, TaxMode taxMode, double taxValue, double minimumBalance, int decimalPlaces, List<String> worlds, boolean worldsBlacklist, String deathMessage) {
        this.economies = economies;
        this.taxMode = taxMode;
        this.taxValue = taxValue;
        this.minimumBalance = minimumBalance;
        this.decimalPlaces = decimalPlaces;
        this.worlds = worlds;
        this.worldsBlacklist = worldsBlacklist;
        this.deathMessage = deathMessage;
    }

    /**
     * Reads configuration values and produces an immutable settings object.
     *
     * @param plugin owning plugin for logging purposes
     * @param config configuration source
     * @return populated settings instance based on the provided configuration
     */
    public static DeathTaxSettings fromConfig(JavaPlugin plugin, FileConfiguration config) {
        List<Economy> economies = new ArrayList<>();
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }

        RegisteredServiceProvider<Economy> defaultEconomy = Bukkit.getServicesManager().getRegistration(Economy.class);
        Collection<RegisteredServiceProvider<Economy>> registeredEconomies = Bukkit.getServicesManager().getRegistrations(Economy.class);
        if (defaultEconomy == null) {
            return null;
        }

        if (config.getBoolean("economy.default", true)) {
            economies.add(defaultEconomy.getProvider());
        } else {
            List<String> economyNames = config.getStringList("economy.economies");
            if (economyNames.isEmpty()) {
                plugin.getLogger().warning("No economies specified in config, defaulting to highest priority economy.");
                economies.add(defaultEconomy.getProvider());
            }

            for (String economyName : economyNames) {
                Economy economy = null;
                for (RegisteredServiceProvider<Economy> economyProvider : registeredEconomies) {
                    Economy provided = economyProvider.getProvider();
                    if (provided.getName().equalsIgnoreCase(economyName)) {
                        economy = provided;
                        break;
                    }
                }

                if (economy == null) {
                    plugin.getLogger().warning("Could not find economy named " + economyName + " from config.");
                    return null;
                }
            }
        }

        String modeValue = config.getString("tax.mode", "PERCENTAGE");
        TaxMode mode = TaxMode.fromConfigValue(modeValue);
        if (mode == null) {
            plugin.getLogger().warning("Invalid tax mode '" + modeValue + "' in config. Defaulting to PERCENTAGE.");
            mode = TaxMode.PERCENTAGE;
        }

        double value = config.getDouble("tax.value", 10.0D);
        double minimumBalance = Math.max(0.0D, config.getDouble("tax.minimum-balance", 0.0D));
        int decimalPlaces = Math.max(0, config.getInt("display.decimal-places", 2));
        String deathMessage = config.getString("messages.death", "<red>You lost <amount> coins to the death tax.</red>");

        List<String> worlds = config.getStringList("tax.worlds");
        boolean worldsBlacklist = config.getBoolean("tax.blacklist-worlds", false);
        if (worlds.isEmpty() && !worldsBlacklist) {
            plugin.getLogger().warning("No worlds specified in config and blacklist is false, death and taxes will have no effect");
        }

        return new DeathTaxSettings(economies, mode, value, minimumBalance, decimalPlaces, worlds, worldsBlacklist, deathMessage);
    }

    public boolean isTaxedWorld(World world) {
        return worlds.contains(world.getName()) != worldsBlacklist;
    }

    /**
     * Calculates how much currency should be removed from a player's balance.
     *
     * @param balance current player balance
     * @return the amount that should be withdrawn according to the active settings
     */
    public double calculateTax(double balance) {
        if (balance <= minimumBalance) {
            return 0.0D;
        }

        double desiredAmount = switch (taxMode) {
            case PERCENTAGE -> balance * (taxValue / 100.0D);
            case FIXED -> taxValue;
        };

        if (desiredAmount <= 0.0D) {
            return 0.0D;
        }

        double maximumAllowed = balance - minimumBalance;
        if (maximumAllowed <= 0.0D) {
            return 0.0D;
        }

        return Math.min(desiredAmount, maximumAllowed);
    }

    /**
     * Renders the configured death message using MiniMessage and the provided placeholder values.
     *
     * @param taxes the taxes taken from the player
     * @param formatter formatter for currency display
     * @param miniMessage MiniMessage instance used to deserialize the message
     * @return parsed Adventure component, or {@code null} if the message is blank
     */
    public Component renderDeathMessage(Map<Economy, Double> taxes, CurrencyFormatter formatter, MiniMessage miniMessage) {
        if (deathMessage == null || deathMessage.isBlank()) {
            return null;
        }

        List<TagResolver> placeholders = new ArrayList<>();
        for (Map.Entry<Economy, Double> entry : taxes.entrySet()) {
            placeholders.add(Placeholder.unparsed("amount_" + entry.getKey().getName(), formatter.format(entry.getValue())));
        }

        if (taxes.size() == 1) {
            placeholders.add(Placeholder.unparsed("amount", formatter.format(taxes.values().iterator().next())));
        }

        return miniMessage.deserialize(
                deathMessage,
                placeholders.toArray(TagResolver[]::new)
        );
    }

    /**
     * @return list of economies to apply the tax to
     */
    public List<Economy> getEconomies() {
        return economies;
    }

    /**
     * @return configured tax calculation mode
     */
    public TaxMode getTaxMode() {
        return taxMode;
    }

    /**
     * @return configured tax value (percentage or fixed amount depending on mode)
     */
    public double getTaxValue() {
        return taxValue;
    }

    /**
     * @return minimum balance that must remain after the tax is applied
     */
    public double getMinimumBalance() {
        return minimumBalance;
    }

    /**
     * @return number of decimal places used for currency formatting
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /**
     * @return raw MiniMessage-formatted death message string
     */
    public String getDeathMessage() {
        return deathMessage;
    }
}
