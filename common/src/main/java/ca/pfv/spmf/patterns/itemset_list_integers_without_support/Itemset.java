package main.java.ca.pfv.spmf.patterns.itemset_list_integers_without_support;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Itemset đại diện cho một tập hợp các item (sản phẩm) được mua cùng nhau.
 */
public class Itemset {
    private List<Integer> items;

    public Itemset() {
        items = new ArrayList<>();
    }

    public Itemset(Integer item) {
        items = new ArrayList<>();
        items.add(item);
    }

    public void addItem(Integer item) {
        items.add(item);
    }

    public List<Integer> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public Integer get(int index) {
        return items.get(index);
    }

    /**
     * Tạo bản sao của itemset.
     */
    public Itemset cloneItemSet() {
        Itemset copy = new Itemset();
        copy.items.addAll(this.items);
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Integer item : items) {
            sb.append(item).append(" ");
        }
        sb.append("-1");
        return sb.toString();
    }
}