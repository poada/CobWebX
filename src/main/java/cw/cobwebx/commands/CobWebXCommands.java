package cw.cobwebx.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import cw.cobwebx.CobWebX;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class CobWebXCommands implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "help",
            "reload",
            "check",
            "bypass",
            "debug",
            "creategroup",
            "deletegroup",
            "renamegroup",
            "listgroups",
            "info",
            "toggle",
            "add",
            "remove",
            "setdelay"
    );

    private final CobWebX plugin;

    public CobWebXCommands(CobWebX plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("cobwebx"), "Command 'cobwebx' not found in plugin.yml")
                .setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            if (!has(sender, "cobwebx.help")) {
                noPerm(sender);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help" -> {
                if (!has(sender, "cobwebx.help")) {
                    noPerm(sender);
                    return true;
                }
                sendHelp(sender);
            }

            case "reload" -> {
                if (!has(sender, "cobwebx.reload")) {
                    noPerm(sender);
                    return true;
                }
                reload(sender);
            }

            case "check" -> {
                if (!has(sender, "cobwebx.check")) {
                    noPerm(sender);
                    return true;
                }
                check(sender);
            }

            case "bypass" -> {
                if (!has(sender, "cobwebx.bypass")) {
                    noPerm(sender);
                    return true;
                }
                bypass(sender);
            }

            case "debug" -> {
                if (!has(sender, "cobwebx.debug")) {
                    noPerm(sender);
                    return true;
                }
                debug(sender);
            }

            case "creategroup" -> {
                if (!has(sender, "cobwebx.creategroup")) {
                    noPerm(sender);
                    return true;
                }
                createGroup(sender, args);
            }

            case "deletegroup" -> {
                if (!has(sender, "cobwebx.deletegroup")) {
                    noPerm(sender);
                    return true;
                }
                deleteGroup(sender, args);
            }

            case "renamegroup" -> {
                if (!has(sender, "cobwebx.renamegroup")) {
                    noPerm(sender);
                    return true;
                }
                renameGroup(sender, args);
            }

            case "listgroups" -> {
                if (!has(sender, "cobwebx.listgroups")) {
                    noPerm(sender);
                    return true;
                }
                listGroups(sender);
            }

            case "info" -> {
                if (!has(sender, "cobwebx.info")) {
                    noPerm(sender);
                    return true;
                }
                info(sender, args);
            }

            case "toggle" -> {
                if (!has(sender, "cobwebx.toggle")) {
                    noPerm(sender);
                    return true;
                }
                toggle(sender, args);
            }

            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);

            case "setdelay" -> {
                if (!has(sender, "cobwebx.setdelay")) {
                    noPerm(sender);
                    return true;
                }
                setDelay(sender, args);
            }

            default -> msg(sender, "§cUnknown subcommand. §7Use §f/cobwebx help");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        line(sender);
        sender.sendMessage("§b§lCobWebX §7Commands");
        sender.sendMessage(" §8• §f/cobwebx help");
        sender.sendMessage(" §8• §f/cobwebx reload");
        sender.sendMessage(" §8• §f/cobwebx check");
        sender.sendMessage(" §8• §f/cobwebx bypass");
        sender.sendMessage(" §8• §f/cobwebx debug");
        sender.sendMessage("");
        sender.sendMessage("§b§lGroups");
        sender.sendMessage(" §8• §f/cobwebx creategroup §7<group>");
        sender.sendMessage(" §8• §f/cobwebx deletegroup §7<group>");
        sender.sendMessage(" §8• §f/cobwebx renamegroup §7<old> <new>");
        sender.sendMessage(" §8• §f/cobwebx listgroups");
        sender.sendMessage(" §8• §f/cobwebx info §7<group>");
        sender.sendMessage(" §8• §f/cobwebx toggle §7<group>");
        sender.sendMessage("");
        sender.sendMessage("§b§lEdit");
        sender.sendMessage(" §8• §f/cobwebx add region §7<group> <world> <region>");
        sender.sendMessage(" §8• §f/cobwebx add block §7<group> <block>");
        sender.sendMessage(" §8• §f/cobwebx remove region §7<group> <world> <region>");
        sender.sendMessage(" §8• §f/cobwebx remove block §7<group> <block>");
        sender.sendMessage(" §8• §f/cobwebx setdelay §7<group> <seconds>");
        line(sender);
    }

    private void reload(CommandSender sender) {
        plugin.reloadPluginData();
        msg(sender, "§aConfig reloaded.");
        msg(sender, "§7Groups cached: §f" + plugin.getCachedGroups().size());
        msg(sender, "§7Valid regions: §a" + plugin.getValidRegionCount());
        msg(sender, "§7Invalid regions: " + (plugin.getInvalidRegionCount() == 0 ? "§a0" : "§c" + plugin.getInvalidRegionCount()));
    }

    private void check(CommandSender sender) {
        line(sender);
        sender.sendMessage("§b§lCobWebX §7Status");
        sender.sendMessage(" §8• §7Version: §f" + plugin.getDescription().getVersion());
        sender.sendMessage(" §8• §7Authors: §f" + String.join(", ", plugin.getDescription().getAuthors()));
        sender.sendMessage(" §8• §7Groups: §f" + plugin.getCachedGroups().size());
        sender.sendMessage(" §8• §7Controlled blocks: §f" + plugin.getCachedGroups().stream().mapToInt(g -> 1).sum());
        sender.sendMessage(" §8• §7Valid regions: §a" + plugin.getValidRegionCount());
        sender.sendMessage(" §8• §7Invalid regions: " + (plugin.getInvalidRegionCount() == 0 ? "§a0" : "§c" + plugin.getInvalidRegionCount()));
        sender.sendMessage(" §8• §7Debug: " + (plugin.getConfig().getBoolean("settings.debug", false) ? "§aON" : "§cOFF"));
        line(sender);
    }

    private void debug(CommandSender sender) {
        boolean value = !plugin.getConfig().getBoolean("settings.debug", false);
        plugin.getConfig().set("settings.debug", value);
        plugin.saveConfig();
        msg(sender, "§7Debug: " + (value ? "§aON" : "§cOFF"));
    }

    private void createGroup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "§cUse: §f/cobwebx creategroup <group>");
            return;
        }

        String group = args[1];

        if (groupExists(group)) {
            msg(sender, "§cGroup already exists.");
            return;
        }

        plugin.getConfig().set("groups." + group + ".enabled", true);
        plugin.getConfig().set("groups." + group + ".regions", new ArrayList<String>());
        plugin.getConfig().set("groups." + group + ".blocks", new ArrayList<String>());
        plugin.getConfig().set("groups." + group + ".block_remove_delay", 60);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aGroup created: §e" + group);
    }

    private void deleteGroup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "§cUse: §f/cobwebx deletegroup <group>");
            return;
        }

        String exactGroup = findGroup(args[1]);
        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        plugin.getConfig().set("groups." + exactGroup, null);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aGroup deleted: §e" + exactGroup);
    }

    private void renameGroup(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "§cUse: §f/cobwebx renamegroup <old> <new>");
            return;
        }

        String oldName = findGroup(args[1]);
        String newName = args[2];

        if (oldName == null) {
            msg(sender, "§cSource group not found.");
            return;
        }

        if (groupExists(newName)) {
            msg(sender, "§cTarget group already exists.");
            return;
        }

        Object data = plugin.getConfig().get("groups." + oldName);
        plugin.getConfig().set("groups." + newName, data);
        plugin.getConfig().set("groups." + oldName, null);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aRenamed §e" + oldName + " §ato §e" + newName);
    }

    private void listGroups(CommandSender sender) {
        List<String> groups = getGroups();

        if (groups.isEmpty()) {
            msg(sender, "§cNo groups found.");
            return;
        }

        line(sender);
        sender.sendMessage("§b§lCobWebX §7Groups");
        for (String group : groups) {
            boolean enabled = plugin.getConfig().getBoolean("groups." + group + ".enabled", true);
            sender.sendMessage(" §8• §f" + group + " §8- " + (enabled ? "§aON" : "§cOFF"));
        }
        line(sender);
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "§cUse: §f/cobwebx info <group>");
            return;
        }

        String exactGroup = findGroup(args[1]);
        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        boolean enabled = plugin.getConfig().getBoolean("groups." + exactGroup + ".enabled", true);
        List<String> regions = plugin.getConfig().getStringList("groups." + exactGroup + ".regions");
        List<String> blocks = plugin.getConfig().getStringList("groups." + exactGroup + ".blocks");
        int seconds = plugin.getConfig().getInt("groups." + exactGroup + ".block_remove_delay", 60);

        line(sender);
        sender.sendMessage("§b§lCobWebX §7Info §8- §f" + exactGroup);
        sender.sendMessage(" §8• §7Enabled: " + (enabled ? "§atrue" : "§cfalse"));
        sender.sendMessage(" §8• §7Regions: §f" + (regions.isEmpty() ? "None" : String.join(", ", regions)));
        sender.sendMessage(" §8• §7Blocks: §f" + (blocks.isEmpty() ? "None" : String.join(", ", blocks)));
        sender.sendMessage(" §8• §7Delay: §f" + seconds + "s");
        line(sender);
    }

    private void toggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "§cUse: §f/cobwebx toggle <group>");
            return;
        }

        String exactGroup = findGroup(args[1]);
        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        boolean value = !plugin.getConfig().getBoolean("groups." + exactGroup + ".enabled", true);
        plugin.getConfig().set("groups." + exactGroup + ".enabled", value);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§7Group §e" + exactGroup + " §7is now " + (value ? "§aON" : "§cOFF"));
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "§cUse: §f/cobwebx add <region|block> ...");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "region" -> {
                if (!has(sender, "cobwebx.add.region")) {
                    noPerm(sender);
                    return;
                }
                addRegion(sender, args);
            }
            case "block" -> {
                if (!has(sender, "cobwebx.add.block")) {
                    noPerm(sender);
                    return;
                }
                addBlock(sender, args);
            }
            default -> msg(sender, "§cUse: §f/cobwebx add <region|block>");
        }
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "§cUse: §f/cobwebx remove <region|block> ...");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "region" -> {
                if (!has(sender, "cobwebx.remove.region")) {
                    noPerm(sender);
                    return;
                }
                removeRegion(sender, args);
            }
            case "block" -> {
                if (!has(sender, "cobwebx.remove.block")) {
                    noPerm(sender);
                    return;
                }
                removeBlock(sender, args);
            }
            default -> msg(sender, "§cUse: §f/cobwebx remove <region|block>");
        }
    }

    private void addRegion(CommandSender sender, String[] args) {
        if (args.length < 5) {
            msg(sender, "§cUse: §f/cobwebx add region <group> <world> <region>");
            return;
        }

        String exactGroup = findGroup(args[2]);
        String exactWorld = findWorldName(args[3]);
        String regionInput = args[4];

        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        if (exactWorld == null) {
            msg(sender, "§cWorld not found.");
            return;
        }

        String exactRegion = findWorldGuardRegion(exactWorld, regionInput);
        if (exactRegion == null) {
            msg(sender, "§cRegion not found in world §e" + exactWorld + "§c.");
            return;
        }

        List<String> regions = plugin.getConfig().getStringList("groups." + exactGroup + ".regions");
        String entry = toRegionEntry(exactWorld, exactRegion);

        if (containsIgnoreCase(regions, entry)) {
            msg(sender, "§cRegion already added.");
            return;
        }

        regions.add(entry);
        plugin.getConfig().set("groups." + exactGroup + ".regions", regions);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aAdded region §e" + exactRegion + " §7(§f" + exactWorld + "§7) §ato §e" + exactGroup);
    }

    private void removeRegion(CommandSender sender, String[] args) {
        if (args.length < 5) {
            msg(sender, "§cUse: §f/cobwebx remove region <group> <world> <region>");
            return;
        }

        String exactGroup = findGroup(args[2]);
        String exactWorld = findWorldName(args[3]);
        String regionInput = args[4];

        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        if (exactWorld == null) {
            msg(sender, "§cWorld not found.");
            return;
        }

        List<String> regions = plugin.getConfig().getStringList("groups." + exactGroup + ".regions");
        String entry = findRegionEntry(regions, exactWorld, regionInput);

        if (entry == null) {
            msg(sender, "§cRegion not found in group.");
            return;
        }

        String[] split = splitRegionEntry(entry);
        regions.remove(entry);
        plugin.getConfig().set("groups." + exactGroup + ".regions", regions);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aRemoved region §e" + split[1] + " §7(§f" + split[0] + "§7) §afrom §e" + exactGroup);
    }

    private void addBlock(CommandSender sender, String[] args) {
        if (args.length < 4) {
            msg(sender, "§cUse: §f/cobwebx add block <group> <block>");
            return;
        }

        String exactGroup = findGroup(args[2]);
        String blockName = args[3].toUpperCase(Locale.ROOT);

        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        Material material = Material.getMaterial(blockName);
        if (material == null || !material.isBlock()) {
            msg(sender, "§cInvalid block.");
            return;
        }

        List<String> blocks = plugin.getConfig().getStringList("groups." + exactGroup + ".blocks");
        if (containsIgnoreCase(blocks, material.name())) {
            msg(sender, "§cBlock already added.");
            return;
        }

        blocks.add(material.name());
        plugin.getConfig().set("groups." + exactGroup + ".blocks", blocks);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aAdded block §e" + material.name() + " §ato §e" + exactGroup);
    }

    private void removeBlock(CommandSender sender, String[] args) {
        if (args.length < 4) {
            msg(sender, "§cUse: §f/cobwebx remove block <group> <block>");
            return;
        }

        String exactGroup = findGroup(args[2]);
        String blockInput = args[3];

        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        List<String> blocks = plugin.getConfig().getStringList("groups." + exactGroup + ".blocks");
        String exactBlock = findIgnoreCase(blocks, blockInput);

        if (exactBlock == null) {
            msg(sender, "§cBlock not found in group.");
            return;
        }

        blocks.remove(exactBlock);
        plugin.getConfig().set("groups." + exactGroup + ".blocks", blocks);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aRemoved block §e" + exactBlock + " §afrom §e" + exactGroup);
    }

    private void setDelay(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "§cUse: §f/cobwebx setdelay <group> <seconds>");
            return;
        }

        String exactGroup = findGroup(args[1]);
        if (exactGroup == null) {
            msg(sender, "§cGroup not found.");
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            msg(sender, "§cInvalid number.");
            return;
        }

        if (seconds < 0) {
            msg(sender, "§cSeconds must be 0 or higher.");
            return;
        }

        plugin.getConfig().set("groups." + exactGroup + ".block_remove_delay", seconds);
        plugin.saveConfig();
        plugin.loadPluginData();

        msg(sender, "§aDelay of §e" + exactGroup + " §aset to §f" + seconds + "s");
    }

    private void bypass(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "§cPlayers only.");
            return;
        }

        if (!player.isOp()) {
            msg(sender, "§cOnly OP can use bypass.");
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean enabled;

        if (plugin.bypassPlayers.contains(uuid)) {
            plugin.bypassPlayers.remove(uuid);
            enabled = true;
        } else {
            plugin.bypassPlayers.add(uuid);
            enabled = false;
        }

        msg(player, "§7Bypass: " + (enabled ? "§aON" : "§cOFF"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 1) {
            return startsWith(getAllowedSubcommands(sender), args[0]);
        }

        if (args.length == 2) {
            if (sub.equals("deletegroup") && has(sender, "cobwebx.deletegroup")) {
                return startsWith(getGroups(), args[1]);
            }

            if (sub.equals("info") && has(sender, "cobwebx.info")) {
                return startsWith(getGroups(), args[1]);
            }

            if (sub.equals("toggle") && has(sender, "cobwebx.toggle")) {
                return startsWith(getGroups(), args[1]);
            }

            if (sub.equals("setdelay") && has(sender, "cobwebx.setdelay")) {
                return startsWith(getGroups(), args[1]);
            }

            if (sub.equals("renamegroup") && has(sender, "cobwebx.renamegroup")) {
                return startsWith(getGroups(), args[1]);
            }

            if (sub.equals("add") && hasAny(sender, "cobwebx.add.region", "cobwebx.add.block")) {
                return startsWith(getAllowedAddRemoveTypes(sender, true), args[1]);
            }

            if (sub.equals("remove") && hasAny(sender, "cobwebx.remove.region", "cobwebx.remove.block")) {
                return startsWith(getAllowedAddRemoveTypes(sender, false), args[1]);
            }

            return Collections.emptyList();
        }

        if (args.length == 3) {
            if (sub.equals("add")) {
                String type = args[1].toLowerCase(Locale.ROOT);
                if ((type.equals("region") || type.equals("block")) && hasAny(sender, "cobwebx.add.region", "cobwebx.add.block")) {
                    return startsWith(getGroups(), args[2]);
                }
            }

            if (sub.equals("remove")) {
                String type = args[1].toLowerCase(Locale.ROOT);
                if ((type.equals("region") || type.equals("block")) && hasAny(sender, "cobwebx.remove.region", "cobwebx.remove.block")) {
                    return startsWith(getGroups(), args[2]);
                }
            }

            return Collections.emptyList();
        }

        if (args.length == 4) {
            if (sub.equals("add")) {
                String type = args[1].toLowerCase(Locale.ROOT);

                if (type.equals("block") && has(sender, "cobwebx.add.block")) {
                    return startsWith(getAllBlocks(), args[3]);
                }

                if (type.equals("region") && has(sender, "cobwebx.add.region")) {
                    return startsWith(getWorldNames(), args[3]);
                }
            }

            if (sub.equals("remove")) {
                String type = args[1].toLowerCase(Locale.ROOT);

                if (type.equals("block") && has(sender, "cobwebx.remove.block")) {
                    String exactGroup = findGroup(args[2]);
                    return exactGroup == null
                            ? Collections.emptyList()
                            : startsWith(plugin.getConfig().getStringList("groups." + exactGroup + ".blocks"), args[3]);
                }

                if (type.equals("region") && has(sender, "cobwebx.remove.region")) {
                    return startsWith(getWorldNamesFromGroup(args[2]), args[3]);
                }
            }

            return Collections.emptyList();
        }

        if (args.length == 5) {
            if (sub.equals("add")) {
                String type = args[1].toLowerCase(Locale.ROOT);

                if (type.equals("region") && has(sender, "cobwebx.add.region")) {
                    String exactWorld = findWorldName(args[3]);
                    return exactWorld == null
                            ? Collections.emptyList()
                            : startsWith(getWorldGuardRegions(exactWorld), args[4]);
                }
            }

            if (sub.equals("remove")) {
                String type = args[1].toLowerCase(Locale.ROOT);

                if (type.equals("region") && has(sender, "cobwebx.remove.region")) {
                    String exactGroup = findGroup(args[2]);
                    String exactWorld = findWorldName(args[3]);
                    return (exactGroup == null || exactWorld == null)
                            ? Collections.emptyList()
                            : startsWith(getRegionsFromGroupByWorld(exactGroup, exactWorld), args[4]);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> getWorldGuardRegions(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Collections.emptyList();
        }

        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(regionManager.getRegions().keySet());
    }

    private String findWorldGuardRegion(String worldName, String input) {
        for (String region : getWorldGuardRegions(worldName)) {
            if (region.equalsIgnoreCase(input)) {
                return region;
            }
        }
        return null;
    }

    private List<String> getWorldNames() {
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worlds.add(world.getName());
        }
        return worlds;
    }

    private String findWorldName(String input) {
        for (String world : getWorldNames()) {
            if (world.equalsIgnoreCase(input)) {
                return world;
            }
        }
        return null;
    }

    private String toRegionEntry(String world, String region) {
        return world + ":" + region;
    }

    private String[] splitRegionEntry(String entry) {
        return entry.split(":", 2);
    }

    private String findRegionEntry(List<String> entries, String world, String region) {
        for (String entry : entries) {
            String[] split = splitRegionEntry(entry);
            if (split.length != 2) {
                continue;
            }

            if (split[0].equalsIgnoreCase(world) && split[1].equalsIgnoreCase(region)) {
                return entry;
            }
        }
        return null;
    }

    private List<String> getWorldNamesFromGroup(String groupInput) {
        String exactGroup = findGroup(groupInput);
        if (exactGroup == null) {
            return Collections.emptyList();
        }

        List<String> worlds = new ArrayList<>();
        List<String> regions = plugin.getConfig().getStringList("groups." + exactGroup + ".regions");

        for (String entry : regions) {
            String[] split = splitRegionEntry(entry);
            if (split.length != 2) {
                continue;
            }

            if (!containsIgnoreCase(worlds, split[0])) {
                worlds.add(split[0]);
            }
        }

        return worlds;
    }

    private List<String> getRegionsFromGroupByWorld(String group, String world) {
        List<String> out = new ArrayList<>();
        List<String> entries = plugin.getConfig().getStringList("groups." + group + ".regions");

        for (String entry : entries) {
            String[] split = splitRegionEntry(entry);
            if (split.length != 2) {
                continue;
            }

            if (split[0].equalsIgnoreCase(world)) {
                out.add(split[1]);
            }
        }

        return out;
    }

    private List<String> getAllBlocks() {
        List<String> blocks = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                blocks.add(material.name());
            }
        }
        return blocks;
    }

    private List<String> getGroups() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("groups");
        if (section == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    private String findGroup(String input) {
        for (String group : getGroups()) {
            if (group.equalsIgnoreCase(input)) {
                return group;
            }
        }
        return null;
    }

    private List<String> getAllowedSubcommands(CommandSender sender) {
        List<String> allowed = new ArrayList<>();

        if (has(sender, "cobwebx.help")) allowed.add("help");
        if (has(sender, "cobwebx.reload")) allowed.add("reload");
        if (has(sender, "cobwebx.check")) allowed.add("check");
        if (has(sender, "cobwebx.bypass")) allowed.add("bypass");
        if (has(sender, "cobwebx.debug")) allowed.add("debug");
        if (has(sender, "cobwebx.creategroup")) allowed.add("creategroup");
        if (has(sender, "cobwebx.deletegroup")) allowed.add("deletegroup");
        if (has(sender, "cobwebx.renamegroup")) allowed.add("renamegroup");
        if (has(sender, "cobwebx.listgroups")) allowed.add("listgroups");
        if (has(sender, "cobwebx.info")) allowed.add("info");
        if (has(sender, "cobwebx.toggle")) allowed.add("toggle");
        if (hasAny(sender, "cobwebx.add.region", "cobwebx.add.block")) allowed.add("add");
        if (hasAny(sender, "cobwebx.remove.region", "cobwebx.remove.block")) allowed.add("remove");
        if (has(sender, "cobwebx.setdelay")) allowed.add("setdelay");

        return allowed;
    }

    private List<String> getAllowedAddRemoveTypes(CommandSender sender, boolean add) {
        List<String> types = new ArrayList<>();

        if (add) {
            if (has(sender, "cobwebx.add.region")) types.add("region");
            if (has(sender, "cobwebx.add.block")) types.add("block");
        } else {
            if (has(sender, "cobwebx.remove.region")) types.add("region");
            if (has(sender, "cobwebx.remove.block")) types.add("block");
        }

        return types;
    }

    private boolean groupExists(String group) {
        return findGroup(group) != null;
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(CobWebX.PREFIX + text);
    }

    private void line(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
    }

    private void noPerm(CommandSender sender) {
        msg(sender, "§cNo permission.");
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("cobwebx.admin");
    }

    private boolean hasAny(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (has(sender, permission)) {
                return true;
            }
        }
        return false;
    }

    private List<String> startsWith(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }

        return result;
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        return findIgnoreCase(list, value) != null;
    }

    private String findIgnoreCase(List<String> list, String value) {
        for (String entry : list) {
            if (entry.equalsIgnoreCase(value)) {
                return entry;
            }
        }
        return null;
    }
}