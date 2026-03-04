package cw.cobwebx.commands;

import cw.cobwebx.CobWebX;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CobWebXCommands implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "help",
            "reload",
            "check",
            "creategroup",
            "deletegroup",
            "addgroup",
            "addblock",
            "viewgroup",
            "bypass"
    );

    private final CobWebX plugin;

    public CobWebXCommands(CobWebX plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(this.plugin.getCommand("cobwebx"), "Command 'cobwebx' not found in plugin.yml")
                .setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(CobWebX.prefix + "§cUse: §7/cobwebx help");
            return true;
        }

        if (!sender.hasPermission("cobwebx.admin")) {
            sender.sendMessage(CobWebX.prefix + "§cYou don't have permission!");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help" -> sendHelp(sender);

            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(CobWebX.prefix + "§aConfig reloaded!");
            }

            case "check" -> sendInfo(sender);

            case "creategroup" -> createGroup(sender, args);

            case "deletegroup" -> deleteGroup(sender, args);

            case "addgroup" -> addRegionToGroup(sender, args);

            case "addblock" -> addBlockToGroup(sender, args);

            case "viewgroup" -> viewGroup(sender, args);

            case "bypass" -> toggleBypass(sender);

            default -> sender.sendMessage(CobWebX.prefix + "§cUnknown command! Use §7/cobwebx help");
        }

        return true;
    }

    // =====================================================
    // Messages
    // =====================================================

    private void sendHelp(CommandSender sender) {
        line(sender);
        sender.sendMessage("§bCobWebX §7Commands");
        sender.sendMessage("§7/cobwebx §fhelp");
        sender.sendMessage("§7/cobwebx §freload");
        sender.sendMessage("§7/cobwebx §fcheck");
        sender.sendMessage("");

        sender.sendMessage("§bGroup management");
        sender.sendMessage("§7/cobwebx §fcreategroup §8<§fname§8>");
        sender.sendMessage("§7/cobwebx §fdeletegroup §8<§fname§8>");
        sender.sendMessage("§7/cobwebx §faddgroup §8<§fregion§8> <§fgroup§8>");
        sender.sendMessage("§7/cobwebx §faddblock §8<§fblock§8> <§fgroup§8>");
        sender.sendMessage("§7/cobwebx §fviewgroup §8[§fgroup§8]");
        sender.sendMessage("");

        sender.sendMessage("§bAdmin tools");
        sender.sendMessage("§7/cobwebx §fbypass §8(§cOP only§8)");
        line(sender);
    }

    private void sendInfo(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());

        sender.sendMessage("§bCobWebX §7Plugin Information");
        sender.sendMessage("§7Version: §f" + version);
        sender.sendMessage("§7Author(s): §f" + authors);
    }

    // =====================================================
    // Group commands
    // =====================================================

    private void createGroup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CobWebX.prefix + "§cUse: §7/cobwebx creategroup <name>");
            return;
        }

        String groupName = args[1];

        if (plugin.getConfig().contains("groups." + groupName)) {
            sender.sendMessage(CobWebX.prefix + "§cGroup already exists!");
            return;
        }

        plugin.getConfig().set("groups." + groupName + ".regions", new ArrayList<String>());
        plugin.getConfig().set("groups." + groupName + ".blocks", new ArrayList<String>());
        plugin.getConfig().set("groups." + groupName + ".block_remove_delay", 1200);
        plugin.saveConfig();

        sender.sendMessage(CobWebX.prefix + "§aGroup created: §e" + groupName + " §7(default delay: §f1200 ticks§7)");
    }

    private void deleteGroup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(CobWebX.prefix + "§cUse: §7/cobwebx deletegroup <name>");
            return;
        }

        String group = args[1];

        if (!plugin.getConfig().contains("groups." + group)) {
            sender.sendMessage(CobWebX.prefix + "§cGroup does not exist!");
            return;
        }

        plugin.getConfig().set("groups." + group, null);
        plugin.saveConfig();

        sender.sendMessage(CobWebX.prefix + "§aGroup deleted: §e" + group);
    }

    private void addRegionToGroup(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CobWebX.prefix + "§cUse: §7/cobwebx addgroup <region> <group>");
            return;
        }

        String regionName = args[1];
        String group = args[2];

        if (!plugin.getConfig().contains("groups." + group)) {
            sender.sendMessage(CobWebX.prefix + "§cGroup does not exist!");
            return;
        }

        List<String> regions = plugin.getConfig().getStringList("groups." + group + ".regions");
        if (regions.contains(regionName)) {
            sender.sendMessage(CobWebX.prefix + "§cRegion already exists in that group!");
            return;
        }

        regions.add(regionName);
        plugin.getConfig().set("groups." + group + ".regions", regions);
        plugin.saveConfig();

        sender.sendMessage(CobWebX.prefix + "§aAdded region §e" + regionName + " §ato group §e" + group);
    }

    private void addBlockToGroup(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(CobWebX.prefix + "§cUse: §7/cobwebx addblock <block> <group>");
            return;
        }

        String blockName = args[1].toUpperCase(Locale.ROOT);
        Material mat = Material.getMaterial(blockName);

        if (mat == null) {
            sender.sendMessage(CobWebX.prefix + "§cInvalid block name!");
            return;
        }

        String group = args[2];
        if (!plugin.getConfig().contains("groups." + group)) {
            sender.sendMessage(CobWebX.prefix + "§cGroup does not exist!");
            return;
        }

        List<String> blocks = plugin.getConfig().getStringList("groups." + group + ".blocks");
        if (blocks.contains(mat.name())) {
            sender.sendMessage(CobWebX.prefix + "§cBlock already exists in that group!");
            return;
        }

        blocks.add(mat.name());
        plugin.getConfig().set("groups." + group + ".blocks", blocks);
        plugin.saveConfig();

        sender.sendMessage(CobWebX.prefix + "§aAdded block §e" + mat.name() + " §ato group §e" + group);
    }

    private void viewGroup(CommandSender sender, String[] args) {
        if (!plugin.getConfig().contains("groups")) {
            sender.sendMessage(CobWebX.prefix + "§cNo groups saved!");
            return;
        }

        sender.sendMessage("§bCobWebX §7Groups, Regions & Blocks");

        if (args.length >= 2) {
            String group = args[1];
            if (!plugin.getConfig().contains("groups." + group)) {
                sender.sendMessage(CobWebX.prefix + "§cGroup §e" + group + " §cdoes not exist!");
                return;
            }
            sendGroupInfo(sender, group);
            return;
        }

        for (String group : plugin.getConfig().getConfigurationSection("groups").getKeys(false)) {
            sendGroupInfo(sender, group);
        }
    }

    private void sendGroupInfo(CommandSender sender, String group) {
        List<String> groupRegions = plugin.getConfig().getStringList("groups." + group + ".regions");
        List<String> groupBlocks = plugin.getConfig().getStringList("groups." + group + ".blocks");
        int delay = plugin.getConfig().getInt("groups." + group + ".block_remove_delay", 1200);

        sender.sendMessage("§e" + group);
        sender.sendMessage(" §8• §7Regions: §f" + (groupRegions.isEmpty() ? "None" : String.join(", ", groupRegions)));
        sender.sendMessage(" §8• §7Blocks: §f" + (groupBlocks.isEmpty() ? "None" : String.join(", ", groupBlocks)));
        sender.sendMessage(" §8• §7Remove Delay: §f" + delay + " ticks §8(§f" + (delay / 20) + "s§8)");
    }

    // =====================================================
    // Bypass
    // =====================================================

    private void toggleBypass(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CobWebX.prefix + "§cOnly players can use this command!");
            return;
        }

        if (!player.isOp()) {
            player.sendMessage(CobWebX.prefix + "§cYou must be OP to use bypass.");
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean enabled;

        if (plugin.bypassPlayers.contains(uuid)) {
            plugin.bypassPlayers.remove(uuid);
            enabled = false;
        } else {
            plugin.bypassPlayers.add(uuid);
            enabled = true;
        }

        player.sendMessage("§bCobWebX §7Bypass");
        player.sendMessage("§7Status: " + (enabled ? "§aENABLED" : "§cDISABLED"));
        player.sendMessage("§7Effect: §f" + (enabled
                ? "You ignore CobWebX restrictions in configured regions."
                : "CobWebX restrictions apply normally."));
    }

    private void line(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
    }

    // =====================================================
    // TabComplete
    // =====================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return startsWith(SUBCOMMANDS, args[0]);
        }

        if (!plugin.getConfig().contains("groups")) return Collections.emptyList();

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            // viewgroup <group>
            // deletegroup <group>
            if (sub.equals("viewgroup") || sub.equals("deletegroup")) {
                List<String> groups = new ArrayList<>(plugin.getConfig().getConfigurationSection("groups").getKeys(false));
                return startsWith(groups, args[1]);
            }

            // addblock <block> <group> -> arg2 = BLOQUE
            if (sub.equals("addblock")) {
                List<String> mats = new ArrayList<>();
                for (Material m : Material.values()) {
                    if (m.isBlock()) mats.add(m.name()); // solo bloques (más limpio)
                }
                return startsWith(mats, args[1]);
            }

            // creategroup <name> -> sin autocompletar
            // addgroup <region> <group> -> región no se puede autocompletar fácil (WG)
            return Collections.emptyList();
        }

        if (args.length == 3) {
            // addgroup <region> <group> -> arg3 = GROUP
            // addblock <block> <group> -> arg3 = GROUP
            if (sub.equals("addgroup") || sub.equals("addblock")) {
                List<String> groups = new ArrayList<>(plugin.getConfig().getConfigurationSection("groups").getKeys(false));
                return startsWith(groups, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> startsWith(List<String> options, String prefix) {
        if (prefix == null) prefix = "";
        String p = prefix.toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(p)) out.add(opt);
        }
        return out;
    }
}