package main.java;

import java.io.*;
import java.util.*;

public class SalesTransactionUtil {

    public static void processTransactions(
        String inputFile,
        String outputFile,
        String productDetailsCsv,
        int transactionIdIdx,
        int productIdIdx,
        int customerIdIdx
    ) throws IOException {
        // Đọc ánh xạ ProductID -> STT
        Map<String, Integer> productIdToStt = loadProductIdToStt(productDetailsCsv);

        Map<String, SortedSet<String>> currentTransactions = new LinkedHashMap<>();
        String lastCustomerID = null;

        File file = new File(inputFile);
        if (!file.exists()) {
            throw new FileNotFoundException("Input file not found: " + inputFile);
        }

        try (
            BufferedReader br = new BufferedReader(new FileReader(file));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))
        ) {
            String header = br.readLine(); // skip header
            if (header == null) throw new IOException("File không có dữ liệu.");
            String[] headerParts = header.split(",");
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = splitCsvLine(line);
                if (parts.length <= Math.max(Math.max(productIdIdx, customerIdIdx), transactionIdIdx)) continue;
                String transactionID = transactionIdIdx >= 0 ? parts[transactionIdIdx].trim() : "";
                String productID = parts[productIdIdx].trim();
                String customerID = parts[customerIdIdx].trim();

                if (customerID.isEmpty() || productID.isEmpty()) continue;
                if (transactionID.startsWith("C")) continue; // skip credit notes

                // Bỏ qua nếu productID không nằm trong Product_Details
                if (!productIdToStt.containsKey(productID)) continue;

                if (lastCustomerID == null || !customerID.equals(lastCustomerID)) {
                    if (!currentTransactions.isEmpty()) {
                        writeSession(currentTransactions, bw, productIdToStt);
                        currentTransactions.clear();
                    }
                    lastCustomerID = customerID;
                }
                String transKey = transactionID.isEmpty() ? UUID.randomUUID().toString() : transactionID;
                currentTransactions
                    .computeIfAbsent(transKey, k -> new TreeSet<>())
                    .add(productID);
            }
            if (!currentTransactions.isEmpty()) {
                writeSession(currentTransactions, bw, productIdToStt);
            }
        }
    }

    // Ghi ra file: mỗi TransactionID là một itemset (STT tăng dần, kết thúc -1), hết phiên thì -2
    private static void writeSession(Map<String, SortedSet<String>> transactions, BufferedWriter bw, Map<String, Integer> productIdToStt) throws IOException {
        for (SortedSet<String> products : transactions.values()) {
            List<Integer> sttList = new ArrayList<>();
            for (String pid : products) {
                Integer stt = productIdToStt.get(pid);
                if (stt != null) sttList.add(stt);
            }
            Collections.sort(sttList);
            for (Integer stt : sttList) {
                bw.write(stt + " ");
            }
            bw.write("-1 ");
        }
        bw.write("-2\n");
    }

    // Đọc Product_Details.csv để ánh xạ ProductID -> STT
    private static Map<String, Integer> loadProductIdToStt(String productDetailsCsv) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(productDetailsCsv))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;
                int stt;
                try {
                    stt = Integer.parseInt(parts[0].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                String productId = parts[1].trim();
                map.put(productId, stt);
            }
        }
        return map;
    }

    /**
     * Helper để tách dòng CSV, xử lý dấu phẩy trong dấu nháy kép.
     */
    private static String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}

