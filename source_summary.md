# FridgeZero — Complete Source Code & Architecture Summary

> **Generated:** 2026-11-19 13:54:50 +07:00
> **Architecture:** Jetpack Compose + Room + WorkManager + CameraX 1.4.2 + Offline Three.js r128 WebGL Stage (Single Shared WebView Host at MainActivity Root + v3 Neo-Brutalist)
> **Excluded:** `local.properties` (machine-specific paths/sensitive config), binary files (`.apk`, `.jar`, `.ttf`, `.webp`), build outputs (`build/`, `.gradle/`), and third-party minified vendor bundle `app/src/main/assets/fridge3d/three.min.js` (603,451 bytes).

## 1. Directory Structure

```text
FridgeZero/
├── docs/spec.md
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/fridge3d/
        │   │   ├── index.html
        │   │   └── three.min.js                # [vendor UMD r128, omitted]
        │   ├── kotlin/com/truong/fridgezero/
        │   │   └── data/AppConfig.kt
        │   │   └── data/Dao.kt
        │   │   └── data/Db.kt
        │   │   └── data/Entities.kt
        │   │   └── data/Repository.kt
        │   │   └── data/Seed.kt
        │   │   └── domain/CulinaryGuide.kt
        │   │   └── domain/Freshness.kt
        │   │   └── domain/Models.kt
        │   │   └── domain/ShelfLife.kt
        │   │   └── MainActivity.kt
        │   │   └── notifications/ContextualNotificationPrompt.kt
        │   │   └── notifications/ExpiryReminderWorker.kt
        │   │   └── ui/aiscan/AiScanPlaceholderScreen.kt
        │   │   └── ui/bento/CompartmentBentoScreen.kt
        │   │   └── ui/bento/ItemSheets.kt
        │   │   └── ui/designer/FridgeDesignerScreen.kt
        │   │   └── ui/designer/FridgeLayoutEditor.kt
        │   │   └── ui/fridge3d/Fridge2DFallback.kt
        │   │   └── ui/fridge3d/Fridge3DView.kt
        │   │   └── ui/fridge3d/ShellColorSpec.kt
        │   │   └── ui/home/HomeDashboardHeader.kt
        │   │   └── ui/home/HomeScreen.kt
        │   │   └── ui/icons/FoodIcon.kt
        │   │   └── ui/recipes/CulinaryGuideScreen.kt
        │   │   └── ui/recipes/RecipeDetailSheet.kt
        │   │   └── ui/theme/BrutalistComponents.kt
        │   │   └── ui/theme/Color.kt
        │   │   └── ui/theme/Theme.kt
        │   │   └── ui/theme/Type.kt
        │   │   └── vm/FridgeViewModel.kt
        │   └── res/
        │       ├── font/                       # 7 bundled TTFs (Space Grotesk, Plus Jakarta Sans, JetBrains Mono)
        │       └── values/{strings.xml, themes.xml}
        └── test/kotlin/com/truong/fridgezero/
            └── domain/LodAndFreshnessTest.kt
```

## 2. File Contents

### `docs/spec.md` (210 lines)

````markdown
# FridgeZero — Đặc tả Kỹ thuật & UI/UX v3
**Kiến trúc:** Interactive 3D Hero (Offline Three.js WebGL Stage) + Neo-Brutalist 2-Column Bento Grid  
**Nền tảng:** Android (Kotlin, Jetpack Compose, Material 3, Room, WebView + Three.js Offline)  
**Trạng thái:** Chuẩn hóa từ bản mô phỏng tương tác HTML đã duyệt (`fridgezero_3d_bento.html`)

---

## §0. Nguyên tắc Cốt lõi & Ràng buộc Triển khai

1. **Tách đôi nhiệm vụ không gian (Giải quyết dứt điểm lỗi cắt chữ & xung đột cử chỉ):**
   - **Màn hình chính (`FridgeHeroScreen`) = 3D Hero tương tác:** Chỉ hiển thị khối tủ lạnh 3D nguyên khối xoay 360° kèm các thẻ sticker trạng thái bám trên từng cánh cửa. Không nhồi danh sách thực phẩm vào bên trong lòng tủ 3D.
   - **Màn hình ngăn (`CompartmentBentoScreen`) = Lưới Bento 2 cột phẳng:** Khi chạm vào một cánh cửa 3D, cửa bật mở 3D (`420ms`) rồi chuyển sang màn hình phẳng 2D dùng `LazyVerticalGrid(GridCells.Fixed(2))` để quản lý thực phẩm rộng rãi, không bao giờ cắt chữ hay tràn khung.
2. **Ngôn ngữ thị giác Gen-Z Neo-Brutalism:**
   - Nền giấy kem ấm `#EAE2D3` phủ lưới chấm `Dot Grid`, bề mặt thẻ trắng tinh `#FFFFFF`, viền đen mực đậm `2.5.dp` (`#111111`), bóng đổ cứng lệch góc `4.dp × 4.dp` (`0.dp` blur), kết hợp bảng màu pastel độ tương phản cao (`#A3E635`, `#FACC15`, `#FB7185`, `#C084FC`, `#38BDF8`).
3. **Kế thừa nguyên vẹn tầng dữ liệu từ `spec_v2`:**
   - Giữ nguyên 3 bảng Room Database (`FridgeEntity`, `CompartmentEntity`, `FoodItemEntity`), trạng thái lịch sử lãng phí (`ACTIVE`, `USED_UP`, `DISCARDED`), cấu trúc tra cứu `ShelfLife` và lịch nhắc hạn dùng `WorkManager`.
4. **Quy tắc dữ liệu & tài nguyên:**
   - Số liệu thời hạn bảo quản `ShelfLife` sử dụng cận dưới bảo thủ theo chuẩn USDA FoodKeeper / FDA (cho 9 `FoodGroup` và 32 món trong `EmojiIconRepository`) để tự điền thông minh theo vùng ngăn, kèm khuyến nghị người dùng kiểm tra thêm bao bì.
   - Toàn bộ chuỗi hiển thị tiếng Việt đặt tập trung trong `res/values/strings.xml`.
   - Xin quyền `POST_NOTIFICATIONS` (Android 13+) và `CAMERA` (Giai đoạn 2) đúng ngữ cảnh khi người dùng kích hoạt tính năng tương ứng.

---

## §1. Hệ thống Thiết kế & Tài nguyên Thị giác (Design System)

### 1.1. Bảng màu Neo-Brutalist (`ui/theme/Color.kt`)
| Token | Mã Hex | Vai trò & Ngữ cảnh sử dụng |
| :--- | :--- | :--- |
| `PaperCream` | `#EAE2D3` | Màu nền chính của toàn bộ ứng dụng và Bottom Sheet. |
| `DotGridInk` | `#C4B8A3` | Màu chấm lưới nền (bán kính `1.5.dp`, bước lưới `18.dp`). |
| `SurfaceWhite` | `#FFFFFF` | Nền thẻ Bento, nút phụ, khung xem trước 3D và thanh Task Bar. |
| `InkBlack` | `#111111` | Viền khối (`2.5.dp`), viền chip (`2.dp`), bóng đổ cứng và chữ chính. |
| `LimeFresh` | `#A3E635` | Trạng thái tươi (`≥ 2` ngày), nút CTA chính (`CHỐT TỦ NÀY`), FAB `+ Thêm nhanh`. |
| `LemonWarn` | `#FACC15` | Trạng thái sắp hết hạn (`1` ngày — `Freshness.SOON`), badge bước thiết kế, nút nhắc hạn. |
| `CoralUrgent` | `#FB7185` | Trạng thái gấp ($\le 0$ ngày — hết hạn hôm nay hoặc quá hạn), nút xóa ngăn/món, cảnh báo khẩn. |
| `LilacAccent` | `#C084FC` | Badge ngăn đông, nút `📷 Quét AI (GĐ2)` trên thanh Task Bar. |
| `SkyInfo` | `#38BDF8` | Huy hiệu thời tiết, badge ngăn kéo (`DRAWER`), chọn ngăn cất đồ. |
| `CavitySlate` | `#1E293B` | Màu lòng khoang bên trong tủ 3D khi cửa mở. |

### 1.2. Hệ thống Chữ (`ui/theme/Type.kt`)
- **Display Font (`Space Grotesk` — Bold 700 / ExtraBold 800):** Tiêu đề màn hình (`THIẾT KẾ TỦ LẠNH`, `FRIDGEZERO`, tên ngăn viết hoa), số đếm lớn.
- **Body Font (`Plus Jakarta Sans` — Medium 500 / SemiBold 600 / Bold 700 / ExtraBold 800):** Tên thực phẩm trên thẻ Bento, lời chào dashboard, nhãn nút và chip lọc.
- **Monospace Font (`JetBrains Mono` — SemiBold 600 / Bold 700 / ExtraBold 800):** Đồng hồ số thời gian thực (`14:05 · T4, 18/11`), nhiệt độ thời tiết (`28°C`), sticker hạn dùng (`Còn 2d`, `Quá hạn 1d`), huy hiệu `GĐ2`.

### 1.3. Thành phần UI Dùng chung (`ui/theme/BrutalistComponents.kt`)
- **`BrutalistCard`:** Bo góc `16.dp`, nền `#FFFFFF`, viền `2.5.dp` `#111111`, bóng cứng dịch chuyển `(4.dp, 4.dp)` màu `#111111` không làm mờ.
- **`BrutalistButton` & `BrutalistChip`:** Viền `2.dp–2.5.dp` `#111111`, bóng cứng `2.dp–4.dp`; khi nhấn (`isPressed`) dịch chuyển tọa độ `(+2.dp, +2.dp)` và thu bóng còn `(2.dp, 2.dp)` kèm phản hồi rung nhẹ (`HapticFeedbackType.TextHandleMove`).
- **`TiltedStickerBadge`:** Nhãn dán xoay góc `-2°` đến `+2°`, viền `2.dp` `#111111`, bóng cứng `(2.dp, 2.dp)`.

### 1.4. Tài nguyên 4 Màu vỏ tủ 3D (`ShellColorSpec`)
| `colorKey` | Tên hiển thị | Màu Hex | `roughness` | `metalness` |
| :--- | :--- | :--- | :--- | :--- |
| `inox` | Bạc Inox | `#CBD5E1` | `0.34f` | `0.42f` |
| `butter` | Vàng Bơ | `#FEF08A` | `0.38f` | `0.12f` |
| `mint` | Xanh Mint | `#A7F3D0` | `0.38f` | `0.12f` |
| `matte` | Đen Nhám | `#27272A` | `0.56f` | `0.24f` |

---

## §2. Kiến trúc Rendering 3D & Tương tác Không gian (`app/src/main/assets/fridge3d/` & `ui/fridge3d/`)

### 2.1. Công nghệ & Thư viện
- Sử dụng **Offline Three.js WebGL Stage** đóng gói cục bộ trong `app/src/main/assets/fridge3d/` (`three.min.js` UMD + `index.html`), nhúng trong một `WebView` dùng chung duy nhất ở tầng root `MainActivity` (`SharedFridge3DWebView` + `Fridge3DStageSlot` với `AndroidView`).
- **Đồng bộ nền không đường nối (`DotGridSpec` Single Source of Truth):** `WebGLRenderer` dùng màu xóa nền đặc (**opaque `alpha: false`**, `renderer.setClearColor(surfaceBg, 1)`) khớp màu `surfaceBg`; trên Màn 2 vẽ kết cấu lưới chấm `CanvasTexture` sinh từ đúng hằng số `DotGridSpec` (`#EAE2D3`, `#C4B8A3`, `18.dp`, `1.5.dp`) và đồng bộ lệch pha UV (`offsetXDp`, `offsetYDp`) theo tọa độ khung `Fridge3DStageSlot` để hòa liền mạch với `Modifier.dotGridBackground()` của Compose.
- **Tối ưu hiệu năng, Bảo mật & Phục hồi sự cố:**
  - **Render theo nhu cầu (`dirty`-flag):** Vòng `requestAnimationFrame` chỉ chạy `renderer.render(...)` và `onStickersProjected` khi có tương tác/hoạt ảnh đang chạy; vẽ đúng một frame nghỉ cuối cùng khi dừng rồi ngưng hẳn đến tương tác tiếp theo.
  - **Chế độ `lite` vs `high` (`?q=lite` / `?q=high`):** Tự chọn `lite` khi `ActivityManager.isLowRamDevice`, RAM $\le 4\text{ GiB}$, hoặc `AppConfig.forceLiteMode == true` (tắt antialias, giới hạn DPR $\le 1.5$, dùng `MeshStandardMaterial` không clearcoat và vẫn giữ viền mực `EdgesGeometry`).
  - **Bảo mật & Lifecycle:** Tạm dừng/khôi phục WebView ở `ON_STOP`/`ON_START`, tắt `allowFileAccess*`/`allowContentAccess`, chỉ cho phép điều hướng cục bộ `file:///android_asset/fridge3d/` hoặc `about:blank`, bật WebView debugging theo `FLAG_DEBUGGABLE`, và chặn rủi ro mất context/crash qua bộ đếm trượt tối đa **2 lần dựng lại trong 60 giây** (`onRenderProcessGone` + `onWebGlContextLost`) trước khi chuyển gọn sang giao diện dự phòng **`Fridge2DFallback`** Neo-Brutalist thuần Compose.

### 2.2. Dựng hình Tủ lạnh 3D Tham số (`assets/fridge3d/index.html`)
Chiếc tủ 3D được lắp ráp động từ cấu hình `FridgeEntity` và danh sách `CompartmentEntity` (tọa độ chuẩn hóa `x, y, w, h` trong khoảng `0..1`):
1. **Vỏ tủ rỗng (`Hollow Cabinet Shell`):**
   - Khối tủ bo góc tỷ lệ `1.15 : 2.25 : 1.0` ghim tâm xoay dưới chân đế tại mặt sàn `y = 0`, gồm 5 tấm bo góc (lưng, vách trái, vách phải, nắp trên, đáy) + viền nét mực `EdgesGeometry` `#111111`.
   - Bên trong gắn tấm hậu khoang tối `#1E293B`, thanh đèn LED trần phát sáng `#E0F2FE`, 4 chân đế trụ tròn `#111111` và đĩa bóng đổ mềm trên mặt sàn dưới `y = 0`.
2. **Nội thất bên trong (`Interior Shelves & Props`):**
   - Vẽ các tấm khay kính cường lực bán trong suốt (`#BAE6FD`, `opacity = 0.55`) theo `compartment.shelves`.
   - Đặt các khối prop thực phẩm 3D bo góc nhiều màu (`#A3E635`, `#FACC15`, `#FB7185`, `#C084FC`) trên khay kính để khi cánh cửa mở ra tạo chiều sâu sinh động.
3. **Cánh cửa & Ngăn kéo tách Node (`Door & Drawer Pivot Nodes`):**
   - **`CompartmentKind.DOOR` (Cánh mở):**
     - Mặc định đặt trục bản lề ở **cạnh trái** cánh cửa, tay nắm dọc bo tròn ở cạnh phải; khi mở xoay quanh trục Y một góc `-85°` hướng về phía người dùng.
     - Riêng cánh nằm bên nửa phải của mẫu chia đôi (`Side-by-Side`, `French Door` với `w < 0.6f && x > 0.4f`) đặt trục bản lề ở **cạnh phải**, tay nắm ở cạnh trái trong; khi mở xoay góc `+85°`.
   - **`CompartmentKind.DRAWER` (Ngăn kéo):**
     - Tay nắm ngang bo tròn đặt phía trên mặt ngăn; khi mở trượt tịnh tiến dọc trục `+Z` hướng về phía trước.
   - Gắn 2 viên nam châm 3D trang trí (`#FACC15` tròn và `#FB7185` chữ nhật bo góc) ở góc trên bên phải cánh cửa trên cùng (`index == 0`).

### 2.3. Cử chỉ Xoay Bàn Xoay 360°, Raycast & Chiếu Sticker 2D (`Fridge3DView.kt`)
- **Bàn xoay 1 trục chính (`yaw` 360° + micro-pitch):** Vuốt ngang xoay `yaw` 360° quanh trục Y với quán tính mượt (`0.92` damping); vuốt dọc nghiêng nhẹ `pitch` giới hạn chặt trong `[-6°, +4°]` và tự đàn hồi về `-4°` khi nhả tay để tủ luôn đứng vững trên sàn. Góc mặc định: `yaw = -16°`, `pitch = -4°`; hỗ trợ `playWelcomeSpin()` (`360°` trong `1000ms`, dừng ngay nếu người dùng chạm vào).
- **Chiếu Sticker bám cửa (`3D-to-2D Screen Projection`):**
  - Mỗi ngăn gắn điểm neo ngay trước tâm cửa 3D, chiếu ra tọa độ chuẩn hóa `(x, y, visible)` gửi qua `AndroidBridge.onStickersProjected(json)` để Compose vẽ `StickerBadge` gốc.
  - Khi tủ xoay quá `±82°` ra mặt lưng, `visible = false` để sticker tự ẩn và tắt nhận chạm, không đè lên lưng tủ.
- **Chạm mở ngăn (`THREE.Raycaster` Tap-to-Open Flow):**
  - Nếu cú chạm dứt khoát (`drag < 10px`) trúng mesh cánh cửa (qua `THREE.Raycaster`) hoặc chạm trực tiếp vào Sticker:
    1. Chạy animation mở cánh cửa (`-85°` / `+85°`) hoặc trượt ngăn kéo trong `~380ms` và gọi `AndroidBridge.onDoorTapped(compartmentId)`.
    2. Chuyển sang `CompartmentBentoScreen` của ngăn vừa chọn và đóng cánh cửa 3D mượt khi quay về Màn 2.

---

## §3. Luồng Trải nghiệm 4 Màn hình & Các Bottom Sheet

### 3.1. Màn 1 — Thiết kế & Chốt tủ (`ui/setup/FridgeSetupScreen.kt`)
Hiển thị khi người dùng mở app lần đầu (hoặc bấm nút `⚙️ Đổi dáng tủ` từ Màn 2):
- **Top Bar:** Tiêu đề `THIẾT KẾ TỦ LẠNH`, phụ đề *"Chọn mẫu & chỉnh ngăn giống tủ nhà bạn"* và sticker `BƯỚC 1/2` màu `#FACC15`.
- **Khung Xem trước 3D trực tiếp (~43% chiều cao màn hình):**
  - Khung thẻ Neo-Brutalist chứa sân khấu 3D xoay 360° thời gian thực, góc trái có pill `↻ Vuốt xoay 3D`, góc phải có pill tên mẫu đang chọn.
- **Bảng Tùy chỉnh 3 bước phía dưới (cuộn dọc mượt):**
  1. **Mẫu tủ thông dụng (6 mẫu chuẩn cuộn ngang):** `1 cánh`, `2 cánh trên`, `2 cánh dưới`, `Side-by-Side`, `French Door`, `Tủ Mini`. Chọn mẫu nào lập tức dựng lại khối 3D và danh sách ngăn tương ứng.
  2. **Màu vỏ tủ (Lưới 4 cột đều nhau):** `Bạc Inox`, `Vàng Bơ`, `Xanh Mint`, `Đen Nhám` — đổi vật liệu PBR 3D ngay lập tức.
  3. **Tùy chỉnh ngăn — Thêm / Bớt (Tối đa 6 ngăn):**
     - Mỗi dòng ngăn gồm: Số thứ tự, ô nhập sửa tên ngăn trực tiếp, nút đổi loại **`Cánh mở` (`#C084FC`)** $\leftrightarrow$ **`Ngăn kéo` (`#38BDF8`)**, và nút xóa **`×` (`#FB7185`)** (khóa khi chỉ còn 1 ngăn).
     - Bên dưới có 2 nút ngang bằng nhau: **`+ Thêm cửa mở`** và **`+ Thêm ngăn kéo`**.
     - Thuật toán `normalizeCompartmentLayout()` tự động tính lại tọa độ chuẩn hóa `x, y, w, h` không bao giờ chồng lấn mỗi khi thêm/bớt ngăn.
- **Nút hành động đáy:** Nút toàn chiều rộng màu `#A3E635`: **`CHỐT TỦ NÀY →`** (lưu cấu hình vào Room và chuyển sang Màn 2).

### 3.2. Màn 2 — Màn hình chính Tủ 3D Tương tác (`ui/home/HomeScreen.kt`, `ui/home/HomeDashboardHeader.kt`)
- **Dashboard Đầu trang gọn gàng (`HomeDashboardHeader`):**
  - **Hàng tiêu đề:**
    - Trái: Nhãn thương hiệu nhỏ `FRIDGEZERO · 28°C ĐÀ NẴNG` + **Lời chào thân thiện động theo khung giờ** (*"Chào buổi sáng / trưa / chiều / tối, [Tên]!"*).
    - Phải: **Đồng hồ số thời gian thực** định dạng `HH:mm · Thứ, dd/MM` (`JetBrains Mono`, nền `#111111`, chữ trắng, bóng cứng `#A3E635`).
  - **Banner Trợ lý & Gợi ý Món ăn Trung tâm:**
    - Thẻ ngang nổi bật viền `2.5.dp` `#111111`: khi có đồ gấp ($\le 1$ ngày) hiển thị `🔔 N món sắp hết hạn` (`#FB7185` nếu có món hết hạn hôm nay/quá hạn $\le 0$ ngày, `#FACC15` nếu còn $1$ ngày) kèm pill `Danh sách` mở `UrgentItemsBottomSheet`, và dải gợi ý món điểm cao nhất (`🍳 Tên món · Xp →`) chạm vào đi thẳng tới `Recipes(topRecommendation.recipe.id)`.
    - Khi tủ đồ đều tươi (`#A3E635`), banner dẫn trực tiếp sang món gợi ý nấu ngay kèm nút chuông nhắc hạn dùng nhỏ gọn bên cạnh.
- **Sân khấu Trung tâm:**
  - Hiển thị khối tủ 3D (camera `y=1.32, z=4.72` tạo khoảng thở thoáng đãng trên lưới chấm `#EAE2D3`) cùng các `DoorStickerBadge` cỡ lớn dễ đọc bám trên cửa tủ (`padding(top = 6.dp, bottom = 8.dp)`), lược bỏ pill hướng dẫn tĩnh phía trên để giải phóng không gian.
  - **Không đặt nút đổi màu vỏ tủ ở màn hình này** (đã cố định từ Màn 1).
- **Nút nổi (`FAB`) `+ Thêm nhanh`:**
  - Nổi bật ở góc dưới bên phải ngay phía trên thanh Task Bar (`bottom = 14.dp`, `end = 16.dp` trong khung sân khấu), nền `#A3E635`, icon `+` trong ô vuông `#111111`, mở `QuickAddSheet`.
- **Thanh Task Bar cố định dưới đáy (`height = 74.dp`, 4 nút cân đối cho màn `360dp`, nền `#FFFFFF`, viền trên `2.5.dp` `#111111`):**
  1. **`🧊 Tủ lạnh` (nền `#FEF9C3`):** Căn lại góc nhìn chuẩn của tủ 3D.
  2. **`🍳 Cẩm nang` (nền `#FFFFFF` + sticker nghiêng `#A3E635` đếm số món sẵn sàng nấu):** Mở Màn 4 (`CulinaryGuideScreen`).
  3. **`📷 Quét AI` (nổi bật nền `#C084FC` + sticker nghiêng `GĐ2`):** Điểm vào luồng CameraX quét hóa đơn AI. Ở GĐ1 mở `CameraAiPreviewSheet` (đã khai báo sẵn quyền/giao diện khung ngắm để sẵn sàng nối API OCR ở GĐ2).
  4. **`⚙️ Đổi dáng` (nền `#FFFFFF`):** Quay lại Màn 1 (`FridgeDesignerScreen`) để chỉnh sửa dáng tủ, màu vỏ hoặc số ngăn.

### 3.3. Màn 3 — Khoang Thực phẩm Bento Grid 2 Cột (`ui/bento/CompartmentBentoScreen.kt`)
- **Top Bar:**
  - Trái: Nút quay lại **`← Tủ 3D`**.
  - Giữa: Tên ngăn viết hoa (`NGĂN MÁT`) + sticker tổng số món (`7 MÓN`, nền `#FACC15`).
  - Phải: Nút **`+ Thêm`** (`#A3E635`) mở `QuickAddSheet` gắn sẵn ngăn hiện tại.
- **Thanh chuyển ngăn nhanh (`LazyRow`):** Các chip tên từng ngăn trong tủ kèm số món để chuyển qua lại giữa các ngăn mà không cần thoát ra tủ 3D.
- **Dải chip lọc (`LazyRow`):** `Tất cả`, `Sắp hết hạn`, `Rau củ`, `Thịt cá`, `Sữa trứng`, `Đồ uống`.
- **Lưới thẻ Bento 2 cột (`LazyVerticalGrid(GridCells.Fixed(2))`, khoảng cách `12.dp`):**
  - Sắp xếp ưu tiên món sắp hết hạn lên đầu (`expiryEpochDay ASC`).
  - **Cấu trúc từng thẻ Bento (`minHeight = 158.dp`):**
    - *Hàng trên:* Khung icon `48.dp` bo góc chứa Emoji/Icon `32.dp` bên trái + Sticker hạn dùng nghiêng `-2°` bên phải (`Quá hạn Nd` / `Hôm nay` nền `#FB7185`, `Còn 1d` nền `#FACC15`, `≥ 2d` nền `#A3E635`).
    - *Phần giữa:* Tên thực phẩm tối đa 2 dòng (`maxLines = 2`, không cắt chữ giữa từ) + tag nhóm thực phẩm (`item.group.labelVi`).
    - *Hàng đáy:* Bộ đếm nhanh **`−` `[qty]` `+`** ngay trên mặt thẻ; bấm `−` khi số lượng $\le 1$ sẽ xóa trực tiếp món khỏi ngăn chỉ với 1 chạm (gọi `onMarkStatus(itemIds, ItemStatus.USED_UP)`) mà không mở hộp thoại xác nhận.

### 3.4. Hệ thống 4 Bottom Sheet (`ui/sheets/`)
1. **`ItemDetailSheet` (Chi tiết, Sửa, Chuyển ngăn & Xóa nhanh 1 chạm):**
   - Hiển thị icon lớn, tên món, nhóm, badge hạn dùng.
   - Bộ đếm số lượng `−` / `+`, chỉnh sửa tên và số ngày hạn dùng.
   - **Chuyển ngăn bảo quản:** Danh sách chip các ngăn khác trong tủ; khi chuyển giữa ngăn mát và ngăn đông sẽ tự điều chỉnh hạn dùng phù hợp.
   - **Xóa món 1 chạm:** Bấm `🗑️ Xóa khỏi tủ` sẽ ghi nhận `ItemStatus.USED_UP` và đóng sheet ngay lập tức.
2. **`QuickAddSheet` (Thêm nhanh thực phẩm):**
   - Hàng chip gợi ý món quen chạm 1 lần điền sẵn icon, tên, nhóm, chọn ngăn phù hợp và tự động điền số ngày hạn dùng theo chuẩn USDA/FDA (tự cập nhật khi đổi ngăn nếu người dùng chưa chọn mốc hạn thủ công, kèm chú thích `✓ Gợi ý theo chuẩn USDA/FDA (<vùng ngăn>)`).
   - Ô nhập tên thực phẩm + dải chip chọn ngăn cất + bộ đếm số lượng + 4 chip hạn dùng nhanh (`+3 ngày`, `+1 tuần`, `+2 tuần`, `+1 tháng`) kèm DatePicker tùy chọn.
3. **`UrgentItemsBottomSheet` (Trung tâm cảnh báo sắp hết hạn & Gợi ý món):**
   - Mở khi chạm Banner Trợ lý trên Dashboard Màn 2; hiển thị thẻ gợi ý **`🍳 TỐI NAY NẤU GÌ?`** nối thẳng với `topRecommendation` (chạm để mở thẳng `RecipeDetailSheet` tương ứng ở Màn 4), liệt kê các món sắp hết hạn kèm nút xóa nhanh **`✓ Dùng hết`** (1 chạm) và nút **`Mở ngăn →`** nhảy thẳng tới đúng ngăn đó ở Màn 3.
4. **`CameraAiPreviewSheet` (Cổng CameraX Quét Hóa đơn — Giai đoạn 2):**
   - Mở khi chạm `📷 Quét AI` trên Task Bar; hiển thị khung ngắm CameraX với 4 góc định vị hóa đơn, tia quét laser xanh `#A3E635`, nút đèn Flash, nút chụp và chọn ảnh thư viện (sẵn sàng nối luồng AI OCR ở GĐ2).

### 3.5. Màn 4 — Cẩm nang Ẩm thực & Giải cứu Hạn dùng (`ui/recipes/CulinaryGuideScreen.kt`, `RecipeDetailSheet.kt`)
- **Top Bar:** Nút quay lại `←`, tiêu đề `CẨM NANG ẨM THỰC` và sticker `#A3E635` hiển thị số món đủ đồ nấu ngay.
- **Lưới công thức Bento 2 cột (`LazyVerticalGrid`):**
  - Thẻ Hero toàn chiều rộng **`🔥 MÓN CỨU TỦ HÔM NAY`** làm nổi bật công thức có điểm giải cứu cao nhất.
  - Hàng chip lọc ngang (`Tất cả`, `Đủ đồ nấu ngay`, `Cứu đồ gấp`, `Nhanh ≤ 15p`, `Cơm nhà Việt`, `Eat-Clean`).
  - Thẻ Bento 2 cột hiển thị emoji món, tỷ lệ khớp nguyên liệu (`#A3E635` khi đủ đồ, `#FACC15` khi còn thiếu), hàng emoji nguyên liệu tô màu theo hạn dùng, thời gian nấu và `sourceName`. Tự động mở `RecipeDetailSheet` khi điều hướng kèm `initialRecipeId`.
- **`RecipeDetailSheet`:**
  - Hiển thị tiêu đề, thời gian nấu, độ khó (`Dễ` / `Vừa`) và nhãn nguồn uy tín; phân tách rõ **`✓ Đã có trong tủ`** (tô màu `#FB7185` / `#FACC15` cho món sắp hết hạn), **`🛒 Cần chuẩn bị thêm`** và **`🧂 Gia vị bếp cơ bản`**.
  - Liệt kê các bước nấu đánh số trong `NeoBrutalistCard` và 2 nút đáy: **`🔗 Xem bài gốc`** (`LocalUriHandler.openUri` bọc `runCatching`) và **`🎉 Đã nấu xong`** (gọi `FridgeViewModel.consumeRecipeIngredients` để trừ `unitsToConsume` / chuyển `USED_UP` trong Room, hiện Toast và đóng sheet).

---

## §4. Tầng Dữ liệu Room & Logic Nghiệp vụ (`data/`, `domain/`)

### 4.1. Mô hình 3 bảng Room Database
- **`FridgeEntity`:** `id`, `name`, `template` (`single`, `top_freezer`, `bottom_freezer`, `side_by_side`, `french_door`, `mini`), `colorKey` (`inox`, `butter`, `mint`, `matte`), `isLockedIn: Boolean`.
- **`CompartmentEntity`:** `id`, `fridgeId`, `name`, `kind` (`DOOR`, `DRAWER`), `x`, `y`, `w`, `h` (`0f..1f`), `row`, `col`, `sort`.
- **`FoodItemEntity`:** `id`, `compartmentId`, `name`, `iconKey`, `group`, `quantity`, `unit`, `addedAtEpochDay`, `expiryEpochDay`, `isOpened: Boolean`, `price: Long?`, `status: ItemStatus` (`ACTIVE`, `USED_UP`, `DISCARDED`), `source: String` (`"manual"`, `"ai_receipt"`).

### 4.2. Logic Nghiệp vụ & Thông báo
- **Không tự xóa món quá hạn:** Món có `daysLeft < 0` vẫn hiển thị ở đầu lưới Bento với sticker màu `#FB7185` ghi rõ *"Quá hạn N ngày"* để người dùng tự xử lý và ghi nhận lý do.
- **Bảng `ConfigurableShelfLifeRepository`:** Nạp sẵn quy tắc tra cứu bảo thủ theo chuẩn USDA FoodKeeper / FDA (9 `FoodGroup` và 32 `iconKey` trong `EmojiIconRepository`) theo vùng (`CHILLED`, `FREEZER`, `DRAWER`), hỗ trợ `isOpened: Boolean = false`.
- **Nhắc hạn dùng (`ExpiryReminderWorker`):** Chạy kiểm tra định kỳ hằng ngày qua `WorkManager`; chỉ yêu cầu quyền `POST_NOTIFICATIONS` (Android 13+) khi người dùng lưu món có hạn ngắn hoặc bật nhắc nhở.

---

## §5. Cấu trúc Package Kotlin & Kiến trúc 3D / Triển khai

```text
app/src/main/
├── assets/fridge3d/    # three.min.js (r128 offline) + index.html (Three.js WebGL stage)
└── kotlin/com/truong/fridgezero/
    ├── ui/
    │   ├── theme/      # Color.kt (kèm DotGridSpec), Type.kt, Theme.kt, BrutalistComponents.kt
    │   ├── fridge3d/   # Fridge3DView.kt, Fridge2DFallback.kt, ShellColorSpec.kt
    │   ├── designer/   # FridgeDesignerScreen.kt, FridgeLayoutEditor.kt (Màn 1)
    │   ├── home/       # HomeScreen.kt, HomeDashboardHeader.kt (Màn 2)
    │   ├── bento/      # CompartmentBentoScreen.kt, ItemSheets.kt (Màn 3)
    │   ├── recipes/    # CulinaryGuideScreen.kt, RecipeDetailSheet.kt (Màn 4)
    │   └── aiscan/     # AiScanPlaceholderScreen.kt (Cổng CameraX GĐ2)
    ├── data/           # Entities.kt, Dao.kt, Db.kt, Repository.kt, Seed.kt, AppConfig.kt
    ├── domain/         # Freshness.kt, ShelfLife.kt, Models.kt, CulinaryGuide.kt
    ├── notifications/  # ExpiryReminderWorker.kt, ContextualNotificationPrompt.kt
    └── vm/             # FridgeViewModel.kt
```

### Tổng quan Kiến trúc Shared WebView + Fallback 2D
1. **Shared Root WebView (`SharedFridge3DWebView` + `Fridge3DStageSlot`):** Một thực thể `WebView + Three.js` duy nhất gắn tại `MainActivity` đồng bộ bố cục theo khung đo của Màn 1 (`FridgeDesignerScreen`) và Màn 2 (`HomeScreen`), nối với Kotlin qua `FridgeJsBridge` (có giữ rule R8/ProGuard trong `proguard-rules.pro`).
2. **Nền & Hạn mức Đồ họa thống nhất:** `DotGridSpec` cung cấp đồng thời tham số lưới chấm cho cả Compose (`dotGridBackground`) và Three.js (`configJson.dotGrid` kèm `surfaceBg`), hỗ trợ `dirty`-flag render theo nhu cầu và tự hạ xuống `?q=lite` trên máy cấu hình thấp hoặc sau sự cố WebGL đầu tiên (`AppConfig.forceLiteMode`).
3. **Phục hồi có giới hạn & `Fridge2DFallback`:** Giới hạn tối đa **2 lần khởi tạo lại WebView trong 60 giây** khi gặp `onRenderProcessGone` hoặc `webglcontextlost`; lần thứ 3 trong cửa sổ 60s sẽ dừng WebView và hiển thị `Fridge2DFallback` Neo-Brutalist thuần Compose để toàn bộ thao tác mở ngăn/thiết kế tủ vẫn hoạt động bình thường.
````

### `settings.gradle.kts` (36 lines)

```kotlin
pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("androidx.*")
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google {
      content {
        includeGroupByRegex("androidx.*")
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
      }
    }
    mavenCentral()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "FridgeZero"

include(":app")
```

### `build.gradle.kts` (7 lines)

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.ksp) apply false
}
```

### `gradle.properties` (30 lines)

```properties
# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
# Enables Gradle Build Cache.
# See https://docs.gradle.org/current/userguide/build_cache.html
org.gradle.caching=true
# Enables Gradle Configuration Cache, the preferred Gradle execution mode.
# See https://docs.gradle.org/current/userguide/configuration_cache.html
org.gradle.configuration-cache=true
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. More details, visit
# http://www.gradle.org/docs/current/userguide/multi_project_builds.html#sec:decoupled_projects
# org.gradle.parallel=true
# AndroidX package structure to make it clearer which packages are bundled with the
# Android operating system, and which are packaged with your app"s APK
# https://developer.android.com/topic/libraries/support-library/androidx-rn
android.useAndroidX=true
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
# Enables namespacing of each library's R class so that its R class includes only the
# resources declared in the library itself and none from the library's dependencies,
# thereby reducing the size of the R class for that library
android.nonTransitiveRClass=true
```

### `gradle/libs.versions.toml` (53 lines)

```toml
[versions]
androidGradlePlugin = "9.0.1"
androidxCore = "1.18.0"
androidxLifecycle = "2.10.0"
androidxActivity = "1.13.0"
androidxComposeBom = "2026.03.01"
androidxTest = "1.7.0"
androidxTestExt = "1.3.0"
androidxTestRunner = "1.7.0"
androidxTestEspresso = "3.7.0"
coroutines = "1.10.2"
datastore = "1.1.7"
junit = "4.13.2"
kotlin = "2.3.20"
ksp = "2.3.6"
room = "2.7.1"
work = "2.10.0"
camerax = "1.4.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidxCore" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidxActivity" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidxComposeBom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "androidxLifecycle" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
androidx-camera-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
androidx-camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTest" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExt" }
androidx-test-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "androidxTestEspresso" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }
junit = { module = "junit:junit", version.ref = "junit" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "androidGradlePlugin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### `app/build.gradle.kts` (86 lines)

```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.truong.fridgezero"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.truong.fridgezero"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

base {
  archivesName.set("FridgeZero")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
```

### `app/src/main/AndroidManifest.xml` (27 lines)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:windowSoftInputMode="adjustResize"
        android:theme="@style/Theme.FridgeZero">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### `app/src/main/res/values/strings.xml` (196 lines)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FridgeZero</string>
    <string name="days_left_fmt">còn %1$d ngày</string>
    <string name="days_today">hết hạn hôm nay</string>
    <string name="days_overdue_fmt">Quá hạn %1$d ngày</string>
    <string name="add_item_title">Thêm thực phẩm</string>
    <string name="edit_item_title">Chỉnh sửa thực phẩm</string>
    <string name="field_name">Tên món</string>
    <string name="field_quantity">Số lượng</string>
    <string name="field_days_left">Hạn dùng (số ngày)</string>
    <string name="expiry_packaging_hint">Chuẩn USDA/FDA · Kiểm tra thêm bao bì</string>
    <string name="opened_toggle_label">Đã mở nắp</string>
    <string name="opened_badge">Đã mở</string>
    <string name="pick_date_label">Chọn ngày</string>
    <string name="recent_items_label">Món gần đây</string>
    <string name="target_compartment_label">Ngăn lưu trữ</string>
    <string name="search_icon_hint">Tìm biểu tượng...</string>
    <string name="action_save">Lưu</string>
    <string name="action_save_and_next">Thêm tiếp</string>
    <string name="action_delete">Xóa khỏi tủ</string>
    <string name="action_move_compartment">Chuyển ngăn</string>
    <string name="delete_reason_title">Lý do xóa món này?</string>
    <string name="delete_reason_used_up">✓ Đã dùng hết</string>
    <string name="delete_reason_discarded">✕ Bỏ đi</string>
    <string name="move_recalc_prompt">Cập nhật hạn dùng khi chuyển giữa ngăn mát và ngăn đông?</string>
    <string name="move_keep_expiry">Giữ hạn hiện tại</string>
    <string name="move_add_freezer_days">+30 ngày (đông)</string>
    <string name="move_set_chilled_days">Còn 3 ngày (mát)</string>
    <string name="notification_channel_name">Nhắc hạn thực phẩm</string>
    <string name="notification_title">FridgeZero · Nhắc hạn dùng</string>
    <string name="notification_body_fmt">Có %1$d món sắp hết hạn hoặc quá hạn. Kiểm tra tủ lạnh nhé!</string>

    <string name="designer_title">THIẾT KẾ TỦ LẠNH</string>
    <string name="designer_subtitle">Chọn mẫu &amp; chỉnh ngăn giống tủ nhà bạn</string>
    <string name="designer_step_badge">BƯỚC 1/2</string>
    <string name="designer_swipe_hint">↻ Vuốt xoay 3D</string>
    <string name="designer_section_template">1 · Mẫu tủ thông dụng</string>
    <string name="designer_section_color">2 · Màu vỏ tủ</string>
    <string name="designer_section_compartments">3 · Tùy chỉnh ngăn</string>
    <string name="template_single">1 cánh</string>
    <string name="template_top_freezer">2 cánh trên</string>
    <string name="template_bottom_freezer">2 cánh dưới</string>
    <string name="template_side_by_side">Side-by-Side</string>
    <string name="template_french_door">French Door</string>
    <string name="template_mini">Tủ Mini</string>
    <string name="color_inox">Bạc Inox</string>
    <string name="color_butter">Vàng Bơ</string>
    <string name="color_mint">Xanh Mint</string>
    <string name="color_matte">Đen Nhám</string>
    <string name="kind_door">Cánh mở</string>
    <string name="kind_drawer">Ngăn kéo</string>
    <string name="designer_add_door">+ Thêm cửa mở</string>
    <string name="designer_add_drawer">+ Thêm ngăn kéo</string>
    <string name="designer_remove">×</string>
    <string name="designer_remove_cd">Xóa ngăn</string>
    <string name="designer_shelves_fmt">%1$d khay</string>
    <string name="designer_shelves_minus_cd">Giảm khay kính</string>
    <string name="designer_shelves_plus_cd">Tăng khay kính</string>
    <string name="designer_new_door_fmt">Cửa mở %1$d</string>
    <string name="designer_new_drawer_fmt">Ngăn kéo %1$d</string>
    <string name="designer_compartment_name_hint">Tên ngăn</string>
    <string name="designer_confirm">CHỐT TỦ NÀY →</string>
    <string name="designer_max_reached">Tối đa 6 ngăn</string>
    <string name="fallback_2d_badge">Chế độ 2D</string>

    <string name="home_brand">FRIDGEZERO</string>
    <string name="home_brand_weather">FRIDGEZERO · 28°C ĐÀ NẴNG</string>
    <string name="home_greeting_morning">Chào buổi sáng, Trường!</string>
    <string name="home_greeting_noon">Chào buổi trưa, Trường!</string>
    <string name="home_greeting_afternoon">Chào buổi chiều, Trường!</string>
    <string name="home_greeting_evening">Chào buổi tối, Trường!</string>
    <string name="home_weather_badge">☀️ 28°C · Đà Nẵng</string>
    <string name="home_urgent_badge_fmt">🔔 %1$d sắp hết hạn</string>
    <string name="home_all_fresh_badge">✨ Tất cả đều tươi</string>
    <string name="home_urgent_banner_title_fmt">🔔 %1$d món sắp hết hạn</string>
    <string name="home_urgent_banner_hint_fmt">Gợi ý hôm nay: %1$s →</string>
    <string name="home_all_fresh_banner">✨ Tất cả đều tươi · Kéo nghiêng 3D</string>
    <string name="home_all_fresh_title">✨ Tất cả đều tươi · Gợi ý nấu ngay</string>
    <string name="home_urgent_view_list">Danh sách</string>
    <string name="home_banner_recipe_fmt">🍳 %1$s · %2$dp →</string>
    <string name="home_reminders_cd">Cài đặt nhắc hạn dùng</string>
    <string name="home_stage_hint">↔ Kéo nghiêng xem tủ · Chạm cửa để mở</string>
    <string name="home_fab_quick_add">+ Thêm nhanh</string>
    <string name="home_tab_fridge">🧊 Tủ lạnh</string>
    <string name="home_tab_fridge_short">Tủ lạnh</string>
    <string name="home_tab_recipes_short">Cẩm nang</string>
    <string name="home_tab_recipes_cd">Mở Cẩm nang ẩm thực</string>
    <string name="home_tab_scan_ai">📷 Quét AI</string>
    <string name="home_tab_scan_ai_short">Quét AI</string>
    <string name="home_tab_scan_badge">GĐ2</string>
    <string name="home_tab_redesign">⚙️ Đổi dáng</string>
    <string name="home_tab_redesign_short">Đổi dáng</string>
    <string name="sticker_urgent_cd">%1$s, %2$d món, %3$d món sắp hết hạn</string>
    <string name="urgent_sheet_title">CẦN XỬ LÝ SỚM</string>
    <string name="urgent_sheet_subtitle">Các món còn ≤ 1 ngày hoặc đã quá hạn</string>
    <string name="urgent_sheet_empty">Tất cả thực phẩm trong tủ đều đang tươi ngon!</string>
    <string name="urgent_sheet_open_compartment">Mở ngăn →</string>
    <string name="urgent_sheet_mark_used_up">✓ Dùng hết</string>
    <string name="urgent_meal_title">🍳 TỐI NAY NẤU GÌ?</string>
    <string name="urgent_meal_cta">Xem công thức &amp; cách làm →</string>
    <string name="urgent_meal_recipe_fmt">%1$s %2$s · %3$dp</string>
    <string name="urgent_meal_chicken_pepper">Gà xào ớt chuông · 15 phút</string>
    <string name="urgent_meal_salmon_salad">Salad cá hồi áp chảo · 15 phút</string>
    <string name="urgent_meal_meat_veggie">Thịt xào rau củ · 15 phút</string>
    <string name="urgent_meal_shrimp_veggie">Tôm xào rau củ · 12 phút</string>
    <string name="urgent_meal_egg_tomato">Trứng chiên sốt cà · 10 phút</string>
    <string name="urgent_meal_fallback_fmt">Ưu tiên chế biến: %1$s</string>
    <string name="urgent_badge_overdue_fmt">Quá hạn %1$dd</string>
    <string name="urgent_badge_today">Hôm nay</string>
    <string name="urgent_badge_days_fmt">Còn %1$dd</string>

    <string name="ai_scan_back">← Quay lại</string>
    <string name="ai_scan_title">QUÉT HÓA ĐƠN AI</string>
    <string name="ai_scan_banner">Tính năng nhận diện hóa đơn AI đang hoàn thiện cho Giai đoạn 2</string>
    <string name="ai_scan_permission_title">Cần quyền Camera để quét hóa đơn</string>
    <string name="ai_scan_permission_body">Cho phép FridgeZero sử dụng camera để căn khung hóa đơn siêu thị và tự động nhập thực phẩm vào tủ ở Giai đoạn 2.</string>
    <string name="ai_scan_permission_grant">Cấp quyền Camera</string>
    <string name="ai_scan_flash_on">🔦 Tắt đèn</string>
    <string name="ai_scan_flash_off">🔦 Bật đèn</string>
    <string name="ai_scan_capture_gd2">Chụp hóa đơn (GĐ2)</string>
    <string name="ai_scan_camera_unavailable">Không tìm thấy camera sau trên thiết bị này, khung ngắm mô phỏng đang hoạt động.</string>

    <string name="bento_back_symbol">←</string>
    <string name="bento_back_cd">Quay lại tủ 3D</string>
    <string name="bento_title_fmt">%1$s · %2$d MÓN</string>
    <string name="bento_add_button">+ Thêm</string>
    <string name="bento_empty_compartment">Ngăn này chưa có món nào. Chạm "+ Thêm" để cất đồ!</string>
    <string name="bento_empty_filter">Không có món nào khớp bộ lọc này.</string>
    <string name="bento_qty_minus_cd">Giảm số lượng</string>
    <string name="bento_qty_plus_cd">Tăng số lượng</string>
    <string name="bento_qty_unit_fmt">%1$d %2$s</string>
    <string name="filter_all">Tất cả</string>
    <string name="filter_urgent">Sắp hết hạn</string>
    <string name="filter_veggies">Rau củ</string>
    <string name="filter_meat_fish">Thịt cá</string>
    <string name="filter_dairy_egg">Sữa trứng</string>
    <string name="filter_drinks">Đồ uống</string>
    <string name="filter_frozen">Đông lạnh</string>

    <string name="quick_add_name_placeholder">Nhập tên thực phẩm...</string>
    <string name="quick_add_unit_label">Đơn vị</string>
    <string name="quick_add_expiry_label">Hạn sử dụng</string>
    <string name="quick_add_expiry_required_hint">Chọn mốc nhanh, chọn ngày hoặc nhập số ngày</string>
    <string name="quick_add_3d">+3 ngày</string>
    <string name="quick_add_7d">+1 tuần</string>
    <string name="quick_add_14d">+2 tuần</string>
    <string name="quick_add_30d">+1 tháng</string>
    <string name="quick_add_days_input_fmt">Còn %1$d ngày</string>
    <string name="quick_add_usda_auto_hint">✓ Gợi ý theo chuẩn USDA/FDA (%1$s)</string>
    <string name="zone_label_chilled">Ngăn mát</string>
    <string name="zone_label_drawer">Ngăn rau củ</string>
    <string name="zone_label_freezer">Ngăn đông</string>

    <string name="reminder_toggle_label">🔔 Nhắc hạn mỗi ngày</string>
    <string name="reminder_toggle_on">Đang bật</string>
    <string name="reminder_toggle_off">Đang tắt</string>
    <string name="reminder_prompt_title">BẬT NHẮC HẠN DÙNG?</string>
    <string name="reminder_prompt_body">FridgeZero sẽ kiểm tra mỗi ngày và nhắc nhẹ khi có món còn ≤ 1 ngày hoặc quá hạn. Không làm phiền khi đồ vẫn tươi.</string>
    <string name="reminder_prompt_confirm">Bật nhắc nhở</string>
    <string name="reminder_prompt_later">Để sau</string>

    <string name="recipe_screen_title">CẨM NANG ẨM THỰC</string>
    <string name="recipe_back_cd">Quay lại tủ 3D</string>
    <string name="recipe_ready_badge_fmt">%1$d SẴN SÀNG</string>
    <string name="recipe_hero_badge">🔥 MÓN CỨU TỦ HÔM NAY</string>
    <string name="recipe_hero_all_fresh_badge">🔥 GỢI Ý NẤU NGAY HÔM NAY</string>
    <string name="recipe_hero_rescue_fmt">Cứu %1$d món gấp</string>
    <string name="recipe_hero_cta">Xem công thức →</string>
    <string name="recipe_filter_all">Tất cả</string>
    <string name="recipe_filter_ready">Đủ đồ nấu ngay</string>
    <string name="recipe_filter_rescue">Cứu đồ gấp</string>
    <string name="recipe_filter_quick">Nhanh ≤ 15p</string>
    <string name="recipe_filter_viet">Cơm nhà Việt</string>
    <string name="recipe_filter_eat_clean">Eat-Clean</string>
    <string name="recipe_empty_filter">Chưa có món nào khớp bộ lọc này. Thử chuyển sang "Tất cả" nhé!</string>
    <string name="recipe_prep_time_short_fmt">⏱ %1$dp</string>
    <string name="recipe_prep_time_long_fmt">⏱ %1$d phút</string>
    <string name="recipe_difficulty_easy">⚡ Dễ</string>
    <string name="recipe_difficulty_medium">⚡ Vừa</string>
    <string name="recipe_match_ratio_fmt">%1$d/%2$d</string>
    <string name="recipe_section_in_fridge">✓ Đã có trong tủ</string>
    <string name="recipe_section_in_fridge_empty">Chưa có nguyên liệu chính nào của món này trong tủ.</string>
    <string name="recipe_section_missing">🛒 Cần chuẩn bị thêm</string>
    <string name="recipe_section_missing_none">✓ Đủ 100% nguyên liệu tủ lạnh — nấu ngay thôi!</string>
    <string name="recipe_section_pantry">🧂 Gia vị bếp cơ bản</string>
    <string name="recipe_section_pantry_hint">Gia vị sẵn trong bếp — không chặn trạng thái đủ đồ</string>
    <string name="recipe_section_steps">👩‍🍳 Cách làm nhanh</string>
    <string name="recipe_substitute_fmt">Thay bằng: %1$s</string>
    <string name="recipe_action_source">🔗 Xem bài gốc</string>
    <string name="recipe_action_cooked">🎉 Đã nấu xong</string>
    <string name="recipe_cooked_toast_fmt">Đã nấu xong! Đã trừ %1$d nguyên liệu khỏi tủ lạnh.</string>
    <string name="recipe_cooked_toast_empty">Chúc ngon miệng!</string>
    <string name="recipe_open_source_error">Không mở được liên kết bài gốc trên thiết bị này.</string>
</resources>
```

### `app/src/main/res/values/themes.xml` (7 lines)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FridgeZero" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#EAE2D3</item>
    </style>
</resources>
```

### `app/src/main/assets/fridge3d/index.html` (1217 lines)

```html
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no" />
<style>
  html, body {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100%;
    overflow: hidden;
    background: #EAE2D3;
    touch-action: none;
    user-select: none;
    -webkit-user-select: none;
  }
  canvas {
    display: block;
    width: 100%;
    height: 100%;
    background: #EAE2D3;
    touch-action: none;
  }
</style>
</head>
<body>
<script src="three.min.js"></script>
<script>
window.addEventListener("error", function(e) {
  console.error("JSERR", e.message, e.filename, e.lineno);
});
console.log("THREE_CHECK", typeof THREE, (typeof THREE !== "undefined" && THREE) ? THREE.REVISION : "missing");
(function () {
  "use strict";

  var qualityParam = "";
  try {
    qualityParam = new URLSearchParams(window.location.search).get("q") || "";
  } catch (_) {}
  var isLiteMode = (qualityParam.toLowerCase() === "lite");
  console.log("QUALITY_MODE", isLiteMode ? "lite" : "high");

  var CAB_W = 1.15;
  var CAB_H = 2.25;
  var CAB_D = 1.00;
  var FOOT_H = 0.09;
  var WALL = 0.065;
  var DOOR_T = 0.086;
  var DOOR_Z_GAP = 0.022;
  var COMP_INSET = 0.014;

  var DEG = Math.PI / 180;
  const DESIGNER_YAW = -18 * DEG;
  const HOME_REST_YAW = 0;
  var DEFAULT_PITCH = -4.5 * DEG;
  var PITCH_MIN = -8 * DEG;
  var PITCH_MAX = 4 * DEG;
  var SPRING_SOFT_LIMIT = 55 * DEG;
  var SPRING_HARD_LIMIT = 78 * DEG;
  var SPRING_STIFFNESS = 135;
  var SPRING_DAMPING = 13.5;
  var BACKFACE_COS = Math.cos(82 * DEG);
  var OPEN_DURATION_MS = 380;

  var SHELL_SPECS = {
    inox: {
      color: 0xD8E1EB,
      metalness: 0.38,
      roughness: 0.16,
      clearcoat: 0.85,
      clearcoatRoughness: 0.10,
      envMapIntensity: 1.15
    },
    butter: {
      color: 0xFDE68A,
      metalness: 0.06,
      roughness: 0.14,
      clearcoat: 0.92,
      clearcoatRoughness: 0.08,
      envMapIntensity: 1.10
    },
    mint: {
      color: 0xA7F3D0,
      metalness: 0.06,
      roughness: 0.14,
      clearcoat: 0.92,
      clearcoatRoughness: 0.08,
      envMapIntensity: 1.10
    },
    matte: {
      color: 0x27272A,
      metalness: 0.22,
      roughness: 0.26,
      clearcoat: 0.55,
      clearcoatRoughness: 0.18,
      envMapIntensity: 1.05
    },
    matte_black: {
      color: 0x27272A,
      metalness: 0.22,
      roughness: 0.26,
      clearcoat: 0.55,
      clearcoatRoughness: 0.18,
      envMapIntensity: 1.05
    }
  };

  var PROP_COLORS = [0xA3E635, 0xFACC15, 0xFB7185, 0xC084FC];

  // Surface-matched WebGL renderer tuned by resolved quality tier (?q=lite vs ?q=high).
  var dpr = window.devicePixelRatio || 1;
  var currentSurfaceBg = "#EAE2D3";
  var renderer = new THREE.WebGLRenderer({
    alpha: false,
    antialias: !isLiteMode
  });
  renderer.setClearColor(currentSurfaceBg, 1);
  renderer.setPixelRatio(Math.min(dpr, isLiteMode ? 1.5 : 2.0));
  renderer.outputEncoding = THREE.sRGBEncoding;
  document.body.appendChild(renderer.domElement);

  // Power-of-two 64x64 tiled dot-grid driven by Kotlin's single-source-of-truth DotGridSpec.
  var dotGridConfig = {
    paperHex: "#EAE2D3",
    dotHex: "#C4B8A3",
    stepDp: 18,
    dotRadiusDp: 1.5,
    offsetXDp: 0,
    offsetYDp: 0
  };
  var lastCssW = window.innerWidth || 360;
  var lastCssH = window.innerHeight || 480;

  function syncDotGridUv(tex) {
    if (!tex) return;
    var step = Math.max(2, Number(dotGridConfig.stepDp) || 18);
    var repX = lastCssW / step;
    var repY = lastCssH / step;
    var ox = Number(dotGridConfig.offsetXDp) || 0;
    var oy = Number(dotGridConfig.offsetYDp) || 0;
    tex.repeat.set(repX, repY);
    tex.offset.set(
      (((ox / step) % 1) + 1) % 1,
      (((-(repY + oy / step)) % 1) + 1) % 1
    );
  }

  function createDotGridTexture() {
    var size = 64;
    var step = Math.max(2, Number(dotGridConfig.stepDp) || 18);
    var radius = Math.max(0.25, Number(dotGridConfig.dotRadiusDp) || 1.5);
    var canvas = document.createElement("canvas");
    canvas.width = size;
    canvas.height = size;
    var ctx = canvas.getContext("2d");
    ctx.fillStyle = dotGridConfig.paperHex || "#EAE2D3";
    ctx.fillRect(0, 0, size, size);
    ctx.fillStyle = dotGridConfig.dotHex || "#C4B8A3";
    ctx.beginPath();
    ctx.arc(size / 2, size / 2, (radius / step) * size, 0, Math.PI * 2);
    ctx.fill();
    var tex = new THREE.CanvasTexture(canvas);
    tex.wrapS = THREE.RepeatWrapping;
    tex.wrapT = THREE.RepeatWrapping;
    tex.encoding = THREE.sRGBEncoding;
    syncDotGridUv(tex);
    return tex;
  }

  // Smooth 512x256 equirectangular studio reflection map with cool-slate gradient & angled softbox bands.
  function createStudioEnvMap() {
    var c = document.createElement("canvas");
    c.width = 512;
    c.height = 256;
    var ctx = c.getContext("2d");

    var g = ctx.createLinearGradient(0, 0, 0, 256);
    g.addColorStop(0.00, "#F8FAFC");
    g.addColorStop(0.24, "#CBD5E1");
    g.addColorStop(0.50, "#64748B");
    g.addColorStop(0.76, "#334155");
    g.addColorStop(1.00, "#0F172A");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, 512, 256);

    function drawSoftboxBand(cx, cy, angleRad, scaleX, scaleY, radius, innerRgba, midRgba, outerRgba) {
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(angleRad);
      ctx.scale(scaleX, scaleY);
      var grad = ctx.createRadialGradient(0, 0, radius * 0.14, 0, 0, radius);
      grad.addColorStop(0.00, innerRgba);
      grad.addColorStop(0.42, midRgba);
      grad.addColorStop(0.76, outerRgba);
      grad.addColorStop(1.00, "rgba(255, 255, 255, 0.0)");
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.arc(0, 0, radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // Primary angled studio softbox band (sweeping highlight on front doors & top-left bevels)
    drawSoftboxBand(
      166, 78, -0.38, 2.15, 0.56, 76,
      "rgba(255, 255, 255, 0.96)",
      "rgba(255, 253, 248, 0.68)",
      "rgba(241, 245, 249, 0.22)"
    );

    // Secondary angled cool-white softbox reflection band (subtle counter-glint on bevel edges)
    drawSoftboxBand(
      374, 94, 0.42, 1.68, 0.42, 64,
      "rgba(248, 252, 255, 0.85)",
      "rgba(224, 242, 254, 0.45)",
      "rgba(224, 242, 254, 0.14)"
    );

    var env = new THREE.CanvasTexture(c);
    env.mapping = THREE.EquirectangularReflectionMapping;
    env.encoding = THREE.sRGBEncoding;
    env.needsUpdate = true;
    return env;
  }

  var dotGridTex = createDotGridTexture();
  var solidBgColor = new THREE.Color(currentSurfaceBg);
  var studioEnv = createStudioEnvMap();

  var scene = new THREE.Scene();
  scene.background = dotGridTex;
  scene.environment = studioEnv;

  var camera = new THREE.PerspectiveCamera(38, 1, 0.1, 30);
  camera.position.set(0, 1.32, 4.72);
  camera.lookAt(0, 1.16, 0);

  // Sculpted high-contrast 3-point studio lighting: upper-front-left key creates bold light/shadow planes.
  var hemiLight = new THREE.HemisphereLight(0xFFFFFF, 0x334155, 0.52);
  scene.add(hemiLight);

  var keyLight = new THREE.DirectionalLight(0xFFFDF8, 1.45);
  keyLight.position.set(-3.2, 4.5, 3.8);
  scene.add(keyLight);

  var rimLight = new THREE.DirectionalLight(0x93C5FD, 0.48);
  rimLight.position.set(3.5, 2.2, -2.6);
  scene.add(rimLight);

  // Soft circular floor contact shadow beneath y = 0.
  function createShadowTexture() {
    var canvas = document.createElement("canvas");
    canvas.width = 256;
    canvas.height = 256;
    var ctx = canvas.getContext("2d");
    var g = ctx.createRadialGradient(128, 128, 16, 128, 128, 124);
    g.addColorStop(0.0, "rgba(17, 17, 17, 0.32)");
    g.addColorStop(0.6, "rgba(17, 17, 17, 0.14)");
    g.addColorStop(1.0, "rgba(17, 17, 17, 0.0)");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, 256, 256);
    var tex = new THREE.CanvasTexture(canvas);
    tex.encoding = THREE.sRGBEncoding;
    return tex;
  }

  var floorShadow = new THREE.Mesh(
    new THREE.CircleGeometry(1.02, 48),
    new THREE.MeshBasicMaterial({
      map: createShadowTexture(),
      transparent: true,
      depthWrite: false
    })
  );
  floorShadow.rotation.x = -Math.PI / 2;
  floorShadow.position.set(0, 0.002, 0);
  scene.add(floorShadow);

  // Root turntable group pinned at floor y = 0.
  var turntable = new THREE.Group();
  turntable.position.set(0, 0, 0);
  scene.add(turntable);

  // Shared materials: sleek glossy tempered-glass/enamel MeshPhysicalMaterial & crisp recessed chassis.
  var inkLineMat = new THREE.LineBasicMaterial({ color: 0x0F172A, transparent: true, opacity: 0.34 });
  var inkMeshMat = new THREE.MeshStandardMaterial({ color: 0x111111, roughness: 0.80, metalness: 0.08, envMapIntensity: 0.20 });
  var cavityMat = new THREE.MeshStandardMaterial({ color: 0x1E293B, roughness: 0.90, metalness: 0.05, envMapIntensity: 0.20 });
  var seamMat = new THREE.MeshStandardMaterial({ color: 0x0F172A, roughness: 0.85, metalness: 0.06, envMapIntensity: 0.22 });
  var ledMat = new THREE.MeshBasicMaterial({ color: 0xE0F2FE });
  var glassMat = new THREE.MeshStandardMaterial({
    color: 0xBAE6FD,
    roughness: 0.14,
    metalness: 0.10,
    transparent: true,
    opacity: 0.54
  });
  var handleMat = isLiteMode
    ? new THREE.MeshStandardMaterial({
        color: 0x1E293B,
        metalness: 0.45,
        roughness: 0.30,
        envMapIntensity: 0.90
      })
    : new THREE.MeshPhysicalMaterial({
        color: 0x1E293B,
        metalness: 0.5,
        roughness: 0.25,
        clearcoat: 0.4,
        clearcoatRoughness: 0.18,
        envMapIntensity: 0.95
      });
  var shellMat = isLiteMode
    ? new THREE.MeshStandardMaterial({
        color: SHELL_SPECS.inox.color,
        metalness: SHELL_SPECS.inox.metalness,
        roughness: SHELL_SPECS.inox.roughness,
        envMapIntensity: SHELL_SPECS.inox.envMapIntensity
      })
    : new THREE.MeshPhysicalMaterial({
        color: SHELL_SPECS.inox.color,
        metalness: SHELL_SPECS.inox.metalness,
        roughness: SHELL_SPECS.inox.roughness,
        clearcoat: SHELL_SPECS.inox.clearcoat,
        clearcoatRoughness: SHELL_SPECS.inox.clearcoatRoughness,
        envMapIntensity: SHELL_SPECS.inox.envMapIntensity
      });
  var propMats = PROP_COLORS.map(function (c) {
    return new THREE.MeshStandardMaterial({ color: c, roughness: 0.46, metalness: 0.06 });
  });
  var magnetYellowMat = new THREE.MeshStandardMaterial({ color: 0xFACC15, roughness: 0.40, metalness: 0.10 });
  var magnetPinkMat = new THREE.MeshStandardMaterial({ color: 0xFB7185, roughness: 0.40, metalness: 0.10 });

  // Build rounded box geometry centered at origin with crisp bevels.
  function createRoundedBoxGeometry(w, h, d, radius, bevelOverride) {
    var r = Math.min(radius, w * 0.42, h * 0.42);
    var hw = w / 2;
    var hh = h / 2;
    var shape = new THREE.Shape();
    shape.moveTo(-hw + r, -hh);
    shape.lineTo(hw - r, -hh);
    shape.quadraticCurveTo(hw, -hh, hw, -hh + r);
    shape.lineTo(hw, hh - r);
    shape.quadraticCurveTo(hw, hh, hw - r, hh);
    shape.lineTo(-hw + r, hh);
    shape.quadraticCurveTo(-hw, hh, -hw, hh - r);
    shape.lineTo(-hw, -hh + r);
    shape.quadraticCurveTo(-hw, -hh, -hw + r, -hh);

    var targetBevel = bevelOverride || 0.012;
    var bevel = Math.min(targetBevel, d * 0.24, r * 0.45);
    var depth = Math.max(0.004, d - bevel * 2);
    var geo = new THREE.ExtrudeGeometry(shape, {
      depth: depth,
      bevelEnabled: true,
      bevelThickness: bevel,
      bevelSize: bevel,
      bevelSegments: 2,
      curveSegments: 5
    });
    geo.center();
    geo.computeVertexNormals();
    return geo;
  }

  function addOutlinedMesh(parent, geo, mat, x, y, z, userData) {
    var mesh = new THREE.Mesh(geo, mat);
    mesh.position.set(x, y, z);
    if (userData) Object.assign(mesh.userData, userData);
    var edges = new THREE.EdgesGeometry(geo, 28);
    var line = new THREE.LineSegments(edges, inkLineMat);
    mesh.add(line);
    parent.add(mesh);
    return mesh;
  }

  function clearGroup(group) {
    while (group.children.length > 0) {
      var child = group.children[0];
      group.remove(child);
      if (child.children && child.children.length > 0) clearGroup(child);
      if (child.geometry) child.geometry.dispose();
    }
  }

  function worldX(nx) { return -CAB_W / 2 + nx * CAB_W; }
  function worldY(ny) { return FOOT_H + (1 - ny) * CAB_H; }

  // Scene state
  var state = {
    template: "top_freezer",
    colorKey: "inox",
    compartments: [],
    openCompartmentId: null,
    showStickers: false,
    springReturn: false,
    showDotGrid: true
  };
  var layoutSignature = "";
  var doorControllers = []; // { id, kind, rightHinge, pivot, anchor, progress }
  var raycastTargets = [];
  var lastProjectedPayload = "";
  var dirty = true;

  function markDirty() {
    dirty = true;
  }

  function updateShellMaterial(colorKey) {
    var spec = SHELL_SPECS[colorKey] || SHELL_SPECS.inox;
    shellMat.color.setHex(spec.color);
    shellMat.metalness = spec.metalness;
    shellMat.roughness = spec.roughness;
    if (!isLiteMode) {
      shellMat.clearcoat = spec.clearcoat;
      shellMat.clearcoatRoughness = spec.clearcoatRoughness;
    }
    shellMat.envMapIntensity = spec.envMapIntensity;
    shellMat.needsUpdate = true;
    markDirty();
  }

  function rebuildFridge() {
    clearGroup(turntable);
    doorControllers = [];
    raycastTargets = [];
    lastProjectedPayload = "";
    markDirty();

    var halfW = CAB_W / 2;
    var midY = FOOT_H + CAB_H / 2;
    var topY = FOOT_H + CAB_H;

    // 4 grounded feet sitting flush at y = 0..FOOT_H
    var footGeo = new THREE.CylinderGeometry(0.062, 0.056, FOOT_H, 16);
    var fx = halfW - 0.12;
    var fz = CAB_D / 2 - 0.12;
    [[-fx, -fz], [fx, -fz], [-fx, fz], [fx, fz]].forEach(function (pt) {
      var foot = new THREE.Mesh(footGeo, inkMeshMat);
      foot.position.set(pt[0], FOOT_H / 2, pt[1]);
      turntable.add(foot);
    });

    // Hollow cabinet shell (5 panels)
    var cabData = { isCabinet: true };
    var backMesh = addOutlinedMesh(
      turntable,
      createRoundedBoxGeometry(CAB_W, CAB_H, WALL, 0.045, 0.014),
      shellMat,
      0, midY, -CAB_D / 2 + WALL / 2,
      cabData
    );
    var sideGeo = createRoundedBoxGeometry(WALL, CAB_H, CAB_D, 0.02, 0.012);
    var leftMesh = addOutlinedMesh(turntable, sideGeo, shellMat, -halfW + WALL / 2, midY, 0, cabData);
    var rightMesh = addOutlinedMesh(turntable, sideGeo.clone(), shellMat, halfW - WALL / 2, midY, 0, cabData);
    var capGeo = createRoundedBoxGeometry(CAB_W, WALL, CAB_D, 0.04, 0.012);
    var topMesh = addOutlinedMesh(turntable, capGeo, shellMat, 0, topY - WALL / 2, 0, cabData);
    var botMesh = addOutlinedMesh(turntable, capGeo.clone(), shellMat, 0, FOOT_H + WALL / 2, 0, cabData);
    raycastTargets.push(backMesh, leftMesh, rightMesh, topMesh, botMesh);

    // Dark recessed shadow perimeter frame (#1E293B) behind outer door seams
    var seamZ = CAB_D / 2 + 0.004;
    var seamD = 0.014;
    var topSeam = new THREE.Mesh(new THREE.BoxGeometry(CAB_W - 0.04, WALL * 0.85, seamD), seamMat);
    topSeam.position.set(0, topY - WALL * 0.5, seamZ);
    var botSeam = new THREE.Mesh(new THREE.BoxGeometry(CAB_W - 0.04, WALL * 0.85, seamD), seamMat);
    botSeam.position.set(0, FOOT_H + WALL * 0.5, seamZ);
    var leftSeam = new THREE.Mesh(new THREE.BoxGeometry(WALL * 0.82, CAB_H - 0.04, seamD), seamMat);
    leftSeam.position.set(-halfW + WALL * 0.48, midY, seamZ);
    var rightSeam = new THREE.Mesh(new THREE.BoxGeometry(WALL * 0.82, CAB_H - 0.04, seamD), seamMat);
    rightSeam.position.set(halfW - WALL * 0.48, midY, seamZ);
    turntable.add(topSeam, botSeam, leftSeam, rightSeam);

    // Dark interior cavity + ceiling LED light
    var cavGeo = new THREE.BoxGeometry(CAB_W - WALL * 2, CAB_H - WALL * 2, 0.015);
    var cavMesh = new THREE.Mesh(cavGeo, cavityMat);
    cavMesh.position.set(0, midY, -CAB_D / 2 + WALL + 0.008);
    turntable.add(cavMesh);

    var ledGeo = new THREE.BoxGeometry(CAB_W - 0.24, 0.022, 0.07);
    var ledMesh = new THREE.Mesh(ledGeo, ledMat);
    ledMesh.position.set(0, topY - WALL - 0.018, CAB_D / 2 - 0.22);
    turntable.add(ledMesh);

    // Build interior compartments + doors/drawers
    var zFront = CAB_D / 2 + DOOR_Z_GAP;
    var depthInt = CAB_D - WALL * 2;

    state.compartments.forEach(function (c, index) {
      var left = worldX(c.x) + WALL;
      var right = worldX(c.x + c.w) - WALL;
      var intW = Math.max(0.12, right - left);
      var cxInt = (left + right) / 2;
      var top = worldY(c.y);
      var bottom = worldY(c.y + c.h);

      // Divider shelf + dark recessed seam strip between compartments
      if (c.y + c.h < 0.96) {
        var divGeo = new THREE.BoxGeometry(intW, WALL * 0.55, depthInt);
        var divMesh = new THREE.Mesh(divGeo, shellMat);
        divMesh.position.set(cxInt, bottom, 0);
        turntable.add(divMesh);

        var hGapStrip = new THREE.Mesh(
          new THREE.BoxGeometry(intW + WALL * 0.4, WALL * 0.72, 0.018),
          seamMat
        );
        hGapStrip.position.set(cxInt, bottom, seamZ);
        turntable.add(hGapStrip);
      }
      if (c.x + c.w < 0.94) {
        var vDivGeo = new THREE.BoxGeometry(WALL * 0.55, top - bottom, depthInt);
        var vDivMesh = new THREE.Mesh(vDivGeo, shellMat);
        vDivMesh.position.set(worldX(c.x + c.w), (top + bottom) / 2, 0);
        turntable.add(vDivMesh);

        var vGapStrip = new THREE.Mesh(
          new THREE.BoxGeometry(WALL * 0.72, top - bottom, 0.018),
          seamMat
        );
        vGapStrip.position.set(worldX(c.x + c.w), (top + bottom) / 2, seamZ);
        turntable.add(vGapStrip);
      }

      // Glass shelves & colorful 3D food props
      var shelfCount = Math.max(0, Math.min(5, c.shelves || 1));
      for (var s = 1; s <= shelfCount; s++) {
        var sy = bottom + (top - bottom) * (s / (shelfCount + 1));
        var shelfGeo = new THREE.BoxGeometry(intW - 0.03, 0.014, depthInt - 0.08);
        var shelfMesh = new THREE.Mesh(shelfGeo, glassMat);
        shelfMesh.position.set(cxInt, sy, 0.01);
        turntable.add(shelfMesh);

        var p1Geo = createRoundedBoxGeometry(0.15, 0.11, 0.15, 0.03);
        var p1 = new THREE.Mesh(p1Geo, propMats[(s + index) % propMats.length]);
        p1.position.set(left + intW * 0.28, sy + 0.062, 0.03);
        turntable.add(p1);

        var p2Geo = createRoundedBoxGeometry(0.12, 0.085, 0.12, 0.025);
        var p2 = new THREE.Mesh(p2Geo, propMats[(s + index + 2) % propMats.length]);
        p2.position.set(left + intW * 0.68, sy + 0.05, 0.08);
        turntable.add(p2);
      }

      // Door / Drawer mesh bounds with deep separation gap & pop-out 3D relief
      var doorW = Math.max(0.16, c.w * CAB_W - COMP_INSET * 2);
      var doorH = Math.max(0.16, c.h * CAB_H - COMP_INSET * 2);
      var centerY = worldY(c.y + c.h / 2);
      var centerX = worldX(c.x + c.w / 2);
      var doorUserData = { compartmentId: c.id };
      var isRightHinge = (c.kind === "DOOR") && (c.w < 0.6 && c.x > 0.4);

      var pivot = new THREE.Group();
      pivot.name = "DoorNode_" + index;
      turntable.add(pivot);

      var anchor = new THREE.Object3D();

      if (c.kind === "DOOR") {
        var hingeX = isRightHinge
          ? (worldX(c.x + c.w) - COMP_INSET)
          : (worldX(c.x) + COMP_INSET);
        var dir = isRightHinge ? -1 : 1;
        pivot.position.set(hingeX, centerY, zFront);

        // Recessed magnetic door gasket backing (#1E293B) behind the beveled door slab
        var gasketMesh = new THREE.Mesh(
          new THREE.BoxGeometry(Math.max(0.12, doorW - 0.026), Math.max(0.12, doorH - 0.026), 0.014),
          seamMat
        );
        gasketMesh.position.set(dir * doorW / 2, 0, -0.005);
        pivot.add(gasketMesh);

        var doorMesh = addOutlinedMesh(
          pivot,
          createRoundedBoxGeometry(doorW, doorH, DOOR_T, 0.044, 0.018),
          shellMat,
          dir * doorW / 2, 0, DOOR_T / 2,
          doorUserData
        );
        raycastTargets.push(doorMesh);

        // Dark charcoal anodized bar handle + subtle shadow backing strip
        var hLen = Math.min(0.75, doorH * 0.44);
        var handleX = dir * (doorW - 0.082);
        var stripMesh = new THREE.Mesh(
          new THREE.BoxGeometry(0.026, hLen * 0.94, 0.006),
          seamMat
        );
        stripMesh.position.set(handleX, 0, DOOR_T + 0.003);
        pivot.add(stripMesh);

        var handleGeo = new THREE.CylinderGeometry(0.019, 0.019, hLen, 16);
        var handleMesh = addOutlinedMesh(
          pivot,
          handleGeo,
          handleMat,
          handleX, 0, DOOR_T + 0.036,
          doorUserData
        );
        raycastTargets.push(handleMesh);

        // Signature magnets on top-right corner of top door (index == 0)
        if (index === 0) {
          var mag1Geo = new THREE.CylinderGeometry(0.045, 0.045, 0.022, 20);
          mag1Geo.rotateX(Math.PI / 2);
          var mag1X = isRightHinge ? dir * (doorW * 0.24) : dir * (doorW * 0.72);
          var mag1 = addOutlinedMesh(
            pivot,
            mag1Geo,
            magnetYellowMat,
            mag1X, doorH * 0.30, DOOR_T + 0.012,
            doorUserData
          );
          var mag2X = isRightHinge ? dir * (doorW * 0.38) : dir * (doorW * 0.84);
          var mag2 = addOutlinedMesh(
            pivot,
            createRoundedBoxGeometry(0.09, 0.06, 0.022, 0.015, 0.005),
            magnetPinkMat,
            mag2X, doorH * 0.16, DOOR_T + 0.012,
            doorUserData
          );
          raycastTargets.push(mag1, mag2);
        }

        anchor.position.set(dir * doorW * 0.5, 0, DOOR_T + 0.055);
        pivot.add(anchor);
      } else {
        // DRAWER
        pivot.position.set(centerX, centerY, zFront);

        var drawerGasket = new THREE.Mesh(
          new THREE.BoxGeometry(Math.max(0.12, doorW - 0.026), Math.max(0.12, doorH - 0.026), 0.014),
          seamMat
        );
        drawerGasket.position.set(0, 0, -0.005);
        pivot.add(drawerGasket);

        var drawerMesh = addOutlinedMesh(
          pivot,
          createRoundedBoxGeometry(doorW, doorH, DOOR_T, 0.040, 0.018),
          shellMat,
          0, 0, DOOR_T / 2,
          doorUserData
        );
        raycastTargets.push(drawerMesh);

        // Horizontal charcoal anodized handle + subtle shadow backing strip
        var hWidth = Math.min(0.56, doorW * 0.48);
        var handleY = doorH * 0.32;
        var dhStrip = new THREE.Mesh(
          new THREE.BoxGeometry(hWidth * 0.94, 0.026, 0.006),
          seamMat
        );
        dhStrip.position.set(0, handleY, DOOR_T + 0.003);
        pivot.add(dhStrip);

        var dhGeo = new THREE.CylinderGeometry(0.019, 0.019, hWidth, 16);
        dhGeo.rotateZ(Math.PI / 2);
        var dhMesh = addOutlinedMesh(
          pivot,
          dhGeo,
          handleMat,
          0, handleY, DOOR_T + 0.036,
          doorUserData
        );
        raycastTargets.push(dhMesh);

        // Inner pulled-out tray
        var trayGeo = createRoundedBoxGeometry(doorW - 0.12, doorH * 0.68, CAB_D * 0.65, 0.02, 0.008);
        var trayMesh = addOutlinedMesh(
          pivot,
          trayGeo,
          shellMat,
          0, -doorH * 0.08, -CAB_D * 0.32,
          doorUserData
        );
        raycastTargets.push(trayMesh);

        anchor.position.set(0, 0, DOOR_T + 0.055);
        pivot.add(anchor);
      }

      doorControllers.push({
        id: c.id,
        kind: c.kind,
        rightHinge: isRightHinge,
        baseZ: zFront,
        pivot: pivot,
        anchor: anchor,
        progress: (c.id === state.openCompartmentId) ? 1 : 0
      });
    });
  }

  function getRestYaw() {
    return state.springReturn ? HOME_REST_YAW : DESIGNER_YAW;
  }

  // Turntable rotation & welcome spin state
  var yaw = getRestYaw();
  var pitch = DEFAULT_PITCH;
  var yawVel = 0;
  var isDragging = false;
  var hasReceivedConfig = false;
  var welcomeSpin = null; // { startTime, startYaw, targetYaw }
  var resetAnim = null;   // { startTime, startYaw, targetYaw, startPitch }
  var bounceAnim = null;  // { startTime, impulseApplied }

  function nearestYaw(current, desired) {
    var delta = (desired - current) % (Math.PI * 2);
    if (delta > Math.PI) delta -= Math.PI * 2;
    if (delta < -Math.PI) delta += Math.PI * 2;
    return current + delta;
  }

  // Global Kotlin -> JS API
  window.updateFridge = function (configJson) {
    try {
      var cfg = typeof configJson === "string" ? JSON.parse(configJson) : configJson;
      var wasSpring = state.springReturn;
      var isInitialConfig = !hasReceivedConfig;
      hasReceivedConfig = true;
      state.template = cfg.template || "top_freezer";
      state.colorKey = cfg.colorKey || "inox";
      state.openCompartmentId = cfg.openCompartmentId || null;
      state.showStickers = !!cfg.showStickers;
      state.springReturn = (cfg.springReturn !== undefined) ? !!cfg.springReturn : state.showStickers;
      state.showDotGrid = cfg.showDotGrid !== false;
      state.compartments = Array.isArray(cfg.compartments) ? cfg.compartments : [];

      if (isInitialConfig && !welcomeSpin) {
        yaw = getRestYaw();
        yawVel = 0;
        resetAnim = null;
      } else if (!wasSpring && state.springReturn) {
        if (welcomeSpin) {
          welcomeSpin.targetYaw = HOME_REST_YAW;
        } else {
          var wrapped = nearestYaw(HOME_REST_YAW, yaw);
          yaw = Math.max(
            HOME_REST_YAW - SPRING_HARD_LIMIT,
            Math.min(HOME_REST_YAW + SPRING_HARD_LIMIT, wrapped)
          );
          yawVel = 0;
          resetAnim = null;
        }
      }

      if (cfg.dotGrid) {
        var dg = cfg.dotGrid;
        var nextPaper = dg.paperHex || dotGridConfig.paperHex;
        var nextDot = dg.dotHex || dotGridConfig.dotHex;
        var nextStep = Number(dg.stepDp) || dotGridConfig.stepDp;
        var nextRadius = Number(dg.dotRadiusDp) || dotGridConfig.dotRadiusDp;
        var texDirty =
          nextPaper !== dotGridConfig.paperHex ||
          nextDot !== dotGridConfig.dotHex ||
          nextStep !== dotGridConfig.stepDp ||
          nextRadius !== dotGridConfig.dotRadiusDp;
        dotGridConfig.paperHex = nextPaper;
        dotGridConfig.dotHex = nextDot;
        dotGridConfig.stepDp = nextStep;
        dotGridConfig.dotRadiusDp = nextRadius;
        dotGridConfig.offsetXDp = Number(dg.offsetXDp) || 0;
        dotGridConfig.offsetYDp = Number(dg.offsetYDp) || 0;
        if (texDirty) {
          if (dotGridTex) dotGridTex.dispose();
          dotGridTex = createDotGridTexture();
        }
      }

      var nextBg = cfg.surfaceBg || (state.showDotGrid ? dotGridConfig.paperHex : "#FFFFFF");
      if (nextBg !== currentSurfaceBg) {
        currentSurfaceBg = nextBg;
        document.documentElement.style.backgroundColor = nextBg;
        document.body.style.backgroundColor = nextBg;
        if (renderer.domElement) renderer.domElement.style.backgroundColor = nextBg;
      }
      renderer.setClearColor(nextBg, 1);

      if (state.showDotGrid) {
        syncDotGridUv(dotGridTex);
        scene.background = dotGridTex;
      } else {
        solidBgColor.set(nextBg);
        scene.background = solidBgColor;
      }

      updateShellMaterial(state.colorKey);

      var nextSig = state.template + "|" + JSON.stringify(state.compartments.map(function (c) {
        return [c.id, c.kind, c.x, c.y, c.w, c.h, c.shelves];
      }));
      if (nextSig !== layoutSignature) {
        layoutSignature = nextSig;
        rebuildFridge();
      }
      if (state.showStickers) {
        lastProjectedPayload = "";
      }
      markDirty();
    } catch (err) {
      console.error("UPDATE_ERR", err);
    }
  };

  window.playWelcomeSpin = function () {
    var restYaw = getRestYaw();
    resetAnim = null;
    yawVel = 0;
    yaw = restYaw;
    pitch = DEFAULT_PITCH;
    welcomeSpin = {
      startTime: performance.now(),
      startYaw: restYaw,
      targetYaw: restYaw
    };
    markDirty();
  };

  window.resetView = function () {
    var restYaw = getRestYaw();
    welcomeSpin = null;
    if (state.springReturn) {
      resetAnim = null;
      if (Math.abs(yaw - restYaw) < 6 * DEG) {
        yawVel = 3.4;
      } else {
        yawVel = 0;
      }
      markDirty();
      return;
    }
    yawVel = 0;
    resetAnim = {
      startTime: performance.now(),
      startYaw: yaw,
      targetYaw: nearestYaw(yaw, restYaw),
      startPitch: pitch
    };
    markDirty();
  };

  window.playItemAddedBounce = function () {
    bounceAnim = {
      startTime: performance.now(),
      impulseApplied: false
    };
    markDirty();
  };

  // Pointer & Raycaster handling
  var raycaster = new THREE.Raycaster();
  var downPos = { x: 0, y: 0 };
  var lastPos = { x: 0, y: 0 };
  var lastMoveTime = performance.now();
  var totalDrag = 0;
  var activePointerId = null;

  var dom = renderer.domElement;
  dom.addEventListener("touchstart", function (e) {
    if (e.cancelable) e.preventDefault();
  }, { passive: false });
  dom.addEventListener("touchmove", function (e) {
    if (e.cancelable) e.preventDefault();
  }, { passive: false });

  dom.addEventListener("pointerdown", function (e) {
    activePointerId = e.pointerId;
    isDragging = true;
    welcomeSpin = null;
    resetAnim = null;
    yawVel = 0;
    downPos = { x: e.clientX, y: e.clientY };
    lastPos = { x: e.clientX, y: e.clientY };
    lastMoveTime = performance.now();
    totalDrag = 0;
    if (dom.setPointerCapture) {
      try { dom.setPointerCapture(e.pointerId); } catch (_) {}
    }
    markDirty();
  });

  dom.addEventListener("pointermove", function (e) {
    if (!isDragging || e.pointerId !== activePointerId) return;
    var nowMs = performance.now();
    var moveDt = Math.max(0.008, (nowMs - lastMoveTime) / 1000);
    lastMoveTime = nowMs;

    var dx = e.clientX - lastPos.x;
    var dy = e.clientY - lastPos.y;
    lastPos = { x: e.clientX, y: e.clientY };
    totalDrag += Math.hypot(dx, dy);

    var dyaw = dx * 0.0095;
    if (state.springReturn) {
      var offset = yaw - HOME_REST_YAW;
      if (Math.abs(offset) > SPRING_SOFT_LIMIT && (offset * dyaw > 0)) {
        var over = Math.min(1, (Math.abs(offset) - SPRING_SOFT_LIMIT) / (SPRING_HARD_LIMIT - SPRING_SOFT_LIMIT));
        dyaw *= Math.max(0.08, 1 - over * 0.88);
      }
      yaw = Math.max(HOME_REST_YAW - SPRING_HARD_LIMIT, Math.min(HOME_REST_YAW + SPRING_HARD_LIMIT, yaw + dyaw));
      yawVel = Math.max(-8, Math.min(8, (dyaw / moveDt) * 0.35));
    } else {
      yaw += dyaw;
      yawVel = dyaw * 0.75;
    }
    pitch = Math.max(PITCH_MIN, Math.min(PITCH_MAX, pitch - dy * 0.0022));
    markDirty();
  });

  function handlePointerEnd(e) {
    if (!isDragging || e.pointerId !== activePointerId) return;
    isDragging = false;
    activePointerId = null;

    if (totalDrag < 10) {
      yawVel = 0;
      var rect = dom.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        var ndc = new THREE.Vector2(
          ((e.clientX - rect.left) / rect.width) * 2 - 1,
          -((e.clientY - rect.top) / rect.height) * 2 + 1
        );
        raycaster.setFromCamera(ndc, camera);
        var hits = raycaster.intersectObjects(raycastTargets, false);
        if (hits.length > 0) {
          var first = hits[0].object;
          if (!first.userData.isCabinet && first.userData.compartmentId) {
            var tappedId = first.userData.compartmentId;
            state.openCompartmentId = (state.openCompartmentId === tappedId) ? null : tappedId;
            if (window.AndroidBridge && window.AndroidBridge.onDoorTapped) {
              window.AndroidBridge.onDoorTapped(tappedId);
            }
          }
        }
      }
    }
    markDirty();
  }

  dom.addEventListener("pointerup", handlePointerEnd);
  dom.addEventListener("pointercancel", handlePointerEnd);

  // Resize observer guarding against zero-size layout passes
  function applyResize(w, h) {
    if (!w || !h || w <= 0 || h <= 0) return;
    console.log("CANVAS_RESIZE", w, h);
    lastCssW = w;
    lastCssH = h;
    syncDotGridUv(dotGridTex);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
    renderer.setSize(w, h, false);
    lastProjectedPayload = "";
    markDirty();
  }
  if (typeof ResizeObserver !== "undefined") {
    var ro = new ResizeObserver(function (entries) {
      var rect = (entries && entries[0] && entries[0].contentRect) || document.documentElement.getBoundingClientRect();
      applyResize(
        Math.round(rect.width || window.innerWidth || 0),
        Math.round(rect.height || window.innerHeight || 0)
      );
    });
    ro.observe(document.documentElement);
  }
  window.addEventListener("resize", function () {
    applyResize(window.innerWidth || 0, window.innerHeight || 0);
  });
  document.addEventListener("visibilitychange", function () {
    if (!document.hidden) markDirty();
  });
  applyResize(window.innerWidth || 0, window.innerHeight || 0);

  // Frame loop
  var lastFrameTime = performance.now();
  var tmpVec = new THREE.Vector3();

  function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
  }

  function hasDoorAnimation() {
    for (var i = 0; i < doorControllers.length; i++) {
      var dc = doorControllers[i];
      var target = (dc.id === state.openCompartmentId) ? 1 : 0;
      if (dc.progress !== target) return true;
    }
    return false;
  }

  function hasActiveMotion() {
    if (Math.abs(DEFAULT_PITCH - pitch) >= 0.0005) return true;
    if (state.springReturn) {
      return Math.abs(yaw - HOME_REST_YAW) >= 0.0005 || Math.abs(yawVel) >= 0.002;
    }
    return Math.abs(yawVel) > 0.0002;
  }

  var contextLost = false;
  var rafId = 0;

  function animate(now) {
    if (contextLost) {
      rafId = 0;
      return;
    }
    rafId = requestAnimationFrame(animate);
    var dt = Math.min(0.05, Math.max(0.001, (now - lastFrameTime) / 1000));
    lastFrameTime = now;

    var shouldStep =
      dirty ||
      isDragging ||
      welcomeSpin !== null ||
      resetAnim !== null ||
      bounceAnim !== null ||
      hasDoorAnimation() ||
      hasActiveMotion();
    if (!shouldStep) return;

    if (welcomeSpin) {
      var t = Math.min(1, (now - welcomeSpin.startTime) / 1000);
      var spinTarget = (welcomeSpin.targetYaw !== undefined) ? welcomeSpin.targetYaw : getRestYaw();
      yaw = welcomeSpin.startYaw + (spinTarget + Math.PI * 2 - welcomeSpin.startYaw) * easeInOutCubic(t);
      pitch = DEFAULT_PITCH;
      if (t >= 1) {
        yaw = spinTarget;
        welcomeSpin = null;
        if (window.AndroidBridge && window.AndroidBridge.onWelcomeSpinFinished) {
          window.AndroidBridge.onWelcomeSpinFinished();
        }
      }
    } else if (resetAnim) {
      var rt = Math.min(1, (now - resetAnim.startTime) / 360);
      var re = easeInOutCubic(rt);
      yaw = resetAnim.startYaw + (resetAnim.targetYaw - resetAnim.startYaw) * re;
      pitch = resetAnim.startPitch + (DEFAULT_PITCH - resetAnim.startPitch) * re;
      if (rt >= 1) {
        yaw = getRestYaw();
        pitch = DEFAULT_PITCH;
        resetAnim = null;
      }
    } else if (!isDragging) {
      if (state.springReturn) {
        var subDt = dt * 0.5;
        for (var k = 0; k < 2; k++) {
          var displacement = yaw - HOME_REST_YAW;
          var accel = -SPRING_STIFFNESS * displacement - SPRING_DAMPING * yawVel;
          yawVel += accel * subDt;
          yaw = Math.max(
            HOME_REST_YAW - SPRING_HARD_LIMIT,
            Math.min(HOME_REST_YAW + SPRING_HARD_LIMIT, yaw + yawVel * subDt)
          );
        }
        if (Math.abs(yaw - HOME_REST_YAW) < 0.0005 && Math.abs(yawVel) < 0.002) {
          yaw = HOME_REST_YAW;
          yawVel = 0;
        }
      } else {
        if (Math.abs(yawVel) > 0.0002) {
          yaw += yawVel * (dt * 60);
          yawVel *= Math.pow(0.91, dt * 60);
        } else {
          yawVel = 0;
        }
      }
      // Spring pitch back to grounded -4.5 deg and snap cleanly when settled
      if (Math.abs(DEFAULT_PITCH - pitch) < 0.0005) {
        pitch = DEFAULT_PITCH;
      } else {
        pitch += (DEFAULT_PITCH - pitch) * Math.min(1, dt * 10);
      }
    }

    if (bounceAnim) {
      var elapsed = now - bounceAnim.startTime;
      if (elapsed < 90) {
        var p1 = elapsed / 90;
        var e1 = easeInOutCubic(p1);
        var sy1 = 1 + (0.94 - 1) * e1;
        var sxz1 = 1 + (1.032 - 1) * e1;
        turntable.scale.set(sxz1, sy1, sxz1);
        turntable.position.y = 0;
        floorShadow.scale.set(1, 1, 1);
      } else if (elapsed < 220) {
        if (!bounceAnim.impulseApplied) {
          bounceAnim.impulseApplied = true;
          yawVel += 1.5;
        }
        var p2 = (elapsed - 90) / 130;
        var e2 = easeInOutCubic(p2);
        var sy2 = 0.94 + (1.045 - 0.94) * e2;
        var sxz2 = 1.032 + (0.978 - 1.032) * e2;
        turntable.scale.set(sxz2, sy2, sxz2);
        turntable.position.y = 0.045 * e2;
        var sh2 = 1 - turntable.position.y * 2.4;
        floorShadow.scale.set(sh2, sh2, 1);
      } else if (elapsed < 450) {
        var p3 = (elapsed - 220) / 230;
        var e3 = easeInOutCubic(p3);
        var sy3 = 1.045 + (1.0 - 1.045) * e3;
        var sxz3 = 0.978 + (1.0 - 0.978) * e3;
        turntable.scale.set(sxz3, sy3, sxz3);
        turntable.position.y = 0.045 * (1 - e3);
        var sh3 = 1 - turntable.position.y * 2.4;
        floorShadow.scale.set(sh3, sh3, 1);
      } else {
        turntable.scale.set(1, 1, 1);
        turntable.position.y = 0;
        floorShadow.scale.set(1, 1, 1);
        bounceAnim = null;
      }
    }

    turntable.rotation.y = yaw;
    turntable.rotation.x = pitch;

    // Animate door & drawer open/close
    var step = (dt * 1000) / OPEN_DURATION_MS;
    for (var i = 0; i < doorControllers.length; i++) {
      var dc = doorControllers[i];
      var target = (dc.id === state.openCompartmentId) ? 1 : 0;
      if (dc.progress !== target) {
        if (dc.progress < target) dc.progress = Math.min(1, dc.progress + step);
        else dc.progress = Math.max(0, dc.progress - step);
      }
      var eased = easeInOutCubic(dc.progress);
      if (dc.kind === "DOOR") {
        var maxAngle = (dc.rightHinge ? 85 : -85) * DEG;
        dc.pivot.rotation.y = maxAngle * eased;
      } else {
        dc.pivot.position.z = dc.baseZ + 0.62 * eased;
      }
    }

    turntable.updateMatrixWorld(true);

    // Project door sticker anchors to normalized screen coords when requested
    if (state.showStickers && window.AndroidBridge && window.AndroidBridge.onStickersProjected) {
      var isFront = Math.cos(yaw) > BACKFACE_COS;
      var list = [];
      for (var j = 0; j < doorControllers.length; j++) {
        var ctrl = doorControllers[j];
        ctrl.anchor.getWorldPosition(tmpVec);
        tmpVec.project(camera);
        var sx = (tmpVec.x * 0.5 + 0.5);
        var sy = (-tmpVec.y * 0.5 + 0.5);
        var vis = isFront && tmpVec.z < 1 && sx >= -0.1 && sx <= 1.1 && sy >= -0.1 && sy <= 1.1;
        list.push({
          id: ctrl.id,
          x: Math.round(sx * 1000) / 1000,
          y: Math.round(sy * 1000) / 1000,
          visible: !!vis
        });
      }
      var payload = JSON.stringify(list);
      if (payload !== lastProjectedPayload) {
        lastProjectedPayload = payload;
        window.AndroidBridge.onStickersProjected(payload);
      }
    }

    renderer.render(scene, camera);

    dirty =
      isDragging ||
      welcomeSpin !== null ||
      resetAnim !== null ||
      bounceAnim !== null ||
      hasDoorAnimation() ||
      hasActiveMotion();
  }

  dom.addEventListener("webglcontextlost", function (e) {
    e.preventDefault();
    contextLost = true;
    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = 0;
    }
    console.error("WEBGL_CONTEXT_LOST");
    if (window.AndroidBridge && window.AndroidBridge.onWebGlContextLost) {
      window.AndroidBridge.onWebGlContextLost();
    }
  }, false);

  dom.addEventListener("webglcontextrestored", function () {
    console.log("WEBGL_CONTEXT_RESTORED");
    contextLost = false;
    if (dotGridTex) dotGridTex.dispose();
    dotGridTex = createDotGridTexture();
    if (studioEnv) studioEnv.dispose();
    studioEnv = createStudioEnvMap();
    scene.environment = studioEnv;
    scene.background = state.showDotGrid ? dotGridTex : solidBgColor;
    if (floorShadow.material.map) floorShadow.material.map.dispose();
    floorShadow.material.map = createShadowTexture();
    floorShadow.material.needsUpdate = true;
    updateShellMaterial(state.colorKey);
    rebuildFridge();
    applyResize(lastCssW, lastCssH);
    lastFrameTime = performance.now();
    markDirty();
    if (!rafId) {
      rafId = requestAnimationFrame(animate);
    }
  }, false);

  rafId = requestAnimationFrame(animate);
})();
</script>
</body>
</html>
```

### `app/src/main/kotlin/com/truong/fridgezero/data/AppConfig.kt` (67 lines)

```kotlin
package com.truong.fridgezero.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "fridgezero_prefs"
)

class AppConfig(context: Context) {

  private val dataStore = context.applicationContext.appPrefsDataStore

  val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_ONBOARDING_DONE] ?: false
  }

  val expiryRemindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_EXPIRY_REMINDERS] ?: false
  }

  val notificationPromptShown: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_NOTIFICATION_PROMPTED] ?: false
  }

  val forceLiteMode: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_FORCE_LITE_MODE] ?: false
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_ONBOARDING_DONE] = completed
    }
  }

  suspend fun setExpiryRemindersEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_EXPIRY_REMINDERS] = enabled
    }
  }

  suspend fun setNotificationPromptShown(shown: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_NOTIFICATION_PROMPTED] = shown
    }
  }

  suspend fun setForceLiteMode(enabled: Boolean) {
    dataStore.edit { prefs ->
      prefs[KEY_FORCE_LITE_MODE] = enabled
    }
  }

  companion object {
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("has_completed_onboarding")
    private val KEY_EXPIRY_REMINDERS = booleanPreferencesKey("expiry_reminders_enabled")
    private val KEY_NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_prompt_shown")
    private val KEY_FORCE_LITE_MODE = booleanPreferencesKey("force_lite_mode")
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/data/Dao.kt` (83 lines)

```kotlin
package com.truong.fridgezero.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FridgeDao {
  @Query("SELECT * FROM fridges LIMIT 1")
  fun observeActiveFridge(): Flow<FridgeEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(fridge: FridgeEntity)
}

@Dao
interface CompartmentDao {
  @Query("SELECT * FROM compartments ORDER BY sort ASC")
  fun observeAll(): Flow<List<CompartmentEntity>>

  @Query("SELECT * FROM compartments WHERE fridgeId = :fridgeId ORDER BY sort ASC")
  fun observeByFridge(fridgeId: String): Flow<List<CompartmentEntity>>

  @Query("SELECT * FROM compartments WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): CompartmentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CompartmentEntity>)

  @Query("DELETE FROM compartments WHERE fridgeId = :fridgeId")
  suspend fun deleteByFridge(fridgeId: String)

  @Transaction
  suspend fun replaceForFridge(fridgeId: String, items: List<CompartmentEntity>) {
    deleteByFridge(fridgeId)
    insertAll(items)
  }
}

@Dao
interface FoodItemDao {
  @Query("SELECT * FROM food_items WHERE status = :status ORDER BY expiresAtEpochDay ASC")
  fun observeByStatus(status: ItemStatus = ItemStatus.ACTIVE): Flow<List<FoodItemEntity>>

  @Query("SELECT * FROM food_items WHERE status = :status")
  suspend fun getByStatusSnapshot(status: ItemStatus = ItemStatus.ACTIVE): List<FoodItemEntity>

  @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): FoodItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<FoodItemEntity>)

  @Upsert
  suspend fun upsert(item: FoodItemEntity): Long

  @Query("UPDATE food_items SET status = :status WHERE id = :id")
  suspend fun updateStatus(id: Long, status: ItemStatus)

  @Query("UPDATE food_items SET status = :status WHERE id IN (:ids)")
  suspend fun updateStatusForIds(ids: List<Long>, status: ItemStatus)

  @Query("UPDATE food_items SET compartmentId = :compartmentId WHERE id = :id")
  suspend fun moveToCompartment(id: Long, compartmentId: String)

  @Query("UPDATE food_items SET compartmentId = :compartmentId WHERE id IN (:ids)")
  suspend fun moveToCompartmentForIds(ids: List<Long>, compartmentId: String)

  @Query("UPDATE food_items SET compartmentId = :fallbackId WHERE status = :status AND compartmentId NOT IN (:validIds)")
  suspend fun reassignOrphanedItems(
    validIds: List<String>,
    fallbackId: String,
    status: ItemStatus = ItemStatus.ACTIVE
  )

  @Query("DELETE FROM food_items WHERE id = :id")
  suspend fun deleteById(id: Long)
}
```

### `app/src/main/kotlin/com/truong/fridgezero/data/Db.kt` (74 lines)

```kotlin
package com.truong.fridgezero.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DbConverters {
  @TypeConverter
  fun fromKind(kind: CompartmentKind): String = kind.name

  @TypeConverter
  fun toKind(value: String): CompartmentKind =
    runCatching { CompartmentKind.valueOf(value) }.getOrDefault(CompartmentKind.DOOR)

  @TypeConverter
  fun fromStatus(status: ItemStatus): String = status.name

  @TypeConverter
  fun toStatus(value: String): ItemStatus =
    runCatching { ItemStatus.valueOf(value) }.getOrDefault(ItemStatus.ACTIVE)
}

@Database(
  entities = [FridgeEntity::class, CompartmentEntity::class, FoodItemEntity::class],
  version = 3,
  exportSchema = false
)
@TypeConverters(DbConverters::class)
abstract class FridgeDb : RoomDatabase() {

  abstract fun fridgeDao(): FridgeDao
  abstract fun compartmentDao(): CompartmentDao
  abstract fun foodItemDao(): FoodItemDao

  companion object {
    private const val DB_NAME = "fridgezero_mvp.db"

    @Volatile
    private var INSTANCE: FridgeDb? = null

    fun get(context: Context): FridgeDb {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
      }
    }

    private fun build(appContext: Context): FridgeDb {
      return Room.databaseBuilder(appContext, FridgeDb::class.java, DB_NAME)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(SeedCallback(appContext))
        .build()
    }
  }

  private class SeedCallback(private val appContext: Context) : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      super.onCreate(db)
      CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        val instance = get(appContext)
        instance.fridgeDao().upsert(Seed.defaultFridge)
        instance.compartmentDao().insertAll(Seed.compartments)
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/data/Entities.kt` (57 lines)

```kotlin
package com.truong.fridgezero.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CompartmentKind {
  DOOR,
  DRAWER
}

enum class ItemStatus {
  ACTIVE,
  USED_UP,
  DISCARDED
}

@Entity(tableName = "fridges")
data class FridgeEntity(
  @PrimaryKey val id: String,
  val name: String,
  val template: String,
  val colorKey: String,
  val handleStyle: String
)

@Entity(tableName = "compartments")
data class CompartmentEntity(
  @PrimaryKey val id: String,
  val fridgeId: String,
  val name: String,
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  val kind: CompartmentKind,
  val shelves: Int,
  val sort: Int
)

@Entity(tableName = "food_items")
data class FoodItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val compartmentId: String,
  val name: String,
  val iconKey: String,
  val group: String,
  val quantity: Int = 1,
  val unit: String? = null,
  val addedAtEpochDay: Long,
  val expiresAtEpochDay: Long,
  val note: String? = null,
  val price: Long? = null,
  val isOpened: Boolean = false,
  val status: ItemStatus = ItemStatus.ACTIVE,
  val source: String = "manual"
)
```

### `app/src/main/kotlin/com/truong/fridgezero/data/Repository.kt` (114 lines)

```kotlin
package com.truong.fridgezero.data

import com.truong.fridgezero.domain.StorageZone
import com.truong.fridgezero.domain.zoneForCompartment
import kotlinx.coroutines.flow.Flow

class FridgeRepository(
  private val fridgeDao: FridgeDao,
  private val compartmentDao: CompartmentDao,
  private val foodItemDao: FoodItemDao
) {
  fun observeActiveFridge(): Flow<FridgeEntity?> = fridgeDao.observeActiveFridge()

  fun observeCompartments(fridgeId: String = Seed.DEFAULT_FRIDGE_ID): Flow<List<CompartmentEntity>> =
    compartmentDao.observeByFridge(fridgeId)

  fun observeFoodItems(): Flow<List<FoodItemEntity>> =
    foodItemDao.observeByStatus(ItemStatus.ACTIVE)

  suspend fun getCompartment(id: String): CompartmentEntity? = compartmentDao.getById(id)

  suspend fun getFoodItem(id: Long): FoodItemEntity? = foodItemDao.getById(id)

  suspend fun upsertFoodItem(item: FoodItemEntity): Long = foodItemDao.upsert(item)

  suspend fun updateItemStatus(id: Long, status: ItemStatus) =
    foodItemDao.updateStatus(id, status)

  suspend fun updateItemsStatus(ids: List<Long>, status: ItemStatus) {
    if (ids.isNotEmpty()) {
      foodItemDao.updateStatusForIds(ids, status)
    }
  }

  suspend fun moveItemToCompartment(id: Long, compartmentId: String) =
    foodItemDao.moveToCompartment(id, compartmentId)

  suspend fun moveItemsToCompartment(ids: List<Long>, compartmentId: String) {
    if (ids.isNotEmpty()) {
      foodItemDao.moveToCompartmentForIds(ids, compartmentId)
    }
  }

  suspend fun deleteFoodItem(id: Long) = foodItemDao.deleteById(id)

  suspend fun applyTemplate(
    template: String,
    colorKey: String,
    handleStyle: String,
    fridgeId: String = Seed.DEFAULT_FRIDGE_ID
  ) {
    fridgeDao.upsert(
      Seed.defaultFridge.copy(
        id = fridgeId,
        template = template,
        colorKey = colorKey,
        handleStyle = handleStyle
      )
    )
    val nextCompartments = Seed.compartmentsForTemplate(template, fridgeId)
    compartmentDao.replaceForFridge(fridgeId, nextCompartments)
    reassignOrphanedActiveItems(nextCompartments)
  }

  suspend fun saveCustomFridge(
    fridge: FridgeEntity,
    compartments: List<CompartmentEntity>,
    includeSampleItems: Boolean = false
  ) {
    fridgeDao.upsert(fridge)
    compartmentDao.replaceForFridge(fridge.id, compartments)
    reassignOrphanedActiveItems(compartments)

    if (includeSampleItems && compartments.isNotEmpty() &&
      foodItemDao.getByStatusSnapshot(ItemStatus.ACTIVE).isEmpty()
    ) {
      val validIds = compartments.map { it.id }.toSet()
      val chilledId = preferredChilledCompartment(compartments).id
      val drawerId = compartments.firstOrNull { zoneForCompartment(it) == StorageZone.DRAWER }?.id ?: chilledId
      val freezerId = compartments.firstOrNull { zoneForCompartment(it) == StorageZone.FREEZER }?.id ?: chilledId
      val mappedSeed = Seed.foodItems().map { item ->
        if (item.compartmentId in validIds) {
          item
        } else {
          val mappedCompId = when (item.compartmentId) {
            "freezer" -> freezerId
            "crisper" -> drawerId
            else -> chilledId
          }
          item.copy(compartmentId = mappedCompId)
        }
      }
      foodItemDao.insertAll(mappedSeed)
    }
  }

  private suspend fun reassignOrphanedActiveItems(compartments: List<CompartmentEntity>) {
    if (compartments.isEmpty()) return
    val validIds = compartments.map { it.id }
    val fallback = preferredChilledCompartment(compartments)
    foodItemDao.reassignOrphanedItems(validIds = validIds, fallbackId = fallback.id)
  }

  private fun preferredChilledCompartment(compartments: List<CompartmentEntity>): CompartmentEntity =
    compartments.firstOrNull { zoneForCompartment(it) == StorageZone.CHILLED }
      ?: compartments.first()

  suspend fun populateSampleData() {
    fridgeDao.upsert(Seed.defaultFridge)
    compartmentDao.insertAll(Seed.compartments)
    foodItemDao.insertAll(Seed.foodItems())
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/data/Seed.kt` (90 lines)

```kotlin
package com.truong.fridgezero.data

import com.truong.fridgezero.ui.icons.EmojiIconRepository
import java.time.LocalDate

object Seed {

  const val DEFAULT_FRIDGE_ID = "default_fridge"

  val defaultFridge = FridgeEntity(
    id = DEFAULT_FRIDGE_ID,
    name = "Tủ lạnh chính",
    template = "top_freezer",
    colorKey = "inox",
    handleStyle = "recessed"
  )

  fun compartmentsForTemplate(
    template: String,
    fridgeId: String = DEFAULT_FRIDGE_ID
  ): List<CompartmentEntity> = when (template) {
    "single" -> listOf(
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.04f, 0.03f, 0.92f, 0.72f, CompartmentKind.DOOR, 4, 0),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.765f, 0.92f, 0.21f, CompartmentKind.DRAWER, 1, 1)
    )
    "bottom_freezer" -> listOf(
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.04f, 0.02f, 0.92f, 0.48f, CompartmentKind.DOOR, 3, 0),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.513f, 0.92f, 0.20f, CompartmentKind.DRAWER, 1, 1),
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.726f, 0.92f, 0.254f, CompartmentKind.DRAWER, 2, 2)
    )
    "side_by_side" -> listOf(
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.02f, 0.41f, 0.96f, CompartmentKind.DOOR, 4, 0),
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.465f, 0.02f, 0.495f, 0.73f, CompartmentKind.DOOR, 3, 1),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.465f, 0.765f, 0.495f, 0.215f, CompartmentKind.DRAWER, 1, 2)
    )
    "french_door" -> listOf(
      CompartmentEntity("fresh_left", fridgeId, "Ngăn mát trái", 0.04f, 0.02f, 0.453f, 0.50f, CompartmentKind.DOOR, 3, 0),
      CompartmentEntity("fresh_right", fridgeId, "Ngăn mát phải", 0.507f, 0.02f, 0.453f, 0.50f, CompartmentKind.DOOR, 3, 1),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.533f, 0.92f, 0.19f, CompartmentKind.DRAWER, 1, 2),
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.736f, 0.92f, 0.244f, CompartmentKind.DRAWER, 2, 3)
    )
    "mini" -> listOf(
      CompartmentEntity("freezer", fridgeId, "Ngăn đá", 0.05f, 0.04f, 0.90f, 0.26f, CompartmentKind.DOOR, 1, 0),
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.05f, 0.32f, 0.90f, 0.64f, CompartmentKind.DOOR, 2, 1)
    )
    else -> listOf( // top_freezer
      CompartmentEntity("freezer", fridgeId, "Ngăn đông", 0.04f, 0.02f, 0.92f, 0.25f, CompartmentKind.DOOR, 2, 0),
      CompartmentEntity("fresh", fridgeId, "Ngăn mát", 0.04f, 0.28f, 0.92f, 0.48f, CompartmentKind.DOOR, 3, 1),
      CompartmentEntity("crisper", fridgeId, "Ngăn rau củ", 0.04f, 0.773f, 0.92f, 0.207f, CompartmentKind.DRAWER, 1, 2)
    )
  }

  val compartments: List<CompartmentEntity>
    get() = compartmentsForTemplate("top_freezer")

  fun foodItems(todayEpochDay: Long = LocalDate.now().toEpochDay()): List<FoodItemEntity> {
    fun item(compartmentId: String, name: String, iconKey: String, daysLeft: Long) =
      FoodItemEntity(
        compartmentId = compartmentId,
        name = name,
        iconKey = iconKey,
        group = EmojiIconRepository.groupFor(iconKey).name,
        quantity = 1,
        addedAtEpochDay = todayEpochDay,
        expiresAtEpochDay = todayEpochDay + daysLeft,
        status = ItemStatus.ACTIVE,
        source = "seed"
      )

    return listOf(
      item("freezer", "Tôm", "shrimp", 40),
      item("freezer", "Kem", "ice_cream", 3),
      item("freezer", "Bánh bao", "dumpling", 25),
      item("freezer", "Cá hồi", "salmon", 1),
      item("fresh", "Sữa", "milk", 4),
      item("fresh", "Trứng", "egg", 12),
      item("fresh", "Phô mai", "cheese", 9),
      item("fresh", "Nước cam", "orange_juice", 2),
      item("fresh", "Thịt gà", "chicken", 1),
      item("fresh", "Sữa chua", "yogurt", 6),
      item("fresh", "Bơ", "butter", 30),
      item("fresh", "Sốt cà", "tomato_sauce", 20),
      item("fresh", "Dưa muối", "pickle", 15),
      item("crisper", "Cà rốt", "carrot", 8),
      item("crisper", "Xà lách", "lettuce", 2),
      item("crisper", "Ớt chuông", "bell_pepper", 4)
    )
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/domain/CulinaryGuide.kt` (1225 lines)

```kotlin
package com.truong.fridgezero.domain

import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.ui.icons.FoodGroup
import java.text.Normalizer
import java.util.Locale

enum class CulinaryFilter(val labelVi: String) {
  ALL("Tất cả"),
  READY_NOW("Đủ đồ nấu ngay"),
  RESCUE_URGENT("Cứu đồ sắp hết hạn"),
  QUICK_15("Nhanh ≤ 15p"),
  VIET_HOME("Cơm nhà Việt"),
  EAT_CLEAN("Eat-Clean")
}

typealias RecipeCategory = CulinaryFilter

enum class RecipeDifficulty {
  EASY,
  MEDIUM
}

enum class MatchTier {
  EXACT_ICON,
  KEYWORD,
  GROUP_FALLBACK
}

data class InventoryDeduction(
  val itemId: Long,
  val remainingQuantity: Int
)

data class RecipeIngredient(
  val name: String,
  val iconKeys: List<String>,
  val keywords: List<String>,
  val fallbackGroup: FoodGroup,
  val amountText: String,
  val unitsToConsume: Int = 1
)

data class CulinaryRecipe(
  val id: String,
  val title: String,
  val emoji: String,
  val summary: String,
  val cookMinutes: Int,
  val servings: Int = 2,
  val categories: Set<CulinaryFilter>,
  val fridgeIngredients: List<RecipeIngredient>,
  val pantryStaples: List<String>,
  val steps: List<String>,
  val sourceName: String,
  val sourceUrl: String,
  val difficulty: RecipeDifficulty = if (cookMinutes <= 15) RecipeDifficulty.EASY else RecipeDifficulty.MEDIUM
)

data class MatchedIngredient(
  val ingredient: RecipeIngredient,
  val item: DisplayItem,
  val tier: MatchTier
) {
  val isUrgent: Boolean get() = item.minDaysLeft <= CulinaryGuideEngine.URGENT_DAYS_THRESHOLD
}

data class RecipeRecommendation(
  val recipe: CulinaryRecipe,
  val matchedIngredients: List<MatchedIngredient>,
  val missingIngredients: List<RecipeIngredient>,
  val score: Int
) {
  val matchedCount: Int get() = matchedIngredients.size
  val totalFridgeIngredients: Int get() = recipe.fridgeIngredients.size
  val isReadyToCook: Boolean get() = recipe.fridgeIngredients.isNotEmpty() && missingIngredients.isEmpty()
  val urgentMatchedCount: Int get() = matchedIngredients.count { it.isUrgent }
  val freshMatchedCount: Int get() = matchedIngredients.count { !it.isUrgent }
  val hasUrgentMatch: Boolean get() = urgentMatchedCount > 0
  val minUrgentDaysLeft: Int? get() = matchedIngredients.minOfOrNull { it.item.minDaysLeft }
}

/**
 * 3-layer ingredient matcher and expiry-rescue ranker:
 * Layer 1: Exact `iconKey` -> Layer 2: Vietnamese name keyword -> Layer 3: `FoodGroup` fallback.
 */
object CulinaryGuideEngine {

  const val URGENT_DAYS_THRESHOLD = 1
  const val SCORE_URGENT_MATCH = 15
  const val SCORE_FRESH_MATCH = 6
  const val SCORE_MISSING_PENALTY = -3
  const val SCORE_READY_BONUS = 8

  private val VI_LOCALE = Locale.forLanguageTag("vi-VN")
  private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{Nd}]+")
  private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

  fun recommend(
    fridgeItems: List<DisplayItem>,
    filter: CulinaryFilter = CulinaryFilter.ALL,
    recipes: List<CulinaryRecipe> = CulinaryGuideRepository.recipes
  ): List<RecipeRecommendation> {
    return recipes
      .map { evaluate(it, fridgeItems) }
      .filter { rec ->
        when (filter) {
          CulinaryFilter.ALL -> true
          CulinaryFilter.READY_NOW -> rec.isReadyToCook
          CulinaryFilter.RESCUE_URGENT -> rec.hasUrgentMatch
          CulinaryFilter.QUICK_15 ->
            rec.recipe.cookMinutes <= 15 || CulinaryFilter.QUICK_15 in rec.recipe.categories
          CulinaryFilter.VIET_HOME -> CulinaryFilter.VIET_HOME in rec.recipe.categories
          CulinaryFilter.EAT_CLEAN -> CulinaryFilter.EAT_CLEAN in rec.recipe.categories
        }
      }
      .sortedWith(
        compareByDescending<RecipeRecommendation> { it.score }
          .thenByDescending { it.isReadyToCook }
          .thenByDescending { it.urgentMatchedCount }
          .thenByDescending { rec -> rec.matchedIngredients.count { it.tier == MatchTier.EXACT_ICON } }
          .thenByDescending { rec ->
            rec.matchedIngredients.count {
              it.tier == MatchTier.EXACT_ICON || it.tier == MatchTier.KEYWORD
            }
          }
          .thenBy { it.recipe.cookMinutes }
          .thenBy { it.recipe.id }
      )
  }

  fun evaluate(
    recipe: CulinaryRecipe,
    fridgeItems: List<DisplayItem>
  ): RecipeRecommendation {
    // Track consumed candidates by index rather than primaryId so in-memory items with id=0 stay distinct.
    val candidates = fridgeItems
      .filter { it.quantity > 0 }
      .withIndex()
      .sortedWith(compareBy<IndexedValue<DisplayItem>> { it.value.minDaysLeft }.thenBy { it.index })

    val usedIndices = mutableSetOf<Int>()
    val matchedByIngredient = arrayOfNulls<MatchedIngredient>(recipe.fridgeIngredients.size)

    assignTier(
      recipe = recipe,
      candidates = candidates,
      usedIndices = usedIndices,
      matchedByIngredient = matchedByIngredient,
      tier = MatchTier.EXACT_ICON
    ) { ingredient, item ->
      val itemKey = item.iconKey.trim().lowercase(Locale.ROOT)
      ingredient.iconKeys.any { it.trim().lowercase(Locale.ROOT) == itemKey }
    }

    assignTier(
      recipe = recipe,
      candidates = candidates,
      usedIndices = usedIndices,
      matchedByIngredient = matchedByIngredient,
      tier = MatchTier.KEYWORD
    ) { ingredient, item ->
      matchesKeyword(item.name, ingredient.keywords)
    }

    assignTier(
      recipe = recipe,
      candidates = candidates,
      usedIndices = usedIndices,
      matchedByIngredient = matchedByIngredient,
      tier = MatchTier.GROUP_FALLBACK
    ) { ingredient, item ->
      item.group == ingredient.fallbackGroup
    }

    val matchedList = mutableListOf<MatchedIngredient>()
    val missingList = mutableListOf<RecipeIngredient>()
    recipe.fridgeIngredients.forEachIndexed { idx, ingredient ->
      val matched = matchedByIngredient[idx]
      if (matched != null) matchedList.add(matched) else missingList.add(ingredient)
    }

    val urgentCount = matchedList.count { it.isUrgent }
    val freshCount = matchedList.size - urgentCount
    val missingCount = missingList.size
    val isReady = recipe.fridgeIngredients.isNotEmpty() && missingCount == 0
    val score = (urgentCount * SCORE_URGENT_MATCH) +
      (freshCount * SCORE_FRESH_MATCH) +
      (missingCount * SCORE_MISSING_PENALTY) +
      if (isReady) SCORE_READY_BONUS else 0

    return RecipeRecommendation(
      recipe = recipe,
      matchedIngredients = matchedList,
      missingIngredients = missingList,
      score = score
    )
  }

  /**
   * Computes earliest-expiry quantity decrements across active Room rows backing matched items.
   * Returned entries with `remainingQuantity <= 0` should be transitioned to [ItemStatus.USED_UP].
   */
  fun planDeductions(
    matched: List<MatchedIngredient>,
    entitiesById: Map<Long, FoodItemEntity>
  ): List<InventoryDeduction> {
    if (matched.isEmpty() || entitiesById.isEmpty()) return emptyList()
    val originalQty = entitiesById
      .filterValues { it.status == ItemStatus.ACTIVE && it.quantity > 0 }
      .mapValues { it.value.quantity }
    val remainingQty = originalQty.toMutableMap()

    matched.forEach { entry ->
      var need = entry.ingredient.unitsToConsume.coerceAtLeast(1)
      val orderedIds = entry.item.itemIds
        .distinct()
        .filter { (remainingQty[it] ?: 0) > 0 }
        .sortedWith(compareBy<Long> { entitiesById[it]?.expiresAtEpochDay ?: Long.MAX_VALUE }.thenBy { it })

      for (id in orderedIds) {
        if (need <= 0) break
        val available = remainingQty[id] ?: 0
        if (available <= 0) continue
        val consumed = minOf(available, need)
        remainingQty[id] = available - consumed
        need -= consumed
      }
    }

    return remainingQty.entries.mapNotNull { (id, remaining) ->
      val initial = originalQty[id] ?: return@mapNotNull null
      if (remaining < initial) InventoryDeduction(itemId = id, remainingQuantity = remaining) else null
    }
  }

  private inline fun assignTier(
    recipe: CulinaryRecipe,
    candidates: List<IndexedValue<DisplayItem>>,
    usedIndices: MutableSet<Int>,
    matchedByIngredient: Array<MatchedIngredient?>,
    tier: MatchTier,
    predicate: (RecipeIngredient, DisplayItem) -> Boolean
  ) {
    recipe.fridgeIngredients.forEachIndexed { idx, ingredient ->
      if (matchedByIngredient[idx] != null) return@forEachIndexed
      val match = candidates.firstOrNull { candidate ->
        candidate.index !in usedIndices && predicate(ingredient, candidate.value)
      } ?: return@forEachIndexed
      usedIndices.add(match.index)
      matchedByIngredient[idx] = MatchedIngredient(ingredient, match.value, tier)
    }
  }

  private fun matchesKeyword(itemName: String, keywords: List<String>): Boolean {
    val normNameAccented = normalizeAccented(itemName)
    if (normNameAccented.isEmpty()) return false
    val normNameStripped = stripVietnameseAccents(itemName)

    return keywords.any { rawKw ->
      val normKwAccented = normalizeAccented(rawKw)
      if (normKwAccented.isEmpty()) return@any false
      if (normNameAccented.contains(normKwAccented)) return@any true

      // Only fall back to unaccented matching for multi-word or >=4-char keywords
      // so short Vietnamese pairs like "bơ" (butter) vs "bò" (beef) never collide.
      val trimmedKw = normKwAccented.trim()
      if (' ' in trimmedKw || trimmedKw.length >= 4) {
        val normKwStripped = stripVietnameseAccents(rawKw)
        normKwStripped.isNotEmpty() && normNameStripped.contains(normKwStripped)
      } else {
        false
      }
    }
  }

  private fun normalizeAccented(text: String): String {
    val cleaned = text.lowercase(VI_LOCALE).replace(NON_WORD_REGEX, " ").trim()
    return if (cleaned.isEmpty()) "" else " $cleaned "
  }

  private fun stripVietnameseAccents(text: String): String {
    val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
    val withoutMarks = DIACRITICS_REGEX.replace(nfd, "")
      .replace('đ', 'd')
      .replace('Đ', 'D')
    return normalizeAccented(withoutMarks)
  }
}

/**
 * Curated catalog of 36 practical Vietnamese home-cooking & Eat-Clean recipes
 * covering all 32 `EmojiIconRepository` icon keys.
 */
object CulinaryGuideRepository {

  private const val SRC_SAVOURY_DAYS = "Savoury Days"
  private const val URL_SAVOURY_DAYS = "https://www.savourydays.com/"
  private const val SRC_COOKY = "Cooky"
  private const val URL_COOKY = "https://www.cooky.vn/cach-lam"
  private const val SRC_VNEXPRESS = "VnExpress Ẩm Thực"
  private const val URL_VNEXPRESS = "https://vnexpress.net/doi-song/cooking"
  private const val SRC_DMX = "Điện máy XANH Vào Bếp"
  private const val URL_DMX = "https://www.dienmayxanh.com/vao-bep"
  private const val SRC_SERIOUS_EATS = "Serious Eats"
  private const val URL_SERIOUS_EATS = "https://www.seriouseats.com/"

  private fun ingChicken(amount: String = "300g") = RecipeIngredient(
    name = "Thịt gà / Ức gà",
    iconKeys = listOf("chicken"),
    keywords = listOf("thịt gà", "ức gà", "đùi gà", "cánh gà", "gà"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingMeat(amount: String = "250g", name: String = "Thịt bò / Thịt nạc") = RecipeIngredient(
    name = name,
    iconKeys = listOf("meat"),
    keywords = listOf("thịt bò", "thịt heo", "thịt lợn", "nạc vai", "thăn bò", "thịt bằm", "bò", "heo"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingBacon(amount: String = "200g") = RecipeIngredient(
    name = "Thịt ba chỉ",
    iconKeys = listOf("bacon"),
    keywords = listOf("ba chỉ", "ba rọi", "thịt ba chỉ", "bacon"),
    fallbackGroup = FoodGroup.THIT,
    amountText = amount
  )

  private fun ingSalmon(amount: String = "250g") = RecipeIngredient(
    name = "Cá hồi",
    iconKeys = listOf("salmon"),
    keywords = listOf("cá hồi", "phi lê cá", "salmon"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingShrimp(amount: String = "250g") = RecipeIngredient(
    name = "Tôm tươi",
    iconKeys = listOf("shrimp"),
    keywords = listOf("tôm", "tôm sú", "tôm thẻ", "tôm tươi"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingCrab(amount: String = "200g") = RecipeIngredient(
    name = "Cua / Thịt cua",
    iconKeys = listOf("crab"),
    keywords = listOf("thịt cua", "cua", "cua biển", "ghẹ"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingSquid(amount: String = "250g") = RecipeIngredient(
    name = "Mực tươi",
    iconKeys = listOf("squid"),
    keywords = listOf("mực", "mực ống", "mực lá", "mực tươi"),
    fallbackGroup = FoodGroup.CA_HAI_SAN,
    amountText = amount
  )

  private fun ingLettuce(amount: String = "1 bó") = RecipeIngredient(
    name = "Rau xà lách",
    iconKeys = listOf("lettuce"),
    keywords = listOf("xà lách", "rau xà lách", "rau cải", "rau xanh"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingCarrot(amount: String = "1 củ") = RecipeIngredient(
    name = "Cà rốt",
    iconKeys = listOf("carrot"),
    keywords = listOf("cà rốt", "carrot"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingBellPepper(amount: String = "1 quả") = RecipeIngredient(
    name = "Ớt chuông",
    iconKeys = listOf("bell_pepper"),
    keywords = listOf("ớt chuông", "ớt ngọt", "ớt chuông đỏ"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingBroccoli(amount: String = "1 bông") = RecipeIngredient(
    name = "Bông cải xanh",
    iconKeys = listOf("broccoli"),
    keywords = listOf("bông cải", "súp lơ", "bông cải xanh"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingCorn(amount: String = "1 trái") = RecipeIngredient(
    name = "Bắp ngọt",
    iconKeys = listOf("corn"),
    keywords = listOf("bắp ngọt", "ngô ngọt", "bắp mỹ", "ngô", "bắp"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingMushroom(amount: String = "150g") = RecipeIngredient(
    name = "Nấm tươi",
    iconKeys = listOf("mushroom"),
    keywords = listOf("nấm", "nấm kim châm", "nấm đùi gà", "nấm hương", "nấm rơm"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingPickle(amount: String = "100g") = RecipeIngredient(
    name = "Dưa muối / Dưa chuột",
    iconKeys = listOf("pickle"),
    keywords = listOf("dưa muối", "dưa cải", "dưa chua", "dưa leo", "dưa chuột"),
    fallbackGroup = FoodGroup.RAU,
    amountText = amount
  )

  private fun ingEgg(amount: String = "2 quả") = RecipeIngredient(
    name = "Trứng gà",
    iconKeys = listOf("egg"),
    keywords = listOf("trứng", "trứng gà", "trứng vịt"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingMilk(amount: String = "150ml") = RecipeIngredient(
    name = "Sữa tươi",
    iconKeys = listOf("milk"),
    keywords = listOf("sữa tươi", "sữa không đường", "sữa"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingCheese(amount: String = "2 lát") = RecipeIngredient(
    name = "Phô mai",
    iconKeys = listOf("cheese"),
    keywords = listOf("phô mai", "cheddar", "mozzarella", "cheese"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingYogurt(amount: String = "1 hũ") = RecipeIngredient(
    name = "Sữa chua",
    iconKeys = listOf("yogurt"),
    keywords = listOf("sữa chua", "sữa chua không đường", "yogurt"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingButter(amount: String = "15g") = RecipeIngredient(
    name = "Bơ",
    iconKeys = listOf("butter"),
    keywords = listOf("bơ lạt", "bơ mặn", "bơ thực vật", "bơ"),
    fallbackGroup = FoodGroup.SUA_TRUNG,
    amountText = amount
  )

  private fun ingOrangeJuice(amount: String = "120ml") = RecipeIngredient(
    name = "Nước cam",
    iconKeys = listOf("orange_juice"),
    keywords = listOf("nước cam", "cam ép", "nước ép cam"),
    fallbackGroup = FoodGroup.DO_UONG,
    amountText = amount
  )

  private fun ingWater(amount: String = "300ml") = RecipeIngredient(
    name = "Nước suối",
    iconKeys = listOf("water"),
    keywords = listOf("nước suối", "nước lọc", "nước tinh khiết"),
    fallbackGroup = FoodGroup.DO_UONG,
    amountText = amount
  )

  private fun ingTomatoSauce(amount: String = "3 muỗng") = RecipeIngredient(
    name = "Sốt cà",
    iconKeys = listOf("tomato_sauce"),
    keywords = listOf("sốt cà", "sốt cà chua", "tương cà", "cà chua cô đặc"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingBread(amount: String = "2 ổ / lát") = RecipeIngredient(
    name = "Bánh mì",
    iconKeys = listOf("bread"),
    keywords = listOf("bánh mì", "bánh mì gối", "sandwich", "baguette"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingRice(amount: String = "2 chén") = RecipeIngredient(
    name = "Cơm nguội / Gạo",
    iconKeys = listOf("rice"),
    keywords = listOf("cơm", "cơm nguội", "gạo", "gạo tẻ"),
    fallbackGroup = FoodGroup.DO_KHO,
    amountText = amount
  )

  private fun ingDumpling(amount: String = "3 cái") = RecipeIngredient(
    name = "Bánh bao",
    iconKeys = listOf("dumpling"),
    keywords = listOf("bánh bao", "há cảo", "sủi cảo"),
    fallbackGroup = FoodGroup.AN_SAN,
    amountText = amount
  )

  private fun ingIceCream(amount: String = "2 viên") = RecipeIngredient(
    name = "Kem lạnh",
    iconKeys = listOf("ice_cream"),
    keywords = listOf("kem", "kem vani", "kem tươi"),
    fallbackGroup = FoodGroup.AN_SAN,
    amountText = amount
  )

  private fun ingApple(amount: String = "1 quả") = RecipeIngredient(
    name = "Táo",
    iconKeys = listOf("apple"),
    keywords = listOf("táo", "táo đỏ", "táo xanh"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingBanana(amount: String = "1 quả") = RecipeIngredient(
    name = "Chuối",
    iconKeys = listOf("banana"),
    keywords = listOf("chuối", "chuối chín", "chuối cau"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingGrape(amount: String = "100g") = RecipeIngredient(
    name = "Nho tươi",
    iconKeys = listOf("grape"),
    keywords = listOf("nho", "nho xanh", "nho đỏ", "nho đen"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingStrawberry(amount: String = "100g") = RecipeIngredient(
    name = "Dâu tây",
    iconKeys = listOf("strawberry"),
    keywords = listOf("dâu tây", "dâu", "strawberry"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingWatermelon(amount: String = "300g") = RecipeIngredient(
    name = "Dưa hấu",
    iconKeys = listOf("watermelon"),
    keywords = listOf("dưa hấu", "ruột dưa hấu"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  private fun ingMango(amount: String = "1 quả") = RecipeIngredient(
    name = "Xoài",
    iconKeys = listOf("mango"),
    keywords = listOf("xoài", "xoài xanh", "xoài chín"),
    fallbackGroup = FoodGroup.TRAI_CAY,
    amountText = amount
  )

  val recipes: List<CulinaryRecipe> = listOf(
    // 1–12: Ready out-of-the-box with Seed.kt and prioritize rescuing salmon (1d), chicken (1d), orange_juice (2d), lettuce (2d), ice_cream (3d).
    CulinaryRecipe(
      id = "ga_ap_chao_sot_cam_xa_lach",
      title = "Gà áp chảo sốt cam mật ong & xà lách",
      emoji = "🍗",
      summary = "Ức gà áp chảo vàng cạnh quyện sốt cam chua ngọt dịu nhẹ, ăn cùng xà lách giòn mát.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken(), ingOrangeJuice(), ingLettuce(), ingButter("10g")),
      pantryStaples = listOf("Tỏi băm", "Mật ong", "Muối", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thấm khô thịt gà, ướp với chút muối, tiêu và tỏi băm trong 5 phút.",
        "Làm nóng chảo với dầu ăn, áp chảo gà mỗi mặt 4 phút cho chín vàng đều rồi gắp ra.",
        "Hạ lửa nhỏ, cho bơ, nước cam và 1 muỗng mật ong vào chảo khuấy tới khi sốt hơi sánh.",
        "Xếp xà lách rửa sạch ra đĩa, thái lát gà đặt lên trên và rưới sốt cam ấm nóng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "ca_hoi_sot_cam_bo_toi",
      title = "Cá hồi áp chảo sốt cam bơ tỏi & xà lách",
      emoji = "🐟",
      summary = "Cá hồi da giòn thấm bơ tỏi kết hợp sốt cam thơm thanh, chuẩn bữa Eat-Clean nhanh gọn.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon(), ingOrangeJuice("100ml"), ingButter("15g"), ingLettuce()),
      pantryStaples = listOf("Tỏi đập dập", "Muối", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thấm thật ráo miếng cá hồi rồi rắc đều một lớp muối mỏng và tiêu lên hai mặt.",
        "Áp chảo mặt da trước trên lửa vừa khoảng 3 phút cho giòn, lật mặt áp tiếp 2 phút.",
        "Cho bơ và tỏi đập dập vào chảo rưới đều lên thân cá, sau đó gắp cá đặt lên đĩa xà lách.",
        "Đổ nước cam vào phần bơ tỏi còn lại, đun sôi nhẹ 1 phút cho sánh rồi chan lên cá."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "ca_hoi_ap_chao_bo_toi_salad",
      title = "Cá hồi áp chảo bơ tỏi kèm salad cà rốt",
      emoji = "🥗",
      summary = "Phi lê cá hồi béo ngậy bơ tỏi ăn kèm xà lách và cà rốt bào sợi cân vị.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon(), ingButter("15g"), ingLettuce(), ingCarrot()),
      pantryStaples = listOf("Tỏi băm", "Nước cốt chanh", "Muối", "Tiêu xay", "Dầu ô liu"),
      steps = listOf(
        "Ướp cá hồi với muối tiêu trong 5 phút; xà lách cắt vừa ăn, cà rốt bào sợi mảnh.",
        "Áp chảo cá hồi với ít dầu cho vàng hai mặt rồi thả bơ và tỏi băm vào dậy mùi thơm.",
        "Trộn xà lách và cà rốt với dầu ô liu, nước cốt chanh và chút muối tiêu làm salad nền.",
        "Đặt cá hồi lên trên đĩa salad, rưới thêm bơ tỏi nóng và thưởng thức ngay."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "salad_ga_xe_sua_chua",
      title = "Salad ức gà xé xà lách sốt sữa chua",
      emoji = "🥗",
      summary = "Xốt sữa chua chua dịu thay mayonnaise giúp đĩa salad gà thanh đạm mà vẫn đậm vị.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("250g"), ingLettuce(), ingYogurt(), ingCarrot()),
      pantryStaples = listOf("Nước cốt chanh", "Mật ong", "Muối", "Tiêu xay", "Tỏi băm"),
      steps = listOf(
        "Luộc hoặc áp chảo chín ức gà với nhúm muối, để nguội bớt rồi xé thành sợi vừa ăn.",
        "Rửa sạch xà lách để ráo, cà rốt thái sợi mỏng hoặc hạt lựu nhỏ.",
        "Khuấy đều sữa chua với 1 muỗng mật ong, nước cốt chanh, muối và tiêu làm xốt trộn.",
        "Cho gà xé, xà lách, cà rốt vào tô lớn, rưới xốt sữa chua và đảo nhẹ trước khi ăn."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "ga_xao_ot_chuong_ca_rot",
      title = "Ức gà xào ớt chuông cà rốt kiểu cơm nhà",
      emoji = "🫑",
      summary = "Món xào nhiều màu sắc giàu đạm, thịt gà mềm ngọt hòa cùng ớt chuông giòn tươi.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken(), ingBellPepper(), ingCarrot()),
      pantryStaples = listOf("Hành tỏi băm", "Dầu hào", "Nước mắm", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thái gà miếng vừa ăn, ướp nhanh với tỏi băm, 1 muỗng dầu hào và nửa muỗng tiêu.",
        "Thái vuông ớt chuông và tỉa lát mỏng cà rốt để khi xào chín đồng đều.",
        "Xào nhanh thịt gà trên lửa lớn cho săn chín khoảng 80% rồi trút ra đĩa.",
        "Phi thơm hành tỏi, xào cà rốt và ớt chuông 2 phút rồi trút gà vào đảo chung, nêm nước mắm vừa ăn."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "salad_ca_hoi_dua_muoi",
      title = "Salad cá hồi áp chảo dưa muối thanh vị",
      emoji = "🥬",
      summary = "Chút chua giòn của dưa muối kết hợp cá hồi béo thơm giúp chống ngấy cực hiệu quả.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSalmon(), ingLettuce(), ingPickle("80g"), ingCarrot()),
      pantryStaples = listOf("Dầu ô liu", "Đường", "Muối", "Tiêu xay", "Hành tím"),
      steps = listOf(
        "Vắt nhẹ dưa muối cho ráo nước và thái nhỏ vừa ăn; cà rốt bào sợi, xà lách cắt khúc.",
        "Áp chảo cá hồi với muối tiêu đến khi chín vàng hai mặt, tách miếng vừa gắp.",
        "Pha nhanh dầu ô liu, chút đường và tiêu thành nước trộn salad cân vị.",
        "Xếp xà lách, cà rốt, dưa muối ra tô, đặt cá hồi lên trên và rưới sốt trộn đều."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "kem_sua_chua_cam_tuoi",
      title = "Kem sữa chua sốt cam tươi mát lạnh",
      emoji = "🍨",
      summary = "Tráng miệng 5 phút giải cứu ngay hộp kem lạnh, nước cam và sữa chua trong tủ.",
      cookMinutes = 5,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingIceCream(), ingOrangeJuice("80ml"), ingYogurt()),
      pantryStaples = listOf("Mật ong", "Lá bạc hà hoặc vỏ cam bào"),
      steps = listOf(
        "Đun ấm nước cam với nửa muỗng mật ong trong 2 phút cho thơm nhẹ rồi để nguội.",
        "Múc sữa chua ra ly hoặc chén lạnh làm lớp nền mịn màng.",
        "Thêm 2 viên kem lạnh lên trên phần sữa chua.",
        "Rưới nước sốt cam nguội lên mặt kem, trang trí thêm vỏ cam bào và thưởng thức ngay."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "trung_chien_pho_mai_sot_ca",
      title = "Trứng chiên phô mai sốt cà chua đậm đà",
      emoji = "🍳",
      summary = "Trứng xốp thơm mùi bơ kẹp nhân phô mai tan chảy và sốt cà chua bắt cơm.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingCheese(), ingTomatoSauce(), ingButter("10g")),
      pantryStaples = listOf("Hành lá", "Nước mắm", "Đường", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Đánh tan trứng với hành lá cắt nhỏ, nửa muỗng nước mắm và chút tiêu xay.",
        "Đun chảy bơ trên chảo chống dính lửa nhỏ, đổ trứng vào tráng mỏng đều mặt chảo.",
        "Đặt phô mai lên một nửa mặt trứng, rưới sốt cà pha chút đường rồi gập đôi miếng trứng.",
        "Chiên thêm 1 phút cho phô mai chảy đều, rưới phần sốt cà còn lại lên mặt và rắc tiêu."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "tom_rim_bo_toi_sot_ca",
      title = "Tôm rim bơ tỏi sốt cà ăn kèm dưa muối",
      emoji = "🦐",
      summary = "Tôm rim săn chắc áo lớp sốt cà bơ tỏi bóng đẹp, ăn cùng cơm trắng và dưa muối cực đưa cơm.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingShrimp(), ingButter("15g"), ingTomatoSauce(), ingPickle()),
      pantryStaples = listOf("Tỏi băm", "Nước mắm", "Đường", "Tiêu xay", "Hành lá"),
      steps = listOf(
        "Cắt bỏ râu tôm, rút chỉ lưng, rửa sạch để ráo và xóc nhẹ với chút tiêu.",
        "Đun chảy bơ cùng tỏi băm cho dậy thơm rồi cho tôm vào đảo nhanh trên lửa lớn.",
        "Khi tôm chuyển đỏ săn lại, thêm sốt cà, 1 muỗng nước mắm và 1 muỗng nhỏ đường vào rim.",
        "Rim 3 phút đến khi sốt sệt bám đều vỏ tôm, rắc hành lá và dọn kèm đĩa dưa muối."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "trung_bac_sua_tuoi_pho_mai",
      title = "Trứng bác sữa tươi phô mai béo mịn",
      emoji = "🥚",
      summary = "Bữa sáng mềm mịn kiểu Tây nhờ đánh trứng với sữa tươi và nấu chậm cùng bơ phô mai.",
      cookMinutes = 10,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingEgg("3 quả"), ingMilk("60ml"), ingCheese(), ingButter("15g")),
      pantryStaples = listOf("Muối", "Tiêu đen", "Hành lá hoặc ngò rí"),
      steps = listOf(
        "Đập trứng vào tô, thêm sữa tươi và nhúm muối rồi đánh đều cho tan quyện.",
        "Làm tan chảy bơ ở lửa thật nhỏ trên chảo chống dính.",
        "Đổ trứng vào, dùng phới đảo nhẹ liên tục từ ngoài vào trong để tạo các lớp trứng mềm mịn.",
        "Khi trứng còn hơi ướt thì tắt bếp, thả phô mai xé nhỏ vào đảo tan và rắc tiêu đen."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "banh_bao_chien_sot_ca_dua_muoi",
      title = "Bánh bao chiên giòn chấm sốt cà & dưa muối",
      emoji = "🥟",
      summary = "Bánh bao vỏ giòn rụm nóng hổi chấm sốt cà pha đậm vị và kèm dưa muối chống ngấy.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingDumpling("4 cái"), ingTomatoSauce(), ingPickle("80g")),
      pantryStaples = listOf("Dầu ăn", "Tỏi băm", "Đường", "Xì dầu", "Tương ớt"),
      steps = listOf(
        "Cho bánh bao vào chảo dầu nóng vừa (hoặc nồi chiên không dầu), chiên vàng giòn các mặt.",
        "Trong lúc đó phi thơm tỏi băm, cho sốt cà, chút xì dầu và nửa muỗng đường vào đảo sánh.",
        "Vớt bánh bao ra giấy thấm dầu và cắt đôi cho dễ thưởng thức.",
        "Dọn bánh bao nóng kèm chén sốt cà ấm và dưa muối thái vừa ăn."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "tom_xao_ot_chuong_ca_rot",
      title = "Tôm tươi xào ớt chuông cà rốt giòn ngọt",
      emoji = "🦐",
      summary = "Món xào cơm nhà ít dầu mỡ, giữ nguyên vị ngọt giòn tự nhiên của tôm và rau củ.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp(), ingBellPepper(), ingCarrot()),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Hành lá", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Tôm bóc vỏ chừa đuôi, rút chỉ lưng, ướp nhanh với chút tỏi băm và tiêu.",
        "Cà rốt tỉa lát mỏng, ớt chuông bỏ hạt thái miếng vuông vừa đũa.",
        "Xào tôm trên lửa lớn 2 phút đến khi vừa săn cong lại thì trút ra bát.",
        "Xào cà rốt và ớt chuông 2 phút, trút tôm vào đảo cùng 1 muỗng dầu hào và rắc hành lá."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),

    // 13–36: Practical Vietnamese home-cooking & Eat-Clean coverage for all remaining iconKeys.
    CulinaryRecipe(
      id = "ba_chi_rang_chay_canh_dua_muoi",
      title = "Thịt ba chỉ rang cháy cạnh kèm dưa muối & cơm",
      emoji = "🥓",
      summary = "Ba chỉ xém vàng thơm lừng mắm hành, ăn cùng dưa cải muối chua và cơm nóng chuẩn vị.",
      cookMinutes = 20,
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBacon("300g"), ingPickle("150g"), ingRice()),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Đường", "Hành lá", "Tiêu xay"),
      steps = listOf(
        "Thịt ba chỉ thái miếng mỏng vừa ăn, chần sơ nước sôi 1 phút rồi để ráo.",
        "Đặt chảo nóng, đảo thịt trên lửa vừa đến khi ra mỡ và các cạnh cháy xém vàng.",
        "Chắt bớt mỡ thừa, phi thơm hành tím rồi nêm nước mắm, chút đường đảo thấm đều.",
        "Rắc hành lá, tiêu và dọn ăn nóng cùng dưa muối và cơm trắng."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "bo_xao_bong_cai_ca_rot",
      title = "Thịt bò xào bông cải xanh cà rốt",
      emoji = "🥦",
      summary = "Thịt bò mềm mọng xào lửa lớn với bông cải xanh và cà rốt giòn giữ trọn vitamin.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("250g", "Thịt bò"), ingBroccoli(), ingCarrot()),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Xì dầu", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thái mỏng thịt bò ngang thớ, ướp 1 muỗng dầu hào, chút xì dầu, tiêu và nửa phần tỏi băm.",
        "Cắt nhỏ bông cải xanh và thái lát cà rốt, chần qua nước sôi 45 giây rồi vớt ngâm nước mát.",
        "Làm nóng chảo thật già dầu, xào thịt bò thật nhanh tay trên lửa lớn 1 phút rồi trút ra.",
        "Xào thơm phần tỏi còn lại với bông cải, cà rốt rồi đổ thịt bò vào đảo chung 30 giây."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "bo_xao_ot_chuong_nam",
      title = "Thịt bò xào ớt chuông nấm hương",
      emoji = "🥩",
      summary = "Nấm ngọt nước quyện vị thịt bò mềm và ớt chuông thơm lừng cho bữa tối gọn lẹ.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingMeat("250g", "Thịt bò"), ingBellPepper(), ingMushroom()),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Xì dầu", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Thịt bò thái lát mỏng ướp nhanh với xì dầu, tỏi băm và tiêu trong 5 phút.",
        "Nấm làm sạch cắt đôi; ớt chuông bỏ hạt thái miếng vuông vừa ăn.",
        "Xào thịt bò trên lửa lớn cho chín tái rồi vớt ra đĩa riêng.",
        "Xào nấm và ớt chuông với chút dầu hào 2 phút rồi trút bò vào đảo đều, rắc tiêu và tắt bếp."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "muc_xao_ot_chuong_nam",
      title = "Mực xào ớt chuông nấm & cà rốt giòn ngọt",
      emoji = "🦑",
      summary = "Mực khứa vảy rồng giòn sần sật xào nhanh cùng ớt chuông, nấm và cà rốt không ra nước.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSquid(), ingBellPepper(), ingMushroom("100g"), ingCarrot()),
      pantryStaples = listOf("Gừng thái lát", "Tỏi băm", "Dầu hào", "Nước mắm", "Hành lá"),
      steps = listOf(
        "Làm sạch mực, khứa caro nhẹ và cắt miếng vừa, chần nhanh qua nước sôi có vài lát gừng.",
        "Cắt miếng vừa ăn ớt chuông, cà rốt và nấm tươi.",
        "Phi thơm tỏi, xào mực trên lửa lớn 1 phút với nửa muỗng dầu hào rồi vớt ra.",
        "Xào rau củ 2 phút cho chín tới, trút mực vào đảo lại cùng hành lá và chút nước mắm."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "muc_hap_gung_cuon_xa_lach",
      title = "Mực hấp gừng sả cuốn xà lách",
      emoji = "🦑",
      summary = "Mực hấp giữ nguyên độ giòn ngọt tự nhiên, cuốn xà lách cà rốt nhẹ bụng ít calo.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingSquid("300g"), ingLettuce(), ingCarrot()),
      pantryStaples = listOf("Gừng thái sợi", "Sả đập dập", "Nước mắm", "Chanh", "Tỏi ớt"),
      steps = listOf(
        "Rửa sạch mực với gừng giã để khử tanh, để nguyên con hoặc cắt khoanh dày.",
        "Lót sả đập dập và gừng sợi dưới đĩa, xếp mực lên hấp cách thủy khoảng 6–7 phút.",
        "Rửa sạch xà lách và thái cà rốt thành thanh dài để chuẩn bị đồ cuốn.",
        "Pha nước mắm gừng chanh tỏi ớt; dọn mực hấp nóng ăn cuốn kèm xà lách và cà rốt."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "sup_cua_bap_ngot_trung",
      title = "Súp cua bắp ngọt trứng gà sánh mịn",
      emoji = "🦀",
      summary = "Chén súp nóng hổi sánh nhẹ với thịt cua ngọt, hạt bắp vàng giòn và vân trứng mây.",
      cookMinutes = 25,
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingCrab(), ingCorn(), ingEgg(), ingCarrot("1/2 củ")),
      pantryStaples = listOf("Bột năng hoặc bột bắp", "Hành tím băm", "Dầu mè", "Tiêu xay", "Ngò rí"),
      steps = listOf(
        "Tách hạt bắp ngọt, thái hạt lựu nhỏ cà rốt; xào thơm nhẹ thịt cua với hành tím băm.",
        "Đun sôi 800ml nước với bắp và cà rốt trong 8 phút cho ra nước ngọt.",
        "Cho thịt cua vào, nêm muối và hạt nêm rồi khuấy từ từ nước bột năng cho súp sánh nhẹ.",
        "Đánh tan trứng rót thành dòng mảnh vào nồi khuấy theo một chiều, thêm dầu mè, tiêu và ngò."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "com_chien_cua_bap_ngot",
      title = "Cơm chiên thịt cua bắp ngọt trứng vàng",
      emoji = "🦀",
      summary = "Hạt cơm tơi săn áo trứng vàng ươm hòa với thịt cua và bắp ngọt thơm nức mũi.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingCrab("150g"), ingRice(), ingCorn("1/2 trái"), ingEgg()),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Hành lá", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Đánh 1 lòng đỏ trứng trộn sẵn vào cơm nguội để hạt cơm tơi và lên màu vàng đẹp.",
        "Phi thơm hành tím, xào nhanh thịt cua và hạt bắp ngọt với chút tiêu rồi múc ra.",
        "Đổ phần trứng còn lại vào chảo đảo tơi, trút cơm vào rang săn trên lửa vừa.",
        "Cho thịt cua và bắp vào đảo chung 2 phút, nêm chút nước mắm và rắc hành lá trước khi tắt bếp."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "com_chien_duong_chau_tom",
      title = "Cơm chiên Dương Châu tôm trứng bắp ngọt",
      emoji = "🍚",
      summary = "Đĩa cơm chiên đủ sắc màu từ tôm, trứng và hạt bắp ngọt tận dụng hoàn hảo cơm nguội.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingRice(), ingShrimp("150g"), ingEgg(), ingCorn("1/2 trái")),
      pantryStaples = listOf("Tỏi băm", "Xì dầu", "Dầu ăn", "Hành lá", "Tiêu xay"),
      steps = listOf(
        "Tôm bóc vỏ cắt hạt lựu; luộc sơ hạt bắp ngọt khoảng 2 phút cho chín mềm.",
        "Làm nóng dầu, chiên tơi trứng gà rồi trút cơm nguội vào đảo đều cho hạt cơm ráo tơi.",
        "Đẩy cơm sang một bên chảo, xào chín tôm và bắp với chút tỏi băm rồi trộn chung.",
        "Nêm xì dầu vừa miệng, đảo thêm 1 phút trên lửa lớn và rắc hành lá, tiêu xay."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "com_chien_ba_chi_dua_muoi",
      title = "Cơm chiên thịt ba chỉ dưa cải muối",
      emoji = "🍚",
      summary = "Vị chua mặn của dưa cải muối giúp đĩa cơm rang thịt ba chỉ trứng gà tròn vị không ngán.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingRice(), ingBacon("150g"), ingPickle("100g"), ingEgg()),
      pantryStaples = listOf("Tỏi băm", "Nước mắm", "Hành lá", "Tiêu xay", "Đường"),
      steps = listOf(
        "Thịt ba chỉ thái hạt lựu nhỏ, dưa muối vắt ráo thái vừa; đảo ba chỉ xém cạnh để lấy mỡ thơm.",
        "Cho tỏi băm và dưa muối vào xào săn cùng chút đường rồi múc bớt ra.",
        "Đập trứng vào chảo đảo nhanh tay cùng cơm nguội cho hạt cơm khô tơi.",
        "Trút dưa muối và ba chỉ vào đảo chung 2 phút, nêm chút nước mắm, rắc hành lá và tiêu."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "canh_thit_bam_nam_bong_cai",
      title = "Canh thịt bằm nấm bông cải cà rốt thanh ngọt",
      emoji = "🍲",
      summary = "Bát canh rau củ thanh mát, ngọt tự nhiên từ nấm, bông cải, cà rốt và thịt nạc bằm.",
      cookMinutes = 20,
      categories = setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMeat("150g", "Thịt heo bằm"), ingMushroom(), ingBroccoli("1/2 bông"), ingCarrot()),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Muối", "Hành lá", "Tiêu xay"),
      steps = listOf(
        "Ướp thịt bằm với hành tím, chút nước mắm và tiêu; cắt vừa ăn nấm, bông cải và cà rốt.",
        "Xào săn thịt bằm với nhúm hành tím trong nồi rồi chế khoảng 800ml nước đun sôi.",
        "Hớt bọt, thả cà rốt vào nấu 3 phút rồi cho tiếp bông cải và nấm vào.",
        "Nấu thêm 2–3 phút đến khi rau chín tới, nêm muối vừa ăn, rắc hành lá và tiêu."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "ba_chi_cuon_nam_ap_chao",
      title = "Thịt ba chỉ cuộn nấm áp chảo cuốn xà lách",
      emoji = "🍄",
      summary = "Nấm ngọt mọng gói gọn trong lát ba chỉ áp chảo cháy cạnh, ăn cuốn xà lách cực cuốn.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBacon(), ingMushroom("200g"), ingLettuce()),
      pantryStaples = listOf("Xì dầu", "Tỏi băm", "Mật ong", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Nấm cắt gốc rửa sạch để ráo; trải lát ba chỉ mỏng cuộn chặt quanh từng bó nấm nhỏ.",
        "Pha xốt quét gồm 2 muỗng xì dầu, 1 muỗng mật ong, tỏi băm và ít tiêu.",
        "Áp chảo các cuộn thịt nấm ở lửa vừa cho xém vàng đều các mặt không cần nhiều dầu.",
        "Phết đều xốt lên cuộn thịt trong 1 phút cuối cho bóng đẹp, dọn cuốn với xà lách tươi."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "ga_ham_nam_bap_ngot",
      title = "Gà hầm nấm cà rốt bắp ngọt bổ dưỡng",
      emoji = "🍄",
      summary = "Nồi gà hầm thanh ngọt đậm vị nấm, cà rốt và bắp thích hợp cho bữa cơm gia đình ấm áp.",
      cookMinutes = 30,
      categories = setOf(CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingChicken("400g"), ingMushroom(), ingCarrot(), ingCorn()),
      pantryStaples = listOf("Hành tím", "Gừng đập dập", "Nước mắm", "Muối", "Hành ngò"),
      steps = listOf(
        "Chặt gà miếng vừa ăn, ướp với chút muối, nước mắm và hành tím trong 10 phút.",
        "Cắt khúc bắp ngọt, thái khoanh cà rốt và làm sạch nấm tươi.",
        "Xào săn thịt gà với hành tím và gừng, đổ nước ngập mặt rồi hầm lửa vừa 12 phút.",
        "Cho cà rốt, bắp ngọt và nấm vào hầm thêm 10 phút đến khi mềm ngọt, nêm vừa ăn và rắc hành ngò."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "tom_xao_bong_cai_toi",
      title = "Tôm xào bông cải xanh tỏi thơm Eat-Clean",
      emoji = "🥦",
      summary = "Bữa tối ít béo chỉ 12 phút với tôm săn giòn cùng bông cải xanh và cà rốt.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingShrimp(), ingBroccoli(), ingCarrot("1/2 củ")),
      pantryStaples = listOf("Tỏi băm", "Dầu hào", "Muối", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Bóc vỏ tôm chừa đuôi, xóc với chút muối tiêu; tách bông cải miếng nhỏ và thái lát cà rốt.",
        "Chần nhanh bông cải và cà rốt trong nước sôi 45 giây rồi vớt ra giữ màu xanh.",
        "Phi thơm tỏi băm với ít dầu, xào tôm trên lửa lớn 2 phút đến khi vừa chuyển đỏ.",
        "Trút bông cải và cà rốt vào đảo đều 1 phút với nửa muỗng dầu hào và rắc tiêu."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "banh_mi_chao_trung_sot_ca",
      title = "Bánh mì chảo trứng ốp la sốt cà bơ thơm",
      emoji = "🥖",
      summary = "Bữa sáng kiểu tiệm nóng hổi ngay tại nhà với bánh mì giòn chấm sốt cà chua bơ béo.",
      cookMinutes = 12,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(ingBread("1 ổ"), ingEgg("2 quả"), ingTomatoSauce(), ingButter("15g")),
      pantryStaples = listOf("Tỏi băm", "Đường", "Xì dầu", "Tiêu xay", "Ngò rí"),
      steps = listOf(
        "Đun chảy bơ với chút tỏi băm trên chảo nhỏ, cho sốt cà và nửa muỗng đường vào đun sánh.",
        "Tạo hai khoảng trống trên chảo sốt rồi đập 2 quả trứng gà vào ốp la nhẹ tay.",
        "Đậy nắp khoảng 2 phút trên lửa nhỏ để lòng trắng chín mượt còn lòng đỏ vẫn dẻo thơm.",
        "Rắc tiêu, ngò rí lên mặt chảo, rưới chút xì dầu và chấm nóng với bánh mì nướng giòn."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "banh_mi_kep_ba_chi_pho_mai",
      title = "Bánh mì kẹp thịt ba chỉ phô mai xà lách",
      emoji = "🥪",
      summary = "Sandwich kẹp nhanh 10 phút với ba chỉ áp chảo giòn, phô mai tan nhẹ và xà lách tươi.",
      cookMinutes = 10,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingBread("2 lát"), ingBacon("120g"), ingCheese(), ingLettuce("3 lá")),
      pantryStaples = listOf("Tiêu đen", "Tương cà hoặc tương ớt", "Dầu ăn"),
      steps = listOf(
        "Áp chảo thịt ba chỉ trên lửa vừa cho xém giòn hai mặt rồi thấm bớt dầu.",
        "Áp nóng nhanh hai lát bánh mì trên chính chảo vừa chiên để bánh giòn thơm.",
        "Xếp xà lách khô ráo lên lát bánh dưới, đặt ba chỉ nóng và lát phô mai lên trên cho chảy nhẹ.",
        "Phết thêm chút tương cà hoặc tương ớt, rắc tiêu và kẹp lát bánh còn lại."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "banh_mi_bo_toi_nam_pho_mai",
      title = "Bánh mì nướng bơ tỏi nấm phô mai kéo sợi",
      emoji = "🧀",
      summary = "Lớp bơ tỏi thơm lừng phủ nấm xào đậm vị và phô mai nướng chảy vàng rụm.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingBread("2 ổ nhỏ"), ingButter("25g"), ingMushroom("120g"), ingCheese("50g")),
      pantryStaples = listOf("Tỏi băm", "Muối", "Tiêu xay", "Ngò rí hoặc lá oregano"),
      steps = listOf(
        "Trộn bơ mềm với tỏi băm, nhúm muối và ngò rí băm nhuyễn; xào nhanh nấm thái lát cho ráo nước.",
        "Xẻ dọc bánh mì, phết đều hỗn hợp bơ tỏi lên mặt cắt.",
        "Xếp phần nấm đã xào lên bánh rồi phủ phô mai bào sợi kín mặt.",
        "Nướng ở 180°C trong 6–7 phút (hoặc áp chảo đậy nắp) đến khi phô mai tan vàng rụm."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "banh_mi_nuong_phap_chuoi",
      title = "Bánh mì nướng kiểu Pháp chuối sữa tươi",
      emoji = "🍌",
      summary = "Bánh mì nhúng hỗn hợp trứng sữa áp chảo thơm lừng kết hợp chuối chín ngọt tự nhiên.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingBread("3 lát"), ingBanana("2 quả"), ingMilk("100ml"), ingEgg()),
      pantryStaples = listOf("Mật ong", "Bột quế (tùy chọn)", "Nhúm muối", "Dầu dừa hoặc dầu ăn"),
      steps = listOf(
        "Đánh đều trứng gà, sữa tươi và nhúm muối nhỏ trong tô rộng; cắt lát chuối chín.",
        "Nhúng từng lát bánh mì vào hỗn hợp trứng sữa khoảng 15 giây cho thấm đều hai mặt.",
        "Áp chảo bánh trên lửa vừa nhỏ mỗi mặt 2–3 phút đến khi vàng thơm.",
        "Xếp bánh ra đĩa kèm chuối thái lát và rưới thêm chút mật ong trước khi thưởng thức."
      ),
      sourceName = SRC_SAVOURY_DAYS,
      sourceUrl = URL_SAVOURY_DAYS
    ),
    CulinaryRecipe(
      id = "sinh_to_chuoi_dau_sua_chua",
      title = "Sinh tố chuối dâu tây sữa chua ít đường",
      emoji = "🍓",
      summary = "Ly sinh tố sánh mịn giàu chất xơ, ngọt dịu từ chuối chín và dâu tây tươi mát.",
      cookMinutes = 5,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingBanana(), ingStrawberry(), ingYogurt(), ingMilk("100ml")),
      pantryStaples = listOf("Đá viên", "Hạt chia hoặc mật ong (tùy chọn)"),
      steps = listOf(
        "Rửa sạch dâu tây, bỏ cuống và cắt đôi; bóc vỏ chuối cắt khoanh vừa xay.",
        "Cho chuối, dâu tây, sữa chua và sữa tươi vào máy xay sinh tố.",
        "Thêm vài viên đá nhỏ nếu thích uống lạnh và xay nhuyễn khoảng 30 giây.",
        "Rót ra ly, rắc thêm chút hạt chia nếu có và dùng ngay để giữ vitamin."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "sua_chua_to_tao_nho_dau",
      title = "Sữa chua tô trái cây táo nho dâu tây",
      emoji = "🍎",
      summary = "Smoothie bowl mát giòn đầy màu sắc từ táo, nho, dâu tây và sữa chua không làm nóng bếp.",
      cookMinutes = 5,
      servings = 1,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingYogurt("2 hũ"), ingApple(), ingGrape(), ingStrawberry("80g")),
      pantryStaples = listOf("Mật ong", "Yến mạch hoặc hạt điều rang (tùy chọn)"),
      steps = listOf(
        "Rửa sạch táo, nho và dâu tây với nước muối loãng rồi để ráo hoàn toàn.",
        "Thái táo miếng nhỏ vừa ăn, cắt đôi quả nho và lát mỏng dâu tây.",
        "Đổ sữa chua lạnh ra tô rộng làm phần nền sánh mịn.",
        "Xếp gọn táo, nho và dâu tây lên mặt tô, rưới thêm 1 muỗng nhỏ mật ong nếu thích ngọt."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "salad_ga_tao_nho_xa_lach",
      title = "Salad ức gà táo nho xà lách giòn mát",
      emoji = "🍏",
      summary = "Vị ngọt giòn của táo và nho giúp món salad ức gà Eat-Clean tươi mới và dễ ăn hơn hẳn.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingChicken("250g"), ingApple(), ingGrape("100g"), ingLettuce()),
      pantryStaples = listOf("Dầu ô liu", "Nước cốt chanh", "Mật ong", "Muối", "Tiêu xay"),
      steps = listOf(
        "Áp chảo hoặc luộc chín ức gà với nhúm muối, để nguội bớt rồi thái lát vừa ăn.",
        "Cắt xà lách khúc ngắn; táo thái lát mỏng ngâm nhanh nước chanh loãng chống thâm; nho cắt đôi.",
        "Khuấy đều 2 muỗng dầu ô liu, 1 muỗng nước cốt chanh, nửa muỗng mật ong, muối và tiêu.",
        "Cho toàn bộ nguyên liệu vào tô, rưới sốt trộn nhẹ tay và thưởng thức."
      ),
      sourceName = SRC_SERIOUS_EATS,
      sourceUrl = URL_SERIOUS_EATS
    ),
    CulinaryRecipe(
      id = "goi_xoai_tom_ca_rot",
      title = "Gỏi xoài tôm tươi cà rốt cuốn xà lách",
      emoji = "🥭",
      summary = "Đĩa gỏi xoài chua ngọt giòn sần sật cùng tôm luộc ngọt thịt và nước mắm chua cay.",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingMango(), ingShrimp("200g"), ingCarrot("1/2 củ"), ingLettuce()),
      pantryStaples = listOf("Nước mắm", "Đường", "Nước cốt chanh", "Tỏi ớt băm", "Đậu phộng rang"),
      steps = listOf(
        "Luộc chín tôm với nhúm muối, bóc vỏ và chẻ đôi theo chiều dọc; xoài và cà rốt bào sợi.",
        "Pha nước trộn gỏi theo tỷ lệ 2 muỗng nước mắm, 2 muỗng đường, 2 muỗng nước chanh và tỏi ớt.",
        "Trộn xoài bào và cà rốt với một nửa nước trộn cho thấm dịu rồi gạn bớt nước.",
        "Cho tôm và phần nước gỏi còn lại vào đảo đều, bày trên lá xà lách và rắc đậu phộng rang."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    ),
    CulinaryRecipe(
      id = "sinh_to_xoai_sua_tuoi_kem",
      title = "Sinh tố xoài sữa tươi topping kem lạnh",
      emoji = "🥭",
      summary = "Món tráng miệng xoài chín thơm lừng, sánh mịn quyện sữa tươi và viên kem mát lạnh.",
      cookMinutes = 8,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(ingMango(), ingMilk("150ml"), ingIceCream("1 viên")),
      pantryStaples = listOf("Đá viên", "Đường hoặc mật ong (tùy chọn)"),
      steps = listOf(
        "Gọt vỏ xoài chín, lấy phần thịt cắt miếng vuông (giữ lại vài miếng nhỏ để trang trí).",
        "Cho thịt xoài, sữa tươi và ít đá viên vào máy xay mịn trong 30 giây.",
        "Rót sinh tố xoài ra ly cao, nhẹ nhàng đặt 1 viên kem lạnh lên trên mặt.",
        "Trang trí thêm vài miếng xoài cắt hạt lựu rồi dùng ngay bằng muỗng và ống hút."
      ),
      sourceName = SRC_DMX,
      sourceUrl = URL_DMX
    ),
    CulinaryRecipe(
      id = "detox_dua_hau_nuoc_cam",
      title = "Nước ép detox dưa hấu nước cam giải nhiệt",
      emoji = "🍉",
      summary = "Thức uống giải nhiệt giàu vitamin C kết hợp dưa hấu thanh mát, nước cam và nước suối lạnh.",
      cookMinutes = 5,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingWatermelon(), ingOrangeJuice(), ingWater("150ml")),
      pantryStaples = listOf("Lá bạc hà", "Đá viên", "Nước cốt chanh (tùy chọn)"),
      steps = listOf(
        "Lọc bỏ hạt dưa hấu, cắt phần ruột đỏ thành khối nhỏ cho vào cối xay.",
        "Thêm nước suối mát vào xay nhanh 20 giây rồi lọc qua rây nếu thích nước trong mượt.",
        "Rót hỗn hợp dưa hấu ra ly lớn cùng nước cam tươi và khuấy nhẹ cho hòa quyện.",
        "Thêm đá viên và vài lá bạc hà vò nhẹ để tăng hương thơm sảng khoái."
      ),
      sourceName = SRC_COOKY,
      sourceUrl = URL_COOKY
    ),
    CulinaryRecipe(
      id = "canh_trung_ca_chua_bap_ngot",
      title = "Canh trứng mây sốt cà bắp ngọt thanh mát",
      emoji = "🍅",
      summary = "Bát canh quốc dân nấu chỉ 12 phút, vân trứng tơ mềm cùng vị cà chua và bắp ngọt tự nhiên.",
      cookMinutes = 12,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      fridgeIngredients = listOf(ingEgg(), ingTomatoSauce(), ingCorn("1/2 trái"), ingWater("600ml")),
      pantryStaples = listOf("Hành tím băm", "Nước mắm", "Hành lá", "Tiêu xay", "Dầu ăn"),
      steps = listOf(
        "Phi thơm hành tím với chút dầu, đảo nhanh sốt cà chua trong 1 phút cho lên màu đỏ đẹp.",
        "Chế nước suối và hạt bắp ngọt vào nồi, đun sôi khoảng 5 phút cho hạt bắp chín mềm.",
        "Nêm nước mắm và nhúm muối vừa khẩu vị, đánh tan trứng rồi rót thành dòng mỏng vào nồi đang sôi nhẹ.",
        "Khuấy vòng tròn tạo vân trứng mây mượt, rắc hành lá cùng tiêu xay rồi tắt bếp."
      ),
      sourceName = SRC_VNEXPRESS,
      sourceUrl = URL_VNEXPRESS
    )
  )
}
```

### `app/src/main/kotlin/com/truong/fridgezero/domain/Freshness.kt` (22 lines)

```kotlin
package com.truong.fridgezero.domain

import androidx.compose.ui.graphics.Color

enum class Freshness {
  OK,
  SOON,
  BAD;

  fun color(): Color = when (this) {
    OK -> Color(0xFFA3E635)
    SOON -> Color(0xFFFACC15)
    BAD -> Color(0xFFFB7185)
  }
}

fun freshnessOf(daysLeft: Int): Freshness = when {
  daysLeft <= 0 -> Freshness.BAD
  daysLeft == 1 -> Freshness.SOON
  else -> Freshness.OK
}
```

### `app/src/main/kotlin/com/truong/fridgezero/domain/Models.kt` (66 lines)

```kotlin
package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup

data class NRect(val x: Float, val y: Float, val w: Float, val h: Float) {
  val cx: Float get() = x + w / 2f
  val cy: Float get() = y + h / 2f
}

data class DisplayItem(
  val primaryId: Long,
  val itemIds: List<Long> = listOf(primaryId),
  val compartmentId: String = "",
  val name: String,
  val iconKey: String,
  val group: FoodGroup = EmojiIconRepository.groupFor(iconKey),
  val quantity: Int,
  val unit: String? = null,
  val minDaysLeft: Int,
  val isOpened: Boolean = false
) {
  val freshness: Freshness get() = freshnessOf(minDaysLeft)
}

fun List<FoodItemEntity>.toDisplay(todayEpochDay: Long): List<DisplayItem> =
  groupBy { it.name.trim().lowercase() }
    .values
    .map { xs ->
      val earliest = xs.minBy { it.expiresAtEpochDay }
      DisplayItem(
        primaryId = earliest.id,
        itemIds = xs.map { it.id },
        compartmentId = earliest.compartmentId,
        name = earliest.name,
        iconKey = earliest.iconKey,
        group = EmojiIconRepository.groupFor(earliest.iconKey),
        quantity = xs.sumOf { it.quantity },
        unit = earliest.unit,
        minDaysLeft = xs.minOf { (it.expiresAtEpochDay - todayEpochDay).toInt() },
        isOpened = xs.any { it.isOpened }
      )
    }
    .sortedWith(compareBy({ it.freshness.ordinal * -1 }, { it.minDaysLeft }))

data class CompartmentUi(
  val id: String,
  val name: String,
  val rect: NRect,
  val kind: CompartmentKind,
  val shelves: Int = 2,
  val items: List<DisplayItem>,
  val worstFreshness: Freshness,
  val urgentCount: Int
)

data class FridgeUiState(
  val activeFridge: FridgeEntity? = null,
  val compartments: List<CompartmentUi> = emptyList(),
  val recentItemKeys: List<String> = emptyList(),
  val playWelcomeSpin: Boolean = false
)
```

### `app/src/main/kotlin/com/truong/fridgezero/domain/ShelfLife.kt` (122 lines)

```kotlin
package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.ui.icons.FoodGroup

enum class StorageZone {
  CHILLED,
  FREEZER,
  DRAWER
}

data class ShelfLifeRule(
  val chilledDays: Int? = null,
  val freezerDays: Int? = null,
  val drawerDays: Int? = null
)

interface ShelfLifeRepository {
  fun suggestDays(
    iconKey: String,
    group: FoodGroup,
    zone: StorageZone,
    isOpened: Boolean = false
  ): Int?
}

/**
 * Standardized shelf-life lookup table based on conservative USDA FoodKeeper / FDA lower-bound
 * cold-storage guidance across chilled (`~2–4°C`), freezer (`≤ -18°C`), and crisper drawer zones.
 */
object ConfigurableShelfLifeRepository : ShelfLifeRepository {

  private const val OPENED_FACTOR = 0.5f

  private val byKey: Map<String, ShelfLifeRule> = mapOf(
    "chicken" to ShelfLifeRule(chilledDays = 2, freezerDays = 270, drawerDays = 2),
    "meat" to ShelfLifeRule(chilledDays = 3, freezerDays = 180, drawerDays = 3),
    "bacon" to ShelfLifeRule(chilledDays = 7, freezerDays = 30, drawerDays = 7),
    "shrimp" to ShelfLifeRule(chilledDays = 2, freezerDays = 120, drawerDays = 2),
    "salmon" to ShelfLifeRule(chilledDays = 2, freezerDays = 60, drawerDays = 2),
    "crab" to ShelfLifeRule(chilledDays = 2, freezerDays = 90, drawerDays = 2),
    "squid" to ShelfLifeRule(chilledDays = 2, freezerDays = 90, drawerDays = 2),
    "lettuce" to ShelfLifeRule(chilledDays = 4, freezerDays = null, drawerDays = 5),
    "broccoli" to ShelfLifeRule(chilledDays = 4, freezerDays = 240, drawerDays = 5),
    "mushroom" to ShelfLifeRule(chilledDays = 4, freezerDays = 180, drawerDays = 5),
    "corn" to ShelfLifeRule(chilledDays = 3, freezerDays = 240, drawerDays = 4),
    "bell_pepper" to ShelfLifeRule(chilledDays = 7, freezerDays = 180, drawerDays = 10),
    "carrot" to ShelfLifeRule(chilledDays = 14, freezerDays = 240, drawerDays = 21),
    "pickle" to ShelfLifeRule(chilledDays = 30, freezerDays = null, drawerDays = 30),
    "strawberry" to ShelfLifeRule(chilledDays = 3, freezerDays = 180, drawerDays = 4),
    "banana" to ShelfLifeRule(chilledDays = 4, freezerDays = 60, drawerDays = 5),
    "mango" to ShelfLifeRule(chilledDays = 5, freezerDays = 180, drawerDays = 6),
    "grape" to ShelfLifeRule(chilledDays = 7, freezerDays = 180, drawerDays = 10),
    "watermelon" to ShelfLifeRule(chilledDays = 7, freezerDays = null, drawerDays = 10),
    "apple" to ShelfLifeRule(chilledDays = 21, freezerDays = 180, drawerDays = 28),
    "milk" to ShelfLifeRule(chilledDays = 7, freezerDays = 90, drawerDays = 7),
    "yogurt" to ShelfLifeRule(chilledDays = 14, freezerDays = 60, drawerDays = 14),
    "egg" to ShelfLifeRule(chilledDays = 21, freezerDays = null, drawerDays = 21),
    "cheese" to ShelfLifeRule(chilledDays = 21, freezerDays = 180, drawerDays = 21),
    "butter" to ShelfLifeRule(chilledDays = 30, freezerDays = 180, drawerDays = 30),
    "rice" to ShelfLifeRule(chilledDays = 3, freezerDays = 30, drawerDays = 3),
    "bread" to ShelfLifeRule(chilledDays = 5, freezerDays = 90, drawerDays = 5),
    "tomato_sauce" to ShelfLifeRule(chilledDays = 30, freezerDays = 90, drawerDays = 30),
    "orange_juice" to ShelfLifeRule(chilledDays = 7, freezerDays = 180, drawerDays = 7),
    "water" to ShelfLifeRule(chilledDays = 30, freezerDays = null, drawerDays = 30),
    "dumpling" to ShelfLifeRule(chilledDays = 3, freezerDays = 60, drawerDays = 3),
    "ice_cream" to ShelfLifeRule(chilledDays = 1, freezerDays = 60, drawerDays = 1)
  )

  private val byGroup: Map<FoodGroup, ShelfLifeRule> = mapOf(
    FoodGroup.THIT to ShelfLifeRule(chilledDays = 3, freezerDays = 180, drawerDays = 3),
    FoodGroup.CA_HAI_SAN to ShelfLifeRule(chilledDays = 2, freezerDays = 90, drawerDays = 2),
    FoodGroup.RAU to ShelfLifeRule(chilledDays = 4, freezerDays = 180, drawerDays = 5),
    FoodGroup.TRAI_CAY to ShelfLifeRule(chilledDays = 5, freezerDays = 180, drawerDays = 7),
    FoodGroup.SUA_TRUNG to ShelfLifeRule(chilledDays = 7, freezerDays = 60, drawerDays = 7),
    FoodGroup.DO_KHO to ShelfLifeRule(chilledDays = 7, freezerDays = 90, drawerDays = 7),
    FoodGroup.DO_UONG to ShelfLifeRule(chilledDays = 7, freezerDays = 90, drawerDays = 7),
    FoodGroup.AN_SAN to ShelfLifeRule(chilledDays = 3, freezerDays = 60, drawerDays = 3),
    FoodGroup.KHAC to ShelfLifeRule(chilledDays = 4, freezerDays = 60, drawerDays = 4)
  )

  override fun suggestDays(
    iconKey: String,
    group: FoodGroup,
    zone: StorageZone,
    isOpened: Boolean
  ): Int? {
    val rule = byKey[iconKey] ?: byGroup[group] ?: return null
    val baseDays = when (zone) {
      StorageZone.CHILLED -> rule.chilledDays
      StorageZone.FREEZER -> rule.freezerDays
      StorageZone.DRAWER -> rule.drawerDays ?: rule.chilledDays
    } ?: return null

    return if (isOpened) {
      (baseDays * OPENED_FACTOR).toInt().coerceAtLeast(1)
    } else {
      baseDays
    }
  }
}

fun zoneForCompartment(
  compartmentId: String,
  compartmentName: String = "",
  kind: CompartmentKind = CompartmentKind.DOOR
): StorageZone {
  val text = "$compartmentId $compartmentName".lowercase()
  return when {
    text.contains("freezer") || text.contains("đông") || text.contains("đá") -> StorageZone.FREEZER
    kind == CompartmentKind.DRAWER || text.contains("crisper") || text.contains("rau") -> StorageZone.DRAWER
    else -> StorageZone.CHILLED
  }
}

fun zoneForCompartment(compartment: CompartmentEntity): StorageZone =
  zoneForCompartment(compartment.id, compartment.name, compartment.kind)

fun zoneForCompartment(compartment: CompartmentUi): StorageZone =
  zoneForCompartment(compartment.id, compartment.name, compartment.kind)
```

### `app/src/main/kotlin/com/truong/fridgezero/MainActivity.kt` (407 lines)

```kotlin
package com.truong.fridgezero

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truong.fridgezero.data.AppConfig
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.Seed
import com.truong.fridgezero.notifications.rememberReminderPromptController
import com.truong.fridgezero.ui.aiscan.AiScanPlaceholderScreen
import com.truong.fridgezero.ui.bento.BentoFilter
import com.truong.fridgezero.ui.bento.CompartmentBentoScreen
import com.truong.fridgezero.ui.designer.FridgeDesignerScreen
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageBackdrop
import com.truong.fridgezero.ui.fridge3d.LocalFridge3DStageController
import com.truong.fridgezero.ui.fridge3d.SharedFridge3DWebView
import com.truong.fridgezero.ui.fridge3d.rememberFridge3DStageController
import com.truong.fridgezero.ui.home.HomeScreen
import com.truong.fridgezero.ui.recipes.CulinaryGuideScreen
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.FridgeZeroTheme
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.dotGridBackground
import com.truong.fridgezero.vm.FridgeViewModel
import kotlinx.coroutines.launch

private const val CRASH_TRACE_FILE = "crash_trace.txt"

private class FridgeCrashHandler(
  private val crashFile: File,
  private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    runCatching {
      crashFile.writeText(Log.getStackTraceString(throwable))
    }
    previous?.uncaughtException(thread, throwable)
  }
}

private sealed interface RootRoute {
  data object Home : RootRoute
  data object Designer : RootRoute
  data object AiScan : RootRoute
  data class Bento(val compartmentId: String, val filterUrgent: Boolean = false) : RootRoute
  data class Recipes(val initialRecipeId: String? = null) : RootRoute
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      WebView.setWebContentsDebuggingEnabled(true)
    }

    val crashFile = File(applicationContext.filesDir, CRASH_TRACE_FILE)
    if (Thread.getDefaultUncaughtExceptionHandler() !is FridgeCrashHandler) {
      val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler(FridgeCrashHandler(crashFile, previousHandler))
    }

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    )
    setContent {
      FridgeZeroTheme {
        var savedCrashTrace by remember {
          mutableStateOf(
            runCatching {
              if (crashFile.exists()) crashFile.readText().takeIf { it.isNotBlank() } else null
            }.getOrNull()
          )
        }

        if (savedCrashTrace != null) {
          CrashReportScreen(
            trace = savedCrashTrace.orEmpty(),
            onClearAndContinue = {
              runCatching { if (crashFile.exists()) crashFile.delete() }
              savedCrashTrace = null
            }
          )
        } else {
          val stageController = rememberFridge3DStageController()
          val appConfig = remember { AppConfig(applicationContext) }
          val scope = rememberCoroutineScope()
          val onboardingDone by appConfig.hasCompletedOnboarding
            .collectAsStateWithLifecycle(initialValue = null)
          val vm: FridgeViewModel = viewModel(factory = FridgeViewModel.factory(applicationContext))
          val uiState by vm.uiState.collectAsStateWithLifecycle()
          val reminderController = rememberReminderPromptController(appConfig)
          var route by remember { mutableStateOf<RootRoute>(RootRoute.Home) }
          var justClosedCompartmentId by remember { mutableStateOf<String?>(null) }

          CompositionLocalProvider(LocalFridge3DStageController provides stageController) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .dotGridBackground()
                .onGloballyPositioned { stageController.rootCoordinates = it }
            ) {
              // Optional framed card backdrop behind WebView (used by FridgeDesignerScreen)
              Fridge3DStageBackdrop(
                controller = stageController,
                modifier = Modifier.zIndex(0.5f)
              )

              // Single shared WebView hosted at root above dotGridBackground and below foreground UI
              SharedFridge3DWebView(
                controller = stageController,
                appConfig = appConfig,
                modifier = Modifier.zIndex(1f)
              )

              // Foreground screen UI, DoorStickersOverlay, HomeDashboardHeader, HomeTaskBar, and FAB
              Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
                when {
                  onboardingDone == null -> Unit
                  onboardingDone == false || route is RootRoute.Designer -> {
                    val isEditingExisting = onboardingDone == true
                    val fridge = uiState.activeFridge ?: Seed.defaultFridge
                    val current = if (isEditingExisting) {
                      uiState.compartments.mapIndexed { idx, c ->
                        CompartmentEntity(
                          id = c.id, fridgeId = fridge.id, name = c.name,
                          x = c.rect.x, y = c.rect.y, w = c.rect.w, h = c.rect.h,
                          kind = c.kind, shelves = c.shelves, sort = idx
                        )
                      }
                    } else {
                      Seed.compartmentsForTemplate(Seed.defaultFridge.template, Seed.defaultFridge.id)
                    }
                    if (isEditingExisting) {
                      BackHandler { route = RootRoute.Home }
                    }
                    FridgeDesignerScreen(
                      initialFridge = if (isEditingExisting) fridge else Seed.defaultFridge,
                      initialCompartments = current,
                      onConfirm = { f, comps ->
                        vm.saveCustomFridge(
                          fridge = f,
                          compartments = comps,
                          includeSampleItems = true,
                          triggerWelcomeSpin = true
                        )
                        scope.launch { appConfig.setOnboardingCompleted(true) }
                        route = RootRoute.Home
                      }
                    )
                  }
                  route is RootRoute.AiScan -> {
                    BackHandler { route = RootRoute.Home }
                    AiScanPlaceholderScreen(onBack = { route = RootRoute.Home })
                  }
                  route is RootRoute.Recipes -> {
                    val recipesRoute = route as RootRoute.Recipes
                    BackHandler { route = RootRoute.Home }
                    CulinaryGuideScreen(
                      compartments = uiState.compartments,
                      initialRecipeId = recipesRoute.initialRecipeId,
                      onBack = { route = RootRoute.Home },
                      onConsumeRecipe = { rec -> vm.consumeRecipeIngredients(rec.matchedIngredients) }
                    )
                  }
                  route is RootRoute.Bento -> {
                    val bentoRoute = route as RootRoute.Bento
                    val comp = uiState.compartments.firstOrNull { it.id == bentoRoute.compartmentId }
                      ?: uiState.compartments.firstOrNull()
                    if (comp == null) {
                      route = RootRoute.Home
                    } else {
                      val exitToHome = {
                        justClosedCompartmentId = comp.id
                        route = RootRoute.Home
                      }
                      BackHandler(onBack = exitToHome)
                      CompartmentBentoScreen(
                        compartment = comp,
                        allCompartments = uiState.compartments,
                        recentKeys = uiState.recentItemKeys,
                        initialFilter = if (bentoRoute.filterUrgent) BentoFilter.URGENT else BentoFilter.ALL,
                        onSelectCompartment = { nextId ->
                          route = RootRoute.Bento(nextId, filterUrgent = false)
                        },
                        onBack = exitToHome,
                        onAddItem = { compId, name, iconKey, qty, daysLeft, unit ->
                          vm.addItem(compId, name, iconKey, qty, daysLeft, unit)
                          reminderController.onItemSavedWithExpiry(daysLeft)
                        },
                        onUpdateItem = { id, name, iconKey, qty, daysLeft, unit ->
                          vm.updateItem(id, name, iconKey, qty, daysLeft, unit)
                          reminderController.onItemSavedWithExpiry(daysLeft)
                        },
                        onAdjustQuantity = vm::adjustQuantity,
                        onMoveCompartment = { ids, targetId, newDays ->
                          vm.moveItemToCompartment(ids, targetId, newDays)
                        },
                        onMarkStatus = { ids, status ->
                          vm.markItemStatus(ids, status)
                        }
                      )
                    }
                  }
                  else -> {
                    HomeScreen(
                      uiState = uiState,
                      remindersEnabled = reminderController.remindersEnabled,
                      justClosedCompartmentId = justClosedCompartmentId,
                      onJustClosedConsumed = { justClosedCompartmentId = null },
                      onToggleReminders = reminderController.onToggleReminders,
                      onWelcomeSpinConsumed = vm::consumeWelcomeSpin,
                      onOpenCompartment = { compId, filterUrgent ->
                        route = RootRoute.Bento(compId, filterUrgent)
                      },
                      onAddItem = { compId, name, iconKey, qty, daysLeft, unit ->
                        vm.addItem(compId, name, iconKey, qty, daysLeft, unit)
                        reminderController.onItemSavedWithExpiry(daysLeft)
                      },
                      onMarkStatus = { ids, status ->
                        vm.markItemStatus(ids, status)
                      },
                      onOpenRecipes = { recipeId -> route = RootRoute.Recipes(recipeId) },
                      onOpenAiScan = { route = RootRoute.AiScan },
                      onOpenDesigner = { route = RootRoute.Designer }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CrashReportScreen(
  trace: String,
  onClearAndContinue: () -> Unit
) {
  val context = LocalContext.current
  val summary = remember(trace) {
    trace.lineSequence().firstOrNull { it.isNotBlank() } ?: "UncaughtException"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "NHẬT KÝ LỖI THIẾT BỊ",
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
        Text(
          text = summary,
          fontFamily = BodyFont,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = InkBlack.copy(alpha = 0.8f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      StickerBadge(
        text = "CRASH LOG",
        containerColor = CoralUrgent,
        mono = true
      )
    }

    NeoBrutalistCard(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(12.dp)
      ) {
        Text(
          text = trace,
          fontFamily = MonoFont,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          lineHeight = 16.sp,
          color = InkBlack
        )
      }
    }

    Spacer(Modifier.height(2.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          cm?.setPrimaryClip(ClipData.newPlainText("FridgeZero Crash", trace))
          Toast.makeText(context, "Đã sao chép lỗi", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.weight(1f),
        containerColor = LemonWarn,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
      ) {
        Text(
          text = "Sao chép lỗi",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack,
          maxLines = 1
        )
      }

      NeoBrutalistButton(
        onClick = onClearAndContinue,
        modifier = Modifier.weight(1f),
        containerColor = LimeFresh,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
      ) {
        Text(
          text = "Xóa lỗi & Vào app",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack,
          maxLines = 1
        )
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/notifications/ContextualNotificationPrompt.kt` (189 lines)

```kotlin
package com.truong.fridgezero.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truong.fridgezero.R
import com.truong.fridgezero.data.AppConfig
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch

@Stable
class ReminderPromptController(
  val remindersEnabled: Boolean,
  val onItemSavedWithExpiry: (daysLeft: Int) -> Unit,
  val onToggleReminders: () -> Unit
)

/**
 * Contextual POST_NOTIFICATIONS controller (spec §0 & §4.2):
 * Never prompts on cold app launch; prompts only when saving a short-lived item or toggling reminders.
 */
@Composable
fun rememberReminderPromptController(appConfig: AppConfig): ReminderPromptController {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val remindersEnabled by appConfig.expiryRemindersEnabled
    .collectAsStateWithLifecycle(initialValue = false)
  val promptShown by appConfig.notificationPromptShown
    .collectAsStateWithLifecycle(initialValue = false)

  var showDialog by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    scope.launch {
      appConfig.setNotificationPromptShown(true)
      if (granted) {
        appConfig.setExpiryRemindersEnabled(true)
        ExpiryReminderScheduler.scheduleDaily(context)
      } else {
        appConfig.setExpiryRemindersEnabled(false)
      }
    }
  }

  fun enableWithContextualPermission() {
    if (hasNotificationPermission(context)) {
      scope.launch {
        appConfig.setExpiryRemindersEnabled(true)
        appConfig.setNotificationPromptShown(true)
      }
      ExpiryReminderScheduler.scheduleDaily(context)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  if (showDialog) {
    Dialog(onDismissRequest = {
      showDialog = false
      scope.launch { appConfig.setNotificationPromptShown(true) }
    }) {
      NeoBrutalistCard(containerColor = PaperCream, modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          StickerBadge(text = "🔔 NHẮC HẠN", containerColor = LemonWarn)
          Text(
            text = stringResource(R.string.reminder_prompt_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = InkBlack
          )
          Text(
            text = stringResource(R.string.reminder_prompt_body),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = InkBlack
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            NeoBrutalistButton(
              onClick = {
                showDialog = false
                scope.launch { appConfig.setNotificationPromptShown(true) }
              },
              modifier = Modifier.weight(1f),
              containerColor = SurfaceWhite
            ) {
              Text(
                text = stringResource(R.string.reminder_prompt_later),
                fontFamily = BodyFont,
                fontWeight = FontWeight.Bold,
                color = InkBlack
              )
            }
            NeoBrutalistButton(
              onClick = {
                showDialog = false
                enableWithContextualPermission()
              },
              modifier = Modifier.weight(1f),
              containerColor = LimeFresh
            ) {
              Text(
                text = stringResource(R.string.reminder_prompt_confirm),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                color = InkBlack
              )
            }
          }
        }
      }
    }
  }

  return remember(remindersEnabled, promptShown) {
    ReminderPromptController(
      remindersEnabled = remindersEnabled && hasNotificationPermission(context),
      onItemSavedWithExpiry = { daysLeft ->
        if (daysLeft <= 7 && (!remindersEnabled || !hasNotificationPermission(context)) && !promptShown) {
          showDialog = true
        } else if (remindersEnabled && hasNotificationPermission(context)) {
          ExpiryReminderScheduler.scheduleDaily(context)
        }
      },
      onToggleReminders = {
        if (remindersEnabled && hasNotificationPermission(context)) {
          scope.launch { appConfig.setExpiryRemindersEnabled(false) }
          ExpiryReminderScheduler.cancel(context)
        } else if (hasNotificationPermission(context)) {
          enableWithContextualPermission()
        } else {
          showDialog = true
        }
      }
    )
  }
}

private fun hasNotificationPermission(context: Context): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
  return ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.POST_NOTIFICATIONS
  ) == PackageManager.PERMISSION_GRANTED
}
```

### `app/src/main/kotlin/com/truong/fridgezero/notifications/ExpiryReminderWorker.kt` (116 lines)

```kotlin
package com.truong.fridgezero.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truong.fridgezero.MainActivity
import com.truong.fridgezero.R
import com.truong.fridgezero.data.FridgeDb
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.freshnessOf
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ExpiryReminderWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val activeItems = FridgeDb.get(applicationContext)
      .foodItemDao()
      .getByStatusSnapshot(ItemStatus.ACTIVE)

    val today = LocalDate.now().toEpochDay()
    val expiringCount = activeItems.count { item ->
      val daysLeft = (item.expiresAtEpochDay - today).toInt()
      freshnessOf(daysLeft) != Freshness.OK
    }

    if (expiringCount > 0) {
      postNotification(expiringCount)
    }
    return Result.success()
  }

  private fun postNotification(expiringCount: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    ensureChannel()

    val intent = Intent(applicationContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      applicationContext,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(applicationContext.getString(R.string.notification_title))
      .setContentText(applicationContext.getString(R.string.notification_body_fmt, expiringCount))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
  }

  private fun ensureChannel() {
    val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return
    val channel = NotificationChannel(
      CHANNEL_ID,
      applicationContext.getString(R.string.notification_channel_name),
      NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val CHANNEL_ID = "fridgezero_expiry"
    private const val NOTIFICATION_ID = 1001
  }
}

object ExpiryReminderScheduler {
  private const val WORK_NAME = "fridgezero_daily_expiry"

  fun scheduleDaily(context: Context) {
    val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
      WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request
    )
  }

  fun cancel(context: Context) {
    WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/aiscan/AiScanPlaceholderScreen.kt` (363 lines)

```kotlin
package com.truong.fridgezero.ui.aiscan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.truong.fridgezero.R
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CavitySlate
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

/**
 * Phase-2 CameraX Receipt Scanner preview screen with contextual CAMERA permission handling.
 */
@Composable
fun AiScanPlaceholderScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
  }

  var torchEnabled by remember { mutableStateOf(false) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      NeoBrutalistButton(
        onClick = onBack,
        containerColor = SurfaceWhite,
        cornerRadius = 10.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
      ) {
        Text(
          text = stringResource(R.string.ai_scan_back),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }

      Text(
        text = stringResource(R.string.ai_scan_title),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = InkBlack
      )

      StickerBadge(
        text = stringResource(R.string.home_tab_scan_badge),
        containerColor = LilacAccent,
        tiltDegrees = 2f,
        mono = true
      )
    }

    // Center Viewfinder / Permission Card
    NeoBrutalistCard(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      containerColor = if (hasPermission) CavitySlate else SurfaceWhite
    ) {
      if (hasPermission) {
        CameraViewfinderBox(
          torchEnabled = torchEnabled,
          onCameraBound = { boundCamera = it },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Column(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(text = "📷", fontSize = 48.sp)
          Spacer(Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.ai_scan_permission_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = InkBlack,
            textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.ai_scan_permission_body),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = InkBlack.copy(alpha = 0.78f),
            textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(18.dp))
          NeoBrutalistButton(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            containerColor = LimeFresh
          ) {
            Text(
              text = stringResource(R.string.ai_scan_permission_grant),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = InkBlack
            )
          }
        }
      }
    }

    // Phase 2 Banner
    NeoBrutalistCard(
      modifier = Modifier.fillMaxWidth(),
      containerColor = LemonWarn,
      cornerRadius = 14.dp,
      shadowOffset = 3.dp
    ) {
      Text(
        text = stringResource(R.string.ai_scan_banner),
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = InkBlack,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)
      )
    }

    // Bottom Action Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = {
          val next = !torchEnabled
          torchEnabled = next
          boundCamera?.cameraControl?.enableTorch(next)
        },
        enabled = hasPermission && boundCamera != null,
        modifier = Modifier.weight(1f),
        containerColor = if (torchEnabled) LemonWarn else SurfaceWhite
      ) {
        Text(
          text = stringResource(if (torchEnabled) R.string.ai_scan_flash_on else R.string.ai_scan_flash_off),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }

      NeoBrutalistButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.weight(1.4f),
        containerColor = LimeFresh
      ) {
        Text(
          text = stringResource(R.string.ai_scan_capture_gd2),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack
        )
      }
    }
  }
}

@Composable
private fun CameraViewfinderBox(
  torchEnabled: Boolean,
  onCameraBound: (Camera?) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }
  var cameraError by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner, previewView) {
    val future = ProcessCameraProvider.getInstance(context)
    var provider: ProcessCameraProvider? = null
    val listener = Runnable {
      try {
        val camProvider = future.get()
        provider = camProvider
        val preview = Preview.Builder().build().also {
          it.surfaceProvider = previewView.surfaceProvider
        }
        camProvider.unbindAll()
        val cam = camProvider.bindToLifecycle(
          lifecycleOwner,
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview
        )
        cam.cameraControl.enableTorch(torchEnabled)
        onCameraBound(cam)
        cameraError = false
      } catch (_: Exception) {
        cameraError = true
        onCameraBound(null)
      }
    }
    future.addListener(listener, ContextCompat.getMainExecutor(context))
    onDispose {
      provider?.unbindAll()
      onCameraBound(null)
    }
  }

  val transition = rememberInfiniteTransition(label = "scanBeam")
  val beamProgress by transition.animateFloat(
    initialValue = 0.14f,
    targetValue = 0.86f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "beamY"
  )

  Box(modifier = modifier.background(CavitySlate)) {
    if (!cameraError) {
      AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Text(
        text = stringResource(R.string.ai_scan_camera_unavailable),
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = Color.White.copy(alpha = 0.82f),
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).padding(28.dp)
      )
    }

    // 4 receipt finder corner brackets + LimeFresh laser scan line
    Canvas(modifier = Modifier.fillMaxSize()) {
      val insetX = size.width * 0.12f
      val insetY = size.height * 0.14f
      val left = insetX
      val right = size.width - insetX
      val top = insetY
      val bottom = size.height - insetY
      val bracket = 28.dp.toPx()
      val stroke = 4.dp.toPx()

      // Top-left
      drawLine(LimeFresh, Offset(left, top), Offset(left + bracket, top), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(left, top), Offset(left, top + bracket), stroke, StrokeCap.Square)
      // Top-right
      drawLine(LimeFresh, Offset(right - bracket, top), Offset(right, top), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(right, top), Offset(right, top + bracket), stroke, StrokeCap.Square)
      // Bottom-left
      drawLine(LimeFresh, Offset(left, bottom - bracket), Offset(left, bottom), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(left, bottom), Offset(left + bracket, bottom), stroke, StrokeCap.Square)
      // Bottom-right
      drawLine(LimeFresh, Offset(right - bracket, bottom), Offset(right, bottom), stroke, StrokeCap.Square)
      drawLine(LimeFresh, Offset(right, bottom - bracket), Offset(right, bottom), stroke, StrokeCap.Square)

      // Animated horizontal scan laser
      val beamY = size.height * beamProgress
      drawLine(
        color = LimeFresh,
        start = Offset(left + 8.dp.toPx(), beamY),
        end = Offset(right - 8.dp.toPx(), beamY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/bento/CompartmentBentoScreen.kt` (472 lines)

```kotlin
package com.truong.fridgezero.ui.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.StorageZone
import com.truong.fridgezero.domain.zoneForCompartment
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import com.truong.fridgezero.ui.icons.FoodIcon
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

enum class BentoFilter(val labelRes: Int) {
  ALL(R.string.filter_all),
  URGENT(R.string.filter_urgent),
  VEGGIES(R.string.filter_veggies),
  MEAT_FISH(R.string.filter_meat_fish),
  DAIRY_EGG(R.string.filter_dairy_egg),
  DRINKS(R.string.filter_drinks),
  FROZEN(R.string.filter_frozen)
}

/**
 * Screen 3 — Flat 2-Column Neo-Brutalist Bento Grid for managing food inside a compartment.
 */
@Composable
fun CompartmentBentoScreen(
  compartment: CompartmentUi,
  allCompartments: List<CompartmentUi>,
  recentKeys: List<String>,
  initialFilter: BentoFilter = BentoFilter.ALL,
  onSelectCompartment: (String) -> Unit,
  onBack: () -> Unit,
  onAddItem: (compartmentId: String, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onUpdateItem: (id: Long, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onAdjustQuantity: (id: Long, delta: Int) -> Unit,
  onMoveCompartment: (ids: List<Long>, targetCompartmentId: String, newDaysLeft: Int?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember(compartment.id, initialFilter) { mutableStateOf(initialFilter) }
  var showAddSheet by remember { mutableStateOf(false) }
  var detailTarget by remember { mutableStateOf<DisplayItem?>(null) }

  val isFreezerComp = zoneForCompartment(compartment) == StorageZone.FREEZER
  val filteredItems = remember(compartment.items, selectedFilter, isFreezerComp) {
    val list = when (selectedFilter) {
      BentoFilter.ALL -> compartment.items
      BentoFilter.URGENT -> compartment.items.filter { it.freshness != Freshness.OK }
      BentoFilter.VEGGIES -> compartment.items.filter {
        it.group == FoodGroup.RAU || it.group == FoodGroup.TRAI_CAY
      }
      BentoFilter.MEAT_FISH -> compartment.items.filter {
        it.group == FoodGroup.THIT || it.group == FoodGroup.CA_HAI_SAN
      }
      BentoFilter.DAIRY_EGG -> compartment.items.filter { it.group == FoodGroup.SUA_TRUNG }
      BentoFilter.DRINKS -> compartment.items.filter { it.group == FoodGroup.DO_UONG }
      BentoFilter.FROZEN -> compartment.items.filter {
        isFreezerComp || it.group == FoodGroup.AN_SAN || it.iconKey in setOf("ice_cream", "dumpling", "shrimp", "salmon")
      }
    }
    list.sortedWith(compareBy({ it.freshness.ordinal * -1 }, { it.minDaysLeft }))
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Top Bar: square ← button | "NGĂN MÁT · N MÓN" | + Thêm button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val backCd = stringResource(R.string.bento_back_cd)
      NeoBrutalistButton(
        onClick = onBack,
        modifier = Modifier.size(44.dp).semantics { contentDescription = backCd },
        containerColor = SurfaceWhite,
        cornerRadius = 12.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(0.dp)
      ) {
        Text(
          text = stringResource(R.string.bento_back_symbol),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
      }

      Text(
        text = stringResource(R.string.bento_title_fmt, compartment.name.uppercase(), compartment.items.size),
        modifier = Modifier.weight(1f),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      NeoBrutalistButton(
        onClick = { showAddSheet = true },
        containerColor = LimeFresh,
        cornerRadius = 12.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Text(
          text = stringResource(R.string.bento_add_button),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }
    }

    // Quick compartment switcher chips
    if (allCompartments.size > 1) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        items(allCompartments, key = { it.id }) { comp ->
          val active = comp.id == compartment.id
          NeoBrutalistButton(
            onClick = { onSelectCompartment(comp.id) },
            containerColor = if (active) SkyInfo else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = "${comp.name} (${comp.items.size})",
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp,
              color = InkBlack
            )
          }
        }
      }
    }

    // Category filter chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      items(BentoFilter.entries) { filter ->
        val selected = selectedFilter == filter
        val chipBg = when {
          selected && filter == BentoFilter.URGENT -> CoralUrgent
          selected && filter == BentoFilter.FROZEN -> LilacAccent
          selected -> LemonWarn
          else -> SurfaceWhite
        }
        NeoBrutalistButton(
          onClick = { selectedFilter = filter },
          containerColor = chipBg,
          cornerRadius = 10.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
        ) {
          Text(
            text = stringResource(filter.labelRes),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack
          )
        }
      }
    }

    // 2-Column Flat Bento Grid (zero text truncation)
    if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = stringResource(
              if (compartment.items.isEmpty()) R.string.bento_empty_compartment
              else R.string.bento_empty_filter
            ),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredItems, key = { it.primaryId }) { item ->
          FoodBentoCard(
            item = item,
            onCardClick = { detailTarget = item },
            onDecrement = {
              if (item.quantity <= 1) {
                onMarkStatus(item.itemIds, ItemStatus.USED_UP)
              } else {
                onAdjustQuantity(item.primaryId, -1)
              }
            },
            onIncrement = { onAdjustQuantity(item.primaryId, +1) }
          )
        }
      }
    }
  }

  if (showAddSheet) {
    QuickAddItemSheet(
      compartments = allCompartments,
      initialCompartmentId = compartment.id,
      recentKeys = recentKeys,
      onSaveItem = onAddItem,
      onDismiss = { showAddSheet = false }
    )
  }

  val activeDetail = detailTarget
  if (activeDetail != null) {
    val latestItem = compartment.items.firstOrNull { it.primaryId == activeDetail.primaryId }
      ?: activeDetail
    ItemDetailSheet(
      item = latestItem,
      compartments = allCompartments,
      onUpdateItem = onUpdateItem,
      onMoveCompartment = onMoveCompartment,
      onMarkStatus = onMarkStatus,
      onDismiss = { detailTarget = null }
    )
  }
}

@Composable
private fun FoodBentoCard(
  item: DisplayItem,
  onCardClick: () -> Unit,
  onDecrement: () -> Unit,
  onIncrement: () -> Unit
) {
  val emoji = (EmojiIconRepository.iconFor(item.iconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val freshnessBg = when (item.freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> LimeFresh
  }
  val expiryText = when {
    item.minDaysLeft < 0 -> stringResource(R.string.urgent_badge_overdue_fmt, -item.minDaysLeft)
    item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, item.minDaysLeft)
  }
  val minusCd = stringResource(R.string.bento_qty_minus_cd)
  val plusCd = stringResource(R.string.bento_qty_plus_cd)

  NeoBrutalistCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = SurfaceWhite,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 166.dp)
        .clickable(role = Role.Button, onClick = onCardClick)
        .padding(11.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top row: 48.dp emoji icon box + tilted freshness status pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = emoji, fontSize = 28.sp)
        }
        StickerBadge(
          text = expiryText,
          containerColor = freshnessBg,
          tiltDegrees = -2f,
          mono = true
        )
      }

      Spacer(Modifier.height(8.dp))

      // Middle: full 2-line Vietnamese item name + group tag
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
          text = item.name,
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          lineHeight = 18.sp,
          color = InkBlack,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PaperCream)
            .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
          Text(
            text = item.group.labelVi,
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = InkBlack,
            maxLines = 1
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // Bottom row: crisp 30.dp x 30.dp − [qty] + stepper buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        NeoBrutalistButton(
          onClick = onDecrement,
          modifier = Modifier.size(30.dp).semantics { contentDescription = minusCd },
          containerColor = if (item.quantity <= 1) Color(0xFFFFD6DF) else SurfaceWhite,
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "−",
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = InkBlack
          )
        }

        Text(
          text = if (item.unit.isNullOrBlank()) "${item.quantity}"
          else stringResource(R.string.bento_qty_unit_fmt, item.quantity, item.unit),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        NeoBrutalistButton(
          onClick = onIncrement,
          modifier = Modifier.size(30.dp).semantics { contentDescription = plusCd },
          containerColor = LimeFresh,
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "+",
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = InkBlack
          )
        }
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/bento/ItemSheets.kt` (848 lines)

```kotlin
package com.truong.fridgezero.ui.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.ConfigurableShelfLifeRepository
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.StorageZone
import com.truong.fridgezero.domain.zoneForCompartment
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import com.truong.fridgezero.ui.icons.FoodIcon
import com.truong.fridgezero.ui.icons.IconEntry
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterSoft
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val UNITS = listOf("cái", "gói", "hộp", "kg", "chai", "quả")
private val QUICK_EXPIRY_CHIPS = listOf(
  3 to R.string.quick_add_3d,
  7 to R.string.quick_add_7d,
  14 to R.string.quick_add_14d,
  30 to R.string.quick_add_30d
)
private val DEFAULT_FAVORITE_KEYS = listOf("milk", "egg", "chicken", "carrot", "lettuce", "salmon", "orange_juice", "apple")

private fun suggestCompartmentId(
  group: FoodGroup,
  iconKey: String,
  compartments: List<CompartmentUi>
): String? {
  if (compartments.isEmpty()) return null
  val preferredZone = when {
    iconKey in setOf("ice_cream", "dumpling", "shrimp") -> StorageZone.FREEZER
    group == FoodGroup.RAU -> StorageZone.DRAWER
    else -> StorageZone.CHILLED
  }
  return compartments.firstOrNull { zoneForCompartment(it) == preferredZone }?.id
    ?: compartments.firstOrNull { zoneForCompartment(it) == StorageZone.CHILLED }?.id
    ?: compartments.first().id
}

/**
 * Fast Add Item Sheet (<= 3 taps) used from both CompartmentBentoScreen (+ Thêm)
 * and HomeScreen's floating "+ Thêm nhanh" CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddItemSheet(
  compartments: List<CompartmentUi>,
  initialCompartmentId: String?,
  recentKeys: List<String>,
  onSaveItem: (compartmentId: String, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedCompartmentId by remember(initialCompartmentId, compartments) {
    mutableStateOf(
      initialCompartmentId
        ?: suggestCompartmentId(FoodGroup.KHAC, "milk", compartments)
        ?: compartments.firstOrNull()?.id.orEmpty()
    )
  }
  var compartmentPinnedManually by remember(initialCompartmentId) {
    mutableStateOf(initialCompartmentId != null)
  }
  var selectedIconKey by remember { mutableStateOf("milk") }
  var itemName by remember { mutableStateOf("") }
  var iconQuery by remember { mutableStateOf("") }
  var quantity by remember { mutableIntStateOf(1) }
  var selectedUnit by remember { mutableStateOf<String?>("hộp") }
  var daysLeftText by remember { mutableStateOf("") }
  var expiryPinnedManually by remember { mutableStateOf(false) }
  var autoSuggestedZoneLabel by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  val zoneChilledLabel = stringResource(R.string.zone_label_chilled)
  val zoneDrawerLabel = stringResource(R.string.zone_label_drawer)
  val zoneFreezerLabel = stringResource(R.string.zone_label_freezer)

  val quickKeys = remember(recentKeys) {
    (recentKeys + DEFAULT_FAVORITE_KEYS).distinct().take(8)
  }
  val quickEntries = remember(quickKeys) {
    quickKeys.mapNotNull { key -> EmojiIconRepository.entries.firstOrNull { it.key == key } }
  }
  val filteredIcons = remember(iconQuery) { EmojiIconRepository.search(iconQuery) }

  fun applySuggestedExpiry(iconKey: String, compId: String) {
    val selectedComp = compartments.firstOrNull { it.id == compId }
    val zone = selectedComp?.let(::zoneForCompartment) ?: zoneForCompartment(compId)
    val suggested = ConfigurableShelfLifeRepository.suggestDays(
      iconKey = iconKey,
      group = EmojiIconRepository.groupFor(iconKey),
      zone = zone
    )
    if (suggested != null) {
      daysLeftText = suggested.toString()
      autoSuggestedZoneLabel = when (zone) {
        StorageZone.CHILLED -> zoneChilledLabel
        StorageZone.DRAWER -> zoneDrawerLabel
        StorageZone.FREEZER -> zoneFreezerLabel
      }
    } else {
      autoSuggestedZoneLabel = null
    }
  }

  fun pickIcon(entry: IconEntry, fillName: Boolean) {
    selectedIconKey = entry.key
    if (fillName || itemName.isBlank()) {
      itemName = entry.label
    }
    if (!compartmentPinnedManually) {
      suggestCompartmentId(entry.group, entry.key, compartments)?.let {
        selectedCompartmentId = it
      }
    }
    expiryPinnedManually = false
    applySuggestedExpiry(entry.key, selectedCompartmentId)
  }

  fun resetForNextEntry() {
    itemName = ""
    iconQuery = ""
    quantity = 1
    daysLeftText = ""
    expiryPinnedManually = false
    autoSuggestedZoneLabel = null
  }

  val parsedDays = daysLeftText.toIntOrNull()
  val canSave = itemName.isNotBlank() && parsedDays != null && selectedCompartmentId.isNotEmpty()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PaperCream,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(R.string.add_item_title).uppercase(),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
        val previewEmoji = (EmojiIconRepository.iconFor(selectedIconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
        StickerBadge(text = previewEmoji, containerColor = LimeFresh, tiltDegrees = 2f)
      }

      // Recent / quick-pick chips (1-tap fills icon, name & smart compartment)
      Text(
        text = stringResource(R.string.recent_items_label),
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = InkBlack
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(quickEntries, key = { it.key }) { entry ->
          val active = selectedIconKey == entry.key && itemName == entry.label
          NeoBrutalistButton(
            onClick = { pickIcon(entry, fillName = true) },
            containerColor = if (active) LimeFresh else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(text = entry.glyph, fontSize = 15.sp)
            Text(
              text = entry.label,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      // Item name + icon search row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        BrutalInputBox(
          value = itemName,
          onValueChange = { itemName = it },
          placeholder = stringResource(R.string.quick_add_name_placeholder),
          modifier = Modifier.weight(1.3f)
        )
        BrutalInputBox(
          value = iconQuery,
          onValueChange = { iconQuery = it },
          placeholder = stringResource(R.string.search_icon_hint),
          modifier = Modifier.weight(1f)
        )
      }

      // Searchable emoji icon row
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filteredIcons, key = { it.key }) { entry ->
          val selected = selectedIconKey == entry.key
          NeoBrutalistButton(
            onClick = { pickIcon(entry, fillName = false) },
            containerColor = if (selected) ButterSoft else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(text = entry.glyph, fontSize = 18.sp)
          }
        }
      }

      // Compartment chips
      Text(
        text = stringResource(R.string.target_compartment_label),
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = InkBlack
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(compartments, key = { it.id }) { comp ->
          val selected = comp.id == selectedCompartmentId
          NeoBrutalistButton(
            onClick = {
              selectedCompartmentId = comp.id
              compartmentPinnedManually = true
              if (!expiryPinnedManually) {
                applySuggestedExpiry(selectedIconKey, comp.id)
              }
            },
            containerColor = if (selected) SkyInfo else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = comp.name,
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      // Quantity stepper + unit chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = stringResource(R.string.field_quantity),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { if (quantity > 1) quantity-- },
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)
        ) { Text("−", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
        Text(
          text = "$quantity",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 16.sp,
          color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { quantity++ },
          cornerRadius = 8.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)
        ) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(UNITS) { unit ->
          val active = selectedUnit == unit
          NeoBrutalistButton(
            onClick = { selectedUnit = if (active) null else unit },
            containerColor = if (active) LemonWarn else SurfaceWhite,
            cornerRadius = 8.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = unit,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      // Expiry quick chips + manual input + DatePicker + packaging hint
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = stringResource(R.string.quick_add_expiry_label),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
        Text(
          text = stringResource(R.string.expiry_packaging_hint),
          fontFamily = BodyFont,
          fontStyle = FontStyle.Italic,
          fontSize = 11.sp,
          color = InkBlack.copy(alpha = 0.65f)
        )
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(QUICK_EXPIRY_CHIPS) { (days, labelRes) ->
          val active = parsedDays == days
          NeoBrutalistButton(
            onClick = {
              daysLeftText = days.toString()
              expiryPinnedManually = true
              autoSuggestedZoneLabel = null
            },
            containerColor = if (active) LimeFresh else SurfaceWhite,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = stringResource(labelRes),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
        item {
          NeoBrutalistButton(
            onClick = { showDatePicker = true },
            containerColor = ButterSoft,
            cornerRadius = 10.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "📅 ${stringResource(R.string.pick_date_label)}",
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 12.sp,
              color = InkBlack
            )
          }
        }
      }

      BrutalInputBox(
        value = daysLeftText,
        onValueChange = { raw ->
          daysLeftText = raw.filter { it.isDigit() || it == '-' }.take(4)
          expiryPinnedManually = true
          autoSuggestedZoneLabel = null
        },
        placeholder = stringResource(R.string.quick_add_expiry_required_hint),
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth()
      )
      autoSuggestedZoneLabel?.let { zoneLabel ->
        Text(
          text = stringResource(R.string.quick_add_usda_auto_hint, zoneLabel),
          fontFamily = MonoFont,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = InkBlack.copy(alpha = 0.72f)
        )
      }

      // Dual Save / Save & Continue buttons
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        NeoBrutalistButton(
          onClick = {
            val d = parsedDays ?: return@NeoBrutalistButton
            onSaveItem(selectedCompartmentId, itemName, selectedIconKey, quantity, d, selectedUnit)
            resetForNextEntry()
          },
          enabled = canSave,
          modifier = Modifier.weight(1f),
          containerColor = LemonWarn
        ) {
          Text(
            text = stringResource(R.string.action_save_and_next),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = InkBlack
          )
        }
        NeoBrutalistButton(
          onClick = {
            val d = parsedDays ?: return@NeoBrutalistButton
            onSaveItem(selectedCompartmentId, itemName, selectedIconKey, quantity, d, selectedUnit)
            onDismiss()
          },
          enabled = canSave,
          modifier = Modifier.weight(1f),
          containerColor = LimeFresh
        ) {
          Text(
            text = stringResource(R.string.action_save),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = InkBlack
          )
        }
      }
    }
  }

  if (showDatePicker) {
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { millis ->
            val selectedDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
            val diff = (selectedDay - LocalDate.now().toEpochDay()).toInt()
            daysLeftText = diff.toString()
            expiryPinnedManually = true
            autoSuggestedZoneLabel = null
          }
          showDatePicker = false
        }) {
          Text(stringResource(R.string.action_save), fontWeight = FontWeight.ExtraBold, color = InkBlack)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text(stringResource(R.string.reminder_prompt_later), color = InkBlack)
        }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }
}

/**
 * Item Detail, Edit, Compartment Move & 1-Tap Direct Delete Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
  item: DisplayItem,
  compartments: List<CompartmentUi>,
  onUpdateItem: (id: Long, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onMoveCompartment: (ids: List<Long>, targetCompartmentId: String, newDaysLeft: Int?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(item.primaryId) { mutableStateOf(item.name) }
  var quantity by remember(item.primaryId, item.quantity) { mutableIntStateOf(item.quantity) }
  var unit by remember(item.primaryId, item.unit) { mutableStateOf(item.unit) }
  var daysText by remember(item.primaryId, item.minDaysLeft) { mutableStateOf(item.minDaysLeft.toString()) }
  var pendingMoveTargetId by remember { mutableStateOf<String?>(null) }

  val emoji = (EmojiIconRepository.iconFor(item.iconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val badgeColor = when (item.freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> LimeFresh
  }
  val expiryBadgeText = when {
    item.minDaysLeft < 0 -> stringResource(R.string.days_overdue_fmt, -item.minDaysLeft)
    item.minDaysLeft == 0 -> stringResource(R.string.days_today)
    else -> stringResource(R.string.days_left_fmt, item.minDaysLeft)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PaperCream,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(2.5.dp, InkBlack, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = emoji, fontSize = 32.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = item.name,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = InkBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StickerBadge(text = item.group.labelVi, containerColor = SurfaceWhite, tiltDegrees = 0f)
            StickerBadge(text = expiryBadgeText, containerColor = badgeColor, tiltDegrees = -1.5f, mono = true)
          }
        }
      }

      // Edit name + days + quantity
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BrutalInputBox(
          value = name,
          onValueChange = { name = it },
          placeholder = stringResource(R.string.field_name),
          modifier = Modifier.weight(1.6f)
        )
        BrutalInputBox(
          value = daysText,
          onValueChange = { raw -> daysText = raw.filter { it.isDigit() || it == '-' }.take(4) },
          placeholder = stringResource(R.string.field_days_left),
          keyboardType = KeyboardType.Number,
          modifier = Modifier.weight(0.8f)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NeoBrutalistButton(
          onClick = { if (quantity > 1) quantity-- },
          cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp)
        ) { Text("−", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
        Text(
          text = if (unit.isNullOrBlank()) "$quantity" else "$quantity $unit",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp,
          color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { quantity++ },
          cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp)
        ) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = InkBlack) }
        Spacer(Modifier.weight(1f))
        NeoBrutalistButton(
          onClick = {
            val d = daysText.toIntOrNull() ?: item.minDaysLeft
            onUpdateItem(item.primaryId, name, item.iconKey, quantity, d, unit)
            onDismiss()
          },
          enabled = name.isNotBlank() && daysText.toIntOrNull() != null,
          containerColor = LimeFresh,
          cornerRadius = 10.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
        ) {
          Text(
            text = stringResource(R.string.action_save),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack
          )
        }
      }

      // Move compartment
      val otherCompartments = compartments.filter { it.id != item.compartmentId }
      val fromComp = compartments.firstOrNull { it.id == item.compartmentId }
      val fromZone = fromComp?.let(::zoneForCompartment) ?: zoneForCompartment(item.compartmentId)
      if (otherCompartments.isNotEmpty()) {
        Text(
          text = stringResource(R.string.action_move_compartment),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(otherCompartments, key = { it.id }) { target ->
            NeoBrutalistButton(
              onClick = {
                val fromFreezer = fromZone == StorageZone.FREEZER
                val toFreezer = zoneForCompartment(target) == StorageZone.FREEZER
                if (fromFreezer != toFreezer) {
                  pendingMoveTargetId = target.id
                } else {
                  onMoveCompartment(item.itemIds, target.id, null)
                  onDismiss()
                }
              },
              containerColor = Color(0xFFE0F2FE),
              cornerRadius = 10.dp,
              borderWidth = 2.dp,
              shadowOffset = 2.dp,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = "→ ${target.name}",
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack
              )
            }
          }
        }
      }

      // Chilled <-> Freezer expiry recalculation prompt
      val moveTarget = pendingMoveTargetId
      if (moveTarget != null) {
        val targetComp = compartments.firstOrNull { it.id == moveTarget }
        val movingToFreezer = (targetComp?.let(::zoneForCompartment) ?: zoneForCompartment(moveTarget)) == StorageZone.FREEZER
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), containerColor = ButterSoft, cornerRadius = 12.dp, shadowOffset = 3.dp) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = stringResource(R.string.move_recalc_prompt),
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = InkBlack
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              NeoBrutalistButton(
                onClick = {
                  onMoveCompartment(item.itemIds, moveTarget, null)
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                containerColor = SurfaceWhite,
                cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp
              ) {
                Text(stringResource(R.string.move_keep_expiry), fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = InkBlack)
              }
              NeoBrutalistButton(
                onClick = {
                  val recalculated = if (movingToFreezer) (item.minDaysLeft.coerceAtLeast(0) + 30) else 3
                  onMoveCompartment(item.itemIds, moveTarget, recalculated)
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                containerColor = if (movingToFreezer) LilacAccent else LimeFresh,
                cornerRadius = 8.dp, borderWidth = 2.dp, shadowOffset = 2.dp
              ) {
                Text(
                  text = stringResource(if (movingToFreezer) R.string.move_add_freezer_days else R.string.move_set_chilled_days),
                  fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = InkBlack
                )
              }
            }
          }
        }
      }

      // 1-Tap Direct Deletion
      NeoBrutalistButton(
        onClick = {
          onMarkStatus(item.itemIds, ItemStatus.USED_UP)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        containerColor = CoralUrgent
      ) {
        Text(
          text = "🗑️ ${stringResource(R.string.action_delete)}",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = InkBlack
        )
      }
    }
  }
}

@Composable
private fun BrutalInputBox(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text
) {
  val shape = RoundedCornerShape(10.dp)
  Box(
    modifier = modifier
      .clip(shape)
      .background(SurfaceWhite)
      .border(2.dp, InkBlack, shape)
      .padding(horizontal = 10.dp, vertical = 9.dp)
  ) {
    if (value.isEmpty()) {
      Text(
        text = placeholder,
        fontFamily = BodyFont,
        fontSize = 12.sp,
        color = InkBlack.copy(alpha = 0.45f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      textStyle = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = InkBlack
      ),
      cursorBrush = SolidColor(InkBlack),
      modifier = Modifier.fillMaxWidth()
    )
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/designer/FridgeDesignerScreen.kt` (450 lines)

```kotlin
package com.truong.fridgezero.ui.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.data.Seed
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageSlot
import com.truong.fridgezero.ui.fridge3d.LocalFridge3DStageController
import com.truong.fridgezero.ui.fridge3d.ShellColors
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite

private val TEMPLATES = listOf(
  "single" to R.string.template_single,
  "top_freezer" to R.string.template_top_freezer,
  "bottom_freezer" to R.string.template_bottom_freezer,
  "side_by_side" to R.string.template_side_by_side,
  "french_door" to R.string.template_french_door,
  "mini" to R.string.template_mini
)

private fun colorLabel(key: String): Int = when (key) {
  "butter" -> R.string.color_butter
  "mint" -> R.string.color_mint
  "matte" -> R.string.color_matte
  else -> R.string.color_inox
}

/** Screen 1 — live 3D preview (top ~43%) + Neo-Brutalist controls + "CHỐT TỦ NÀY →". */
@Composable
fun FridgeDesignerScreen(
  initialFridge: FridgeEntity,
  initialCompartments: List<CompartmentEntity>,
  onConfirm: (FridgeEntity, List<CompartmentEntity>) -> Unit,
  modifier: Modifier = Modifier
) {
  var template by rememberSaveable { mutableStateOf(initialFridge.template) }
  var colorKey by rememberSaveable { mutableStateOf(ShellColors.normalizeKey(initialFridge.colorKey)) }
  var compartments by remember {
    mutableStateOf(initialCompartments.sortedBy { it.sort }.ifEmpty {
      Seed.compartmentsForTemplate(initialFridge.template, initialFridge.id)
    })
  }
  var openId by remember { mutableStateOf<String?>(null) }

  val fridgeId = initialFridge.id
  val doorFmt = stringResource(R.string.designer_new_door_fmt, compartments.size + 1)
  val drawerFmt = stringResource(R.string.designer_new_drawer_fmt, compartments.size + 1)

  fun applyTemplate(key: String) {
    template = key
    compartments = Seed.compartmentsForTemplate(key, fridgeId)
    openId = null
  }

  fun addCompartment(kind: CompartmentKind) {
    if (compartments.size >= MAX_COMPARTMENTS) return
    val c = CompartmentEntity(
      id = "c_" + System.currentTimeMillis().toString(36),
      fridgeId = fridgeId,
      name = if (kind == CompartmentKind.DOOR) doorFmt else drawerFmt,
      x = 0f, y = 0f, w = 1f, h = 1f,
      kind = kind,
      shelves = if (kind == CompartmentKind.DOOR) 2 else 1,
      sort = compartments.size
    )
    compartments = normalizeCompartmentLayout(compartments + c, template)
    openId = c.id
  }

  fun update(id: String, change: (CompartmentEntity) -> CompartmentEntity, relayout: Boolean = false) {
    val next = compartments.map { if (it.id == id) change(it) else it }
    compartments = if (relayout) normalizeCompartmentLayout(next, template) else next
  }

  fun remove(id: String) {
    if (compartments.size <= 1) return
    compartments = normalizeCompartmentLayout(compartments.filterNot { it.id == id }, template)
    if (openId == id) openId = null
  }

  val screenH = LocalConfiguration.current.screenHeightDp.dp
  val templateLabel = TEMPLATES.firstOrNull { it.first == template }?.second ?: R.string.template_single
  val stageController = LocalFridge3DStageController.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Top bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          stringResource(R.string.designer_title),
          fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
          fontSize = 22.sp, color = InkBlack
        )
        Text(
          stringResource(R.string.designer_subtitle),
          fontFamily = BodyFont, fontWeight = FontWeight.Medium,
          fontSize = 12.sp, color = InkBlack.copy(alpha = 0.7f)
        )
      }
      StickerBadge(stringResource(R.string.designer_step_badge), containerColor = LemonWarn, mono = true)
    }

    // Live 3D preview (~43% of screen height)
    NeoBrutalistCard(
      transparentInterior = true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(screenH * 0.43f)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Fridge3DStageSlot(
          colorKey = colorKey,
          compartments = compartments,
          template = template,
          openCompartmentId = openId,
          surfaceBg = "#FFFFFF",
          cornerRadius = 16.dp,
          backdropColor = SurfaceWhite,
          onDoorClick = { id -> openId = if (openId == id) null else id },
          showStickers = false,
          modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
          StickerBadge(
            stringResource(
              if (stageController.use2DFallback) R.string.fallback_2d_badge else R.string.designer_swipe_hint
            ),
            containerColor = if (stageController.use2DFallback) LemonWarn else SurfaceWhite,
            tiltDegrees = 0f
          )
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
          StickerBadge(stringResource(templateLabel), containerColor = LimeFresh, tiltDegrees = 1f)
        }
      }
    }

    // Scrollable controls
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(top = 14.dp, bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      SectionTitle(stringResource(R.string.designer_section_template))
      LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(TEMPLATES) { (key, label) ->
          NeoBrutalistButton(
            onClick = { applyTemplate(key) },
            containerColor = if (template == key) LimeFresh else SurfaceWhite,
            shadowOffset = 3.dp,
            borderWidth = 2.dp
          ) {
            Text(stringResource(label), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
          }
        }
      }

      SectionTitle(stringResource(R.string.designer_section_color))
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ShellColors.all.forEach { spec ->
          val selected = colorKey == spec.key
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            NeoBrutalistButton(
              onClick = { colorKey = spec.key },
              modifier = Modifier.fillMaxWidth(),
              containerColor = if (selected) LimeFresh else SurfaceWhite,
              shadowOffset = 3.dp,
              borderWidth = 2.dp,
              contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(spec.color)
                  .border(2.dp, InkBlack, CircleShape)
              )
            }
            Text(
              stringResource(colorLabel(spec.key)),
              fontFamily = BodyFont, fontWeight = FontWeight.Bold,
              fontSize = 11.sp, color = InkBlack,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      SectionTitle(stringResource(R.string.designer_section_compartments))
      Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        compartments.forEachIndexed { index, c ->
          CompartmentRow(
            index = index,
            compartment = c,
            isSelected = c.id == openId,
            canRemove = compartments.size > 1,
            onFocus = { openId = if (openId == c.id) null else c.id },
            onName = { name -> update(c.id, { it.copy(name = name) }) },
            onToggleKind = {
              update(
                c.id,
                {
                  val next = if (it.kind == CompartmentKind.DOOR) CompartmentKind.DRAWER else CompartmentKind.DOOR
                  it.copy(kind = next)
                },
                relayout = true
              )
            },
            onShelves = { delta -> update(c.id, { it.copy(shelves = (it.shelves + delta).coerceIn(1, 5)) }) },
            onRemove = { remove(c.id) }
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          NeoBrutalistButton(
            onClick = { addCompartment(CompartmentKind.DOOR) },
            modifier = Modifier.weight(1f),
            enabled = compartments.size < MAX_COMPARTMENTS,
            containerColor = LilacAccent,
            shadowOffset = 3.dp
          ) {
            Text(stringResource(R.string.designer_add_door), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
          }
          NeoBrutalistButton(
            onClick = { addCompartment(CompartmentKind.DRAWER) },
            modifier = Modifier.weight(1f),
            enabled = compartments.size < MAX_COMPARTMENTS,
            containerColor = SkyInfo,
            shadowOffset = 3.dp
          ) {
            Text(stringResource(R.string.designer_add_drawer), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
          }
        }
        if (compartments.size >= MAX_COMPARTMENTS) {
          Text(stringResource(R.string.designer_max_reached), fontFamily = MonoFont, fontSize = 11.sp, color = InkBlack.copy(alpha = 0.6f))
        }
      }
      Spacer(Modifier.height(4.dp))
    }

    // Bottom CTA
    NeoBrutalistButton(
      onClick = {
        val fridge = initialFridge.copy(template = template, colorKey = colorKey)
        onConfirm(fridge, normalizeSort(compartments))
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      containerColor = LimeFresh,
      contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp, horizontal = 16.dp)
    ) {
      Text(
        stringResource(R.string.designer_confirm),
        fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, color = InkBlack
      )
    }
  }
}

private fun normalizeSort(list: List<CompartmentEntity>): List<CompartmentEntity> =
  list.mapIndexed { i, c -> c.copy(sort = i) }

@Composable
private fun SectionTitle(text: String) {
  Text(
    text,
    modifier = Modifier.padding(horizontal = 16.dp),
    fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp, color = InkBlack
  )
}

@Composable
private fun CompartmentRow(
  index: Int,
  compartment: CompartmentEntity,
  isSelected: Boolean,
  canRemove: Boolean,
  onFocus: () -> Unit,
  onName: (String) -> Unit,
  onToggleKind: () -> Unit,
  onShelves: (Int) -> Unit,
  onRemove: () -> Unit
) {
  val removeCd = stringResource(R.string.designer_remove_cd)
  val minusCd = stringResource(R.string.designer_shelves_minus_cd)
  val plusCd = stringResource(R.string.designer_shelves_plus_cd)
  NeoBrutalistCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = if (isSelected) Color(0xFFFEF9C3) else SurfaceWhite,
    cornerRadius = 14.dp,
    shadowOffset = 3.dp,
    borderWidth = if (isSelected) 2.5.dp else 2.dp
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (isSelected) LimeFresh else LemonWarn)
            .border(2.dp, InkBlack, CircleShape)
            .clickable(onClick = onFocus),
          contentAlignment = Alignment.Center
        ) {
          Text("${index + 1}", fontFamily = MonoFont, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = InkBlack)
        }
        BasicTextField(
          value = compartment.name,
          onValueChange = onName,
          singleLine = true,
          textStyle = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkBlack),
          cursorBrush = SolidColor(InkBlack),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, InkBlack, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        NeoBrutalistButton(
          onClick = onRemove,
          enabled = canRemove,
          containerColor = CoralUrgent,
          shadowOffset = 2.dp,
          borderWidth = 2.dp,
          cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.semantics { contentDescription = removeCd }
        ) {
          Text(stringResource(R.string.designer_remove), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = InkBlack)
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val isDoor = compartment.kind == CompartmentKind.DOOR
        NeoBrutalistButton(
          onClick = onToggleKind,
          containerColor = if (isDoor) LilacAccent else SkyInfo,
          shadowOffset = 2.dp,
          borderWidth = 2.dp,
          cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            stringResource(if (isDoor) R.string.kind_door else R.string.kind_drawer),
            fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = InkBlack
          )
        }
        Spacer(Modifier.weight(1f))
        NeoBrutalistButton(
          onClick = { onShelves(-1) },
          shadowOffset = 2.dp, borderWidth = 2.dp, cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
          modifier = Modifier.semantics { contentDescription = minusCd }
        ) { Text("−", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = InkBlack) }
        Text(
          stringResource(R.string.designer_shelves_fmt, compartment.shelves),
          fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InkBlack
        )
        NeoBrutalistButton(
          onClick = { onShelves(1) },
          shadowOffset = 2.dp, borderWidth = 2.dp, cornerRadius = 8.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
          modifier = Modifier.semantics { contentDescription = plusCd }
        ) { Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = InkBlack) }
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/designer/FridgeLayoutEditor.kt` (44 lines)

```kotlin
package com.truong.fridgezero.ui.designer

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind

const val MAX_COMPARTMENTS = 6

private const val MARGIN_X = 0.04f
private const val FULL_W = 0.92f
private const val TOP = 0.02f
private const val TOTAL_H = 0.96f
private const val GAP = 0.013f

/**
 * Recomputes normalized x, y, w, h for [list] so compartments never overlap, whatever the
 * add/remove/kind changes. Side-by-side puts the first compartment in a full-height left column and
 * stacks the rest on the right; every other template stacks vertically (drawers get less height).
 */
fun normalizeCompartmentLayout(
  list: List<CompartmentEntity>,
  template: String
): List<CompartmentEntity> {
  if (list.isEmpty()) return list

  fun stack(items: List<CompartmentEntity>, x: Float, w: Float): List<CompartmentEntity> {
    val weights = items.map { if (it.kind == CompartmentKind.DRAWER) 0.65f else 1f }
    val avail = TOTAL_H - GAP * (items.size - 1)
    val sum = weights.sum()
    var y = TOP
    return items.mapIndexed { i, c ->
      val h = avail * weights[i] / sum
      c.copy(x = x, y = y, w = w, h = h).also { y += h + GAP }
    }
  }

  val placed = if (template == "side_by_side" && list.size >= 2) {
    val left = list.first().copy(x = MARGIN_X, y = TOP, w = 0.41f, h = TOTAL_H)
    listOf(left) + stack(list.drop(1), 0.465f, 0.495f)
  } else {
    stack(list, MARGIN_X, FULL_W)
  }
  return placed.mapIndexed { index, c -> c.copy(sort = index) }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/fridge3d/Fridge2DFallback.kt` (294 lines)

```kotlin
package com.truong.fridgezero.ui.fridge3d

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.SkyInfo
import com.truong.fridgezero.ui.theme.SurfaceWhite

data class FallbackCompartmentStat(
  val itemCount: Int,
  val worstFreshness: Freshness
)

/**
 * Pure Compose Neo-Brutalist 2D refrigerator fallback used when WebGL exceeds the bounded
 * crash-recovery budget (`> 2` failures within 60s). Keeps all Designer & Home flows interactive.
 */
@Composable
fun Fridge2DFallback(
  colorKey: String,
  compartments: List<CompartmentEntity>,
  modifier: Modifier = Modifier,
  openCompartmentId: String? = null,
  compartmentStats: Map<String, FallbackCompartmentStat> = emptyMap(),
  showItemCounts: Boolean = false,
  surfaceBg: Color? = null,
  onDoorClick: ((compartmentId: String) -> Unit)? = null
) {
  val normalizedKey = ShellColors.normalizeKey(colorKey)
  val shellColor = ShellColors.forKey(normalizedKey).color
  val handleColor = if (normalizedKey == ShellColors.Matte.key) {
    Color(0xFF94A3B8)
  } else {
    InkBlack
  }

  val containerModifier = if (surfaceBg != null) {
    modifier.background(surfaceBg)
  } else {
    modifier
  }

  BoxWithConstraints(
    modifier = containerModifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center
  ) {
    val feetHeight = 8.dp
    val availH = (maxHeight - feetHeight).coerceAtLeast(120.dp)
    val availW = maxWidth.coerceAtLeast(80.dp)
    val aspect = 0.54f
    val cabH = minOf(availH, availW / aspect)
    val cabW = cabH * aspect

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      NeoBrutalistCard(
        modifier = Modifier.size(width = cabW, height = cabH),
        containerColor = shellColor,
        cornerRadius = 16.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp
      ) {
        BoxWithConstraints(
          modifier = Modifier
            .fillMaxSize()
            .padding(7.dp)
        ) {
          val innerW = maxWidth
          val innerH = maxHeight
          val inset = 3.dp

          compartments.forEachIndexed { index, comp ->
            val xOff = innerW * comp.x + inset
            val yOff = innerH * comp.y + inset
            val doorW = (innerW * comp.w - inset * 2).coerceAtLeast(36.dp)
            val doorH = (innerH * comp.h - inset * 2).coerceAtLeast(36.dp)
            val isOpen = comp.id == openCompartmentId
            val isRightHinge = (comp.kind == CompartmentKind.DOOR) && (comp.w < 0.6f && comp.x > 0.4f)
            val doorShape = RoundedCornerShape(10.dp)

            Box(
              modifier = Modifier
                .offset(x = xOff, y = yOff)
                .size(width = doorW, height = doorH)
                .clip(doorShape)
                .background(
                  color = if (isOpen) ButterPale.copy(alpha = 0.45f) else Color.Transparent,
                  shape = doorShape
                )
                .border(2.dp, InkBlack, doorShape)
                .clickable(
                  enabled = onDoorClick != null,
                  role = Role.Button,
                  onClick = { onDoorClick?.invoke(comp.id) }
                )
                .padding(6.dp)
            ) {
              // Handle indicator
              if (comp.kind == CompartmentKind.DOOR) {
                Box(
                  modifier = Modifier
                    .align(if (isRightHinge) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 2.dp)
                    .width(4.dp)
                    .height((doorH * 0.28f).coerceIn(14.dp, 40.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
                )
              } else {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .width((doorW * 0.34f).coerceIn(24.dp, 64.dp))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
                )
              }

              // Signature mini magnets on top compartment
              if (index == 0) {
                Row(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 8.dp),
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .clip(CircleShape)
                      .background(LemonWarn)
                      .border(1.dp, InkBlack, CircleShape)
                  )
                  Box(
                    modifier = Modifier
                      .size(width = 10.dp, height = 6.dp)
                      .clip(RoundedCornerShape(2.dp))
                      .background(CoralUrgent)
                      .border(1.dp, InkBlack, RoundedCornerShape(2.dp))
                  )
                }
              }

              // Compartment name + optional count badge
              val stat = compartmentStats[comp.id]
              val badgeShape = RoundedCornerShape(8.dp)
              Row(
                modifier = Modifier
                  .align(Alignment.Center)
                  .background(SurfaceWhite, badgeShape)
                  .border(2.dp, InkBlack, badgeShape)
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                if (showItemCounts && stat != null) {
                  val dotColor = when (stat.worstFreshness) {
                    Freshness.BAD -> CoralUrgent
                    Freshness.SOON -> LemonWarn
                    Freshness.OK -> LimeFresh
                  }
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(dotColor)
                      .border(1.dp, InkBlack, CircleShape)
                  )
                  Text(
                    text = comp.name,
                    modifier = Modifier.weight(1f, fill = false),
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = InkBlack,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Box(
                    modifier = Modifier
                      .defaultMinSize(minWidth = 22.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(InkBlack)
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "${stat.itemCount}",
                      fontFamily = MonoFont,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 13.sp,
                      lineHeight = 14.sp,
                      color = LimeFresh,
                      softWrap = false,
                      maxLines = 1
                    )
                  }
                } else {
                  val kindDot = if (comp.kind == CompartmentKind.DOOR) LilacAccent else SkyInfo
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .clip(CircleShape)
                      .background(kindDot)
                      .border(1.dp, InkBlack, CircleShape)
                  )
                  Text(
                    text = comp.name,
                    fontFamily = BodyFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = InkBlack,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      // Grounded cabinet feet
      Row(
        modifier = Modifier
          .width(cabW - 28.dp)
          .padding(top = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Spacer(
          modifier = Modifier
            .size(width = 16.dp, height = feetHeight)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(InkBlack)
        )
        Spacer(
          modifier = Modifier
            .size(width = 16.dp, height = feetHeight)
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(InkBlack)
        )
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/fridge3d/Fridge3DView.kt` (883 lines)

```kotlin
package com.truong.fridgezero.ui.fridge3d

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Outline
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truong.fridgezero.data.AppConfig
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.ui.theme.DotGridSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "Fridge3D"
private const val PROJECTION_EPS = 0.005f
private const val DEFAULT_SURFACE_BG = DotGridSpec.PAPER_HEX
private const val ALLOWED_ASSET_PREFIX = "file:///android_asset/fridge3d/"
private const val ALLOWED_ABOUT_BLANK = "about:blank"
internal const val LOW_MEM_THRESHOLD_BYTES = 4L * 1024 * 1024 * 1024

internal fun isAllowedFridge3dUrl(url: String?): Boolean {
  if (url.isNullOrEmpty()) return false
  if (url == ALLOWED_ABOUT_BLANK) return true
  return url.startsWith(ALLOWED_ASSET_PREFIX) && !url.contains("..")
}

internal fun resolveWebGlQuality(
  isLowRamDevice: Boolean,
  totalMemBytes: Long,
  forceLiteMode: Boolean
): String {
  val lowMem = isLowRamDevice || (totalMemBytes in 1L..LOW_MEM_THRESHOLD_BYTES)
  return if (forceLiteMode || lowMem) "lite" else "high"
}

private fun isLowRamOrLowMemDevice(context: Context): Boolean {
  val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
  if (am.isLowRamDevice) return true
  val memInfo = ActivityManager.MemoryInfo()
  am.getMemoryInfo(memInfo)
  return memInfo.totalMem in 1L..LOW_MEM_THRESHOLD_BYTES
}

internal enum class WebGlRecoveryAction {
  REBUILD_IN_LITE_AND_PERSIST,
  REBUILD_IN_LITE,
  FALLBACK_TO_2D
}

/**
 * Sliding-window recovery budget: up to [maxRecoveries] WebView rebuilds within [windowMs]
 * before degrading cleanly to the Compose Neo-Brutalist 2D fallback.
 */
internal class BoundedWebGlCrashTracker(
  private val windowMs: Long = 60_000L,
  private val maxRecoveries: Int = 2
) {
  private val timestamps = ArrayDeque<Long>()

  fun recordCrash(nowMs: Long): WebGlRecoveryAction {
    pruneOld(nowMs)
    timestamps.addLast(nowMs)
    val count = timestamps.size
    return when {
      count == 1 -> WebGlRecoveryAction.REBUILD_IN_LITE_AND_PERSIST
      count <= maxRecoveries -> WebGlRecoveryAction.REBUILD_IN_LITE
      else -> WebGlRecoveryAction.FALLBACK_TO_2D
    }
  }

  fun recentCrashCount(nowMs: Long): Int {
    pruneOld(nowMs)
    return timestamps.size
  }

  fun reset() {
    timestamps.clear()
  }

  private fun pruneOld(nowMs: Long) {
    while (timestamps.isNotEmpty() && (nowMs - timestamps.first()) > windowMs) {
      timestamps.removeFirst()
    }
  }
}

data class DoorProjection(
  val xFrac: Float,
  val yFrac: Float,
  val visible: Boolean
)

data class StageBoundsPx(
  val left: Int = 0,
  val top: Int = 0,
  val width: Int = 0,
  val height: Int = 0
) {
  val isValid: Boolean get() = width > 0 && height > 0
}

@Stable
class Fridge3DStageController {
  var rootCoordinates: LayoutCoordinates? = null

  var activeToken by mutableStateOf<Any?>(null)
    private set
  var boundsPx by mutableStateOf(StageBoundsPx())
    private set
  var colorKey by mutableStateOf("inox")
    private set
  var compartments by mutableStateOf<List<CompartmentEntity>>(emptyList())
    private set
  var template by mutableStateOf("top_freezer")
    private set
  var openCompartmentId by mutableStateOf<String?>(null)
    private set
  var playWelcomeSpin by mutableStateOf(false)
    private set
  var showStickers by mutableStateOf(false)
    private set
  var surfaceBg by mutableStateOf(DEFAULT_SURFACE_BG)
    private set
  var cornerRadius by mutableStateOf(0.dp)
    private set
  var backdropColor by mutableStateOf<Color?>(null)
    private set
  var resetNonce by mutableIntStateOf(0)
    private set
  var itemAddedPulseNonce by mutableIntStateOf(0)
    private set
  var use2DFallback by mutableStateOf(false)
    private set
  var fallbackResetNonce by mutableIntStateOf(0)
    private set

  internal var onWelcomeSpinConsumed: () -> Unit = {}
  internal var onDoorClick: ((String) -> Unit)? = null
  internal var onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)? = null

  val isVisible: Boolean
    get() = activeToken != null && boundsPx.isValid && !use2DFallback

  fun set2DFallback(active: Boolean) {
    if (use2DFallback != active) {
      use2DFallback = active
    }
  }

  fun updateSlot(
    token: Any,
    colorKey: String,
    compartments: List<CompartmentEntity>,
    template: String,
    openCompartmentId: String?,
    playWelcomeSpin: Boolean,
    showStickers: Boolean,
    surfaceBg: String,
    cornerRadius: Dp,
    backdropColor: Color?,
    onWelcomeSpinConsumed: () -> Unit,
    onDoorClick: ((String) -> Unit)?,
    onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)?
  ) {
    val slotSwitched = activeToken !== token
    activeToken = token
    if (slotSwitched && use2DFallback) {
      use2DFallback = false
      fallbackResetNonce += 1
    }
    this.colorKey = colorKey
    this.compartments = compartments
    this.template = template
    this.openCompartmentId = openCompartmentId
    this.playWelcomeSpin = playWelcomeSpin
    this.showStickers = showStickers
    this.surfaceBg = surfaceBg
    this.cornerRadius = cornerRadius
    this.backdropColor = backdropColor
    this.onWelcomeSpinConsumed = onWelcomeSpinConsumed
    this.onDoorClick = onDoorClick
    this.onProjectionsUpdated = onProjectionsUpdated
  }

  fun updateBounds(token: Any, bounds: StageBoundsPx) {
    if (activeToken === token && bounds.isValid && boundsPx != bounds) {
      boundsPx = bounds
    }
  }

  fun requestResetOrbit() {
    resetNonce += 1
  }

  fun requestItemAddedBounce() {
    itemAddedPulseNonce += 1
  }

  fun releaseSlot(token: Any) {
    if (activeToken === token) {
      activeToken = null
      boundsPx = StageBoundsPx()
      onDoorClick = null
      onProjectionsUpdated = null
      if (use2DFallback) {
        use2DFallback = false
        fallbackResetNonce += 1
      }
    }
  }
}

@Composable
fun rememberFridge3DStageController(): Fridge3DStageController = remember { Fridge3DStageController() }

val LocalFridge3DStageController = staticCompositionLocalOf<Fridge3DStageController> {
  error("Fridge3DStageController is not provided in CompositionLocal")
}

@Keep
internal class FridgeJsBridge(
  private val onDoorTappedCallback: (String) -> Unit,
  private val onStickersProjectedCallback: (String) -> Unit,
  private val onWelcomeSpinFinishedCallback: () -> Unit,
  private val onWebGlContextLostCallback: () -> Unit
) {
  @JavascriptInterface
  fun onDoorTapped(compartmentId: String) = onDoorTappedCallback(compartmentId)

  @JavascriptInterface
  fun onStickersProjected(json: String) = onStickersProjectedCallback(json)

  @JavascriptInterface
  fun onWelcomeSpinFinished() = onWelcomeSpinFinishedCallback()

  @JavascriptInterface
  fun onWebGlContextLost() = onWebGlContextLostCallback()
}

/** Optional framed backdrop (e.g. `SurfaceWhite` card face in Designer) drawn strictly behind the WebView. */
@Composable
fun Fridge3DStageBackdrop(
  controller: Fridge3DStageController,
  modifier: Modifier = Modifier
) {
  val backdrop = controller.backdropColor ?: return
  if (!controller.isVisible || !controller.boundsPx.isValid) return
  val b = controller.boundsPx
  val density = LocalDensity.current
  val shape = if (controller.cornerRadius > 0.dp) RoundedCornerShape(controller.cornerRadius) else RectangleShape
  Box(
    modifier = modifier
      .offset { IntOffset(b.left, b.top) }
      .size(with(density) { b.width.toDp() }, with(density) { b.height.toDp() })
      .background(backdrop, shape)
  )
}

/**
 * Single shared WebView host mounted once at [com.truong.fridgezero.MainActivity] root.
 * Resolves `lite` vs `high` quality before creating the WebView and degrades to [Fridge2DFallback]
 * after >2 WebGL/renderer crashes in 60 seconds.
 */
@Composable
fun SharedFridge3DWebView(
  controller: Fridge3DStageController,
  appConfig: AppConfig,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val persistedForceLite by appConfig.forceLiteMode.collectAsStateWithLifecycle(initialValue = null)
  val resolvedPersistedLite = persistedForceLite ?: return
  val hardwareRequiresLite = remember(context) { isLowRamOrLowMemDevice(context) }
  var runtimeForceLite by remember { mutableStateOf(false) }
  val crashTracker = remember { BoundedWebGlCrashTracker() }

  LaunchedEffect(controller.fallbackResetNonce) {
    crashTracker.reset()
  }

  if (controller.use2DFallback) return

  SharedFridge3DWebViewHost(
    controller = controller,
    appConfig = appConfig,
    hardwareRequiresLite = hardwareRequiresLite,
    persistedForceLite = resolvedPersistedLite,
    runtimeForceLite = runtimeForceLite,
    onRequireRuntimeLite = { runtimeForceLite = true },
    crashTracker = crashTracker,
    modifier = modifier
  )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SharedFridge3DWebViewHost(
  controller: Fridge3DStageController,
  appConfig: AppConfig,
  hardwareRequiresLite: Boolean,
  persistedForceLite: Boolean,
  runtimeForceLite: Boolean,
  onRequireRuntimeLite: () -> Unit,
  crashTracker: BoundedWebGlCrashTracker,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  val latestHaptic by rememberUpdatedState(haptic)

  val projectionCache = remember { HashMap<String, DoorProjection>() }
  var pageReady by remember { mutableStateOf(false) }
  var recoveryKey by remember { mutableIntStateOf(0) }
  var lastValidBounds by remember { mutableStateOf(StageBoundsPx()) }

  LaunchedEffect(controller.activeToken, recoveryKey) {
    projectionCache.clear()
  }

  LaunchedEffect(controller.boundsPx) {
    if (controller.boundsPx.isValid) {
      lastValidBounds = controller.boundsPx
    }
  }

  val offsetXDp = if (controller.boundsPx.isValid) controller.boundsPx.left / density.density else 0f
  val offsetYDp = if (controller.boundsPx.isValid) controller.boundsPx.top / density.density else 0f

  val configJson = remember(
    controller.template,
    controller.colorKey,
    controller.compartments,
    controller.openCompartmentId,
    controller.showStickers,
    controller.surfaceBg,
    offsetXDp,
    offsetYDp
  ) {
    buildConfigJson(
      template = controller.template,
      colorKey = ShellColors.normalizeKey(controller.colorKey),
      compartments = controller.compartments,
      openCompartmentId = controller.openCompartmentId,
      showStickers = controller.showStickers,
      surfaceBg = controller.surfaceBg,
      dotGridOffsetXDp = offsetXDp,
      dotGridOffsetYDp = offsetYDp
    )
  }

  val webViewDestroyed = remember(recoveryKey) { AtomicBoolean(false) }

  // Deliberately keyed on (context, recoveryKey) only so persisting forceLiteMode mid-session
  // does not trigger a duplicate WebView creation beyond the single bounded recovery.
  val webView = remember(context, recoveryKey) {
    val instanceCrashHandled = AtomicBoolean(false)
    val initialQuality = if (hardwareRequiresLite || persistedForceLite || runtimeForceLite) {
      "lite"
    } else {
      "high"
    }
    Log.d(TAG, "Initializing WebView (recoveryKey=$recoveryKey, quality=$initialQuality)")

    fun handleWebGlFailure(source: String) {
      if (!instanceCrashHandled.compareAndSet(false, true)) return
      mainHandler.post {
        if (webViewDestroyed.get() || controller.use2DFallback) return@post
        val action = crashTracker.recordCrash(SystemClock.elapsedRealtime())
        Log.e(TAG, "WebGL failure via $source -> action=$action")
        pageReady = false
        when (action) {
          WebGlRecoveryAction.REBUILD_IN_LITE_AND_PERSIST -> {
            onRequireRuntimeLite()
            scope.launch { appConfig.setForceLiteMode(true) }
            recoveryKey += 1
          }
          WebGlRecoveryAction.REBUILD_IN_LITE -> {
            onRequireRuntimeLite()
            recoveryKey += 1
          }
          WebGlRecoveryAction.FALLBACK_TO_2D -> {
            controller.set2DFallback(true)
          }
        }
      }
    }

    val jsBridge = FridgeJsBridge(
      onDoorTappedCallback = { compartmentId ->
        mainHandler.post {
          if (controller.isVisible) {
            controller.onDoorClick?.invoke(compartmentId)
          }
        }
      },
      onStickersProjectedCallback = { json ->
        val parsed = parseProjections(json)
        mainHandler.post {
          val listener = controller.onProjectionsUpdated
          if (!controller.isVisible || !controller.showStickers || listener == null) {
            return@post
          }
          var changed = false
          val removed = projectionCache.keys - parsed.keys
          if (removed.isNotEmpty()) {
            removed.forEach { projectionCache.remove(it) }
            changed = true
          }
          parsed.forEach { (id, next) ->
            val prev = projectionCache[id]
            if (
              prev == null ||
              prev.visible != next.visible ||
              abs(prev.xFrac - next.xFrac) > PROJECTION_EPS ||
              abs(prev.yFrac - next.yFrac) > PROJECTION_EPS
            ) {
              projectionCache[id] = next
              changed = true
            }
          }
          if (changed) {
            listener(projectionCache.toMap())
          }
        }
      },
      onWelcomeSpinFinishedCallback = {
        mainHandler.post {
          if (!webViewDestroyed.get()) {
            runCatching {
              latestHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
          }
        }
      },
      onWebGlContextLostCallback = {
        handleWebGlFailure("webglcontextlost")
      }
    )

    WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(parseSurfaceColorInt(DEFAULT_SURFACE_BG))
      isVerticalScrollBarEnabled = false
      isHorizontalScrollBarEnabled = false
      overScrollMode = View.OVER_SCROLL_NEVER
      isHapticFeedbackEnabled = false

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = false
        allowFileAccess = false
        allowContentAccess = false
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = false
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        cacheMode = WebSettings.LOAD_NO_CACHE
      }

      addJavascriptInterface(jsBridge, "AndroidBridge")

      webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
          if (consoleMessage != null) {
            val text = "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
            when (consoleMessage.messageLevel()) {
              ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG, text)
              ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, text)
              else -> Log.d(TAG, text)
            }
          }
          return true
        }
      }

      webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
          val url = request?.url?.toString()
          val allowed = isAllowedFridge3dUrl(url)
          if (!allowed) {
            Log.w(TAG, "Blocked external navigation: $url")
          }
          return !allowed
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          pageReady = true
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
          handleWebGlFailure("onRenderProcessGone(didCrash=${detail?.didCrash()})")
          return true
        }
      }

      loadUrl("file:///android_asset/fridge3d/index.html?q=$initialQuality")
    }
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, webView, webViewDestroyed) {
    val observer = LifecycleEventObserver { _, event ->
      if (webViewDestroyed.get()) return@LifecycleEventObserver
      when (event) {
        Lifecycle.Event.ON_STOP -> {
          Log.d(TAG, "ON_STOP -> webView.onPause()")
          webView.onPause()
        }
        Lifecycle.Event.ON_START -> {
          Log.d(TAG, "ON_START -> webView.onResume()")
          webView.onResume()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  DisposableEffect(webView, webViewDestroyed) {
    onDispose {
      webViewDestroyed.set(true)
      mainHandler.removeCallbacksAndMessages(null)
      (webView.parent as? ViewGroup)?.removeView(webView)
      webView.removeJavascriptInterface("AndroidBridge")
      webView.stopLoading()
      webView.destroy()
    }
  }

  val isActive = controller.activeToken != null
  val show = controller.isVisible
  val bounds = controller.boundsPx

  LaunchedEffect(pageReady, isActive, configJson) {
    if (pageReady && isActive) {
      safeEvaluateJs(
        webView = webView,
        destroyed = webViewDestroyed,
        script = "window.updateFridge(${JSONObject.quote(configJson)});"
      )
    }
  }

  LaunchedEffect(controller.resetNonce) {
    if (pageReady && show && controller.resetNonce > 0) {
      safeEvaluateJs(webView, webViewDestroyed, "window.resetView();")
    }
  }

  LaunchedEffect(controller.itemAddedPulseNonce) {
    if (pageReady && show && controller.itemAddedPulseNonce > 0) {
      safeEvaluateJs(webView, webViewDestroyed, "window.playItemAddedBounce();")
    }
  }

  LaunchedEffect(pageReady, show, controller.playWelcomeSpin) {
    if (pageReady && show && controller.playWelcomeSpin) {
      delay(250L)
      if (pageReady && controller.isVisible && !webViewDestroyed.get()) {
        safeEvaluateJs(webView, webViewDestroyed, "window.playWelcomeSpin();")
      }
      runCatching { controller.onWelcomeSpinConsumed() }
    }
  }

  LaunchedEffect(pageReady, show, bounds) {
    if (pageReady && show && bounds.isValid) {
      webView.post {
        if (pageReady && controller.isVisible) {
          safeEvaluateJs(webView, webViewDestroyed, "window.dispatchEvent(new Event('resize'));")
        }
      }
    }
  }

  key(recoveryKey) {
    val activeBounds = if (bounds.isValid) bounds else lastValidBounds
    val hasValidBounds = activeBounds.isValid
    val wDp = if (hasValidBounds) with(density) { activeBounds.width.toDp() } else 0.dp
    val hDp = if (hasValidBounds) with(density) { activeBounds.height.toDp() } else 0.dp
    val xOff = if (hasValidBounds) activeBounds.left else 0
    val yOff = if (hasValidBounds) activeBounds.top else 0
    val cornerRadius = controller.cornerRadius
    val cornerPx = with(density) { cornerRadius.toPx() }
    val cornerShape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RectangleShape
    var appliedCornerPx by remember { mutableFloatStateOf(-1f) }
    var appliedSurfaceBg by remember { mutableStateOf("") }
    val surfaceBg = controller.surfaceBg

    AndroidView(
      factory = { webView },
      modifier = modifier
        .offset { IntOffset(xOff, yOff) }
        .size(wDp, hDp)
        .then(if (cornerRadius > 0.dp && show) Modifier.clip(cornerShape) else Modifier),
      update = { view ->
        val targetVisibility = if (show && bounds.isValid) View.VISIBLE else View.GONE
        if (view.visibility != targetVisibility) {
          view.visibility = targetVisibility
        }
        if (appliedSurfaceBg != surfaceBg) {
          appliedSurfaceBg = surfaceBg
          view.setBackgroundColor(parseSurfaceColorInt(surfaceBg))
        }
        if (appliedCornerPx != cornerPx) {
          appliedCornerPx = cornerPx
          if (cornerPx > 0f) {
            view.clipToOutline = true
            view.outlineProvider = object : ViewOutlineProvider() {
              override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, cornerPx)
              }
            }
          } else {
            view.clipToOutline = false
            view.outlineProvider = ViewOutlineProvider.BACKGROUND
          }
        }
        if (show && bounds.isValid) {
          view.requestLayout()
        }
      }
    )
  }
}

/**
 * Lightweight placeholder slot placed in screens (`FridgeDesignerScreen`, `HomeScreen`) to measure
 * root-relative stage bounds and synchronize current screen props with [SharedFridge3DWebView].
 */
@Composable
fun Fridge3DStageSlot(
  colorKey: String,
  compartments: List<CompartmentEntity>,
  modifier: Modifier = Modifier,
  template: String = "top_freezer",
  openCompartmentId: String? = null,
  resetOrbitKey: Int = 0,
  itemAddedPulseKey: Int = 0,
  playWelcomeSpin: Boolean = false,
  onWelcomeSpinConsumed: () -> Unit = {},
  showStickers: Boolean = false,
  compartmentStats: Map<String, FallbackCompartmentStat> = emptyMap(),
  surfaceBg: String = DEFAULT_SURFACE_BG,
  cornerRadius: Dp = 0.dp,
  backdropColor: Color? = null,
  onDoorClick: ((compartmentId: String) -> Unit)? = null,
  onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)? = null
) {
  val controller = LocalFridge3DStageController.current
  val slotToken = remember { Any() }

  val latestOnWelcomeConsumed by rememberUpdatedState(onWelcomeSpinConsumed)
  val latestOnDoorClick by rememberUpdatedState(onDoorClick)
  val latestOnProjectionsUpdated by rememberUpdatedState(onProjectionsUpdated)

  SideEffect {
    controller.updateSlot(
      token = slotToken,
      colorKey = colorKey,
      compartments = compartments,
      template = template,
      openCompartmentId = openCompartmentId,
      playWelcomeSpin = playWelcomeSpin,
      showStickers = showStickers,
      surfaceBg = surfaceBg,
      cornerRadius = cornerRadius,
      backdropColor = backdropColor,
      onWelcomeSpinConsumed = { latestOnWelcomeConsumed() },
      onDoorClick = latestOnDoorClick,
      onProjectionsUpdated = latestOnProjectionsUpdated
    )
  }

  LaunchedEffect(resetOrbitKey) {
    if (resetOrbitKey > 0) {
      controller.requestResetOrbit()
    }
  }

  LaunchedEffect(itemAddedPulseKey) {
    if (itemAddedPulseKey > 0) {
      controller.requestItemAddedBounce()
    }
  }

  DisposableEffect(controller, slotToken) {
    onDispose {
      controller.releaseSlot(slotToken)
    }
  }

  Box(
    modifier = modifier.onGloballyPositioned { coords ->
      if (!coords.isAttached || coords.size.width <= 0 || coords.size.height <= 0) {
        return@onGloballyPositioned
      }
      val root = controller.rootCoordinates
      val topLeft = if (root != null && root.isAttached) {
        root.localPositionOf(coords, Offset.Zero)
      } else {
        coords.positionInRoot()
      }
      controller.updateBounds(
        slotToken,
        StageBoundsPx(
          left = topLeft.x.roundToInt(),
          top = topLeft.y.roundToInt(),
          width = coords.size.width,
          height = coords.size.height
        )
      )
    }
  ) {
    if (controller.use2DFallback) {
      Fridge2DFallback(
        colorKey = colorKey,
        compartments = compartments,
        openCompartmentId = openCompartmentId,
        compartmentStats = compartmentStats,
        showItemCounts = showStickers,
        surfaceBg = backdropColor,
        onDoorClick = latestOnDoorClick,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

private fun safeEvaluateJs(
  webView: WebView,
  destroyed: AtomicBoolean,
  script: String
) {
  if (destroyed.get() || !webView.isAttachedToWindow) return
  webView.evaluateJavascript(script, null)
}

private fun parseSurfaceColorInt(hex: String): Int =
  runCatching { AndroidColor.parseColor(hex) }
    .getOrElse { AndroidColor.parseColor(DotGridSpec.PAPER_HEX) }

private fun buildConfigJson(
  template: String,
  colorKey: String,
  compartments: List<CompartmentEntity>,
  openCompartmentId: String?,
  showStickers: Boolean,
  surfaceBg: String,
  dotGridOffsetXDp: Float = 0f,
  dotGridOffsetYDp: Float = 0f
): String {
  val compsArray = JSONArray()
  compartments.forEach { c ->
    compsArray.put(
      JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("kind", c.kind.name)
        put("x", c.x.toDouble())
        put("y", c.y.toDouble())
        put("w", c.w.toDouble())
        put("h", c.h.toDouble())
        put("shelves", c.shelves)
      }
    )
  }
  val showDotGrid = showStickers || !surfaceBg.equals("#FFFFFF", ignoreCase = true)
  val dotGridObj = JSONObject().apply {
    put("paperHex", DotGridSpec.PAPER_HEX)
    put("dotHex", DotGridSpec.DOT_HEX)
    put("stepDp", DotGridSpec.STEP_DP.toDouble())
    put("dotRadiusDp", DotGridSpec.DOT_RADIUS_DP.toDouble())
    put("offsetXDp", dotGridOffsetXDp.toDouble())
    put("offsetYDp", dotGridOffsetYDp.toDouble())
  }
  return JSONObject().apply {
    put("template", template)
    put("colorKey", colorKey)
    put("openCompartmentId", openCompartmentId ?: JSONObject.NULL)
    put("showStickers", showStickers)
    put("springReturn", showStickers)
    put("surfaceBg", surfaceBg)
    put("showDotGrid", showDotGrid)
    put("dotGrid", dotGridObj)
    put("compartments", compsArray)
  }.toString()
}

private fun parseProjections(json: String): Map<String, DoorProjection> {
  val out = HashMap<String, DoorProjection>()
  runCatching {
    val arr = JSONArray(json)
    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)
      val id = obj.optString("id")
      if (id.isNotEmpty()) {
        out[id] = DoorProjection(
          xFrac = obj.optDouble("x", 0.5).toFloat(),
          yFrac = obj.optDouble("y", 0.5).toFloat(),
          visible = obj.optBoolean("visible", false)
        )
      }
    }
  }
  return out
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/fridge3d/ShellColorSpec.kt` (36 lines)

```kotlin
package com.truong.fridgezero.ui.fridge3d

import androidx.compose.ui.graphics.Color

/** spec.md §1.4 palette with balanced matte-satin studio PBR values to prevent specular glare. */
data class ShellColorSpec(
  val key: String,
  val color: Color,
  val roughness: Float,
  val metalness: Float
)

object ShellColors {
  val Inox = ShellColorSpec("inox", Color(0xFFCBD5E1), 0.42f, 0.25f)
  val Butter = ShellColorSpec("butter", Color(0xFFFEF08A), 0.44f, 0.16f)
  val Mint = ShellColorSpec("mint", Color(0xFFA7F3D0), 0.44f, 0.16f)
  val Matte = ShellColorSpec("matte", Color(0xFF27272A), 0.56f, 0.22f)

  val all = listOf(Inox, Butter, Mint, Matte)

  /** Normalizes legacy color keys (such as "matte_black") to canonical spec keys. */
  fun normalizeKey(key: String): String = when (key.trim().lowercase()) {
    "butter" -> Butter.key
    "mint" -> Mint.key
    "matte", "matte_black" -> Matte.key
    else -> Inox.key
  }

  fun forKey(key: String): ShellColorSpec = when (normalizeKey(key)) {
    Butter.key -> Butter
    Mint.key -> Mint
    Matte.key -> Matte
    else -> Inox
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/home/HomeDashboardHeader.kt` (666 lines)

```kotlin
package com.truong.fridgezero.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodIcon
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.ButterSoft
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UrgentEntry(
  val compartmentId: String,
  val compartmentName: String,
  val item: DisplayItem
)

private val CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM", Locale.US)

private fun DayOfWeek.toViShort(): String = when (this) {
  DayOfWeek.MONDAY -> "T2"
  DayOfWeek.TUESDAY -> "T3"
  DayOfWeek.WEDNESDAY -> "T4"
  DayOfWeek.THURSDAY -> "T5"
  DayOfWeek.FRIDAY -> "T6"
  DayOfWeek.SATURDAY -> "T7"
  DayOfWeek.SUNDAY -> "CN"
}

/**
 * Top Home Dashboard:
 * Row 1: Compact brand+weather caption + dynamic greeting | live HH:mm:ss + day/date pill
 * Row 2: Prominent centered expiring-items assistant banner directly above the 3D fridge
 */
@Composable
fun HomeDashboardHeader(
  compartments: List<CompartmentUi>,
  topRecommendation: RecipeRecommendation?,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
  onOpenCompartment: (compartmentId: String, filterUrgent: Boolean) -> Unit,
  onOpenRecipes: (recipeId: String?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  var now by remember { mutableStateOf(LocalDateTime.now()) }
  LaunchedEffect(Unit) {
    while (true) {
      now = LocalDateTime.now()
      delay(1000L)
    }
  }

  val urgentEntries = remember(compartments) {
    compartments.flatMap { comp ->
      comp.items
        .filter { it.freshness == Freshness.SOON || it.freshness == Freshness.BAD }
        .map { UrgentEntry(comp.id, comp.name, it) }
    }.sortedBy { it.item.minDaysLeft }
  }
  val hasBad = urgentEntries.any { it.item.freshness == Freshness.BAD }
  var showUrgentSheet by remember { mutableStateOf(false) }

  val greetingRes = when (now.hour) {
    in 5..10 -> R.string.home_greeting_morning
    in 11..13 -> R.string.home_greeting_noon
    in 14..17 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: Merged brand + weather caption above Greeting | Live digital clock
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = stringResource(R.string.home_brand_weather),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          letterSpacing = 1.0.sp,
          color = InkBlack.copy(alpha = 0.65f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = stringResource(greetingRes),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      DigitalClockPill(now = now)
    }

    // Row 2: Alert banner + live recipe recommendation strip
    if (urgentEntries.isNotEmpty()) {
      NeoBrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (hasBad) CoralUrgent else LemonWarn,
        cornerRadius = 14.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(role = Role.Button) { showUrgentSheet = true }
              .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.home_urgent_banner_title_fmt, urgentEntries.size),
              modifier = Modifier.weight(1f),
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = InkBlack,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceWhite)
                .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = stringResource(R.string.home_urgent_view_list),
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }

          if (topRecommendation != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(InkBlack)
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(ButterSoft)
                .clickable(role = Role.Button) { onOpenRecipes(topRecommendation.recipe.id) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = stringResource(
                  R.string.home_banner_recipe_fmt,
                  topRecommendation.recipe.title,
                  topRecommendation.recipe.cookMinutes
                ),
                modifier = Modifier.weight(1f),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (topRecommendation.isReadyToCook) LimeFresh else LemonWarn)
                  .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = stringResource(
                    R.string.recipe_match_ratio_fmt,
                    topRecommendation.matchedCount,
                    topRecommendation.totalFridgeIngredients
                  ),
                  fontFamily = MonoFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 10.sp,
                  color = InkBlack,
                  softWrap = false,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    } else {
      val remindersCd = stringResource(R.string.home_reminders_cd)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        NeoBrutalistButton(
          onClick = { onOpenRecipes(topRecommendation?.recipe?.id) },
          modifier = Modifier.weight(1f),
          containerColor = LimeFresh,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = stringResource(R.string.home_all_fresh_title),
              fontFamily = MonoFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.sp,
              color = InkBlack.copy(alpha = 0.85f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (topRecommendation != null) {
              Text(
                text = stringResource(
                  R.string.home_banner_recipe_fmt,
                  topRecommendation.recipe.title,
                  topRecommendation.recipe.cookMinutes
                ),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        NeoBrutalistButton(
          onClick = { showUrgentSheet = true },
          modifier = Modifier
            .size(46.dp)
            .semantics { contentDescription = remindersCd },
          containerColor = if (remindersEnabled) ButterPale else SurfaceWhite,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(text = "🔔", fontSize = 16.sp)
        }
      }
    }
  }

  if (showUrgentSheet) {
    UrgentItemsBottomSheet(
      entries = urgentEntries,
      topRecommendation = topRecommendation,
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
      onMarkStatus = onMarkStatus,
      onOpenRecipe = { recipeId ->
        showUrgentSheet = false
        onOpenRecipes(recipeId)
      },
      onDismiss = { showUrgentSheet = false },
      onOpenCompartment = { compId ->
        showUrgentSheet = false
        onOpenCompartment(compId, true)
      }
    )
  }
}

@Composable
private fun DigitalClockPill(now: LocalDateTime) {
  val shape = RoundedCornerShape(10.dp)
  val timeStr = remember(now.second, now.minute, now.hour) { now.format(CLOCK_FMT) }
  val dateStr = remember(now.dayOfYear, now.year) {
    "${now.dayOfWeek.toViShort()}, ${now.format(DATE_FMT)}"
  }
  Box(modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(3.dp, 3.dp)
        .background(LimeFresh, shape)
        .border(1.5.dp, InkBlack, shape)
    )
    Column(
      modifier = Modifier
        .background(InkBlack, shape)
        .border(2.dp, InkBlack, shape)
        .padding(horizontal = 10.dp, vertical = 5.dp),
      horizontalAlignment = Alignment.End
    ) {
      Text(
        text = timeStr,
        fontFamily = MonoFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = Color.White
      )
      Text(
        text = dateStr,
        fontFamily = MonoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        color = LimeFresh
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrgentItemsBottomSheet(
  entries: List<UrgentEntry>,
  topRecommendation: RecipeRecommendation?,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  onOpenRecipe: (String) -> Unit,
  onDismiss: () -> Unit,
  onOpenCompartment: (String) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PaperCream,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.urgent_sheet_title),
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = InkBlack
          )
          Text(
            text = stringResource(R.string.urgent_sheet_subtitle),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = InkBlack.copy(alpha = 0.7f)
          )
        }
        if (entries.isNotEmpty()) {
          StickerBadge(
            text = "${entries.size}",
            containerColor = CoralUrgent,
            mono = true
          )
        }
      }

      if (entries.isEmpty()) {
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), containerColor = LimeFresh) {
          Text(
            text = stringResource(R.string.urgent_sheet_empty),
            fontFamily = BodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = InkBlack,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        // Highlighted clickable recipe suggestion card linked directly to CulinaryGuideScreen
        if (topRecommendation != null) {
          val recipe = topRecommendation.recipe
          NeoBrutalistCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(role = Role.Button) { onOpenRecipe(recipe.id) },
            containerColor = ButterSoft,
            cornerRadius = 14.dp,
            borderWidth = 2.5.dp,
            shadowOffset = 3.dp
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = stringResource(R.string.urgent_meal_title),
                  fontFamily = DisplayFont,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 13.sp,
                  color = InkBlack
                )
                StickerBadge(
                  text = stringResource(
                    R.string.recipe_match_ratio_fmt,
                    topRecommendation.matchedCount,
                    topRecommendation.totalFridgeIngredients
                  ),
                  containerColor = if (topRecommendation.isReadyToCook) LimeFresh else LemonWarn,
                  tiltDegrees = 0f,
                  mono = true
                )
              }
              Text(
                text = stringResource(
                  R.string.urgent_meal_recipe_fmt,
                  recipe.emoji,
                  recipe.title,
                  recipe.cookMinutes
                ),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = stringResource(R.string.urgent_meal_cta),
                fontFamily = MonoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = InkBlack
              )
            }
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(entries, key = { "${it.compartmentId}_${it.item.primaryId}" }) { entry ->
            UrgentItemRow(
              entry = entry,
              onMarkUsedUp = { onMarkStatus(entry.item.itemIds, ItemStatus.USED_UP) },
              onOpenCompartment = { onOpenCompartment(entry.compartmentId) }
            )
          }
        }
      }

      NeoBrutalistButton(
        onClick = onToggleReminders,
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (remindersEnabled) LimeFresh else SurfaceWhite,
        cornerRadius = 12.dp,
        shadowOffset = 3.dp
      ) {
        Text(
          text = "${stringResource(R.string.reminder_toggle_label)} · ${
            stringResource(if (remindersEnabled) R.string.reminder_toggle_on else R.string.reminder_toggle_off)
          }",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }
    }
  }
}

@Composable
private fun UrgentItemRow(
  entry: UrgentEntry,
  onMarkUsedUp: () -> Unit,
  onOpenCompartment: () -> Unit
) {
  val item = entry.item
  val icon = EmojiIconRepository.iconFor(item.iconKey)
  val emoji = (icon as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val daysText = when {
    item.minDaysLeft < 0 -> stringResource(R.string.urgent_badge_overdue_fmt, -item.minDaysLeft)
    item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, item.minDaysLeft)
  }
  val badgeColor = if (item.freshness == Freshness.BAD) CoralUrgent else LemonWarn

  NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, shadowOffset = 3.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = emoji, fontSize = 24.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = item.name,
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = InkBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StickerBadge(
              text = entry.compartmentName,
              containerColor = SurfaceWhite,
              tiltDegrees = 0f
            )
            StickerBadge(
              text = daysText,
              containerColor = badgeColor,
              tiltDegrees = -1.5f,
              mono = true
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NeoBrutalistButton(
          onClick = onMarkUsedUp,
          modifier = Modifier.weight(1f),
          containerColor = LimeFresh,
          cornerRadius = 10.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = stringResource(R.string.urgent_sheet_mark_used_up),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack,
            maxLines = 1
          )
        }
        NeoBrutalistButton(
          onClick = onOpenCompartment,
          modifier = Modifier.weight(1f),
          containerColor = SurfaceWhite,
          cornerRadius = 10.dp,
          borderWidth = 2.dp,
          shadowOffset = 2.dp,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(
            text = stringResource(R.string.urgent_sheet_open_compartment),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack,
            maxLines = 1
          )
        }
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/home/HomeScreen.kt` (569 lines)

```kotlin
package com.truong.fridgezero.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.CulinaryFilter
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
import com.truong.fridgezero.ui.bento.QuickAddItemSheet
import com.truong.fridgezero.ui.fridge3d.DoorProjection
import com.truong.fridgezero.ui.fridge3d.FallbackCompartmentStat
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageSlot
import com.truong.fridgezero.ui.fridge3d.LocalFridge3DStageController
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DotGridSpec
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LilacAccent
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DOOR_OPEN_NAV_DELAY_MS = 380L

/**
 * Screen 2 — Home Dashboard + Interactive 3D Hero Stage + Door Stickers + FAB + Fixed Task Bar.
 */
@Composable
fun HomeScreen(
  uiState: FridgeUiState,
  remindersEnabled: Boolean,
  justClosedCompartmentId: String? = null,
  onJustClosedConsumed: () -> Unit = {},
  onToggleReminders: () -> Unit,
  onWelcomeSpinConsumed: () -> Unit,
  onOpenCompartment: (compartmentId: String, filterUrgent: Boolean) -> Unit,
  onAddItem: (compartmentId: String, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  onOpenRecipes: (recipeId: String?) -> Unit,
  onOpenAiScan: () -> Unit,
  onOpenDesigner: () -> Unit,
  modifier: Modifier = Modifier
) {
  val fridge = uiState.activeFridge
  val compartments = uiState.compartments
  val allItems = remember(compartments) { compartments.flatMap { it.items } }
  val recommendations = remember(allItems) {
    CulinaryGuideEngine.recommend(allItems, CulinaryFilter.ALL)
  }
  val readyRecipeCount = remember(recommendations) {
    recommendations.count { it.isReadyToCook }
  }
  val topRecommendation = remember(recommendations) {
    recommendations.firstOrNull { it.hasUrgentMatch }
      ?: recommendations.firstOrNull { it.matchedCount > 0 }
      ?: recommendations.firstOrNull()
  }
  val entities = remember(fridge?.id, compartments) {
    val fridgeId = fridge?.id ?: "default_fridge"
    compartments.mapIndexed { idx, c ->
      CompartmentEntity(
        id = c.id,
        fridgeId = fridgeId,
        name = c.name,
        x = c.rect.x,
        y = c.rect.y,
        w = c.rect.w,
        h = c.rect.h,
        kind = c.kind,
        shelves = c.shelves,
        sort = idx
      )
    }
  }

  var resetOrbitKey by remember { mutableIntStateOf(0) }
  var itemAddedPulseKey by remember { mutableIntStateOf(0) }
  var openingId by remember { mutableStateOf<String?>(justClosedCompartmentId) }
  var isNavigatingOut by remember { mutableStateOf(false) }
  var showQuickAddSheet by remember { mutableStateOf(false) }
  var projections by remember { mutableStateOf<Map<String, DoorProjection>>(emptyMap()) }

  // When returning from CompartmentBentoScreen, start with the door open for one frame and close smoothly.
  LaunchedEffect(justClosedCompartmentId) {
    if (justClosedCompartmentId != null && !isNavigatingOut) {
      openingId = justClosedCompartmentId
      delay(40L)
      openingId = null
      onJustClosedConsumed()
    }
  }

  val stageController = LocalFridge3DStageController.current
  val fallbackStats = remember(compartments) {
    compartments.associate { comp ->
      comp.id to FallbackCompartmentStat(
        itemCount = comp.items.size,
        worstFreshness = comp.worstFreshness
      )
    }
  }
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  fun triggerDoorOpen(compartmentId: String) {
    if (isNavigatingOut) return
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    if (stageController.use2DFallback) {
      onOpenCompartment(compartmentId, false)
      return
    }
    isNavigatingOut = true
    openingId = compartmentId
    scope.launch {
      delay(DOOR_OPEN_NAV_DELAY_MS)
      onOpenCompartment(compartmentId, false)
      openingId = null
      isNavigatingOut = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
  ) {
    HomeDashboardHeader(
      compartments = compartments,
      topRecommendation = topRecommendation,
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
      onOpenCompartment = onOpenCompartment,
      onOpenRecipes = onOpenRecipes,
      onMarkStatus = onMarkStatus
    )

    // Center 3D Hero Stage + Floating "+ Thêm nhanh" button
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 6.dp, bottom = 8.dp)
      ) {
        Fridge3DStageSlot(
          colorKey = fridge?.colorKey ?: "inox",
          compartments = entities,
          template = fridge?.template ?: "top_freezer",
          openCompartmentId = openingId,
          resetOrbitKey = resetOrbitKey,
          itemAddedPulseKey = itemAddedPulseKey,
          playWelcomeSpin = uiState.playWelcomeSpin,
          onWelcomeSpinConsumed = onWelcomeSpinConsumed,
          showStickers = true,
          compartmentStats = fallbackStats,
          surfaceBg = DotGridSpec.PAPER_HEX,
          onDoorClick = { compId -> triggerDoorOpen(compId) },
          onProjectionsUpdated = { projections = it },
          modifier = Modifier.fillMaxSize()
        )
        if (!stageController.use2DFallback) {
          DoorStickersOverlay(
            compartments = compartments,
            projections = projections,
            onStickerClick = { compId -> triggerDoorOpen(compId) }
          )
        }
      }

      // Floating "+ Thêm nhanh" CTA anchored above bottom-right of Task Bar
      NeoBrutalistButton(
        onClick = { showQuickAddSheet = true },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 14.dp),
        containerColor = LimeFresh,
        cornerRadius = 14.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 4.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(InkBlack),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "+",
            fontFamily = MonoFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = LimeFresh
          )
        }
        Text(
          text = stringResource(R.string.home_fab_quick_add).removePrefix("+ "),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
      }
    }

    // Fixed Neo-Brutalist 4-Button Bottom Task Bar calibrated for 360dp+ viewports
    HomeTaskBar(
      readyRecipeCount = readyRecipeCount,
      onResetFridgeView = { resetOrbitKey++ },
      onOpenRecipes = { onOpenRecipes(null) },
      onOpenAiScan = onOpenAiScan,
      onOpenDesigner = onOpenDesigner
    )
  }

  if (showQuickAddSheet && compartments.isNotEmpty()) {
    QuickAddItemSheet(
      compartments = compartments,
      initialCompartmentId = null,
      recentKeys = uiState.recentItemKeys,
      onSaveItem = { compId, name, iconKey, qty, daysLeft, unit ->
        onAddItem(compId, name, iconKey, qty, daysLeft, unit)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
          delay(140L)
          itemAddedPulseKey++
        }
      },
      onDismiss = { showQuickAddSheet = false }
    )
  }
}

@Composable
private fun DoorStickersOverlay(
  compartments: List<CompartmentUi>,
  projections: Map<String, DoorProjection>,
  onStickerClick: (String) -> Unit
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val density = LocalDensity.current
    val maxWPx = with(density) { maxWidth.toPx() }
    val maxHPx = with(density) { maxHeight.toPx() }
    val padPx = with(density) { 8.dp.toPx() }

    compartments.forEachIndexed { index, comp ->
      key(comp.id) {
        val proj = projections[comp.id]
        val isVisible = proj?.visible == true
        val stickerAlpha by animateFloatAsState(
          targetValue = if (isVisible) 1f else 0f,
          animationSpec = tween(durationMillis = 140),
          label = "stickerAlpha_${comp.id}"
        )
        var stickerSize by remember { mutableStateOf(IntSize.Zero) }
        if (proj != null && stickerAlpha > 0.02f) {
          val tilt = if (index % 2 == 0) -1.5f else 1.5f
          val rawX = proj.xFrac * maxWPx - stickerSize.width / 2f
          val rawY = proj.yFrac * maxHPx - stickerSize.height / 2f
          val clampedX = rawX.coerceIn(padPx, (maxWPx - stickerSize.width - padPx).coerceAtLeast(padPx))
          val clampedY = rawY.coerceIn(padPx, (maxHPx - stickerSize.height - padPx).coerceAtLeast(padPx))

          DoorStickerBadge(
            compartment = comp,
            tiltDegrees = tilt,
            enabled = isVisible,
            onClick = { onStickerClick(comp.id) },
            modifier = Modifier
              .onSizeChanged { stickerSize = it }
              .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
              .alpha(stickerAlpha)
          )
        }
      }
    }
  }
}

@Composable
private fun DoorStickerBadge(
  compartment: CompartmentUi,
  tiltDegrees: Float,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dotColor = when (compartment.worstFreshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> LimeFresh
  }
  val cd = stringResource(
    R.string.sticker_urgent_cd,
    compartment.name,
    compartment.items.size,
    compartment.urgentCount
  )
  val corner = 10.dp
  val shape = RoundedCornerShape(corner)

  Box(
    modifier = modifier
      .rotate(tiltDegrees)
      .padding(end = 2.dp, bottom = 2.dp)
      .semantics { contentDescription = cd }
  ) {
    Row(
      modifier = Modifier
        .drawBehind {
          val d = 2.dp.toPx()
          val r = corner.toPx()
          drawRoundRect(
            color = InkBlack,
            topLeft = Offset(d, d),
            size = size,
            cornerRadius = CornerRadius(r, r)
          )
        }
        .background(SurfaceWhite, shape)
        .border(2.dp, InkBlack, shape)
        .clip(shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(dotColor)
          .border(1.5.dp, InkBlack, CircleShape)
      )
      Text(
        text = compartment.name,
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = InkBlack,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Box(
        modifier = Modifier
          .defaultMinSize(minWidth = 22.dp)
          .clip(RoundedCornerShape(7.dp))
          .background(InkBlack)
          .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "${compartment.items.size}",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          lineHeight = 14.sp,
          color = LimeFresh,
          softWrap = false,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun HomeTaskBar(
  readyRecipeCount: Int,
  onResetFridgeView: () -> Unit,
  onOpenRecipes: () -> Unit,
  onOpenAiScan: () -> Unit,
  onOpenDesigner: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(SurfaceWhite)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(2.5.dp)
        .background(InkBlack)
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(74.dp)
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      CompactTaskTabButton(
        emoji = "🧊",
        label = stringResource(R.string.home_tab_fridge_short),
        containerColor = ButterPale,
        onClick = onResetFridgeView,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
      CompactTaskTabButton(
        emoji = "🍳",
        label = stringResource(R.string.home_tab_recipes_short),
        containerColor = SurfaceWhite,
        badgeText = if (readyRecipeCount > 0) "$readyRecipeCount" else null,
        badgeColor = LimeFresh,
        contentDescriptionText = stringResource(R.string.home_tab_recipes_cd),
        onClick = onOpenRecipes,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
      CompactTaskTabButton(
        emoji = "📷",
        label = stringResource(R.string.home_tab_scan_ai_short),
        containerColor = LilacAccent,
        badgeText = stringResource(R.string.home_tab_scan_badge),
        badgeColor = LemonWarn,
        onClick = onOpenAiScan,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
      CompactTaskTabButton(
        emoji = "⚙️",
        label = stringResource(R.string.home_tab_redesign_short),
        containerColor = SurfaceWhite,
        onClick = onOpenDesigner,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
    }
    Spacer(Modifier.navigationBarsPadding())
  }
}

@Composable
private fun CompactTaskTabButton(
  emoji: String,
  label: String,
  containerColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeText: String? = null,
  badgeColor: Color = LimeFresh,
  contentDescriptionText: String? = null
) {
  val buttonModifier = if (contentDescriptionText != null) {
    Modifier.fillMaxSize().semantics { contentDescription = contentDescriptionText }
  } else {
    Modifier.fillMaxSize()
  }
  Box(modifier = modifier) {
    NeoBrutalistButton(
      onClick = onClick,
      modifier = buttonModifier,
      containerColor = containerColor,
      cornerRadius = 11.dp,
      borderWidth = 2.dp,
      shadowOffset = 2.5.dp,
      contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
      ) {
        Text(text = emoji, fontSize = 15.sp, lineHeight = 16.sp)
        Text(
          text = label,
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.5.sp,
          lineHeight = 12.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (badgeText != null) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = (-1).dp, y = (-5).dp)
          .rotate(3f)
          .clip(RoundedCornerShape(6.dp))
          .background(badgeColor)
          .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
          .padding(horizontal = 5.dp, vertical = 1.dp)
      ) {
        Text(
          text = badgeText,
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.sp,
          lineHeight = 10.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1
        )
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/icons/FoodIcon.kt` (92 lines)

```kotlin
package com.truong.fridgezero.ui.icons

import androidx.annotation.DrawableRes

sealed interface FoodIcon {
  data class Emoji(val glyph: String) : FoodIcon
  data class Vector(@param:DrawableRes val res: Int) : FoodIcon
}

enum class FoodGroup(val labelVi: String, val fallbackEmoji: String) {
  RAU("Rau", "🥬"),
  THIT("Thịt", "🥩"),
  CA_HAI_SAN("Cá & Hải sản", "🐟"),
  SUA_TRUNG("Sữa & Trứng", "🥛"),
  TRAI_CAY("Trái cây", "🍎"),
  DO_KHO("Đồ khô", "🌾"),
  DO_UONG("Đồ uống", "🧃"),
  AN_SAN("Ăn sẵn", "🥟"),
  KHAC("Khác", "🍽️")
}

data class IconEntry(
  val key: String,
  val label: String,
  val group: FoodGroup,
  val glyph: String
)

interface IconRepository {
  fun iconFor(key: String): FoodIcon
  fun groupFor(key: String): FoodGroup
  fun search(query: String): List<IconEntry>
}

object EmojiIconRepository : IconRepository {

  val entries: List<IconEntry> = listOf(
    IconEntry("shrimp", "Tôm", FoodGroup.CA_HAI_SAN, "🦐"),
    IconEntry("salmon", "Cá hồi", FoodGroup.CA_HAI_SAN, "🐟"),
    IconEntry("crab", "Cua", FoodGroup.CA_HAI_SAN, "🦀"),
    IconEntry("squid", "Mực", FoodGroup.CA_HAI_SAN, "🦑"),
    IconEntry("chicken", "Thịt gà", FoodGroup.THIT, "🍗"),
    IconEntry("meat", "Thịt heo / bò", FoodGroup.THIT, "🥩"),
    IconEntry("bacon", "Thịt ba chỉ", FoodGroup.THIT, "🥓"),
    IconEntry("milk", "Sữa", FoodGroup.SUA_TRUNG, "🥛"),
    IconEntry("egg", "Trứng", FoodGroup.SUA_TRUNG, "🥚"),
    IconEntry("cheese", "Phô mai", FoodGroup.SUA_TRUNG, "🧀"),
    IconEntry("yogurt", "Sữa chua", FoodGroup.SUA_TRUNG, "🥣"),
    IconEntry("butter", "Bơ", FoodGroup.SUA_TRUNG, "🧈"),
    IconEntry("orange_juice", "Nước cam", FoodGroup.DO_UONG, "🧃"),
    IconEntry("water", "Nước suối", FoodGroup.DO_UONG, "🥤"),
    IconEntry("ice_cream", "Kem", FoodGroup.AN_SAN, "🍦"),
    IconEntry("dumpling", "Bánh bao", FoodGroup.AN_SAN, "🥟"),
    IconEntry("tomato_sauce", "Sốt cà", FoodGroup.DO_KHO, "🍅"),
    IconEntry("pickle", "Dưa muối", FoodGroup.RAU, "🥒"),
    IconEntry("carrot", "Cà rốt", FoodGroup.RAU, "🥕"),
    IconEntry("lettuce", "Xà lách", FoodGroup.RAU, "🥬"),
    IconEntry("bell_pepper", "Ớt chuông", FoodGroup.RAU, "🫑"),
    IconEntry("broccoli", "Bông cải", FoodGroup.RAU, "🥦"),
    IconEntry("corn", "Bắp ngọt", FoodGroup.RAU, "🌽"),
    IconEntry("mushroom", "Nấm", FoodGroup.RAU, "🍄"),
    IconEntry("apple", "Táo", FoodGroup.TRAI_CAY, "🍎"),
    IconEntry("banana", "Chuối", FoodGroup.TRAI_CAY, "🍌"),
    IconEntry("grape", "Nho", FoodGroup.TRAI_CAY, "🍇"),
    IconEntry("strawberry", "Dâu tây", FoodGroup.TRAI_CAY, "🍓"),
    IconEntry("watermelon", "Dưa hấu", FoodGroup.TRAI_CAY, "🍉"),
    IconEntry("mango", "Xoài", FoodGroup.TRAI_CAY, "🥭"),
    IconEntry("bread", "Bánh mì", FoodGroup.DO_KHO, "🍞"),
    IconEntry("rice", "Cơm / Gạo", FoodGroup.DO_KHO, "🍚")
  )

  private val byKey = entries.associateBy { it.key }

  override fun iconFor(key: String): FoodIcon {
    val entry = byKey[key]
    return FoodIcon.Emoji(entry?.glyph ?: groupFor(key).fallbackEmoji)
  }

  override fun groupFor(key: String): FoodGroup =
    byKey[key]?.group ?: FoodGroup.KHAC

  override fun search(query: String): List<IconEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return entries
    return entries.filter {
      it.label.lowercase().contains(q) ||
        it.key.lowercase().contains(q) ||
        it.group.labelVi.lowercase().contains(q)
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/recipes/CulinaryGuideScreen.kt` (508 lines)

```kotlin
package com.truong.fridgezero.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.CulinaryFilter
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.RecipeCategory
import com.truong.fridgezero.domain.RecipeIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodIcon
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterSoft
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite
import com.truong.fridgezero.ui.theme.dotGridBackground

/**
 * Screen 4 — Neo-Brutalist Culinary Guide with top rescue hero card, category chips,
 * and 2-column Bento recipe grid.
 */
@Composable
fun CulinaryGuideScreen(
  compartments: List<CompartmentUi>,
  initialRecipeId: String? = null,
  onBack: () -> Unit,
  onConsumeRecipe: (RecipeRecommendation) -> Unit,
  modifier: Modifier = Modifier
) {
  val allItems = remember(compartments) { compartments.flatMap { it.items } }
  val allRecommendations = remember(allItems) {
    CulinaryGuideEngine.recommend(allItems, CulinaryFilter.ALL)
  }
  val readyCount = remember(allRecommendations) {
    allRecommendations.count { it.isReadyToCook }
  }
  val heroRecommendation = remember(allRecommendations) {
    allRecommendations.firstOrNull { it.hasUrgentMatch }
      ?: allRecommendations.firstOrNull { it.matchedCount > 0 }
      ?: allRecommendations.firstOrNull()
  }

  var selectedFilter by remember { mutableStateOf(RecipeCategory.ALL) }
  var selectedRecipeId by remember(initialRecipeId) { mutableStateOf(initialRecipeId) }

  val filteredRecommendations = remember(allItems, selectedFilter) {
    CulinaryGuideEngine.recommend(allItems, selectedFilter)
  }
  val activeDetail = remember(allRecommendations, selectedRecipeId) {
    selectedRecipeId?.let { id -> allRecommendations.firstOrNull { it.recipe.id == id } }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .dotGridBackground()
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Top Bar: square ← button | "CẨM NANG ẨM THỰC" | Ready count badge
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      val backCd = stringResource(R.string.recipe_back_cd)
      NeoBrutalistButton(
        onClick = onBack,
        modifier = Modifier
          .size(44.dp)
          .semantics { contentDescription = backCd },
        containerColor = SurfaceWhite,
        cornerRadius = 12.dp,
        borderWidth = 2.5.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(0.dp)
      ) {
        Text(
          text = stringResource(R.string.bento_back_symbol),
          fontFamily = DisplayFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 20.sp,
          color = InkBlack
        )
      }

      Text(
        text = stringResource(R.string.recipe_screen_title),
        modifier = Modifier.weight(1f),
        fontFamily = DisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      StickerBadge(
        text = stringResource(R.string.recipe_ready_badge_fmt, readyCount),
        containerColor = if (readyCount > 0) LimeFresh else LemonWarn,
        mono = true
      )
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (heroRecommendation != null) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "hero_rescue_card") {
          HeroRescueRecipeCard(
            recommendation = heroRecommendation,
            onClick = { selectedRecipeId = heroRecommendation.recipe.id }
          )
        }
      }

      item(span = { GridItemSpan(maxLineSpan) }, key = "category_chips") {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp)
        ) {
          items(RecipeCategory.entries, key = { it.name }) { category ->
            val selected = selectedFilter == category
            NeoBrutalistButton(
              onClick = { selectedFilter = category },
              containerColor = if (selected) LimeFresh else SurfaceWhite,
              cornerRadius = 10.dp,
              borderWidth = 2.dp,
              shadowOffset = if (selected) 3.dp else 2.dp,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            ) {
              Text(
                text = categoryLabel(category),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = InkBlack,
                softWrap = false,
                maxLines = 1
              )
            }
          }
        }
      }

      if (filteredRecommendations.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter") {
          NeoBrutalistCard(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = stringResource(R.string.recipe_empty_filter),
              modifier = Modifier.padding(20.dp),
              fontFamily = BodyFont,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = InkBlack.copy(alpha = 0.78f)
            )
          }
        }
      } else {
        items(filteredRecommendations, key = { it.recipe.id }) { rec ->
          RecipeBentoCard(
            recommendation = rec,
            onClick = { selectedRecipeId = rec.recipe.id }
          )
        }
      }
    }
  }

  if (activeDetail != null) {
    RecipeDetailSheet(
      recommendation = activeDetail,
      onCooked = { onConsumeRecipe(activeDetail) },
      onDismiss = { selectedRecipeId = null }
    )
  }
}

@Composable
private fun HeroRescueRecipeCard(
  recommendation: RecipeRecommendation,
  onClick: () -> Unit
) {
  val recipe = recommendation.recipe
  val hasUrgent = recommendation.hasUrgentMatch
  val cardColor = if (hasUrgent) CoralUrgent else ButterSoft
  val ratioColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn

  NeoBrutalistCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick),
    containerColor = cardColor,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        StickerBadge(
          text = stringResource(
            if (hasUrgent) R.string.recipe_hero_badge else R.string.recipe_hero_all_fresh_badge
          ),
          containerColor = SurfaceWhite,
          tiltDegrees = -1.2f
        )
        StickerBadge(
          text = stringResource(
            R.string.recipe_match_ratio_fmt,
            recommendation.matchedCount,
            recommendation.totalFridgeIngredients
          ),
          containerColor = ratioColor,
          tiltDegrees = 1.5f,
          mono = true
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = recipe.emoji, fontSize = 28.sp)
        }
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Text(
            text = recipe.title,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = InkBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = recipe.summary,
            fontFamily = BodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = InkBlack.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (hasUrgent) {
          StickerBadge(
            text = stringResource(R.string.recipe_hero_rescue_fmt, recommendation.urgentMatchedCount),
            containerColor = LemonWarn,
            tiltDegrees = 0f
          )
        }
        Text(
          text = "${stringResource(R.string.recipe_prep_time_short_fmt, recipe.cookMinutes)} · ${recipe.sourceName}",
          modifier = Modifier.weight(1f),
          fontFamily = MonoFont,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = InkBlack,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InkBlack)
            .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_hero_cta),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = LimeFresh,
            softWrap = false,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun RecipeBentoCard(
  recommendation: RecipeRecommendation,
  onClick: () -> Unit
) {
  val recipe = recommendation.recipe
  val matchedByIng = remember(recommendation) {
    recommendation.matchedIngredients.associateBy { it.ingredient.name }
  }
  val ratioColor = if (recommendation.isReadyToCook) LimeFresh else LemonWarn

  NeoBrutalistCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick),
    containerColor = SurfaceWhite,
    cornerRadius = 16.dp,
    borderWidth = 2.5.dp,
    shadowOffset = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PaperCream)
            .border(2.dp, InkBlack, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(text = recipe.emoji, fontSize = 22.sp)
        }
        StickerBadge(
          text = stringResource(
            R.string.recipe_match_ratio_fmt,
            recommendation.matchedCount,
            recommendation.totalFridgeIngredients
          ),
          containerColor = ratioColor,
          tiltDegrees = -2f,
          mono = true
        )
      }

      Text(
        text = recipe.title,
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = InkBlack,
        minLines = 2,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        recipe.fridgeIngredients.take(4).forEach { ingredient ->
          val match = matchedByIng[ingredient.name]
          val bg = when {
            match == null -> PaperCream
            match.item.freshness == Freshness.BAD -> CoralUrgent
            match.item.freshness == Freshness.SOON -> LemonWarn
            else -> LimeFresh
          }
          Box(
            modifier = Modifier
              .size(26.dp)
              .alpha(if (match != null) 1f else 0.45f)
              .clip(RoundedCornerShape(7.dp))
              .background(bg)
              .border(1.5.dp, InkBlack, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(text = ingredientGlyph(ingredient), fontSize = 14.sp)
          }
        }
      }

      Spacer(Modifier.height(1.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = stringResource(R.string.recipe_prep_time_short_fmt, recipe.cookMinutes),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1
        )
        Text(
          text = "· ${recipe.sourceName}",
          modifier = Modifier.weight(1f),
          fontFamily = BodyFont,
          fontWeight = FontWeight.SemiBold,
          fontSize = 10.sp,
          color = InkBlack.copy(alpha = 0.68f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun categoryLabel(category: RecipeCategory): String = when (category) {
  CulinaryFilter.ALL -> stringResource(R.string.recipe_filter_all)
  CulinaryFilter.READY_NOW -> stringResource(R.string.recipe_filter_ready)
  CulinaryFilter.RESCUE_URGENT -> stringResource(R.string.recipe_filter_rescue)
  CulinaryFilter.QUICK_15 -> stringResource(R.string.recipe_filter_quick)
  CulinaryFilter.VIET_HOME -> stringResource(R.string.recipe_filter_viet)
  CulinaryFilter.EAT_CLEAN -> stringResource(R.string.recipe_filter_eat_clean)
}

internal fun ingredientGlyph(ingredient: RecipeIngredient): String {
  val key = ingredient.iconKeys.firstOrNull()
  val icon = key?.let { EmojiIconRepository.iconFor(it) }
  return (icon as? FoodIcon.Emoji)?.glyph ?: ingredient.fallbackGroup.fallbackEmoji
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/recipes/RecipeDetailSheet.kt` (493 lines)

```kotlin
package com.truong.fridgezero.ui.recipes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.MatchTier
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.RecipeDifficulty
import com.truong.fridgezero.domain.RecipeIngredient
import com.truong.fridgezero.domain.RecipeRecommendation
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.CoralUrgent
import com.truong.fridgezero.ui.theme.DisplayFont
import com.truong.fridgezero.ui.theme.InkBlack
import com.truong.fridgezero.ui.theme.LemonWarn
import com.truong.fridgezero.ui.theme.LimeFresh
import com.truong.fridgezero.ui.theme.MonoFont
import com.truong.fridgezero.ui.theme.NeoBrutalistButton
import com.truong.fridgezero.ui.theme.NeoBrutalistCard
import com.truong.fridgezero.ui.theme.PaperCream
import com.truong.fridgezero.ui.theme.SkyPale
import com.truong.fridgezero.ui.theme.StickerBadge
import com.truong.fridgezero.ui.theme.SurfaceWhite

/**
 * Detailed recipe sheet separating in-fridge items, missing items, pantry staples,
 * cooking steps, original culinary citation link, and 1-tap inventory deduction.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailSheet(
  recommendation: RecipeRecommendation,
  onCooked: () -> Unit,
  onDismiss: () -> Unit
) {
  val recipe = recommendation.recipe
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current
  val openErrorText = stringResource(R.string.recipe_open_source_error)
  val cookedToastText = if (recommendation.matchedCount > 0) {
    stringResource(R.string.recipe_cooked_toast_fmt, recommendation.matchedCount)
  } else {
    stringResource(R.string.recipe_cooked_toast_empty)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PaperCream,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Scrollable body
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header: Emoji + Title + Summary + Meta badges
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(SurfaceWhite)
              .border(2.5.dp, InkBlack, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(text = recipe.emoji, fontSize = 30.sp)
          }
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = recipe.title,
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 19.sp,
              lineHeight = 23.sp,
              color = InkBlack
            )
            Text(
              text = recipe.summary,
              fontFamily = BodyFont,
              fontWeight = FontWeight.Medium,
              fontSize = 12.sp,
              lineHeight = 16.sp,
              color = InkBlack.copy(alpha = 0.78f)
            )
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          StickerBadge(
            text = stringResource(R.string.recipe_prep_time_long_fmt, recipe.cookMinutes),
            containerColor = SurfaceWhite,
            tiltDegrees = 0f,
            mono = true
          )
          StickerBadge(
            text = stringResource(
              if (recipe.difficulty == RecipeDifficulty.EASY) {
                R.string.recipe_difficulty_easy
              } else {
                R.string.recipe_difficulty_medium
              }
            ),
            containerColor = if (recipe.difficulty == RecipeDifficulty.EASY) LimeFresh else LemonWarn,
            tiltDegrees = 0f
          )
          StickerBadge(
            text = recipe.sourceName,
            containerColor = SkyPale,
            tiltDegrees = 0f
          )
        }

        // Section 1: In Fridge
        RecipeSectionHeader(stringResource(R.string.recipe_section_in_fridge))
        if (recommendation.matchedIngredients.isEmpty()) {
          NeoBrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = SurfaceWhite,
            cornerRadius = 12.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp
          ) {
            Text(
              text = stringResource(R.string.recipe_section_in_fridge_empty),
              modifier = Modifier.padding(12.dp),
              fontFamily = BodyFont,
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp,
              color = InkBlack.copy(alpha = 0.72f)
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recommendation.matchedIngredients.forEach { matched ->
              MatchedIngredientRow(matched = matched)
            }
          }
        }

        // Section 2: Missing Fridge Ingredients
        RecipeSectionHeader(stringResource(R.string.recipe_section_missing))
        if (recommendation.missingIngredients.isEmpty()) {
          NeoBrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = LimeFresh,
            cornerRadius = 12.dp,
            borderWidth = 2.dp,
            shadowOffset = 2.dp
          ) {
            Text(
              text = stringResource(R.string.recipe_section_missing_none),
              modifier = Modifier.padding(12.dp),
              fontFamily = BodyFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              color = InkBlack
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recommendation.missingIngredients.forEach { missing ->
              MissingIngredientRow(ingredient = missing)
            }
          }
        }

        // Section 3: Pantry Staples
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          RecipeSectionHeader(stringResource(R.string.recipe_section_pantry))
          Text(
            text = stringResource(R.string.recipe_section_pantry_hint),
            fontFamily = MonoFont,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = InkBlack.copy(alpha = 0.65f)
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            recipe.pantryStaples.forEach { staple ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(SurfaceWhite)
                  .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp))
                  .padding(horizontal = 9.dp, vertical = 4.dp)
              ) {
                Text(
                  text = staple,
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = InkBlack
                )
              }
            }
          }
        }

        // Section 4: Cooking Steps inside NeoBrutalistCard
        RecipeSectionHeader(stringResource(R.string.recipe_section_steps))
        NeoBrutalistCard(
          modifier = Modifier.fillMaxWidth(),
          containerColor = SurfaceWhite,
          cornerRadius = 16.dp,
          borderWidth = 2.5.dp,
          shadowOffset = 3.dp
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            recipe.steps.forEachIndexed { index, stepText ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(InkBlack),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${index + 1}",
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = LimeFresh
                  )
                }
                Text(
                  text = stepText,
                  modifier = Modifier.weight(1f),
                  fontFamily = BodyFont,
                  fontWeight = FontWeight.Medium,
                  fontSize = 13.sp,
                  lineHeight = 18.sp,
                  color = InkBlack
                )
              }
            }
          }
        }
      }

      // Bottom Action Row: Source Link | 1-Tap Cook Deduction
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        NeoBrutalistButton(
          onClick = {
            runCatching { uriHandler.openUri(recipe.sourceUrl) }
              .onFailure {
                Toast.makeText(context, openErrorText, Toast.LENGTH_SHORT).show()
              }
          },
          modifier = Modifier.weight(1f),
          containerColor = SurfaceWhite,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_action_source),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        NeoBrutalistButton(
          onClick = {
            onCooked()
            Toast.makeText(context, cookedToastText, Toast.LENGTH_SHORT).show()
            onDismiss()
          },
          modifier = Modifier.weight(1.15f),
          containerColor = LimeFresh,
          cornerRadius = 12.dp,
          borderWidth = 2.5.dp,
          shadowOffset = 4.dp,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
        ) {
          Text(
            text = stringResource(R.string.recipe_action_cooked),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun RecipeSectionHeader(text: String) {
  Text(
    text = text,
    fontFamily = DisplayFont,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 14.sp,
    color = InkBlack
  )
}

@Composable
private fun MatchedIngredientRow(matched: MatchedIngredient) {
  val freshness = matched.item.freshness
  val rowColor: Color = when (freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.SOON -> LemonWarn
    Freshness.OK -> SurfaceWhite
  }
  val expiryBadgeColor = freshness.color()
  val expiryLabel = when {
    matched.item.minDaysLeft < 0 ->
      stringResource(R.string.urgent_badge_overdue_fmt, -matched.item.minDaysLeft)
    matched.item.minDaysLeft == 0 -> stringResource(R.string.urgent_badge_today)
    else -> stringResource(R.string.urgent_badge_days_fmt, matched.item.minDaysLeft)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(rowColor)
      .border(2.dp, InkBlack, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(PaperCream)
        .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = ingredientGlyph(matched.ingredient), fontSize = 18.sp)
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = "${matched.ingredient.name} · ${matched.ingredient.amountText}",
        fontFamily = BodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        color = InkBlack,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      val subtitle = if (matched.tier == MatchTier.GROUP_FALLBACK) {
        stringResource(R.string.recipe_substitute_fmt, matched.item.name)
      } else {
        "${matched.item.name} (${matched.item.quantity})"
      }
      Text(
        text = subtitle,
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        color = InkBlack.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    StickerBadge(
      text = expiryLabel,
      containerColor = expiryBadgeColor,
      tiltDegrees = 0f,
      mono = true
    )
  }
}

@Composable
private fun MissingIngredientRow(ingredient: RecipeIngredient) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(SurfaceWhite)
      .border(2.dp, InkBlack, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(PaperCream)
        .border(1.5.dp, InkBlack, RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center
    ) {
      Text(text = ingredientGlyph(ingredient), fontSize = 18.sp)
    }
    Text(
      text = ingredient.name,
      modifier = Modifier.weight(1f),
      fontFamily = BodyFont,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      color = InkBlack,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = ingredient.amountText,
      fontFamily = MonoFont,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp,
      color = InkBlack.copy(alpha = 0.75f),
      softWrap = false,
      maxLines = 1
    )
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/theme/BrutalistComponents.kt` (240 lines)

```kotlin
package com.truong.fridgezero.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrutalBorder = 2.5.dp
val BrutalShadow = 4.dp
val PressShift = 2.dp

/** Warm paper with a cached tiled dot grid (spec §1.1 & DotGridSpec). */
fun Modifier.dotGridBackground(
  paper: Color = DotGridSpec.paperColor,
  dot: Color = DotGridSpec.dotColor,
  step: Dp = DotGridSpec.step,
  dotRadius: Dp = DotGridSpec.dotRadius
): Modifier = this.drawWithCache {
  if (size.width <= 0f || size.height <= 0f) {
    return@drawWithCache onDrawBehind {}
  }
  val tilePx = step.toPx().toInt().coerceIn(2, 256)
  if (tilePx <= 0) {
    return@drawWithCache onDrawBehind {}
  }
  val tileBitmap = ImageBitmap(tilePx, tilePx)
  val tileCanvas = Canvas(tileBitmap)
  val bgPaint = Paint().apply { color = paper }
  tileCanvas.drawRect(0f, 0f, tilePx.toFloat(), tilePx.toFloat(), bgPaint)
  val dotPaint = Paint().apply {
    color = dot
    isAntiAlias = true
  }
  val center = tilePx / 2f
  val radiusPx = dotRadius.toPx().coerceAtLeast(0.5f)
  tileCanvas.drawCircle(Offset(center, center), radiusPx, dotPaint)
  val tiledBrush = ShaderBrush(ImageShader(tileBitmap, TileMode.Repeated, TileMode.Repeated))
  onDrawBehind {
    drawRect(tiledBrush)
  }
}

private fun Modifier.hardOffsetShadow(
  color: Color,
  cornerRadius: Dp,
  offset: Dp
): Modifier = this.drawBehind {
  val d = offset.toPx()
  val r = cornerRadius.toPx()
  drawRoundRect(
    color = color,
    topLeft = Offset(d, d),
    size = size,
    cornerRadius = CornerRadius(r, r)
  )
}

/** White card, 2.5.dp ink border, hard (4.dp, 4.dp) zero-blur shadow drawn strictly behind the face. */
@Composable
fun NeoBrutalistCard(
  modifier: Modifier = Modifier,
  containerColor: Color = SurfaceWhite,
  cornerRadius: Dp = 16.dp,
  borderWidth: Dp = BrutalBorder,
  shadowOffset: Dp = BrutalShadow,
  transparentInterior: Boolean = false,
  content: @Composable () -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
    propagateMinConstraints = true
  ) {
    val faceModifier = if (transparentInterior) {
      Modifier
        .drawWithCache {
          if (size.width <= 0f || size.height <= 0f) {
            return@drawWithCache onDrawBehind {}
          }
          val d = shadowOffset.toPx()
          val r = cornerRadius.toPx()
          val cr = CornerRadius(r, r)
          val inner = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, cr))
          }
          val shadow = Path().apply {
            addRoundRect(RoundRect(d, d, size.width + d, size.height + d, cr))
          }
          val outerShadow = Path.combine(PathOperation.Difference, shadow, inner)
          onDrawBehind {
            drawPath(outerShadow, color = InkBlack)
          }
        }
        .border(borderWidth, InkBlack, shape)
        .clip(shape)
    } else {
      Modifier
        .hardOffsetShadow(InkBlack, cornerRadius, shadowOffset)
        .background(containerColor, shape)
        .border(borderWidth, InkBlack, shape)
        .clip(shape)
    }
    Box(modifier = faceModifier) {
      content()
    }
  }
}

/** Tactile button: hard shadow drawn strictly behind face; on press face shifts (+2.dp, +2.dp). */
@Composable
fun NeoBrutalistButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = SurfaceWhite,
  enabled: Boolean = true,
  cornerRadius: Dp = 12.dp,
  borderWidth: Dp = BrutalBorder,
  shadowOffset: Dp = BrutalShadow,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val haptic = LocalHapticFeedback.current
  LaunchedEffect(pressed) {
    if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
  }
  val shown = if (pressed) minOf(PressShift, shadowOffset) else shadowOffset
  val shift = shadowOffset - shown
  val faceColor = if (enabled) containerColor else PaperCream
  val shadowColor = if (enabled) InkBlack else DotGridInk

  Box(
    modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
    propagateMinConstraints = true
  ) {
    Box(
      modifier = Modifier
        .offset(shift, shift)
        .hardOffsetShadow(shadowColor, cornerRadius, shown)
        .background(faceColor, shape)
        .border(BorderStroke(borderWidth, InkBlack), shape)
        .clip(shape)
        .clickable(
          interactionSource = interaction,
          indication = null,
          enabled = enabled,
          role = Role.Button,
          onClick = onClick
        )
        .padding(contentPadding),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
      )
    }
  }
}

/** Tilted sticker label: -2°..+2°, 2.dp ink border, hard (2.dp, 2.dp) shadow behind the face. */
@Composable
fun StickerBadge(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = LemonWarn,
  contentColor: Color = InkBlack,
  tiltDegrees: Float = -2f,
  mono: Boolean = false
) {
  val corner = 8.dp
  val shape = RoundedCornerShape(corner)
  Box(
    modifier = modifier
      .rotate(tiltDegrees)
      .padding(end = 2.dp, bottom = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .hardOffsetShadow(InkBlack, corner, 2.dp)
        .background(containerColor, shape)
        .border(2.dp, InkBlack, shape)
        .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = if (mono) MonoFont else BodyFont,
        softWrap = false,
        maxLines = 1
      )
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/theme/Color.kt` (35 lines)

```kotlin
package com.truong.fridgezero.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// spec.md §1.1 — Neo-Brutalist palette
val PaperCream = Color(0xFFEAE2D3)
val DotGridInk = Color(0xFFC4B8A3)
val SurfaceWhite = Color(0xFFFFFFFF)
val InkBlack = Color(0xFF111111)
val LimeFresh = Color(0xFFA3E635)
val LemonWarn = Color(0xFFFACC15)
val CoralUrgent = Color(0xFFFB7185)
val LilacAccent = Color(0xFFC084FC)
val SkyInfo = Color(0xFF38BDF8)
val CavitySlate = Color(0xFF1E293B)

val ButterSoft = Color(0xFFFEF08A)
val ButterPale = Color(0xFFFEF9C3)
val SkyPale = Color(0xFFE0F2FE)

/** Single source of truth for the Neo-Brutalist dot-grid background shared by Compose and Three.js. */
object DotGridSpec {
  const val PAPER_HEX: String = "#EAE2D3"
  const val DOT_HEX: String = "#C4B8A3"
  const val STEP_DP: Float = 18f
  const val DOT_RADIUS_DP: Float = 1.5f

  val paperColor: Color = PaperCream
  val dotColor: Color = DotGridInk
  val step: Dp = STEP_DP.dp
  val dotRadius: Dp = DOT_RADIUS_DP.dp
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/theme/Theme.kt` (32 lines)

```kotlin
package com.truong.fridgezero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NeoBrutalistColors = lightColorScheme(
  primary = LimeFresh,
  onPrimary = InkBlack,
  secondary = LemonWarn,
  onSecondary = InkBlack,
  tertiary = LilacAccent,
  error = CoralUrgent,
  onError = InkBlack,
  background = PaperCream,
  onBackground = InkBlack,
  surface = SurfaceWhite,
  onSurface = InkBlack,
  surfaceVariant = PaperCream,
  onSurfaceVariant = InkBlack,
  outline = InkBlack
)

@Composable
fun FridgeZeroTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = NeoBrutalistColors,
    typography = NeoBrutalistTypography,
    content = content
  )
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/theme/Type.kt` (74 lines)

```kotlin
package com.truong.fridgezero.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R

// spec.md §1.2 — Space Grotesk / Plus Jakarta Sans / JetBrains Mono with full Vietnamese support.
val SpaceGroteskFont: FontFamily = FontFamily(
  Font(R.font.space_grotesk_bold, FontWeight.SemiBold),
  Font(R.font.space_grotesk_bold, FontWeight.Bold),
  Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val PlusJakartaSansFont: FontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans_medium, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
  Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val JetBrainsMonoFont: FontFamily = FontFamily(
  Font(R.font.jetbrains_mono_medium, FontWeight.Normal),
  Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
  Font(R.font.jetbrains_mono_medium, FontWeight.SemiBold),
  Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
  Font(R.font.jetbrains_mono_bold, FontWeight.ExtraBold)
)

val DisplayFont: FontFamily = SpaceGroteskFont
val BodyFont: FontFamily = PlusJakartaSansFont
val MonoFont: FontFamily = JetBrainsMonoFont

val NeoBrutalistTypography = Typography(
  headlineMedium = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 26.sp, letterSpacing = (-0.5).sp, color = InkBlack
  ),
  titleLarge = TextStyle(
    fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
    fontSize = 20.sp, color = InkBlack
  ),
  titleMedium = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp, color = InkBlack
  ),
  bodyLarge = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp, color = InkBlack
  ),
  bodyMedium = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Medium,
    fontSize = 13.sp, color = InkBlack
  ),
  labelLarge = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold,
    fontSize = 13.sp, color = InkBlack
  ),
  labelMedium = TextStyle(
    fontFamily = BodyFont, fontWeight = FontWeight.Bold,
    fontSize = 12.sp, color = InkBlack
  ),
  labelSmall = TextStyle(
    fontFamily = MonoFont, fontWeight = FontWeight.Bold,
    fontSize = 11.sp, color = InkBlack
  )
)

val FridgeTypography: Typography = NeoBrutalistTypography
```

### `app/src/main/kotlin/com/truong/fridgezero/vm/FridgeViewModel.kt` (292 lines)

```kotlin
package com.truong.fridgezero.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.FridgeDb
import com.truong.fridgezero.data.FridgeEntity
import com.truong.fridgezero.data.FridgeRepository
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.domain.ConfigurableShelfLifeRepository
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.CulinaryGuideEngine
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
import com.truong.fridgezero.domain.MatchedIngredient
import com.truong.fridgezero.domain.NRect
import com.truong.fridgezero.domain.toDisplay
import com.truong.fridgezero.domain.zoneForCompartment
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class FridgeViewModel(
  private val repository: FridgeRepository
) : ViewModel() {

  private val pendingWelcomeSpin = MutableStateFlow(false)

  val uiState: StateFlow<FridgeUiState> = combine(
    repository.observeActiveFridge(),
    repository.observeCompartments(),
    repository.observeFoodItems(),
    pendingWelcomeSpin
  ) { activeFridge, compartments, foodItems, welcomeSpin ->
    val today = LocalDate.now().toEpochDay()
    val itemsByComp = foodItems.groupBy { it.compartmentId }

    val uiCompartments = compartments.map { entity ->
      val rawItems = itemsByComp[entity.id].orEmpty()
      val displayItems = rawItems.toDisplay(today)
      val worst = displayItems.minByOrNull { it.minDaysLeft }?.freshness ?: Freshness.OK
      val urgentCount = displayItems.count { it.freshness != Freshness.OK }

      CompartmentUi(
        id = entity.id,
        name = entity.name,
        rect = NRect(entity.x, entity.y, entity.w, entity.h),
        kind = entity.kind,
        shelves = entity.shelves,
        items = displayItems,
        worstFreshness = worst,
        urgentCount = urgentCount
      )
    }

    val recentKeys = foodItems
      .sortedByDescending { it.addedAtEpochDay }
      .map { it.iconKey }
      .distinct()
      .take(8)

    FridgeUiState(
      activeFridge = activeFridge,
      compartments = uiCompartments,
      recentItemKeys = recentKeys,
      playWelcomeSpin = welcomeSpin
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = FridgeUiState()
  )

  fun consumeWelcomeSpin() {
    pendingWelcomeSpin.value = false
  }

  fun addItem(
    compartmentId: String,
    name: String,
    iconKey: String,
    quantity: Int,
    daysLeft: Int,
    unit: String? = null,
    isOpened: Boolean = false
  ) {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      repository.upsertFoodItem(
        FoodItemEntity(
          compartmentId = compartmentId,
          name = cleanName,
          iconKey = iconKey,
          group = EmojiIconRepository.groupFor(iconKey).name,
          quantity = quantity.coerceAtLeast(1),
          unit = unit?.takeIf { it.isNotBlank() },
          addedAtEpochDay = today,
          expiresAtEpochDay = today + daysLeft.toLong(),
          isOpened = isOpened,
          status = ItemStatus.ACTIVE,
          source = "manual"
        )
      )
    }
  }

  fun updateItem(
    id: Long,
    name: String,
    iconKey: String,
    quantity: Int,
    daysLeft: Int,
    unit: String? = null
  ) {
    val cleanName = name.trim()
    if (cleanName.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      val existing = repository.getFoodItem(id) ?: return@launch
      repository.upsertFoodItem(
        existing.copy(
          name = cleanName,
          iconKey = iconKey,
          group = EmojiIconRepository.groupFor(iconKey).name,
          quantity = quantity.coerceAtLeast(1),
          unit = unit ?: existing.unit,
          expiresAtEpochDay = today + daysLeft.toLong()
        )
      )
    }
  }

  fun adjustQuantity(id: Long, delta: Int) {
    viewModelScope.launch {
      val existing = repository.getFoodItem(id) ?: return@launch
      val next = existing.quantity + delta
      if (next <= 0) {
        repository.updateItemStatus(id, ItemStatus.USED_UP)
      } else {
        repository.upsertFoodItem(existing.copy(quantity = next))
      }
    }
  }

  fun toggleItemOpened(id: Long) {
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      val existing = repository.getFoodItem(id) ?: return@launch
      val nextOpened = !existing.isOpened
      var nextExpiry = existing.expiresAtEpochDay
      if (nextOpened) {
        val currentRemaining = (existing.expiresAtEpochDay - today).toInt()
        val group = EmojiIconRepository.groupFor(existing.iconKey)
        val comp = repository.getCompartment(existing.compartmentId)
        val zone = comp?.let(::zoneForCompartment) ?: zoneForCompartment(existing.compartmentId)
        val suggestedOpened = ConfigurableShelfLifeRepository.suggestDays(
          iconKey = existing.iconKey,
          group = group,
          zone = zone,
          isOpened = true
        )
        if (suggestedOpened != null && currentRemaining > suggestedOpened) {
          nextExpiry = today + suggestedOpened.toLong()
        } else if (currentRemaining > 1) {
          val halved = (currentRemaining * 0.5f).toInt().coerceAtLeast(1)
          nextExpiry = today + halved.toLong()
        }
      }
      repository.upsertFoodItem(
        existing.copy(
          isOpened = nextOpened,
          expiresAtEpochDay = nextExpiry
        )
      )
    }
  }

  fun markItemStatus(ids: List<Long>, status: ItemStatus) {
    if (ids.isEmpty()) return
    viewModelScope.launch {
      repository.updateItemsStatus(ids, status)
    }
  }

  fun markItemStatus(id: Long, status: ItemStatus) = markItemStatus(listOf(id), status)

  fun consumeRecipeIngredients(matched: List<MatchedIngredient>) {
    if (matched.isEmpty()) return
    viewModelScope.launch {
      val uniqueIds = matched.flatMap { it.item.itemIds }.distinct()
      val entitiesById = uniqueIds.mapNotNull { id ->
        repository.getFoodItem(id)?.let { id to it }
      }.toMap()
      val deductions = CulinaryGuideEngine.planDeductions(matched, entitiesById)
      deductions.forEach { deduction ->
        val entity = entitiesById[deduction.itemId] ?: return@forEach
        if (deduction.remainingQuantity <= 0) {
          repository.updateItemStatus(deduction.itemId, ItemStatus.USED_UP)
        } else {
          repository.upsertFoodItem(entity.copy(quantity = deduction.remainingQuantity))
        }
      }
    }
  }

  fun moveItemToCompartment(
    ids: List<Long>,
    compartmentId: String,
    newDaysLeft: Int? = null
  ) {
    if (ids.isEmpty()) return
    val today = LocalDate.now().toEpochDay()
    viewModelScope.launch {
      if (newDaysLeft != null) {
        ids.forEach { itemId ->
          val existing = repository.getFoodItem(itemId) ?: return@forEach
          repository.upsertFoodItem(
            existing.copy(
              compartmentId = compartmentId,
              expiresAtEpochDay = today + newDaysLeft.toLong()
            )
          )
        }
      } else {
        repository.moveItemsToCompartment(ids, compartmentId)
      }
    }
  }

  fun moveItemToCompartment(
    id: Long,
    compartmentId: String,
    newDaysLeft: Int? = null
  ) = moveItemToCompartment(listOf(id), compartmentId, newDaysLeft)

  fun deleteItem(id: Long) {
    viewModelScope.launch {
      repository.updateItemStatus(id, ItemStatus.DISCARDED)
    }
  }

  fun applyFridgeTemplate(template: String, colorKey: String, handleStyle: String) {
    viewModelScope.launch {
      repository.applyTemplate(template, colorKey, handleStyle)
    }
  }

  fun saveCustomFridge(
    fridge: FridgeEntity,
    compartments: List<CompartmentEntity>,
    includeSampleItems: Boolean = false,
    triggerWelcomeSpin: Boolean = false
  ) {
    if (triggerWelcomeSpin) {
      pendingWelcomeSpin.value = true
    }
    viewModelScope.launch {
      repository.saveCustomFridge(fridge, compartments, includeSampleItems)
    }
  }

  fun loadSampleData() {
    viewModelScope.launch {
      repository.populateSampleData()
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory {
      val appContext = context.applicationContext
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val db = FridgeDb.get(appContext)
          val repo = FridgeRepository(db.fridgeDao(), db.compartmentDao(), db.foodItemDao())
          return FridgeViewModel(repo) as T
        }
      }
    }
  }
}
```

### `app/src/test/kotlin/com/truong/fridgezero/domain/LodAndFreshnessTest.kt` (490 lines)

```kotlin
package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.ItemStatus
import com.truong.fridgezero.data.Seed
import com.truong.fridgezero.ui.fridge3d.BoundedWebGlCrashTracker
import com.truong.fridgezero.ui.fridge3d.LOW_MEM_THRESHOLD_BYTES
import com.truong.fridgezero.ui.fridge3d.WebGlRecoveryAction
import com.truong.fridgezero.ui.fridge3d.isAllowedFridge3dUrl
import com.truong.fridgezero.ui.fridge3d.resolveWebGlQuality
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodGroup
import com.truong.fridgezero.ui.theme.DotGridInk
import com.truong.fridgezero.ui.theme.DotGridSpec
import com.truong.fridgezero.ui.theme.PaperCream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LodAndFreshnessTest {

  @Test
  fun freshnessThresholdsMatchSpec() {
    assertEquals(Freshness.BAD, freshnessOf(-1))
    assertEquals(Freshness.BAD, freshnessOf(0))
    assertEquals(Freshness.SOON, freshnessOf(1))
    assertEquals(Freshness.OK, freshnessOf(2))
    assertEquals(Freshness.OK, freshnessOf(3))
    assertEquals(Freshness.OK, freshnessOf(4))
  }

  @Test
  fun seedItemsMatchExpectedCounts() {
    val today = 20_000L
    val items = Seed.foodItems(today)
    assertEquals(4, items.count { it.compartmentId == "freezer" })
    assertEquals(9, items.count { it.compartmentId == "fresh" })
    assertEquals(3, items.count { it.compartmentId == "crisper" })
  }

  @Test
  fun toDisplayAggregatesDuplicatesTracksAllIdsAndSortsByExpiry() {
    val today = 20_000L
    val items = listOf(
      FoodItemEntity(
        id = 1L,
        compartmentId = "fresh",
        name = "Sữa tươi",
        iconKey = "milk",
        group = "SUA_TRUNG",
        quantity = 1,
        addedAtEpochDay = today,
        expiresAtEpochDay = today + 5
      ),
      FoodItemEntity(
        id = 2L,
        compartmentId = "fresh",
        name = "sữa tươi",
        iconKey = "milk",
        group = "SUA_TRUNG",
        quantity = 2,
        addedAtEpochDay = today,
        expiresAtEpochDay = today + 2
      ),
      FoodItemEntity(
        id = 3L,
        compartmentId = "fresh",
        name = "Thịt gà",
        iconKey = "chicken",
        group = "THIT",
        quantity = 1,
        addedAtEpochDay = today,
        expiresAtEpochDay = today + 1
      )
    )
    val display = items.toDisplay(today)
    assertEquals(2, display.size)
    assertEquals("Thịt gà", display[0].name)
    assertEquals(1, display[0].minDaysLeft)
    assertEquals(listOf(3L), display[0].itemIds)

    assertEquals(2L, display[1].primaryId)
    assertEquals(listOf(1L, 2L), display[1].itemIds)
    assertEquals(3, display[1].quantity)
    assertEquals(2, display[1].minDaysLeft)
  }

  @Test
  fun zoneDetectionSupportsCustomVietnameseCompartmentNamesAndKinds() {
    val customFreezer = CompartmentEntity("c_abc", "f1", "Ngăn đông mềm", 0f, 0f, 1f, 0.3f, CompartmentKind.DOOR, 1, 0)
    val customIce = CompartmentEntity("c_xyz", "f1", "Ngăn đá nhỏ", 0f, 0f, 1f, 0.2f, CompartmentKind.DOOR, 1, 1)
    val customDrawer = CompartmentEntity("c_123", "f1", "Ngăn kéo phụ", 0f, 0f, 1f, 0.2f, CompartmentKind.DRAWER, 1, 2)
    val customChilled = CompartmentEntity("c_999", "f1", "Cánh phụ", 0f, 0f, 1f, 0.5f, CompartmentKind.DOOR, 2, 3)

    assertEquals(StorageZone.FREEZER, zoneForCompartment(customFreezer))
    assertEquals(StorageZone.FREEZER, zoneForCompartment(customIce))
    assertEquals(StorageZone.DRAWER, zoneForCompartment(customDrawer))
    assertEquals(StorageZone.CHILLED, zoneForCompartment(customChilled))
  }

  @Test
  fun frenchDoorTemplateHasTwoTopDoorsPlusTwoDrawers() {
    val comps = Seed.compartmentsForTemplate("french_door")
    assertEquals(4, comps.size)
    assertEquals(CompartmentKind.DOOR, comps[0].kind)
    assertEquals(CompartmentKind.DOOR, comps[1].kind)
    assertEquals(0.04f, comps[0].x, 0.001f)
    assertEquals(0.453f, comps[0].w, 0.001f)
    assertEquals(0.507f, comps[1].x, 0.001f)
    assertEquals(0.453f, comps[1].w, 0.001f)
    assertTrue(comps[0].y == 0.02f && comps[1].y == 0.02f)
    assertEquals(CompartmentKind.DRAWER, comps[2].kind)
    assertEquals(CompartmentKind.DRAWER, comps[3].kind)
  }

  @Test
  fun shelfLifeItemRuleOverridesGroupFallback() {
    assertEquals(7, ConfigurableShelfLifeRepository.suggestDays("milk", FoodGroup.SUA_TRUNG, StorageZone.CHILLED))
    assertEquals(21, ConfigurableShelfLifeRepository.suggestDays("egg", FoodGroup.SUA_TRUNG, StorageZone.CHILLED))
    EmojiIconRepository.entries.forEach { entry ->
      assertNotNull(
        "Missing chilled shelf-life rule for ${entry.key}",
        ConfigurableShelfLifeRepository.suggestDays(entry.key, entry.group, StorageZone.CHILLED)
      )
    }
  }

  @Test
  fun shelfLifeZoneSwitchingUsesChilledFreezerAndDrawerRules() {
    assertEquals(2, ConfigurableShelfLifeRepository.suggestDays("chicken", FoodGroup.THIT, StorageZone.CHILLED))
    assertEquals(270, ConfigurableShelfLifeRepository.suggestDays("chicken", FoodGroup.THIT, StorageZone.FREEZER))
    assertEquals(14, ConfigurableShelfLifeRepository.suggestDays("carrot", FoodGroup.RAU, StorageZone.CHILLED))
    assertEquals(21, ConfigurableShelfLifeRepository.suggestDays("carrot", FoodGroup.RAU, StorageZone.DRAWER))
  }

  @Test
  fun shelfLifeFallsBackToGroupForUnknownItemKeys() {
    assertEquals(3, ConfigurableShelfLifeRepository.suggestDays("custom_item", FoodGroup.THIT, StorageZone.CHILLED))
    assertEquals(3, ConfigurableShelfLifeRepository.suggestDays("custom_item", FoodGroup.AN_SAN, StorageZone.CHILLED))
  }

  @Test
  fun culinaryCatalogCoversAll32IconKeysAndMeetsRecipeMetadataSpec() {
    val recipes = CulinaryGuideRepository.recipes
    assertEquals(36, recipes.size)
    assertEquals(36, recipes.map { it.id }.toSet().size)

    val coveredKeys = recipes
      .flatMap { it.fridgeIngredients }
      .flatMap { it.iconKeys }
      .toSet()
    val allIconKeys = EmojiIconRepository.entries.map { it.key }.toSet()
    assertTrue("Missing icon keys: ${allIconKeys - coveredKeys}", coveredKeys.containsAll(allIconKeys))

    recipes.forEach { recipe ->
      assertTrue("${recipe.id} must have 1..4 fridge ingredients", recipe.fridgeIngredients.size in 1..4)
      assertTrue("${recipe.id} must declare pantry staples", recipe.pantryStaples.isNotEmpty())
      assertTrue("${recipe.id} must have 4..5 steps", recipe.steps.size in 4..5)
      assertTrue("${recipe.id} must cite sourceName", recipe.sourceName.isNotBlank())
      assertTrue("${recipe.id} must cite https sourceUrl", recipe.sourceUrl.startsWith("https://"))
      assertEquals(
        "${recipe.id} QUICK_15 tag must align with cookMinutes <= 15",
        recipe.cookMinutes <= 15,
        CulinaryFilter.QUICK_15 in recipe.categories
      )
      assertEquals(
        "${recipe.id} difficulty should default from cookMinutes",
        if (recipe.cookMinutes <= 15) RecipeDifficulty.EASY else RecipeDifficulty.MEDIUM,
        recipe.difficulty
      )
      recipe.fridgeIngredients.forEach { ing ->
        assertTrue("${recipe.id}/${ing.name} amountText blank", ing.amountText.isNotBlank())
        assertTrue("${recipe.id}/${ing.name} unitsToConsume < 1", ing.unitsToConsume >= 1)
      }
    }

    val seedDisplay = Seed.foodItems(20_000L).toDisplay(20_000L)
    val readyNow = CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.READY_NOW)
    assertTrue("Expected at least 10 seed-ready recipes, found ${readyNow.size}", readyNow.size >= 10)

    val rescueUrgent = CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.RESCUE_URGENT)
    assertTrue(rescueUrgent.isNotEmpty())
    assertTrue(rescueUrgent.all { it.hasUrgentMatch })

    val topOverall = CulinaryGuideEngine.recommend(seedDisplay, CulinaryFilter.ALL).first()
    assertTrue(topOverall.isReadyToCook)
    assertTrue(topOverall.hasUrgentMatch)
    assertTrue(topOverall.matchedIngredients.all { it.tier == MatchTier.EXACT_ICON })
  }

  @Test
  fun culinaryEngineThreeLayerMatchingPicksEarliestExpiryAndAvoidsDoubleAssignment() {
    val recipe = CulinaryRecipe(
      id = "test_3layer",
      title = "Test 3-layer recipe",
      emoji = "🍳",
      summary = "Test",
      cookMinutes = 15,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(
        RecipeIngredient("Thịt gà", listOf("chicken"), listOf("thịt gà", "gà"), FoodGroup.THIT, "200g"),
        RecipeIngredient("Ớt chuông", listOf("bell_pepper"), listOf("ớt chuông"), FoodGroup.RAU, "1 quả"),
        RecipeIngredient("Cà rốt", listOf("carrot"), listOf("cà rốt"), FoodGroup.RAU, "1 củ"),
        RecipeIngredient("Bơ", listOf("butter"), listOf("bơ"), FoodGroup.SUA_TRUNG, "10g")
      ),
      pantryStaples = listOf("Muối"),
      steps = listOf("B1", "B2", "B3", "B4"),
      sourceName = "VnExpress Ẩm Thực",
      sourceUrl = "https://vnexpress.net/doi-song/cooking"
    )
    val fridge = listOf(
      DisplayItem(10L, listOf(10L), "fresh", "Ức gà tươi", "chicken", FoodGroup.THIT, 1, minDaysLeft = 5),
      DisplayItem(11L, listOf(11L), "fresh", "Đùi gà", "chicken", FoodGroup.THIT, 1, minDaysLeft = 1),
      DisplayItem(12L, listOf(12L), "fresh", "Thịt bò", "meat", FoodGroup.THIT, 1, minDaysLeft = 0),
      DisplayItem(13L, listOf(13L), "crisper", "Ot chuong do", "custom_veg", FoodGroup.RAU, 1, minDaysLeft = 2),
      DisplayItem(14L, listOf(14L), "crisper", "Nấm đùi gà", "mushroom", FoodGroup.RAU, 1, minDaysLeft = 3)
    )

    val eval = CulinaryGuideEngine.evaluate(recipe, fridge)
    assertEquals(3, eval.matchedCount)
    assertEquals(1, eval.missingIngredients.size)
    assertEquals("Bơ", eval.missingIngredients.single().name)

    val chickenMatch = eval.matchedIngredients.first { it.ingredient.name == "Thịt gà" }
    assertEquals(MatchTier.EXACT_ICON, chickenMatch.tier)
    assertEquals(11L, chickenMatch.item.primaryId)

    val pepperMatch = eval.matchedIngredients.first { it.ingredient.name == "Ớt chuông" }
    assertEquals(MatchTier.KEYWORD, pepperMatch.tier)
    assertEquals(13L, pepperMatch.item.primaryId)

    val carrotMatch = eval.matchedIngredients.first { it.ingredient.name == "Cà rốt" }
    assertEquals(MatchTier.GROUP_FALLBACK, carrotMatch.tier)
    assertEquals(14L, carrotMatch.item.primaryId)

    // Layer 3 must not steal an exact Layer 1 item belonging to another ingredient in the same recipe.
    val antiStealRecipe = CulinaryRecipe(
      id = "anti_steal",
      title = "Anti steal",
      emoji = "🥗",
      summary = "Test",
      cookMinutes = 10,
      categories = setOf(CulinaryFilter.QUICK_15),
      fridgeIngredients = listOf(
        RecipeIngredient("Cà rốt", listOf("carrot"), listOf("cà rốt"), FoodGroup.RAU, "1 củ"),
        RecipeIngredient("Xà lách", listOf("lettuce"), listOf("xà lách"), FoodGroup.RAU, "1 bó")
      ),
      pantryStaples = listOf("Muối"),
      steps = listOf("B1", "B2", "B3", "B4"),
      sourceName = "Cooky",
      sourceUrl = "https://www.cooky.vn/cach-lam"
    )
    val antiStealEval = CulinaryGuideEngine.evaluate(
      antiStealRecipe,
      listOf(
        DisplayItem(20L, listOf(20L), "crisper", "Xà lách", "lettuce", FoodGroup.RAU, 1, minDaysLeft = 2),
        DisplayItem(21L, listOf(21L), "crisper", "Nấm rơm", "mushroom", FoodGroup.RAU, 1, minDaysLeft = 4)
      )
    )
    assertTrue(antiStealEval.isReadyToCook)
    val lettuceRes = antiStealEval.matchedIngredients.first { it.ingredient.name == "Xà lách" }
    assertEquals(MatchTier.EXACT_ICON, lettuceRes.tier)
    assertEquals(20L, lettuceRes.item.primaryId)
    val carrotRes = antiStealEval.matchedIngredients.first { it.ingredient.name == "Cà rốt" }
    assertEquals(MatchTier.GROUP_FALLBACK, carrotRes.tier)
    assertEquals(21L, carrotRes.item.primaryId)

    // Single RAU item must never be assigned to two ingredients in the same recipe.
    val singleRauEval = CulinaryGuideEngine.evaluate(
      antiStealRecipe,
      listOf(DisplayItem(21L, listOf(21L), "crisper", "Nấm rơm", "mushroom", FoodGroup.RAU, 1, minDaysLeft = 4))
    )
    assertEquals(1, singleRauEval.matchedCount)
    assertEquals(1, singleRauEval.missingIngredients.size)
    assertFalse(singleRauEval.isReadyToCook)
  }

  @Test
  fun culinaryEngineExpiryRescueScoringAndFilterRanking() {
    fun makeRecipe(
      id: String,
      cookMinutes: Int,
      categories: Set<CulinaryFilter>,
      vararg keys: Pair<String, FoodGroup>
    ) = CulinaryRecipe(
      id = id,
      title = id,
      emoji = "🍽️",
      summary = id,
      cookMinutes = cookMinutes,
      categories = categories,
      fridgeIngredients = keys.map { (k, g) ->
        RecipeIngredient(k, listOf(k), listOf(k), g, "1 phần")
      },
      pantryStaples = listOf("Muối"),
      steps = listOf("B1", "B2", "B3", "B4"),
      sourceName = "Savoury Days",
      sourceUrl = "https://www.savourydays.com/"
    )

    val twoUrgentReady = makeRecipe(
      "two_urgent_ready", 12, setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      "salmon" to FoodGroup.CA_HAI_SAN, "lettuce" to FoodGroup.RAU
    ) // 15 + 15 + 8 = 38
    val twoFreshReady = makeRecipe(
      "two_fresh_ready", 25, setOf(CulinaryFilter.VIET_HOME, CulinaryFilter.EAT_CLEAN),
      "milk" to FoodGroup.SUA_TRUNG, "carrot" to FoodGroup.RAU
    ) // 6 + 6 + 8 = 20
    val oneUrgentOneMissing = makeRecipe(
      "one_urgent_missing", 15, setOf(CulinaryFilter.QUICK_15, CulinaryFilter.EAT_CLEAN),
      "chicken" to FoodGroup.THIT, "dumpling" to FoodGroup.AN_SAN
    ) // 15 - 3 = 12
    val twoMissing = makeRecipe(
      "two_missing", 20, setOf(CulinaryFilter.VIET_HOME),
      "mango" to FoodGroup.TRAI_CAY, "apple" to FoodGroup.TRAI_CAY
    ) // -3 - 3 = -6

    val fridge = listOf(
      DisplayItem(1L, listOf(1L), "freezer", "Cá hồi", "salmon", FoodGroup.CA_HAI_SAN, 1, minDaysLeft = -1),
      DisplayItem(2L, listOf(2L), "crisper", "Xà lách", "lettuce", FoodGroup.RAU, 1, minDaysLeft = 1),
      DisplayItem(3L, listOf(3L), "fresh", "Thịt gà", "chicken", FoodGroup.THIT, 1, minDaysLeft = 1),
      DisplayItem(4L, listOf(4L), "fresh", "Sữa", "milk", FoodGroup.SUA_TRUNG, 1, minDaysLeft = 2),
      DisplayItem(5L, listOf(5L), "crisper", "Cà rốt", "carrot", FoodGroup.RAU, 1, minDaysLeft = 8)
    )
    val subset = listOf(twoMissing, twoFreshReady, oneUrgentOneMissing, twoUrgentReady)

    val all = CulinaryGuideEngine.recommend(fridge, CulinaryFilter.ALL, subset)
    assertEquals(listOf("two_urgent_ready", "two_fresh_ready", "one_urgent_missing", "two_missing"), all.map { it.recipe.id })
    assertEquals(listOf(38, 20, 12, -6), all.map { it.score })

    assertEquals(listOf("two_urgent_ready", "two_fresh_ready"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.READY_NOW, subset).map { it.recipe.id })
    assertEquals(listOf("two_urgent_ready", "one_urgent_missing"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.RESCUE_URGENT, subset).map { it.recipe.id })
    assertEquals(listOf("two_urgent_ready", "one_urgent_missing"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.QUICK_15, subset).map { it.recipe.id })
    assertEquals(listOf("two_urgent_ready", "two_fresh_ready", "two_missing"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.VIET_HOME, subset).map { it.recipe.id })
    assertEquals(listOf("two_fresh_ready", "one_urgent_missing"), CulinaryGuideEngine.recommend(fridge, CulinaryFilter.EAT_CLEAN, subset).map { it.recipe.id })
  }

  @Test
  fun culinaryEnginePantryStaplesNeverBlockReadiness() {
    val recipe = CulinaryRecipe(
      id = "pantry_only_test",
      title = "Trứng chiên nước mắm tỏi",
      emoji = "🥚",
      summary = "Gia vị cơ bản không bao giờ chặn trạng thái đủ đồ.",
      cookMinutes = 10,
      categories = setOf(CulinaryFilter.QUICK_15, CulinaryFilter.VIET_HOME),
      fridgeIngredients = listOf(
        RecipeIngredient("Trứng gà", listOf("egg"), listOf("trứng"), FoodGroup.SUA_TRUNG, "2 quả")
      ),
      pantryStaples = listOf("Tỏi băm", "Nước mắm", "Dầu ăn", "Tiêu xay", "Muối", "Đường"),
      steps = listOf("Đánh trứng.", "Phi thơm tỏi.", "Chiên trứng.", "Chan nước mắm đường và rắc tiêu."),
      sourceName = "VnExpress Ẩm Thực",
      sourceUrl = "https://vnexpress.net/doi-song/cooking"
    )
    val fridgeWithOnlyEgg = listOf(
      DisplayItem(1L, listOf(1L), "fresh", "Trứng", "egg", FoodGroup.SUA_TRUNG, 2, minDaysLeft = 5)
    )

    val result = CulinaryGuideEngine.evaluate(recipe, fridgeWithOnlyEgg)
    assertTrue(result.isReadyToCook)
    assertTrue(result.missingIngredients.isEmpty())
    assertEquals(1, result.matchedCount)
    assertEquals(14, result.score)
  }

  @Test
  fun culinaryEnginePlanDeductionsDecrementsAndMarksUsedUpEarliestFirst() {
    val today = 20_000L
    val milkEarly = FoodItemEntity(
      id = 101L,
      compartmentId = "fresh",
      name = "Sữa tươi",
      iconKey = "milk",
      group = FoodGroup.SUA_TRUNG.name,
      quantity = 1,
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 1,
      status = ItemStatus.ACTIVE
    )
    val milkLater = FoodItemEntity(
      id = 102L,
      compartmentId = "fresh",
      name = "Sữa tươi",
      iconKey = "milk",
      group = FoodGroup.SUA_TRUNG.name,
      quantity = 3,
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 4,
      status = ItemStatus.ACTIVE
    )
    val chickenSingle = FoodItemEntity(
      id = 103L,
      compartmentId = "fresh",
      name = "Thịt gà",
      iconKey = "chicken",
      group = FoodGroup.THIT.name,
      quantity = 1,
      addedAtEpochDay = today,
      expiresAtEpochDay = today + 1,
      status = ItemStatus.ACTIVE
    )

    val milkDisplay = listOf(milkLater, milkEarly).toDisplay(today).single()
    val chickenDisplay = listOf(chickenSingle).toDisplay(today).single()

    val matched = listOf(
      MatchedIngredient(
        ingredient = RecipeIngredient("Sữa tươi", listOf("milk"), listOf("sữa"), FoodGroup.SUA_TRUNG, "2 hộp", unitsToConsume = 2),
        item = milkDisplay,
        tier = MatchTier.EXACT_ICON
      ),
      MatchedIngredient(
        ingredient = RecipeIngredient("Thịt gà", listOf("chicken"), listOf("gà"), FoodGroup.THIT, "300g", unitsToConsume = 1),
        item = chickenDisplay,
        tier = MatchTier.EXACT_ICON
      )
    )
    val entitiesById = mapOf(
      101L to milkEarly,
      102L to milkLater,
      103L to chickenSingle
    )

    val deductions = CulinaryGuideEngine.planDeductions(matched, entitiesById).associateBy { it.itemId }
    assertEquals(3, deductions.size)
    assertEquals(0, deductions[101L]?.remainingQuantity)
    assertEquals(2, deductions[102L]?.remainingQuantity)
    assertEquals(0, deductions[103L]?.remainingQuantity)
  }

  @Test
  fun fridge3dUrlAllowlistAllowsOnlyLocalAssetsAndAboutBlank() {
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/index.html"))
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/index.html?q=lite"))
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/index.html?q=high"))
    assertTrue(isAllowedFridge3dUrl("file:///android_asset/fridge3d/three.min.js"))
    assertTrue(isAllowedFridge3dUrl("about:blank"))

    assertFalse(isAllowedFridge3dUrl("https://example.com"))
    assertFalse(isAllowedFridge3dUrl("http://localhost:8080/"))
    assertFalse(isAllowedFridge3dUrl("file:///data/local/tmp/exploit.html"))
    assertFalse(isAllowedFridge3dUrl("file:///android_asset/fridge3d/../secret.html"))
    assertFalse(isAllowedFridge3dUrl(""))
    assertFalse(isAllowedFridge3dUrl(null))
  }

  @Test
  fun webGlQualityResolvesLiteForLowRamLowMemOrForcedFlag() {
    assertEquals("high", resolveWebGlQuality(isLowRamDevice = false, totalMemBytes = 8L * 1024 * 1024 * 1024, forceLiteMode = false))
    assertEquals("lite", resolveWebGlQuality(isLowRamDevice = true, totalMemBytes = 8L * 1024 * 1024 * 1024, forceLiteMode = false))
    assertEquals("lite", resolveWebGlQuality(isLowRamDevice = false, totalMemBytes = LOW_MEM_THRESHOLD_BYTES, forceLiteMode = false))
    assertEquals("lite", resolveWebGlQuality(isLowRamDevice = false, totalMemBytes = 8L * 1024 * 1024 * 1024, forceLiteMode = true))
  }

  @Test
  fun boundedWebGlCrashTrackerEnforcesTwoRecoveriesIn60SecondsThenFallback() {
    val tracker = BoundedWebGlCrashTracker(windowMs = 60_000L, maxRecoveries = 2)

    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE_AND_PERSIST, tracker.recordCrash(1_000L))
    assertEquals(1, tracker.recentCrashCount(1_000L))
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE, tracker.recordCrash(10_000L))
    assertEquals(2, tracker.recentCrashCount(10_000L))
    assertEquals(WebGlRecoveryAction.FALLBACK_TO_2D, tracker.recordCrash(59_000L))

    // Leaving and re-entering screen resets recovery budget.
    tracker.reset()
    assertEquals(0, tracker.recentCrashCount(60_000L))
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE_AND_PERSIST, tracker.recordCrash(100_000L))
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE, tracker.recordCrash(120_000L))

    // Old entries (> 60s earlier) expire from sliding window.
    assertEquals(WebGlRecoveryAction.REBUILD_IN_LITE, tracker.recordCrash(165_000L))
    assertEquals(2, tracker.recentCrashCount(165_000L))
  }

  @Test
  fun dotGridSpecMatchesDesignTokens() {
    assertEquals("#EAE2D3", DotGridSpec.PAPER_HEX)
    assertEquals("#C4B8A3", DotGridSpec.DOT_HEX)
    assertEquals(18f, DotGridSpec.STEP_DP, 0.0001f)
    assertEquals(1.5f, DotGridSpec.DOT_RADIUS_DP, 0.0001f)
    assertEquals(PaperCream, DotGridSpec.paperColor)
    assertEquals(DotGridInk, DotGridSpec.dotColor)
  }
}
```
