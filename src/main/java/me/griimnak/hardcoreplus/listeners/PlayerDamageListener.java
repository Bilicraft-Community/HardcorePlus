package me.griimnak.hardcoreplus.listeners;

import me.griimnak.hardcoreplus.DescParseTickFormat;
import me.griimnak.hardcoreplus.HardcorePlus;
import me.griimnak.hardcoreplus.config.ConfigManager;
import org.bukkit.BanList.Type;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerDamageListener implements Listener {
    private final HardcorePlus plugin;

    public PlayerDamageListener(HardcorePlus plugin) {
        this.plugin = plugin;
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

    private final Random random = new Random();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDragonDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) {
            return;
        }
        EnderDragon dragon = (EnderDragon) event.getEntity();
        Location damagerLoc = event.getDamager().getLocation();
        Location dragonLoc = event.getEntity().getLocation();
        org.bukkit.util.Vector to = new Vector(damagerLoc.getBlockX(),damagerLoc.getBlockY(),damagerLoc.getBlockZ());
        org.bukkit.util.Vector from = new Vector(dragonLoc.getBlockX(),dragonLoc.getBlockY(),dragonLoc.getBlockZ());

        switch (random.nextInt(3)){
            case 0:
                dragon.launchProjectile(DragonFireball.class, to.subtract(from));
                break;
            case 1:
                dragon.launchProjectile(Arrow.class, to.subtract(from));
                break;
            case 2:
                Entity entity = dragon.getWorld().spawnEntity(dragon.getLocation(),EntityType.PRIMED_TNT);
                entity.setVelocity(to.subtract(from));
                break;
            case 3:
                dragon.launchProjectile(ShulkerBullet.class,to.subtract(from));
                break;
        }

        if (event.getDamager() instanceof Player) {
            for (Entity nearbyEntity : dragon.getNearbyEntities(100, 100, 100)) {
                if (nearbyEntity instanceof Enderman) {
                    ((Enderman) nearbyEntity).setTarget((LivingEntity) event.getDamager());
                }
            }
        }

        if (event.getDamager() instanceof AbstractArrow) {
            ProjectileSource source = ((AbstractArrow) event.getDamager()).getShooter();
            if (source instanceof Player) {
                for (Entity nearbyEntity : event.getEntity().getNearbyEntities(100, 100, 100)) {
                    if (nearbyEntity instanceof Enderman) {
                        ((Enderman) nearbyEntity).setTarget((LivingEntity) event.getDamager());
                    }
                }
            }
            if (dragon.getHealth() < 1000) {
                event.setCancelled(true);
                if (event.getDamager() instanceof Player) {
                    event.getDamager().sendMessage(ChatColor.RED + "当末影龙血量低于 1000 HP 时无法造成远程攻击伤害");
                }
            }
        }
        if (random.nextInt(20) == 0) {
            for (Entity nearbyEntity : dragon.getNearbyEntities(50, 50, 50)) {
                if (nearbyEntity instanceof Player) {
                    nearbyEntity.teleport(nearbyEntity.getLocation().clone().add(0, 100, 0));
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();

        if (damaged.isBlocking()) {
            return;
        }

        // Blood effect if enabled
        if (ConfigManager.config.getBoolean("BloodEffectEnabled")) {
            damaged.getWorld().playEffect(damaged.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
        }

        // if hp - dmg less or equal 0.0
        if ((damaged.getHealth() - event.getFinalDamage()) <= 0.0D) {

            /*
                ==========
                Totem fix [Dec 5 2019]
                ==========
                github.com/AleksanderEvensen
            */
            // Check if totems are enabled in the config
            if (ConfigManager.config.getBoolean("TotemOfUndyingWorks")) {
                // Check if player is holding a Totem of Undying in Main- or Offhand
                if (damaged.getInventory().getItemInMainHand().getType().equals(Material.TOTEM_OF_UNDYING) ||
                        damaged.getInventory().getItemInOffHand().getType().equals(Material.TOTEM_OF_UNDYING)) {

                    // Play the totem effect.
                    // Removing the totem from the players hand is unnecessary because the spawnParticle will handle it.
                    damaged.spawnParticle(Particle.TOTEM, damaged.getLocation(), 1);
                    damaged.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + ConfigManager.config.getString("TotemDeathPlayerText"));
                    // return so the player won't be teleported to their bed or loose the amount of hearts specified.
                    return;
                }
            }

            // max hp of damaged player
            double max_hp = damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            if (max_hp - ConfigManager.config.getDouble("LoseMaxHealthOnRespawnAmmount") > 0.0D) {
                // fake death
                String sourceTracking = getSource(event);
                String damage = new Formatter().format("%.2f", event.getFinalDamage()).toString();
                event.setCancelled(true);

                // announce death if enabled
                if (ConfigManager.config.getBoolean("AnnounceDeathEnabled")) {

                    EntityDamageEvent.DamageCause cause = (ConfigManager.config.getBoolean("CustomDeathMessagesEnabled")) ? event.getCause() : null;
                    if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                        broadcast(GetConfigString("dmsg_BlockExplosion").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.CONTACT) {
                        broadcast(GetConfigString("dmsg_Contact").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.CRAMMING) {
                        broadcast(GetConfigString("dmsg_Cramming").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
                        broadcast(GetConfigString("dmsg_DragonBreath").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.DROWNING) {
                        broadcast(GetConfigString("dmsg_Drowning").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                        broadcast(GetConfigString("dmsg_EntityExplosion").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.FALL) {
                        broadcast(GetConfigString("dmsg_Fall").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
                        broadcast(GetConfigString("dmsg_FallingBlock").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.FIRE) {
                        broadcast(GetConfigString("dmsg_Fire").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                        broadcast(GetConfigString("dmsg_FireTick").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
                        broadcast(GetConfigString("dmsg_FlyIntoWall").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                        broadcast(GetConfigString("dmsg_HotFloor").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.LAVA) {
                        broadcast(GetConfigString("dmsg_Lava").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
                        broadcast(GetConfigString("dmsg_Lightning").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.MAGIC) {
                        broadcast(GetConfigString("dmsg_Magic").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.POISON) {
                        broadcast(GetConfigString("dmsg_Poison").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
                        broadcast(GetConfigString("dmsg_Projectile").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.STARVATION) {
                        broadcast(GetConfigString("dmsg_Starvation").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
                        broadcast(GetConfigString("dmsg_Suffocation").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.SUICIDE) {
                        broadcast(GetConfigString("dmsg_Suicide").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.THORNS) {
                        broadcast(GetConfigString("dmsg_Thorns").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.VOID) {
                        broadcast(GetConfigString("dmsg_Void").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.WITHER) {
                        broadcast(GetConfigString("dmsg_Wither").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                        broadcast(GetConfigString("dmsg_EntityAttack").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else if (cause == EntityDamageEvent.DamageCause.FREEZE) {
                        broadcast(GetConfigString("dmsg_Freeze").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    } else {
                        broadcast(GetConfigString("AnnounceDeathText").replaceAll("%PLAYER%", damaged.getDisplayName()));
                    }
                    broadcast(damaged.getDisplayName() + " 死于 " + sourceTracking + " 伤害: " + damage);
                }

                // drop loot
                if (!ConfigManager.config.getBoolean("KeepInventoryEnabled")) {
                    // if item in off hand
                    if (damaged.getInventory().getItemInOffHand().getType() != Material.AIR) {
                        if (damaged.getInventory().getItemInOffHand().getEnchantments().containsKey(Enchantment.VANISHING_CURSE)) {
                            damaged.getInventory().getItemInOffHand().setAmount(0);
                        } else {
                            // drop
                            damaged.getWorld().dropItemNaturally(damaged.getLocation(), damaged.getInventory().getItemInOffHand());
                            damaged.getInventory().setItemInOffHand(null);
                        }
                    }

                    // helm
                    if (damaged.getInventory().getHelmet() != null) {
                        if (damaged.getInventory().getHelmet().getEnchantments().containsKey(Enchantment.VANISHING_CURSE)) {
                            damaged.getInventory().getHelmet().setAmount(0);
                        } else {
                            damaged.getWorld().dropItemNaturally(damaged.getLocation(), damaged.getInventory().getHelmet());
                            damaged.getInventory().setHelmet(null);
                        }
                    }

                    // chest
                    if (damaged.getInventory().getChestplate() != null) {
                        if (damaged.getInventory().getChestplate().getEnchantments().containsKey(Enchantment.VANISHING_CURSE)) {
                            damaged.getInventory().getChestplate().setAmount(0);
                        } else {
                            damaged.getWorld().dropItemNaturally(damaged.getLocation(), damaged.getInventory().getChestplate());
                            damaged.getInventory().setChestplate(null);
                        }
                    }

                    // legs
                    if (damaged.getInventory().getLeggings() != null) {
                        if (damaged.getInventory().getLeggings().getEnchantments().containsKey(Enchantment.VANISHING_CURSE)) {
                            damaged.getInventory().getLeggings().setAmount(0);
                        } else {
                            damaged.getWorld().dropItemNaturally(damaged.getLocation(), damaged.getInventory().getLeggings());
                            damaged.getInventory().setLeggings(null);
                        }
                    }

                    // boots
                    if (damaged.getInventory().getBoots() != null) {
                        if (damaged.getInventory().getBoots().getEnchantments().containsKey(Enchantment.VANISHING_CURSE)) {
                            damaged.getInventory().getBoots().setAmount(0);
                        } else {
                            damaged.getWorld().dropItemNaturally(damaged.getLocation(), damaged.getInventory().getBoots());
                            damaged.getInventory().setBoots(null);
                        }
                    }

                    // for each inv item
                    for (ItemStack i : damaged.getInventory().getContents()) {
                        // drop item
                        if (i != null) {
                            if (i.getEnchantments().containsKey(Enchantment.VANISHING_CURSE)) {
                                i.setAmount(0);
                            } else {
                                damaged.getWorld().dropItemNaturally(damaged.getLocation(), i);
                                damaged.getInventory().remove(i);
                            }


                        }
                    }
                }

                // drop xp
                if (!ConfigManager.config.getBoolean("KeepExperienceEnabled")) {
                    int total_xp = damaged.getLevel();
                    if (total_xp > 0) {
                        // drop xp
                        damaged.getWorld().spawn(damaged.getLocation(), ExperienceOrb.class).setExperience(total_xp);
                    }
                    // reset
                    damaged.setLevel(0);
                    damaged.setExp(0);
                }


                // degrade max hp if enabled
                if (ConfigManager.config.getBoolean("LoseMaxHealthOnRespawnEnabled")) {
                    damaged.setHealth(max_hp - ConfigManager.config.getDouble("LoseMaxHealthOnRespawnAmmount"));
                    damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max_hp - ConfigManager.config.getDouble("LoseMaxHealthOnRespawnAmmount"));
                    ConfigManager.config.set("playermax." + damaged.getUniqueId(), damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
                    damaged.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + ConfigManager.config.getString("HealthLossText"));
                    plugin.getLogger().info(damaged.getDisplayName() + " has lost permanent health.");
                } else {
                    damaged.setHealth(max_hp);
                }
                broadcast(damaged.getDisplayName()+" 剩余最大生命值: "+damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()+"@"+damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

                // re saturate
                damaged.setSaturation(5);
                damaged.setFoodLevel(20);
                damaged.setRemainingAir(200);
                damaged.setNoDamageTicks(20 * 10);
                damaged.setFallDistance(0.0f);
                event.setDamage(0.0);

                // clear effects
                damaged.getActivePotionEffects().clear();
                damaged.setFireTicks(0);
                for (PotionEffectType value : PotionEffectType.values()) {
                    damaged.removePotionEffect(value);
                }
//                if (event instanceof EntityDamageByEntityEvent) {
//                    if (!(((EntityDamageByEntityEvent) event).getDamager() instanceof Player)) {
//                        if (!(((EntityDamageByEntityEvent) event).getDamager() instanceof Boss)) {
//                            ((EntityDamageByEntityEvent) event).getDamager().remove();
//                        }
//                    }
//                }

                // respawn
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    if (damaged.getBedSpawnLocation() != null) {
                        damaged.teleport(damaged.getBedSpawnLocation());
                        damaged.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + ConfigManager.config.getString("RespawnBedText"));
                    } else {
                        damaged.teleport(damaged.getServer().getWorlds().get(0).getSpawnLocation());
                        damaged.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + ConfigManager.config.getString("RespawnWildText"));
                    }
                }, 5);

                // trippy effects if enabled
                if (ConfigManager.config.getBoolean("RespawnSoundEnabled")) {
                    damaged.getWorld().playEffect(damaged.getLocation(), Effect.ZOMBIE_CONVERTED_VILLAGER, 0);
                }
                if (ConfigManager.config.getBoolean("RespawnEffectEnabled")) {
                    damaged.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 220, 1));
                    damaged.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                }

                // ban effect if enabled
                if (ConfigManager.config.getBoolean("BanOnDeathEnabled")) {
                    if (damaged.hasPermission("hardcoreplus.ban_exempt")) {
                        // exempt permission
                        damaged.sendMessage("This server has death ban enabled, but you are exempt via permission.");
                        plugin.getLogger().info(damaged.getName() + " is has been pardoned from death ban.");
                    } else {
                        // if this isn't delayed it gives a weird "user took too long to log in" bug.
                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            if (damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() > 0) {
                                plugin.getServer().getBanList(Type.NAME).addBan(damaged.getName(), ChatColor.RED + ConfigManager.config.getString("BanOnDeathText") + ChatColor.RESET, getBanDate(ConfigManager.config.getInt("BanOnDeathHoursAmmount")), "HardcorePlus");
                                damaged.kickPlayer(ChatColor.RED + ConfigManager.config.getString("BanOnDeathText") + ChatColor.RESET);
                            }
                        }, 20);
                    }
                }
                World rworld;
                if (damaged.getBedSpawnLocation() != null) {
                    rworld = damaged.getBedSpawnLocation().getWorld();
                } else {
                    rworld = damaged.getServer().getWorlds().get(0);
                }
                long gameTime = rworld.getTime();
                if (gameTime > 10000 && damaged.getLocation().getBlock().getLightLevel() < 6) {
                    damaged.kickPlayer(ChatColor.YELLOW + "\n由于现在为夜间且您的复活位置没有光源，为了避免重复死亡，您已被强制下线。\n现在是游戏时间的 " + DescParseTickFormat.format24(gameTime) + "\n您可以选择等待天亮或立刻重新加入。");
                }

            }
        }

    }

    private Date getBanDate(int h) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR_OF_DAY, h);
        return c.getTime();
    }

    // Sends a message to the whole server
    private void broadcast(String msg) {
        plugin.getServer().broadcastMessage(msg);
    }

    private String GetConfigString(String configEntry) {
        return ConfigManager.config.getString(configEntry);
    }
}
