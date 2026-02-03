package gg.jos.deathandtaxes.command;

import gg.jos.deathandtaxes.DeathAndTaxesPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class DeathAndTaxesCommand implements CommandExecutor, TabCompleter {

    private final DeathAndTaxesPlugin plugin;

    public DeathAndTaxesCommand(DeathAndTaxesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("deathandtaxes.reload")) {
                sender.sendMessage("You do not have permission to reload DeathAndTaxes.");
                return true;
            }

            plugin.reloadSettings();
            sender.sendMessage("DeathAndTaxes configuration reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("deathandtaxes.reload")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(prefix)) {
                return Collections.singletonList("reload");
            }
        }
        return Collections.emptyList();
    }
}
