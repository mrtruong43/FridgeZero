# FridgeZero — Complete Source Code & Architecture Summary

> **Generated:** 2026-11-18 23:06:03 +07:00
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
        │   │   └── ui/fridge3d/Fridge3DView.kt
        │   │   └── ui/fridge3d/ShellColorSpec.kt
        │   │   └── ui/home/HomeDashboardHeader.kt
        │   │   └── ui/home/HomeScreen.kt
        │   │   └── ui/icons/FoodIcon.kt
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

### `docs/spec.md` (194 lines)

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
   - Nền giấy kem ấm `#FAF8F5` phủ lưới chấm `Dot Grid`, bề mặt thẻ trắng tinh `#FFFFFF`, viền đen mực đậm `2.5.dp` (`#111111`), bóng đổ cứng lệch góc `4.dp × 4.dp` (`0.dp` blur), kết hợp bảng màu pastel độ tương phản cao (`#A3E635`, `#FACC15`, `#FB7185`, `#C084FC`, `#38BDF8`).
3. **Kế thừa nguyên vẹn tầng dữ liệu từ `spec_v2`:**
   - Giữ nguyên 3 bảng Room Database (`FridgeEntity`, `CompartmentEntity`, `FoodItemEntity`), trạng thái lịch sử lãng phí (`ACTIVE`, `USED_UP`, `DISCARDED`), cấu trúc tra cứu `ShelfLife` và lịch nhắc hạn dùng `WorkManager`.
4. **Quy tắc dữ liệu & tài nguyên:**
   - Không tự bịa số liệu thời hạn bảo quản `ShelfLife` chưa kiểm chứng; để cấu trúc tra cứu mở để chủ dự án nạp dữ liệu chuẩn.
   - Toàn bộ chuỗi hiển thị tiếng Việt đặt tập trung trong `res/values/strings.xml`.
   - Xin quyền `POST_NOTIFICATIONS` (Android 13+) và `CAMERA` (Giai đoạn 2) đúng ngữ cảnh khi người dùng kích hoạt tính năng tương ứng.

---

## §1. Hệ thống Thiết kế & Tài nguyên Thị giác (Design System)

### 1.1. Bảng màu Neo-Brutalist (`ui/theme/Color.kt`)
| Token | Mã Hex | Vai trò & Ngữ cảnh sử dụng |
| :--- | :--- | :--- |
| `PaperCream` | `#FAF8F5` | Màu nền chính của toàn bộ ứng dụng và Bottom Sheet. |
| `DotGridInk` | `#D6D3CD` | Màu chấm lưới nền (`1.25.dp` chấm, bước lưới `18.dp`). |
| `SurfaceWhite` | `#FFFFFF` | Nền thẻ Bento, nút phụ, khung xem trước 3D và thanh Task Bar. |
| `InkBlack` | `#111111` | Viền khối (`2.5.dp`), viền chip (`2.dp`), bóng đổ cứng và chữ chính. |
| `LimeFresh` | `#A3E635` | Trạng thái tươi (`> 3` ngày), nút CTA chính (`CHỐT TỦ NÀY`), FAB `+ Thêm nhanh`. |
| `LemonWarn` | `#FACC15` | Trạng thái sắp hết hạn (`2–3` ngày), badge bước thiết kế, nút nhắc hạn. |
| `CoralUrgent` | `#FB7185` | Trạng thái gấp ($\le 1$ ngày / quá hạn), nút xóa ngăn/món, cảnh báo khẩn. |
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
- Sử dụng **Offline Three.js WebGL Stage** đóng gói cục bộ trong `app/src/main/assets/fridge3d/` (`three.min.js` UMD + `index.html`), nhúng trong Jetpack Compose qua hardware-accelerated `WebView` (`AndroidView`), nền trong suốt (`alpha: true`, `Color.TRANSPARENT`) để hiển thị trực tiếp trên nền giấy kem lưới chấm `#FAF8F5`.
- Mục tiêu hiệu năng: **60fps+** ổn định, nhẹ dung lượng APK và không phụ thuộc mạng hay native SceneView/Filament NDK.

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

## §3. Luồng Trải nghiệm 3 Màn hình & Các Bottom Sheet

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

### 3.2. Màn 2 — Màn hình chính Tủ 3D Tương tác (`ui/hero/FridgeHeroScreen.kt`)
- **Dashboard Đầu trang (2 hàng thông tin gọn gàng):**
  - **Hàng 1:**
    - Trái: Nhãn thương hiệu nhỏ `FRIDGEZERO` + **Lời chào thân thiện động theo khung giờ** (*"Chào buổi sáng / trưa / chiều / tối, [Tên]!"*).
    - Phải: **Đồng hồ số thời gian thực** định dạng `HH:mm · Thứ, dd/MM` (`JetBrains Mono`, nền `#111111`, chữ trắng, bóng cứng `#A3E635`).
  - **Hàng 2:**
    - Trái: **Huy hiệu Thời tiết** (`☀️ 28°C · Đà Nẵng`, nền `#E0F2FE`, viền `2.dp` `#111111`).
    - Phải: **Nút Thông báo Sắp hết hạn** (`🔔 N sắp hết hạn`, tự đổi màu `#FB7185` nếu có món $\le 1$ ngày, `#FACC15` nếu có món $2–3$ ngày, hoặc `#A3E635` *"✨ Tất cả đều tươi"* nếu không có món gấp). Bấm vào mở `UrgentItemsSheet`.
