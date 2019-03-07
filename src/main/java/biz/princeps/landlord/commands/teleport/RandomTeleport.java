package biz.princeps.landlord.commands.teleport;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import biz.princeps.landlord.Landlord;
import biz.princeps.landlord.commands.LandlordCommand;

public class RandomTeleport extends LandlordCommand {

	private Random random = new Random();

	private Set<UUID> players = new HashSet<>();

	public void onRandomTeleport(Player player) {
		UUID playerUUID = player.getUniqueId();
		if (players.contains(playerUUID)) {
			lm.sendMessage(player, lm.getString("Commands.RandomTeleport.tooRecent"));
			return;
		}

		int xMax = Landlord.getInstance().getConfig().getInt("CommandSettings.RandomTeleport.x");
		int zMax = Landlord.getInstance().getConfig().getInt("CommandSettings.RandomTeleport.z");

		Location randomLocation = getRandomTeleportLocation(player, xMax, zMax);
		int i = 0;
		while (true) {
			if (randomLocation == null) {
				randomLocation = getRandomTeleportLocation(player, xMax, zMax);
			} else {
				break;
			}
			i++;
			if (i > 100) {
				lm.sendMessage(player, lm.getString("Commands.RandomTeleport.noneAvailable"));
				return;
			}
		}

		player.teleport(randomLocation);
		lm.sendMessage(player, lm.getString("Commands.RandomTeleport.success"));

		players.add(playerUUID);
		Bukkit.getScheduler().runTaskLater(Landlord.getInstance(), () -> {
			players.remove(playerUUID);
		}, 20 * 60);
	}

	public Location getRandomTeleportLocation(Player player, int xMax, int zMax) {
		int x = xMax * (int) random.nextDouble();
		int z = zMax - ((zMax * 2) * (int) random.nextDouble());

		Block block = player.getWorld().getHighestBlockAt(x, z);
		if (block.getY() < 62) {
			return null;
		} else if (block.isLiquid()) {
			return null;
		} else if (!Landlord.getInstance().getWgHandler().canClaim(player, block.getChunk())) {
			return null;
		}

		return block.getLocation();
	}

}
