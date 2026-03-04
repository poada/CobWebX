package cw.cobwebx;

import cw.cobwebx.commands.CobWebXCommands;
import cw.cobwebx.listeners.CobWebXListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CobWebX extends JavaPlugin {

    public static String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&4CobWebX&8] ");
    public Set<UUID> bypassPlayers = new HashSet<>();
    private String version;

    @Override
    public void onEnable() {

        version = getDescription().getVersion();
        String server = Bukkit.getName();
        String mcVersion = Bukkit.getBukkitVersion();

        // ---------------- WORLDGUARD CHECK ----------------
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {

            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&c"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Plugin  » &cCoBWebX"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Version » &c" + version));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Author  » &cC16 Plugins"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Server  » &c" + server));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  MC Ver  » &c" + mcVersion));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  WorldGuard » &cNot Detected ✘"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&c"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&c  Plugin could not start!"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&c  Please install WorldGuard 7.x"));
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&c"));

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // ---------------- NORMAL LOAD ----------------

        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        saveDefaultConfig();
        registerCommands();

        // Banner
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a _______  _______  ______            _______  ______"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a(  ____ \\(  ___  )(  ___ \\ |\\     /|(  ____ \\(  ___ \\ |\\     /|"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a| (    \\/| (   ) || (   ) )| )   ( || (    \\/| (   ) )( \\   / )"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a| |      | |   | || (__/ / | | _ | || (__    | (__/ /  \\ (_) /"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a| |      | |   | ||  __ (  | |( )| ||  __)   |  __ (    ) _ ("));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a| |      | |   | || (  \\ \\ | || || || (      | (  \\ \\  / ( ) \\"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a| (____/\\| (___) || )___) )| () () || (____/\\| )___) )( /   \\ )"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a(_______/(_______)|/ \\___/ (_______)(_______/|/ \\___/ |/     \\|"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a"));

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Plugin  » &aCoBWebX"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Version » &a" + version));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Author  » &aC16 Plugins"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  Server  » &a" + server));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  MC Ver  » &a" + mcVersion));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7  WorldGuard » &aDetected ✔"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a  Plugin loaded successfully!"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&a"));
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&fVersion: " + version));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&fAuthor: &1C16 Plugins"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&cPlugin has been disabled!"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------"));
    }

    private void registerCommands() {
        this.getCommand("cobwebx").setExecutor(new CobWebXCommands(this));
        new CobWebXListener(this);
    }
}