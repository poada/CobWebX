package cw.cobwebx;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import cw.cobwebx.commands.CobWebXCommands;
import cw.cobwebx.listeners.CobWebXListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CobWebX extends JavaPlugin {

    public static final String PREFIX = color("&8[&4CobWebX&8] ");

    public final Set<UUID> bypassPlayers = new HashSet<>();

    private final List<GroupCache> cachedGroups = new ArrayList<>();
    private final Set<String> controlledBlocks = new HashSet<>();

    private String version;
    private int validRegionCount;
    private int invalidRegionCount;

    @Override
    public void onEnable() {
        this.version = getDescription().getVersion();

        String server = Bukkit.getName();
        String mcVersion = Bukkit.getBukkitVersion();

        if (!hasWorldGuard()) {
            sendStartupError(server, mcVersion);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        createFiles();
        loadPluginData();
        registerPlugin();

        sendStartupSuccess(server, mcVersion);
    }

    @Override
    public void onDisable() {
        cachedGroups.clear();
        controlledBlocks.clear();

        line("&8&m----------------------------------------");
        log(PREFIX + "&7Version: &f" + version);
        log(PREFIX + "&7Author: &fC16 Plugins");
        log(PREFIX + "&cPlugin disabled.");
        line("&8&m----------------------------------------");
    }

    public static final class GroupCache {
        private final String name;
        private final boolean enabled;
        private final Set<String> blocks;
        private final Map<String, Set<String>> regionsByWorld;
        private final int delaySeconds;
        private final long delayTicks;

        public GroupCache(String name, boolean enabled, Set<String> blocks, Map<String, Set<String>> regionsByWorld, int delaySeconds) {
            this.name = name;
            this.enabled = enabled;
            this.blocks = Collections.unmodifiableSet(blocks);
            this.regionsByWorld = Collections.unmodifiableMap(regionsByWorld);
            this.delaySeconds = delaySeconds;
            this.delayTicks = Math.max(0L, delaySeconds * 20L);
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean allowsBlock(Material material) {
            return blocks.contains(material.name().toUpperCase(Locale.ROOT));
        }

        public boolean containsRegion(String worldName, String regionName) {
            Set<String> regions = regionsByWorld.get(worldName.toLowerCase(Locale.ROOT));
            return regions != null && regions.contains(regionName.toLowerCase(Locale.ROOT));
        }

        public int getDelaySeconds() {
            return delaySeconds;
        }

        public long getDelayTicks() {
            return delayTicks;
        }
    }

    public void loadPluginData() {
        rebuildCaches();
        validateConfiguredRegions();
    }

    public void reloadPluginData() {
        reloadConfig();
        loadPluginData();
    }

    public List<GroupCache> getCachedGroups() {
        return Collections.unmodifiableList(cachedGroups);
    }

    public boolean isControlledBlock(Material material) {
        return controlledBlocks.contains(material.name().toUpperCase(Locale.ROOT));
    }

    public int getValidRegionCount() {
        return validRegionCount;
    }

    public int getInvalidRegionCount() {
        return invalidRegionCount;
    }

    public void debug(String message) {
        if (!getConfig().getBoolean("settings.debug", false)) {
            return;
        }
        Bukkit.getConsoleSender().sendMessage(color(PREFIX + "&8[DEBUG] &7" + message));
    }

    private boolean hasWorldGuard() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    private void createFiles() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();

        if (!getConfig().contains("settings.debug")) {
            getConfig().set("settings.debug", false);
            saveConfig();
        }
    }

    private void registerPlugin() {
        PluginCommand command = getCommand("cobwebx");
        if (command == null) {
            throw new IllegalStateException("Command 'cobwebx' is not defined in plugin.yml");
        }

        CobWebXCommands commands = new CobWebXCommands(this);
        command.setExecutor(commands);
        command.setTabCompleter(commands);

        new CobWebXListener(this);
    }

    private void rebuildCaches() {
        cachedGroups.clear();
        controlledBlocks.clear();

        ConfigurationSection groups = getConfig().getConfigurationSection("groups");
        if (groups == null) {
            return;
        }

        for (String groupName : groups.getKeys(false)) {
            String path = "groups." + groupName + ".";
            boolean enabled = getConfig().getBoolean(path + "enabled", true);
            int delaySeconds = Math.max(0, getConfig().getInt(path + "block_remove_delay", 60));

            Set<String> blocks = new HashSet<>();
            for (String block : getConfig().getStringList(path + "blocks")) {
                String name = block.toUpperCase(Locale.ROOT);
                Material material = Material.getMaterial(name);
                if (material != null && material.isBlock()) {
                    blocks.add(name);
                    controlledBlocks.add(name);
                }
            }

            Map<String, Set<String>> regionsByWorld = new HashMap<>();
            for (String entry : getConfig().getStringList(path + "regions")) {
                String[] split = splitRegionEntry(entry);
                if (split == null) {
                    continue;
                }

                String worldName = split[0].toLowerCase(Locale.ROOT);
                String regionName = split[1].toLowerCase(Locale.ROOT);

                regionsByWorld.computeIfAbsent(worldName, k -> new HashSet<>()).add(regionName);
            }

            cachedGroups.add(new GroupCache(groupName, enabled, blocks, regionsByWorld, delaySeconds));
        }
    }

    private void validateConfiguredRegions() {
        validRegionCount = 0;
        invalidRegionCount = 0;

        ConfigurationSection groups = getConfig().getConfigurationSection("groups");
        if (groups == null) {
            return;
        }

        for (String groupName : groups.getKeys(false)) {
            List<String> entries = getConfig().getStringList("groups." + groupName + ".regions");

            for (String entry : entries) {
                String[] split = splitRegionEntry(entry);
                if (split == null) {
                    invalidRegionCount++;
                    log(PREFIX + "&cInvalid region entry in group &e" + groupName + "&c: &f" + entry + " &7(expected world:region)");
                    continue;
                }

                String worldName = split[0];
                String regionName = split[1];

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    invalidRegionCount++;
                    log(PREFIX + "&cInvalid region in group &e" + groupName + "&c: &f" + entry + " &7(world not found)");
                    continue;
                }

                RegionManager regionManager = WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer()
                        .get(BukkitAdapter.adapt(world));

                if (regionManager == null) {
                    invalidRegionCount++;
                    log(PREFIX + "&cInvalid region in group &e" + groupName + "&c: &f" + entry + " &7(region manager not found)");
                    continue;
                }

                if (regionManager.getRegion(regionName) == null) {
                    invalidRegionCount++;
                    log(PREFIX + "&cInvalid region in group &e" + groupName + "&c: &f" + entry + " &7(region not found)");
                    continue;
                }

                validRegionCount++;
            }
        }
    }

    private String[] splitRegionEntry(String entry) {
        String[] split = entry.split(":", 2);
        if (split.length != 2 || split[0].isBlank() || split[1].isBlank()) {
            return null;
        }
        return split;
    }

    private void sendStartupSuccess(String server, String mcVersion) {
        line("&a");
        line("&a   ██████╗  ██████╗ ██████╗ ██╗    ██╗███████╗██████╗ ");
        line("&a  ██╔════╝ ██╔═══██╗██╔══██╗██║    ██║██╔════╝██╔══██╗");
        line("&a  ██║      ██║   ██║██████╔╝██║ █╗ ██║█████╗  ██████╔╝");
        line("&a  ██║      ██║   ██║██╔══██╗██║███╗██║██╔══╝  ██╔══██╗");
        line("&a  ╚██████╗ ╚██████╔╝██████╔╝╚███╔███╔╝███████╗██████╔╝");
        line("&a   ╚═════╝  ╚═════╝ ╚═════╝  ╚══╝╚══╝ ╚══════╝╚═════╝ ");
        line("&a");

        log("&7  Plugin     » &aCobWebX");
        log("&7  Version    » &a" + version);
        log("&7  Author     » &aC16 Plugins");
        log("&7  Server     » &a" + server);
        log("&7  MC Version » &a" + mcVersion);
        log("&7  WorldGuard » &aDetected ✔");
        log("&7  Groups     » &a" + cachedGroups.size());
        log("&7  Valid RG   » &a" + validRegionCount);
        log("&7  Invalid RG » " + (invalidRegionCount == 0 ? "&a0" : "&c" + invalidRegionCount));
        line("&a");
        log("&a  Plugin loaded successfully.");
        line("&a");
    }

    private void sendStartupError(String server, String mcVersion) {
        line("&c");
        log("&7  Plugin     » &cCobWebX");
        log("&7  Version    » &c" + version);
        log("&7  Author     » &cC16 Plugins");
        log("&7  Server     » &c" + server);
        log("&7  MC Version » &c" + mcVersion);
        log("&7  WorldGuard » &cNot Detected ✘");
        line("&c");
        log("&c  Plugin could not start.");
        log("&c  Please install WorldGuard 7.x");
        line("&c");
    }

    private void log(String message) {
        Bukkit.getConsoleSender().sendMessage(color(message));
    }

    private void line(String message) {
        Bukkit.getConsoleSender().sendMessage(color(message));
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}