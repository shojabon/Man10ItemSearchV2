package com.shojabon.man10itemsearchv2;

import com.shojabon.man10itemsearchv2.commands.SearchCommand;
import com.shojabon.mcutils.Utils.MySQL.ThreadedMySQLAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Man10ItemSearchV2 extends JavaPlugin {

    public Man10ItemSearchV2API api;
    public String server = "null";
    public ArrayList<UUID> userInPreview = new ArrayList<>();

    public ThreadedMySQLAPI mysql = null;

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
            "\t`custom_model_data` INT(10) NULL DEFAULT NULL,\n" +
            "\t`date_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
            "\tPRIMARY KEY (`id`) USING BTREE\n" +
            ")\n" +
            "COLLATE='utf8mb4_0900_ai_ci'\n" +
            "ENGINE=InnoDB\n" +
            ";\n";

    public String tableCreate2 = "CREATE TABLE IF NOT EXISTS`item_log` (\n" +
            "\t`id` INT(10) NOT NULL AUTO_INCREMENT,\n" +
            "\t`final_editor_name` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`final_editor_uuid` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`item_hash` VARCHAR(64) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
            "\t`item_count` INT(10) NULL DEFAULT NULL,\n" +
            "\t`custom_model_data` INT(10) NULL DEFAULT NULL,\n" +
            "\t`date_time` DATETIME NULL DEFAULT NULL,\n" +
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

        mysql = new ThreadedMySQLAPI(this);

        mysql.execute(tableCreate);
        mysql.execute(tableCreate2);
        //loggin
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            api.logWholeServerItemCount();
        }, getConfig().getInt("loggingInterval"), getConfig().getInt("loggingInterval"));

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

        //unexpected shutdown
        long lastLog = getConfig().getLong("lastLog");
        if(System.currentTimeMillis()/1000L - lastLog >= 30*60){
            api.logWholeServerItemCount();
        }

        if(threadPool != null){
            threadPool.shutdown();
        }
    }



}
