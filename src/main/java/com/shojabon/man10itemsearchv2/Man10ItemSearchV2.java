package com.shojabon.man10itemsearchv2;

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

public final class Man10ItemSearchV2 extends JavaPlugin {

    Man10ItemSearchV2API api;
    MySQLManager mysql;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new ListeningEvents(this), this);
        api = new Man10ItemSearchV2API(this);
        mysql = new MySQLManager(this, "man10ItemTracer");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player p = (Player) sender;
        return false;
    }
}
