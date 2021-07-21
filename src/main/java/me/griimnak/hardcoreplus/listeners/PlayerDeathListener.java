package me.griimnak.hardcoreplus.listeners;

import me.griimnak.hardcoreplus.HardcorePlus;
import me.griimnak.hardcoreplus.config.ConfigManager;
import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.util.Properties;

public class PlayerDeathListener implements Listener {
    final HardcorePlus plugin;

    public PlayerDeathListener(HardcorePlus plugin) {
        this.plugin = plugin;
    }

    private static boolean loadStatsFile(File statsFile, Properties properties) {
        if (!statsFile.exists() || !statsFile.isFile()) {
            properties = new Properties();
        } else {
            try {
                properties.load(new FileInputStream(statsFile));
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        long lastRespawnAll = ConfigManager.config.getLong("lastrespawnall");
        if (ConfigManager.config.getLong("deaths." + event.getPlayer().getUniqueId() + ".time") <= lastRespawnAll) {
            event.getPlayer().teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            event.getPlayer().setGameMode(GameMode.SURVIVAL);
            ConfigManager.config.set("deaths." + event.getPlayer().getUniqueId(),null);
            ConfigManager.saveConfig();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setDeathMessage(ChatColor.RED + "" + ChatColor.BOLD + player.getName() + ConfigManager.config.getString("PermaDeathServerText"));
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + ConfigManager.config.getString("PermaDeathPlayerText"));

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0D);
        updateStatsFile();

        ConfigManager.config.set("deaths." + event.getEntity().getUniqueId() + ".time", System.currentTimeMillis());

        // if perma ban enabled
        if (ConfigManager.config.getBoolean("PermaBanOnFinalDeathEnabled")) {
            // delay 10 ticks
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                // ban
                plugin.getServer().getBanList(Type.NAME).addBan(player.getName(), ChatColor.RED + ConfigManager.config.getString("PermaBanText") + ChatColor.RESET, null, "HardcorePlus");
                player.kickPlayer(ChatColor.RED + ConfigManager.config.getString("PermaBanText") + ChatColor.RESET);
            }, 10);
        }
    }

    private boolean updateStatsFile() {
        File statsFile = new File(plugin.getDataFolder(), "stats.properties");
        Properties properties = new Properties();
        if (loadStatsFile(statsFile, properties)) return true;

        if (properties.getProperty("total-perm-dead-players") == null) {
            properties.setProperty("total-perm-dead-players", "0");
        }

        int total = Integer.parseInt(properties.getProperty("total-perm-dead-players"));
        properties.setProperty("total-perm-dead-players", total + 1 + "");

        try {
            properties.store(new FileOutputStream(statsFile), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}
