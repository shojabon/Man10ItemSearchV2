package com.shojabon.man10itemsearchv2;

import com.shojabon.man10itemsearchv2.commands.SearchCommand;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import utils.MySQLManager;
import utils.SItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

public final class Man10ItemSearchV2 extends JavaPlugin {

    public Man10ItemSearchV2API api;
    MySQLManager mysql;
    public String server = "null";

    @Override
    public void onEnable() {
        // Plugin startup logic
        api = new Man10ItemSearchV2API(this);
        mysql = new MySQLManager(this, "man10ItemTracer");
        server = getConfig().getString("server");
        Objects.requireNonNull(getCommand("msearch")).setExecutor(new SearchCommand(this));
        getServer().getPluginManager().registerEvents(new ListeningEvents(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
