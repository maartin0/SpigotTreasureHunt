package me.maartin0.treasurehunt.game;

import me.maartin0.treasurehunt.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Vault extends Interactable {
    public Vault(@NotNull TreasureHunt hunt, @NotNull Block block) {
        super(hunt, block);
    }
    final static String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
    static String ordinal(int i) {
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + suffixes[i % 10];
        };
    }
    @Override
    synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItemInMainHand();
        if (TreasureHunt.isItem(item)) {
            int amount = item.getAmount();
            inventory.remove(item);
            try {
                hunt.addScore(player, amount);
            } catch (IOException | InvalidConfigurationException e) {
                Logger.sendPlayerGenericErrorMessage(player);
                Logger.logWarning("An error occurred while handling an interact event:");
                e.printStackTrace();
                return;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(Collections.emptyList());
                meta.setCustomModelData(null);
                item.setItemMeta(meta);
            }
            Location location = player.getLocation();
            World world = location.getWorld();
            assert world != null;
            world.dropItem(location, item);
            Logger.sendPlayerMessage(player, "Added %s points to your score!".formatted(amount));
            playDing(player);
        }
        int score = hunt.getScore(player);
        List<UUID> scores = hunt.getScores()
                .stream()
                .map(Map.Entry::getKey) // Get just UUIDs from sorted entries
                .toList();
        int index = scores.indexOf(player.getUniqueId());
        int position = index + 1; // Indexes start from 0, so 0 = 1st place
        if (position == 0) {
            Logger.sendPlayerMessage(player, "You don't have any points, collect some treasure first!");
            return;
        }
        Logger.sendPlayerMessage(player,
                "You are currently in %s place with %s point(s)".formatted(
                    ordinal(position),
                    score));
        if (index > 0) {
            UUID nextPlayerId = scores.get(index - 1);
            OfflinePlayer nextPlayer = Bukkit.getOfflinePlayer(nextPlayerId);
            String name = nextPlayer.getName();
            int distance = hunt.getScore(nextPlayer) - score;
            Logger.sendPlayerMessage(player, "You are %s point(s) behind %s".formatted(distance, name));
        }
        event.setCancelled(true);
    }
}
