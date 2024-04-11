package dev.goldenn.bettergroups;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterGroups extends JavaPlugin {

    private GroupManager groupManager;
    private CommandManager commandManager;
    private String chatFormat;

    @Override
    public void onEnable() {
        // Initialize GroupManager
        this.groupManager = new GroupManager(this);

        // Initialize CommandManager with GroupManager instance
        this.commandManager = new CommandManager(this, groupManager);

        // Load chat format from config
        FileConfiguration config = getConfig();
        chatFormat = config.getString("chatFormat", "{TAG}&r {PLAYER}:&r {MESSAGE}");

        // Register events
        getServer().getPluginManager().registerEvents(groupManager, this);
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public String getChatFormat() {
        return chatFormat;
    }
}
