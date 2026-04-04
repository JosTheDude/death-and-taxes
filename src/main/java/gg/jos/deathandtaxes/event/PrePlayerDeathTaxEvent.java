package gg.jos.deathandtaxes.event;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PrePlayerDeathTaxEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Map<Economy, Double> taxes;
    private boolean cancelled;

    public PrePlayerDeathTaxEvent(Player who, Map<Economy, Double> taxes) {
        super(who);
        this.taxes = taxes;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * @return A mutable {@link Map} of {@link Economy economies} & the {@link Double tax} on it.
     */
    public @NotNull Map<Economy, Double> getTaxes() {
        return taxes;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
