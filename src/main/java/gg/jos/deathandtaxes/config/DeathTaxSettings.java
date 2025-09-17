package gg.jos.deathandtaxes.config;

import gg.jos.deathandtaxes.service.CurrencyFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Immutable representation of the plugin's configurable tax behaviour.
 */
public final class DeathTaxSettings {
    private final TaxMode taxMode;
    private final double taxValue;
    private final double minimumBalance;
    private final int decimalPlaces;
    private final String deathMessage;

    private DeathTaxSettings(TaxMode taxMode, double taxValue, double minimumBalance, int decimalPlaces, String deathMessage) {
        this.taxMode = taxMode;
        this.taxValue = taxValue;
        this.minimumBalance = minimumBalance;
        this.decimalPlaces = decimalPlaces;
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

        return new DeathTaxSettings(mode, value, minimumBalance, decimalPlaces, deathMessage);
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
     * @param amount amount withdrawn from the player
     * @param formatter formatter for currency display
     * @param miniMessage MiniMessage instance used to deserialize the message
     * @return parsed Adventure component, or {@code null} if the message is blank
     */
    public Component renderDeathMessage(double amount, CurrencyFormatter formatter, MiniMessage miniMessage) {
        if (deathMessage == null || deathMessage.isBlank()) {
            return null;
        }

        String formattedAmount = formatter.format(amount);
        return miniMessage.deserialize(
                deathMessage,
                Placeholder.unparsed("amount", formattedAmount)
        );
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
