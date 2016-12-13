package jp.sasrai.fixdpcrash;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Created by sasrai on 2016/12/13.
 */
public class EventListener implements Listener {
    final FixDoublePlantCrashInventory plugin;

    public EventListener(FixDoublePlantCrashInventory plugin) {
        this.plugin = plugin;

        registerEvents();
    }

    private void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void fixDoublePlantPlayerInventory(Player player) {
        for (ItemStack item: player.getInventory().getContents()) {
            if (null != fixDoublePlant(item)) {
                player.sendMessage("[WARN] fixed DoublePlant.");
            }
        }
    }
    private ItemStack fixDoublePlant(ItemStack item) {
        if (item != null && item.getType() == Material.DOUBLE_PLANT && item.getDurability() > 5) {
            item.setDurability((short) 0);
            plugin.getLogger().info("fixed double plant");
            return item;
        }
        return null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        fixDoublePlantPlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemPickup(InventoryClickEvent event) {
        fixDoublePlant(event.getCursor());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        boolean fixed = false;
        for (int y = 0; y < 255; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = event.getChunk().getBlock(x, y, z);
                    if (block.getType() == Material.DOUBLE_PLANT) {
                        byte data = block.getData();
                        if (data > 5 && data != 8) {
                            if (block.getRelative(0, -1, 0).getType() == Material.DOUBLE_PLANT) {
                                block.setData((byte) 8);
                            } else {
                                block.setData((byte) 0);
                            }
                            fixed = true;

                            plugin.getLogger().warning("[" + x + ","  + y + ","  + z + "] fixed double plant block.");
                        }
                    }
                }
            }
        }

        if (fixed) {
            event.getWorld().save();
        }
    }
}
