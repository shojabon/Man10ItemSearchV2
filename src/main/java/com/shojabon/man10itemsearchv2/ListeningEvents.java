package com.shojabon.man10itemsearchv2;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;

public class ListeningEvents implements @NotNull Listener {

    Man10ItemSearchV2 plugin;

    public ListeningEvents(Man10ItemSearchV2 plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        Block b = e.getBlock();
        if(!(b.getState() instanceof InventoryHolder)){
            return;
        }
        if(e.isCancelled()){
            return;
        }
        String inventoryType = ((InventoryHolder) b.getState()).getInventory().getType().name();
        plugin.api.cache.remove(plugin.api.generateContainerId(inventoryType, b.getLocation(), e.getPlayer().getName()));
        plugin.api.deleteRecordInLocation(b.getLocation(), inventoryType);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e){
        InventoryHolder inv = e.getInventory().getHolder();
        Inventory inventory;

        if(inv instanceof StorageMinecart || inv instanceof HopperMinecart) {
            return;
        }

        //if entity
        if(e.getInventory().getType() == InventoryType.CHEST){
            Location l = e.getInventory().getLocation();

            if(l == null){
                return;
            }
            Block b = l.getBlock();
            if(b.getType() != Material.CHEST){
                return;
            }
        }

        //update user inventory
        plugin.api.createLog(e.getPlayer().getInventory(), e.getInventory().getLocation(), e.getPlayer().getName(), e.getPlayer().getUniqueId(), "PLAYER");
        if(inv instanceof PlayerInventory || e.getInventory().getType() == InventoryType.CRAFTING){
            return;
        }
        //record log
        if(inv instanceof DoubleChest){
            Chest[] chests = plugin.api.splitDoubleChest(((DoubleChest) inv));
            for(Chest chest: chests){
                plugin.api.createLog(chest.getBlockInventory(), chest.getLocation(), e.getPlayer().getName(), e.getPlayer().getUniqueId(), e.getInventory().getType().name());
            }
            return;
        }else{
            inventory = e.getInventory();
        }
        plugin.api.createLog(inventory, e.getInventory().getLocation(), e.getPlayer().getName(), e.getPlayer().getUniqueId(), e.getInventory().getType().name());

    }

}