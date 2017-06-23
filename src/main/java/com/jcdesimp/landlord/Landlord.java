package com.jcdesimp.landlord;

import biz.princeps.lib.storage.AbstractDatabase;
import com.jcdesimp.landlord.configuration.CustomConfig;
import com.jcdesimp.landlord.landFlags.*;
import com.jcdesimp.landlord.landManagement.FlagManager;
import com.jcdesimp.landlord.landManagement.LandManager;
import com.jcdesimp.landlord.landManagement.ViewManager;
import com.jcdesimp.landlord.landMap.MapManager;
import com.jcdesimp.landlord.persistantData.db.MySQLDatabase;
import com.jcdesimp.landlord.persistantData.db.SQLiteDatabase;
import com.jcdesimp.landlord.pluginHooks.VaultHandler;
import com.jcdesimp.landlord.pluginHooks.WorldguardHandler;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Landlord
 */
public final class Landlord extends JavaPlugin {

    private AbstractDatabase db;
    private Landlord plugin;
    private MapManager mapManager;
    private WorldguardHandler wgHandler;
    private VaultHandler vHandler;
    private FlagManager flagManager;
    private ViewManager manageViewManager;
    private LandAlerter pListen;
    private LandManager landManager;

    private CustomConfig mainConfig;
    private CustomConfig messagesConfig;

    public static Landlord getInstance() {
        return (Landlord) Bukkit.getPluginManager().getPlugin("Landlord");
    }

    @Override
    public void onEnable() {
        plugin = this;
        mapManager = new MapManager(this);

        flagManager = new FlagManager(this);
        manageViewManager = new ViewManager();
        getServer().getPluginManager().registerEvents(mapManager, this);

        mainConfig = new CustomConfig(this, "config.yml", "config.yml");

        String lang = (mainConfig.get().getString("options.messagesFile").replace("/", "."));
        messagesConfig = new CustomConfig(this, "messages/" + lang, "messages/" + lang);

        pListen = new LandAlerter(plugin);
        if (getConfig().getBoolean("options.showLandAlerts", true)) {
            getServer().getPluginManager().registerEvents(pListen, this);
        }

        // database stuff
        if (getConfig().getBoolean("SQLite.enable")) {
            db = new SQLiteDatabase(this.getDataFolder() + "/database.db");
            ((SQLiteDatabase) db).setupDatabase();
        } else
            db = new MySQLDatabase(getConfig().getString("MySQL.Hostname"), getConfig().getInt("MySQL.Port"), getConfig().getString("MySQL.Database"), getConfig().getString("MySQL.User"), getConfig().getString("MySQL.Password"));

        landManager = new LandManager();


        // Command Executor
        getCommand("landlord").setExecutor(new LandlordCommandExecutor(this));

        //Worldguard Check
        if (!hasWorldGuard() && this.getConfig().getBoolean("worldguard.blockRegionClaim", true)) {
            getLogger().warning("Worldguard not found, worldguard features disabled.");
        } else if (hasWorldGuard()) {
            getLogger().info("Worldguard found!");
            wgHandler = new WorldguardHandler(getWorldGuard());
        }

        //Vault Check
        if (!hasVault() && this.getConfig().getBoolean("economy.enable", true)) {
            getLogger().warning("Vault not found, economy features disabled.");
        } else if (hasVault()) {
            getLogger().info("Vault found!");
            vHandler = new VaultHandler();
            if (!vHandler.hasEconomy()) {
                getLogger().warning("No economy found, economy features disabled.");
            }
        }

        this.getServer().getPluginManager().registerEvents(landManager, this);
        //Register default flags
        if (getConfig().getBoolean("enabled-flags.build")) {
            flagManager.registerFlag(new Build(this));
        }
        if (getConfig().getBoolean("enabled-flags.harmAnimals")) {
            flagManager.registerFlag(new HarmAnimals(this));
        }
        if (getConfig().getBoolean("enabled-flags.useContainers")) {
            flagManager.registerFlag(new UseContainers(this));
        }
        if (getConfig().getBoolean("enabled-flags.tntDamage")) {
            flagManager.registerFlag(new TntDamage(this));
        }
        if (getConfig().getBoolean("enabled-flags.useRedstone")) {
            flagManager.registerFlag(new UseRedstone(this));
        }
        if (getConfig().getBoolean("enabled-flags.openDoor")) {
            flagManager.registerFlag(new OpenDoor(this));
        }
        if (getConfig().getBoolean("enabled-flags.pvp")) {
            flagManager.registerFlag(new PVP(this));
        }
        if (getConfig().getBoolean("enabled-flags.setHome")) {
            flagManager.registerFlag(new SetHome(this));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            landManager.insertLandCount(p.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            landManager.removeLandCount(p.getUniqueId());
        }
        mapManager.removeAllMaps();
        manageViewManager.deactivateAll();
        pListen.clearPtrack();
        db.close();
    }

    @Override
    public FileConfiguration getConfig() {
        return mainConfig.get();
    }

    public FileConfiguration getMessageConfig() {
        return messagesConfig.get();
    }

    public FlagManager getFlagManager() {
        return flagManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public ViewManager getManageViewManager() {
        return manageViewManager;
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }


    /**
     * Provides access to the Landlord WorldGuardHandler
     *
     * @return ll wg handler
     */
    public WorldguardHandler getWgHandler() {
        return wgHandler;
    }

    public boolean hasWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !(plugin instanceof WorldGuardPlugin) || !this.getConfig().getBoolean("worldguard.blockRegionClaim", true)) {
            return false;
        }
        return true;
    }

    public boolean hasVault() {
        Plugin plugin = getServer().getPluginManager().getPlugin("Vault");
        return !(plugin == null || !this.getConfig().getBoolean("economy.enable", true));
    }

    public VaultHandler getvHandler() {
        return vHandler;
    }

    public AbstractDatabase getDatabase() {
        return db;
    }

    public LandManager getLandManager() {
        return landManager;
    }


}
