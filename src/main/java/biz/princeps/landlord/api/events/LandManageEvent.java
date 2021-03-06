package biz.princeps.landlord.api.events;

import biz.princeps.landlord.util.OwnedLand;
import com.sk89q.worldguard.protection.flags.Flag;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LandManageEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Player player;
    private OwnedLand land;
    private Flag<?> flagChanged;
    private Object oldValue;
    private Object newValue;


    public LandManageEvent(Player player, OwnedLand land, Flag<?> flagChanged, Object oldValue, Object newValue) {
        this.player = player;
        this.land = land;
        this.flagChanged = flagChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Returns the player, who wants to claim a land
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the bought land
     */
    public OwnedLand getLand() {
        return land;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public Flag<?> getChangedFlag() {
        return flagChanged;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
