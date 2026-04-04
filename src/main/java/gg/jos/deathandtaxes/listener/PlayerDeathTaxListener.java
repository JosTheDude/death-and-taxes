package gg.jos.deathandtaxes.listener;

import gg.jos.deathandtaxes.DeathAndTaxesPlugin;
import gg.jos.deathandtaxes.config.DeathTaxSettings;
import gg.jos.deathandtaxes.event.PlayerDeathTaxEvent;
import gg.jos.deathandtaxes.event.PrePlayerDeathTaxEvent;
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

import java.util.HashMap;
import java.util.Map;
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
        DeathTaxSettings settings = plugin.getSettings();
        if (!settings.isTaxedWorld(player.getWorld()) || settings.getEconomies().isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        CurrencyFormatter formatter = plugin.getCurrencyFormatter();
        MiniMessage miniMessage = plugin.getMiniMessage();

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            OfflinePlayer targetPlayer = onlinePlayer != null && onlinePlayer.isOnline()
                    ? onlinePlayer
                    : Bukkit.getOfflinePlayer(playerId);

            Map<Economy, Double> taxes = new HashMap<>();
            for (Economy economy : settings.getEconomies()) {
                double balance = economy.getBalance(targetPlayer);
                double taxAmount = settings.calculateTax(balance);
                if (taxAmount > 0.0D) {
                    taxes.put(economy, taxAmount);
                }
            }

            if (taxes.isEmpty()) {
                return;
            }

            PrePlayerDeathTaxEvent preTaxEvent = new PrePlayerDeathTaxEvent(player, taxes);
            if (!preTaxEvent.callEvent()) {
                return;
            }

            Map<Economy, EconomyResponse> taxResponses = new HashMap<>();
            Map<Economy, Double> taxed = new HashMap<>();
            for (Map.Entry<Economy, Double> entry : taxes.entrySet()) {
                EconomyResponse response = entry.getKey().withdrawPlayer(targetPlayer, entry.getValue());
                if (!response.transactionSuccess()) {
                    plugin.getLogger().warning(
                            "Failed to withdraw death tax for economy " + entry.getKey().getName() + " from " + playerName + " (" + playerId + "): " + response.errorMessage
                    );
                } else {
                    taxed.entrySet().add(entry);
                }
                taxResponses.put(entry.getKey(), response);
            }

            new PlayerDeathTaxEvent(player, taxResponses).callEvent();

            Component message = settings.renderDeathMessage(taxed, formatter, miniMessage);
            if (message == null) {
                return;
            }

            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            onlinePlayer.getScheduler().execute(plugin, () -> onlinePlayer.sendMessage(message), null, 1L);
        });
    }
}
