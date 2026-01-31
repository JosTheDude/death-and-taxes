package gg.jos.deathandtaxes.listener;

import gg.jos.deathandtaxes.DeathAndTaxesPlugin;
import gg.jos.deathandtaxes.config.DeathTaxSettings;
import gg.jos.deathandtaxes.service.CurrencyFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

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

        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        DeathTaxSettings settings = plugin.getSettings();
        CurrencyFormatter formatter = plugin.getCurrencyFormatter();
        MiniMessage miniMessage = plugin.getMiniMessage();

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            double balance = economy.getBalance(offlinePlayer);
            double taxAmount = settings.calculateTax(balance);
            if (taxAmount <= 0.0D) {
                return;
            }

            EconomyResponse response = economy.withdrawPlayer(offlinePlayer, taxAmount);
            if (!response.transactionSuccess()) {
                plugin.getLogger().warning(
                        "Failed to withdraw death tax from " + playerName + " (" + playerId + "): " + response.errorMessage
                );
                return;
            }

            Component message = settings.renderDeathMessage(taxAmount, formatter, miniMessage);
            if (message == null) {
                return;
            }

            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            onlinePlayer.getScheduler().execute(plugin, () -> onlinePlayer.sendMessage(message), null, 1L);
        });
    }
}
