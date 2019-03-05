package biz.princeps.landlord.commands.teleport;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import biz.princeps.landlord.Landlord;
import biz.princeps.landlord.commands.LandlordCommand;

public class RandomTeleport extends LandlordCommand {

	private Random random = new Random();

	public void onRandomTeleport(Player player) {
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
				// no available locations!! say something sad
				return;
			}
		}

		player.teleport(randomLocation);
		// say something nice because they are teleported
		
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
