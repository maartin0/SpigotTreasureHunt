package me.maartin0.treasurehunt.util.persistance;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PersistentLocation implements PersistentDataType<PersistentDataContainer, Location>  {
    private final JavaPlugin plugin;

    public PersistentLocation (JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public @NotNull Class<Location> getComplexType() {
        return Location.class;
    }

    @Override
    public @NotNull PersistentDataContainer toPrimitive(Location complex, PersistentDataAdapterContext context) {
        PersistentDataContainer persistentDataContainer = context.newPersistentDataContainer();

        persistentDataContainer.set(key("world"), PersistentDataType.STRING, complex.getWorld().getName());
        persistentDataContainer.set(key("x"), PersistentDataType.DOUBLE, complex.getX());
        persistentDataContainer.set(key("z"), PersistentDataType.DOUBLE, complex.getZ());
        persistentDataContainer.set(key("y"), PersistentDataType.DOUBLE, complex.getY());
        persistentDataContainer.set(key("pitch"), PersistentDataType.FLOAT, complex.getPitch());
        persistentDataContainer.set(key("yaw"), PersistentDataType.FLOAT, complex.getYaw());

        return persistentDataContainer;
    }

    @Override
    public @NotNull Location fromPrimitive(@NotNull PersistentDataContainer primitive, @NotNull PersistentDataAdapterContext context) {
        String world = primitive.get(key("world"), PersistentDataType.STRING);
        double x = primitive.get(key("x"), PersistentDataType.DOUBLE);
        double z = primitive.get(key("z"), PersistentDataType.DOUBLE);
        double y = primitive.get(key("y"), PersistentDataType.DOUBLE);
        float pitch = primitive.get(key("pitch"), PersistentDataType.FLOAT);
        float yaw = primitive.get(key("yaw"), PersistentDataType.FLOAT);

        assert world != null;
        return new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
    }

    private NamespacedKey key(String key) {
        return new NamespacedKey(plugin, key);
    }
}
