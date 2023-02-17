package me.maartin0.treasurehunt.game;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.maartin0.treasurehunt.util.Logger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class Chest extends Interactable {
    private final static String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM1YzE2NTUxYWYzOTI4MWQ4MzA3ODAwZjg1NGMxOGRiNTg3NDdhMWNiYjAxMTYyODY1OGZjYzI1NWExM2M3YyJ9fX0=";
    private final static URL textureUrl;
    static {
        try {
            textureUrl = new URL("https://textures.minecraft.net/texture/535c16551af39281d8307800f854c18db58747a1cbb011628658fcc255a13c7c");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static GameProfile profile;
    public Chest(@NotNull TreasureHunt hunt, @NotNull Block block) {
        super(hunt, block);
    }
    public static boolean isChest(Block block) {
        Material type = block.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) return false;
        Skull skull = (Skull) block.getState();
        PlayerProfile profile1 = skull.getOwnerProfile();
        if (profile1 == null) return false;
        URL skinUrl = profile1.getTextures().getSkin();
        if (skinUrl == null) return false;
        return skinUrl.getPath().contains(textureUrl.getPath());
    }
    @Override
    synchronized void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = getItem();

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

        // Send message to player
        Logger.sendPlayerMessage(player,
                "Congratulations, you found a piece of "
                        + ChatColor.GOLD + ChatColor.MAGIC + "#"
                        + ChatColor.RESET + ChatColor.GOLD + "treasure"
                        + ChatColor.MAGIC + "#"
                        + ChatColor.RESET + ChatColor.GREEN + "!");
        event.setCancelled(true);
    }
    public static ItemStack getItemStack() throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);
        updateProfile();
        assert itemMeta != null;
        Method mtd = itemMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
        mtd.setAccessible(true);
        mtd.invoke(itemMeta, profile);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    private static void updateProfile() {
        if (profile == null) {
            profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
        }
    }
    private void setTexture() throws IllegalAccessException, NoSuchFieldException {
        block.setType(Material.PLAYER_HEAD, false);
        updateProfile();
        Skull skull = (Skull) block.getState();
        Field profileField = skull.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        profileField.set(skull, profile);
        skull.update();
    }
    private void setItem(@NotNull ItemStack itemStack) throws IOException, InvalidConfigurationException {
        hunt.setItem(block.getLocation(), itemStack);
    }
    private ItemStack getItem() {
        return hunt.getItem(block.getLocation());
    }
    private void deleteItem() throws IOException, InvalidConfigurationException {
        hunt.deleteItem(block.getLocation());
    }
    public boolean mark() {
        Inventory inventory = ((InventoryHolder) block.getState()).getInventory();

        Location location = block.getLocation();
        World world = location.getWorld();
        assert world != null;

        // Get first item, Drop all other items from inventory
        ItemStack item = null;
        boolean found = false;
        for (ItemStack i : inventory.getContents()) {
            if (i == null) continue;
            if (found) world.dropItem(location, i);
            else {
                found = true;
                item = i;
            }
        }

        if (item == null) return false;

        // Store item in chest NBT
        try {
            setItem(item);
        } catch (IOException | InvalidConfigurationException e) {
            Logger.logWarning("An error occurred while storing item data:");
            e.printStackTrace();
            return false;
        }

        ItemStack toDrop = new ItemStack(block.getType());

        // Change block to treasure chest texture
        try {
            setTexture();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.logWarning("An error occurred while changing a blocks state:");
            e.printStackTrace();
            return false;
        }

        world.dropItem(location, toDrop);
        return true;
    }
}
