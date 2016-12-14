package jp.sasrai.fixdpcrash;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

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
                player.sendMessage("[WARN] fixed inventory DoublePlant.");
            }
        }
    }
    private ItemStack fixDoublePlant(ItemStack item) {
        if (item != null && item.getType() == Material.DOUBLE_PLANT && item.getDurability() > plugin.maxDPMetadata) {
            item.setDurability((short) 0);
            plugin.getLogger().info("fixed double plant");
            return item;
        }
        return null;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isCheckLoginPlayerInventory) return;
        fixDoublePlantPlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemPickup(InventoryClickEvent event) {
        if (!plugin.isCheckPickup) return;
        fixDoublePlant(event.getCursor());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.isCheckChunkLoad) return;
        boolean fixed = false;
        for (int y = 0; y < 255; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = event.getChunk().getBlock(x, y, z);
                    if (block.getType() == Material.DOUBLE_PLANT) {
                        byte data = block.getData();
                        if (data > plugin.maxDPMetadata && data != plugin.topDPBlockMetadata) {
                            if (block.getRelative(0, -1, 0).getType() == Material.DOUBLE_PLANT) {
                                block.setData(plugin.topDPBlockMetadata);
                            } else {
                                block.setData(plugin.maxDPMetadata);
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

    // MetaCycler等でメタデータを書き換えた時用チェック
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isCheckBlockClicked) return;
        if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && event.getClickedBlock().getType() == Material.DOUBLE_PLANT) {
            Block target = event.getClickedBlock();
            Block blockBelow = target.getRelative(0, -1, 0);
            if (blockBelow.getType() == Material.DOUBLE_PLANT && blockBelow.getData() != plugin.topDPBlockMetadata) {
                event.getPlayer().sendMessage("[WARN] fixed DoublePlant block.");
                target.setData((byte) plugin.topDPBlockMetadata);
                if (blockBelow.getData() > plugin.maxDPMetadata) blockBelow.setData(plugin.maxDPMetadata);
            } else if (target.getData() > plugin.maxDPMetadata) {
                event.getPlayer().sendMessage("[WARN] fixed DoublePlant block.");
                target.setData(plugin.maxDPMetadata);
            }
        }
    }
}
