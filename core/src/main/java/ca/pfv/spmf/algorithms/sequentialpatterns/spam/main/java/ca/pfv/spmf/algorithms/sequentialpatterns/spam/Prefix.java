package main.java.ca.pfv.spmf.algorithms.sequentialpatterns.spam;

import java.util.ArrayList;
import java.util.List;
import main.java.ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;

/**
 * Represents a sequence (prefix) as a list of itemsets.
 */
class Prefix {
    
    final List<Itemset> itemsets = new ArrayList<>();
    
    public Prefix() { }
    
    /**
     * Adds an itemset to the sequence.
     * @param itemset the itemset to add.
     */
    public void addItemset(Itemset itemset) {
        itemsets.add(itemset);
    }
    
    /**
     * Creates a deep copy of the sequence.
     * @return cloned sequence.
     */
    public Prefix cloneSequence(){
        Prefix sequence = new Prefix();
        for(Itemset itemset : itemsets){
            sequence.addItemset(itemset.cloneItemSet());
        }
        return sequence;
    }
    
    @Override
    public String toString() {
        StringBuilder r = new StringBuilder();
        for(Itemset itemset : itemsets){
            for(Integer item : itemset.getItems()){
                r.append(item).append(" ");
            }
            r.append("-1 ");
        }
        return r.toString();
    }
    
    /**
     * Returns the list of itemsets.
     */
    public List<Itemset> getItemsets() {
        return itemsets;
    }
    
    /**
     * Returns the itemset at a given index.
     */
    public Itemset get(int index) {
        return itemsets.get(index);
    }
    
    /**
     * Returns the ith item of the sequence across all itemsets.
     */
    public Integer getIthItem(int i) { 
        for(int j = 0; j < itemsets.size(); j++){
            if(i < itemsets.get(j).size()){
                return itemsets.get(j).get(i);
            }
            i = i - itemsets.get(j).size();
        }
        return null;
    }
    
    /**
     * Returns the number of itemsets in the sequence.
     */
    public int size(){
        return itemsets.size();
    }
    
    /**
     * Returns the total count of items in the sequence.
     */
    public int getItemOccurencesTotalCount(){
        int count = 0;
        for(Itemset itemset : itemsets){
            count += itemset.size();
        }
        return count;
    }
    
    /**
     * Checks if the sequence contains a specific item.
     */
    public boolean containsItem(Integer item) {
        for(Itemset itemset : itemsets) {
            if(itemset.getItems().contains(item)) {
                return true;
            }
        }
        return false;
    }
}