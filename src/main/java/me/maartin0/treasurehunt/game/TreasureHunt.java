package me.maartin0.treasurehunt.game;

import me.maartin0.treasurehunt.commands.ManageCommand;
import me.maartin0.treasurehunt.util.Data;
import me.maartin0.treasurehunt.util.Logger;
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
    private ConfigurationSection section;
    public static void loadInteractables() {
        data.getKeys(false).forEach(k1 -> {
            ConfigurationSection huntSection = Objects.requireNonNull(data.getConfigurationSection(k1));
            TreasureHunt hunt = TreasureHunt.fromConfigurationSection(huntSection);
            ConfigurationSection treasureSection = huntSection.getConfigurationSection("treasure");
            if (treasureSection == null) return;
            treasureSection.getKeys(false).forEach(k2 -> {
                Location location = deserializeLocation(k2);
                Block block = Objects.requireNonNull(Objects.requireNonNull(location).getWorld()).getBlockAt(location);
                if (ManageCommand.validBlock(block)) new Chest(hunt, block);
            });
        });
    }
    private static TreasureHunt fromConfigurationSection(@NotNull ConfigurationSection section) {
        TreasureHunt hunt = new TreasureHunt();
        hunt.name = section.getName();
        hunt.section = section;

        String locationString = section.getString("vault_location");
        assert locationString != null;
        Location location = deserializeLocation(locationString);
        assert location != null;
        World world = location.getWorld();
        assert world != null;
        Block block = world.getBlockAt(location);
        hunt.vault = new Vault(hunt, block);

        return hunt;
    }
    public synchronized void delete() throws IOException, InvalidConfigurationException {
        data.set(name, null);
        data.reload();
        vault.block.breakNaturally(new ItemStack(Material.DIAMOND_PICKAXE));
        Interactable.interactables.stream()
                .filter(i -> i.hunt.equals(this))
                .forEach(i -> Interactable.interactables.remove(i));
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
        section.set("vault_location", serializeLocation(vault.block.getLocation()));
        data.reload();
    }
    public String name;
    public Vault vault;
    @NotNull
    public static TreasureHunt create(@Nullable String name, Block vaultLocation) throws IOException, InvalidConfigurationException {
        if (name == null) name = "default";
        TreasureHunt hunt = get(name);
        if (hunt != null) return hunt;
        hunt = new TreasureHunt();
        hunt.name = name;
        hunt.vault = new Vault(hunt, vaultLocation);
        hunt.section = data.createSection(name);
        hunt.section.createSection("scores");
        hunt.save();
        return hunt;
    }
    @Nullable
    public static TreasureHunt get(@Nullable String name) {
        if (name == null) name = "default";
        ConfigurationSection section = data.getConfigurationSection(name);
        if (section == null) return null;
        return fromConfigurationSection(section);
    }
    public static Set<String> getNames() {
        return data.getKeys(false);
    }
    public synchronized void setItem(@NotNull Location location, @NotNull ItemStack itemStack) throws IOException, InvalidConfigurationException {
        ConfigurationSection treasureSection = section.getConfigurationSection("treasure");
        if (treasureSection == null) treasureSection = section.createSection("treasure");
        treasureSection.set(serializeLocation(location), itemStack.serialize());
        data.reload();
    }
    public synchronized void deleteItem(@NotNull Location location) throws IOException, InvalidConfigurationException {
        ConfigurationSection treasureSection = section.getConfigurationSection("treasure");
        if (treasureSection != null) treasureSection.set(serializeLocation(location), null);
        data.reload();
    }
    @Nullable
    public ItemStack getItem(@NotNull Location location) {
        ConfigurationSection treasureSection = section.getConfigurationSection("treasure");
        if (treasureSection == null) return null;
        Map<String, Object> result = (Map<String, Object>) treasureSection.get(serializeLocation(location));
        if (result == null) return null;
        ItemStack item = ItemStack.deserialize(result);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        assert meta != null;
        meta.setLore(Collections.singletonList(itemLore));
        item.setItemMeta(meta);
        return item;
    }
    public static boolean isItem(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return false;
        List<String> lore = itemMeta.getLore();
        return lore != null && lore.size() == 1 && lore.get(0).equalsIgnoreCase(itemLore);
    }
    @NotNull
    protected ConfigurationSection getScoresSection() {
        ConfigurationSection result = section.getConfigurationSection("scores");
        assert result != null;
        return result;
    }
    public static boolean exists(@NotNull String name) {
        return data.contains(name);
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
        section.set("scores", getScoresSection());
        data.set(name, section);
        data.reload();
    }
    public synchronized void addScore(OfflinePlayer player, int amount) throws IOException, InvalidConfigurationException {
        setScore(player, getScore(player) + amount);
    }
}
