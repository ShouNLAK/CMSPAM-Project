package main.java;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoPickFixed {

    // Configuration
    private static final int SAMPLE_SIZE = 100;
    private static final double DESCRIPTION_MIN_AVG_LENGTH = 15.0;
    private static final double TIME_COLUMN_THRESHOLD = 0.3; // 30% giá trị có định dạng thời gian thì loại bỏ

    // Column names
    private static final String INVOICE_NO = "Mã giao dịch (InvoiceNo)";
    private static final String STOCK_CODE = "Mã sản phẩm (StockCode)";
    private static final String DESCRIPTION = "Tên sản phẩm (Description)";
    private static final String CUSTOMER_ID = "Mã khách hàng (CustomerID)";
    private static final String CUSTOMER_NAME = "Tên khách hàng (CustomerName)";

    // Regex patterns for time formats
    private static final Pattern[] TIME_PATTERNS = {
        Pattern.compile("^\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}$"), // dd/mm/yyyy, dd-mm-yyyy
        Pattern.compile("^\\d{2,4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}$"), // yyyy/mm/dd, yyyy-mm-dd
        Pattern.compile("^\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}\\s+\\d{1,2}:\\d{2}$"), // dd/mm/yyyy hh:mm
        Pattern.compile("^\\d{2,4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}\\s+\\d{1,2}:\\d{2}$"), // yyyy/mm/dd hh:mm
        Pattern.compile("^\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}\\s+\\d{1,2}:\\d{2}:\\d{2}$"), // dd/mm/yyyy hh:mm:ss
        Pattern.compile("^\\d{2,4}[/\\-]\\d{1,2}[/\\-]\\d{1,2}\\s+\\d{1,2}:\\d{2}:\\d{2}$"), // yyyy/mm/dd hh:mm:ss
        Pattern.compile("^\\d{1,2}:\\d{2}$"), // hh:mm
        Pattern.compile("^\\d{1,2}:\\d{2}:\\d{2}$"), // hh:mm:ss
        Pattern.compile("^\\d{2,4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"), // ISO format
    };

    // Add the file path for your CSV file here
    // private static final String FILE_PATH = "data.csv"; // Change "data.csv" to your actual file path

    public static void main(String[] args) {
        // Đọc file input từ đối số truyền vào, nếu không có thì dùng mặc định
        String FILE_PATH = "data.csv";
        if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            FILE_PATH = args[0].trim();
        }
        try {
            List<String[]> dataSample = readCsvSample(FILE_PATH, SAMPLE_SIZE);
            if (dataSample.isEmpty()) {
                System.out.println("Không thể đọc dữ liệu hoặc file trống");
                return;
            }

            // Skip header row if present
            if (!dataSample.isEmpty()) {
                dataSample.remove(0);
            }

            List<List<String>> columns = transposeData(dataSample);

            Map<String, Integer> identifiedColumns = identifyColumnsNewLogic(columns, dataSample);

            // Loại bỏ sớm các cột đã chọn trước khi tìm cột khách hàng ưu tiên
            int transactionIdIndex = identifiedColumns.getOrDefault(INVOICE_NO, -1);
            int productIdIndex = identifiedColumns.getOrDefault(STOCK_CODE, -1);
            int productNameIndex = identifiedColumns.getOrDefault(DESCRIPTION, -1);
            int customerIdIndex = identifiedColumns.getOrDefault(CUSTOMER_ID, -1);
            int customerNameIndex = identifiedColumns.getOrDefault(CUSTOMER_NAME, -1);

            Set<Integer> usedIndices = new HashSet<>();
            if (transactionIdIndex != -1) usedIndices.add(transactionIdIndex);
            if (productIdIndex != -1) usedIndices.add(productIdIndex);
            if (productNameIndex != -1) usedIndices.add(productNameIndex);
            if (customerIdIndex != -1) usedIndices.add(customerIdIndex);
            if (customerNameIndex != -1) usedIndices.add(customerNameIndex);

            Map<String, Integer> customerCols = new HashMap<>();
            if (transactionIdIndex != -1) {
                customerCols = identifyCustomerColumnsExclude(columns, dataSample, transactionIdIndex, usedIndices);
            }

            printResults(identifiedColumns, columns.size(), customerCols);

            // --- Thêm xuất ra Index.txt ---
            int customerPhoneIdx = customerCols.getOrDefault("CustomerPhone", -1);
            int customerNameIdx = customerCols.getOrDefault("CustomerName", -1);
            int customerIdIdx = customerCols.getOrDefault("CustomerID", -1);
            if (customerIdIdx == -1) {
                customerIdIdx = identifiedColumns.getOrDefault(CUSTOMER_ID, -1);
            }
            writeIndexFile("Index.txt", transactionIdIndex, productIdIndex, productNameIndex, customerIdIdx, customerNameIdx, customerPhoneIdx);

        } catch (FileNotFoundException e) {
            System.err.println("Không tìm thấy file: " + FILE_PATH);
        } catch (IOException e) {
            System.err.println("Lỗi đọc file: " + e.getMessage());
        }
    }

    // --- Thêm hàm ghi ra Index.txt ---
    private static void writeIndexFile(String filePath, int transactionIdIdx, int productIdIdx, int productNameIdx, int customerIdIdx, int customerNameIdx, int customerPhoneIdx) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(transactionIdIdx + "\n");
            bw.write(productIdIdx + "\n");
            bw.write(productNameIdx + "\n");
            bw.write(customerIdIdx + "\n");
            bw.write(customerNameIdx + "\n");
            bw.write(customerPhoneIdx + "\n");
        } catch (IOException e) {
            System.err.println("Lỗi ghi file Index.txt: " + e.getMessage());
        }
    }

    private static Map<String, Integer> identifyColumnsNewLogic(List<List<String>> columns, List<String[]> dataSample) {
        Map<String, Integer> result = new HashMap<>();

        // BƯỚC 1: Loại bỏ toàn bộ cột rỗng
        Set<Integer> validColumns = removeEmptyColumns(columns);

        // BƯỚC 2: Tìm cột tên sản phẩm (cột có nhiều ký tự nhất)
        int descriptionIndex = findDescriptionColumn(columns, validColumns);
        if (descriptionIndex == -1) {
            System.err.println("❌ Không thể tìm thấy cột Tên sản phẩm!");
            return result;
        }
        result.put(DESCRIPTION, descriptionIndex);
        validColumns.remove(descriptionIndex);

        // BƯỚC 3: Tìm cột mã sản phẩm (quan hệ 1-1 với tên sản phẩm)
        int productIdIndex = findProductIdColumn(columns, validColumns, descriptionIndex, dataSample);
        if (productIdIndex == -1) {
            System.err.println("❌ Không thể tìm thấy cột Mã sản phẩm!");
            return result;
        }
        result.put(STOCK_CODE, productIdIndex);
        validColumns.remove(productIdIndex);

        // BƯỚC 3.1: Loại bỏ cột thời gian trước khi tìm mã giao dịch
        Set<Integer> timeColumns = identifyTimeColumns(columns, validColumns);
        if (!timeColumns.isEmpty()) {
            validColumns.removeAll(timeColumns);
        }

        // BƯỚC 4: Tìm cột mã giao dịch (không có trùng lặp mã sản phẩm trong cùng giao dịch)
        int transactionIdIndex = findTransactionIdColumn(columns, validColumns, productIdIndex, dataSample);
        if (transactionIdIndex == -1) {
            System.err.println("❌ Không thể tìm thấy cột Mã giao dịch!");
            return result;
        }
        result.put(INVOICE_NO, transactionIdIndex);
        validColumns.remove(transactionIdIndex);

        // BƯỚC 5: Lọc các cột ứng viên cho mã khách hàng
        Set<Integer> customerCandidates = filterCustomerCandidates(columns, validColumns, transactionIdIndex, dataSample);

        // BƯỚC 6: Tìm cột mã khách hàng từ danh sách ứng viên đã lọc
        int customerIdIndex = findCustomerIdColumn(columns, customerCandidates, transactionIdIndex, productIdIndex, dataSample);
        if (customerIdIndex != -1) {
            result.put(CUSTOMER_ID, customerIdIndex);
        } else {
            System.err.println("❌ Không thể tìm thấy cột Mã khách hàng! Thử tìm Tên khách hàng...");
            // NEW: Fallback to find Customer Name
            int customerNameIndex = findCustomerNameColumn(columns, validColumns, transactionIdIndex, productIdIndex, dataSample);
            if (customerNameIndex != -1) {
                result.put(CUSTOMER_NAME, customerNameIndex);
            }
        }

        return result;
    }

    // BƯỚC 1: Loại bỏ cột rỗng
    private static Set<Integer> removeEmptyColumns(List<List<String>> columns) {
        Set<Integer> validColumns = new HashSet<>();

        for (int i = 0; i < columns.size(); i++) {
            List<String> column = columns.get(i);

            // Kiểm tra xem cột có ít nhất 1 giá trị không rỗng không
            boolean hasNonEmptyValue = column.stream()
                .anyMatch(s -> s != null && !s.trim().isEmpty());

            if (hasNonEmptyValue) {
                validColumns.add(i);
            }
        }

        return validColumns;
    }

    // BƯỚC 2: Tìm cột tên sản phẩm (cột có nhiều ký tự nhất)
    private static int findDescriptionColumn(List<List<String>> columns, Set<Integer> validColumns) {
        int bestIndex = -1;
        double maxAvgLength = 0;

        for (int i : validColumns) {
            double avgLength = getAverageStringLength(columns.get(i));
            if (avgLength > maxAvgLength) {
                maxAvgLength = avgLength;
                bestIndex = i;
            }
        }

        return (maxAvgLength > DESCRIPTION_MIN_AVG_LENGTH) ? bestIndex : -1;
    }

    // BƯỚC 3: Tìm cột mã sản phẩm (quan hệ 1-1 với tên sản phẩm)
    private static int findProductIdColumn(List<List<String>> columns, Set<Integer> validColumns,
                                        int descriptionIndex, List<String[]> dataSample) {

        for (int candidateIndex : validColumns) {
            // Kiểm tra cột không có giá trị rỗng
            if (!hasEmptyValues(columns.get(candidateIndex))) {
                if (hasOneToOneMapping(candidateIndex, descriptionIndex, dataSample)) {
                    return candidateIndex;
                }
            }
        }
        return -1;
    }

    // Kiểm tra quan hệ 1-1 giữa mã sản phẩm và tên sản phẩm
    private static boolean hasOneToOneMapping(int productIdIndex, int descriptionIndex, List<String[]> dataSample) {
        Map<String, Set<String>> productToDescriptionMap = new HashMap<>();
        Map<String, Set<String>> descriptionToProductMap = new HashMap<>();

        for (String[] row : dataSample) {
            if (row == null || productIdIndex >= row.length || descriptionIndex >= row.length) {
                continue;
            }

            String productId = row[productIdIndex] != null ? row[productIdIndex].trim() : "";
            String description = row[descriptionIndex] != null ? row[descriptionIndex].trim() : "";

            if (productId.isEmpty() || description.isEmpty()) {
                continue;
            }

            // Kiểm tra 1 mã sản phẩm -> 1 tên sản phẩm
            productToDescriptionMap.computeIfAbsent(productId, k -> new HashSet<>()).add(description);
            if (productToDescriptionMap.get(productId).size() > 1) {
                return false;
            }

            // Kiểm tra 1 tên sản phẩm -> 1 mã sản phẩm
            descriptionToProductMap.computeIfAbsent(description, k -> new HashSet<>()).add(productId);
            if (descriptionToProductMap.get(description).size() > 1) {
                return false;
            }
        }

        return true;
    }

    // Kiểm tra xem cột có giá trị rỗng không
    private static boolean hasEmptyValues(List<String> column) {
        if (column == null || column.isEmpty()) {
            return true;
        }

        for (String value : column) {
            if (value == null || value.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Kiểm tra xem cột có phải là số thứ tự không
    private static boolean isSequentialNumbers(List<String> column) {
        if (column == null || column.isEmpty()) {
            return false;
        }

        List<Integer> numbers = new ArrayList<>();

        // Chuyển đổi các giá trị thành số
        for (String value : column) {
            if (value == null || value.trim().isEmpty()) {
                return false; // Có giá trị rỗng
            }

            try {
                int number = Integer.parseInt(value.trim());
                numbers.add(number);
            } catch (NumberFormatException e) {
                return false; // Không phải số
            }
        }

        // Sắp xếp và kiểm tra tính liên tục
        numbers.sort(Integer::compareTo);

        for (int i = 1; i < numbers.size(); i++) {
            // Nếu không liên tục hoặc có số trùng lặp
            if (numbers.get(i) - numbers.get(i - 1) != 1) {
                return false;
            }
        }

        return true;
    }

    // Kiểm tra xem cột có chứa ít nhất 1 chữ số trong mỗi giá trị không rỗng
    private static boolean hasDigitInValues(List<String> column) {
        if (column == null || column.isEmpty()) {
            return false;
        }

        for (String value : column) {
            if (value != null && !value.trim().isEmpty()) {
                // Kiểm tra xem có ít nhất 1 chữ số không
                if (!value.matches(".*\\d.*")) {
                    return false;
                }
            }
        }
        return true;
    }

    // BƯỚC 4: Tìm cột mã giao dịch
    private static int findTransactionIdColumn(List<List<String>> columns, Set<Integer> validColumns,
                                            int productIdIndex, List<String[]> dataSample) {

        for (int candidateIndex : validColumns) {
            // Kiểm tra cột không có giá trị rỗng
            if (!hasEmptyValues(columns.get(candidateIndex))) {
                // Kiểm tra cột không phải là số thứ tự
                if (!isSequentialNumbers(columns.get(candidateIndex))) {
                    if (isValidTransactionColumn(candidateIndex, productIdIndex, dataSample)) {
                        return candidateIndex;
                    }
                }
            }
        }
        return -1;
    }

    // Kiểm tra cột giao dịch hợp lệ (không có trùng lặp mã sản phẩm trong cùng giao dịch)
    private static boolean isValidTransactionColumn(int transactionIndex, int productIdIndex, List<String[]> dataSample) {
        Map<String, Set<String>> transactionToProductsMap = new HashMap<>();

        for (String[] row : dataSample) {
            if (row == null || transactionIndex >= row.length || productIdIndex >= row.length) {
                continue;
            }

            String transactionId = row[transactionIndex] != null ? row[transactionIndex].trim() : "";
            String productId = row[productIdIndex] != null ? row[productIdIndex].trim() : "";

            if (transactionId.isEmpty() || productId.isEmpty()) {
                continue;
            }

            Set<String> productsInTransaction = transactionToProductsMap.computeIfAbsent(transactionId, k -> new HashSet<>());

            // Nếu mã sản phẩm đã tồn tại trong giao dịch này -> trùng lặp
            if (!productsInTransaction.add(productId)) {
                return false;
            }
        }

        return true;
    }
    
    // NEW: Helper to check if all non-empty values in a column are unique
    private static boolean isAllUnique(List<String> column) {
        List<String> nonEmptyValues = column.stream()
            .filter(s -> s != null && !s.trim().isEmpty())
            .collect(Collectors.toList());

        if (nonEmptyValues.size() < 2) {
            return false; // Not enough data to determine a pattern
        }

        Set<String> uniqueValues = new HashSet<>(nonEmptyValues);
        return uniqueValues.size() == nonEmptyValues.size();
    }

    // BƯỚC 5: Lọc các cột ứng viên cho mã khách hàng
    private static Set<Integer> filterCustomerCandidates(List<List<String>> columns, Set<Integer> validColumns,
                                                        int transactionIdIndex, List<String[]> dataSample) {
        Set<Integer> candidates = new HashSet<>();

        // Tạo TreeSet dựa trên mã giao dịch
        Map<String, Map<Integer, Set<String>>> transactionColumnValuesMap = new HashMap<>();

        // Thu thập dữ liệu cho mỗi giao dịch
        for (String[] row : dataSample) {
            if (row == null || transactionIdIndex >= row.length) {
                continue;
            }

            String transactionId = row[transactionIdIndex] != null ? row[transactionIdIndex].trim() : "";
            if (transactionId.isEmpty()) {
                continue;
            }

            // Cho mỗi cột ứng viên, thu thập giá trị trong giao dịch này
            for (int columnIndex : validColumns) {
                if (columnIndex < row.length) {
                    String value = row[columnIndex] != null ? row[columnIndex].trim() : "";
                    if (!value.isEmpty()) {
                        transactionColumnValuesMap.computeIfAbsent(transactionId, k -> new HashMap<>())
                            .computeIfAbsent(columnIndex, k -> new HashSet<>())
                            .add(value);
                    }
                }
            }
        }

        // Kiểm tra từng cột ứng viên
        for (int columnIndex : validColumns) {
            boolean isValidCandidate = true;

            // Kiểm tra xem cột này có chứa ít nhất 1 chữ số trong mỗi giá trị không rỗng
            if (!hasDigitInValues(columns.get(columnIndex))) {
                continue; // Bỏ qua cột không chứa chữ số
            }
            
            // NEW: Loại bỏ cột có giá trị xuyên suốt là duy nhất
            if (isAllUnique(columns.get(columnIndex))) {
                continue;
            }

            // Kiểm tra xem trong mỗi giao dịch, cột này có tối đa 1 giá trị không
            for (String transactionId : transactionColumnValuesMap.keySet()) {
                Map<Integer, Set<String>> columnValuesMap = transactionColumnValuesMap.get(transactionId);

                if (columnValuesMap.containsKey(columnIndex)) {
                    Set<String> valuesInTransaction = columnValuesMap.get(columnIndex);

                    // Nếu có 2 giá trị trở lên trong cùng giao dịch -> loại bỏ
                    if (valuesInTransaction.size() > 1) {
                        isValidCandidate = false;
                        break;
                    }
                }
            }

            if (isValidCandidate) {
                candidates.add(columnIndex);
            }
        }

        return candidates;
    }

    // BƯỚC 5: Xác định các cột chứa giá trị thời gian
    private static Set<Integer> identifyTimeColumns(List<List<String>> columns, Set<Integer> validColumns) {
        Set<Integer> timeColumns = new HashSet<>();

        for (int columnIndex : validColumns) {
            List<String> column = columns.get(columnIndex);

            int totalNonEmptyValues = 0;
            int timeFormatCount = 0;

            for (String value : column) {
                if (value != null && !value.trim().isEmpty()) {
                    totalNonEmptyValues++;

                    String trimmedValue = value.trim();
                    if (isTimeFormat(trimmedValue)) {
                        timeFormatCount++;
                    }
                }
            }

            // Nếu có ít nhất 30% giá trị có định dạng thời gian thì coi là cột thời gian
            if (totalNonEmptyValues > 0) {
                double timeRatio = (double) timeFormatCount / totalNonEmptyValues;
                if (timeRatio >= TIME_COLUMN_THRESHOLD) {
                    timeColumns.add(columnIndex);
                }
            }
        }

        return timeColumns;
    }

    // Kiểm tra xem một chuỗi có phải là định dạng thời gian không
    private static boolean isTimeFormat(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String trimmedValue = value.trim();

        // Kiểm tra với các pattern định dạng thời gian
        for (Pattern pattern : TIME_PATTERNS) {
            if (pattern.matcher(trimmedValue).matches()) {
                return true;
            }
        }

        // Kiểm tra thêm một số trường hợp đặc biệt
        try {
            // Kiểm tra số có phải là timestamp không (10 hoặc 13 chữ số)
            if (trimmedValue.matches("^\\d{10}$") || trimmedValue.matches("^\\d{13}$")) {
                long timestamp = Long.parseLong(trimmedValue);
                // Kiểm tra xem timestamp có hợp lý không (từ năm 1970 đến năm 2099)
                long minTimestamp = 0L; // 1970-01-01
                long maxTimestamp = 4102444800000L; // 2099-12-31

                if (trimmedValue.length() == 10) {
                    timestamp *= 1000; // Convert to milliseconds
                }

                return timestamp >= minTimestamp && timestamp <= maxTimestamp;
            }
        } catch (NumberFormatException e) {
            // Không phải số, bỏ qua
        }

        return false;
    }

    // BƯỚC 6: Tìm cột mã khách hàng từ danh sách ứng viên đã lọc
    private static int findCustomerIdColumn(List<List<String>> columns, Set<Integer> candidateColumns,
                                         int transactionIdIndex, int productIdIndex, List<String[]> dataSample) {

        for (int candidateIndex : candidateColumns) {
            if (isValidCustomerColumn(candidateIndex, transactionIdIndex, productIdIndex, dataSample)) {
                return candidateIndex;
            }
        }
        return -1;
    }
    
    // NEW: Helper to check if a column contains no digits
    private static boolean containsNoDigits(List<String> column) {
        if (column == null || column.isEmpty()) {
            return true;
        }
        for (String value : column) {
            if (value != null && !value.trim().isEmpty()) {
                if (value.matches(".*\\d.*")) {
                    return false; // Found a digit
                }
            }
        }
        return true; // No digits found
    }

    // NEW: Find Customer Name column if Customer ID is not found
    private static int findCustomerNameColumn(List<List<String>> columns, Set<Integer> validColumns,
                                              int transactionIdIndex, int productIdIndex, List<String[]> dataSample) {
        for (int candidateIndex : validColumns) {
            // Rule 1: A name column should not contain digits.
            if (!containsNoDigits(columns.get(candidateIndex))) {
                continue;
            }
            
            // Rule 2: A name column should not have all unique values (customers should reappear).
            if (isAllUnique(columns.get(candidateIndex))) {
                continue;
            }

            // Rule 3: Use the strict check to ensure one name per transaction.
            if (isValidCustomerColumn(candidateIndex, transactionIdIndex, productIdIndex, dataSample)) {
                return candidateIndex; // Found a valid name column
            }
        }
        return -1;
    }

    // Kiểm tra cột khách hàng hợp lệ (mỗi giao dịch chỉ có 1 giá trị khách hàng)
    private static boolean isValidCustomerColumn(int customerIndex, int transactionIdIndex,
                                               int productIdIndex, List<String[]> dataSample) {

        // Tạo TreeSet dựa trên mã giao dịch, chứa mã sản phẩm và giá trị khách hàng
        Map<String, Map<String, Set<String>>> transactionProductCustomerMap = new HashMap<>();

        for (String[] row : dataSample) {

            if (row == null || transactionIdIndex >= row.length ||
                productIdIndex >= row.length || customerIndex >= row.length) {
                continue;
            }

            String transactionId = row[transactionIdIndex] != null ? row[transactionIdIndex].trim() : "";
            String productId = row[productIdIndex] != null ? row[productIdIndex].trim() : "";
            String customerId = row[customerIndex] != null ? row[customerIndex].trim() : "";

            if (transactionId.isEmpty() || productId.isEmpty() || customerId.isEmpty()) {
                continue;
            }

            // Tạo cấu trúc: TransactionId -> ProductId -> Set<CustomerId>
            transactionProductCustomerMap.computeIfAbsent(transactionId, k -> new HashMap<>())
                .computeIfAbsent(productId, k -> new HashSet<>())
                .add(customerId);
        }

        // Kiểm tra: trong mỗi giao dịch, mỗi sản phẩm chỉ có 1 khách hàng
        for (String transactionId : transactionProductCustomerMap.keySet()) {
            Map<String, Set<String>> productCustomerMap = transactionProductCustomerMap.get(transactionId);

            // Tập hợp tất cả khách hàng trong giao dịch này
            Set<String> allCustomersInTransaction = new HashSet<>();
            for (Set<String> customers : productCustomerMap.values()) {

                if (customers.size() > 1) {
                    // Nếu có sản phẩm nào có nhiều hơn 1 khách hàng -> không hợp lệ
                    return false;
                }
                allCustomersInTransaction.addAll(customers);
            }

            // Mỗi giao dịch chỉ nên có 1 khách hàng và giá trị đó không được chứa space
            if (allCustomersInTransaction.size() > 1 || allCustomersInTransaction.stream().anyMatch(c -> c.contains(" "))) {
                return false; // Nhiều khách hàng hoặc có space
            }
        }

        return true;
    }

    // NEW: Candidate detection and matching logic for CustomerPhone, CustomerName, CustomerID
    private static Map<String, Integer> identifyCustomerColumns(List<List<String>> columns, List<String[]> dataSample, int transactionIdIndex) {
        Map<String, Integer> result = new HashMap<>();

        // 1. Tạo danh sách ứng viên cho từng loại
        List<Integer> phoneCandidates = new ArrayList<>();
        List<Integer> nameCandidates = new ArrayList<>();
        List<Integer> idCandidates = new ArrayList<>();

        for (int col = 0; col < columns.size(); col++) {
            List<String> column = columns.get(col);

            // LOẠI BỎ SỚM
            // 1. Toàn bộ giá trị rỗng
            boolean allEmpty = column.stream().allMatch(s -> s == null || s.trim().isEmpty());
            if (allEmpty) continue;

            // 2. Giá trị đơn nhất xuyên suốt
            Set<String> nonEmptyValues = column.stream().filter(s -> s != null && !s.trim().isEmpty()).collect(Collectors.toSet());
            if (nonEmptyValues.size() == 1) continue;

            // 3. Giá trị lặp lại hoàn toàn (chỉ giữ lại dòng đầu tiên)
            boolean allSame = false;
            if (!nonEmptyValues.isEmpty()) {
                String first = nonEmptyValues.iterator().next();
                allSame = column.stream().filter(s -> s != null && !s.trim().isEmpty()).allMatch(s -> s.trim().equals(first));
            }
            if (allSame) continue;

            // 4. Số thứ tự
            if (isSequentialNumbers(column)) continue;

            // 5. Ngày tháng
            int timeCount = 0;
            int nonEmptyCount = 0;
            for (String v : column) {
                if (v != null && !v.trim().isEmpty()) {
                    nonEmptyCount++;
                    if (isTimeFormat(v.trim()) && v.trim().charAt(0) != '0') timeCount++;
                }
            }
            if (nonEmptyCount > 0 && ((double)timeCount / nonEmptyCount) >= TIME_COLUMN_THRESHOLD) continue;

            // Ứng viên Phone
            if (isCustomerPhoneCandidate(column)) {
                phoneCandidates.add(col);
            }
            // Ứng viên Name
            if (isCustomerNameCandidate(column)) {
                nameCandidates.add(col);
            }
            // Ứng viên ID
            if (isCustomerIdCandidate(column)) {
                idCandidates.add(col);
            }
        }

        // 2. Đối chiếu theo thứ tự ưu tiên
        // Trường hợp 3: Bộ ba ID-Name-Phone
        for (int idCol : idCandidates) {
            for (int nameCol : nameCandidates) {
                for (int phoneCol : phoneCandidates) {
                    if (isValidCustomerTriple(idCol, nameCol, phoneCol, transactionIdIndex, dataSample)) {
                        result.put("CustomerID", idCol);
                        result.put("CustomerName", nameCol);
                        result.put("CustomerPhone", phoneCol);
                        return result;
                    }
                }
            }
        }

        // Trường hợp 1: Cặp Phone-Name
        for (int phoneCol : phoneCandidates) {
            for (int nameCol : nameCandidates) {
                if (isValidCustomerPair(phoneCol, nameCol, transactionIdIndex, dataSample)) {
                    result.put("CustomerPhone", phoneCol);
                    result.put("CustomerName", nameCol);
                    return result;
                }
            }
        }

        // Trường hợp 1: Cặp Phone-ID
        for (int phoneCol : phoneCandidates) {
            for (int idCol : idCandidates) {
                if (isValidCustomerPair(phoneCol, idCol, transactionIdIndex, dataSample)) {
                    result.put("CustomerPhone", phoneCol);
                    result.put("CustomerID", idCol);
                    return result;
                }
            }
        }

        // Trường hợp 2: Chỉ Phone
        for (int phoneCol : phoneCandidates) {
            if (isValidCustomerSingle(phoneCol, transactionIdIndex, dataSample)) {
                result.put("CustomerPhone", phoneCol);
                return result;
            }
        }
        // Trường hợp 2: Chỉ Name
        for (int nameCol : nameCandidates) {
            if (isValidCustomerSingle(nameCol, transactionIdIndex, dataSample)) {
                result.put("CustomerName", nameCol);
                return result;
            }
        }
        // Trường hợp 2: Chỉ ID
        for (int idCol : idCandidates) {
            if (isValidCustomerSingle(idCol, transactionIdIndex, dataSample)) {
                result.put("CustomerID", idCol);
                return result;
            }
        }

        // Không tìm thấy
        result.put("CustomerPhone", -1);
        result.put("CustomerName", -1);
        result.put("CustomerID", -1);
        return result;
    }

    // Kiểm tra ứng viên CustomerPhone
    private static boolean isCustomerPhoneCandidate(List<String> column) {
        for (String value : column) {
            if (value == null || value.trim().isEmpty()) continue;
            String v = value.trim();
            if (v.contains(" ")) return false;
            if (!v.matches("\\d{9,12}")) return false;
            // Phân biệt rõ ràng với ngày tháng/timestamp
            if (isTimeFormat(v) && v.trim().charAt(0) != '0') return false;
        }
        return true;
    }

    // Kiểm tra ứng viên CustomerName
    private static boolean isCustomerNameCandidate(List<String> column) {
        for (String value : column) {
            if (value == null || value.trim().isEmpty()) continue;
            String v = value.trim();
            if (!v.matches("[a-zA-ZÀ-ỹ\\s]+")) return false; // Chỉ chữ và khoảng trắng
            if (!v.matches(".*[a-zA-ZÀ-ỹ].*")) return false; // Phải có ít nhất một chữ
        }
        return true;
    }

    // Kiểm tra ứng viên CustomerID
    private static boolean isCustomerIdCandidate(List<String> column) {
        for (String value : column) {
            if (value == null || value.trim().isEmpty()) continue;
            String v = value.trim();
            if (v.contains(" ")) return false;
            if (!v.matches("\\d+") && !v.matches("[a-zA-Z0-9]+")) return false;
        }
        return true;
    }

    // Kiểm tra cặp Phone-Name hoặc Phone-ID
    private static boolean isValidCustomerPair(int colA, int colB, int transactionIdIndex, List<String[]> dataSample) {
        Map<String, String> aToB = new HashMap<>();
        Map<String, String> bToA = new HashMap<>();
        Map<String, Set<String>> transactionA = new HashMap<>();
        Map<String, Set<String>> transactionB = new HashMap<>();

        for (String[] row : dataSample) {
            if (row == null || transactionIdIndex >= row.length ||
                colA >= row.length || colB >= row.length) continue;
            String trans = row[transactionIdIndex].trim();
            String va = row[colA].trim();
            String vb = row[colB].trim();
            if (trans.isEmpty() || va.isEmpty() || vb.isEmpty()) continue;

            // 1-1 mapping
            if (aToB.containsKey(va) && !aToB.get(va).equals(vb)) return false;
            if (bToA.containsKey(vb) && !bToA.get(vb).equals(va)) return false;
            aToB.put(va, vb);
            bToA.put(vb, va);

            // Toàn vẹn giao dịch
            transactionA.computeIfAbsent(trans, k -> new HashSet<>()).add(va);
            transactionB.computeIfAbsent(trans, k -> new HashSet<>()).add(vb);
            if (transactionA.get(trans).size() > 1 || transactionB.get(trans).size() > 1) return false;
        }
        return true;
    }

    // Kiểm tra bộ ba ID-Name-Phone
    private static boolean isValidCustomerTriple(int idCol, int nameCol, int phoneCol, int transactionIdIndex, List<String[]> dataSample) {
        Map<String, String> idToName = new HashMap<>();
        Map<String, String> idToPhone = new HashMap<>();
        Map<String, String> nameToId = new HashMap<>();
        Map<String, String> phoneToId = new HashMap<>();
        Map<String, Set<String>> transactionIds = new HashMap<>();

        for (String[] row : dataSample) {
            if (row == null || transactionIdIndex >= row.length ||
                idCol >= row.length || nameCol >= row.length || phoneCol >= row.length) continue;
            String trans = row[transactionIdIndex].trim();
            String id = row[idCol].trim();
            String name = row[nameCol].trim();
            String phone = row[phoneCol].trim();
            if (trans.isEmpty() || id.isEmpty() || name.isEmpty() || phone.isEmpty()) continue;

            // 1-1-1 mapping
            if (idToName.containsKey(id) && !idToName.get(id).equals(name)) return false;
            if (idToPhone.containsKey(id) && !idToPhone.get(id).equals(phone)) return false;
            if (nameToId.containsKey(name) && !nameToId.get(name).equals(id)) return false;
            if (phoneToId.containsKey(phone) && !phoneToId.get(phone).equals(id)) return false;
            idToName.put(id, name);
            idToPhone.put(id, phone);
            nameToId.put(name, id);
            phoneToId.put(phone, id);

            // Toàn vẹn giao dịch
            String key = id + "|" + name + "|" + phone;
            transactionIds.computeIfAbsent(trans, k -> new HashSet<>()).add(key);
            if (transactionIds.get(trans).size() > 1) return false;
        }
        return true;
    }

    // Kiểm tra từng cột riêng lẻ
    private static boolean isValidCustomerSingle(int col, int transactionIdIndex, List<String[]> dataSample) {
        Map<String, Set<String>> transactionValues = new HashMap<>();
        for (String[] row : dataSample) {
            if (row == null || transactionIdIndex >= row.length || col >= row.length) continue;
            String trans = row[transactionIdIndex].trim();
            String val = row[col].trim();
            if (trans.isEmpty() || val.isEmpty()) continue;
            transactionValues.computeIfAbsent(trans, k -> new HashSet<>()).add(val);
            if (transactionValues.get(trans).size() > 1) return false;
        }
        return true;
    }

    private static double getAverageStringLength(List<String> column) {
        if (column == null || column.isEmpty()) return 0;

        double totalLength = 0;
        int nonEmptyCount = 0;

        for (String s : column) {
            if (s != null && !s.trim().isEmpty()) {
                totalLength += s.length();
                nonEmptyCount++;
            }
        }
        return nonEmptyCount > 0 ? totalLength / nonEmptyCount : 0;
    }

    private static List<String[]> readCsvSample(String filePath, int sampleSize) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String delimiter = ",";
            int lineCount = 0;

            while ((line = br.readLine()) != null && lineCount < sampleSize + 1) {
                if (lineCount == 0) { // Auto-detect delimiter on the first line
                    if (line.contains(";") && line.split(";", -1).length > line.split(",", -1).length) {
                        delimiter = ";";
                    }
                }
                records.add(line.split(delimiter, -1));
                lineCount++;
            }
        }
        return records;
    }

    private static List<List<String>> transposeData(List<String[]> data) {
        if (data == null || data.isEmpty()) return new ArrayList<>();

        int numColumns = data.stream()
            .filter(row -> row != null)
            .mapToInt(row -> row.length)
            .max()
            .orElse(0);

        List<List<String>> columns = new ArrayList<>(numColumns);
        for (int i = 0; i < numColumns; i++) {
            columns.add(new ArrayList<>());
        }

        for (String[] row : data) {
            if (row == null) continue;

            for (int i = 0; i < numColumns; i++) {
                if (i < row.length && row[i] != null) {
                    columns.get(i).add(row[i].trim());
                } else {
                    columns.get(i).add("");
                }
            }
        }
        return columns;
    }

    private static void printResults(Map<String, Integer> identifiedColumns, int totalColumns, Map<String, Integer> customerCols) {
        // Chỉ in thông báo hoàn thành tự động hóa
        System.out.println("✅ Đã hoàn thành tự phân tích và tự động hóa chọn chỉ số cột cần thiết");
    }

    // Thêm hàm này để loại bỏ các cột đã chọn trước khi xác định cột khách hàng ưu tiên
    private static Map<String, Integer> identifyCustomerColumnsExclude(List<List<String>> columns, List<String[]> dataSample, int transactionIdIndex, Set<Integer> excludeIndices) {
        Map<String, Integer> result = new HashMap<>();

        List<Integer> phoneCandidates = new ArrayList<>();
        List<Integer> nameCandidates = new ArrayList<>();
        List<Integer> idCandidates = new ArrayList<>();

        for (int col = 0; col < columns.size(); col++) {
            if (excludeIndices.contains(col)) continue;
            List<String> column = columns.get(col);

            // ...loại bỏ sớm như logic hiện tại...
            boolean allEmpty = column.stream().allMatch(s -> s == null || s.trim().isEmpty());
            if (allEmpty) continue;

            Set<String> nonEmptyValues = column.stream().filter(s -> s != null && !s.trim().isEmpty()).collect(Collectors.toSet());
            if (nonEmptyValues.size() == 1) continue;

            boolean allSame = false;
            if (!nonEmptyValues.isEmpty()) {
                String first = nonEmptyValues.iterator().next();
                allSame = column.stream().filter(s -> s != null && !s.trim().isEmpty()).allMatch(s -> s.trim().equals(first));
            }
            if (allSame) continue;

            if (isSequentialNumbers(column)) continue;

            int timeCount = 0;
            int nonEmptyCount = 0;
            for (String v : column) {
                if (v != null && !v.trim().isEmpty()) {
                    nonEmptyCount++;
                    if (isTimeFormat(v.trim()) && v.trim().charAt(0) != '0') timeCount++;
                }
            }
            if (nonEmptyCount > 0 && ((double)timeCount / nonEmptyCount) >= TIME_COLUMN_THRESHOLD) continue;

            if (isCustomerPhoneCandidate(column)) {
                phoneCandidates.add(col);
            }
            if (isCustomerNameCandidate(column)) {
                nameCandidates.add(col);
            }
            if (isCustomerIdCandidate(column)) {
                idCandidates.add(col);
            }
        }

        // 2. Đối chiếu theo thứ tự ưu tiên
        // Trường hợp 3: Bộ ba ID-Name-Phone
        for (int idCol : idCandidates) {
            for (int nameCol : nameCandidates) {
                for (int phoneCol : phoneCandidates) {
                    if (isValidCustomerTriple(idCol, nameCol, phoneCol, transactionIdIndex, dataSample)) {
                        result.put("CustomerID", idCol);
                        result.put("CustomerName", nameCol);
                        result.put("CustomerPhone", phoneCol);
                        return result;
                    }
                }
            }
        }

        // Trường hợp 1: Cặp Phone-Name
        for (int phoneCol : phoneCandidates) {
            for (int nameCol : nameCandidates) {
                if (isValidCustomerPair(phoneCol, nameCol, transactionIdIndex, dataSample)) {
                    result.put("CustomerPhone", phoneCol);
                    result.put("CustomerName", nameCol);
                    return result;
                }
            }
        }

        // Trường hợp 1: Cặp Phone-ID
        for (int phoneCol : phoneCandidates) {
            for (int idCol : idCandidates) {
                if (isValidCustomerPair(phoneCol, idCol, transactionIdIndex, dataSample)) {
                    result.put("CustomerPhone", phoneCol);
                    result.put("CustomerID", idCol);
                    return result;
                }
            }
        }

        // Trường hợp 2: Chỉ Phone
        for (int phoneCol : phoneCandidates) {
            if (isValidCustomerSingle(phoneCol, transactionIdIndex, dataSample)) {
                result.put("CustomerPhone", phoneCol);
                return result;
            }
        }
        // Trường hợp 2: Chỉ Name
        for (int nameCol : nameCandidates) {
            if (isValidCustomerSingle(nameCol, transactionIdIndex, dataSample)) {
                result.put("CustomerName", nameCol);
                return result;
            }
        }
        // Trường hợp 2: Chỉ ID
        for (int idCol : idCandidates) {
            if (isValidCustomerSingle(idCol, transactionIdIndex, dataSample)) {
                result.put("CustomerID", idCol);
                return result;
            }
        }

        // Không tìm thấy
        result.put("CustomerPhone", -1);
        result.put("CustomerName", -1);
        result.put("CustomerID", -1);
        return result;
    }
}