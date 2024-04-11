package dev.goldenn.bettergroups;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class GroupManager implements Listener {

    private BetterGroups plugin;
    private Map<String, String> groups;
    private Map<String, Integer> groupHierarchy;
    private Map<String, Set<UUID>> groupUsers;

    public GroupManager(BetterGroups plugin) {
        this.plugin = plugin;
        this.groups = new HashMap<>();
        this.groupHierarchy = new HashMap<>();
        this.groupUsers = new HashMap<>();
        loadGroups();
    }

    private void loadGroups() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("groups")) {
            Set<String> groupNames = config.getConfigurationSection("groups").getKeys(false);
            for (String groupName : groupNames) {
                String prefix = config.getString("groups." + groupName + ".prefix");
                int hierarchy = config.getInt("groups." + groupName + ".hierarchy");
                groups.put(groupName, prefix);

                List<String> userUUIDStrings = config.getStringList("groups." + groupName + ".users");
                Set<UUID> userUUIDs = new HashSet<>();
                for (String userUUIDString : userUUIDStrings) {
                    userUUIDs.add(UUID.fromString(userUUIDString));
                }
                groupUsers.put(groupName, userUUIDs);

                groupHierarchy.put(groupName, hierarchy);
            }
        }
    }

    public boolean addGroup(String groupName, String prefix, int hierarchy) {
        if (!groups.containsKey(groupName)) {
            groups.put(groupName, prefix);
            groupHierarchy.put(groupName, hierarchy);
            groupUsers.put(groupName, new HashSet<>());

            // Save to config
            FileConfiguration config = plugin.getConfig();
            config.set("groups." + groupName + ".prefix", prefix);
            config.set("groups." + groupName + ".hierarchy", hierarchy);
            config.set("groups." + groupName + ".users", new ArrayList<>(groupUsers.get(groupName)));
            plugin.saveConfig();
            return true;
        }
        return false;
    }

    public boolean addUserToGroup(String groupName, UUID userUUID) {
        if (groups.containsKey(groupName)) {
            Set<UUID> users = groupUsers.get(groupName);
            if (!users.contains(userUUID)) {
                users.add(userUUID);
                groupUsers.put(groupName, users);

                // Save to config
                FileConfiguration config = plugin.getConfig();
                config.set("groups." + groupName + ".users", new ArrayList<>(groupUsers.get(groupName)));
                plugin.saveConfig();
                return true;
            }
        }
        return false;
    }

    public boolean removeUserFromGroup(String groupName, UUID userUUID) {
        if (groups.containsKey(groupName)) {
            Set<UUID> users = groupUsers.get(groupName);
            if (users.contains(userUUID)) {
                users.remove(userUUID);
                groupUsers.put(groupName, users);

                // Save to config
                FileConfiguration config = plugin.getConfig();
                config.set("groups." + groupName + ".users", new ArrayList<>(groupUsers.get(groupName)));
                plugin.saveConfig();
                return true;
            }
        }
        return false;
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
            String formattedMessage = plugin.getChatFormat()
                    .replace("{TAG}", prefix)
                    .replace("{PLAYER}", playerName)
                    .replace("{MESSAGE}", message);

            event.setFormat(ChatColor.translateAlternateColorCodes('&', formattedMessage));
        }
    }
}
