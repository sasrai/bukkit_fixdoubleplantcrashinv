package jp.sasrai.fixdpcrash;
/**
 *  * Created by sasrai on 2016/12/13.
 */

import org.bukkit.plugin.java.JavaPlugin;

public class FixDoublePlantCrashInventory extends JavaPlugin {
  @Override
  public void onEnable() {
    super.onEnable();

    getLogger().info("Enabled fix double plant plugin.");
    new EventListener(this);
  }

  @Override
  public void onDisable() {
    super.onDisable();
  }
}

