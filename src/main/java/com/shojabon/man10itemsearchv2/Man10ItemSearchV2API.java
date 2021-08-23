package com.shojabon.man10itemsearchv2;

import com.google.common.cache.Cache;
import com.shojabon.man10itemsearchv2.data.SearchContainerData;
import com.shojabon.man10itemsearchv2.data.SearchItemData;
import com.shojabon.man10itemsearchv2.data.UserItemCountData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import utils.MySQL.MySQLAPI;
import utils.MySQL.MySQLCachedResultSet;
import utils.MySQL.MySQLQueue;
import utils.SItemStack;

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
        if(type.equals("CRAFTING") || type.equals("PLAYER") || type.equals("ENDER_CHEST")){
            return type + "|" + name;
        }
        if(l == null){
            return null;
        }
        return plugin.server + "|" + type +"|"+l.getWorld().getName() + "|"+l.getBlockX() + "|"+ l.getBlockY() + "|"+ l.getBlockZ();
    }

    //core functions

    public void createLog(Inventory inv, Location l, String name, UUID uuid, String containerType){
        plugin.threadPool.execute(()-> {
            HashMap<String, Object> payload = new HashMap<>();
            String containerId = generateContainerId(containerType, l, name);
            if(containerId == null){
                return;
            }


            HashMap<Integer, String> localCache = getCache(containerId);

            //headers
            payload.put("container_id", containerId);
            payload.put("final_editor_name", name);
            payload.put("final_editor_uuid", uuid.toString());
            payload.put("container_type", containerType);
            payload.put("server", plugin.server);
            if(l != null){
                payload.put("world", l.getWorld().getName());
                payload.put("x", l.getBlockX());
                payload.put("y", l.getBlockY());
                payload.put("z", l.getBlockZ());
            }else{
                payload.put("world", "");
                payload.put("x", 0);
                payload.put("y", 0);
                payload.put("z", 0);
            }

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
                //localPayload.put("item_name", ComponentSerializer.toString(item.getDisplayName()));
                localPayload.put("amount", item.getAmount());

//            if(item.getType() == Material.SHULKER_BOX){
//                BlockStateMeta bm = (BlockStateMeta) item.getItemStack().getItemMeta();
//                ShulkerBox sb = (ShulkerBox) bm.getBlockState();
//
//            }
                payloads.add(localPayload);
            }

            //update cache

            for(int slotToDelete: deleteSlot){
                cache.get(containerId).remove(slotToDelete);
            }
            for(HashMap<String, Object> obj: payloads){
                cache.get(containerId).put((Integer) obj.get("slot"), String.valueOf(obj.get("full_item_hash")));
            }

//        if(cache.get(containerId).size() == 0){
//            cache.remove(containerId);
//        }
            deleteRecords(containerId, deleteSlot);

            if(payloads.size() == 0){
                return;
            }
            plugin.mysqlQueue.execute(MySQLAPI.buildInsertQuery(payloads, "item_database"));
        });

    }

    public ArrayList<SearchContainerData> getItems(String typeHash, String server, String order){
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
            query.append("server = '").append(MySQLAPI.escapeString(server)).append("'");
            needsAnd = true;
        }
        if(order != null){
            query.append("ORDER BY ").append(order).append(" DESC");
        }
        ArrayList<SearchItemData> result = new ArrayList<>();
        ArrayList<MySQLCachedResultSet> results = plugin.mysqlQueue.query(String.valueOf(query));
        for(MySQLCachedResultSet rs: results){
            SearchItemData data = new SearchItemData(rs.getString("final_editor_name"), rs.getString("final_editor_uuid"),
                    rs.getString("container_type"),
                    rs.getString("container_id"),
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
                    data.location = new Location(w, rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
                }
            }
            result.add(data);
        }


        //create Container
        ArrayList<String> containerOrder = new ArrayList<>();
        HashMap<String, ArrayList<SearchItemData>> finalData = new HashMap<>();
        for(SearchItemData datum: result){
            if(!finalData.containsKey(datum.containerId)){
                finalData.put(datum.containerId, new ArrayList<>());
                containerOrder.add(datum.containerId);
            }
            finalData.get(datum.containerId).add(datum);
        }

        ArrayList<SearchContainerData> containerList = new ArrayList<>();
        for(String key: containerOrder){
            containerList.add(new SearchContainerData(finalData.get(key)));
        }

        return containerList;
    }

    public ArrayList<UserItemCountData> getItemCountRanking(String typeHash, String server){
        boolean needsAnd = false;
        StringBuilder query = new StringBuilder("SELECT final_editor_name,final_editor_uuid,SUM(amount) AS total FROM item_database WHERE ");
        if(typeHash != null){
            query.append("item_hash = '").append(typeHash).append("'");
            needsAnd = true;
        }
        if(server != null && !server.equalsIgnoreCase("all")){
            if(needsAnd){
                query.append(" AND ");
            }
            query.append("server = '").append(MySQLAPI.escapeString(server)).append("'");
            needsAnd = true;
        }
        query.append(" GROUP BY final_editor_uuid ORDER BY total DESC");

        ArrayList<UserItemCountData> result = new ArrayList<>();
        ArrayList<MySQLCachedResultSet> results = plugin.mysqlQueue.query(String.valueOf(query));
        for(MySQLCachedResultSet rs: results){
            result.add(new UserItemCountData(rs.getString("final_editor_name"), rs.getString("final_editor_uuid"), rs.getInt("total")));
        }
        return result;
    }

    //internal functions

    public void deleteRecords(String containerId , ArrayList<Integer> slots){
        if(slots.size() == 0){
            return;
        }
        String query = "DELETE FROM item_database WHERE container_id = \"" + containerId + "\" AND slot IN ";
        StringBuilder ids = new StringBuilder("(");
        for(int slot: slots){
            ids.append(slot).append(",");
        }
        ids.deleteCharAt(ids.length()-1);
        ids.append(")");
        plugin.mysqlQueue.execute(query + ids);
    }
    public void deleteRecordInLocation(Location l, String containerType){
        plugin.threadPool.execute(()->{
            String query = "DELETE FROM item_database WHERE `container_type`='" + containerType + "' AND `world`='" + l.getWorld().getName() + "' AND `x` =" + l.getBlockX() + " AND `y` = " + l.getBlockY() + " AND `z` =" + l.getBlockZ() + ";";
            plugin.mysqlQueue.execute(query);
        });
    }

    public HashMap<Integer, String> getCache(String containerId){
        if(cache.containsKey(containerId)){
           return cache.get(containerId);
        }
        HashMap<Integer, String> container = new HashMap<>();
        ArrayList<MySQLCachedResultSet> results = plugin.mysqlQueue.query("SELECT slot,full_item_hash FROM item_database WHERE container_id = \"" + containerId + "\" LIMIT 200;");
        for(MySQLCachedResultSet rs: results){
            container.put(rs.getInt("slot"), rs.getString("full_item_hash"));
        }
        cache.put(containerId, container);
        return container;
    }

}
