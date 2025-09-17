package gg.jos.deathandtaxes.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyService {
    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hook() {
        RegisteredServiceProvider<Economy> provider =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            this.economy = null;
            return false;
        }
        this.economy = provider.getProvider();
        return true;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) {
            return 0.0D;
        }
        return economy.getBalance(player);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning(
                    "Failed to withdraw death tax from " + player.getName() + ": "
                            + response.errorMessage);
        }
        return response.transactionSuccess();
    }
}
