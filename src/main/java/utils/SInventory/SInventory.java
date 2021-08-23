package utils.SInventory;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class SInventory implements Listener {


    HashMap<String, Consumer<Object>> events = new HashMap<>();
    HashMap<Integer, SInventoryItem> items = new HashMap<>();
    Consumer<InventoryCloseEvent> onCloseEvent = null;

    //inventory status
    int rows;
    JavaPlugin plugin;
    String title;

    ArrayList<UUID> playerInMenu = new ArrayList<>();
    public ArrayList<UUID> movingPlayer = new ArrayList<>();

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

    public SInventory fillItem(SInventoryItem data){
        for(int i = 0; i < rows*9; i++){
            items.put(i, data);
        }
        return this;
    }

    public SInventory fillItem(ItemStack data){
        for(int i = 0; i < rows*9; i++){
            items.put(i, new SInventoryItem(data));
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
        for(int key: items.keySet()){
            inv.setItem(key, items.get(key).getItemStack());
        }
        p.openInventory(inv);

        if(plugin != null){
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            playerInMenu.add(p.getUniqueId());
        }
    }

    public Inventory buildInventory(){
        Inventory inv = plugin.getServer().createInventory(null, this.rows*9, title);
        for(int key: items.keySet()){
            inv.setItem(key, items.get(key).getItemStack());
        }
        return inv;
    }

    public void moveToMenu(Player p, SInventory inv){
        movingPlayer.add(p.getUniqueId());
        inv.open(p);
        movingPlayer.remove(p.getUniqueId());
    }


    //======== event =====

    public void setOnCloseEvent(Consumer<InventoryCloseEvent> event){
        this.onCloseEvent = event;
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

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(!playerInMenu.contains(e.getWhoClicked().getUniqueId())) return;
        if(!items.containsKey(e.getRawSlot())){
            return;
        }
        SInventoryItem item = items.get(e.getRawSlot());
        item.activateClick(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if(!playerInMenu.contains(e.getPlayer().getUniqueId())) return;
        playerInMenu.remove(e.getPlayer().getUniqueId());
        HandlerList.unregisterAll(this);
        if(onCloseEvent != null){
            if(!movingPlayer.contains(e.getPlayer().getUniqueId())){
                plugin.getServer().getScheduler().runTaskLater(plugin,()->{onCloseEvent.accept(e);}, 1);
            }else{
                movingPlayer.remove(e.getPlayer().getUniqueId());
            }
        }
    }

}