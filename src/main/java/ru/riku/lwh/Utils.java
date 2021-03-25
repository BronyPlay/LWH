package ru.riku.lwh;

import com.destroystokyo.paper.Title;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public enum ConfigKeys {

        MESSAGES("LightWallHack.messages"),
        GROUPS("LightWallHack.groups");

        private final String path;

        ConfigKeys(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static String get(ConfigKeys key, String path, boolean color, boolean placeholders, Player p) {
        String string = Main.getInstance().getConfig().getString(key.getPath() + (path != null ? "." + path : ""));
        if (string != null) {
            if (color) {
                string = ChatColor.translateAlternateColorCodes('&', string);
            }
            if (placeholders) {
                string = PlaceholderAPI.setPlaceholders(p, string);
            }
            return string;
        } else {
            return "Нулл";
        }

    }

    public static int getInt(ConfigKeys key, String path) {
        int ints = Main.getInstance().getConfig().getInt(key.getPath() + "." + path, -1);
        return ints;
    }

    public static Double getDouble(ConfigKeys key, String path) {
        double ints = Main.getInstance().getConfig().getDouble(key.getPath() + "." + path, -1);
        if (ints != -1) {
            return ints;
        } else {
            return 0.0;
        }
    }

    public static List<String> getList(ConfigKeys key, String path, boolean color, boolean placeholders, Player p) {
        List<String> string = Main.getInstance().getConfig().getStringList(key.getPath() + "." + path);
        if (string != null) {

            if (color) {
                string = color(string);
            }
            if (placeholders) {
                string = PlaceholderAPI.setPlaceholders(p, string);
            }

            return string;
        } else {
            return null;
        }
    }

    public static List<String> color(List<String> msg) {
        List<String> list = new ArrayList<>();
        for (String a : msg) {
            list.add(ChatColor.translateAlternateColorCodes('&', a));
        }
        return list;
    }

    public static void sendMessage(Player p, ConfigKeys key, String path, boolean color, boolean placeholders) {
        String[] message = get(key, path, color, placeholders, p).split(":");
        switch (message[0].toLowerCase()) {
            case "actionbar": {
                p.sendActionBar(message[1].replace("%n", ""));
                break;
            }
            case "title": {
                String[] title = message[1].split("%n");
                p.sendTitle(
                        Title.builder()
                                .title(title[0] != null ? title[0] : "")
                                .subtitle(title[1] != null ? title[1] : "")
                                .build()
                );
                break;
            }
            default: {
                p.sendMessage(message[0].replace("%n", ""));
                break;
            }
        }
    }

    public static void sendMessage(Player p, String mes) {
        String[] message = mes.split(":");

        switch (message[0].toLowerCase()) {
            case "actionbar": {
                p.sendActionBar(message[1]);
                break;
            }
            case "title": {
                String[] title = message[1].split("%n");
                p.sendTitle(
                        Title.builder()
                                .title(title[0] != null ? title[0] : "")
                                .subtitle(title[1] != null ? title[1] : "")
                                .build()
                );
                break;
            }
            default: {
                p.sendMessage(message[0]);
                break;
            }
        }
    }



}