- **Sân khấu Trung tâm:**
  - Hiển thị khối tủ 3D đã chốt cùng các Sticker bám cửa và pill hướng dẫn phía trên `↔ Vuốt xoay 360° · Chạm cửa để mở`.
  - **Không đặt nút đổi màu vỏ tủ ở màn hình này** (đã cố định từ Màn 1).
- **Nút nổi (`FAB`) `+ Thêm nhanh`:**
  - Nổi bật ở góc dưới bên phải ngay phía trên thanh Task Bar (`bottom = 86.dp`, `end = 16.dp`), nền `#A3E635`, icon `+` trong ô vuông `#111111`, mở `QuickAddSheet`.
- **Thanh Task Bar cố định dưới đáy (`height = 70.dp`, nền `#FFFFFF`, viền trên `2.5.dp` `#111111`):**
  1. **`🧊 Tủ lạnh` (nền `#FEF9C3`):** Căn lại góc nhìn chuẩn của tủ 3D.
  2. **`📷 Quét AI` (nổi bật nền `#C084FC` + sticker nghiêng `GĐ2`):** Điểm vào luồng CameraX quét hóa đơn AI. Ở GĐ1 mở `CameraAiPreviewSheet` (đã khai báo sẵn quyền/giao diện khung ngắm để sẵn sàng nối API OCR ở GĐ2).
  3. **`⚙️ Đổi dáng tủ` (nền `#FFFFFF`):** Quay lại Màn 1 (`FridgeSetupScreen`) để chỉnh sửa dáng tủ, màu vỏ hoặc số ngăn.

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
    - *Hàng trên:* Khung icon `48.dp` bo góc chứa Emoji/Icon `32.dp` bên trái + Sticker hạn dùng nghiêng `-2°` bên phải (`Quá hạn Nd` / `Còn 1d` nền `#FB7185`, `Còn 2–3d` nền `#FACC15`, `> 3d` nền `#A3E635`).
    - *Phần giữa:* Tên thực phẩm tối đa 2 dòng (`maxLines = 2`, không cắt chữ giữa từ) + tag nhóm thực phẩm và tag `Đã mở` (nền `#FEF08A`) nếu đã mở bao bì.
    - *Hàng đáy:* Bộ đếm nhanh **`−` `[qty]` `+`** ngay trên mặt thẻ; bấm `−` khi số lượng đang là `1` sẽ tự động mở `ItemDetailSheet` ở bước hỏi lý do xóa.

### 3.4. Hệ thống 4 Bottom Sheet (`ui/sheets/`)
1. **`ItemDetailSheet` (Chi tiết, Sửa, Chuyển ngăn & Xóa có lý do):**
   - Hiển thị icon lớn, tên món, nhóm, badge hạn dùng.
   - Bộ đếm số lượng `−` / `+` và nút bật/tắt **`Đã mở nắp`** (tự động rút ngắn 50% số ngày hạn còn lại và gắn nhãn `Đã mở`).
   - **Chuyển ngăn bảo quản:** Danh sách chip các ngăn khác trong tủ; khi chuyển giữa ngăn mát và ngăn đông sẽ tự điều chỉnh hạn dùng phù hợp.
   - **Xóa món bắt buộc chọn lý do:** Bấm `🗑️ Xóa món khỏi tủ` sẽ bung 2 lựa chọn: **`✓ Đã dùng hết`** (`ItemStatus.USED_UP`) hoặc **`✕ Bỏ đi`** (`ItemStatus.DISCARDED`).
2. **`QuickAddSheet` (Thêm nhanh thực phẩm):**
   - Hàng chip gợi ý món quen chạm 1 lần điền sẵn icon, tên, nhóm và hạn dùng gợi ý.
   - Ô nhập tên thực phẩm + dải chip chọn ngăn cất + bộ đếm số lượng + 4 chip hạn dùng nhanh (`+3 ngày`, `+1 tuần`, `+2 tuần`, `+1 tháng`) kèm DatePicker tùy chọn.
3. **`UrgentItemsSheet` (Trung tâm cảnh báo sắp hết hạn):**
   - Mở khi chạm nút `🔔 N sắp hết hạn` trên Dashboard Màn 2; liệt kê các món còn $\le 3$ ngày kèm nút **`Mở ngăn →`** nhảy thẳng tới đúng ngăn đó ở Màn 3 với bộ lọc `Sắp hết hạn`.
4. **`CameraAiPreviewSheet` (Cổng CameraX Quét Hóa đơn — Giai đoạn 2):**
   - Mở khi chạm `📷 Quét AI` trên Task Bar; hiển thị khung ngắm CameraX với 4 góc định vị hóa đơn, tia quét laser xanh `#A3E635`, nút đèn Flash, nút chụp và chọn ảnh thư viện (sẵn sàng nối luồng AI OCR ở GĐ2).

---

## §4. Tầng Dữ liệu Room & Logic Nghiệp vụ (`data/`, `domain/`)

### 4.1. Mô hình 3 bảng Room Database
- **`FridgeEntity`:** `id`, `name`, `template` (`single`, `top_freezer`, `bottom_freezer`, `side_by_side`, `french_door`, `mini`), `colorKey` (`inox`, `butter`, `mint`, `matte`), `isLockedIn: Boolean`.
- **`CompartmentEntity`:** `id`, `fridgeId`, `name`, `kind` (`DOOR`, `DRAWER`), `x`, `y`, `w`, `h` (`0f..1f`), `row`, `col`, `sort`.
- **`FoodItemEntity`:** `id`, `compartmentId`, `name`, `iconKey`, `group`, `quantity`, `unit`, `addedAtEpochDay`, `expiryEpochDay`, `isOpened: Boolean`, `price: Long?`, `status: ItemStatus` (`ACTIVE`, `USED_UP`, `DISCARDED`), `source: String` (`"manual"`, `"ai_receipt"`).

