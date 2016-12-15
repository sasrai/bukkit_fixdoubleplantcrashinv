package jp.sasrai.fixdpcrash;
/**
 *  * Created by sasrai on 2016/12/13.
 */

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class FixDoublePlantCrashInventory extends JavaPlugin {
  private static final String ConfigKeyDoublePlantMaterial = "doubleplant.material";
  private static final String ConfigKeyMaxDPMetadata       = "doubleplant.maxmeta";
  private static final String ConfigKeyTopDPBlockMetadata  = "doubleplant.topmeta";

  private static final String ConfigKeyCheckSettingPickup               = "settings.pickup.enabled";
  private static final String ConfigKeyCheckSettingLoginPlayerInventory = "settings.loginInventory.enabled";
  private static final String ConfigKeyCheckSettingChunkLoad            = "settings.chunk.enabled";
  private static final String ConfigKeyCheckSettingBlockClicked         = "settings.blockClick.enabled";

  private static final String ConfigKeyBlockClickTools                  = "settings.blockClick.tools";
  private static final String ConfigKeyBlockClickMessageSilent          = "settings.blockClick.isSilent";

  private static final Material DefaultDoublePlantMaterial = Material.DOUBLE_PLANT;
  private static final byte DefaultMaxDPMetadata = 5;
  private static final byte DefaultTopDPBlockMetadata = 8;

  public Material doubleplantMaterial = DefaultDoublePlantMaterial;
  public byte maxDPMetadata = DefaultMaxDPMetadata;
  public byte topDPBlockMetadata = DefaultTopDPBlockMetadata;

  public boolean isCheckPickup = true;
  public boolean isCheckLoginPlayerInventory = true;
  public boolean isCheckChunkLoad = true;
  public boolean isCheckBlockClicked = true;

  public boolean isBlockClickedFixMessageSilent = false;

  public final Map<Material, List<Short>> clickTools = new HashMap<>();

  private void loadConfig() {
    getLogger().info("load config...");
    doubleplantMaterial = Material.matchMaterial(getConfig().getString("doubleplant.material", DefaultDoublePlantMaterial.name()).toUpperCase());
    if (null == doubleplantMaterial) {
      getLogger().log(Level.WARNING, ChatColor.RED + "%s : Unknown material name!!", getConfig().getString("doubleplant.material"));
      doubleplantMaterial = DefaultDoublePlantMaterial; // Default
    }
    maxDPMetadata = (byte) getConfig().getInt("doubleplant.maxmeta", DefaultMaxDPMetadata);
    if (maxDPMetadata > 15) {
      getLogger().log(Level.WARNING, ChatColor.RED + "%d : MaxMeta value is out of range!!", maxDPMetadata);
      maxDPMetadata = DefaultMaxDPMetadata; // Default
    }
    topDPBlockMetadata = (byte) getConfig().getInt("doubleplant.topmeta", DefaultTopDPBlockMetadata);
    if (topDPBlockMetadata > 15) {
      getLogger().log(Level.WARNING, ChatColor.RED + "%d : TopBlockMeta value is out of range!!", topDPBlockMetadata);
      topDPBlockMetadata = DefaultTopDPBlockMetadata; // Default
    }

    isCheckPickup = getConfig().getBoolean(ConfigKeyCheckSettingPickup, true);
    isCheckLoginPlayerInventory = getConfig().getBoolean(ConfigKeyCheckSettingLoginPlayerInventory, true);
    isCheckChunkLoad = getConfig().getBoolean(ConfigKeyCheckSettingChunkLoad, true);
    isCheckBlockClicked = getConfig().getBoolean(ConfigKeyCheckSettingBlockClicked, true);

    isBlockClickedFixMessageSilent = getConfig().getBoolean(ConfigKeyBlockClickMessageSilent, false);

    for (String toolName : getConfig().getStringList(ConfigKeyBlockClickTools)) {
      String[] parsedName = toolName.split(" *: *", 0);

      Material toolType = Material.matchMaterial(parsedName[0].toUpperCase());
      if (toolType == null) {
        getLogger().log(Level.WARNING, ChatColor.RED + "%s : Unknown material name!!", parsedName[0]);
        continue;
      }

      Short durability = null;
      try {
        if (parsedName.length > 1) durability = Short.parseShort(parsedName[1]);
      } catch (NumberFormatException e) {
        durability = null;
      }

      if (clickTools.containsKey(toolType)) {
        if (clickTools.get(toolType) != null) {
          if (durability != null) clickTools.get(toolType).add(durability);
          else clickTools.put(toolType, null);
        }
      } else {
        List<Short> newList = null;
        if (durability != null) {
          newList = new ArrayList<>();
          newList.add(durability);
        }
        clickTools.put(toolType, newList);
      }
    }
    if (isCheckBlockClicked) {
      for (Map.Entry<Material, List<Short>> tool : clickTools.entrySet()) {
        getLogger().info("ClickedTool :: " + tool.getKey().name()
                + ( (tool.getValue() == null) ? "" : (":" + tool.getValue().toString()) ));
      }
    }
  }

  @Override
  public void onEnable() {
    super.onEnable();

    // Config setting.
    this.getConfig().options().copyDefaults(true);
    this.saveDefaultConfig();

    loadConfig();

    new EventListener(this);
  }

  @Override
  public void onDisable() {
    super.onDisable();
  }
}

