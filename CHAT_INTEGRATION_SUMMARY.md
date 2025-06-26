# Tích hợp Chat vào HomeScreenWrapper

## 🎯 Những thay đổi đã thực hiện

### 📱 **Bottom Navigation Updates**
- **Thay đổi tab "Thêm" → "Tin nhắn"** 
  - Icon: `Icons.Default.Add` → `Icons.Default.Message`
  - Route: `addservices_main` → `chat_main`
  - Chức năng: RegisterServiceScreen → ChatListScreen

- **Thay đổi tab "Tài khoản" → "Hồ sơ"**
  - Label: "Tài khoản" → "Hồ sơ"
  - Content: UserProfileScreen → UserProfileView (wrapper mới)

### 🚀 **Các Routes mới được thêm**

#### 1. **chat_main** - Danh sách chat
```kotlin
ChatListScreen(
    onChatClick = { chatId, otherUserId, otherUserName ->
        navController.navigate("chat_detail/$chatId/$otherUserId/$otherUserName")
    },
    onSearchClick = {
        navController.navigate("search_users")
    }
)
```

#### 2. **chat_detail/{chatId}/{otherUserId}/{otherUserName}** - Màn hình chat
```kotlin
ChatScreen(
    chatId = chatId,
    otherUserId = otherUserId, 
    otherUserName = otherUserName,
    onBackClick = { navController.popBackStack() }
)
```

#### 3. **search_users** - Tìm kiếm người dùng
```kotlin
SearchUsersScreen(
    onBackClick = { navController.popBackStack() },
    onUserClick = { user ->
        navController.navigate("chat_detail/new_${user.id}/${user.id}/${user.name}")
    }
)
```

#### 4. **avatar_change** - Thay đổi avatar
```kotlin
AvatarChangeScreen(
    onBackClick = { navController.popBackStack() },
    onAvatarSelected = { avatarUrl ->
        navController.popBackStack()
    }
)
```

### 📂 **Imports mới được thêm**
```kotlin
//import com.example.testappcc.presentation.userprofile.UserProfileView
//import com.example.testappcc.presentation.chat.ChatListScreen
//import com.example.testappcc.presentation.chat.ChatScreen
//import androidx.compose.material.icons.filled.Message
```

## 🗂️ **Cấu trúc Bottom Navigation hiện tại**

| Tab | Icon | Route | Chức năng |
|-----|------|-------|-----------|
| **Trang chủ** | `Home` | `home_main` | ProviderHomeScreen |
| **Dịch vụ** | `HomeRepairService` | `service_management` | ServiceManagementScreen |
| **Tin nhắn** ⭐ | `Message` | `chat_main` | ChatListScreen (MỚI) |
| **Lịch** | `DateRange` | `calendar_main` | TaskCalendarScreen |
| **Hồ sơ** ⭐ | `Person` | `profile_main` | UserProfileView (CẬP NHẬT) |

## 🔄 **Navigation Flow**

### Chat Flow:
1. **Tab "Tin nhắn"** → `ChatListScreen`
2. **Tap vào chat** → `ChatScreen` với parameters
3. **Nút tìm kiếm** → `SearchUsersScreen`
4. **Chọn user** → Tạo chat mới

### Profile Flow:
1. **Tab "Hồ sơ"** → `UserProfileView`
2. **Avatar click** → `AvatarChangeScreen`
3. **Chọn avatar** → Quay lại profile

## ✅ **Kết quả**

- ✅ Chat system đã được tích hợp hoàn toàn vào navigation
- ✅ Bottom tabs đã được cập nhật theo yêu cầu
- ✅ Tất cả màn hình chat có thể truy cập từ tab "Tin nhắn"
- ✅ Profile screen sử dụng wrapper mới với avatar change
- ✅ Navigation flows hoạt động mượt mà
- ✅ Back navigation được xử lý đúng

## 🚀 **Sẵn sàng sử dụng**

App bây giờ có đầy đủ tính năng chat tích hợp trong bottom navigation, người dùng có thể:
- Xem danh sách chat từ tab "Tin nhắn"
- Chat với người dùng khác
- Tìm kiếm người dùng mới để chat
- Thay đổi avatar từ tab "Hồ sơ"
- Navigate giữa các màn hình một cách tự nhiên 