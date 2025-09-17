package gg.jos.deathandtaxes.listener;

import gg.jos.deathandtaxes.DeathAndTaxesPlugin;
import gg.jos.deathandtaxes.config.DeathTaxSettings;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Listens for player deaths and applies the configured tax via Vault.
 */
public final class PlayerDeathTaxListener implements Listener {
    private final DeathAndTaxesPlugin plugin;

    /**
     * @param plugin plugin instance used to access configuration and services
     */
    public PlayerDeathTaxListener(DeathAndTaxesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charges the player whenever they die, respecting configuration and economic constraints.
     *
     * @param event death event fired by Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Economy economy = plugin.getEconomy();
        if (economy == null) {
            return;
        }

        DeathTaxSettings settings = plugin.getSettings();
        double balance = economy.getBalance(player);
        double taxAmount = settings.calculateTax(balance);
        if (taxAmount <= 0.0D) {
            return;
        }

        EconomyResponse response = economy.withdrawPlayer(player, taxAmount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Failed to withdraw death tax from " + player.getName() + ": " + response.errorMessage);
            return;
        }

        Component message = settings.renderDeathMessage(
                taxAmount,
                plugin.getCurrencyFormatter(),
                plugin.getMiniMessage()
        );
        if (message != null) {
            player.sendMessage(message);
        }
    }
}
