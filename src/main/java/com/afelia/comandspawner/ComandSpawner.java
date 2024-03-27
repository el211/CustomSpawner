package com.afelia.comandspawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class ComandSpawner extends JavaPlugin implements Listener {
    private HashMap<UUID, Long> cooldowns = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        config = getConfig();
        config.options().copyDefaults(true);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // Spawn command blocks based on configuration
        spawnCommandBlocks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("commandspawners")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("commandspawners.reload")) {
                    reloadConfig();
                    config = getConfig();
                    sender.sendMessage("Config reloaded.");
                    // Spawn command blocks after reloading config
                    spawnCommandBlocks();
                } else {
                    sender.sendMessage("You don't have permission to reload the config.");
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        for (String key : config.getConfigurationSection("commandBlocks").getKeys(false)) {
            Location blockLocation = getLocationFromConfig(key);
            if (blockLocation != null && player.getLocation().distance(blockLocation) <= config.getInt("commandBlocks." + key + ".range")) {
                String command = config.getString("commandBlocks." + key + ".command");
                long cooldownTime = config.getLong("commandBlocks." + key + ".cooldown") * 1000; // Convert seconds to milliseconds
                if (!playerHasCooldown(player.getUniqueId(), key, cooldownTime)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    setCooldown(player.getUniqueId(), key);
                }
            }
        }
    }

    private Location getLocationFromConfig(String key) {
        String[] parts = config.getString("commandBlocks." + key + ".location").split(",");
        if (parts.length == 4) {
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(Bukkit.getWorld(worldName), x, y, z);
        }
        return null;
    }

    private boolean playerHasCooldown(UUID playerId, String blockKey, long cooldownTime) {
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUse < cooldownTime) {
                return true;
            }
        }
        return false;
    }

    private void setCooldown(UUID playerId, String blockKey) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    private void spawnCommandBlocks() {
        // Loop through each command block in the configuration
        ConfigurationSection commandBlocksConfig = config.getConfigurationSection("commandBlocks");
        if (commandBlocksConfig != null) {
            for (String key : commandBlocksConfig.getKeys(false)) {
                Location blockLocation = getLocationFromConfig(key);
                String materialName = config.getString("blockTypes." + key);
                if (blockLocation != null && materialName != null) {
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        blockLocation.getBlock().setType(material);
                    } else {
                        getLogger().warning("Invalid material type specified for command block: " + materialName);
                    }
                }
            }
        }
    }
}
