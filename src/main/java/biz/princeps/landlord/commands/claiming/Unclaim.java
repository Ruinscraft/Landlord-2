package biz.princeps.landlord.commands.claiming;

import biz.princeps.landlord.api.Options;
import biz.princeps.landlord.api.events.LandUnclaimEvent;
import biz.princeps.landlord.commands.LandlordCommand;
import biz.princeps.landlord.persistent.LPlayer;
import biz.princeps.landlord.util.OwnedLand;
import biz.princeps.landlord.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: 17/07/17
 */
public class Unclaim extends LandlordCommand {

    public void onUnclaim(Player player, String chunkname) {

        OwnedLand pr;
        if (chunkname.equals("null")) {
            Chunk chunk = player.getWorld().getChunkAt(player.getLocation());
            pr = plugin.getWgHandler().getRegion(chunk);
        } else {
            if (!plugin.isLLRegion(chunkname)) {
                Bukkit.dispatchCommand(player, "/ll help");
                return;
            }

            pr = plugin.getWgHandler().getRegion(chunkname);
        }

        if (plugin.getConfig().getStringList("disabled-worlds").contains(chunkname.split("_")[0])) {
            lm.sendMessage(player, lm.getString("Disabled-World"));
            return;
        }

        if (pr == null) {
            lm.sendMessage(player, lm.getString("Commands.Unclaim.notOwnFreeLand"));
            return;
        }

        // is admin - allowed to unclaim
        boolean isAdmin = false;
        if (!player.hasPermission("landlord.admin.unclaim")) {
            if (!pr.isOwner(player.getUniqueId())) {
                lm.sendMessage(player, lm.getString("Commands.Unclaim.notOwn")
                        .replace("%owner%", pr.printOwners()));
                return;
            }
        } else
            isAdmin = true;

        // Normal unclaim
        LandUnclaimEvent event = new LandUnclaimEvent(player, pr);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            double payback = -1;
            if (!isAdmin || pr.isOwner(player.getUniqueId())) {
                int regionCount = plugin.getWgHandler().getRegionCountOfPlayer(player.getUniqueId());
                int freeLands = plugin.getConfig().getInt("Freelands");

                // System.out.println("regionCount: " + regionCount + " freeLands: " + freeLands);

                if (Options.isVaultEnabled()) {
                    if (regionCount <= freeLands)
                        payback = 0;
                    else {
                        payback = plugin.getCostManager().calculateCost(regionCount - 1) * plugin.getConfig().getDouble("Payback");
                        // System.out.println(payback);
                        if (payback > 0)
                            plugin.getVaultHandler().give(player.getUniqueId(), payback);
                    }
                }
            }
            plugin.getWgHandler().unclaim(pr.getWorld(), pr.getName());

            // remove possible homes
            LPlayer lPlayer = plugin.getPlayerManager().get(pr.getOwner());
            if (lPlayer != null) {
                Location home = lPlayer.getHome();
                if (home != null) {
                    if (pr.getWGLand().contains(home.getBlockX(), home.getBlockY(), home.getBlockZ())) {
                        lm.sendMessage(player, lm.getString("Commands.SetHome.removed"));
                        plugin.getPlayerManager().get(pr.getOwner()).setHome(null);
                    }
                }
            }

            if (plugin.getConfig().getBoolean("Particles.unclaim.enabled"))
                OwnedLand.highlightLand(player,
                        Particle.valueOf(plugin.getConfig().getString("Particles.unclaim.particle").toUpperCase()));

            // Remove possible advertisements
            plugin.getOfferManager().removeOffer(pr.getName());

            lm.sendMessage(player, lm.getString("Commands.Unclaim.success")
                    .replace("%chunk%", OwnedLand.getName(pr.getChunk()))
                    .replace("%location%", Util.getLocationFormatted(pr.getChunk()))
                    .replace("%world%", pr.getWorld().getName())
                    .replace("%money%", (Options.isVaultEnabled() ? plugin.getVaultHandler().format(payback) : "-eco disabled-")));


            plugin.getMapManager().updateAll();
        }

    }

}
