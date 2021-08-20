package com.shojabon.man10itemsearchv2.commands;

import com.shojabon.man10itemsearchv2.Man10ItemSearchV2;
import com.shojabon.man10itemsearchv2.data.SearchItemData;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.SItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SearchCommand implements @Nullable CommandExecutor {

    Man10ItemSearchV2 plugin;
    String prefix = "§6§l[§e§lMan10Search§d§lV2§6§l]§a§l";

    HashMap<UUID, ArrayList<SearchItemData>> searchCache = new HashMap<>();

    public SearchCommand(Man10ItemSearchV2 plugin){
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("prefix");
    }

    public BaseComponent[] createItemText(SearchItemData data){
        //info
        TextComponent info = new TextComponent("[情報]");
        info.setBold(true);
        info.setColor(ChatColor.WHITE);
        info.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§f§l所有者:" + data.finalEditorName +
                "\n§7§l=========[保管情報]=========" +
                "\n§f§lコンテナ種類: " + data.containerType +
                "\n§f§lスロット番号: " + data.slot +
                "\n§f§l個数: " + data.amount +
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
                preview.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch preview " + data.finalEditorUUID));
            }else{
                preview.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch preview " + data.world + " " + data.x + " " + data.y + " " + data.z));
            }
        }else{
            preview.setColor(ChatColor.GRAY);
        }


        //TP
        TextComponent tp = new TextComponent("[TP]");
        tp.setBold(true);
        if(plugin.server.equalsIgnoreCase(data.server) && !data.containerType.equalsIgnoreCase("PLAYER")){
            tp.setColor(ChatColor.YELLOW);
            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msearch teleport " + data.world + " " + data.x + " " + data.y + " " + data.z));
        }else{
            tp.setColor(ChatColor.GRAY);
        }

        return new ComponentBuilder().append(info).append(preview).event((HoverEvent) null).append(tp).append(" §6§l-§a§l" + data.amount + "個 §b§l" + data.finalEditorName).create();
    }

    public void help(Player p){

    }

    public void displayItemText(Player p, ArrayList<SearchItemData> data, Component itemName, int page){
        int colPerPage = 10;
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
        for(SearchItemData sd: data){
            total += sd.amount;
        }

        p.sendMessage(itemName);
        p.sendMessage("§7§l検索総数:" + total + " §7§l検索件数:" + data.size());
        p.sendMessage("§d§l===============================");
        int target = starting + 10;
        if(target > data.size()-1){
            target = data.size();
        }
        for(int i = starting; i < target; i++){
            p.sendMessage(createItemText(data.get(i)));
        }
        //display information
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
        if(!p.hasPermission("man10.moneytracer.search")){
            p.sendMessage(prefix  +"§c§lあなたには権限がありません");
            return false;
        }

        if(args[0].equalsIgnoreCase("hand")){
            if(p.getInventory().getItemInMainHand().getType() == Material.AIR){
                p.sendMessage(prefix + "§c§lアイテムを持っていません");
                return false;
            }
            if(args.length == 1 || args.length == 2 || args.length == 3){
                //set query to cache
                if(args.length == 1){
                    searchCache.put(uuid, plugin.api.getItems(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), null, "date_time"));
                }else if(args.length == 2){
                    searchCache.put(uuid, plugin.api.getItems(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), args[1], "date_time"));
                }else {
                    searchCache.put(uuid, plugin.api.getItems(new SItemStack(p.getInventory().getItemInMainHand()).getItemTypeMD5(), args[1], args[2]));
                }
                displayItemText(p, searchCache.get(uuid), p.getInventory().getItemInMainHand().displayName(), 0);
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
            }catch (NumberFormatException e){
                p.sendMessage(prefix  +"§c§lページは数字でなくてはなりません");
                return false;
            }
        }
        if(args[0].equalsIgnoreCase("preview")){
            if(args.length != 5 && args.length != 2){
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
                    p.openInventory(((InventoryHolder) b.getState()).getInventory());
                }catch (NumberFormatException e){
                    p.sendMessage(prefix  +"§c§l座標位置は数字でなくてはなりません");
                    return false;
                }
            }
            if(args.length == 2){
                //if player
                try{
                    Player target = plugin.getServer().getPlayer(UUID.fromString(args[1]));
                    if(target == null){
                        p.sendMessage(prefix  +"§c§lプレイヤーが存在しません");
                        return false;
                    }
                    p.openInventory(target.getInventory());
                }catch (Exception e){
                    p.sendMessage(prefix  +"§c§lUUID parse エラー");
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
            }catch (NumberFormatException e){
                p.sendMessage(prefix  +"§c§l座標位置は数字でなくてはなりません");
                return false;
            }
        }

        return false;
    }
}
