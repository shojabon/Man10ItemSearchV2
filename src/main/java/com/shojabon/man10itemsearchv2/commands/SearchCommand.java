package com.shojabon.man10itemsearchv2.commands;

import com.shojabon.man10itemsearchv2.Man10ItemSearchV2;
import com.shojabon.man10itemsearchv2.data.SearchContainerData;
import com.shojabon.man10itemsearchv2.data.SearchItemData;
import com.shojabon.man10itemsearchv2.data.UserItemCountData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.SInventory.SInventory;
import utils.SInventory.SInventoryItem;
import utils.SItemStack;
import utils.SStringBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SearchCommand implements @Nullable CommandExecutor {

    Man10ItemSearchV2 plugin;
    String prefix = "§6§l[§e§lMan10Search§d§lV2§6§l]§a§l";

    HashMap<UUID, ArrayList<SearchContainerData>> searchCache = new HashMap<>();
    HashMap<UUID, ArrayList<UserItemCountData>> statisticsCache = new HashMap<>();


    public void help(Player p){
        p.sendMessage("§d§l==========" + prefix + "§d§l==========");
        p.sendMessage("");
        TextComponent server = new TextComponent("<server>");
        server.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§f検索するサーバー\n" +
                "§f記入されない場合ALLとなる\n" +
                "§fALL: すべてのサーバー")));
        p.sendMessage(new ComponentBuilder().append("§e/msearch hand ").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§e手に持っているアイテムの検索をします"))).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msearch hand ")).append(server).create());
        p.sendMessage(new ComponentBuilder().append("§e/msearch statistics ").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§e手に持っているアイテムの所持数ランキングを表示します"))).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msearch statistics ")).append(server).create());
        p.sendMessage(new ComponentBuilder().append("§e/msearch hash").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§e手に持っているアイテムの識別ハッシュを表示します"))).event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msearch hash")).create());
        p.sendMessage("");
        p.sendMessage("§d§l=====================================");
        p.sendMessage("§eCreated By Sho0");
    }

    public SearchCommand(Man10ItemSearchV2 plugin){
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("prefix");
    }

    public BaseComponent[] createItemText(SearchContainerData container){
        //info
        SearchItemData data = container.getSearchItemData();
        TextComponent info = new TextComponent("[情報]");
        info.setBold(true);
        info.setColor(ChatColor.WHITE);
        info.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§f§l所有者:" + data.finalEditorName +
                "\n§fUUID: " + data.finalEditorUUID +
                "\n§7§l=========[保管情報]=========" +
                "\n§f§lコンテナ種類: " + data.containerType +
                "\n§f§lスロット番号: " + container.getSlotsString() +
                "\n§f§l個数: " + container.getTotalAmount() +
                "\n§7§l=========[位置情報]=========" +
                "\n§f§lサーバー: " + data.server +
                "\n§f§lワールド: " + data.world +
                "\n§f§lx: " + data.x +
                "\n§f§ly: " + data.y +
                "\n§f§lz: " + data.z +
                "\n§7§l=========================" +
                "\n§f§l最終更新日時: " + data.dateTime)));

        //preview
        TextComponent preview = new TextComponent("[プレビュー]");
        preview.setBold(true);
        if(plugin.server.equalsIgnoreCase(data.server)){
            preview.setColor(ChatColor.LIGHT_PURPLE);
            if(data.containerType.equalsIgnoreCase("PLAYER")){
                preview.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, plugin.openInvCommand.replace("<NAME>", data.finalEditorName)));
            }else if(data.containerType.equalsIgnoreCase("ENDER_CHEST")){
                preview.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, plugin.openEnderCommand.replace("<NAME>", data.finalEditorName)));
            }else{
                preview.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch preview " + data.world + " " + data.x + " " + data.y + " " + data.z));
            }
        }else{
            preview.setColor(ChatColor.GRAY);
            preview.setClickEvent(null);
        }


        //TP
        TextComponent tp = new TextComponent("[TP]");
        tp.setBold(true);
        if(plugin.server.equalsIgnoreCase(data.server) && !data.containerType.equalsIgnoreCase("PLAYER")){
            tp.setColor(ChatColor.YELLOW);
            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch teleport " + data.world + " " + data.x + " " + data.y + " " + data.z));
        }else{
            tp.setColor(ChatColor.GRAY);
            tp.setClickEvent(null);
        }

        return new ComponentBuilder().append(info).append(preview).event((HoverEvent) null).append(tp).append(" §a§l" + container.getTotalAmount() + "個 §b§l" + data.finalEditorName).event((ClickEvent) null).create();
    }

    public void displayItemText(Player p, ArrayList<SearchContainerData> data, Component itemName, int page){
        int colPerPage = 15;
        int starting = page*colPerPage;
        if(starting-1 > data.size()){
            p.sendMessage(prefix + "§c§lページが存在しません");
            return;
        }
        boolean hasLeft = page != 0;
        boolean hasRight = (page+1)*colPerPage <= data.size()-1;

        //scroller
        TextComponent left = new TextComponent("[前へ]");
        left.setBold(true);
        TextComponent right = new TextComponent("[次へ]");
        right.setBold(true);
        //left
        if(hasLeft){
            left.setColor(ChatColor.RED);
            left.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch search " + (page-1)));
        }else{
            left.setColor(ChatColor.GRAY);
        }
        //right
        if(hasRight){
            right.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch search " + (page+1)));
            right.setColor(ChatColor.GREEN);
        }else{
            right.setColor(ChatColor.GRAY);
        }
        //================================

        //page display
        TextComponent pageText = new TextComponent(" ・・・[" + (page+1) + "]・・・ ");
        pageText.setColor(ChatColor.WHITE);
        pageText.setBold(true);

        //calculate total
        int total = 0;
        for(SearchContainerData sd: data){
            total += sd.getTotalAmount();
        }
        for(int i = 0; i < 20; i++){
            p.sendMessage("");
        }
        p.sendMessage(itemName);
        p.sendMessage("§7§l検索総数: §b§l" + total + " §7§l検索件数: §b§l" + data.size());
        p.sendMessage("§d§l===============================");
        int target = starting + colPerPage;
        if(target > data.size()-1){
            target = data.size();
        }
        for(int i = 0; i < colPerPage-(target - starting); i++){
            p.sendMessage("");
        }
        for(int i = starting; i < target; i++){
            p.sendMessage(createItemText(data.get(i)));
        }
        p.sendMessage("");
        //display information
        p.sendMessage(new ComponentBuilder().append(left).append(pageText).event((ClickEvent) null).append(right).create());

    }

    public void displayStatistics(Player p, ArrayList<UserItemCountData> data, int page){
        int colPerPage = 15;
        int starting = page*colPerPage;
        if(starting-1 > data.size()){
            p.sendMessage(prefix + "§c§lページが存在しません");
            return;
        }
        boolean hasLeft = page != 0;
        boolean hasRight = (page+1)*colPerPage < data.size()-1;
        //scroller
        TextComponent left = new TextComponent("[前へ]");
        left.setBold(true);
        TextComponent right = new TextComponent("[次へ]");
        right.setBold(true);
        //left
        if(hasLeft){
            left.setColor(ChatColor.RED);
            left.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch statSearch " + (page-1)));
        }else{
            left.setColor(ChatColor.GRAY);
        }
        //right
        if(hasRight){
            right.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch statSearch " + (page+1)));
            right.setColor(ChatColor.GREEN);
        }else{
            right.setColor(ChatColor.GRAY);
        }
        //================================

        //page display
        TextComponent pageText = new TextComponent("・[" + (page+1) + "]・");
        pageText.setColor(ChatColor.WHITE);
        pageText.setBold(true);
        int target = starting + colPerPage;
        if(target > data.size()-1){
            target = data.size();
        }
        for(int i = 0; i < 20; i++){
            p.sendMessage("");
        }
        p.sendMessage("§d§l===[所持数ランキング]===");
        for(int i = 0; i < colPerPage-(target - starting); i++){
            p.sendMessage("");
        }
        for(int i = starting; i < target; i++){
            p.sendMessage("§b§l" + data.get(i).name + " §a§l" + data.get(i).count + "個");
        }
        p.sendMessage(new ComponentBuilder().append(left).append(pageText).event((ClickEvent) null).append(right).create());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(prefix + "§c§lこのコマンドはプレイヤーからのみ実行できます");
            return false;
        }
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();
        if(!p.hasPermission("man10.itemtracer")){
            p.sendMessage(prefix  +"§c§lあなたには権限がありません");
            return false;
        }
        if(args.length == 0){
            help(p);
            return false;
        }

        if(args[0].equalsIgnoreCase("hand")){
            if(p.getInventory().getItemInMainHand().getType() == Material.AIR){
                p.sendMessage(prefix + "§c§lアイテムを持っていません");
                return false;
            }
            if(args.length == 1 || args.length == 2){
                //set query to cache
                plugin.threadPool.execute(()->{
                    if(args.length == 1){
                        searchCache.put(uuid, plugin.api.getItems(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), null, "date_time"));
                    }else {
                        searchCache.put(uuid, plugin.api.getItems(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), args[1], "date_time"));
                    }
                    displayItemText(p, searchCache.get(uuid), p.getInventory().getItemInMainHand().displayName(), 0);
                });
                //display
            }else{
                //error in use case
                p.sendMessage(prefix + "§c§lコマンドの使用方法が間違ってます");
                return false;
            }
        }
        if(args[0].equalsIgnoreCase("search") && args.length == 2){
            if(!searchCache.containsKey(uuid)){
                p.sendMessage(prefix  +"§c§l検索結果がありません");
                return false;
            }
            try{
                displayItemText(p, searchCache.get(uuid), p.getInventory().getItemInMainHand().displayName(), Integer.parseInt(args[1]));
                return true;
            }catch (NumberFormatException e){
                p.sendMessage(prefix  +"§c§lページは数字でなくてはなりません");
                return false;
            }
        }
        if(args[0].equalsIgnoreCase("preview")){
            if(args.length != 5){
                p.sendMessage(prefix  +"§c§l引数が足りません");
                return false;
            }
            if(args.length == 5){
                //if block
                World w = plugin.getServer().getWorld(args[1]);
                if(w == null){
                    p.sendMessage(prefix  +"§c§lワールドが存在しません");
                    return false;
                }
                try{
                    Location l = new Location(w, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                    Block b = l.getBlock();
                    if(!(b.getState() instanceof InventoryHolder)){
                        p.sendMessage(prefix  +"§c§l記録上のブロックと種類が違います");
                        return false;
                    }
                    plugin.userInPreview.add(uuid);
                    Inventory original = ((InventoryHolder) b.getState()).getInventory();
                    int size = original.getSize();
                    if(size < 9){
                        size = 9;
                    }
                    Inventory clone = plugin.getServer().createInventory(null, size);
                    ItemStack[] items = original.getContents().clone();
                    if(items == null){
                        plugin.userInPreview.remove(uuid);
                        p.sendMessage(prefix  +"§c§lインベントリがエラーです");
                        return false;
                    }
                    clone.setContents(items);
                    p.openInventory(clone);
                    return true;
                }catch (NumberFormatException e){
                    plugin.userInPreview.remove(uuid);
                    p.sendMessage(prefix  +"§c§l座標位置は数字でなくてはなりません");
                    return false;
                }
            }
        }
        if(args[0].equalsIgnoreCase("teleport") && args.length == 5){
            try{
                World w = plugin.getServer().getWorld(args[1]);
                if(w == null){
                    p.sendMessage(prefix  +"§c§lワールドが存在しません");
                    return false;
                }
                Location l = new Location(w, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                p.teleport(l);
                p.sendMessage(prefix + "§a§lテレポート完了");
                return true;
            }catch (NumberFormatException e){
                p.sendMessage(prefix  +"§c§l座標位置は数字でなくてはなりません");
                return false;
            }
        }
        if(args[0].equalsIgnoreCase("statistics")){
            if(p.getInventory().getItemInMainHand().getType() == Material.AIR){
                p.sendMessage(prefix + "§c§lアイテムを持っていません");
                return false;
            }
            if(args.length == 1 || args.length == 2){
                //set query to cache
                if(args.length == 1){
                    statisticsCache.put(uuid, plugin.api.getItemCountRanking(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), null));
                }else {
                    statisticsCache.put(uuid, plugin.api.getItemCountRanking(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), args[1]));
                }
                displayStatistics(p, statisticsCache.get(uuid), 0);
                return true;
                //display
            }else{
                //error in use case
                p.sendMessage(prefix + "§c§lコマンドの使用方法が間違ってます");
                return false;
            }
        }
        if(args[0].equalsIgnoreCase("statSearch") && args.length == 2){
            if(!statisticsCache.containsKey(uuid)){
                p.sendMessage(prefix  +"§c§l検索結果がありません");
                return false;
            }
            try{
                displayStatistics(p, statisticsCache.get(uuid), Integer.parseInt(args[1]));
                return false;
            }catch (NumberFormatException e){
                p.sendMessage(prefix  +"§c§lページは数字でなくてはなりません");
                return false;
            }
        }
        if(args[0].equalsIgnoreCase("hash")){
            if(p.getInventory().getItemInMainHand().getType() == Material.AIR){
                p.sendMessage(prefix + "§c§lアイテムを持っていません");
                return false;
            }
            SItemStack item = new SItemStack(p.getInventory().getItemInMainHand());
            p.sendMessage("§d========================");
            p.sendMessage("§e完全ハッシュ:§b " + item.getMD5());
            p.sendMessage("§e種別ハッシュ:§b " + item.getItemTypeMD5());
            return true;
        }

        SInventory inv = new SInventory("test1", 3, plugin);
        SInventory inv2 = new SInventory("test2", 3, plugin);
        SInventory inv3 = new SInventory("test3", 3, plugin);




        SInventoryItem item = new SInventoryItem(new SItemStack(Material.BLUE_STAINED_GLASS).setDisplayName(new SStringBuilder().gold().text("BLOCK!").build()).build());
        item.clickable(false);
        item.setEvent(a -> {
            p.sendMessage("1 " + a.getSlot());
            inv.moveToMenu(p, inv2);

        });
        inv.fillItem( item);


        SInventoryItem item2 = new SInventoryItem(new SItemStack(Material.RED_STAINED_GLASS).setDisplayName(new SStringBuilder().red().text("BLOCK!").build()).build());
        item2.clickable(false);
        item2.setEvent(a -> {
            p.sendMessage("2 " + a.getSlot());
            inv2.moveToMenu(p, inv3);
        });
        inv2.setOnCloseEvent(e -> {
            inv2.moveToMenu(p, inv);
        });
        inv2.fillItem(item2);

        SInventoryItem item3 = new SInventoryItem(new SItemStack(Material.YELLOW_STAINED_GLASS).setDisplayName(new SStringBuilder().yellow().text("BLOCK!").build()).build());
        item3.clickable(false);
        item3.setEvent(a -> {
            p.sendMessage("3 " + a.getSlot());
        });
        inv3.setOnCloseEvent(e -> {
            inv3.moveToMenu(p, inv2);
        });
        inv3.fillItem( item3);


        inv.open(p);
        help(p);


        return false;
    }
}
