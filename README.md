# MealChoice - Trưa nay ăn gì?

MealChoice là Web Application hỗ trợ khách hàng tìm kiếm món ăn, lựa chọn cửa hàng và đặt món trực tuyến. Hệ thống đồng thời cung cấp cổng quản lý cho Merchant và Admin, bao gồm quản lý món ăn, đơn hàng, coupon, doanh thu, đối tác và thanh toán.

## Mục lục

- [Tổng quan](#tổng-quan)
- [Tính năng](#tính-năng)
- [Tech Stack](#tech-stack)
- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cài đặt và chạy dự án](#cài-đặt-và-chạy-dự-án)
- [Cấu hình](#cấu-hình)
- [Các route chính](#các-route-chính)
- [Tài khoản và phân quyền](#tài-khoản-và-phân-quyền)
- [Quy trình làm việc nhóm](#quy-trình-làm-việc-nhóm)
- [Định nghĩa hoàn thành](#định-nghĩa-hoàn-thành-dod)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Kiểm thử](#kiểm-thử)
- [Đóng góp](#đóng-góp)

## Tổng quan

| Hạng mục | Thông tin |
|---|---|
| Tên dự án | MealChoice - Trưa nay ăn gì? |
| Loại ứng dụng | Web Application |
| Backend | Spring Boot 3.4.1, Java 17 |
| Giao diện | Thymeleaf, HTML, CSS, JavaScript |
| Cơ sở dữ liệu | MySQL |
| Build tool | Gradle và Gradle Wrapper |
| Cổng mặc định | `8080` |
| Package gốc | `vn.codegyme.meal_choice` |

## Tính năng

### Khách hàng (User/Customer)

- Đăng ký, đăng nhập và xác thực tài khoản qua liên kết email.
- Quản lý hồ sơ và nhiều địa chỉ giao hàng.
- Tìm kiếm món ăn nhanh, xem gợi ý, món mới, nhà hàng nổi bật và món đang giảm giá.
- Xem chi tiết món ăn, thông tin cửa hàng và theo dõi cửa hàng yêu thích.
- Quản lý giỏ hàng, chọn coupon và đặt hàng.
- Xem chi tiết, lịch sử và trạng thái đơn hàng; hỗ trợ hủy đơn theo trạng thái cho phép.
- Nhận báo giá giao hàng và hỗ trợ định vị địa chỉ thông qua dịch vụ bản đồ.

### Cửa hàng (Merchant)

- Đăng ký Merchant và theo dõi trạng thái phê duyệt.
- Đăng ký chương trình đối tác thân thiết.
- Quản lý món ăn: thêm, xem, sửa, xóa và tìm kiếm món ăn.
- Quản lý đơn hàng: xem chi tiết, tìm theo mã đơn/số điện thoại/tên, xử lý trạng thái, hủy đơn.
- Thống kê doanh số theo tuần, tháng, quý; phân tích theo món ăn, khách hàng và coupon.
- Đối soát doanh thu, gửi yêu cầu claim, thanh lý hợp đồng và yêu cầu rút tiền.
- Tạo, chỉnh sửa, kích hoạt và xóa coupon.

### Quản trị viên (Admin)

- Duyệt hoặc từ chối đăng ký Merchant.
- Khóa, mở khóa Merchant và xem danh sách Merchant bị khóa.
- Duyệt đối tác thân thiết.
- Quản lý đối tác vận chuyển.
- Quản lý yêu cầu payout và hoàn tất đối soát.
- Cấu hình giao diện: menu top, footer và banner.
- Cấu hình tỷ lệ chiết khấu đơn hàng.

## Tech Stack

### Backend và framework

- **Java 17**: phiên bản ngôn ngữ thông qua Java Toolchain.
- **Spring Boot 3.4.1**: nền tảng chính cho ứng dụng.
- **Spring Web**: xây dựng MVC pages và REST API.
- **Spring Data JPA**: truy cập dữ liệu theo mô hình Repository/Entity.
- **Hibernate**: JPA implementation đi kèm Spring Boot.
- **Spring Validation**: kiểm tra dữ liệu đầu vào ở server bằng Bean Validation.
- **Spring Mail**: gửi email kích hoạt tài khoản.
- **Lombok**: giảm boilerplate code, dùng ở compile time.

### Frontend

- **Thymeleaf** và **Thymeleaf Extras Spring Security 6**: server-side template và hiển thị theo quyền người dùng.
- **HTML/CSS/JavaScript**: giao diện và các tương tác phía client.
- Các tài nguyên tĩnh nằm trong `src/main/resources/static`.

### Database và tích hợp

- **MySQL**: cơ sở dữ liệu quan hệ.
- **MySQL Connector/J**: JDBC driver chạy ở runtime.
- **OpenRouteService**: geocoding và directions thông qua các URL cấu hình `heigit.*`.

### Security và xác thực

- **Spring Security**: xác thực, phân quyền và bảo vệ tài nguyên.
- **JJWT 0.12.6**: tạo và xác minh access token/refresh token.
- **Spring Security Test**: hỗ trợ kiểm thử các luồng bảo mật.

### Build và kiểm thử

- **Gradle** với Gradle Wrapper (`gradlew`, `gradlew.bat`): build nhất quán giữa các môi trường.
- **Spring Boot Gradle Plugin 3.4.1**.
- **Spring Dependency Management 1.1.7**.
- **JUnit Platform** và **Spring Boot Starter Test**.
- Compiler được cấu hình với `-parameters` và test chạy bằng `useJUnitPlatform()`.

## Kiến trúc tổng quan

Ứng dụng triển khai theo mô hình nhiều lớp:

```text
Browser
  |
  +-- Thymeleaf Pages / Static JavaScript
  |
  +-- REST Controllers (/api/*)
          |
          +-- Services: nghiệp vụ và transaction
          |
          +-- Repositories: Spring Data JPA
          |
          +-- MySQL

Tích hợp ngoài: SMTP Gmail | OpenRouteService
```

Các lớp chính trong mã nguồn:

- `controller`: tiếp nhận request web và REST.
- `service`: xử lý nghiệp vụ dùng chung.
- `repository`: truy vấn và lưu entity.
- `entity`: mô hình dữ liệu JPA.
- `dto`: request/response object và validation contract.
- `security`: cấu hình Spring Security và JWT.
- `resources/templates`: giao diện Thymeleaf.
- `resources/static`: CSS và JavaScript phía client.

## Yêu cầu môi trường

Cài đặt trước các thành phần sau:

- JDK 17 trở lên, ưu tiên đúng Java 17 theo cấu hình dự án.
- MySQL 8.x hoặc phiên bản tương thích.
- Git.
- Kết nối mạng nếu cần gửi email hoặc gọi OpenRouteService.

Kiểm tra môi trường:

```bash
java -version
```

Trên Windows, có thể sử dụng trực tiếp `gradlew.bat`; trên Linux/macOS sử dụng `./gradlew`.

## Cài đặt và chạy dự án

### 1. Clone mã nguồn

```bash
git clone <URL_REPOSITORY>
cd MealChoice-anh
```

### 2. Chuẩn bị MySQL

Tạo database hoặc để ứng dụng tự tạo database thông qua cấu hình `createDatabaseIfNotExist=true`:

```sql
CREATE DATABASE meal_choice CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Nếu cần dữ liệu mẫu, tham khảo các file `database/schema.sql` và `database/seed.sql`. Khi chạy thông thường, ứng dụng đang dùng `spring.jpa.hibernate.ddl-auto=update` để cập nhật schema.

### 3. Cấu hình thông tin bí mật

Không commit mật khẩu email, JWT secret hoặc API key vào Git. Có thể tạo file cấu hình local không commit hoặc truyền giá trị qua biến môi trường/profile riêng. Tối thiểu cần cấu hình:

- Thông tin kết nối MySQL.
- SMTP username/password nếu dùng xác thực email.
- JWT secret.
- OpenRouteService API key nếu dùng geocoding hoặc directions.

### 4. Build và chạy

Windows PowerShell hoặc Command Prompt:

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
./gradlew clean build
./gradlew bootRun
```

Sau khi khởi động, truy cập:

- Trang chủ: [http://localhost:8080](http://localhost:8080)
- Đăng nhập: [http://localhost:8080/login](http://localhost:8080/login)
- Hướng dẫn API: xem các controller trong `src/main/java/vn/codegyme/meal_choice/controller`.

Có thể chạy file JAR sau khi build:

```bash
java -jar build/libs/meal-choice-0.0.1-SNAPSHOT.jar
```

## Cấu hình

Các cấu hình mặc định nằm trong `src/main/resources/application.properties`:

| Nhóm | Thuộc tính tiêu biểu |
|---|---|
| Ứng dụng | `spring.application.name`, `server.port` |
| MySQL | `spring.datasource.*` |
| JPA/Hibernate | `spring.jpa.*` |
| Email | `spring.mail.*` |
| JWT | `jwt.secret`, `jwt.access-token-expiration`, `jwt.refresh-token-expiration` |
| Kích hoạt tài khoản | `app.base-url`, `app.activation.expiration-minutes` |
| Bản đồ | `heigit.api-key`, `heigit.geocode-url`, `heigit.directions-url` |
| Thymeleaf | `spring.thymeleaf.*` |

Thời hạn mặc định hiện tại là 15 phút cho link kích hoạt, 15 phút cho access token và 7 ngày cho refresh token. Khi triển khai thật, nên tắt các cấu hình debug như SQL log, web/Thymeleaf DEBUG và không bật hiển thị stacktrace ra client.

## Các route chính

### Xác thực và khách hàng

| Method | Route | Mục đích |
|---|---|---|
| `POST` | `/api/auth/register` | Đăng ký tài khoản |
| `POST` | `/api/auth/login` | Đăng nhập |
| `POST` | `/api/auth/logout` | Đăng xuất |
| `GET` | `/api/user/profile` | Xem hồ sơ |
| `GET/POST/PUT/DELETE` | `/api/user/address...` | Quản lý địa chỉ |
| `POST` | `/api/checkout/place-order` | Tạo đơn hàng |
| `GET` | `/api/delivery/quotes` | Lấy báo giá giao hàng |

### Merchant

| Method | Route | Mục đích |
|---|---|---|
| `POST` | `/api/merchant/register` | Đăng ký Merchant |
| `GET` | `/api/merchant/my-status` | Xem trạng thái đăng ký |
| `GET/PUT` | `/api/merchant/{merchantId}/profile` | Xem/cập nhật hồ sơ |
| `GET/POST/PUT/DELETE` | `/api/merchant/{merchantId}/addresses...` | Quản lý địa chỉ |
| `GET/POST/PUT/DELETE` | `/api/merchant/foods...` | Quản lý món ăn |
| `GET` | `/api/merchant/orders` | Danh sách đơn hàng |
| `GET` | `/api/merchant/stats/revenue` | Thống kê doanh thu |
| `GET` | `/api/merchant/stats/foods` | Thống kê theo món |
| `GET` | `/api/merchant/stats/customers` | Thống kê theo khách |
| `GET/POST` | `/merchant/coupons` | Quản lý coupon |
| `GET/POST` | `/merchant/payout` | Đối soát và rút tiền |

### Admin

| Method | Route | Mục đích |
|---|---|---|
| `GET` | `/admin/dashboard` | Dashboard quản trị |
| `GET/POST` | `/admin/merchants` | Quản lý Merchant |
| `GET/POST` | `/admin/delivery-partners` | Quản lý đối tác vận chuyển |
| `GET/POST` | `/admin/payout-requests` | Xử lý yêu cầu payout |

Danh sách trên là các route tiêu biểu; request/response chi tiết và validation được định nghĩa trực tiếp trong controller, DTO và service tương ứng.

## Tài khoản và phân quyền

Hệ thống có ba nhóm người dùng nghiệp vụ chính:

- **USER/CUSTOMER**: mua món, quản lý địa chỉ và đơn hàng.
- **MERCHANT**: quản lý cửa hàng, món ăn, đơn hàng, coupon và doanh thu.
- **ADMIN**: kiểm duyệt Merchant, quản lý đối tác, payout và cấu hình hệ thống.

Một Merchant cần được Admin phê duyệt trước khi sử dụng đầy đủ các chức năng dành cho cửa hàng. Mọi endpoint cần đăng nhập phải được kiểm tra quyền ở tầng security và nghiệp vụ tương ứng.

## Quy trình làm việc nhóm

### Thành viên và vai trò

| Thành viên | Vai trò | Trách nhiệm |
|---|---|---|
| Lê Thị Ngọc Ánh | Leader / Điều hành | Kết nối nhóm và quản lý tiến độ chung |
| Bùi Việt Bắc | Technical Lead | Định hướng kỹ thuật, rà soát hệ thống và phân chia công việc |
| Đỗ Xuân Trường | Core Developer | Giải quyết bài toán kỹ thuật phức tạp và kiểm thử |
| Lữ Nguyễn Hải Triều | Core Developer | Phát triển tính năng và tối ưu hóa code chung |
| Nguyễn Văn Hưng | Developer | Phát triển chức năng theo phân công |
| Đồng Minh Đức | Developer | Phát triển chức năng theo phân công |

### Sprint và Git

- Mỗi Sprint kéo dài **1 tuần**.
- Thành viên cập nhật tiến độ bằng commit/push trên Git.
- Sau khi hoàn thiện và kiểm thử, code được merge trực tiếp vào nhánh `main` theo quy ước của nhóm.
- Mỗi thành viên cập nhật **Nhật ký dự án** hằng ngày bằng đường dẫn do nhóm thống nhất.
- Trước khi merge cần đồng bộ `main`, xử lý conflict và kiểm tra build/test tại máy local.

## Định nghĩa hoàn thành (DoD)

Một chức năng chỉ được xem là hoàn thành khi đáp ứng đầy đủ các tiêu chí sau:

- Đã test chéo giữa các thành viên.
- Chạy ổn định trên Chrome và Edge.
- Đã validate dữ liệu ở cả Client và Server bằng Spring Validation.
- Giao diện Thymeleaf/CSS tuân theo quy chuẩn chung, dữ liệu hiển thị rõ ràng.
- Đã refactor ít nhất một lần, ưu tiên tái sử dụng code.
- Merge thành công vào `main`, không còn xung đột Git.
- Đã cập nhật tiến độ vào Nhật ký dự án.

## Cấu trúc thư mục

```text
MealChoice-anh/
├── database/                 # Schema và dữ liệu mẫu
├── gradle/                   # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/vn/codegyme/meal_choice/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── static/       # CSS và JavaScript
│   │       ├── templates/    # Thymeleaf templates
│   │       └── application.properties
│   └── test/                 # Unit test và integration test
├── build.gradle
├── gradlew
└── gradlew.bat
```

## Kiểm thử

Chạy toàn bộ test:

```bash
./gradlew test
```

Trên Windows:

```powershell
.\gradlew.bat test
```

Build có kiểm thử:

```bash
./gradlew clean build
```

Sau khi chạy, báo cáo HTML nằm trong `build/reports/tests/test/index.html`.

Các luồng nên được kiểm thử tối thiểu:

- Đăng ký, kích hoạt email và đăng nhập.
- Phân quyền giữa Customer, Merchant và Admin.
- Validation request hợp lệ/không hợp lệ.
- CRUD món ăn và coupon.
- Tạo, cập nhật, hủy và tra cứu đơn hàng.
- Tính coupon, phí giao hàng, chiết khấu và tổng tiền.
- Payout, claim và các trạng thái Merchant.

## Đóng góp

1. Nhận task trong Sprint hiện tại và cập nhật Nhật ký dự án.
2. Tạo thay đổi nhỏ, bám theo cấu trúc controller-service-repository hiện có.
3. Chạy test và kiểm tra trên Chrome/Edge.
4. Refactor phần code liên quan, bổ sung validation và xử lý lỗi cần thiết.
5. Commit với nội dung rõ ràng, push lên Git.
6. Đồng bộ `main`, xử lý conflict và chỉ merge khi đạt DoD.

## Bản quyền và thông tin liên hệ

MealChoice là dự án học tập/phát triển nội bộ của nhóm. Thông tin thành viên, nhật ký dự án và quy ước đóng góp được quản lý theo tài liệu chung của nhóm.
