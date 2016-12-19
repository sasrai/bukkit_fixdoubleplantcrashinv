package jp.sasrai.fixdpcrash;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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
            if (item != null) plugin.getLogger().info("[INV] " + item.getType().name() + " :: " + item.getDurability());
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

    private void loadAndChunkCheck(Location location) {
        Chunk centerChunk = location.getChunk();
        World world = centerChunk.getWorld();
        int centerChunkX = centerChunk.getX();
        int centerChunkZ = centerChunk.getZ();

        boolean fixed;

        centerChunk.load(false);
        fixed = fixChunk(centerChunk, false);
        for (int i = 1; i < plugin.getServer().getViewDistance(); i++) {
            for (int j = 0; j < i * 2; j++) {
                int offset = i - 1;
                Chunk checkChunk;

                checkChunk = world.getChunkAt(centerChunkX - i, centerChunkZ + (j - offset));
                checkChunk.load(false);
                if (fixChunk(checkChunk, false)) fixed = true;

                checkChunk = world.getChunkAt(centerChunkX + i, centerChunkZ + (-j + offset));
                checkChunk.load(false);
                if (fixChunk(checkChunk, false)) fixed = true;

                checkChunk = world.getChunkAt(centerChunkX + (-j + offset), centerChunkZ - i);
                checkChunk.load(false);
                if (fixChunk(checkChunk, false)) fixed = true;

                checkChunk = world.getChunkAt(centerChunkX + (j - offset), centerChunkZ + i);
                checkChunk.load(false);
                if (fixChunk(checkChunk, false)) fixed = true;
            }
        }

        if (fixed) centerChunk.getWorld().save();
    }

    private boolean fixChunkWithSave(Chunk chunk) {
        return fixChunk(chunk, true);
    }
    private boolean fixChunk(Chunk chunk, boolean save) {
        boolean fixed = false;
        if (!chunk.isLoaded()) return false;

        for (int y = 0; y < 255; y += 2) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType() == plugin.doubleplantMaterial && fixBlock(block)) {
                        plugin.getLogger().warning("[" + block.getX() + ","  + y + ","  + block.getZ() + "] fixed double plant block.");
                        fixed = true;
                    }
                }
            }
        }

        if (save && fixed) {
            chunk.getWorld().save();
        }

        return fixed;
    }
    private boolean fixBlock(Block block) {
        boolean result = false;

        Block blockBelow = block.getRelative(0, -1, 0);
        Block topBlock = block.getRelative(0, 1, 0);

        if (blockBelow.getType() == plugin.doubleplantMaterial) {
            if (block.getData() != plugin.topDPBlockMetadata) {
                block.setData((byte) plugin.topDPBlockMetadata);
                result = true;
            }
            if (blockBelow.getData() > plugin.maxDPMetadata) {
                blockBelow.setData(plugin.maxDPMetadata);
                result = true;
            }

        } else if (topBlock.getType() == plugin.doubleplantMaterial) {
            if (block.getData() > plugin.maxDPMetadata) {
                block.setData((block.getData() == plugin.maxDPMetadata + 1) ? (byte) 0 : plugin.maxDPMetadata);
                result = true;
            }
        }

        return result;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isCheckLoginPlayerInventory) return;
        fixDoublePlantPlayerInventory(event.getPlayer());
        if (plugin.isCheckLoginAreaChunk) {
            loadAndChunkCheck(event.getPlayer().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemPickup(InventoryClickEvent event) {
        if (!plugin.isCheckPickup) return;
        fixDoublePlant(event.getCursor());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.isCheckChunkLoad) return;
        fixChunkWithSave(event.getChunk());
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
            if (fixBlock(target)) {
                plugin.getLogger().warning(event.getPlayer().getName() + " : fixed DoublePlant block " + target.getLocation().toString());
                if (!plugin.isBlockClickedFixMessageSilent)
                    event.getPlayer().sendMessage("[WARN] fixed DoublePlant block.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (event.getPlayer().isSneaking()) {
            Block target = event.getPlayer().getTargetBlock(null, 1000); // 1.7.10 Deprecated.

            if (target != null && clickedToolCheck(event.getPlayer().getItemInHand())
                    && target.getType() == plugin.doubleplantMaterial) {
                if (fixBlock(target)) {
                    plugin.getLogger().warning(event.getPlayer().getName() + " : fixed DoublePlant block " + target.getLocation().toString());
                    if (!plugin.isBlockClickedFixMessageSilent)
                        event.getPlayer().sendMessage("[WARN] fixed DoublePlant block.");
                }
            }
        }
    }
}
