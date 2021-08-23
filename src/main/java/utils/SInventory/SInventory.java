package utils.SInventory;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.function.Consumer;

public class SInventory {


    HashMap<String, Consumer<Object>> events = new HashMap<>();
    HashMap<Integer, SInventoryItem> items = new HashMap<>();
    //inventory status
    int rows;
    JavaPlugin plugin;
    String title = null;

    public SInventory(String title, int inventoryRows, JavaPlugin plugin){
        this.title = title;
        this.rows = inventoryRows;
        this.plugin = plugin;
    }

    public SInventory(String title, int inventoryRows){
        this.title = title;
        this.rows = inventoryRows;
    }

    //set items

    public SInventory setItem(int slot, SInventoryItem data){
        items.put(slot, data);
        return this;
    }

    public SInventory setItem(int[] slots, SInventoryItem data){
        for(int slot: slots){
            items.put(slot, data);
        }
        return this;
    }

    public SInventory setItem(int slot, ItemStack data){
        items.put(slot, new SInventoryItem(data));
        return this;
    }

    public SInventory setItem(int[] slots, ItemStack data){
        for(int slot: slots){
            items.put(slot, new SInventoryItem(data));
        }
        return this;
    }

    //======================

    public SInventory setTitle(String title){
        this.title = title;
        return this;
    }

    public void open(Player p){
        Inventory inv = plugin.getServer().createInventory(null, this.rows*9, title);
        p.openInventory(inv);
    }

    public void setEvent(String eventName, Consumer<Object> event){
        events.put(eventName, event);
    }

    public void activateEvent(String eventName, Object data){
        if(!events.containsKey(eventName)){
            return;
        }
        events.get(eventName).accept(data);

    }
}
