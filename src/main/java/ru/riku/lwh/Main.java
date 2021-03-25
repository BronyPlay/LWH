package ru.riku.lwh;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Main extends JavaPlugin implements CommandExecutor {

    private static Economy economy = null;
    private static Chat chat = null;
    private static Main plugin;

    public static Economy getEconomy() {
        return economy;
    }
    public static Chat getChat() {
        return chat;
    }
    public static Main getInstance() {
        return plugin;
    }

    @Override
    public void onEnable() {
            plugin = this;
            if (setupEconomy()) {
              saveDefaultConfig();
              setupEconomy();
              setupChat();
             getInstance().getCommand("lightwallhack").setExecutor(this);
             Bukkit.getConsoleSender().sendMessage("Плагин LWH запущен.");
        } else {
            Bukkit.getConsoleSender().sendMessage("Vault не обнаружен, плагин отключается.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equals(args[0].toLowerCase())) {
                if (sender.isOp()) {
                    reloadConfig();
                    Bukkit.getConsoleSender().sendMessage("[LWH] Конфиг успешно перезагружен.");
                }
            }
        } else {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (p.hasPermission("lightwallhack.use")) {
                    Set<String> groups = getInstance().getConfig().getConfigurationSection(Utils.ConfigKeys.GROUPS.getPath()).getKeys(false);
                    if (groups.contains(getChat().getPrimaryGroup(p))) {

                        double group_cost = Utils.getDouble(Utils.ConfigKeys.GROUPS,
                                "." + getChat().getPrimaryGroup(p) + ".cost");

                        if (getEconomy().getBalance(((Player) sender).getPlayer()) < group_cost) {
                            String error = Utils.get(Utils.ConfigKeys.MESSAGES, "NotEnoughtMoney", true, true, p);
                            Utils.sendMessage(p, error.replace("%money", String.valueOf(group_cost - getEconomy().getBalance(p))));
                            return true;
                        }

                        int group_radius = Utils.getInt(Utils.ConfigKeys.GROUPS,
                                "." + getChat().getPrimaryGroup(p) + ".radius");
                        int group_active = Utils.getInt(Utils.ConfigKeys.GROUPS,
                                "." + getChat().getPrimaryGroup(p) + ".active");
                        int group_cdTime = Utils.getInt(Utils.ConfigKeys.GROUPS,
                                "." + getChat().getPrimaryGroup(p) + ".cooldown");

                        getEconomy().withdrawPlayer(p, group_cost);
                        glowing(p, group_radius, group_active, group_cdTime);
                    } else {
                        Utils.sendMessage(p, Utils.ConfigKeys.MESSAGES, "notGroupInGroups", true, false);
                    }
                } else {
                    Utils.sendMessage(p, Utils.ConfigKeys.MESSAGES, "noPerm", true, false);
                }
            } else {
                sender.sendMessage(color(getConfig().getString("LightWallHack.Messages.noConsole")));
            }
        }

        return true;
    }

    private void glowing(Player p, int radius, int active, long cdTime) {
        ArrayList<String> players = new ArrayList<>();
        HashMap<String, Long> list = new HashMap<>();
        if (list.containsKey(p.getName())) {
            long cd = ((list.get(p.getName()) / 1000) + cdTime) - (System.currentTimeMillis() / 1000);
            if (cd > 0) {
                p.sendMessage(color(getConfig().getString("LightWallHack.Messages.Cooldown").replace("%cd%", String.valueOf(cd))));
            } else {
                list.remove(p.getName());
            }
        } else {
            for (Entity entity : p.getNearbyEntities(radius, radius, radius)) {
                if (entity.getType().equals(EntityType.PLAYER)) {
                    players.add(entity.getName());
                    addGlow((Player) entity, p, true);
                    list.put(p.getName(), cdTime);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        addGlow((Player) entity, p, false);
                    }, 20 * active);
                }
            }
            if (players.size() > 0) {
                p.sendMessage(color(getConfig().getString("LightWallHack.Messages.WHCount").replace("%count%", String.valueOf(players.size()))));
            } else {
                Utils.sendMessage(p, Utils.ConfigKeys.MESSAGES, "notFound", true, false);
            }
        }
    }

    public void addGlow(Player player, Player who, boolean bool) {
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, player.getEntityId());
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);
            watcher.setEntity(player);
            if (bool) {
                watcher.setObject(0, serializer, (byte) (0x40)); // 0x40 - glow, 0x0 - off
            } else {
                watcher.setObject(0, serializer, (byte) (0x0)); // 0x40 - glow, 0x0 - off
            }
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            pm.sendServerPacket(who, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private static boolean setupEconomy() {
        if (getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getInstance().getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            } else {
                economy = rsp.getProvider();
                return economy != null;
            }
        } else {
            return false;
        }
    }

    private static void setupChat() {
        if (getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Chat> rsp = getInstance().getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                chat = rsp.getProvider();
            }
        }
    }

}
