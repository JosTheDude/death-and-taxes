package gg.jos.deathandtaxes.service;

import gg.jos.deathandtaxes.config.PluginConfig;
import gg.jos.deathandtaxes.economy.EconomyService;
import org.bukkit.entity.Player;


public final class DeathTaxService {
    private final PluginConfig config;
    private final EconomyService economyService;

    public DeathTaxService(PluginConfig config, EconomyService economyService) {
        this.config = config;
        this.economyService = economyService;
    }


    public double applyDeathTax(Player player) {
        if (!economyService.isAvailable()) {
            return 0.0D;
        }

        double balance = economyService.getBalance(player);
        double taxAmount = config.calculateTaxAmount(balance);
        double withdrawAmount = config.clampTaxToBalance(balance, taxAmount);

        if (withdrawAmount <= 0.0D) {
            return 0.0D;
        }

        boolean success = economyService.withdraw(player, withdrawAmount);
        return success ? withdrawAmount : 0.0D;
    }
}
