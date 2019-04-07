package biz.princeps.landlord.guis;

import biz.princeps.landlord.Landlord;
import biz.princeps.landlord.api.Options;
import biz.princeps.landlord.api.events.LandManageEvent;
import biz.princeps.landlord.flags.LLFlag;
import biz.princeps.landlord.manager.LangManager;
import biz.princeps.landlord.persistent.LPlayer;
import biz.princeps.landlord.util.Mobs;
import biz.princeps.landlord.util.OwnedLand;
import biz.princeps.lib.gui.ConfirmationGUI;
import biz.princeps.lib.gui.MultiPagedGUI;
import biz.princeps.lib.gui.simple.AbstractGUI;
import biz.princeps.lib.gui.simple.Icon;
import co.aikar.taskchain.TaskChain;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldguard.protection.flags.Flags;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.*;

/**
 * Project: LandLord
 * Created by Alex D. (SpatiumPrinceps)
 * Date: 21/7/17
 */
public abstract class AbstractManage extends AbstractGUI {

    private static int SIZE;
    private static Set<String> toggleMobs = new HashSet<>();

    static {
        ConfigurationSection section = Landlord.getInstance().getConfig().getConfigurationSection("Manage");

        Set<String> keys = section.getKeys(true);

        int trues = 0;
        for (String key : keys) {
            if (section.getBoolean(key))
                trues++;
        }

        SIZE = (trues / 9 + (trues % 9 == 0 ? 0 : 1)) * 9;

        toggleMobs.addAll(Landlord.getInstance().getConfig().getStringList("Manage.mob-spawning.toggleableMobs"));
    }

    private List<OwnedLand> regions;
    private LangManager lm;
    private Landlord plugin;

    public AbstractManage(Player player, String header, List<OwnedLand> land) {
        super(player, SIZE, header);
        this.regions = land;
        this.plugin = Landlord.getInstance();
        this.lm = plugin.getLangManager();
    }

    public AbstractManage(Player player, MultiPagedGUI landGui, String header, List<OwnedLand> land) {
        super(player, SIZE + 9, header, landGui);
        this.regions = land;
        this.plugin = Landlord.getInstance();
        this.lm = plugin.getLangManager();
    }

