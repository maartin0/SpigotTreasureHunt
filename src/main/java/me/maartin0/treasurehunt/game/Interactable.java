package me.maartin0.treasurehunt.game;

import me.maartin0.treasurehunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class Interactable {
    public final static class InteractionListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public synchronized void onPlayerInteract(PlayerInteractEvent event) {
            if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND)) return;
            Block block = event.getClickedBlock();
            if (block == null) return;
            Location location = block.getLocation();
            new HashSet<>(interactables).stream() // Copy set to prevent concurrency issues
                    .filter(i -> i.block.getLocation().equals(location))
                    .forEach(i -> i.onPlayerInteract(event));
        }
        @EventHandler(priority = EventPriority.HIGH)
        public synchronized void onBlockBreak(BlockBreakEvent event) {
            Block block = event.getBlock();
            Location location = block.getLocation();
            interactables.stream()
                    .filter(i -> i.block.getLocation().equals(location))
                    .forEach(i -> i.onBlockBreak(event));
        }
    }
    protected final TreasureHunt hunt;
    public final Block block;
    protected static Set<Interactable> interactables = new HashSet<>();
    abstract void onPlayerInteract(PlayerInteractEvent event);
    void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }
    protected Interactable(@NotNull TreasureHunt hunt, @NotNull Block block) {
        this.hunt = hunt;
        this.block = block;
        Location location = block.getLocation();
        if (interactables.stream().noneMatch(i -> i.block.getLocation().equals(location)))
            interactables.add(this);
    }
    private static void playDing(Player player, float pitch, long delay) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, () -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.BLOCKS, 1F, pitch), delay);
    }
    protected static void playDing(Player player) {
        playDing(player, 1F, 0L);
        playDing(player, 1.12F, 8L);
    }
}
