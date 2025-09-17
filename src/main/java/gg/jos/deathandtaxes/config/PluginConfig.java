package gg.jos.deathandtaxes.config;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {
    private final JavaPlugin plugin;

    private TaxMode taxMode;
    private double taxValue;
    private double minimumBalance;
    private int decimalPlaces;
    private String deathMessage;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        load(plugin.getConfig());
    }

    private void load(FileConfiguration config) {
        String modeValue = config.getString("tax.mode", "PERCENTAGE");
        this.taxMode = TaxMode.fromString(modeValue, TaxMode.PERCENTAGE);
        this.taxValue = Math.max(0.0, config.getDouble("tax.value", 10.0));
        this.minimumBalance = Math.max(0.0, config.getDouble("tax.minimum-balance", 0.0));
        this.decimalPlaces = Math.max(0, config.getInt("display.decimal-places", 2));
        this.deathMessage = config.getString(
                "messages.death",
                "&cYou lost ${amount} to the death tax.");
    }

    public TaxMode getTaxMode() {
        return taxMode;
    }

    public double getTaxValue() {
        return taxValue;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public double calculateTaxAmount(double balance) {
        double rawAmount;
        if (taxMode == TaxMode.FIXED) {
            rawAmount = taxValue;
        } else {
            rawAmount = balance * (taxValue / 100.0D);
        }
        if (Double.isNaN(rawAmount) || Double.isInfinite(rawAmount)) {
            return 0.0D;
        }
        return Math.max(0.0D, rawAmount);
    }

    public double clampTaxToBalance(double balance, double taxAmount) {
        double maxDrain = Math.max(0.0D, balance - minimumBalance);
        return Math.max(0.0D, Math.min(taxAmount, maxDrain));
    }

    public String formatAmount(double amount) {
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(decimalPlaces);
        format.setMinimumFractionDigits(Math.min(decimalPlaces, 2));
        format.setGroupingUsed(false);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        format.setDecimalFormatSymbols(symbols);
        return format.format(amount);
    }
}