    @Override
    protected void create() {
        List<String> regenerateDesc = lm.getStringList("Commands.Manage.Regenerate.description");
        List<String> greetDesc = lm.getStringList("Commands.Manage.SetGreet.description");
        List<String> farewellDesc = lm.getStringList("Commands.Manage.SetFarewell.description");


        int position = 0;

        if (regions.size() < 1)
            return;

        OwnedLand land = regions.get(0);


        for (LLFlag iFlag : land.getFlags()) {

            // For every IFlag of the land we wanna display an icon in the gui IF the flag is enabled for change
            String flagName = iFlag.getWGFlag().getName();
            String title = lm.getRawString("Commands.Manage.Allow" + flagName.substring(0, 1).toUpperCase() + flagName.substring(1) + ".title");
            List<String> description = lm.getStringList("Commands.Manage.Allow" + flagName.substring(0, 1).toUpperCase() + flagName.substring(1) + ".description");

            if (plugin.getConfig().getBoolean("Manage." + flagName + ".enable") &&
                    player.hasPermission("landlord.player.manage." + flagName)) {

                int finalPosition = position;
                this.setIcon(position, new Icon(createItem(iFlag.getMaterial(), 1,
                        title, formatList(description, iFlag.getStatus())))
                        .addClickAction((p) -> {
                            for (OwnedLand region : regions) {
                                //TODO clean this mess up
                                for (LLFlag llFlag : region.getFlags()) {
                                    if (llFlag.getWGFlag().equals(iFlag.getWGFlag())) {
                                        for (OwnedLand ownedLand : regions) {
                                            String oldstatus = iFlag.getStatus();
                                            iFlag.toggle();
                                            LandManageEvent landManageEvent = new LandManageEvent(player, ownedLand,
                                                    iFlag.getWGFlag(), oldstatus, iFlag.getStatus());
                                            Bukkit.getPluginManager().callEvent(landManageEvent);
                                        }
                                        break;
                                    }
                                }
                            }
                            updateLore(finalPosition, formatList(description, iFlag.getStatus()));
                        })
                );
                position++;
            }
        }

        // Reminder: Regenerate is not implemented in Manageall, cos it might cos some trouble. Calculating costs might be a bit tedious
        if (plugin.getConfig().getBoolean("Manage.regenerate.enable") && regions.size() == 1 &&
                player.hasPermission("landlord.player.manage.regenerate")) {
            double cost = plugin.getConfig().getDouble("ResetCost");
            this.setIcon(position, new Icon(createItem(Material.BARRIER, 1,
                    lm.getRawString("Commands.Manage.Regenerate.title"), formatList(regenerateDesc, (Options.isVaultEnabled() ? plugin.getVaultHandler().format(cost) : "-1"))))
                    .addClickAction((p) -> {
                        if (land.isOwner(player.getUniqueId())) {
                            ConfirmationGUI confi = new ConfirmationGUI(p, lm.getRawString("Commands.Manage.Regenerate.confirmation")
                                    .replace("%cost%", (Options.isVaultEnabled() ? plugin.getVaultHandler().format(cost) : "-1")),
                                    (p1) -> {
                                        boolean flag = false;
                                        if (Options.isVaultEnabled())
                                            if (plugin.getVaultHandler().hasBalance(player.getUniqueId(), cost)) {
                                                plugin.getVaultHandler().take(player.getUniqueId(), cost);
                                                flag = true;
                                            } else {
                                                lm.sendMessage(player, lm.getString("Commands.Manage.Regenerate.notEnoughMoney")
                                                        .replace("%cost%", plugin.getVaultHandler().format(cost))
                                                        .replace("%name%", land.getName()));
                                            }
                                        else {
                                            flag = true;
                                        }
                                        if (flag) {
                                            if (land.isOwner(player.getUniqueId())) {
                                                LandManageEvent landManageEvent = new LandManageEvent(player, land, null, "REGENERATE", "REGENERATE");
                                                Bukkit.getPluginManager().callEvent(landManageEvent);
                                                player.getWorld().regenerateChunk(land.getChunk().getX(), land.getChunk().getZ());
                                                lm.sendMessage(player, lm.getString("Commands.Manage.Regenerate.success")
                                                        .replace("%land%", land.getName()));
                                                display();
                                            }
                                        }

                                    }, (p2) -> {
                                lm.sendMessage(player, lm.getString("Commands.Manage.Regenerate.abort")
                                        .replace("%land%", land.getName()));
                                display();
                            }, this);
                            confi.setConfirm(lm.getRawString("Confirmation.accept"));
                            confi.setDecline(lm.getRawString("Confirmation.decline"));

                            confi.display();
                        }
                    })
            );
            position++;
        }

        // Set greet icon
        if (plugin.getConfig().getBoolean("Manage.setgreet.enable") &&
                player.hasPermission("landlord.player.manage.setgreet")) {
            String currentGreet = land.getWGLand().getFlag(Flags.GREET_MESSAGE);
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.setgreet.item")), 1,
                    lm.getRawString("Commands.Manage.SetGreet.title"), formatList(greetDesc, currentGreet)))
                    .addClickAction(((p) -> {
                        p.closeInventory();
                        ComponentBuilder builder = new ComponentBuilder(lm.getString("Commands.Manage.SetGreet.clickMsg"));
                        if (regions.size() > 1)
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setgreetall "));
                        else
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setgreet "));

                        p.spigot().sendMessage(builder.create());
                    }))
            );
            position++;
        }

        if (plugin.getConfig().getBoolean("Manage.mob-spawning.enable") &&
                player.hasPermission("landlord.player.manage.mobspawn")) {
            String title = lm.getRawString("Commands.Manage.AllowMob-spawning.title");
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.mob-spawning.item")), 1,
                    title, lm.getStringList("Commands.Manage.AllowMob-spawning.description")))
                    .addClickAction((p) -> {
                        // Open a new gui with spawneggs where you can manage the spawns by clicking on them

                        List<Icon> icons = new ArrayList<>();
                        List<String> lore = lm.getStringList("Commands.Manage.AllowMob-spawning.toggleItem.description");

                        MultiPagedGUI gui = new MultiPagedGUI(p, 4, title, icons, this) {
                        };

                        for (Mobs m : Mobs.values()) {
                            // Skip mob if its not in the list, because that means this mob should not be manageable
                            if (!toggleMobs.contains(m.name())) {
                                continue;
                            }

                            ItemStack spawnEgg = new ItemStack(m.getEgg());
                            SpawnEggMeta meta = (SpawnEggMeta) spawnEgg.getItemMeta();
                            meta.setDisplayName(lm.getRawString("Commands.Manage.AllowMob-spawning.toggleItem.title").replace("%mob%", m.getName()));

                            Set<EntityType> flag = land.getWGLand().getFlag(Flags.DENY_SPAWN);
                            String state;
                            if (flag != null)
                                state = (flag.contains(m.getWGType()) ? "DENY" : "ALLOW");
                            else
                                state = "ALLOW";

                            List<String> formattedLore = new ArrayList<>();
                            for (String s : lore) {
                                formattedLore.add(s.replace("%mob%", m.getName()).replace("%value%", state));
                            }

                            meta.setLore(formattedLore);
                            spawnEgg.setItemMeta(meta);

                            Icon ic = new Icon(spawnEgg);
                            ic.addClickAction((clickingPlayer) -> {

                                for (OwnedLand region : regions) {
                                    Set<EntityType> localFlag = region.getWGLand().getFlag(Flags.DENY_SPAWN);
                                    // Toggle spawning of specific mob
                                    if (localFlag != null) {
                                        if (localFlag.contains(m.getWGType()))
                                            localFlag.remove(m.getWGType());
                                        else {
                                            localFlag.add(m.getWGType());
                                        }
                                        LandManageEvent landManageEvent = new LandManageEvent(player, land, Flags.MOB_SPAWNING, null, localFlag);
                                        Bukkit.getPluginManager().callEvent(landManageEvent);
                                    } else {
                                        Set<EntityType> set = new HashSet<>();
                                        set.add(m.getWGType());
                                        region.getWGLand().setFlag(Flags.DENY_SPAWN, set);
                                        LandManageEvent landManageEvent = new LandManageEvent(player, land, Flags.MOB_SPAWNING, null, set);
                                        Bukkit.getPluginManager().callEvent(landManageEvent);
                                    }
                                }

                                Set<EntityType> newFlag = regions.get(0).getWGLand().getFlag(Flags.DENY_SPAWN);
                                // update icon text
                                String iconState;
                                if (newFlag != null)
                                    iconState = (newFlag.contains(m.getWGType()) ? "DENY" : "ALLOW");
                                else
                                    iconState = "ALLOW";

                                List<String> newLore = new ArrayList<>();
                                for (String s : lore) {
                                    newLore.add(s.replace("%mob%", m.getName()).replace("%value%", iconState));
                                }

                                // System.out.println(newLore + " :");
                                ic.setLore(newLore);
                                gui.refresh();
                            });
                            icons.add(ic);
                        }

                        gui.display();
                    }));
            position++;
        }
        // set farewell icon
        if (plugin.getConfig().getBoolean("Manage.setfarewell.enable") &&
                player.hasPermission("landlord.player.manage.setfarewell")) {
            String currentFarewell = land.getWGLand().getFlag(Flags.FAREWELL_MESSAGE);
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.setfarewell.item")), 1,
                    lm.getRawString("Commands.Manage.SetFarewell.title"), formatList(farewellDesc, currentFarewell)))
                    .addClickAction(((p) -> {
                        p.closeInventory();
                        ComponentBuilder builder = new ComponentBuilder(lm.getString("Commands.Manage.SetFarewell.clickMsg"));
                        if (regions.size() > 1)
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setfarewellall "));
                        else
                            builder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/land manage setfarewell "));

                        p.spigot().sendMessage(builder.create());
                    }))
            );
            position++;
        }

        // set friends icon
        if (plugin.getConfig().getBoolean("Manage.friends.enable") &&
                player.hasPermission("landlord.player.manage.friends")) {
            ItemStack skull = createSkull(player.getName(), lm.getRawString("Commands.Manage.ManageFriends.title"), lm.getStringList("Commands.Manage.ManageFriends.description"));
            Set<UUID> friends = land.getWGLand().getMembers().getUniqueIds();
            MultiPagedGUI friendsGui = new MultiPagedGUI(player, (int) Math.ceil((double) friends.size() / 9.0), lm.getRawString("Commands.Manage.ManageFriends.title"), new ArrayList<>(), this) {

            };
            friends.forEach(id -> friendsGui.addIcon(new Icon(createSkull(Bukkit.getOfflinePlayer(id).getName(),
                    Bukkit.getOfflinePlayer(id).getName(), formatFriendsSegment(id)))
                    .addClickAction((player) -> {
                        ConfirmationGUI confirmationGUI = new ConfirmationGUI(player, lm.getRawString("Commands.Manage.ManageFriends.unfriend")
                                .replace("%player%", Bukkit.getOfflinePlayer(id).getName()),
                                (p) -> {
                                    friendsGui.removeIcon(friendsGui.filter(Bukkit.getOfflinePlayer(id).getName()).get(0));
                                    if (regions.size() > 1)
                                        Bukkit.dispatchCommand(player, "land unfriendall " + Bukkit.getOfflinePlayer(id).getName());
                                    else
                                        Bukkit.dispatchCommand(player, "land unfriend " + Bukkit.getOfflinePlayer(id).getName());

                                    player.closeInventory();
                                    friendsGui.display();
                                },
                                (p) -> {
                                    player.closeInventory();
                                    friendsGui.display();
                                }, friendsGui);
                        confirmationGUI.setConfirm(lm.getRawString("Confirmation.accept"));
                        confirmationGUI.setDecline(lm.getRawString("Confirmation.decline"));
                        confirmationGUI.display();
                    })));


            this.setIcon(position, new Icon(skull)
                    .setName(lm.getRawString("Commands.Manage.ManageFriends.title"))
                    .addClickAction((p) -> friendsGui.display())
            );
            position++;
        }

        if (plugin.getConfig().getBoolean("Manage.unclaim.enable") &&
                player.hasPermission("landlord.player.manage.unclaim")) {
            this.setIcon(position, new Icon(createItem(Material.valueOf(plugin.getConfig().getString("Manage.unclaim.item")),
                    1, lm.getRawString("Commands.Manage.Unclaim.title"), lm.getStringList("Commands.Manage.Unclaim.description")))
                    .addClickAction(((player1) -> {
                        ConfirmationGUI gui = new ConfirmationGUI(player1, lm.getRawString("Commands.Manage.Unclaim.confirmationTitle").replace("%land%", land.getName()),
                                (p) -> {
                                    if (regions.size() > 1)
                                        Bukkit.dispatchCommand(p, "ll unclaimall");
                                    else
                                        Bukkit.dispatchCommand(p, "ll unclaim " + land.getName());

                                    p.closeInventory();
                                },
                                (p) -> {
                                    p.closeInventory();
                                    display();
                                }, this);
                        gui.setConfirm(lm.getRawString("Confirmation.accept"));
                        gui.setDecline(lm.getRawString("Confirmation.decline"));
                        gui.display();
                    })));
            position++;
        }
    }

    private List<String> formatFriendsSegment(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        Vector<String> vec = new Vector<>();

        TaskChain<?> chain = Landlord.newChain();
        chain.asyncFirst(() -> chain.setTaskData("lp", plugin.getPlayerManager().getOfflinePlayerSync(id)))
                .sync(() -> {
                    List<String> stringList = lm.getStringList("Commands.Manage.ManageFriends.friendSegment");
                    String lastseen;
                    if (op.isOnline()) {
                        lastseen = lm.getRawString("Commands.Info.online");
                    } else {
                        LPlayer lp = chain.getTaskData("lp");
                        if (lp != null)
                            lastseen = lp.getLastSeenAsString();
                        else
                            lastseen = "NaN";
                    }
                    stringList.forEach(s -> {
                        String ss = s.replace("%seen%", lastseen);
                        vec.add(ss);
                    });
                });

        return new ArrayList<>(vec);
    }

    private void updateLore(int index, List<String> lore) {
        this.getIcon(index).setLore(lore);
        this.refresh();
    }

    private List<String> formatList(Collection<String> allowDesc, String flag) {
        List<String> newList = new ArrayList<>();
        allowDesc.forEach(s -> newList.add(s.replace("%var%", flag)));
        return newList;
    }

    private ItemStack createItem(Material mat, int amount, String title, List<String> desc) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(title);
        itemMeta.setLore(desc);
        item.setItemMeta(itemMeta);
        return item;
    }

    private ItemStack createSkull(String owner, String displayname, List<String> lore) {
        ItemStack skull = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(owner);
        skullMeta.setDisplayName(displayname);
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);
        return skull;
    }
}
