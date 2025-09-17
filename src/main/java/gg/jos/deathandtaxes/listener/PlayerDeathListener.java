package gg.jos.deathandtaxes.listener;

import gg.jos.deathandtaxes.config.PluginConfig;
import gg.jos.deathandtaxes.service.DeathTaxService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathListener implements Listener {
    private final DeathTaxService deathTaxService;
    private final PluginConfig config;

    public PlayerDeathListener(DeathTaxService deathTaxService, PluginConfig config) {
        this.deathTaxService = deathTaxService;
        this.config = config;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        double withdrawn = deathTaxService.applyDeathTax(player);
        if (withdrawn <= 0.0D) {
            return;
        }

        String formatted = config.formatAmount(withdrawn);
        String message = config.getDeathMessage().replace("{amount}", formatted);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
