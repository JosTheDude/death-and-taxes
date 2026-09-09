package gg.jos.deathandtaxes.listener;

import gg.jos.deathandtaxes.DeathAndTaxesPlugin;
import gg.jos.deathandtaxes.config.DeathTaxSettings;
import gg.jos.deathandtaxes.config.TaxAccount;
import gg.jos.deathandtaxes.event.PlayerDeathTaxEvent;
import gg.jos.deathandtaxes.event.PrePlayerDeathTaxEvent;
import gg.jos.deathandtaxes.service.CurrencyFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for player deaths and applies the configured tax via Vault.
 */
public final class PlayerDeathTaxListener implements Listener {
    private final DeathAndTaxesPlugin plugin;
    private final NamespacedKey graceDeathsUsedKey;

    /**
     * @param plugin plugin instance used to access configuration and services
     */
    public PlayerDeathTaxListener(DeathAndTaxesPlugin plugin) {
        this.plugin = plugin;
        this.graceDeathsUsedKey = new NamespacedKey(plugin, "grace-deaths-used");
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
        int discountPercent = settings.getDiscountPercent(player);
        int remainingGraceDeaths = claimGraceDeath(player, settings.getGraceDeathCount());

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            OfflinePlayer targetPlayer = onlinePlayer != null && onlinePlayer.isOnline()
                    ? onlinePlayer
                    : Bukkit.getOfflinePlayer(playerId);

            Map<Economy, Double> taxes = new HashMap<>();
            for (Economy economy : settings.getEconomies()) {
                double balance = economy.getBalance(targetPlayer);
                double taxAmount = applyDiscount(settings.calculateTax(balance), discountPercent);
                if (taxAmount > 0.0D) {
                    taxes.put(economy, taxAmount);
                }
            }

            if (taxes.isEmpty()) {
                return;
            }

            if (remainingGraceDeaths >= 0) {
                Component message = settings.renderGraceDeathMessage(taxes, remainingGraceDeaths, formatter, miniMessage);
                sendMessage(playerId, message);
                return;
            }

            PrePlayerDeathTaxEvent preTaxEvent = new PrePlayerDeathTaxEvent(player, taxes);
            if (!preTaxEvent.callEvent()) {
                return;
            }

            Map<Economy, EconomyResponse> taxResponses = new HashMap<>();
            Map<Economy, Double> taxed = new HashMap<>();
            for (Map.Entry<Economy, Double> entry : taxes.entrySet()) {
                Economy economy = entry.getKey();
                double amount = entry.getValue();
                EconomyResponse response = economy.withdrawPlayer(targetPlayer, amount);
                if (!response.transactionSuccess()) {
                    plugin.getLogger().warning(
                            "Failed to withdraw death tax for economy " + economy.getName() + " from " + playerName + " (" + playerId + "): " + response.errorMessage
                    );
                } else if (depositTax(economy, targetPlayer, playerName, playerId, amount, settings.getTaxAccount())) {
                    taxed.put(economy, amount);
                }
                taxResponses.put(economy, response);
            }

            new PlayerDeathTaxEvent(player, taxResponses).callEvent();

            Component discountMessage = settings.renderDiscountMessage(discountPercent, miniMessage);
            Component message = settings.renderDeathMessage(taxed, formatter, miniMessage);
            if (discountMessage == null && message == null) {
                return;
            }

            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            onlinePlayer.getScheduler().execute(plugin, () -> {
                if (message != null) {
                    onlinePlayer.sendMessage(message);
                }

                if (discountMessage != null && !taxed.isEmpty()) {
                    onlinePlayer.sendMessage(discountMessage);
                }
            }, null, 1L);
        });
    }

    private int claimGraceDeath(Player player, int graceDeathCount) {
        if (graceDeathCount <= 0) {
            return -1;
        }

        PersistentDataContainer data = player.getPersistentDataContainer();
        int used = data.getOrDefault(graceDeathsUsedKey, PersistentDataType.INTEGER, 0);
        if (used >= graceDeathCount) {
            return -1;
        }

        int updated = used + 1;
        data.set(graceDeathsUsedKey, PersistentDataType.INTEGER, updated);
        return graceDeathCount - updated;
    }

    private void sendMessage(UUID playerId, Component message) {
        if (message == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        player.getScheduler().execute(plugin, () -> player.sendMessage(message), null, 1L);
    }

    private double applyDiscount(double taxAmount, int discountPercent) {
        if (taxAmount <= 0.0D || discountPercent <= 0) {
            return taxAmount;
        }

        return taxAmount * ((100.0D - discountPercent) / 100.0D);
    }

    private boolean depositTax(Economy economy, OfflinePlayer taxpayer, String playerName, UUID playerId, double amount, TaxAccount taxAccount) {
        if (!taxAccount.isEnabled()) {
            return true;
        }

        EconomyResponse depositResponse = economy.depositPlayer(taxAccount.name(), amount);
        if (depositResponse.transactionSuccess()) {
            return true;
        }

        if (!taxAccount.refundOnDepositFailure()) {
            plugin.getLogger().warning(
                    "Failed to deposit death tax for economy " + economy.getName() + " into tax account '" + taxAccount.name() + "'. Tax from " + playerName + " (" + playerId + ") was retained because refunds are disabled: " + depositResponse.errorMessage
            );
            return true;
        }

        plugin.getLogger().warning(
                "Failed to deposit death tax for economy " + economy.getName() + " into tax account '" + taxAccount.name() + "'. Refunding " + playerName + " (" + playerId + "): " + depositResponse.errorMessage
        );
        EconomyResponse refundResponse = economy.depositPlayer(taxpayer, amount);
        if (!refundResponse.transactionSuccess()) {
            plugin.getLogger().severe(
                    "Failed to refund " + playerName + " (" + playerId + ") after the tax-account deposit failed for economy " + economy.getName() + ": " + refundResponse.errorMessage
            );
            return true;
        }

        return false;
    }
}
