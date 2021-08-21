package utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by takatronix on 2017/03/05.
 */

public class MySQLAPI {
    Connection connection = null;
    Statement statement = null;

    Plugin plugin;
    public MySQLAPI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + plugin.getConfig().getString("mysql.host") + ":" + plugin.getConfig().getString("mysql.port") +"/" + plugin.getConfig().getString("mysql.db") + "?useSSL=false", plugin.getConfig().getString("mysql.user"), plugin.getConfig().getString("mysql.pass"));
        } catch (SQLException var2) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not connect to MySQL server, error code: " + var2.getErrorCode());
        } catch (ClassNotFoundException var3) {
            Bukkit.getLogger().log(Level.SEVERE, "JDBC driver was not found in this machine.");
        }
    }

    public boolean execute(String query) {
        open();
        if(this.connection == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return false;
        }
        boolean ret = true;
        try {
            this.statement = this.connection.createStatement();
            this.statement.execute(query);
        } catch (SQLException var3) {
            this.plugin.getLogger().info("Error executing statement: " +var3.getErrorCode() +":"+ var3.getLocalizedMessage());
            this.plugin.getLogger().info(query);
            ret = false;

        }

        this.close();
        return ret;
    }

    public ResultSet query(String query) {
        open();
        ResultSet rs = null;
        if(this.connection == null){
            Bukkit.getLogger().info("failed to open MYSQL");
            return rs;
        }
        try {
            this.statement = this.connection.createStatement();
            rs = this.statement.executeQuery(query);
        } catch (SQLException var4) {
            this.plugin.getLogger().info("Error executing query: " + var4.getErrorCode());
            this.plugin.getLogger().info(query);
        }
        return rs;
    }

    //build mysql query
    public static String buildInsertQuery(List<HashMap<String, Object>> objects, String table){
        if(objects.size() == 0){
            return "";
        }
        String query = "INSERT INTO " + table;
        StringBuilder fields = new StringBuilder("(");
        StringBuilder finalValues = new StringBuilder();
        boolean fieldsDone = false;

        for(HashMap<String, Object> object: objects){
            StringBuilder values = new StringBuilder("(");
            for(String key : object.keySet()){
                Object obj = object.get(key);
                if(obj instanceof Integer){
                    values.append(obj);
                    values.append(",");
                }else if (obj instanceof String){
                    values.append("\"").append(escapeString((String) obj)).append("\"");
                    values.append(",");
                }else{
                    continue;
                }
                if(!fieldsDone){
                    fields.append(key).append(",");
                }
            }
            fieldsDone = true;
            values.deleteCharAt(values.length()-1);
            values.append("),");
            finalValues.append(values);
        }
        fields.deleteCharAt(fields.length()-1);
        fields.append(")");

        finalValues.deleteCharAt(finalValues.length()-1);
        return query + " " + fields + " VALUES " + finalValues;
    }

    public static String buildInsertQuery(HashMap<String, Object> object, String table) {
        List<HashMap<String, Object>> lis = new ArrayList<>();
        lis.add(object);
        return buildInsertQuery(lis, table);
    }

    public static String escapeString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\b","\\b")
                .replace("\n","\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\\x1A", "\\Z")
                .replace("\\x00", "\\0")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }


    public void close(){

        try {
            if(statement != null){
                this.statement.close();
                this.statement = null;
            }
            if(connection != null){
                this.connection.close();
                this.connection = null;
            }
        } catch (SQLException var4) {
        }


    }
}