### 4.2. Logic Nghiệp vụ & Thông báo
- **Không tự xóa món quá hạn:** Món có `daysLeft < 0` vẫn hiển thị ở đầu lưới Bento với sticker màu `#FB7185` ghi rõ *"Quá hạn N ngày"* để người dùng tự xử lý và ghi nhận lý do.
- **Bảng `ShelfLifeRepository`:** Giữ cấu trúc mở tra cứu theo `(foodKey, compartmentKind, isOpened)`, không hard-code số ngày chưa kiểm chứng.
- **Nhắc hạn dùng (`ExpiryReminderWorker`):** Chạy kiểm tra định kỳ hằng ngày qua `WorkManager`; chỉ yêu cầu quyền `POST_NOTIFICATIONS` (Android 13+) khi người dùng lưu món có hạn ngắn hoặc bật nhắc nhở.

---

## §5. Cấu trúc Package Kotlin & Lộ trình Chuyển đổi từ v2 sang v3

```text
app/src/main/
├── assets/fridge3d/    # three.min.js (r128 offline) + index.html (Three.js WebGL stage)
└── kotlin/com/truong/fridgezero/
    ├── ui/
    │   ├── theme/      # Color.kt, Type.kt, Theme.kt, BrutalistComponents.kt
    │   ├── fridge3d/   # Fridge3DView.kt, ShellColorSpec.kt (WebView + AndroidBridge)
    │   ├── designer/   # FridgeDesignerScreen.kt, FridgeLayoutEditor.kt (Màn 1)
    │   ├── home/       # HomeScreen.kt, HomeDashboardHeader.kt (Màn 2)
    │   ├── bento/      # CompartmentBentoScreen.kt, ItemSheets.kt (Màn 3)
    │   └── aiscan/     # AiScanPlaceholderScreen.kt (Cổng CameraX GĐ2)
    ├── data/           # Entities.kt, Dao.kt, Db.kt, Repository.kt, Seed.kt, AppConfig.kt
    ├── domain/         # Freshness.kt, ShelfLife.kt, Models.kt
    ├── notifications/  # ExpiryReminderWorker.kt, ContextualNotificationPrompt.kt
    └── vm/             # FridgeViewModel.kt
```

### Lộ trình 3 Bước Chuyển đổi An toàn cho Antigravity
1. **Bước 1 — Thiết lập Design System Neo-Brutalist & Màn 3 Bento Grid:** Cập nhật `Color.kt`, `Type.kt`, `BrutalistComponents.kt`, xây dựng `CompartmentBentoScreen` (lưới 2 cột) và 4 Bottom Sheet (`ItemDetailSheet`, `QuickAddSheet`, `UrgentItemsSheet`, `CameraAiPreviewSheet`) nối thẳng vào `FridgeViewModel` hiện có.
2. **Bước 2 — Tích hợp SceneView 3D & Màn 1 Thiết kế Tủ (`FridgeSetupScreen`):** Thêm dependency `SceneView`, viết `Fridge3DBuilder` dựng tủ 3D có viền mực + bản lề cửa/ngăn kéo, và hoàn thiện màn hình chọn 6 mẫu tủ, 4 màu vỏ, thêm/bớt ngăn rồi chốt tủ.
3. **Bước 3 — Hoàn thiện Màn 2 Chính (`FridgeHeroScreen`) & Kiểm thử:** Gắn Dashboard đầu trang (lời chào, đồng hồ số, thời tiết, thông báo sắp hết hạn), sân khấu tủ 3D xoay 360° kèm sticker chiếu 2D, FAB `+ Thêm nhanh` và thanh Task Bar có cổng `📷 Quét AI (GĐ2)`.
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

### `app/src/main/res/values/strings.xml` (135 lines)

