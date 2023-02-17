package me.maartin0.treasurehunt.game;

import me.maartin0.treasurehunt.util.Data;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class TreasureHunt {
    public final static String itemLore = ChatColor.MAGIC + "##" + ChatColor.RESET + " " + ChatColor.GOLD + "Treasure" + ChatColor.RESET + " " + ChatColor.MAGIC + "##";
    private static final Data data;
    static {
        try {
            data = new Data();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    private static TreasureHunt fromConfigurationSection() {
        TreasureHunt hunt = new TreasureHunt();

        String locationString = data.getString("vault_location");
        if (locationString == null) return null;
        Location location = deserializeLocation(locationString);
        if (location == null) return null;
        World world = location.getWorld();
        if (world == null) return null;
        Block block = world.getBlockAt(location);
        hunt.vault = new Vault(hunt, block);

        return hunt;
    }
    public synchronized void delete() throws IOException, InvalidConfigurationException {
        data.getKeys(false).forEach(k -> data.set(k, null));
        data.reload();
        vault.block.breakNaturally(new ItemStack(Material.DIAMOND_PICKAXE));
    }
    @NotNull
    private static String serializeLocation(@NotNull Location location) {
        return "%s %s %s %s".formatted(
                location.getWorld() == null
                        ? "world"
                        : location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
    @Nullable
    private static Location deserializeLocation(@NotNull String string) {
        String[] values = string.split(" ");
        if (values.length != 4) return null;
        World world = Bukkit.getWorld(values[0]);
        if (world == null) return null;
        int x, y, z;
        try {
            x = Integer.parseInt(values[1], 10);
            y = Integer.parseInt(values[2], 10);
            z = Integer.parseInt(values[3], 10);
        } catch (NumberFormatException e) {
            return null;
        }
        return new Location(world, x, y, z);
    }
    public synchronized void save() throws IOException, InvalidConfigurationException {
        data.set("vault_location", serializeLocation(vault.block.getLocation()));
        data.reload();
    }
    public Vault vault;
    public static void create(Block vaultLocation) throws IOException, InvalidConfigurationException {
        TreasureHunt hunt = get();
        if (hunt != null) {
            hunt.vault = new Vault(hunt, vaultLocation);
            hunt.save();
            return;
        }
        hunt = new TreasureHunt();
        hunt.vault = new Vault(hunt, vaultLocation);
        data.createSection("scores");
        hunt.save();
    }
    public static TreasureHunt get() {
        return fromConfigurationSection();
    }
    public static Set<String> getNames() {
        return data.getKeys(false);
    }
    public synchronized void setItem(@NotNull Location location, @NotNull ItemStack itemStack) throws IOException, InvalidConfigurationException {
        ConfigurationSection treasureSection = data.getConfigurationSection("treasure");
        if (treasureSection == null) treasureSection = data.createSection("treasure");
        treasureSection.set(serializeLocation(location), itemStack);
        data.reload();
    }
    public synchronized void deleteItem(@NotNull Location location) throws IOException, InvalidConfigurationException {
        ConfigurationSection treasureSection = data.getConfigurationSection("treasure");
        if (treasureSection != null) treasureSection.set(serializeLocation(location), null);
        data.reload();
    }
    @NotNull
    public ItemStack getItem(@NotNull Location location) {
        ItemStack item = null;
        ConfigurationSection treasureSection = data.getConfigurationSection("treasure");
        if (treasureSection != null) item = treasureSection.getItemStack(serializeLocation(location));
        if (item == null) item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        assert meta != null;
        meta.setLore(Collections.singletonList(itemLore));
        meta.setCustomModelData(5);
        item.setItemMeta(meta);
        return item;
    }
    public static boolean isItem(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return false;
        List<String> lore = itemMeta.getLore();
        return lore != null
                && lore.size() == 1
                && lore.get(0).equalsIgnoreCase(itemLore)
                && itemMeta.hasCustomModelData()
                && itemMeta.getCustomModelData() == 5;
    }
    @NotNull
    protected ConfigurationSection getScoresSection() {
        ConfigurationSection result = data.getConfigurationSection("scores");
        assert result != null;
        return result;
    }
    @NotNull
    public List<Map.Entry<UUID, Integer>> getScores() {
        ConfigurationSection scores = getScoresSection();
        Set<String> players = scores.getKeys(false);
        return players.stream()
                .map(UUID::fromString)
                .map(uuid -> (Map.Entry<UUID, Integer>) new AbstractMap.SimpleEntry<>(uuid, getScore(Bukkit.getOfflinePlayer(uuid))))
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .toList();
    }
    public synchronized int getScore(OfflinePlayer player) {
        return getScoresSection().getInt(player.getUniqueId().toString());
    }
    public synchronized void setScore(OfflinePlayer player, int amount) throws IOException, InvalidConfigurationException {
        getScoresSection().set(player.getUniqueId().toString(), amount);
        data.set("scores", getScoresSection());
        data.reload();
    }
    public synchronized void addScore(OfflinePlayer player, int amount) throws IOException, InvalidConfigurationException {
        setScore(player, getScore(player) + amount);
    }
}
