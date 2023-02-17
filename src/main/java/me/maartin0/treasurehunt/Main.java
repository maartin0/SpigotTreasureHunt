package me.maartin0.treasurehunt;

import me.maartin0.treasurehunt.commands.ManageCommand;
import me.maartin0.treasurehunt.game.Interactable;
import me.maartin0.treasurehunt.game.TreasureHunt;
import me.maartin0.treasurehunt.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public static JavaPlugin plugin;
    public final static String permission = "treasurehunt.manage";
    @Override
    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(new Interactable.InteractionListener(), this);
        new ManageCommand().register("hunt");
        Logger.logInfo("Ready!");
    }

    @Override
    public void onDisable() {
        Logger.logInfo("Disabled");
    }
}
