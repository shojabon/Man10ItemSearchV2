package com.shojabon.man10itemsearchv2;

import com.shojabon.man10itemsearchv2.commands.SearchCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import utils.MySQL.MySQLAPI;
import utils.MySQL.MySQLCachedResultSet;
import utils.MySQL.MySQLQueue;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public final class Man10ItemSearchV2 extends JavaPlugin {

    public Man10ItemSearchV2API api;
    public String server = "null";
    public ArrayList<UUID> userInPreview = new ArrayList<>();

    public MySQLQueue mysqlQueue = null;

    public String openInvCommand;
    public String openEnderCommand;

    public ExecutorService threadPool;

    public String tableCreate = "CREATE TABLE IF NOT EXISTS `item_database` (\n" +
            "\t`id` INT(10) NOT NULL AUTO_INCREMENT,\n" +
            "\t`container_id` VARCHAR(256) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`final_editor_name` VARCHAR(32) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`final_editor_uuid` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`container_type` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`slot` INT(10) NULL DEFAULT NULL,\n" +
            "\t`server` VARCHAR(128) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`world` VARCHAR(128) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`x` INT(10) NULL DEFAULT NULL,\n" +
            "\t`y` INT(10) NULL DEFAULT NULL,\n" +
            "\t`z` INT(10) NULL DEFAULT NULL,\n" +
            "\t`full_item_hash` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`item_hash` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`amount` INT(10) NULL DEFAULT NULL,\n" +
            "\t`item_type` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`date_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
            "\tPRIMARY KEY (`id`) USING BTREE\n" +
            ")\n" +
            "COLLATE='utf8mb4_0900_ai_ci'\n" +
            "ENGINE=InnoDB\n" +
            ";\n";

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        api = new Man10ItemSearchV2API(this);

        mysqlQueue = new MySQLQueue(1, 1, this);


        mysqlQueue.execute(tableCreate);

        server = getConfig().getString("server");
        openInvCommand = getConfig().getString("openInvCommand");
        openEnderCommand = getConfig().getString("openEnderCommand");
        Objects.requireNonNull(getCommand("msearch")).setExecutor(new SearchCommand(this));
        getServer().getPluginManager().registerEvents(new ListeningEvents(this), this);
        threadPool = Executors.newCachedThreadPool();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(threadPool != null){
            threadPool.shutdown();
        }
        mysqlQueue.stop();
    }



}
