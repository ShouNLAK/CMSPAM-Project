package main.java.ca.pfv.spmf.algorithms.sequentialpatterns.spam;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a bitmap used in the SPAM algorithm.
 * It uses Java's BitSet to record the presence of items in sequences.
 */
public class Bitmap {

    public static long INTERSECTION_COUNT = 0;
    
    // Internal BitSet used to record the bitmap.
    BitSet bitmap = new BitSet();
    
    // Variables to help compute support.
    int lastSID = -1;  
    int firstItemsetID = -1;
    private int support = 0;
    int sidsum = 0;
    
    private int supportWithoutGapTotal = 0;
    
    /**
     * Constructor.
     * @param lastBitIndex desired size of the BitSet minus 1.
     */
    public Bitmap(int lastBitIndex){
        this.bitmap = new BitSet(lastBitIndex + 1);
    }
    
    private Bitmap(BitSet bitmap){
        this.bitmap = bitmap;
    }
    
    /**
     * Registers a bit corresponding to a sequence id (sid) and transaction id (tid).
     */
    public void registerBit(int sid, int tid, List<Integer> sequencesSize) {
        int pos = sequencesSize.get(sid) + tid;
        bitmap.set(pos, true);
        if(sid != lastSID){
            support++;
            sidsum += sid;
        }
        if(firstItemsetID == -1 || tid < firstItemsetID){
            firstItemsetID = tid;
        }
        lastSID = sid;
    }
    
    /**
     * Converts a bit position to a sequence id.
     */
    private int bitToSID(int bit, List<Integer> sequencesSize) {
        int result = Collections.binarySearch(sequencesSize, bit);
        if(result >= 0){
            return result;
        }
        return -result - 2;
    }
    
    public int getSupport() {
        return support;
    }
    
    /**
     * Creates a new bitmap for the S-step.
     */
    Bitmap createNewBitmapSStep(Bitmap bitmapItem, List<Integer> sequencesSize, int lastBitIndex, int maxGap) {
        Bitmap newBitmap = new Bitmap(new BitSet(lastBitIndex));
        if(maxGap == Integer.MAX_VALUE) {
            for (int bitK = bitmap.nextSetBit(0); bitK >= 0; bitK = bitmap.nextSetBit(bitK + 1)) {
                int sid = bitToSID(bitK, sequencesSize);
                int lastBitOfSID = lastBitOfSID(sid, sequencesSize, lastBitIndex);
                boolean match = false;
                for (int bit = bitmapItem.bitmap.nextSetBit(bitK + 1); 
                     bit >= 0 && bit <= lastBitOfSID; 
                     bit = bitmapItem.bitmap.nextSetBit(bit + 1)) {
                    newBitmap.bitmap.set(bit);
                    match = true;
                    int tid = bit - sequencesSize.get(sid);					
                    if(firstItemsetID == -1 || tid < firstItemsetID){
                        firstItemsetID = tid;
                    }
                }
                if(match){
                    if(sid != newBitmap.lastSID){
                        newBitmap.support++;
                        newBitmap.supportWithoutGapTotal++;
                        newBitmap.sidsum += sid;
                        newBitmap.lastSID = sid;
                    }
                }
                bitK = lastBitOfSID;
            }
        } else {
            int previousSid = -1;
            for (int bitK = bitmap.nextSetBit(0); bitK >= 0; bitK = bitmap.nextSetBit(bitK + 1)) {
                int sid = bitToSID(bitK, sequencesSize);
                int lastBitOfSID = lastBitOfSID(sid, sequencesSize, lastBitIndex);
                boolean match = false;
                boolean matchWithoutGap = false;
                for (int bit = bitmapItem.bitmap.nextSetBit(bitK + 1); 
                     bit >= 0 && bit <= lastBitOfSID; 
                     bit = bitmapItem.bitmap.nextSetBit(bit + 1)) {
                    matchWithoutGap = true;
                    if (bit - bitK > maxGap) {
                        break;
                    }
                    newBitmap.bitmap.set(bit);
                    match = true;
                    int tid = bit - sequencesSize.get(sid);
                    if(firstItemsetID == -1 || tid < firstItemsetID){
                        firstItemsetID = tid;
                    }
                }
                if(matchWithoutGap && previousSid != sid) {
                    newBitmap.supportWithoutGapTotal++;
                    previousSid = sid;
                }
                if(match){
                    if(sid != newBitmap.lastSID){
                        newBitmap.sidsum += sid;
                        newBitmap.support++;
                    }
                    newBitmap.lastSID = sid;
                }
            }
        }
        return newBitmap;
    }
    
    public int getSupportWithoutGapTotal() {
        return supportWithoutGapTotal;
    }
    
    private int lastBitOfSID(int sid, List<Integer> sequencesSize, int lastBitIndex) {
        if(sid + 1 >= sequencesSize.size()){
            return lastBitIndex;
        } else {
            return sequencesSize.get(sid + 1) - 1;
        }
    }
    
    /**
     * Creates a new bitmap for the I-step.
     */
    Bitmap createNewBitmapIStep(Bitmap bitmapItem, List<Integer> sequencesSize, int lastBitIndex) {
        BitSet newBitset = new BitSet(lastBitIndex);
        Bitmap newBitmap = new Bitmap(newBitset);
        for (int bit = bitmap.nextSetBit(0); bit >= 0; bit = bitmap.nextSetBit(bit + 1)) {
            if(bitmapItem.bitmap.get(bit)){
                newBitmap.bitmap.set(bit);
                int sid = bitToSID(bit, sequencesSize);
                if(sid != newBitmap.lastSID){
                    newBitmap.sidsum += sid;
                    newBitmap.support++;
                }
                newBitmap.lastSID = sid;
                int tid = bit - sequencesSize.get(sid);
                if(firstItemsetID == -1 || tid < firstItemsetID){
                    firstItemsetID = tid;
                }
            }
        }
        return newBitmap;
    }
    
    /**
     * Returns a string of sequence IDs where bits are set.
     */
    public String getSIDs(List<Integer> sequencesSize) {
        StringBuilder builder = new StringBuilder();
        int lastSidSeen = -1;
        for (int bitK = bitmap.nextSetBit(0); bitK >= 0; bitK = bitmap.nextSetBit(bitK + 1)) {
            int sid = bitToSID(bitK, sequencesSize);
            if(sid != lastSidSeen) {
                if(lastSidSeen != -1){
                    builder.append(" ");
                }
                builder.append(sid);
                lastSidSeen = sid;
            }
        }
        return builder.toString();
    }
}