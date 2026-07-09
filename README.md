# MyTodoApp 📝

**MyTodoApp** là một ứng dụng quản lý công việc (To-Do List) mạnh mẽ và tinh tế được xây dựng bằng **Kotlin** và các thành phần **Android Jetpack** hiện đại. Ứng dụng giúp bạn tổ chức cuộc sống hàng ngày thông qua việc phân loại công việc theo thư mục, xem lịch trình và quản lý thùng rác thông minh.

## 🚀 Tính năng chính

- **Quản lý Thư mục (Folders):** Tạo và tùy chỉnh các thư mục với biểu tượng (icons) và màu sắc riêng biệt (Cá nhân, Học tập, Du lịch, Mua sắm...).
- **Quản lý Task & Subtask:** Thêm, sửa, xóa công việc chính và các đầu mục nhỏ (subtasks) đi kèm.
- **Tùy chỉnh linh hoạt:** Chọn màu sắc và icon cho thư mục thông qua Bottom Sheet màu sắc.
- **Chế độ xem Lịch (Calendar View):** Theo dõi công việc theo tuần với giao diện lịch ngang trực quan.
- **Lọc thông minh:** Phân loại nhanh các công việc trong ngày (Today) hoặc sắp tới (Upcoming).
- **Thùng rác (Trash Bin):** Khôi phục hoặc xóa vĩnh viễn các thư mục, danh sách và công việc đã xóa.
- **Giao diện hiện đại:** Thiết kế theo phong cách Material Design 3, hỗ trợ hiệu ứng Ripple và giao diện sạch sẽ.

## 🛠 Tech Stack

- **Ngôn ngữ:** [Kotlin](https://kotlinlang.org/)
- **Cơ sở dữ liệu:** [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite với Coroutines & Flow)
- **Kiến trúc:** Repository Pattern, Fragment-based Architecture.
- **Navigation:** [Jetpack Navigation Component](https://developer.android.com/guide/navigation)
- **UI Components:**
    - RecyclerView (Nested RecyclerView cho các Task trong Folder)
    - ConstraintLayout, NestedScrollView
    - Material Design 3
    - Splash Screen API (Android 12+)
- **Dependency:** [Gson](https://github.com/google/gson) (xử lý dữ liệu JSON).

## 📂 Cấu trúc dự án

- `adapter/`: Các bộ điều hợp cho RecyclerView (Folder, Task, Calendar...).
- `database/`: Cấu hình Room DB, DAO, Repository và TypeConverters.
- `fragment/`: Xử lý giao diện người dùng (Home, Calendar, Add Task, Trash Bin...).
- `model/`: Các Data classes (Folder, Task, TrashItem...).
- `res/`: Chứa các tài nguyên XML cho giao diện, màu sắc, và themes.

## 📸 Screenshots
*(Bạn có thể thêm hình ảnh vào đây sau khi chụp màn hình ứng dụng)*

| Home Screen | Calendar View | Folder Trash |
| :---: | :---: | :---: |
| ![Home](https://via.placeholder.com/200x400) | ![Calendar](https://via.placeholder.com/200x400) | ![Trash](https://via.placeholder.com/200x400) |

## ⚙️ Cài đặt & Chạy thử

1. Clone repository này:
   ```bash
   git clone https://github.com/yourusername/MyTodoApp.git
   ```
2. Mở dự án trong **Android Studio**.
3. Chờ Gradle đồng bộ hóa các dependencies.
4. Chạy ứng dụng trên máy ảo hoặc thiết bị thật (Yêu cầu Android 8.0 trở lên).

---
*Phát triển bởi Ngô Khánh Hưng*
