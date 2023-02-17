package me.maartin0.treasurehunt.game;

import me.maartin0.treasurehunt.Main;
import org.bukkit.*;
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
import org.jetbrains.annotations.Nullable;

public abstract class Interactable {
    public final static class InteractionListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public synchronized void onPlayerInteract(PlayerInteractEvent event) {
            if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND)) return;
            Block block = event.getClickedBlock();
            Interactable interactable = getInteractable(block);
            if (interactable != null) interactable.onPlayerInteract(event);
        }
        @EventHandler(priority = EventPriority.HIGH)
        public synchronized void onBlockBreak(BlockBreakEvent event) {
            Block block = event.getBlock();
            Interactable interactable = getInteractable(block);
            if (interactable != null) interactable.onBlockBreak(event);
        }
        Location vaultLocation = null;
        @Nullable
        private Interactable getInteractable(Block block) {
            if (block == null) return null;
            if (Chest.isChest(block)) {
                return new Chest(TreasureHunt.get(), block);
            }
            if (vaultLocation == null) {
                vaultLocation = TreasureHunt.get().vault.block.getLocation();
            }
            if (block.getLocation().equals(vaultLocation)) {
                return TreasureHunt.get().vault;
            }
            return null;
        }
    }
    protected final TreasureHunt hunt;
    public final Block block;
    abstract void onPlayerInteract(PlayerInteractEvent event);
    void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }
    protected Interactable(@NotNull TreasureHunt hunt, @NotNull Block block) {
        this.hunt = hunt;
        this.block = block;
    }
    private static void playDing(Player player, float pitch, long delay) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, () -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.BLOCKS, 1F, pitch), delay);
    }
    protected static void playDing(Player player) {
        playDing(player, 1F, 0L);
        playDing(player, 1.12F, 8L);
    }
}
