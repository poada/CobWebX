package cw.cobwebx.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import cw.cobwebx.CobWebX;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CobWebXListener implements Listener {

    private final CobWebX plugin;

    public CobWebXListener(CobWebX plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static final class GroupMatch {
        private final long delayTicks;

        private GroupMatch(long delayTicks) {
            this.delayTicks = delayTicks;
        }
    }

    private boolean hasBypass(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.isOp()) {
            return !plugin.bypassPlayers.contains(uuid);
        }

        return false;
    }

    private GroupMatch matchGroup(Block block, Material material) {
        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(block.getWorld()));

        if (regionManager == null) {
            plugin.debug("RegionManager is null for world " + block.getWorld().getName());
            return null;
        }

        ApplicableRegionSet applicable = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()));
        if (applicable == null || applicable.size() == 0) {
            plugin.debug("No applicable regions at " + block.getLocation());
            return null;
        }

        String worldName = block.getWorld().getName().toLowerCase();
        Set<String> regionIds = applicable.getRegions().stream()
                .map(region -> region.getId().toLowerCase())
                .collect(Collectors.toSet());

        plugin.debug("Checking block " + material.name() + " at world=" + worldName + " regions=" + regionIds);

        for (CobWebX.GroupCache group : plugin.getCachedGroups()) {
            if (!group.isEnabled()) {
                continue;
            }

            if (!group.allowsBlock(material)) {
                continue;
            }

            for (String regionId : regionIds) {
                if (group.containsRegion(worldName, regionId)) {
                    plugin.debug("Match found in group " + group.getName() + " with delay " + group.getDelayTicks() + " ticks.");
                    return new GroupMatch(group.getDelayTicks());
                }
            }
        }

        plugin.debug("No matching group found for block " + material.name() + " at " + block.getLocation());
        return null;
    }

    private void takeOneFromHand(Player player, EquipmentSlot hand, Material expectedType) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        if (item.getType() != expectedType) {
            return;
        }

        if (item.getAmount() <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return;
        }

        item.setAmount(item.getAmount() - 1);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            plugin.debug("Player " + player.getName() + " has bypass.");
            return;
        }

        Block block = event.getBlockPlaced();
        Material type = block.getType();

        if (!plugin.isControlledBlock(type)) {
            return;
        }

        GroupMatch match = matchGroup(block, type);

        event.setCancelled(true);

        if (match == null) {
            plugin.debug("Place cancelled because no group matched for " + type.name());
            return;
        }

        EquipmentSlot hand = event.getHand();
        BlockData blockData = block.getBlockData().clone();
        var location = block.getLocation();
        long delayTicks = match.delayTicks;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Block placed = location.getBlock();
            placed.setType(type, false);
            placed.setBlockData(blockData, false);

            takeOneFromHand(player, hand, type);

            plugin.debug("Placed " + type.name() + " manually at " + location + " with delay " + delayTicks + " ticks.");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Block current = location.getBlock();
                if (current.getType() == type) {
                    current.setType(Material.AIR, false);
                    plugin.debug("Removed " + type.name() + " automatically at " + location);
                }
            }, delayTicks);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }

        Block block = event.getBlock();
        Material type = block.getType();

        if (!plugin.isControlledBlock(type)) {
            return;
        }

        GroupMatch match = matchGroup(block, type);

        event.setCancelled(true);

        if (match == null) {
            plugin.debug("Break cancelled because no group matched for " + type.name());
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() == Material.AIR) {
                return;
            }

            if (player.getGameMode() != GameMode.CREATIVE) {
                for (ItemStack drop : block.getDrops(tool, player)) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }

            block.setType(Material.AIR, false);
            plugin.debug("Broke block " + type.name() + " at " + block.getLocation());
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }

        if (!plugin.isControlledBlock(block.getType())) {
            return;
        }

        GroupMatch match = matchGroup(block, block.getType());

        if (match == null) {
            event.setCancelled(true);
            plugin.debug("Use cancelled for block " + block.getType().name() + " at " + block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onUseOverride(PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        if (hasBypass(player)) {
            return;
        }

        if (!plugin.isControlledBlock(block.getType())) {
            return;
        }

        GroupMatch match = matchGroup(block, block.getType());
        if (match == null) {
            return;
        }

        event.setCancelled(false);
        plugin.debug("Use override applied for block " + block.getType().name() + " at " + block.getLocation());
    }
}