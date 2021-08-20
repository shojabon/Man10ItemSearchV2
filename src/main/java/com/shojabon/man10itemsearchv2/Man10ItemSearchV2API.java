package com.shojabon.man10itemsearchv2;

import com.shojabon.man10itemsearchv2.data.SearchItemData;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import utils.SItemStack;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Man10ItemSearchV2API {

    Man10ItemSearchV2 plugin;
    HashMap<String, HashMap<Integer, String>> cache = new HashMap<>();

    public Man10ItemSearchV2API(Man10ItemSearchV2 plugin){
        this.plugin = plugin;
    }

    //util functions

    //split double chest object into 2 chest objects
    public Chest[] splitDoubleChest(DoubleChest dChest){
        return new Chest[]{((Chest)dChest.getLeftSide()), ((Chest)dChest.getRightSide())};
    }

    String generateContainerId(String type,Location l, String name){
        if(type.equals("CRAFTING") || type.equals("PLAYER")){
            return type + "|" + name;
        }
        return plugin.server + "|" + type +"|"+l.getWorld().getName() + "|"+l.getBlockX() + "|"+ l.getBlockY() + "|"+ l.getBlockZ();
    }

    //core functions

    public void createLog(Inventory inv, Location l, String name, UUID uuid, String containerType){
        HashMap<String, Object> payload = new HashMap<>();
        String containerId = generateContainerId(containerType, l, name);


        HashMap<Integer, String> localCache = getCache(containerId);


        //headers
        payload.put("container_id", containerId);
        payload.put("final_editor_name", name);
        payload.put("final_editor_uuid", uuid.toString());
        payload.put("container_type", containerType);
        payload.put("server", plugin.server);
        payload.put("world", l.getWorld().getName());
        payload.put("x", l.getBlockX());
        payload.put("y", l.getBlockY());
        payload.put("z", l.getBlockZ());

        ArrayList<HashMap<String, Object>> payloads = new ArrayList<>();
        ArrayList<Integer> deleteSlot = new ArrayList<>();
        for(int i = 0; i < inv.getSize(); i++){
            HashMap<String, Object> localPayload = new HashMap<>(payload);

            if(inv.getItem(i) == null){
                //check differences
                if(localCache.containsKey(i)){
                    deleteSlot.add(i);
                }
                continue;
            }
            SItemStack item = new SItemStack(inv.getItem(i));
            String md5 = item.getMD5();

            if(localCache.containsKey(i)){
                if(localCache.get(i).equals(md5)){
                    //same item
                    continue;
                }else{
                    //contains different item
                    deleteSlot.add(i);
                }
            }


            //add item payload
            localPayload.put("slot", i);
            localPayload.put("full_item_hash", md5);
            localPayload.put("item_hash", item.getItemTypeMD5());
            localPayload.put("item_type", item.getType().name());
            localPayload.put("item_name", ComponentSerializer.toString(item.getDisplayName()));
            localPayload.put("amount", item.getAmount());


            payloads.add(localPayload);
        }

        //update cache
        for(int slotToDelete: deleteSlot){
            cache.get(containerId).remove(slotToDelete);
        }
        for(HashMap<String, Object> obj: payloads){
            cache.get(containerId).put((Integer) obj.get("slot"), String.valueOf(obj.get("item_hash")));
        }
//        if(cache.get(containerId).size() == 0){
//            cache.remove(containerId);
//        }
        deleteRecords(containerId, deleteSlot);

        if(payloads.size() == 0){
            return;
        }
        plugin.mysql.execute(plugin.mysql.buildInsertQuery(payloads, "item_database"));

    }

    public ArrayList<SearchItemData> getItems(String typeHash, String server, String order){
        boolean needsAnd = false;
        StringBuilder query = new StringBuilder("SELECT * FROM item_database WHERE ");
        if(typeHash != null){
            query.append("item_hash = '").append(typeHash).append("'");
            needsAnd = true;
        }
        if(server != null && !server.equalsIgnoreCase("all")){
            if(needsAnd){
                query.append(" AND ");
            }
            query.append("server = '").append(server).append("'");
            needsAnd = true;
        }
        if(order != null){
            query.append("ORDER BY ").append(order).append(" DESC");
        }
        ArrayList<SearchItemData> result = new ArrayList<>();

        ResultSet rs = plugin.mysql.query(String.valueOf(query));
        try{
            while(rs.next()){
                SearchItemData data = new SearchItemData(rs.getString("final_editor_name"), rs.getString("final_editor_uuid"),
                        rs.getString("container_type"),
                        rs.getInt("slot"),
                        rs.getInt("amount"),
                        rs.getString("server"),
                        null,
                        rs.getString("date_time"),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"));
                if(plugin.server.equalsIgnoreCase(rs.getString("server"))){
                    World w = plugin.getServer().getWorld(rs.getString("world"));
                    if(w != null){
                        data.location = new Location(w, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                    }
                }
                result.add(data);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    //internal functions

    public boolean deleteRecords(String containerId , ArrayList<Integer> slots){
        if(slots.size() == 0){
            return true;
        }
        String query = "DELETE FROM item_database WHERE container_id = \"" + containerId + "\" AND slot IN ";
        StringBuilder ids = new StringBuilder("(");
        for(int slot: slots){
            ids.append(slot).append(",");
        }
        ids.deleteCharAt(ids.length()-1);
        ids.append(")");
        return plugin.mysql.execute(query + ids);
    }
    public boolean deleteRecordInLocation(Location l, String containerType){
        String query = "DELETE FROM item_database WHERE `container_type`='" + containerType + "' AND `world`='" + l.getWorld().getName() + "' AND `x` =" + l.getBlockX() + " AND `y` = " + l.getBlockY() + " AND `z` =" + l.getBlockZ() + ";";
        return plugin.mysql.execute(query);
    }

    public HashMap<Integer, String> getCache(String containerId){
        if(cache.containsKey(containerId)){
           return cache.get(containerId);
        }
        HashMap<Integer, String> container = new HashMap<>();
        try {
            ResultSet rs = plugin.mysql.query("SELECT slot,full_item_hash FROM item_database WHERE container_id = \"" + containerId + "\" LIMIT 200;");
            while(rs.next()){
                container.put(rs.getInt("slot"), rs.getString("full_item_hash"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cache.put(containerId, container);
        return container;
    }

}
