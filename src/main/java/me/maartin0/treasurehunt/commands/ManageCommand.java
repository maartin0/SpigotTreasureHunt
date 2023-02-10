package me.maartin0.treasurehunt.commands;

import me.maartin0.treasurehunt.game.Chest;
import me.maartin0.treasurehunt.game.TreasureHunt;
import me.maartin0.treasurehunt.util.Command;
import me.maartin0.treasurehunt.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ManageCommand extends Command implements Listener {
    Map<UUID, Runnable> confirmSessions = new ConcurrentHashMap<>();
    final static String permission = "treasurehunt.manage";
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        confirmSessions.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        confirmSessions.remove(event.getPlayer().getUniqueId());
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String commandName, @NotNull String[] args) {
        if (!sender.hasPermission(permission)) {
            TreasureHunt hunt = TreasureHunt.get(args.length == 0 ? null : args[0]);
            if (hunt == null) Logger.sendPlayerMessage(sender, "No treasure hunts available");
            else this.scores(hunt, sender);
        } if (args.length != 0 && args[0].equalsIgnoreCase("create")) {
            create(args.length > 1 ? args[1] : null, sender);
        } else if (args.length != 0 && args[0].equalsIgnoreCase("confirm")) {
            confirm(sender);
        } else {
            BiConsumer<TreasureHunt, CommandSender> command =
                    (args.length == 0 || args[0].equalsIgnoreCase("scores"))
                            ? this::scores
                            : (args[0].equalsIgnoreCase("delete"))
                            ? this::delete
                            : (args[0].equalsIgnoreCase("mark"))
                            ? this::mark
                            : null;
            if (command == null) return false;
            TreasureHunt hunt = TreasureHunt.get(args.length > 1 ? args[1] : null);
            if (hunt == null) Logger.sendPlayerMessage(sender, "Create a hunt first with '/hunt create'!");
            else command.accept(hunt, sender);
        }
        return true;
    }
    void create(@Nullable String name, @NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Logger.sendPlayerMessage(sender, "You can only use this command as a player");
            return;
        }
        Block block = getTargetBlock(player);
        if (block.isEmpty() || block.isLiquid()) {
            Logger.sendPlayerMessage(sender, "The block you're currently looking at cannot be used as a vault");
            return;
        }
        confirmSessions.put(player.getUniqueId(), () -> {
            try {
                TreasureHunt.create(name, block);
            } catch (IOException | InvalidConfigurationException e) {
                Logger.sendPlayerGenericErrorMessage(player);
                Logger.logWarning("An error occurred while trying to create a treasure hunt:");
                e.printStackTrace();
                return;
            }
            Logger.sendPlayerMessage(player, "Success!");
        });
        sendConfirmMessage(sender);
    }
    void delete(@NotNull TreasureHunt hunt, @NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Logger.sendPlayerMessage(sender, "You can only use this command as a player");
            return;
        }
        confirmSessions.put(player.getUniqueId(), () -> {
            try {
                hunt.delete();
            } catch (IOException | InvalidConfigurationException e) {
                Logger.sendPlayerGenericErrorMessage(sender);
                Logger.logWarning("An error occurred while trying to delete a treasure hunt from storage:");
                e.printStackTrace();
                return;
            }
            Logger.sendPlayerMessage(sender, "Success");
        });
        sendConfirmMessage(sender);
    }

    void scores(@NotNull TreasureHunt hunt, @NotNull CommandSender sender) {
        List<Map.Entry<UUID, Integer>> scores = hunt.getScores();
        scores.forEach((Map.Entry<UUID, Integer> entry) -> {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            int score = entry.getValue();
            Logger.sendPlayerMessage(sender, name + ChatColor.GRAY + ":" + ChatColor.GOLD + " " + score);
        });
        if (scores.size() == 0) {
            Logger.sendPlayerMessage(sender, "No data available");
        }
    }
    public static boolean validBlock(Block block) {
        Material type = block.getType();
        return !type.isAir()
                && type.isSolid()
                && type.isInteractable()
                && block.getState() instanceof InventoryHolder;
    }
    static Block getTargetBlock(Player player) {
        return player.getTargetBlock(Set.of(Material.AIR, Material.WATER, Material.LAVA), 4);
    }
    static void sendConfirmMessage(CommandSender sender) {
        sender.sendMessage("Are you sure you want to do this? Run '/hunt confirm' if you're sure");
    }
    void mark(@NotNull TreasureHunt hunt, @NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Logger.sendPlayerMessage(sender, "You can only use this command as a player");
            return;
        }
        Block block = getTargetBlock(player);
        if (!validBlock(block) || hunt.vault.block.getLocation().equals(block.getLocation())) Logger.sendPlayerMessage(sender, "Invalid inventory");
        else if (new Chest(hunt, block).mark()) Logger.sendPlayerMessage(sender, "Success");
        else Logger.sendPlayerMessage(player, "Invalid inventory or item");
    }

    void confirm(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Logger.sendPlayerMessage(sender, "You can only use this command as a player");
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!confirmSessions.containsKey(uuid)) {
            Logger.sendPlayerMessage(sender, "Nothing to confirm");
            return;
        }
        confirmSessions.get(uuid).run();
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender player, @NotNull String commandName, @NotNull String[] args) {
        if (args.length < 2 && player.hasPermission(permission))
            return List.of("create", "delete", "scores", "mark", "confirm");
        if (args.length < 3)
            return TreasureHunt.getNames().stream().toList();
        return null;
    }
}
