# MyTodoApp 📝

**MyTodoApp** là một ứng dụng quản lý công việc (To-Do List) mạnh mẽ và tinh tế được xây dựng bằng **Kotlin** và các thành phần **Android Jetpack** hiện đại. Ứng dụng giúp bạn tổ chức cuộc sống hàng ngày thông qua việc phân loại công việc theo thư mục, xem lịch trình và quản lý thùng rác thông minh.

## 🚀 Tính năng chính

- **Quản lý Thư mục (Folders):** Tạo và tùy chỉnh các thư mục với biểu tượng (icons) và màu sắc riêng biệt (Cá nhân, Học tập, Du lịch, Mua sắm...).
- **Quản lý Task & Subtask:** Thêm, sửa, xóa công việc chính và các đầu mục nhỏ (subtasks) đi kèm.
- **Soạn thảo văn bản phong phú (Rich Text):** Hỗ trợ định dạng Bold, Italic, Underline và màu sắc cho các Subtasks giúp ghi chú trực quan hơn.
- **Tiện ích màn hình chính (App Widget):** Xem và quản lý danh sách công việc ngay trên màn hình Home.
- **Tùy chỉnh linh hoạt:** Chọn màu sắc và icon cho thư mục thông qua giao diện chọn màu sắc hiện đại.
- **Chế độ xem Lịch (Calendar View):** Theo dõi công việc theo tuần và ngày với giao diện lịch trực quan.
- **Lọc thông minh:** Phân loại nhanh các công việc trong ngày (Today) hoặc sắp tới (Upcoming).
- **Thùng rác (Trash Bin):** Khôi phục hoặc xóa vĩnh viễn các thư mục và công việc đã xóa.
- **Giao diện hiện đại:** Thiết kế theo phong cách Material Design 3, hỗ trợ Dynamic Color và hiệu ứng mượt mà.

## 🛠 Tech Stack

- **Ngôn ngữ:** [Kotlin](https://kotlinlang.org/)
- **Cơ sở dữ liệu:** [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite với Coroutines & Flow)
- **Kiến trúc:** Clean Architecture (Repository Pattern), Fragment-based.
- **Navigation:** [Jetpack Navigation Component](https://developer.android.com/guide/navigation)
- **UI Components:**
    - RecyclerView (Giao diện danh sách lồng nhau)
    - ViewPager2 (Onboarding)
    - ConstraintLayout, NestedScrollView
    - Material Design 3
    - Splash Screen API (Android 12+)
- **Khác:** [Gson](https://github.com/google/gson), [KSP](https://google.github.io/ksp/) (xử lý annotation cho Room).

## 📂 Cấu trúc dự án

Dự án đã được tối ưu hóa cấu trúc để dễ dàng bảo trì:

- `adapter/`: Quản lý hiển thị danh sách (Folders, Tasks, Calendar, Widget...).
- `database/`: Cấu hình Room DB, DAO, Repository và TypeConverters.
- `fragment/`: Xử lý logic giao diện người dùng.
- `model/`: Các định nghĩa dữ liệu (Folder, Task, SubTask...).
- `view/`: Các Custom View (RichEditText...).
- `widget/`: Xử lý logic cho App Widget.
- `utils/`: Các hàm tiện ích bổ trợ.
- `res/`: Tài nguyên giao diện, màu sắc, và themes.
- 
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
