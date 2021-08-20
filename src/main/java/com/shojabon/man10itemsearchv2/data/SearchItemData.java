package com.shojabon.man10itemsearchv2.data;

import org.bukkit.Location;

public class SearchItemData {

    public String finalEditorName;
    public String finalEditorUUID;
    public String containerType;
    public int slot;
    public int amount;
    public String server;
    public Location location;
    public String dateTime ;
    public String world;
    public int x;
    public int y;
    public int z;

    public SearchItemData(String finalEditorName, String finalEditorUUID, String containerType, int slot, int amount, String server, Location location, String dateTime, String world, int x, int y, int z){
        this.finalEditorName = finalEditorName;
        this.finalEditorUUID = finalEditorUUID;
        this.containerType = containerType;
        this.slot = slot;
        this.server = server;
        this.location = location;
        this.dateTime = dateTime;
        this.amount = amount;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

}
