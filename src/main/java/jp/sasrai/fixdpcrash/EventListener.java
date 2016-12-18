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

import java.util.List;
import java.util.Map;

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
        if (item != null && item.getType() == plugin.doubleplantMaterial && item.getDurability() > plugin.maxDPMetadata) {
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

                    if (block.getType() == plugin.doubleplantMaterial) {
                        byte data = block.getData();

                        plugin.getLogger().info("dp fix? " + block.toString());
                        if (data > plugin.maxDPMetadata && data != plugin.topDPBlockMetadata) {
                            if (block.getRelative(0, -1, 0).getType() == plugin.doubleplantMaterial) {
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

    private boolean clickedToolCheck(ItemStack _useTool) {
        ItemStack useTool = (_useTool == null) ? new ItemStack(Material.AIR) : _useTool;
        if (plugin.clickTools == null || plugin.clickTools.size() < 1) return true;

        for (Map.Entry<Material, List<Short>> tool : plugin.clickTools.entrySet()) {
            if (tool.getKey() == ((useTool == null) ? Material.AIR : useTool.getType())) {
                if (tool.getValue() == null) return true;
                if (tool.getValue().contains(useTool.getDurability())) return true;
            }
        }
        return false;
    }
    private void rewriteBlockMeta(Block block, byte metadata) {
        block.getChunk();

    }
    // MetaCycler等でメタデータを書き換えた時用チェック
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isCheckBlockClicked) return;

        Block target = null;

        // 空中クリック判定(距離1000までの視線上にあるブロックを取得)
        if (plugin.isBlockClickedFixAirClickCheck && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR)) {
            target = event.getPlayer().getTargetBlock(null, 1000); // 1.7.10 Deprecated.

        // ブロッククリック判定
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK){
            target = event.getClickedBlock();
        }

        if (target != null && clickedToolCheck(event.getItem())
                && target.getType() == plugin.doubleplantMaterial) {

            Block blockBelow = target.getRelative(0, -1, 0);
            Block topBlock = target.getRelative(0, 1, 0);

            if (blockBelow.getType() == plugin.doubleplantMaterial) {
                if (target.getData() != plugin.topDPBlockMetadata) {
                    plugin.getLogger().warning(event.getPlayer().getName() + " : fixed DoublePlant block " + target.getLocation().toString());
                    if (!plugin.isBlockClickedFixMessageSilent)
                        event.getPlayer().sendMessage("[WARN] fixed DoublePlant block.");
                    target.setData((byte) plugin.topDPBlockMetadata);
                }
                if (blockBelow.getData() > plugin.maxDPMetadata) blockBelow.setData(plugin.maxDPMetadata);

            } else if (topBlock.getType() == plugin.doubleplantMaterial) {
                if (target.getData() > plugin.maxDPMetadata) {
                    plugin.getLogger().warning(event.getPlayer().getName() + " : fixed DoublePlant block.");
                    if (!plugin.isBlockClickedFixMessageSilent)
                        event.getPlayer().sendMessage("[WARN] fixed DoublePlant block.");
                    target.setData((target.getData() == plugin.maxDPMetadata + 1) ? (byte) 0 : plugin.maxDPMetadata);
                }
            }
        }
    }
}
