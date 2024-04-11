package dev.goldenn.bettergroups;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.*;


public class BetterGroups extends JavaPlugin implements CommandExecutor, Listener {


    private Map<UUID, List<String>> playerGroups = new HashMap<>();
    private Map<String, String> groups = new HashMap<>();
    private Map<String, Integer> groupHierarchy = new HashMap<>();
    private Map<String, Set<UUID>> groupUsers = new HashMap<>();
    private String chatFormat;


    @Override
    public void onEnable() {
        // Load groups and group hierarchy from config
        FileConfiguration config = getConfig();
        ConfigurationSection groupsConfig = config.getConfigurationSection("groups");
        if (groupsConfig != null) {
            for (String groupName : groupsConfig.getKeys(false)) {
                String prefix = groupsConfig.getString(groupName + ".prefix");
                int hierarchy = groupsConfig.getInt(groupName + ".hierarchy");
                groups.put(groupName, prefix);

                // Load users as UUIDs from config
                Set<String> userStrings = new HashSet<>(groupsConfig.getStringList(groupName + ".users"));
                Set<UUID> users = userStrings.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toSet());
                groupUsers.put(groupName, users);

                groupHierarchy.put(groupName, hierarchy);
            }
        }

        // Load chat format from config
        chatFormat = config.getString("chatFormat", "{TAG}&r {PLAYER}:&r {MESSAGE}");

        // Register commands and events
        this.getCommand("addtogroup").setExecutor(this);
        this.getCommand("removefromgroup").setExecutor(this);
        this.getCommand("tagreload").setExecutor(this);
        this.getCommand("creategroup").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("creategroup")) {
            if (args.length >= 2) {
                String groupName = args[0];
                String prefix = args[1];

                if (!groups.containsKey(groupName)) {
                    int hierarchy = args.length > 2 ? Integer.parseInt(args[2]) : 0;

                    groups.put(groupName, prefix);
                    groupHierarchy.put(groupName, hierarchy);

                    // Save to config
                    FileConfiguration config = getConfig();
                    config.set("groups." + groupName + ".prefix", prefix);
                    config.set("groups." + groupName + ".hierarchy", hierarchy);
                    saveConfig();

                    sender.sendMessage(ChatColor.GREEN + "Group created successfully!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Group already exists!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("addtogroup")) {
            if (args.length >= 2) {
                String groupName = args[0];
                String playerName = args[1];

                if (groups.containsKey(groupName)) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        UUID playerUUID = player.getUniqueId();
                        Set<UUID> users = groupUsers.getOrDefault(groupName, new HashSet<>());
                        if (!users.contains(playerUUID)) {
                            users.add(playerUUID);
                            groupUsers.put(groupName, users);

                            // Save to config
                            FileConfiguration config = getConfig();
                            config.set("groups." + groupName + ".users", new ArrayList<>(users.stream().map(UUID::toString).collect(Collectors.toList())));
                            saveConfig();

                            sender.sendMessage(ChatColor.GREEN + "User added to group successfully!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "User is already in the group!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Group does not exist!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("removefromgroup")) {
            if (args.length == 2) {
                String playerName = args[0];
                String groupName = args[1];

                if (groups.containsKey(groupName)) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        UUID playerUUID = player.getUniqueId();
                        Set<UUID> users = groupUsers.getOrDefault(groupName, new HashSet<>());
                        if (users.contains(playerUUID)) {
                            users.remove(playerUUID);
                            groupUsers.put(groupName, users);

                            // Save to config
                            FileConfiguration config = getConfig();
                            config.set("groups." + groupName + ".users", new ArrayList<>(users.stream().map(UUID::toString).collect(Collectors.toList())));
                            saveConfig();

                            sender.sendMessage(ChatColor.GREEN + "User removed from group successfully!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "User is not in the group!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Group does not exist!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("tagreload")) {
            reloadConfig();

            // Reload groups, group hierarchy, and chat format from config
            groups.clear();
            groupHierarchy.clear();

            FileConfiguration config = getConfig();
            ConfigurationSection groupsConfig = config.getConfigurationSection("groups");
            if (groupsConfig != null) {
                for (String groupName : groupsConfig.getKeys(false)) {
                    String prefix = groupsConfig.getString(groupName + ".prefix");
                    int hierarchy = groupsConfig.getInt(groupName + ".hierarchy");
                    groups.put(groupName, prefix);
                    groupHierarchy.put(groupName, hierarchy);
                }
            }

            // Reload chat format
            chatFormat = config.getString("chatFormat", "{TAG}&r {PLAYER}:&r {MESSAGE}");

            sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
            return true;
        }
        return false;
    }

    private void addGroup(UUID playerId, String groupName) {
        List<String> groups = playerGroups.computeIfAbsent(playerId, k -> new ArrayList<>());
        groups.add(groupName);

        // Sort groups by hierarchy
        groups.sort(Comparator.comparingInt(group -> groupHierarchy.getOrDefault(group, 0)));

        // Remove duplicate groups
        Set<String> uniqueGroups = new LinkedHashSet<>(groups);
        groups.clear();
        groups.addAll(uniqueGroups);
    }

    private void removeGroup(UUID playerId, String groupName) {
        List<String> groups = playerGroups.get(playerId);
        if (groups != null) {
            groups.remove(groupName);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String message = event.getMessage();

        String playerGroup = null;
        int highestHierarchy = Integer.MIN_VALUE;

        for (Map.Entry<String, Set<UUID>> entry : groupUsers.entrySet()) {
            if (entry.getValue().contains(player.getUniqueId())) {
                int hierarchy = groupHierarchy.get(entry.getKey());
                if (hierarchy > highestHierarchy) {
                    highestHierarchy = hierarchy;
                    playerGroup = entry.getKey();
                }
            }
        }

        if (playerGroup != null) {
            String prefix = groups.get(playerGroup);
            String formattedMessage = chatFormat
                    .replace("{TAG}", prefix)
                    .replace("{PLAYER}", playerName)
                    .replace("{MESSAGE}", message);

            event.setFormat(ChatColor.translateAlternateColorCodes('&', formattedMessage));
        }
    }

}
