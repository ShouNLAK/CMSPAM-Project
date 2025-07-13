

<h1 align="center">🚀 Trợ Lý Bán Hàng Thông Minh thế hệ mới 🚀</h1>
<p align="center">
<i>Nâng cấp với thuật toán CM-SPAM và Tự động nhận diện dữ liệu</i>
</p>

<p align="center">
<img src="https://img.shields.io/badge/Java-1.8%2B-blue.svg" />
  <img src="https://img.shields.io/badge/SPMF-Pattern%20Mining-orange.svg" />
  <img src="https://img.shields.io/badge/CLI-Assistant-green.svg" />
</p>

-----

## 🌟 Giới Thiệu

Đây là phiên bản cải tiến vượt trội của dự án **Trợ Lý Bán Hàng Thông Minh**, được thiết kế để cách mạng hóa cách bạn phân tích dữ liệu giao dịch. Không chỉ kế thừa những tính năng mạnh mẽ của phiên bản trước, dự án này được nâng cấp với hai công nghệ cốt lõi:

1.  **Thuật toán CM-SPAM**: Một phiên bản tối ưu của SPAM, giúp khai phá mẫu tuần tự với tốc độ vượt trội và hiệu suất cao hơn.
2.  **Tự động nhận diện cột (AutoPickFixed)**: Hệ thống không còn yêu cầu bạn phải cấu hình thủ công vị trí các cột dữ liệu. Cơ chế thông minh sẽ tự động phân tích file CSV đầu vào và xác định chính xác đâu là `Mã giao dịch`, `Mã sản phẩm`, `Tên sản phẩm` và `Thông tin khách hàng`.

Dự án này cung cấp một giải pháp toàn diện, từ khâu chuẩn bị dữ liệu tự động đến phân tích sâu và đưa ra gợi ý kinh doanh giá trị, tất cả gói gọn trong một giao diện dòng lệnh (CLI) thân thiện và mạnh mẽ.

-----

## 🛠️ Những Cải Tiến Vượt Trội

### 🧠 Tự Động Hóa với `AutoPickFixed.java`

Nói lời tạm biệt với việc phải chỉnh sửa mã nguồn hay file cấu hình mỗi khi có dữ liệu mới. `AutoPickFixed` là bộ não phân tích của hệ thống:

  - **Tự động quét**: Đọc và phân tích cấu trúc file CSV của bạn.
  - **Logic suy luận thông minh**: Dựa trên các quy luật về mối quan hệ dữ liệu (ví dụ: một mã sản phẩm chỉ có một tên sản phẩm duy nhất, một khách hàng có thể có nhiều giao dịch), hệ thống sẽ tự động xác định các cột cần thiết.
  - **Linh hoạt & Tiết kiệm thời gian**: Tương thích với nhiều định dạng file bán hàng khác nhau mà không cần can thiệp thủ công.

### ⚡ Khai Phá Tốc Độ Cao với `CM-SPAM`

Dự án được nâng cấp từ SPAM lên **CM-SPAM** (Co-occurrence Map SPAM), một thuật toán khai phá mẫu tuần tự tiên tiến từ thư viện SPMF.

  - **Tốc độ vượt trội**: Sử dụng cấu trúc `Co-occurrence Map` để lọc bỏ những ứng viên không tiềm năng từ sớm, giảm đáng kể thời gian tính toán.
  - **Tối ưu bộ nhớ**: Hiệu quả hơn trong việc quản lý bộ nhớ, đặc biệt với các tập dữ liệu lớn.
  - **Kết quả chính xác**: Đảm bảo tìm ra các mẫu mua hàng tuần tự phổ biến một cách hiệu quả để đưa ra những gợi ý đáng giá.

-----

## 📊 So Sánh Hiệu Năng: SPAM và CM-SPAM

**CM-SPAM** không chỉ là một bản cập nhật mà là một bước nhảy vọt về hiệu suất so với thuật toán SPAM truyền thống.

| Tính năng | SPAM (Truyền thống) | ⭐ **CM-SPAM (Phiên bản nâng cấp)** |
| :--- | :--- | :--- |
| **Tốc độ** | Tương đối | Nhanh hơn đáng kể |
| **Kỹ thuật cốt lõi** | Dùng biểu diễn bitmap dọc | Kết hợp **Co-occurrence Map (CMAP)** và bitmap dọc |
| **Cắt tỉa tìm kiếm** | Dựa trên support cơ bản | **Cắt tỉa thông minh** nhờ CMAP, loại bỏ sớm các nhánh không tiềm năng, giảm không gian tìm kiếm |
| **Sử dụng bộ nhớ**| Tốt | Tối ưu hơn, yêu cầu ít bộ nhớ hơn |
| **Độ phức tạp** | Thấp hơn | Cao hơn nhưng mang lại hiệu quả vượt trội |
| **Trường hợp lý tưởng** | Các tập dữ liệu thông thường | Các tập dữ liệu **lớn và dày đặc** (như lịch sử giao dịch bán lẻ) |

