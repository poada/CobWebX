package cw.cobwebx.listeners;

import cw.cobwebx.CobWebX;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CobWebXListener implements Listener {

    private final CobWebX plugin;

    public CobWebXListener(CobWebX plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // -------------------------
    // Models
    // -------------------------

    private static final class GroupMatch {
        final String group;
        final int delayTicks;

        GroupMatch(String group, int delayTicks) {
            this.group = group;
            this.delayTicks = delayTicks;
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

    private boolean isBypassed(Player p) {
        return plugin.bypassPlayers.contains(p.getUniqueId());
    }

    private boolean isInRegion(Block block, String regionName) {
        try {
            String id = regionName.toLowerCase(Locale.ROOT);

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager rm = container.get(BukkitAdapter.adapt(Objects.requireNonNull(block.getWorld())));
            if (rm == null) return false;

            ProtectedRegion region = rm.getRegion(id);
            return region != null && region.contains(block.getX(), block.getY(), block.getZ());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isAllowedForGroup(Material mat, String group) {
        List<String> blocks = plugin.getConfig().getStringList("groups." + group + ".blocks");
        String name = mat.name();
        for (String b : blocks) {
            if (name.equalsIgnoreCase(b)) return true;
        }
        return false;
    }

    private int getDelayForGroup(String group) {
        return plugin.getConfig().getInt("groups." + group + ".block_remove_delay", 170);
    }

    /**
     * Devuelve match si:
     * - el material está permitido en ese grupo
     * - y el bloque está dentro de alguna región de ese grupo
     */
    private GroupMatch matchGroup(Block block, Material mat) {
        if (!plugin.getConfig().contains("groups")) return null;

        for (String group : plugin.getConfig().getConfigurationSection("groups").getKeys(false)) {
            if (!isAllowedForGroup(mat, group)) continue;

            List<String> regions = plugin.getConfig().getStringList("groups." + group + ".regions");
            for (String r : regions) {
                if (isInRegion(block, r)) {
                    return new GroupMatch(group, getDelayForGroup(group));
                }
            }
        }
        return null;
    }

    /**
     * “Controlado” = aparece en blocks de cualquier grupo
     */
    private boolean isControlledBlock(Material mat) {
        if (!plugin.getConfig().contains("groups")) return false;

        String name = mat.name();
        for (String group : plugin.getConfig().getConfigurationSection("groups").getKeys(false)) {
            List<String> blocks = plugin.getConfig().getStringList("groups." + group + ".blocks");
            for (String b : blocks) {
                if (name.equalsIgnoreCase(b)) return true;
            }
        }
        return false;
    }

    private void takeOneFromHand(Player p, EquipmentSlot hand, Material expectedType) {
        if (p.getGameMode() == GameMode.CREATIVE) return;

        ItemStack item = (hand == EquipmentSlot.OFF_HAND)
                ? p.getInventory().getItemInOffHand()
                : p.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;
        if (item.getType() != expectedType) return;

        int amount = item.getAmount();
        if (amount <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) p.getInventory().setItemInOffHand(null);
            else p.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(amount - 1);
        }
    }

    // ============================================================
    // PLACE (modo evento)
    // ============================================================

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (isBypassed(p)) return;

        Block b = e.getBlockPlaced();
        Material type = b.getType();

        // No controlado -> no tocamos (WG/WE normal)
        if (!isControlledBlock(type)) return;

        GroupMatch match = matchGroup(b, type);

        // Controlado fuera de región -> deny
        if (match == null) {
            e.setCancelled(true);
            return;
        }

        // Dentro de región -> imitamos "cancel_event: false" (manual place)
        e.setCancelled(true);

        EquipmentSlot hand = e.getHand();
        int delay = match.delayTicks;

        Bukkit.getScheduler().runTask(plugin, () -> {
            // colocar manual
            b.setType(type, false);
            takeOneFromHand(p, hand, type);

            // auto-remove
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (b.getType() == type) b.setType(Material.AIR, false);
            }, delay);
        });
    }

    // ============================================================
    // BREAK (modo evento)
    // ============================================================

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (isBypassed(p)) return;

        Block b = e.getBlock();
        Material type = b.getType();

        if (!isControlledBlock(type)) return;

        GroupMatch match = matchGroup(b, type);

        if (match == null) {
            e.setCancelled(true);
            return;
        }

        // rompemos manual (como "cancel_event: false")
        e.setCancelled(true);

        ItemStack tool = p.getInventory().getItemInMainHand();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.getGameMode() != GameMode.CREATIVE) {
                for (ItemStack drop : b.getDrops(tool, p)) {
                    b.getWorld().dropItemNaturally(b.getLocation(), drop);
                }
            }
            b.setType(Material.AIR, false);
        });
    }

    // ============================================================
    // USE (modo evento)
    // ============================================================

    // 1) LOWEST: fuera de región -> deny SOLO si es bloque controlado
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        if (isBypassed(p)) return;

        Block b = e.getClickedBlock();
        Material type = b.getType();

        if (!isControlledBlock(type)) return;

        GroupMatch match = matchGroup(b, type);

        // Controlado fuera de región -> deny
        if (match == null) {
            e.setCancelled(true);
        }
        // Dentro de región -> dejamos que intente normal
    }

    // 2) MONITOR: si otro plugin canceló (WG “you can't use”), y está permitido en región -> lo forzamos
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onUseOverride(PlayerInteractEvent e) {
        if (!e.isCancelled()) return;

        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        if (isBypassed(p)) return;

        Block b = e.getClickedBlock();
        Material type = b.getType();

        if (!isControlledBlock(type)) return;

        GroupMatch match = matchGroup(b, type);
        if (match == null) return;

        // ✅ igual que "cancel_event: false"
        e.setCancelled(false);
    }
}