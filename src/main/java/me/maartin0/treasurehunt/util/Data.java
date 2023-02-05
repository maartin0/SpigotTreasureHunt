package me.maartin0.treasurehunt.util;

import me.maartin0.treasurehunt.Main;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Contains data file loading and saving logic
 */
public class Data extends YamlConfiguration {
    File file = new File(Main.plugin.getDataFolder(), "data.yml");
    public Data() throws IOException, InvalidConfigurationException {
        super();
        if (file.exists()) load();
        else save();
    }
    public synchronized void reload() throws IOException, InvalidConfigurationException {
        save();
        load();
    }
    public synchronized void load() throws IOException, InvalidConfigurationException {
        this.load(file);
    }
    public synchronized void save() throws IOException {
        save(file);
    }
}