package utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SItemStack {

    private ItemStack item = null;

    public SItemStack(ItemStack item){
        this.item = item;
    }

    public ItemStack build(){
        return this.item;
    }

    //identification methods

    public String getBase64(ItemStack item){
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(1);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public String getBase64(){
        return getBase64(this.item);
    }

    public String getMD5(ItemStack item){
        try {
            byte[] result =
                    MessageDigest.getInstance("MD5")
                            .digest(this.getBase64(item)
                                    .getBytes(StandardCharsets.UTF_8))
            ;
            return String.format("%020x", new BigInteger(1, result));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getMD5(){
        return this.getMD5(this.item);
    }

    //ItemStack identification functions

    public ItemStack getTypeItem(){
        ItemStack clone = this.item.clone();

        //set durability to 0
        if(clone.hasItemMeta()){
            ItemMeta itemMeta = clone.getItemMeta();
            ((Damageable) itemMeta).setDamage(0);
            clone.setItemMeta(itemMeta);
        }
        clone.setAmount(1);

        return clone;
    }

    public String getItemTypeBase64(){
        return getBase64(this.getTypeItem());
    }

    public String getItemTypeMD5(){
        return this.getMD5(this.getTypeItem());
    }

    //utils

    public Material getType(){
        return this.item.getType();
    }

    //==================== display name =======================

    public String getDisplayName(){
        if(!this.item.hasItemMeta()){
            return null;
        }
        if(!this.item.getItemMeta().hasDisplayName()){
            return null;
        }

        return this.item.getItemMeta().getDisplayName();
    }

    public SItemStack setDisplayName(String name){
        ItemMeta itemMeta = this.item.getItemMeta();
        itemMeta.setDisplayName(name);
        this.item.setItemMeta(itemMeta);
        return this;
    }

    public SItemStack addDisplayName(String name){
        String currentName = getDisplayName();
        return setDisplayName(currentName + name);
    }

    //========================================================

    public SItemStack setLore(List<String> lore){
        ItemMeta itemMeta = this.item.getItemMeta();
        itemMeta.setLore(lore);
        this.item.setItemMeta(itemMeta);
        return this;
    }

    public SItemStack addLore(String lore){
        List<String> lores = getLore();
        lores.add(lore);
        return setLore(lores);
    }

    public List<String> getLore(){
        ItemMeta itemMeta = this.item.getItemMeta();
        return itemMeta.getLore();
    }

    public SItemStack setAmount(int amount){
        this.item.setAmount(amount);
        return this;
    }

    public int getDamage(){
        ItemMeta itemMeta = this.item.getItemMeta();
        return ((Damageable) itemMeta).getDamage();
    }

    public SItemStack setDamage(int damage){
        ItemMeta itemMeta = this.item.getItemMeta();
        ((Damageable) itemMeta).setDamage(damage);
        this.item.setItemMeta(itemMeta);
        return this;
    }

    public int getAmount(){
        return this.item.getAmount();
    }
}