```xml
﻿<?xml version="1.0" encoding="utf-8"?>
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
    <string name="expiry_packaging_hint">Gợi ý, hãy kiểm tra bao bì</string>
    <string name="opened_toggle_label">Đã mở nắp</string>
    <string name="opened_badge">Đã mở</string>
    <string name="pick_date_label">Chọn ngày</string>
    <string name="recent_items_label">Món gần đây</string>
    <string name="target_compartment_label">Ngăn lưu trữ</string>
    <string name="search_icon_hint">Tìm biểu tượng...</string>
    <string name="action_save">Lưu</string>
    <string name="action_save_and_next">Thêm tiếp</string>
    <string name="action_delete">Xóa món</string>
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

    <string name="home_brand">FRIDGEZERO</string>
    <string name="home_greeting_morning">Chào buổi sáng, Trường!</string>
    <string name="home_greeting_noon">Chào buổi trưa, Trường!</string>
    <string name="home_greeting_afternoon">Chào buổi chiều, Trường!</string>
    <string name="home_greeting_evening">Chào buổi tối, Trường!</string>
    <string name="home_weather_badge">☀️ 28°C · Đà Nẵng</string>
    <string name="home_urgent_badge_fmt">🔔 %1$d sắp hết hạn</string>
    <string name="home_all_fresh_badge">✨ Tất cả đều tươi</string>
    <string name="home_stage_hint">↔ Kéo nghiêng xem tủ · Chạm cửa để mở</string>
    <string name="home_fab_quick_add">+ Thêm nhanh</string>
    <string name="home_tab_fridge">🧊 Tủ lạnh</string>
    <string name="home_tab_scan_ai">📷 Quét AI</string>
    <string name="home_tab_scan_badge">GĐ2</string>
    <string name="home_tab_redesign">⚙️ Đổi dáng</string>
    <string name="sticker_urgent_cd">%1$s, %2$d món, %3$d món sắp hết hạn</string>
    <string name="urgent_sheet_title">CẦN XỬ LÝ SỚM</string>
    <string name="urgent_sheet_subtitle">Các món còn ≤ 3 ngày hoặc đã quá hạn</string>
    <string name="urgent_sheet_empty">Tất cả thực phẩm trong tủ đều đang tươi ngon!</string>
    <string name="urgent_sheet_open_compartment">Mở ngăn →</string>
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

    <string name="reminder_toggle_label">🔔 Nhắc hạn mỗi ngày</string>
    <string name="reminder_toggle_on">Đang bật</string>
    <string name="reminder_toggle_off">Đang tắt</string>
    <string name="reminder_prompt_title">BẬT NHẮC HẠN DÙNG?</string>
    <string name="reminder_prompt_body">FridgeZero sẽ kiểm tra mỗi ngày và nhắc nhẹ khi có món còn ≤ 3 ngày hoặc quá hạn. Không làm phiền khi đồ vẫn tươi.</string>
    <string name="reminder_prompt_confirm">Bật nhắc nhở</string>
    <string name="reminder_prompt_later">Để sau</string>
</resources>
```

### `app/src/main/res/values/themes.xml` (7 lines)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FridgeZero" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">#FAF8F5</item>
    </style>
</resources>
```

### `app/src/main/assets/fridge3d/index.html` (1047 lines)

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
    background: #FAF8F5;
    touch-action: none;
    user-select: none;
    -webkit-user-select: none;
  }
  canvas {
    display: block;
    width: 100%;
    height: 100%;
    background: #FAF8F5;
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

  // Solid surface-matched WebGL renderer with crisp MSAA anti-aliasing.
  var dpr = window.devicePixelRatio || 1;
  var currentSurfaceBg = "#FAF8F5";
  var renderer = new THREE.WebGLRenderer({
    alpha: false,
    antialias: true,
    powerPreference: "high-performance"
  });
  renderer.setClearColor(currentSurfaceBg, 1);
  renderer.setPixelRatio(Math.min(dpr, 2.0));
  renderer.outputEncoding = THREE.sRGBEncoding;
  document.body.appendChild(renderer.domElement);

  // Power-of-two 64x64 tiled dot-grid matching Compose dotGridBackground() (#FAF8F5 + #D6D3CD @ 18dp).
  var DOT_STEP_PX = 18;
  var lastCssW = window.innerWidth || 360;
  var lastCssH = window.innerHeight || 480;
  function createDotGridTexture() {
    var size = 64;
    var canvas = document.createElement("canvas");
    canvas.width = size;
    canvas.height = size;
    var ctx = canvas.getContext("2d");
    ctx.fillStyle = "#FAF8F5";
    ctx.fillRect(0, 0, size, size);
    ctx.fillStyle = "#D6D3CD";
    ctx.beginPath();
    ctx.arc(size / 2, size / 2, (0.72 / DOT_STEP_PX) * size, 0, Math.PI * 2);
    ctx.fill();
    var tex = new THREE.CanvasTexture(canvas);
    tex.wrapS = THREE.RepeatWrapping;
    tex.wrapT = THREE.RepeatWrapping;
    tex.encoding = THREE.sRGBEncoding;
    tex.repeat.set(lastCssW / DOT_STEP_PX, lastCssH / DOT_STEP_PX);
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
  camera.position.set(0, 1.30, 4.18);
  camera.lookAt(0, 1.15, 0);

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
  var handleMat = new THREE.MeshPhysicalMaterial({
    color: 0x1E293B,
    metalness: 0.5,
    roughness: 0.25,
    clearcoat: 0.4,
    clearcoatRoughness: 0.18,
    envMapIntensity: 0.95
  });
  var shellMat = new THREE.MeshPhysicalMaterial({
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

  function updateShellMaterial(colorKey) {
    var spec = SHELL_SPECS[colorKey] || SHELL_SPECS.inox;
    shellMat.color.setHex(spec.color);
    shellMat.metalness = spec.metalness;
    shellMat.roughness = spec.roughness;
    shellMat.clearcoat = spec.clearcoat;
    shellMat.clearcoatRoughness = spec.clearcoatRoughness;
    shellMat.envMapIntensity = spec.envMapIntensity;
    shellMat.needsUpdate = true;
  }

  function rebuildFridge() {
    clearGroup(turntable);
    doorControllers = [];
    raycastTargets = [];
    lastProjectedPayload = "";

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

      var nextBg = cfg.surfaceBg || (state.showDotGrid ? "#FAF8F5" : "#FFFFFF");
      if (nextBg !== currentSurfaceBg) {
        currentSurfaceBg = nextBg;
        document.documentElement.style.backgroundColor = nextBg;
        document.body.style.backgroundColor = nextBg;
        if (renderer.domElement) renderer.domElement.style.backgroundColor = nextBg;
      }
      renderer.setClearColor(nextBg, 1);

      if (state.showDotGrid) {
        dotGridTex.repeat.set(lastCssW / DOT_STEP_PX, lastCssH / DOT_STEP_PX);
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
      return;
    }
    yawVel = 0;
    resetAnim = {
      startTime: performance.now(),
      startYaw: yaw,
      targetYaw: nearestYaw(yaw, restYaw),
      startPitch: pitch
    };
  };

  window.playItemAddedBounce = function () {
    bounceAnim = {
      startTime: performance.now(),
      impulseApplied: false
    };
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
  }

  dom.addEventListener("pointerup", handlePointerEnd);
  dom.addEventListener("pointercancel", handlePointerEnd);

  // Resize observer guarding against zero-size layout passes
  function applyResize(w, h) {
    if (!w || !h || w <= 0 || h <= 0) return;
    console.log("CANVAS_RESIZE", w, h);
    lastCssW = w;
    lastCssH = h;
    if (dotGridTex) {
      dotGridTex.repeat.set(w / DOT_STEP_PX, h / DOT_STEP_PX);
    }
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
    renderer.setSize(w, h, false);
    lastProjectedPayload = "";
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
  applyResize(window.innerWidth || 0, window.innerHeight || 0);

  // Frame loop
  var lastFrameTime = performance.now();
  var tmpVec = new THREE.Vector3();

  function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
  }

  function animate(now) {
    requestAnimationFrame(animate);
    var dt = Math.min(0.05, Math.max(0.001, (now - lastFrameTime) / 1000));
    lastFrameTime = now;

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
      // Spring pitch back to grounded -4.5 deg
      pitch += (DEFAULT_PITCH - pitch) * Math.min(1, dt * 10);
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
  }

  requestAnimationFrame(animate);
})();
</script>
</body>
</html>
```

