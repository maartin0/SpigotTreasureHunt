package me.maartin0.treasurehunt.util;

import me.maartin0.treasurehunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class Command implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            return this.onCommand(sender, command.getName(), args);
    }

    public abstract boolean onCommand(@NotNull CommandSender sender, @NotNull String commandName, @NotNull String[] args);

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return null;
        @Nullable List<String> result = onTabComplete(sender, command.getName(), args);
        if (result == null) return null;
        String start = args[args.length - 1];
        return start.length() == 0
                ? result
                : result.stream()
                    .filter(s -> s.startsWith(start))
                    .toList();
    }

    @Nullable
    abstract public List<String> onTabComplete(@NotNull CommandSender player, @NotNull String commandName, @NotNull String[] args);

    public void register(String commandName) {
        PluginCommand command = Main.plugin.getCommand(commandName);
        if (command == null) {
            Bukkit.getLogger().warning("Unable to register command with name '" + commandName + "': null");
        } else {
            command.setExecutor(this);
        }
    }
}
