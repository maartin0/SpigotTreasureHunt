package me.maartin0.treasurehunt.game;

import me.maartin0.treasurehunt.util.Logger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Chest extends Interactable {
    private final static URL skinURL;

    static {
        try {
            skinURL = new URL("https://textures.minecraft.net/texture/535c16551af39281d8307800f854c18db58747a1cbb011628658fcc255a13c7c");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayerProfile profile;
    public Chest(@NotNull TreasureHunt hunt, @NotNull Block block) {
        super(hunt, block);
    }
    @Override
    synchronized void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = getItem();
        if (item == null) return;

        // Play sound effect
        Player player = event.getPlayer();
        playDing(player);

        // Spawn particles
        Location location = block.getLocation();
        World world = location.getWorld();
        assert world != null;
        world.spawnParticle(Particle.CLOUD, location, 8);

        // Destroy block
        block.setType(Material.AIR);

        // Drop item
        world.dropItem(location, item);

        // Delete stored item
        try {
            deleteItem();
        } catch (IOException | InvalidConfigurationException e) {
            Logger.logWarning("An error occurred while trying to handle an interact event:");
            e.printStackTrace();
        }
        interactables.remove(this);

        // Send message to player
        Logger.sendPlayerMessage(player,
                ChatColor.GREEN + "Congratulations, you found a piece of "
                        + ChatColor.GOLD + ChatColor.MAGIC + "#"
                        + ChatColor.RESET + ChatColor.GOLD + "treasure"
                        + ChatColor.MAGIC + "#"
                        + ChatColor.RESET + ChatColor.GREEN + "!");
        event.setCancelled(true);
    }
    @Override
    void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = getItem();
        if (item == null) return;
        Location location = block.getLocation();
        World world = location.getWorld();
        assert world != null;
        world.dropItem(location, item);
        try {
            deleteItem();
        } catch (IOException | InvalidConfigurationException e) {
            Logger.logWarning("An error occurred while trying to handle an block break event:");
        }
    }
    private void setTexture() throws IllegalAccessException, NoSuchFieldException {
        block.setType(Material.PLAYER_HEAD, false);
        if (profile == null) {
            profile = Bukkit.createPlayerProfile("ChestHead");
            profile.getTextures().setSkin(skinURL);
        }
        Skull skull = (Skull) block.getState();
        skull.setOwnerProfile(profile);
        skull.update();
    }
    private void setItem(@NotNull ItemStack itemStack) throws IOException, InvalidConfigurationException {
        hunt.setItem(block.getLocation(), itemStack);
    }
    @Nullable
    private ItemStack getItem() {
        return hunt.getItem(block.getLocation());
    }
    private void deleteItem() throws IOException, InvalidConfigurationException {
        hunt.deleteItem(block.getLocation());
    }
    public boolean mark() {
        Inventory inventory = ((InventoryHolder) block.getState()).getInventory();

        // Get first item in inventory
        Optional<ItemStack> treasure = Arrays.stream(inventory.getContents()).filter(Objects::nonNull).findFirst();
        if (treasure.isEmpty()) {
            interactables.remove(this);
            return false;
        }
        ItemStack item = treasure.get();
        Location location = block.getLocation();
        World world = location.getWorld();
        assert world != null;

        // Drop all other items from inventory
        boolean found = false;
        for (ItemStack i : inventory.getContents()) {
            if (i != null && (found || !i.equals(item))) {
                found = true;
                world.dropItem(location, i);
            }
        }

        // Store item in chest NBT
        try {
            setItem(item);
        } catch (IOException | InvalidConfigurationException e) {
            Logger.logWarning("An error occurred while storing item data:");
            e.printStackTrace();
            interactables.remove(this);
            return false;
        }

        ItemStack toDrop = new ItemStack(block.getType());

        // Change block to treasure chest texture
        try {
            setTexture();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.logWarning("An error occurred while changing a blocks state:");
            e.printStackTrace();
            interactables.remove(this);
            return false;
        }

        world.dropItem(location, toDrop);
        return true;
    }
}
