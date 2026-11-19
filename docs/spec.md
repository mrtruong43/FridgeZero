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