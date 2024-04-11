package dev.goldenn.bettergroups;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {

    private BetterGroups plugin;
    private GroupManager groupManager;

    public CommandManager(BetterGroups plugin, GroupManager groupManager) {
        this.plugin = plugin;
        this.groupManager = groupManager;
        plugin.getCommand("creategroup").setExecutor(this);
        plugin.getCommand("addtogroup").setExecutor(this);
        plugin.getCommand("removefromgroup").setExecutor(this);
        plugin.getCommand("tagreload").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("creategroup")) {
            if (args.length >= 2) {
                String groupName = args[0];
                String prefix = args[1];
                int hierarchy = args.length > 2 ? Integer.parseInt(args[2]) : 0;

                boolean created = groupManager.addGroup(groupName, prefix, hierarchy);
                if (created) {
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

                Player targetPlayer = plugin.getServer().getPlayer(playerName);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    boolean added = groupManager.addUserToGroup(groupName, targetPlayer.getUniqueId());
                    if (added) {
                        sender.sendMessage(ChatColor.GREEN + "User added to group successfully!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "User is already in the group!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Player not found or offline!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("removefromgroup")) {
            if (args.length == 2) {
                String playerName = args[0];
                String groupName = args[1];

                Player targetPlayer = plugin.getServer().getPlayer(playerName);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    boolean removed = groupManager.removeUserFromGroup(groupName, targetPlayer.getUniqueId());
                    if (removed) {
                        sender.sendMessage(ChatColor.GREEN + "User removed from group successfully!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "User is not in the group!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Player not found or offline!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("tagreload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
            return true;
        }
        return false;
    }
}
