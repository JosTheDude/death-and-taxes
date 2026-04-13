package gg.jos.deathandtaxes.event;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class PlayerDeathTaxEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Map<Economy, EconomyResponse> taxes;

    public PlayerDeathTaxEvent(Player who, Map<Economy, EconomyResponse> taxes) {
        super(who);
        this.taxes = taxes;
    }

    /**
     * @return An immutable {@link Map} of {@link Economy Economies} & the {@link EconomyResponse} for each.
     */
    public @NotNull Map<Economy, EconomyResponse> getTaxes() {
        return Collections.unmodifiableMap(taxes);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