### `app/src/main/kotlin/com/truong/fridgezero/data/AppConfig.kt` (56 lines)

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

  companion object {
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("has_completed_onboarding")
    private val KEY_EXPIRY_REMINDERS = booleanPreferencesKey("expiry_reminders_enabled")
    private val KEY_NOTIFICATION_PROMPTED = booleanPreferencesKey("notification_prompt_shown")
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

### `app/src/main/kotlin/com/truong/fridgezero/domain/Freshness.kt` (22 lines)

```kotlin
package com.truong.fridgezero.domain

import androidx.compose.ui.graphics.Color

enum class Freshness {
  OK,
  WARN,
  BAD;

  fun color(): Color = when (this) {
    OK -> Color(0xFFA3E635)
    WARN -> Color(0xFFFACC15)
    BAD -> Color(0xFFFB7185)
  }
}

fun freshnessOf(daysLeft: Int): Freshness = when {
  daysLeft <= 1 -> Freshness.BAD
  daysLeft <= 3 -> Freshness.WARN
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

### `app/src/main/kotlin/com/truong/fridgezero/domain/ShelfLife.kt` (79 lines)

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
  val drawerDays: Int? = null,
  val openedFactor: Float = 0.5f
)

interface ShelfLifeRepository {
  fun suggestDays(
    iconKey: String,
    group: FoodGroup,
    zone: StorageZone,
    isOpened: Boolean
  ): Int?
}

/**
 * Configurable shelf-life lookup table.
 * Intentionally empty by default per spec.md §0 & §6: do NOT invent unverified
 * storage day numbers. Add verified Vietnamese food storage rules here when approved;
 * missing entries return null so the user enters or picks the expiry date directly.
 */
object ConfigurableShelfLifeRepository : ShelfLifeRepository {

  private val byKey: Map<String, ShelfLifeRule> = emptyMap()
  private val byGroup: Map<FoodGroup, ShelfLifeRule> = emptyMap()

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
      (baseDays * rule.openedFactor).toInt().coerceAtLeast(1)
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

### `app/src/main/kotlin/com/truong/fridgezero/MainActivity.kt` (385 lines)

```kotlin
package com.truong.fridgezero

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
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
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
                        onToggleOpened = vm::toggleItemOpened,
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

### `app/src/main/kotlin/com/truong/fridgezero/ui/bento/CompartmentBentoScreen.kt` (499 lines)

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
  onToggleOpened: (id: Long) -> Unit,
  onMoveCompartment: (ids: List<Long>, targetCompartmentId: String, newDaysLeft: Int?) -> Unit,
  onMarkStatus: (ids: List<Long>, status: ItemStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember(compartment.id, initialFilter) { mutableStateOf(initialFilter) }
  var showAddSheet by remember { mutableStateOf(false) }
  var detailTarget by remember { mutableStateOf<Pair<DisplayItem, Boolean>?>(null) }

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
            onCardClick = { detailTarget = item to false },
            onDecrement = {
              if (item.quantity <= 1) {
                detailTarget = item to true
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
    val latestItem = compartment.items.firstOrNull { it.primaryId == activeDetail.first.primaryId }
      ?: activeDetail.first
    ItemDetailSheet(
      item = latestItem,
      compartments = allCompartments,
      startWithDeleteReason = activeDetail.second,
      onUpdateItem = onUpdateItem,
      onToggleOpened = onToggleOpened,
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
    Freshness.WARN -> LemonWarn
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

      // Middle: full 2-line Vietnamese item name + group / opened tags
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
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
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
          if (item.isOpened) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ButterSoft)
                .border(1.5.dp, InkBlack, RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Text(
                text = stringResource(R.string.opened_badge),
                fontFamily = BodyFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                color = InkBlack,
                maxLines = 1
              )
            }
          }
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

### `app/src/main/kotlin/com/truong/fridgezero/ui/bento/ItemSheets.kt` (868 lines)

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
  var showDatePicker by remember { mutableStateOf(false) }

  val quickKeys = remember(recentKeys) {
    (recentKeys + DEFAULT_FAVORITE_KEYS).distinct().take(8)
  }
  val quickEntries = remember(quickKeys) {
    quickKeys.mapNotNull { key -> EmojiIconRepository.entries.firstOrNull { it.key == key } }
  }
  val filteredIcons = remember(iconQuery) { EmojiIconRepository.search(iconQuery) }

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
    val selectedComp = compartments.firstOrNull { it.id == selectedCompartmentId }
    val zone = selectedComp?.let(::zoneForCompartment) ?: zoneForCompartment(selectedCompartmentId)
    val suggestedDays = ConfigurableShelfLifeRepository.suggestDays(
      iconKey = entry.key,
      group = entry.group,
      zone = zone,
      isOpened = false
    )
    if (suggestedDays != null) {
      daysLeftText = suggestedDays.toString()
    }
  }

  fun resetForNextEntry() {
    itemName = ""
    iconQuery = ""
    quantity = 1
    daysLeftText = ""
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
            onClick = { daysLeftText = days.toString() },
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
        onValueChange = { raw -> daysLeftText = raw.filter { it.isDigit() || it == '-' }.take(4) },
        placeholder = stringResource(R.string.quick_add_expiry_required_hint),
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth()
      )

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
 * Item Detail, Edit, "Đã mở nắp", Compartment Move & Mandatory Waste-Reason Delete Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
  item: DisplayItem,
  compartments: List<CompartmentUi>,
  startWithDeleteReason: Boolean = false,
  onUpdateItem: (id: Long, name: String, iconKey: String, quantity: Int, daysLeft: Int, unit: String?) -> Unit,
  onToggleOpened: (id: Long) -> Unit,
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
  var showDeleteReasons by remember(item.primaryId, startWithDeleteReason) {
    mutableStateOf(startWithDeleteReason)
  }

  val emoji = (EmojiIconRepository.iconFor(item.iconKey) as? FoodIcon.Emoji)?.glyph ?: "🍽️"
  val badgeColor = when (item.freshness) {
    Freshness.BAD -> CoralUrgent
    Freshness.WARN -> LemonWarn
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
            if (item.isOpened) {
              StickerBadge(text = stringResource(R.string.opened_badge), containerColor = ButterSoft, tiltDegrees = 1.5f)
            }
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

      // "Đã mở nắp" toggle
      NeoBrutalistButton(
        onClick = { onToggleOpened(item.primaryId) },
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (item.isOpened) ButterSoft else SurfaceWhite,
        cornerRadius = 12.dp,
        shadowOffset = 3.dp
      ) {
        Text(
          text = if (item.isOpened) "✓ ${stringResource(R.string.opened_toggle_label)} (${stringResource(R.string.opened_badge)})"
          else "📦 ${stringResource(R.string.opened_toggle_label)}",
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = InkBlack
        )
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

      // Waste-tracking mandatory reason delete flow
      if (!showDeleteReasons) {
        NeoBrutalistButton(
          onClick = { showDeleteReasons = true },
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
      } else {
        NeoBrutalistCard(modifier = Modifier.fillMaxWidth(), containerColor = SurfaceWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = stringResource(R.string.delete_reason_title),
              fontFamily = DisplayFont,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 14.sp,
              color = InkBlack
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              NeoBrutalistButton(
                onClick = {
                  onMarkStatus(item.itemIds, ItemStatus.USED_UP)
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                containerColor = LimeFresh,
                shadowOffset = 3.dp
              ) {
                Text(stringResource(R.string.delete_reason_used_up), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
              }
              NeoBrutalistButton(
                onClick = {
                  onMarkStatus(item.itemIds, ItemStatus.DISCARDED)
                  onDismiss()
                },
                modifier = Modifier.weight(1f),
                containerColor = CoralUrgent,
                shadowOffset = 3.dp
              ) {
                Text(stringResource(R.string.delete_reason_discarded), fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = InkBlack)
              }
            }
          }
        }
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

### `app/src/main/kotlin/com/truong/fridgezero/ui/designer/FridgeDesignerScreen.kt` (442 lines)

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
          StickerBadge(stringResource(R.string.designer_swipe_hint), containerColor = SurfaceWhite, tiltDegrees = 0f)
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

### `app/src/main/kotlin/com/truong/fridgezero/ui/fridge3d/Fridge3DView.kt` (636 lines)

```kotlin
package com.truong.fridgezero.ui.fridge3d

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.Outline
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.truong.fridgezero.data.CompartmentEntity
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "Fridge3D"
private const val PROJECTION_EPS = 0.005f
private const val DEFAULT_SURFACE_BG = "#FAF8F5"

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

  internal var onWelcomeSpinConsumed: () -> Unit = {}
  internal var onDoorClick: ((String) -> Unit)? = null
  internal var onProjectionsUpdated: ((Map<String, DoorProjection>) -> Unit)? = null

  val isVisible: Boolean
    get() = activeToken != null && boundsPx.isValid

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
    activeToken = token
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
  private val onWelcomeSpinFinishedCallback: () -> Unit
) {
  @JavascriptInterface
  fun onDoorTapped(compartmentId: String) = onDoorTappedCallback(compartmentId)

  @JavascriptInterface
  fun onStickersProjected(json: String) = onStickersProjectedCallback(json)

  @JavascriptInterface
  fun onWelcomeSpinFinished() = onWelcomeSpinFinishedCallback()
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
 * Only destroyed when the Activity leaves composition or recovers from renderer crash.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SharedFridge3DWebView(
  controller: Fridge3DStageController,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
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

  val configJson = remember(
    controller.template,
    controller.colorKey,
    controller.compartments,
    controller.openCompartmentId,
    controller.showStickers,
    controller.surfaceBg
  ) {
    buildConfigJson(
      template = controller.template,
      colorKey = ShellColors.normalizeKey(controller.colorKey),
      compartments = controller.compartments,
      openCompartmentId = controller.openCompartmentId,
      showStickers = controller.showStickers,
      surfaceBg = controller.surfaceBg
    )
  }

  val webViewDestroyed = remember(recoveryKey) { AtomicBoolean(false) }

  val webView = remember(context, recoveryKey) {
    WebView.setWebContentsDebuggingEnabled(true)
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
        @Suppress("DEPRECATION")
        allowFileAccess = true
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
        override fun onPageFinished(view: WebView?, url: String?) {
          pageReady = true
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
          Log.e(TAG, "renderer gone, didCrash=${detail?.didCrash()}")
          pageReady = false
          recoveryKey += 1
          return true
        }
      }

      loadUrl("file:///android_asset/fridge3d/index.html")
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
  )
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
  runCatching { AndroidColor.parseColor(hex) }.getOrDefault(0xFFFAF8F5.toInt())

private fun buildConfigJson(
  template: String,
  colorKey: String,
  compartments: List<CompartmentEntity>,
  openCompartmentId: String?,
  showStickers: Boolean,
  surfaceBg: String
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
  return JSONObject().apply {
    put("template", template)
    put("colorKey", colorKey)
    put("openCompartmentId", openCompartmentId ?: JSONObject.NULL)
    put("showStickers", showStickers)
    put("springReturn", showStickers)
    put("surfaceBg", surfaceBg)
    put("showDotGrid", showDotGrid)
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

### `app/src/main/kotlin/com/truong/fridgezero/ui/home/HomeDashboardHeader.kt` (452 lines)

```kotlin
package com.truong.fridgezero.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truong.fridgezero.R
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.DisplayItem
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.ui.icons.EmojiIconRepository
import com.truong.fridgezero.ui.icons.FoodIcon
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
 * Top Home Dashboard (spec §3.2):
 * Row 1: FRIDGEZERO + dynamic greeting | live HH:mm:ss + day/date pill
 * Row 2: Weather badge (☀️ 28°C · Đà Nẵng) | Interactive expiring-items alert badge
 */
@Composable
fun HomeDashboardHeader(
  compartments: List<CompartmentUi>,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
  onOpenCompartment: (compartmentId: String, filterUrgent: Boolean) -> Unit,
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
        .filter { it.freshness == Freshness.WARN || it.freshness == Freshness.BAD }
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
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: Brand + Greeting | Live digital clock
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text = stringResource(R.string.home_brand),
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          letterSpacing = 1.4.sp,
          color = InkBlack.copy(alpha = 0.65f)
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

    // Row 2: Weather badge | Urgent expiring alert badge
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      WeatherBadge()

      val alertBg = when {
        hasBad -> CoralUrgent
        urgentEntries.isNotEmpty() -> LemonWarn
        else -> LimeFresh
      }
      val alertLabel = if (urgentEntries.isEmpty()) {
        stringResource(R.string.home_all_fresh_badge)
      } else {
        stringResource(R.string.home_urgent_badge_fmt, urgentEntries.size)
      }

      NeoBrutalistButton(
        onClick = { showUrgentSheet = true },
        containerColor = alertBg,
        cornerRadius = 10.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = alertLabel,
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack,
          maxLines = 1
        )
      }
    }
  }

  if (showUrgentSheet) {
    UrgentItemsBottomSheet(
      entries = urgentEntries,
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
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

@Composable
private fun WeatherBadge() {
  val shape = RoundedCornerShape(10.dp)
  Box(modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(2.dp, 2.dp)
        .background(InkBlack, shape)
    )
    Box(
      modifier = Modifier
        .background(SkyPale, shape)
        .border(2.dp, InkBlack, shape)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Text(
        text = stringResource(R.string.home_weather_badge),
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = InkBlack,
        maxLines = 1
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrgentItemsBottomSheet(
  entries: List<UrgentEntry>,
  remindersEnabled: Boolean,
  onToggleReminders: () -> Unit,
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
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(310.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(entries, key = { "${it.compartmentId}_${it.item.primaryId}" }) { entry ->
            UrgentItemRow(
              entry = entry,
              onClick = { onOpenCompartment(entry.compartmentId) }
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
  onClick: () -> Unit
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
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
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
      NeoBrutalistButton(
        onClick = onClick,
        containerColor = LimeFresh,
        cornerRadius = 10.dp,
        borderWidth = 2.dp,
        shadowOffset = 2.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
      ) {
        Text(
          text = stringResource(R.string.urgent_sheet_open_compartment),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack
        )
      }
    }
  }
}
```

### `app/src/main/kotlin/com/truong/fridgezero/ui/home/HomeScreen.kt` (503 lines)

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
import com.truong.fridgezero.domain.CompartmentUi
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
import com.truong.fridgezero.ui.bento.QuickAddItemSheet
import com.truong.fridgezero.ui.fridge3d.DoorProjection
import com.truong.fridgezero.ui.fridge3d.Fridge3DStageSlot
import com.truong.fridgezero.ui.theme.BodyFont
import com.truong.fridgezero.ui.theme.ButterPale
import com.truong.fridgezero.ui.theme.CoralUrgent
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
  onOpenAiScan: () -> Unit,
  onOpenDesigner: () -> Unit,
  modifier: Modifier = Modifier
) {
  val fridge = uiState.activeFridge
  val compartments = uiState.compartments
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

  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  fun triggerDoorOpen(compartmentId: String) {
    if (isNavigatingOut) return
    isNavigatingOut = true
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
      remindersEnabled = remindersEnabled,
      onToggleReminders = onToggleReminders,
      onOpenCompartment = onOpenCompartment
    )

    // Center 3D Hero Stage + Floating "+ Thêm nhanh" button
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 4.dp)
      ) {
        StickerBadge(
          text = stringResource(R.string.home_stage_hint),
          containerColor = SurfaceWhite,
          tiltDegrees = 0f
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 24.dp, bottom = 12.dp)
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
          surfaceBg = "#FAF8F5",
          onDoorClick = { compId -> triggerDoorOpen(compId) },
          onProjectionsUpdated = { projections = it },
          modifier = Modifier.fillMaxSize()
        )
        DoorStickersOverlay(
          compartments = compartments,
          projections = projections,
          onStickerClick = { compId -> triggerDoorOpen(compId) }
        )
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

    // Fixed 72.dp Neo-Brutalist Bottom Task Bar
    HomeTaskBar(
      onResetFridgeView = { resetOrbitKey++ },
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
    Freshness.WARN -> LemonWarn
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
        .padding(horizontal = 9.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Box(
        modifier = Modifier
          .size(9.dp)
          .clip(CircleShape)
          .background(dotColor)
          .border(1.5.dp, InkBlack, CircleShape)
      )
      Text(
        text = compartment.name,
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
          .clip(RoundedCornerShape(6.dp))
          .background(InkBlack)
          .padding(horizontal = 5.dp, vertical = 1.dp)
      ) {
        Text(
          text = "${compartment.items.size}",
          fontFamily = MonoFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
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
  onResetFridgeView: () -> Unit,
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
        .height(72.dp)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      NeoBrutalistButton(
        onClick = onResetFridgeView,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = ButterPale,
        cornerRadius = 12.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.home_tab_fridge),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box(modifier = Modifier.weight(1.05f).fillMaxHeight()) {
        NeoBrutalistButton(
          onClick = onOpenAiScan,
          modifier = Modifier.fillMaxSize(),
          containerColor = LilacAccent,
          cornerRadius = 12.dp,
          borderWidth = 2.dp,
          shadowOffset = 3.dp,
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.home_tab_scan_ai),
            fontFamily = BodyFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = InkBlack,
            softWrap = false,
            maxLines = 1
          )
        }
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-6).dp)
        ) {
          StickerBadge(
            text = stringResource(R.string.home_tab_scan_badge),
            containerColor = LemonWarn,
            tiltDegrees = 3f,
            mono = true
          )
        }
      }

      NeoBrutalistButton(
        onClick = onOpenDesigner,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = SurfaceWhite,
        cornerRadius = 12.dp,
        borderWidth = 2.dp,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.home_tab_redesign),
          fontFamily = BodyFont,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 12.sp,
          color = InkBlack,
          softWrap = false,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    Spacer(Modifier.navigationBarsPadding())
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

/** Warm paper with a cached tiled dot grid (spec §1.1: 1.25.dp dots, 18.dp step). */
fun Modifier.dotGridBackground(
  paper: Color = PaperCream,
  dot: Color = DotGridInk,
  step: Dp = 18.dp,
  dotRadius: Dp = 0.625.dp
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

### `app/src/main/kotlin/com/truong/fridgezero/ui/theme/Color.kt` (20 lines)

```kotlin
package com.truong.fridgezero.ui.theme

import androidx.compose.ui.graphics.Color

// spec.md §1.1 — Neo-Brutalist palette
val PaperCream = Color(0xFFFAF8F5)
val DotGridInk = Color(0xFFD6D3CD)
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

### `app/src/main/kotlin/com/truong/fridgezero/vm/FridgeViewModel.kt` (271 lines)

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
import com.truong.fridgezero.domain.Freshness
import com.truong.fridgezero.domain.FridgeUiState
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

### `app/src/test/kotlin/com/truong/fridgezero/domain/LodAndFreshnessTest.kt` (106 lines)

```kotlin
package com.truong.fridgezero.domain

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind
import com.truong.fridgezero.data.FoodItemEntity
import com.truong.fridgezero.data.Seed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LodAndFreshnessTest {

  @Test
  fun freshnessThresholdsMatchSpec() {
    assertEquals(Freshness.BAD, freshnessOf(0))
    assertEquals(Freshness.BAD, freshnessOf(1))
    assertEquals(Freshness.WARN, freshnessOf(2))
    assertEquals(Freshness.WARN, freshnessOf(3))
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
}
```
