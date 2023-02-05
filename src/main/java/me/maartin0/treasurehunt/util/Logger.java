package me.maartin0.treasurehunt.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Logger {
    public static final String prefix = ChatColor.GREEN + "";
    public static void sendPlayerMessage(CommandSender player, String message) {
        player.sendMessage(prefix + message);
    }
    public static void sendPlayerGenericErrorMessage(CommandSender player) {
        sendPlayerMessage(player, "An unknown error occurred.");
    }
    public static void logWarning(String message) {
        Bukkit.getLogger().warning(message);
    }
    public static void logInfo(String message) {
        Bukkit.getLogger().info(message);
    }
}