-----

## 🖥️ Giao Diện & Tính Năng Chính

Hệ thống cung cấp một giao diện dòng lệnh (CLI) trực quan và giàu tính năng:

  - **`1. Bảng quy đổi sản phẩm`**: Xem nhanh danh sách mã và tên sản phẩm.
  - **`2. Nhập phiên giao dịch mới`**: Mô phỏng một phiên bán hàng, nhận gợi ý và khuyến mãi theo thời gian thực.
  - **`3. Xem lịch sử giao dịch`**: Theo dõi các giao dịch đã nhập.
  - **`4. Xem mẫu thường xuyên`**: Hiển thị các chuỗi sản phẩm hay được mua cùng nhau.
  - **`5. Đề xuất sau mua`**: Nhận gợi ý sản phẩm dựa trên giỏ hàng hiện tại.
  - **`6. Danh sách khuyến mãi`**: Khám phá các combo sản phẩm tiềm năng.
  - **`7. Khai thác luật kết hợp`**: Phân tích sâu hơn về các mẫu với độ tin cậy.
  - **`8. Xem Top-K mẫu tuần tự`**: Liệt kê K mẫu mua hàng phổ biến nhất.
  - **`9. Tóm tắt & Trực quan hóa`**: Thống kê và biểu đồ văn bản về tần suất xuất hiện của sản phẩm trong các mẫu.
  - **`10. Truy vấn mẫu tuần tự`**: Tìm kiếm các mẫu có chứa một chuỗi sản phẩm cụ thể.
  - **`(-1). Tùy chọn nâng cao`**: Tinh chỉnh các tham số của thuật toán CM-SPAM như `minsup`, `maxPatternLength`.
  - **`(-2). Chỉnh sửa chỉ số cột`**: Can thiệp thủ công vào các chỉ số cột nếu cần.

-----

## ⚙️ Quy Trình Hoạt Động

Hệ thống hoạt động theo một quy trình thông minh và hoàn toàn tự động:

1.  **Bước 1: Tự động nhận diện (`AutoPickFixed.java`)**

      - Chương trình khởi chạy và tự động phân tích file `Database 1.csv` (hoặc file đầu vào được chỉ định).
      - Các quy luật trong `Quy luật.txt` được áp dụng để xác định chỉ số các cột quan trọng.
      - Kết quả chỉ số được lưu vào file `Index.txt`.

2.  **Bước 2: Chuẩn bị dữ liệu (`IntegratedSalesAssistant.java`)**

      - Đọc các chỉ số cột từ `Index.txt`.
      - Xử lý, làm sạch và chuyển đổi dữ liệu thô sang định dạng chuẩn của SPMF (`sales_transactions.txt`).
      - Tạo file chi tiết sản phẩm `Product_Details.csv`.

3.  **Bước 3: Khai phá mẫu (`AlgoCMSPAM.java`)**

      - Thuật toán CM-SPAM được thực thi trên file `sales_transactions.txt`.
      - Các mẫu tuần tự phổ biến được phát hiện và lưu vào `sales_patterns.txt`.

4.  **Bước 4: Tương tác và Phân tích (`IntegratedSalesAssistant.java`)**

      - Người dùng tương tác với menu chức năng trên giao diện CLI.
      - Hệ thống sử dụng các mẫu đã khai phá để cung cấp thông tin, thống kê, và các gợi ý bán hàng thông minh.

-----

## 🚀 Hướng Dẫn Sử Dụng

1.  **Biên dịch dự án**:

      - Đảm bảo bạn đã cài đặt JDK (Java Development Kit) 8 trở lên.
      - Sử dụng một IDE như IntelliJ, Eclipse hoặc dùng `javac` trên dòng lệnh để biên dịch tất cả các file `.java`.

2.  **Chuẩn bị dữ liệu**:

      - Đặt file dữ liệu bán hàng của bạn (ví dụ: `Database 1.csv`) vào cùng thư mục với dự án.

3.  **Chạy chương trình**:

      - Thực thi file `IntegratedSalesAssistant.java`.
      - Chương trình sẽ tự động chạy `AutoPickFixed` để phân tích dữ liệu trước.
      - Làm theo các hướng dẫn trên menu để khám phá các chức năng.

4.  **Xem kết quả**:

      - Các file kết quả như `Index.txt`, `sales_patterns.txt`, `Product_Details.csv` và `sales_transactions.txt` sẽ được tạo ra trong thư mục dự án.

-----

## 📞 Tác giả & Đóng góp

  - **Tác giả**: ShouNLAK
  - **Nền tảng thuật toán**: Dựa trên thư viện **SPMF** của Philippe Fournier-Viger.
  - Mọi ý kiến đóng góp xin vui lòng gửi qua email hoặc tạo một Issue trên GitHub.

-----

<p align="center">
<b>CMAP - SPAM Project: Trợ lý bán hàng thông minh, tối ưu hóa doanh nghiệp của bạn!</b>
</p>
