package me.maartin0.treasurehunt.util.persistance;

import me.maartin0.treasurehunt.Main;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PersistentDataManager {
    PersistentDataHolder holder;
    PersistentDataContainer persistentDataContainer;

    public PersistentDataManager(@NotNull PersistentDataHolder holder) {
        this.holder = holder;
        this.persistentDataContainer = holder.getPersistentDataContainer();
    }

    @Nullable
    public Integer getInt(@NotNull String path) {
        return persistentDataContainer.get(getKey(path), PersistentDataType.INTEGER);
    }

    public void setInt(@NotNull String path, int value) {
        persistentDataContainer.set(getKey(path), PersistentDataType.INTEGER, value);
    }

    @Nullable
    public Long getLong(@NotNull String path) {
        return persistentDataContainer.get(getKey(path), PersistentDataType.LONG);
    }

    public void setLong(@NotNull String path, long value) {
        persistentDataContainer.set(getKey(path), PersistentDataType.LONG, value);
    }

    public Boolean getBoolean(@NotNull String path) {
        Integer data = getInt(path);
        if (data == null) return false;
        return data == 1;
    }

    public void setBoolean(@NotNull String path, boolean value) {
        setInt(path, value ? 1 : 0);
    }

    @Nullable
    public String getString(@NotNull String path) {
        return persistentDataContainer.get(getKey(path), PersistentDataType.STRING);
    }

    public void setString(@NotNull String path, @NotNull String value) {
        persistentDataContainer.set(getKey(path), PersistentDataType.STRING, value);
    }

    @Nullable
    public Location getLocation(@NotNull String path) {
        return persistentDataContainer.get(getKey(path), new PersistentLocation(Main.plugin));
    }

    public void setLocation(@NotNull String path, @NotNull Location value) {
        persistentDataContainer.set(getKey(path), new PersistentLocation(Main.plugin), value);
    }

    public void clear(@NotNull String path) {
        persistentDataContainer.remove(getKey(path));
    }

    private NamespacedKey getKey(@NotNull String path) {
        return new NamespacedKey(Main.plugin, path);
    }
}
