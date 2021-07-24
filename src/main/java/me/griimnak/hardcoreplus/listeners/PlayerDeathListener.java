package me.griimnak.hardcoreplus.listeners;

import com.google.common.collect.ImmutableList;
import me.griimnak.hardcoreplus.DescParseTickFormat;
import me.griimnak.hardcoreplus.HardcorePlus;
import me.griimnak.hardcoreplus.config.ConfigManager;
import org.bukkit.BanList.Type;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
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
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()).hasPlayedBefore()) {
            long gameTime = event.getPlayer().getWorld().getTime();
            if (gameTime > 10000) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "\n为避免出生即死亡，新玩家只能在游戏时间的 6:00-12:00 之间加入。\n现在游戏时间: " + DescParseTickFormat.format24(gameTime));
            }
        }
    }

    @EventHandler
    public void eatGoldenApple(PlayerItemConsumeEvent event) {
        if (event.getItem().getType().equals(Material.ENCHANTED_GOLDEN_APPLE)) {
            event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0D);
            event.getPlayer().sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + ConfigManager.config.getString("MaxHealthRestoreText"));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            ItemStack stack = new ItemStack(Material.FIRE_CHARGE, 2);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "应急火种");
            meta.setLore(ImmutableList.of(ChatColor.WHITE + "" + ChatColor.ITALIC + "你非常清楚只有在迫不得已的时候才能使用它..."));
            stack.setItemMeta(meta);
            event.getPlayer().getInventory().addItem(stack);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "qa give deagle " + event.getPlayer().getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "qa give 9mm " + event.getPlayer().getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "qa give 9mm " + event.getPlayer().getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "qa give 9mm " + event.getPlayer().getName());
        }
        if (event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        long lastRespawnAll = ConfigManager.config.getLong("lastrespawnall");
        if (ConfigManager.config.getLong("deaths." + event.getPlayer().getUniqueId() + ".time") <= lastRespawnAll) {
            event.getPlayer().teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            event.getPlayer().setGameMode(GameMode.SURVIVAL);
            ConfigManager.config.set("deaths." + event.getPlayer().getUniqueId(), null);
            ConfigManager.saveConfig();
            event.getPlayer().sendMessage(ChatColor.GREEN + "神秘的力量使你忘记了过去，是时候再一次探索这个世界了!");
        }

       // Bukkit.getScheduler().runTaskLater(HardcorePlus.instance,()->event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(ConfigManager.config.getDouble("playermax."+ event.getPlayer().getUniqueId(),20.0D)), 5);

    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        ConfigManager.config.set("playermax."+ event.getPlayer().getUniqueId(),  event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        ConfigManager.saveConfig();
    }

    public String getSource(EntityDamageEvent damage) {
        if (!(damage.getEntity() instanceof LivingEntity)) {
            return "追踪失败";
        }
        LivingEntity entity = (LivingEntity) damage.getEntity();
        String e = "未知";
        if (damage != null) {
            e = damage.getCause().name().toLowerCase(Locale.ROOT);
            if (damage.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
                e = "darkness";
            }

            boolean skip = true;

            if (damage instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent attack = (EntityDamageByEntityEvent) damage;
                Entity attacker = attack.getDamager();

                if (attacker instanceof Player) {
                    Player player = (Player) attacker;
                    e = player.getName();
                } else if (attacker instanceof AbstractArrow) {
                    AbstractArrow arrow = (AbstractArrow) attacker;
                    ProjectileSource shooter = arrow.getShooter();

                    if (shooter instanceof Player) {
                        Player player = (Player) shooter;
                        e = player.getName();
                    } else if (shooter instanceof LivingEntity) {
                        EntityType entityType = ((LivingEntity) shooter).getType();
                        // Check for MyPet plugin
                        e = entityType.name().toLowerCase(Locale.ROOT);
                    }
                } else if (attacker instanceof ThrownPotion) {
                    ThrownPotion potion = (ThrownPotion) attacker;
                    ProjectileSource shooter = potion.getShooter();

                    if (shooter instanceof Player) {
                        Player player = (Player) shooter;
                        e = player.getName();
                    } else if (shooter instanceof LivingEntity) {
                        EntityType entityType = ((LivingEntity) shooter).getType();
                        e = entityType.name().toLowerCase(Locale.ROOT);
                    }
                } else {
                    attacker.getType();
                    e = attacker.getType().name().toLowerCase(Locale.ROOT);
                }
            } else {
                EntityDamageEvent.DamageCause cause = damage.getCause();
                if (cause.equals(EntityDamageEvent.DamageCause.FIRE)) {
                    e = "火焰";
                } else if (cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)) {
                    if (!skip) {
                        e = "火焰";
                    }
                } else if (cause.equals(EntityDamageEvent.DamageCause.LAVA)) {
                    e = "岩浆";
                } else if (cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
                    e = "爆炸";
                } else if (cause.equals(EntityDamageEvent.DamageCause.MAGIC)) {
                    e = "药水";
                }
            }

//                if (entity instanceof ArmorStand) {
//                    Location entityLocation = entity.getLocation();
//                    if (!Config.getConfig(entityLocation.getWorld()).ITEM_TRANSACTIONS) {
//                        entityLocation.setY(entityLocation.getY() + 0.99);
//                        Block block = entityLocation.getBlock();
//                        Queue.queueBlockBreak(e, block.getState(), Material.ARMOR_STAND, null, (int) entityLocation.getYaw());
//                    }
//                    return;
//                }

            EntityType entity_type = entity.getType();
            if (e.length() == 0) {
                // assume killed self
                if (!skip) {
                    if (!(entity instanceof Player) && entity_type.name() != null) {
                        // Player player = (Player)entity;
                        // e = player.getName();
                        e = entity_type.name().toLowerCase(Locale.ROOT);
                    } else if (entity instanceof Player) {
                        e = entity.getName();
                    }
                }
            }

            if (e.startsWith("wither")) {
                e = "凋零";
            }

            if (e.startsWith("enderdragon")) {
                e = "末影龙";
            }

            if (e.startsWith("primedtnt") || e.startsWith("tnt")) {
                e = "TNT";
            }

            if (e.startsWith("lightning")) {
                e = "闪电";
            }
        }
        return e;
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Bukkit.broadcastMessage(event.getEntity().getDisplayName()+" 剩余最大生命值: "+player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()+"@"+player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
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
        String sourceTracking = getSource(event.getEntity().getLastDamageCause());
        String damage = new Formatter().format("%.2f", event.getEntity().getLastDamageCause().getFinalDamage()).toString();
        Bukkit.broadcastMessage(event.getEntity().getDisplayName() + " 死于 " + sourceTracking + " 伤害: " + damage);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.TORCH || event.getBlockPlaced().getType() == Material.WALL_TORCH) {
            //  event.getBlockPlaced().getWorld().spawnParticle(Particle.CLOUD, event.getBlockPlaced().getLocation().add(0.5, 0.7, 0.5), 2, 0.0d, 0.0d, 0.0d, 0.01d);
            event.getBlockPlaced().getWorld().spawnParticle(Particle.SMOKE_NORMAL, event.getBlockPlaced().getLocation().add(0.5, 0.7, 0.5), 2, 0.0d, 0.0d, 0.0d, 0.01d);
            event.getBlockPlaced().getWorld().spawnParticle(Particle.BLOCK_CRACK, event.getBlockPlaced().getLocation().add(0.5, 0.5, 0.5), 2, 0.0d, 0.0d, 0.0d, event.getBlockPlaced().getBlockData());
            event.getBlockPlaced().getWorld().playSound(event.getBlockPlaced().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.1f, 0.0f);
            event.getBlockPlaced().setType(Material.AIR);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "你尝试将火把放在地上，但一个黑影掠过并将其夺走了");
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event){
       if(event.getBlock().getWorld().getEnvironment() == World.Environment.THE_END){
           event.setCancelled(true);
           event.setItemStack(new ItemStack(Material.BUCKET));
           event.getBlock().getWorld().spawnParticle(Particle.SMOKE_NORMAL, event.getBlock().getLocation().add(0.5, 0.7, 0.5), 2, 0.0d, 0.0d, 0.0d, 0.01d);
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
