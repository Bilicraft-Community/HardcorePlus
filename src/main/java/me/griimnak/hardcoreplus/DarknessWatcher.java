package me.griimnak.hardcoreplus;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DarknessWatcher extends BukkitRunnable {
    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if(onlinePlayer.getWorld().getEnvironment() == World.Environment.NETHER)
                continue;
            Block playerLocationBlock = onlinePlayer.getLocation().getBlock();
            int blockLight = playerLocationBlock.getLightFromBlocks();
            int selfLight = playerLocationBlock.getLightLevel();
            int skyLight = playerLocationBlock.getLightFromSky();
            int light = Math.max(blockLight,selfLight);
            if(!(onlinePlayer.getWorld().getTime() > 13000)){
                light = Math.max(light,skyLight);
            }
            if(light < 4){
                onlinePlayer.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,10,50),true);
            }
        }
    }
}
