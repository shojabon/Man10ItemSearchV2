package com.shojabon.man10itemsearchv2.data;

import java.util.ArrayList;
import java.util.List;

public class SearchContainerData {

    ArrayList<SearchItemData> data;

    public SearchContainerData(ArrayList<SearchItemData> data){
        this.data = data;
    }

    public ArrayList<Integer> getSlots(){
        ArrayList<Integer> slots = new ArrayList<>();
        for(SearchItemData datum: data){
            slots.add(datum.slot);
        }
        return slots;
    }

    public int getTotalAmount(){
        int total = 0;
        for(SearchItemData datum: data){
            total += datum.amount;
        }
        return total;
    }

    public SearchItemData getSearchItemData(){
        if(data.size() == 0){
            return null;
        }
        return data.get(0);
    }

    public String getSlotsString(){
        StringBuilder result = new StringBuilder();
        int i = 0;
        for(int slot: this.getSlots()){
            result.append(slot).append(" ");
            if(i >= 7){
                result.append("... ");
                break;
            }
            i++;
        }
        result.deleteCharAt(result.length()-1);
        return result.toString();
    }
}
