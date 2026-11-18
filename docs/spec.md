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