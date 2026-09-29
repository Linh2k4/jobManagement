# JobManagement System — Đặc Tả Nghiệp Vụ Chi Tiết

---

## MỤC LỤC

1. [Quản lý Vai trò & Người dùng](#1-quản-lý-vai-trò--người-dùng)
2. [Quản lý Công việc](#2-quản-lý-công-việc)
   - 2.1 Fast Task
   - 2.2 Multi-step Task
   - 2.4 Loại đầu việc
3. [Quản lý Đánh giá & KPI](#3-quản-lý-đánh-giá--kpi)
4. [Quản lý Thông báo & Nhắc việc](#4-quản-lý-thông-báo--nhắc-việc)
5. [Dashboard & Báo cáo](#5-dashboard--báo-cáo)

---

## 1. Quản lý Vai trò & Người dùng

### 1.1 Cấu trúc phân cấp

```
Manager (1)
  └── Team Lead (N)
        └── Member (N)
```

- 1 Manager quản lý nhiều Team Lead
- 1 Team Lead quản lý nhiều Member
- 1 Member thuộc về nhiều Team Lead (N-N)

---

### 1.2 Vai trò: Manager

**Trách nhiệm & quyền hạn:**
- Tạo, chỉnh sửa, xóa tài khoản Team Lead và Member
- Cấu hình các loại công việc (job types) cho toàn hệ thống
- Tạo và phân công Fast Task cho Member
- Cấu hình template Multi-step Task cho Lead sử dụng
- Xem toàn bộ dashboard: tiến độ, KPI của tất cả cấp dưới
- Đánh giá Team Lead và Member
- Nhận cảnh báo khi task quá hạn hoặc thiếu estimate

**KPI của Manager:**
- Được tính dựa trên tỷ lệ hoàn thành công việc của toàn bộ cấp dưới
- KPI = (Số task hoàn thành đúng hạn / Tổng số task có estimate) × 100%
- Bị loại trừ các task không có estimate time

---

### 1.3 Vai trò: Team Lead

**Trách nhiệm & quyền hạn:**
- Nhận nhiệm vụ từ Manager và phân công cho Member
- Tự cấu hình và tạo Multi-step Task ngoài (external jobs) — tự chịu trách nhiệm
- Giám sát tiến độ Member trong nhóm của mình
- Xem dashboard nhóm (chỉ member trực thuộc)
- Đánh giá Member thuộc nhóm
- Tự đánh giá bản thân

**KPI của Team Lead:**
- KPI cá nhân: hoàn thành task được giao đúng hạn
- KPI nhóm: tỷ lệ hoàn thành của member trong nhóm
- KPI tổng = (KPI cá nhân × 40%) + (KPI nhóm × 60%)

---

### 1.4 Vai trò: Member

**Trách nhiệm & quyền hạn:**
- Nhận và thực hiện công việc được giao
- Tự tạo Fast Task và gán với Manager hoặc Team Lead phụ trách
- Cập nhật tiến độ từng task
- Ghi chú cá nhân trên task
- Set reminder cho bản thân
- Tự đánh giá bản thân theo kỳ

**KPI của Member:**
- Tỷ lệ hoàn thành task đúng hạn
- Điểm chất lượng từ đánh giá của Lead và Manager
- Chỉ tính task có estimate time

---

### 1.5 Tính năng Quản lý Người dùng

| Tính năng | Manager | Team Lead | Member |
|-----------|---------|-----------|--------|
| Tạo/xóa user | ✓ | ✗ | ✗ |
| Chỉnh sửa thông tin cá nhân | ✓ | Chỉ bản thân | Chỉ bản thân |
| Phân nhóm Lead ↔ Member | ✓ | ✗ | ✗ |
| Xem danh sách toàn hệ thống | ✓ | Chỉ nhóm | ✗ |
| Đổi mật khẩu | ✓ | ✓ | ✓ |
| Reset mật khẩu cấp dưới | ✓ | ✗ | ✗ |

**Thông tin người dùng bao gồm:**
- Họ tên, email, số điện thoại
- Vai trò (role)
- Nhóm / Team Lead phụ trách
- Ngày bắt đầu, trạng thái (active/inactive)
- Avatar

**Business rules:**
- Không thể xóa user đang có task đang thực hiện (in-progress) — phải chuyển giao task trước
- Deactivate user sẽ ẩn khỏi danh sách giao việc nhưng giữ lại lịch sử
- Một Member có thể thuộc nhiều Team Lead (ví dụ: hỗ trợ chéo nhóm)

---

## 2. Quản lý Công việc

### 2.0 Màn hình Overview — Kanban Board

#### 2.0.1 Mô tả

Màn hình chính của module Quản lý Công việc. Hiển thị toàn bộ **Fast Task** và **Multi-step Task** dưới dạng **Kanban 3 cột**, cho phép theo dõi và điều phối trạng thái công việc trong nhóm.

#### 2.0.2 Ba cột trạng thái

| Cột | Label hiển thị | Trạng thái task tương ứng |
|---|---|---|
| **TODO** | Chờ thực hiện | `PENDING` (đã gán, chưa bắt đầu) |
| **DOING** | Đang thực hiện | `IN_PROGRESS` + `OVERDUE` (đang làm, kể cả quá hạn) |
| **DONE** | Hoàn thành | `DONE` + `CLOSED_LATE` (xong đúng hạn + xong trễ) |

- Task `OVERDUE` hiển thị trong cột DOING với viền đỏ và badge "QUÁ HẠN"
- Task `CLOSED_LATE` hiển thị trong cột DONE với badge màu cam "Trễ"
- Mỗi cột hiển thị số lượng task trong ngoặc: **Đang thực hiện (3)**

#### 2.0.3 Task Card

Mỗi task được hiển thị là một card. Cấu trúc card:

```
┌─────────────────────────────────────────────────────────┐
│ ████ (accent bar 4px — Amber=Fast, Purple=Multi-step)   │
│                                                          │
│  Tên task (bold, tối đa 2 dòng)                         │
│                                                          │
│  [Fast Task] [HIGH]                                      │
│  ▓▓▓▓▓▓▓▓░░░  2/5 bước   (chỉ Multi-step)               │
│                                                          │
│  💬 3  📎 2  ✓ 4/10        [Avatar]  [Xem chi tiết]     │
└─────────────────────────────────────────────────────────┘
```

**Màu accent bar:**
- Fast Task: `#F59E0B` (amber)
- Multi-step Task: `#8B5CF6` (purple)

**Meta icons (dòng dưới cùng):**
- 💬 số comment
- 📎 số file đính kèm
- ✓ progress checklist (nếu có)
- Bước hoàn thành / tổng bước (Multi-step)

**Nút "Xem chi tiết"**: button ở góc phải dưới mỗi card → navigate đến màn hình Task Detail

**Card OVERDUE**: border `#EF4444` đậm + badge "QUÁ HẠN" màu đỏ

#### 2.0.4 Controls & Filter

**Thanh filter (trên content area):**
- Tìm kiếm theo tên task
- Lọc theo: Loại task (Fast/Multi-step) | Người thực hiện | Độ ưu tiên | Kỳ thời gian
- Toggle: "Xem của tôi" / "Xem cả nhóm" (Lead/Manager)

**Tab bar:**
- **Kanban** (mặc định) | Timeline | Danh sách

**Nút tạo task:**
- "+ Tạo Fast Task" → modal tạo nhanh
- "+ Tạo Multi-step Task" → modal tạo chi tiết

**Thêm card nhanh:** Mỗi cột có nút "+ Thêm task" ở cuối → inline form (chỉ nhập tên + nhấn Enter)

#### 2.0.5 Quy tắc business

- **Kéo-thả (Drag & Drop)**: User có thể kéo card giữa các cột để cập nhật trạng thái
  - TODO → DOING: hệ thống set `startedAt = now`, status = `IN_PROGRESS`
  - DOING → DONE: hệ thống set `completedAt = now`, status = `DONE` (hoặc `CLOSED_LATE` nếu quá deadline)
  - DONE → DOING: chỉ Lead/Manager có quyền reopen (set status = `IN_PROGRESS`, log lý do)
- **Multi-step Task**: không thể kéo sang DONE trừ khi tất cả bước đã DONE (hoặc Lead/Manager override)
- **Sắp xếp trong cột**: mặc định theo deadline tăng dần (sớm nhất trên đầu)

#### 2.0.6 API Endpoints

| Hành động | Method | Endpoint | Params | Response |
|---|---|---|---|---|
| Load tất cả task theo Kanban | `GET` | `/tasks/kanban` | `?groupId=&assigneeId=&type=FAST,MULTI_STEP&period=` | `{ data: { todo:[Task], doing:[Task], done:[Task] } }` |
| Tìm kiếm task | `GET` | `/tasks/kanban` | `?q=&type=&priority=&assigneeId=` | `{ data: KanbanData }` |
| Kéo thả → cập nhật trạng thái | `PATCH` | `/tasks/{id}/status` | `{ status: "IN_PROGRESS"\|"DONE"\|"PENDING" }` | `{ data: Task }` |
| Thêm task nhanh trong cột | `POST` | `/tasks/quick` | `{ name, type, status:"PENDING"\|"IN_PROGRESS", assigneeId }` | `{ data: Task }` |

---

### Trạng thái chung của Task

```
Tạo mới (Draft) → Chờ xác nhận (Pending) → Đang thực hiện (In Progress) → Hoàn thành (Done)
                                                    ↓
                                              Quá hạn (Overdue)
                                                    ↓
                                          Đóng quá hạn (Closed Late)
```

**Mô tả trạng thái:**
- **Draft**: Task mới tạo, chưa gán hoặc chưa xác nhận
- **Pending**: Đã gán người thực hiện, chờ bắt đầu
- **In Progress**: Đang được thực hiện, có thể cập nhật tiến độ
- **Done**: Hoàn thành trong hạn
- **Overdue**: Quá deadline nhưng chưa hoàn thành
- **Closed Late**: Hoàn thành sau deadline

**Các trường chung của mọi task:**
- Tiêu đề (bắt buộc)
- Mô tả / nội dung chi tiết
- Loại task (Fast / Multi-step)
- Đầu việc (công văn / cấp phát / hợp đồng / dấu thầu / khác)
- Người tạo
- Người thực hiện
- Estimate time (giờ hoặc ngày) — bắt buộc để tính KPI
- Deadline
- Priority (Low / Medium / High / Urgent)
- Trạng thái
- File đính kèm
- Ghi chú / comment thread

---

### 2.1 Fast Task

**Định nghĩa:** Công việc cá nhân, chỉ liên quan đến một người thực hiện. Có thể chia nhỏ thành các nhóm công việc con (GroupSubtask) và từng việc nhỏ (Subtask).

**Ví dụ thực tế:** Trả lời công văn khẩn, xử lý yêu cầu đột xuất, gửi báo cáo cuối ngày.

**Cấu trúc phân cấp:**
```
FastTask
  └── GroupSubtask (nhóm việc con — tuỳ chọn)
        └── Subtask (việc nhỏ bên trong nhóm)
```

**Người tạo & phân công:**
- Member tự tạo → tự gán với Manager hoặc Team Lead để xác nhận
- Manager tạo → gán cho Member cụ thể
- Team Lead tạo → gán cho Member trong nhóm
- FastTask chỉ có **1 người thực hiện** — Subtask cùng assignee với task cha

**Data model — FastTask:**
```json
{
  "id": "uuid",
  "title": "Trả lời công văn 123",
  "type": "FAST",
  "assigneeId": "uuid",
  "deadline": "2026-07-05T17:00:00",
  "estimateHours": 2,
  "priority": "HIGH",
  "status": "IN_PROGRESS",
  "groupSubtasks": [
    {
      "id": "uuid",
      "name": "Chuẩn bị tài liệu",
      "order": 1,
      "subtasks": [
        {
          "id": "uuid",
          "title": "Thu thập văn bản gốc",
          "status": "DONE",
          "estimateHours": 0.5,
          "deadline": "2026-07-05T12:00:00",
          "note": ""
        },
        {
          "id": "uuid",
          "title": "Soạn thảo nội dung trả lời",
          "status": "IN_PROGRESS",
          "estimateHours": 1,
          "deadline": "2026-07-05T15:00:00",
          "note": ""
        }
      ]
    }
  ]
}
```

**Luồng xử lý:**
```
[Tạo FastTask: tiêu đề + assignee + deadline]
        ↓
[Tuỳ chọn: tạo GroupSubtask và Subtask bên trong]
        ↓
[Người thực hiện nhận task → In Progress]
        ↓
[Tick hoàn thành từng Subtask]
        ↓
[Đánh dấu hoàn thành task tổng → Done]
```

**Business rules:**
- Deadline mặc định là 17:00 ngày làm việc hiện tại
- Subtask không có assignee riêng — thuộc về assignee của FastTask
- Tiến độ tự động = (Subtask DONE / Tổng Subtask) × 100% — nếu không có subtask thì do người dùng tự cập nhật
- GroupSubtask và Subtask đều có thể tạo mới, sửa, xóa bất kỳ lúc nào (popup inline)
- Không có estimate time → task vẫn tạo được nhưng không tính KPI
- Task Overdue sẽ bị highlight đỏ

**Tính năng:**
- Tạo nhanh từ giao diện (quick-add: tiêu đề + assignee)
- Thêm GroupSubtask và Subtask inline với popup nhỏ
- Đính kèm file, ảnh
- Comment thread
- Lịch sử chỉnh sửa

---

### 2.2 Multi-step Task

**Định nghĩa:** Công việc phức tạp liên quan đến **nhiều người**, có người giao việc rõ ràng, cấu trúc phân cấp GroupSubtask → Subtask → Step, hỗ trợ theo dõi mục tiêu (target).

**Ví dụ thực tế:** Quy trình ký hợp đồng, triển khai dự án thi công, đấu thầu gói thầu.

**Cấu trúc phân cấp:**
```
Multi-step Task
  ├── isTarget (task level — mục tiêu tổng)
  └── GroupSubtask (nhóm giai đoạn)
        └── Subtask (hạng mục lớn)
              ├── isTarget (subtask level — mục tiêu riêng)
              └── Step (bước nhỏ — có assignee + deadline riêng)
```

**Data model — MultiStepTask:**
```json
{
  "id": "uuid",
  "title": "Ký hợp đồng XYZ",
  "type": "MULTI_STEP",
  "templateId": "uuid",
  "assignerId": "uuid",        // người giao việc
  "assignDate": "2026-07-01",  // ngày giao việc
  "startDate": "2026-07-03",   // ngày bắt đầu làm
  "deadline": "2026-07-31",
  "isTarget": true,            // task có mục tiêu tổng không?
  "estimateTarget": 100,       // mục tiêu kế hoạch (nếu isTarget)
  "target": 75,                // mục tiêu thực tế hiện tại (nếu isTarget)
  "groupSubtasks": [
    {
      "id": "uuid",
      "name": "Giai đoạn 1 — Chuẩn bị",
      "order": 1,
      "subtasks": [
        {
          "id": "uuid",
          "title": "Soạn thảo hợp đồng",
          "order": 1,
          "status": "IN_PROGRESS",
          "isTarget": true,        // subtask có target riêng
          "estimateTarget": 50,
          "target": 40,
          "steps": [
            {
              "id": "uuid",
              "order": 1,
              "name": "Lập outline nội dung",
              "assigneeId": "uuid",
              "estimateHours": 2,
              "deadline": "2026-07-05T17:00:00",
              "status": "DONE"
            },
            {
              "id": "uuid",
              "order": 2,
              "name": "Viết nội dung chính",
              "assigneeId": "uuid",
              "estimateHours": 4,
              "deadline": "2026-07-08T17:00:00",
              "status": "IN_PROGRESS"
            }
          ]
        }
      ]
    }
  ]
}
```

**Template Multi-step:**
```json
{
  "id": "uuid",
  "name": "Template Hợp đồng",
  "description": "Quy trình chuẩn ký hợp đồng",
  "isTarget": true,
  "groupSubtasks": [
    {
      "name": "Giai đoạn 1 — Chuẩn bị",
      "subtasks": [
        {
          "title": "Soạn thảo hợp đồng",
          "isTarget": false,
          "steps": [
            { "order": 1, "name": "Lập outline", "estimateHours": 2 },
            { "order": 2, "name": "Viết nội dung", "estimateHours": 4 },
            { "order": 3, "name": "Review nội bộ", "estimateHours": 1 }
          ]
        }
      ]
    },
    {
      "name": "Giai đoạn 2 — Ký kết",
      "subtasks": [
        {
          "title": "Trình ký",
          "isTarget": false,
          "steps": [
            { "order": 1, "name": "Gửi duyệt lãnh đạo", "estimateHours": 0.5 },
            { "order": 2, "name": "Nhận chữ ký", "estimateHours": 0.5 }
          ]
        }
      ]
    }
  ]
}
```

**Luồng tạo task từ template:**
```
[Chọn loại Multi-step Task]
        ↓
[Chọn template (danh sách dropdown)]
        ↓
[Hệ thống clone toàn bộ GroupSubtask + Subtask + Step]
        ↓
[Người dùng chỉnh sửa: thêm/xoá Group/Subtask/Step, gán assignee từng Step, đặt deadline]
        ↓
[Nhập assignerId, assignDate, startDate, deadline tổng]
        ↓
[Lưu → Task được kích hoạt]
```

**isTarget — Cách hoạt động:**

| Cấp | Field | Ý nghĩa |
|---|---|---|
| Task | `isTarget = true` | Task có mục tiêu tổng — hiển thị thanh tiến độ target trên header |
| Task | `estimateTarget` | Kế hoạch ban đầu (VD: 100 điểm / 100%) |
| Task | `target` | Tiến độ thực tế đang đặt mục tiêu (do người phụ trách cập nhật) |
| Subtask | `isTarget = true` | Subtask này cần theo dõi target riêng |
| Subtask | `estimateTarget` | Kế hoạch của subtask |
| Subtask | `target` | Thực tế của subtask |

**View Detail — Giao diện:**
```
┌─────────────────────────────────────────────────────────────┐
│ [Header: Tên task | Assigner | Assign date | Start date]    │
│ [Target tổng: ████████░░ 75/100] (nếu isTarget)            │
├─────────────────────────────────────────────────────────────┤
│ ▼ Giai đoạn 1 — Chuẩn bị              [+ Thêm Subtask]     │
│   ▼ Soạn thảo hợp đồng  [Target: 40/50]  [Edit] [Delete]   │
│     ✓ Step 1: Lập outline — Nguyễn A — 05/07  [DONE]       │
│     ● Step 2: Viết nội dung — Trần B — 08/07  [IN PROG]    │
│     ○ Step 3: Review nội bộ — Lê C — 10/07    [PENDING]    │
│     [+ Thêm Step]                                           │
│ [+ Thêm GroupSubtask]                                       │
└─────────────────────────────────────────────────────────────┘
```

**Business rules:**
- Mỗi Step có assignee riêng (có thể là người khác nhau)
- Step không có dependency — tất cả step trong 1 subtask chạy tuần tự theo order
- Subtask DONE khi tất cả Step DONE; task tổng DONE khi tất cả Subtask DONE
- GroupSubtask, Subtask, Step đều có popup tạo mới / sửa / xóa
- Khi xóa GroupSubtask → xóa cascade toàn bộ Subtask + Step bên trong (confirm dialog)
- Template không bị ảnh hưởng khi task clone từ nó được chỉnh sửa
- `assignDate` ≤ `startDate` ≤ `deadline`

**Tính năng:**
- Inline CRUD: popup nhỏ thêm/sửa/xóa GroupSubtask, Subtask, Step
- Tiến độ tự động: Step DONE → cập nhật % Subtask → cập nhật % Task
- Target tracking: cập nhật `target` thực tế tại bất kỳ thời điểm
- Notification khi Step của mình bắt đầu / sắp đến deadline
- Export PDF tiến độ

---

### 2.4 Phân loại theo Đầu việc

#### 2.4.1 Trả lời Công văn

**Mô tả:** Tiếp nhận và xử lý các công văn đến, trả lời trong thời hạn quy định.

**Tính chất:** Có thể là Fast Task (công văn khẩn) hoặc task thông thường có deadline dài hơn.

**Thông tin bắt buộc:**
- Số công văn / mã tham chiếu
- Ngày nhận công văn
- Cơ quan/đơn vị gửi
- Nội dung tóm tắt
- Mức độ khẩn (Thường / Khẩn / Hỏa tốc)
- Deadline trả lời (mặc định theo quy định nội bộ: Thường 5 ngày, Khẩn 2 ngày, Hỏa tốc trong ngày)
- File công văn gốc (đính kèm)

**Kết quả xử lý:**
- Nội dung trả lời / hành động đã thực hiện
- File văn bản trả lời (đính kèm)
- Ngày gửi trả lời thực tế

**Business rules:**
- Hệ thống tự động tính deadline dựa trên mức độ khẩn nếu không nhập tay
- Cảnh báo khi còn 1 ngày đến deadline
- Công văn Hỏa tốc → tự động tạo Fast Task và notify ngay lập tức

---

#### 2.4.2 Trả lời Cấp phát

**Mô tả:** Xử lý các yêu cầu cấp phát tài sản/vật tư. Có 2 loại:

**Loại 1 — Cấp phát bộ (theo đợt):**
Cấp phát số lượng lớn theo đợt định kỳ.

**Loại 2 — Cấp phát thường xuyên:**
Cấp phát nhỏ lẻ theo yêu cầu phát sinh.

**Các bước xử lý (Multi-step):**

| Bước | Tên bước | Người thực hiện | Mô tả |
|------|----------|-----------------|-------|
| 1 | Tiếp nhận yêu cầu | Member | Nhận phiếu/đơn yêu cầu cấp phát, kiểm tra thông tin |
| 2 | Kiểm tra tồn kho | Member | Xác nhận số lượng tồn kho còn đủ |
| 3 | Lập phiếu cấp phát | Member | Soạn phiếu, ghi rõ số lượng, hạng mục |
| 4 | Phê duyệt | Team Lead / Manager | Ký duyệt phiếu |
| 5 | Thực hiện cấp phát | Member | Xuất kho, bàn giao thực tế |
| 6 | Xác nhận nhận hàng | Người nhận | Ký xác nhận đã nhận |
| 7 | Lưu hồ sơ | Member | Lưu phiếu có chữ ký vào hệ thống |

**Thông tin bắt buộc:**
- Mã yêu cầu
- Người/bộ phận yêu cầu
- Danh sách hạng mục + số lượng
- Lý do cấp phát
- Ngày cần nhận

---

#### 2.4.3 Quản lý Hợp đồng

**Mô tả:** Theo dõi vòng đời hợp đồng từ soạn thảo đến lưu trữ và theo dõi thực hiện.

**Các bước xử lý (Multi-step):**

| Bước | Tên bước | Người thực hiện | Mô tả |
|------|----------|-----------------|-------|
| 1 | Soạn thảo | Member | Soạn nội dung hợp đồng theo mẫu |
| 2 | Review nội bộ | Team Lead | Kiểm tra nội dung, điều khoản |
| 3 | Chỉnh sửa (nếu có) | Member | Sửa theo góp ý |
| 4 | Gửi đối tác review | Member | Gửi bản draft cho đối tác |
| 5 | Đàm phán / chỉnh sửa | Member + Đối tác | Thống nhất nội dung |
| 6 | Phê duyệt ký kết | Manager | Ký duyệt nội bộ |
| 7 | Ký kết | Member | Tổ chức ký kết với đối tác |
| 8 | Lưu trữ | Member | Scan và lưu hợp đồng có chữ ký |
| 9 | Theo dõi thực hiện | Member / Lead | Gắn cờ các mốc thực hiện trong hợp đồng |

**Thông tin bắt buộc:**
- Số hợp đồng
- Tên đối tác
- Loại hợp đồng (dịch vụ / mua bán / lao động / khác)
- Giá trị hợp đồng
- Ngày ký kết
- Ngày hết hạn / mốc nghiệm thu
- File hợp đồng (bản scan có chữ ký)

**Business rules:**
- Cảnh báo 30 ngày trước ngày hết hạn hợp đồng
- Cảnh báo khi có mốc nghiệm thu trong 7 ngày tới
- Hợp đồng hết hạn mà chưa gia hạn → cảnh báo đỏ trên dashboard

---

#### 2.4.4 Quản lý Dấu thầu

**Mô tả:** Quản lý quy trình tiếp nhận, xử lý và đóng dấu hồ sơ thầu.

**Các bước xử lý (Multi-step):**

| Bước | Tên bước | Người thực hiện | Mô tả |
|------|----------|-----------------|-------|
| 1 | Tiếp nhận hồ sơ | Member | Nhận hồ sơ thầu, kiểm tra đủ giấy tờ |
| 2 | Kiểm tra hồ sơ | Member / Lead | Kiểm tra tính hợp lệ, đầy đủ |
| 3 | Xử lý nội dung | Member | Xem xét, bổ sung tài liệu nếu thiếu |
| 4 | Trình ký | Lead / Manager | Xin chữ ký phê duyệt |
| 5 | Đóng dấu | Member (có thẩm quyền) | Đóng dấu hồ sơ đã được ký |
| 6 | Trả hồ sơ | Member | Bàn giao hồ sơ cho đơn vị yêu cầu |
| 7 | Lưu bản sao | Member | Lưu bản photocopy vào hệ thống |

**Thông tin bắt buộc:**
- Mã hồ sơ thầu
- Tên gói thầu
- Đơn vị yêu cầu
- Ngày tiếp nhận
- Deadline trả hồ sơ
- Danh sách giấy tờ trong hồ sơ

**Business rules:**
- Deadline trả hồ sơ phải được nhập bắt buộc
- Cảnh báo trước 2 giờ so với deadline trả hồ sơ
- Không thể đóng dấu khi chưa có chữ ký ở bước 4

---

## 3. Quản lý Đánh giá & KPI

### 3.1 Chu kỳ đánh giá

- **Đánh giá tháng**: Diễn ra vào 3-5 ngày làm việc cuối tháng
- **Đánh giá quý**: Tổng hợp từ 3 tháng, có trọng số
- **Đánh giá năm**: Tổng hợp từ 4 quý

### 3.2 Quy trình đánh giá

```
[Hệ thống mở kỳ đánh giá (tự động theo lịch)]
        ↓
[Member tự đánh giá (deadline: ngày N+2)]
        ↓
[Team Lead đánh giá từng member (deadline: ngày N+4)]
        ↓
[Manager review & chốt điểm (deadline: ngày N+5)]
        ↓
[Hệ thống tính KPI tổng hợp]
        ↓
[Gửi kết quả đến từng cá nhân]
```

### 3.3 Độ khó & Khối lượng công việc

#### Vấn đề cần giải quyết

Công thức đơn giản `hoàn thành / tổng task` không phản ánh đúng thực tế:

> **Người A**: 5 task khó, estimate 3-5 ngày/task → hoàn thành 4/5 = **80%**
> **Người B**: 20 task nhỏ, estimate 30 phút/task → hoàn thành 18/20 = **90%**
>
> Kết quả: B có KPI cao hơn dù A đóng góp thực chất nhiều hơn. ❌

Giải pháp: **mỗi task được gán trọng số (weight)** dựa trên độ khó và khối lượng. KPI tính trên tổng điểm có trọng số thay vì đếm đầu task.

---

#### 3.3.1 Độ khó (Difficulty)

Mỗi task có trường `difficulty` — thang 1 đến 5, do **Lead hoặc Manager** thiết lập.

| Mức | Nhãn | Mô tả | Ví dụ |
|---|---|---|---|
| 1 | Rất dễ | Thao tác thuần tuý, không cần phán đoán | Photocopy tài liệu, điền form mẫu |
| 2 | Dễ | Quen thuộc, ít biến số | Lấy văn thư, gửi email xác nhận |
| 3 | Trung bình | Cần xử lý thông tin, có quyết định nhỏ | Soạn thảo công văn, tổng hợp báo cáo |
| 4 | Khó | Phức tạp, nhiều bên liên quan, rủi ro | Đàm phán hợp đồng, xử lý khiếu nại |
| 5 | Rất khó | Đòi hỏi chuyên môn cao, ảnh hưởng lớn | Phê duyệt hồ sơ thầu, xử lý sự cố nghiêm trọng |

**Quy tắc thiết lập difficulty:**
- Mặc định = **3** (trung bình) khi tạo task
- Lead hoặc Manager đặt khi tạo task, hoặc điều chỉnh **trước khi task chuyển sang In Progress**
- Sau khi task In Progress: chỉ **Manager** được điều chỉnh (có ghi log lý do)
- Member **không được tự đặt** difficulty cho task của mình (tránh inflate)
- Đối với **Multi-step Task**: mỗi bước có difficulty riêng; difficulty của task tổng = trung bình có trọng số của các bước

---

#### 3.3.2 Khối lượng (Volume)

Khối lượng đo bằng **`estimateHours`** đã có sẵn — số giờ ước tính để hoàn thành task.

Không tạo thêm trường riêng. Công thức sử dụng estimateHours trực tiếp với giới hạn trần để tránh 1 task khổng lồ thống trị toàn bộ KPI:

```
effectiveHours = min(estimateHours, 8)   // trần 1 ngày làm việc (8h)
```

> Lý do cần trần: task 40 giờ mà không cap thì weight = 40 lần task 1 giờ, méo toàn bộ phân phối điểm.
> Task > 8h thực tế nên được break thành multi-step hoặc nhiều task nhỏ hơn.

---

#### 3.3.3 Trọng số task (Task Weight)

```
taskWeight = difficulty × effectiveHours × priorityMultiplier × typeMultiplier

effectiveHours      = min(estimateHours, 8)

priorityMultiplier:
  LOW    → 0.8
  MEDIUM → 1.0
  HIGH   → 1.2
  URGENT → 1.5

typeMultiplier:
  FAST       → 1.0
  MULTI_STEP → 1.3   // overhead điều phối nhiều bước
```

**Ví dụ minh hoạ:**

| Task | Difficulty | Hours | Priority | Type | Weight |
|---|---|---|---|---|---|
| Soạn công văn thường | 3 | 2 | MEDIUM | FAST | 3×2×1.0×1.0 = **6.0** |
| Đàm phán hợp đồng | 4 | 8 | HIGH | MULTI_STEP | 4×8×1.2×1.3 = **49.9** |
| Xử lý sự cố khẩn | 5 | 4 | URGENT | FAST | 5×4×1.5×1.0 = **30.0** |

---

#### 3.3.4 Điểm hoàn thành từng task (Task Completion Score)

```
taskCompletionScore = taskWeight × timelinessFactor

timelinessFactor:
  DONE (đúng hạn)     → 1.0   // hoàn thành đúng hoặc trước deadline
  CLOSED_LATE (trễ)   → 0.6   // hoàn thành nhưng sau deadline
  OVERDUE (chưa xong) → 0.0   // hết kỳ chưa hoàn thành
```

---

### 3.4 Công thức tính KPI

#### Điểm tự động (system-calculated)

**Chỉ số 1 — Weighted Completion Rate (WCR):**
```
WCR = Σ(taskCompletionScore_i) / Σ(taskWeight_i) × 100

  - Chỉ tính task có estimateHours > 0 (có estimate)
  - Chỉ tính task trong kỳ (theo deadline hoặc completedAt)
  - Kết quả: 0 → 100
```

**Chỉ số 2 — Volume Index (VI):**
```
VI = Σ(effectiveHours_i của task DONE hoặc CLOSED_LATE) / workingHoursInPeriod × 100

  - workingHoursInPeriod = số ngày làm việc trong kỳ × 8
  - Cap ở 120 (cho phép làm 120% — overtime được ghi nhận, không vô hạn)
  - Kết quả: 0 → 120
  - Normalise về 0-100: min(VI, 120) / 120 × 100
```

**Chỉ số 3 — Estimate Accuracy (EA):**
```
EA = (1 - avg(|actualHours_i - estimateHours_i| / estimateHours_i)) × 100

  - Chỉ tính task có cả estimateHours và actualHours
  - Nếu không có actualHours → bỏ qua chỉ số này (không phạt)
  - Capped: sai lệch tối đa tính là 100% (không phạt thêm dù sai nhiều hơn)
  - Kết quả: 0 → 100
```

**Điểm tự động tổng hợp:**
```
Điểm_tự_động = (WCR × 60%) + (VI × 25%) + (EA × 15%)
```

> Trọng số có thể điều chỉnh bởi Manager trong cài đặt hệ thống.

---

#### Điểm thủ công (Lead + Manager nhập)

| Tiêu chí | Thang | Người đánh giá |
|---|---|---|
| Chất lượng công việc | 1-10 | Lead + Manager |
| Tinh thần trách nhiệm | 1-10 | Lead + Manager |
| Phối hợp nhóm | 1-10 | Lead |
| Sáng kiến, chủ động | 1-10 | Manager |
| Kỷ luật, chấp hành | 1-10 | Lead + Manager |

```
Điểm_Lead    = trung bình các tiêu chí Lead đánh giá (thang 10) × 10  → 0-100
Điểm_Manager = trung bình các tiêu chí Manager đánh giá (thang 10) × 10 → 0-100
```

---

#### Công thức KPI cuối

```
KPI_final = (Điểm_tự_động × 40%) + (Điểm_Lead × 35%) + (Điểm_Manager × 25%)

Kết quả: 0 → 100
```

**Phân loại:**

| Điểm | Xếp loại |
|---|---|
| 90 – 100 | Xuất sắc |
| 75 – 89 | Tốt |
| 60 – 74 | Đạt |
| 45 – 59 | Cần cải thiện |
| < 45 | Không đạt |

---

#### Ví dụ so sánh sau khi áp dụng

| | Người A (ít task, khó) | Người B (nhiều task, dễ) |
|---|---|---|
| Số task | 5 (difficulty 4-5, 4-8h, HIGH) | 20 (difficulty 1-2, 0.5h, MEDIUM) |
| Hoàn thành đúng hạn | 4/5 | 18/20 |
| Tổng taskWeight | ~160 | ~18 |
| Σ taskCompletionScore | ~128 | ~16.2 |
| **WCR** | **128/160 = 80%** | **16.2/18 = 90%** |
| Volume Index (VI) | ~100% (đủ 8h/ngày) | ~22% (nhiều task ngắn) |
| **Điểm_tự_động** | **(80×60%)+(100×25%)+(EA×15%) ≈ 78** | **(90×60%)+(22×25%)+(EA×15%) ≈ 64** |

→ Người A có `Điểm_tự_động` cao hơn dù tỷ lệ task hoàn thành thấp hơn. ✓

---

### 3.5 Tự đánh giá (Member)

- Điểm tự cho theo từng tiêu chí thủ công (thang 10)
- Nhận xét tóm tắt công việc trong kỳ
- Điểm mạnh cần phát huy
- Điểm cần cải thiện
- Mục tiêu kỳ tiếp theo
- **Nhận xét về task khó/nặng trong kỳ** (text tự do — để Lead/Manager có context khi đánh giá)

### 3.6 Business rules

- Task không có `estimateHours` → không tính vào KPI tự động (WCR, VI, EA)
- Task không có `difficulty` set → dùng mặc định = 3 (trung bình)
- Member không được tự đặt `difficulty` cho task của mình
- Sau khi task In Progress: chỉ Manager được sửa `difficulty` (ghi log)
- Member phải tự đánh giá trước khi Lead có thể đánh giá
- Lead phải hoàn thành đánh giá trước khi Manager chốt
- Sau khi Manager chốt: điểm locked, chỉ Manager mở lại được (có lý do)
- Member xem KPI của mình; Lead xem KPI nhóm; Manager xem tất cả
- Các trọng số công thức (60/25/15, 40/35/25) cấu hình được bởi Manager trong Admin Settings

---

## 4. Quản lý Thông báo & Nhắc việc

### 4.1 Thông báo hệ thống (System Notifications)

Hệ thống tự động gửi notification trong các tình huống:

| Sự kiện | Đối tượng nhận | Thời điểm |
|---------|---------------|-----------|
| Được gán task mới | Member nhận task | Ngay lập tức |
| Task sắp đến hạn | Người thực hiện | 24h trước deadline |
| Task sắp đến hạn (khẩn) | Người thực hiện | 2h trước deadline |
| Task quá hạn | Người thực hiện + Lead/Manager | Ngay khi quá deadline |
| Bước Multi-step sắp đến lượt | Người phụ trách bước | Khi bước trước Done |
| Task được comment | Người liên quan | Ngay lập tức |
| Kỳ đánh giá mở | Toàn bộ user | Đầu kỳ |
| Deadline tự đánh giá | Member chưa đánh giá | 24h trước hết hạn |
| Hợp đồng sắp hết hạn | Lead + Manager | 30 ngày và 7 ngày trước |

**Kênh thông báo:**
- In-app notification (chuông góc màn hình)
- Email (có thể cấu hình bật/tắt theo loại sự kiện)

### 4.2 Nhắc việc cá nhân (Personal Reminders)

**Tính năng:** Mỗi user có thể tự tạo reminder gắn với task hoặc độc lập.

**Loại reminder:**
- **Reminder theo task**: Gắn vào 1 task cụ thể, nhắc vào thời điểm chỉ định
- **Reminder độc lập**: Ghi chú tự do kèm thời gian nhắc (không cần gắn task)

**Thông tin reminder:**
- Nội dung ghi chú (text tự do)
- Thời điểm nhắc (ngày + giờ cụ thể)
- Lặp lại (không / hàng ngày / hàng tuần)
- Task liên quan (optional)

**Business rules:**
- Mỗi task có thể có nhiều reminder
- Reminder chỉ hiển thị cho người tạo, không ai khác xem được
- Sau khi task Done, reminder liên quan tự động hủy
- Reminder quá hạn (đã qua thời điểm nhắc) vẫn hiển thị trong danh sách đến khi user bỏ qua (dismiss)

### 4.3 Ghi chú cá nhân trên Task

- Mỗi user có thể thêm ghi chú riêng tư trên bất kỳ task nào mình liên quan
- Ghi chú riêng tư chỉ người tạo xem được (khác với comment công khai)
- Comment công khai: tất cả người liên quan đến task đều thấy

---

## 5. Dashboard & Báo cáo

### 5.1 Dashboard theo vai trò

#### Dashboard Member
- Danh sách task của tôi hôm nay (Fast Task + Multi-step Task)
- Task đang in-progress / sắp đến hạn
- KPI cá nhân tháng hiện tại (real-time)
- Reminder sắp tới
- Thống kê tuần: số task done / overdue / pending

#### Dashboard Team Lead
- Tổng quan nhóm: số task theo trạng thái
- Task overdue của từng member trong nhóm
- KPI nhóm tháng hiện tại
- Multi-step Task đang theo dõi (timeline)
- Cảnh báo: task không có estimate, member quá tải

#### Dashboard Manager
- Tổng quan toàn hệ thống
- KPI từng nhóm, từng Lead
- Biểu đồ tiến độ theo tháng/quý
- Top task overdue cần xử lý
- Cảnh báo hệ thống: hợp đồng sắp hết hạn, task không estimate

### 5.2 Cơ chế cảnh báo

**Cảnh báo đỏ (Critical):**
- Task overdue chưa xử lý > 1 ngày
- Hợp đồng hết hạn mà không có hành động
- Member có >3 task overdue cùng lúc
- Member có `totalTaskWeight` kỳ này cao hơn trung bình nhóm > 3 lần (workload mất cân bằng nghiêm trọng)

**Cảnh báo vàng (Warning):**
- Task sắp đến hạn trong 24h
- Task không có estimate time
- Task không có `difficulty` set (dùng mặc định 3 — có thể không phản ánh đúng)
- Hợp đồng còn < 30 ngày
- Member có `totalTaskWeight` cao hơn trung bình nhóm > 1.5 lần (nên xem xét cân bằng)
- Cuối kỳ còn > 20% task chưa có `difficulty` được thiết lập thủ công

**Cảnh báo xanh (Info):**
- Task mới được gán
- Bước Multi-step mới được mở khóa
- Kỳ đánh giá sắp mở

### 5.3 Tính KPI trên Dashboard

**Real-time KPI (cập nhật ngay khi task thay đổi trạng thái):**
```
KPI_realtime = (Số task Done đúng hạn trong kỳ / Tổng task có estimate trong kỳ) × 100%
```

- Task không có estimate: không đưa vào công thức, hiển thị riêng số lượng
- Kỳ tính: theo tháng (reset mỗi đầu tháng)
- Hiển thị trend: so sánh với tháng trước (mũi tên lên/xuống)

### 5.4 Báo cáo xuất khẩu

**Các loại báo cáo có thể xuất:**
- Báo cáo tiến độ công việc theo kỳ (PDF / Excel)
- Báo cáo KPI nhóm / cá nhân (PDF / Excel)
- Danh sách task theo trạng thái (Excel)
- Lịch sử công văn (Excel)
- Báo cáo hợp đồng sắp hết hạn (PDF)

**Quyền xuất báo cáo:**
- Member: chỉ báo cáo của bản thân
- Team Lead: báo cáo nhóm mình
- Manager: toàn bộ

---

### 5.5 Timeline View — Gantt theo Thành viên

#### 5.5.1 Mô tả tính năng

Màn hình Timeline hiển thị toàn bộ đầu việc của các thành viên dưới dạng **Gantt chart**, cho phép nhìn tổng quan tiến độ từng người trên một trục thời gian.

**Quyền truy cập:**
- Team Lead: xem timeline của các member trong nhóm mình
- Manager: xem timeline toàn bộ hệ thống (có thể lọc theo nhóm)
- Member: chỉ xem timeline của bản thân

#### 5.5.2 Chế độ hiển thị mặc định

- **Mặc định**: chỉ hiển thị task **chưa hoàn thành** (status ≠ DONE, CLOSED, CANCELLED)
- **Toggle "Xem tất cả"**: bật để hiển thị cả task đã hoàn thành
- **Khoảng thời gian mặc định**: từ đầu tháng hiện tại đến cuối tháng sau
- **Chế độ xem**: Tuần (mặc định) | Tháng | Quý

#### 5.5.3 Cấu trúc Gantt

```
[Controls: Hôm nay | Tất cả | Tuần▼ | Sắp xếp▼ | Màu▼ | Trường▼ | Chưa lên lịch]
─────────────────────────────────────────────────────────────────────────────────
[   Thành viên   ] │  Tháng 6 / 2026              │  Tháng 7 / 2026
                   │  23–29/6  │ 30/6–6/7 │ 7–13/7 │ 14–20/7 │ ...
─────────────────────────────────────────────────────────────────────────────────
Nguyễn Văn A  [Xem]│    ██Fast████  │           │
               Lead│               │           ████Multi-step══════════════════▶
─────────────────────────────────────────────────────────────────────────────────
Trần Thị B    [Xem]│◄══Multi-step══════════════════════════════════════════════▶
                   │  ███Fast███   │
─────────────────────────────────────────────────────────────────────────────────
```

**Màu sắc thanh Gantt:**

| Màu | Loại / Trạng thái |
|---|---|
| Amber (#F59E0B) | Fast Task |
| Purple (#8B5CF6) | Multi-step Task |
| Green (#22C55E) | Task đã hoàn thành (khi bật "Xem tất cả") |
| Red đậm (#DC2626) | Task Quá hạn (deadline đã qua, chưa xong) |

**Thanh vượt ngoài khoảng hiển thị:**
- Nếu task bắt đầu trước khoảng hiển thị → hiển thị mũi tên ◄ bên trái
- Nếu task kết thúc sau khoảng hiển thị → hiển thị mũi tên ▶ bên phải

**Đường "Hôm nay":** Đường dọc màu primary (#2563EB) đánh dấu ngày hiện tại, luôn hiển thị.

#### 5.5.4 Controls & Filter

| Control | Chức năng |
|---|---|
| Hôm nay | Scroll viewport về ngày hiện tại |
| Tất cả / Chưa hoàn thành | Toggle hiển thị task đã xong |
| Tuần / Tháng / Quý | Thay đổi độ rộng thời gian mỗi cột |
| Sắp xếp ▼ | Sắp xếp member theo: tên, KPI, số task quá hạn |
| Màu ▼ | Tô màu theo: loại task (mặc định) | trạng thái | độ ưu tiên |
| Trường ▼ | Ẩn/hiện các cột thông tin |
| Chưa lên lịch | Hiển thị task không có deadline |

#### 5.5.5 Popup: Full Timeline (click "Xem")

Khi click nút **"Xem"** bên cạnh tên thành viên → mở modal hiển thị toàn bộ timeline của thành viên đó.

**Nội dung popup:**
- Header: Tên thành viên, khoảng thời gian đang xem, nút đóng
- Hiển thị **tất cả** task (kể cả đã hoàn thành) của thành viên đó
- Phân nhóm theo loại task: Fast | Multi-step
- Mỗi task có: tên, loại, trạng thái (badge), deadline, thanh tiến độ
- Click vào tên task → navigate đến trang chi tiết task
- Bộ lọc trong popup: khoảng thời gian (from – to), loại task

**Kích thước modal:** 1200 × 700px, backdrop mờ 50%

---

### 5.6 Members & KPI View — Danh sách Thành viên và KPI

#### 5.6.1 Mô tả tính năng

Màn hình hiển thị **danh sách thành viên** kèm tóm tắt KPI tháng hiện tại, cho phép Lead/Manager theo dõi hiệu suất từng người một cách nhanh chóng.

**Quyền truy cập:**
- Team Lead: xem member trong nhóm mình
- Manager: xem toàn bộ (có thể lọc theo nhóm)

#### 5.6.2 Summary Strip (trên cùng)

| Metric | Hiển thị |
|---|---|
| Tổng thành viên | Số |
| KPI trung bình nhóm | Điểm số (0–100) + trend ↑↓ |
| Thành viên đạt mục tiêu (KPI ≥ 80) | Số / Tổng |
| Thành viên cần chú ý (KPI < 60) | Số — highlight đỏ |
| Kỳ đang xem | Tháng M/YYYY — picker |

#### 5.6.3 Bảng danh sách thành viên

**Columns:**

| Cột | Mô tả |
|---|---|
| STT | Số thứ tự |
| Thành viên | Avatar + Tên + Role (badge: Member/Lead) |
| KPI Tháng | Điểm KPI (0–100), màu XANH ≥80 / VÀNG 60–79 / ĐỎ <60 |
| Fast Task | done / total (VD: 28/30) |
| Multi-step | tasks done / total |
| Tổng hoàn thành | Tổng task done trong kỳ |
| Quá hạn | Số task quá deadline — highlight đỏ nếu > 0 |
| Trạng thái | Badge: ĐẠT / CẢNH BÁO / NGUY HIỂM |
| Chi tiết | Button → mở popup KPI chi tiết |

**Trạng thái KPI (badge):**
- ĐẠT (xanh): KPI ≥ 80
- CẢNH BÁO (vàng): 60 ≤ KPI < 80
- NGUY HIỂM (đỏ): KPI < 60

**Sắp xếp:** Click tiêu đề cột để sắp xếp tăng/giảm.

**Lọc:**
- Nhóm (Group)
- Kỳ đánh giá (tháng/năm)
- Khoảng KPI: Tất cả / Đạt / Cảnh báo / Nguy hiểm
- Tìm kiếm tên thành viên

#### 5.6.4 Popup: KPI Chi tiết (click "Chi tiết")

Khi click nút **"Chi tiết"** → mở modal hiển thị breakdown KPI đầy đủ của thành viên đó cho kỳ đang xem.

**Cấu trúc popup:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Nguyễn Văn A · Member · Nhóm Kỹ thuật · Tháng 6/2026    [×]   │
├──────────────────────────────────────────────────────────────────┤
│  KPI: 87.5 / 100  ↑ +3.2 vs tháng trước                        │
│  Trạng thái: ĐẠT  │  Lead đã chấm: Có  │  Manager duyệt: Chờ   │
├──────────────────────────────────────────────────────────────────┤
│  BREAKDOWN THEO LOẠI TASK                                        │
│  ┌──────────────┬──────────────────┬──────────┬──────────────┐  │
│  │ Loại         │ Kết quả          │ % hoàn   │ Điểm đóng góp│  │
│  ├──────────────┼──────────────────┼──────────┼──────────────┤  │
│  │ Fast Task    │ 28/30 đúng hạn   │   93%    │   28.5/30   │  │
│  │ Multi-step   │ 2/3 hoàn thành   │   67%    │   28.0/50   │  │
│  ├──────────────┴──────────────────┴──────────┤              │  │
│  │ Auto Score (tổng hợp)                     │   74.5/100   │  │
│  │ Lead Score (chấm điểm)                    │   82.0/100   │  │
│  │ Manager Override                          │   —          │  │
│  │ ĐIỂM CUỐI (trọng số tổng hợp)            │   87.5/100   │  │
│  └────────────────────────────────────────────┴──────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  DANH SÁCH TASK TRONG KỲ (có thể mở rộng/thu gọn)              │
│  [Fast Task ▼] [Multi-step ▼]                                    │
│  Tên task │ Loại │ Trạng thái │ Deadline │ Hoàn thành │ Điểm   │
│  ─────────────────────────────────────────────────────────────  │
│  Trả lời CV 123 │ Fast │ DONE │ 30/06 │ 29/06 │ 6.0 pt         │
│  ...                                                             │
└─────────────────────────────────────────────────────────────────┘
```

**Công thức điểm cuối (hiển thị trong popup):**
```
finalKPI = autoScore × 0.5 + leadScore × 0.3 + managerScore × 0.2
```
*(Trọng số này configurable trong §7 — System Config)*

**Kích thước modal:** 900 × 680px, backdrop mờ 50%

**Actions trong popup:**
- Export KPI: Xuất PDF cho thành viên đó
- Điều hướng qua lại: ← thành viên trước / → thành viên tiếp theo

---

*Tài liệu này là nguồn duy nhất (single source of truth) cho nghiệp vụ hệ thống JobManagement.*
*Cập nhật lần cuối: 2026-07-05*

---

## 7. Cấu hình Hệ thống (System Configuration)

### 7.1 Tổng quan phân quyền cấu hình

Có 2 cấp cấu hình: **System-level** (Manager) và **Group-level** (Lead trong phạm vi nhóm).

```
Admin Settings (Manager only)
├── Cấu hình Loại Task
├── Cấu hình Trạng thái Task
├── Cấu hình Độ ưu tiên
├── Cấu hình Tiêu chí Đánh giá
├── Cấu hình Công thức KPI
├── Cấu hình Công thức Khối lượng
├── Cấu hình Dropdown hệ thống (Master Data)
└── Cấu hình Ngưỡng & Quy tắc

Group Settings (Lead — trong phạm vi nhóm mình)
├── Cấu hình Danh mục Đầu việc (đã có mục 6)
├── Cấu hình Độ khó mặc định theo Category
└── Cấu hình Reminder mặc định cho nhóm
```

**Nguyên tắc thiết kế:**
- System config là **template mặc định** cho toàn hệ thống
- Group config **override** system config trong phạm vi nhóm (nếu được phép)
- Khi Manager thay đổi system config: áp dụng cho **dữ liệu mới**, không hồi tố dữ liệu cũ (trừ khi có tùy chọn "áp dụng lại")
- Mọi thay đổi config đều ghi **audit log** (ai sửa, lúc nào, từ giá trị gì sang giá trị gì)

---

### 7.2 Cấu hình Loại Task (Task Type Config)

#### Những gì có thể cấu hình

Hệ thống có 2 loại task cốt lõi (FAST / MULTI_STEP) với **hành vi được fix bởi hệ thống**. Manager cấu hình **thuộc tính hiển thị và quy tắc nghiệp vụ** của từng loại.

| Thuộc tính | Cấu hình được | Mô tả |
|---|---|---|
| Tên hiển thị | ✓ | "Fast Task" → có thể đổi thành "Việc trong ngày" |
| Màu sắc / Icon | ✓ | Tag màu phân biệt loại trên task card |
| Deadline mặc định | ✓ | FAST: mặc định cuối ngày; có thể đổi sang +4h, +8h, custom |
| Ai được tạo | ✓ | FAST: Member tự tạo được không? (default: có) |
| typeMultiplier (KPI) | ✓ | Trọng số loại task trong công thức KPI |
| Giới hạn bước Multi-step | ✓ | Tối đa N bước (default: 20) |
| Cho phép parallel steps | ✓ | Multi-step có cho phép bước chạy song song không |
| effectiveHours cap | ✓ | Trần giờ tính weight (default: 8h) |

**Business rules:**
- Không thể tắt loại task đang có task `IN_PROGRESS` hoặc `PENDING`
- Đổi tên loại task: tên mới hiển thị ngay, data cũ giữ nguyên `type` code (FAST/MULTI_STEP)
- `typeMultiplier` thay đổi chỉ áp dụng cho kỳ KPI tiếp theo, không hồi tố kỳ đã chốt

---

### 7.3 Cấu hình Trạng thái Task (Task Status Config)

#### Trạng thái hệ thống (fixed — không thể xóa)

| Code | Tên mặc định | Màu mặc định |
|---|---|---|
| `DRAFT` | Nháp | Xám |
| `PENDING` | Chờ thực hiện | Xanh dương nhạt |
| `IN_PROGRESS` | Đang thực hiện | Xanh dương |
| `DONE` | Hoàn thành | Xanh lá |
| `OVERDUE` | Quá hạn | Đỏ |
| `CLOSED_LATE` | Hoàn thành muộn | Cam |
| `PAUSED` | Tạm dừng | Tím |

#### Những gì có thể cấu hình

| Thuộc tính | Cấu hình được | Ví dụ |
|---|---|---|
| Tên hiển thị | ✓ | "Đang thực hiện" → "Đang xử lý" |
| Màu sắc | ✓ | Đổi màu tag status |
| Icon | ✓ | Chọn icon đại diện |
| Tên viết tắt (badge) | ✓ | "IP" cho IN_PROGRESS |
| Hiển thị trong filter | ✓ | Ẩn DRAFT khỏi filter mặc định |

**Những gì KHÔNG thể cấu hình:**
- Thứ tự chuyển trạng thái (flow) — fixed bởi hệ thống
- Xóa trạng thái hệ thống
- Thêm trạng thái mới (hành vi gắn với state machine cứng)

---

### 7.4 Cấu hình Độ ưu tiên (Priority Config)

#### Mặc định hệ thống

| Code | Tên | Màu | priorityMultiplier |
|---|---|---|---|
| `LOW` | Thấp | Xám | 0.8 |
| `MEDIUM` | Trung bình | Xanh | 1.0 |
| `HIGH` | Cao | Cam | 1.2 |
| `URGENT` | Khẩn cấp | Đỏ | 1.5 |

#### Những gì có thể cấu hình

| Thuộc tính | Cấu hình được |
|---|---|
| Tên hiển thị | ✓ |
| Màu sắc | ✓ |
| `priorityMultiplier` (KPI weight) | ✓ |
| Thứ tự hiển thị trong dropdown | ✓ |
| Deadline offset mặc định theo priority | ✓ |
| Gửi notification ngay khi tạo (URGENT) | ✓ |
| Bật/Tắt mức priority | ✓ |

**Deadline offset theo priority (cấu hình mới):**

| Priority | Offset mặc định | Ý nghĩa |
|---|---|---|
| LOW | +5 ngày | Deadline = ngày tạo + 5 ngày làm việc |
| MEDIUM | +3 ngày | |
| HIGH | +1 ngày | |
| URGENT | +0 (cuối ngày) | Deadline = 17:00 hôm nay |

→ Khi tạo task, hệ thống tự điền deadline dựa trên priority. User vẫn tự override.

**Business rules:**
- Không thể xóa priority đang có task dùng
- Không thể tắt `URGENT` và `MEDIUM` (bắt buộc phải có ít nhất 2 mức)
- Thêm priority mới: phải có `priorityMultiplier` và màu sắc — không cho lưu nếu thiếu

---

### 7.5 Cấu hình Tiêu chí Đánh giá (Evaluation Criteria Config)

#### Tiêu chí hiện tại (mặc định)

| Code | Tên | Người đánh giá | Trọng số |
|---|---|---|---|
| `WORK_QUALITY` | Chất lượng công việc | Lead + Manager | 25% |
| `RESPONSIBILITY` | Tinh thần trách nhiệm | Lead + Manager | 20% |
| `TEAMWORK` | Phối hợp nhóm | Lead | 20% |
| `INITIATIVE` | Sáng kiến, chủ động | Manager | 15% |
| `DISCIPLINE` | Kỷ luật, chấp hành | Lead + Manager | 20% |

#### Những gì có thể cấu hình (Manager)

**Sửa tiêu chí hiện có:**
- Tên tiêu chí
- Người đánh giá (Lead only / Manager only / Cả hai)
- Trọng số (%) — tổng phải = 100%
- Mô tả hướng dẫn chấm điểm
- Bật / Tắt tiêu chí

**Thêm tiêu chí mới:**
- Tên, người đánh giá, trọng số
- Loại điểm: số (1-10) hoặc mức (Không đạt / Đạt / Tốt / Xuất sắc)
- Tùy chọn: áp dụng từ kỳ nào (kỳ hiện tại hoặc kỳ tiếp theo)

**Business rules:**
- Tổng trọng số các tiêu chí đang active phải = **100%** mới cho lưu
- Không được xóa tiêu chí đang có dữ liệu đánh giá → chỉ tắt (inactive)
- Tiêu chí inactive: ẩn khỏi form đánh giá mới, kết quả cũ vẫn giữ
- Tối thiểu phải có **3 tiêu chí active**
- Thay đổi trọng số không hồi tố kỳ đã chốt

---

### 7.6 Cấu hình Công thức KPI

#### Cấu hình Trọng số tổng (Manager)

```
KPI_final = autoScore × W1 + leadScore × W2 + managerScore × W3
W1 + W2 + W3 = 100%
```

| Tham số | Mặc định | Giới hạn |
|---|---|---|
| W1 — Trọng số điểm tự động | 40% | 20% – 70% |
| W2 — Trọng số điểm Lead | 35% | 10% – 60% |
| W3 — Trọng số điểm Manager | 25% | 10% – 50% |

#### Cấu hình Điểm tự động

```
autoScore = WCR × w_wcr + VI × w_vi + EA × w_ea
w_wcr + w_vi + w_ea = 100%
```

| Tham số | Mặc định | Giới hạn |
|---|---|---|
| w_wcr — Trọng số Weighted Completion Rate | 60% | 40% – 80% |
| w_vi — Trọng số Volume Index | 25% | 10% – 40% |
| w_ea — Trọng số Estimate Accuracy | 15% | 0% – 30% |
| Có tính EA không | Bật | Tắt = w_ea → 0, chia lại cho WCR và VI |

#### Cấu hình Hệ số nhân Độ khó (Difficulty Multiplier)

Mặc định: difficulty là giá trị trực tiếp (1-5). Manager có thể override bằng bảng nhân:

| Difficulty | Label mặc định | Multiplier (cấu hình) |
|---|---|---|
| 1 | Rất dễ | 1.0 |
| 2 | Dễ | 2.0 |
| 3 | Trung bình | 3.0 |
| 4 | Khó | 4.0 |
| 5 | Rất khó | 5.0 |

→ Manager có thể đổi multiplier thành phi tuyến: ví dụ difficulty=5 → multiplier=8 (thưởng mạnh hơn cho task rất khó).
→ Manager cũng có thể đổi **label** cho từng mức độ khó (ví dụ: "Rất khó" → "Chuyên gia").

#### Cấu hình Ngưỡng xếp loại

| Xếp loại | Ngưỡng mặc định | Cấu hình được |
|---|---|---|
| Xuất sắc | ≥ 90 | ✓ |
| Tốt | ≥ 75 | ✓ |
| Đạt | ≥ 60 | ✓ |
| Cần cải thiện | ≥ 45 | ✓ |
| Không đạt | < 45 | ✓ (= ngưỡng "Cần cải thiện") |

**Business rules:**
- Tổng W1+W2+W3 phải = 100% mới lưu được
- Tổng w_wcr+w_vi+w_ea phải = 100%
- Ngưỡng xếp loại phải tăng dần, không được trùng
- Preview ảnh hưởng lên kỳ hiện tại trước khi lưu (xem API)
- Thay đổi chỉ áp dụng cho kỳ **chưa chốt**; kỳ đã `FINALIZED` không bị ảnh hưởng

---

### 7.7 Cấu hình Công thức Khối lượng (Workload Formula Config)

#### Cấu hình Volume Index

```
VI = Σ(effectiveHours_i của task hoàn thành) / workingHoursInPeriod × 100
```

| Tham số | Mặc định | Cấu hình được |
|---|---|---|
| Số giờ làm việc/ngày | 8h | ✓ (VD: 7.5h cho ca rút ngắn) |
| effectiveHours cap per task | 8h | ✓ (VD: 4h, 12h) |
| VI cap (trần cộng điểm) | 120% | ✓ (VD: 100% nếu không muốn thưởng overtime) |
| Tính CLOSED_LATE vào VI không | Có (100%) | ✓ |
| Hệ số CLOSED_LATE trong VI | 0.8 | ✓ (VD: 0.5 = task trễ chỉ tính 50% giờ) |

#### Cấu hình Task Weight

```
taskWeight = difficultyMultiplier × effectiveHours × priorityMultiplier × typeMultiplier
```

| Tham số | Cấu hình ở đâu |
|---|---|
| difficultyMultiplier (1-5 → giá trị) | Mục 7.6 — Difficulty Multiplier |
| effectiveHours cap | Mục này |
| priorityMultiplier | Mục 7.4 — Priority Config |
| typeMultiplier | Mục 7.2 — Task Type Config |

#### Cấu hình Workload Imbalance Alert

| Tham số | Mặc định | Mô tả |
|---|---|---|
| Ngưỡng cảnh báo vàng | 1.5× | Member có taskWeight > 1.5× trung bình nhóm |
| Ngưỡng cảnh báo đỏ | 3.0× | Member có taskWeight > 3× trung bình nhóm |

---

### 7.8 Cấu hình Dropdown hệ thống (Master Data)

Đây là danh sách các **lookup table** động — Manager CRUD trực tiếp, hiển thị trong dropdown khắp hệ thống.

| Dropdown | Dùng ở đâu | Ai quản lý |
|---|---|---|
| Loại công văn | Form tạo task Công văn | Manager |
| Mức độ khẩn công văn | Form tạo task Công văn | Manager |
| Loại hợp đồng | Form tạo task Hợp đồng | Manager |
| Loại cấp phát | Form tạo task Cấp phát | Manager |
| Đơn vị tính (vật tư) | Form cấp phát — danh sách hạng mục | Manager |
| Loại đơn vị / bộ phận | Thông tin user, phân nhóm | Manager |
| Loại thông báo (toggle) | Cài đặt notification | Manager |
| Nhãn Difficulty (1-5) | Hiển thị trên task card, form | Manager |
| Nhãn Priority | Toàn hệ thống | Manager |
| Nhãn Status | Toàn hệ thống | Manager |
| Tiêu chí đánh giá | Form đánh giá | Manager |
| Lý do gia hạn deadline | Dropdown khi gia hạn | Manager |
| Lý do skip bước (Multi-step) | Dropdown khi skip | Manager |

#### Cấu trúc MasterData entry

```json
{
  "id": "uuid",
  "type": "CONG_VAN_TYPE",        // mã nhóm dropdown
  "code": "CV_HANH_CHINH",        // slug — tự sinh, không đổi
  "label": "Công văn hành chính", // nhãn hiển thị
  "description": "...",
  "color": "#4A90E2",             // optional
  "icon": "file-text",            // optional
  "sortOrder": 1,
  "isSystem": false,              // true = entry mặc định, không xóa được
  "status": "ACTIVE",
  "metadata": {},                 // thêm thuộc tính tuỳ loại (VD: defaultDeadlineDays)
  "createdAt": "..."
}
```

**Business rules:**
- Entry có `isSystem = true`: không xóa được, chỉ sửa label/màu/thứ tự
- Entry đang được task reference: chỉ vô hiệu hoá, không xóa cứng
- Vô hiệu hoá: ẩn khỏi dropdown tạo task mới, task cũ vẫn hiển thị đúng
- Thứ tự sắp xếp: drag-and-drop trong admin UI, lưu qua `sortOrder`
- Tối thiểu 1 entry active trong mỗi nhóm dropdown bắt buộc

---

### 7.9 Cấu hình Reminder mặc định cho nhóm (Lead)

Lead cấu hình các mốc nhắc nhở mặc định áp dụng cho **toàn bộ task trong nhóm** — member không cần tự tạo reminder thủ công.

| Tham số | Mặc định | Mô tả |
|---|---|---|
| Nhắc trước deadline | 24h | Áp dụng cho mọi task trong nhóm |
| Nhắc thứ hai (URGENT) | 2h | Thêm reminder thứ 2 cho task URGENT |
| Nhắc task OVERDUE | Ngay lập tức + 8h sáng hôm sau | Nhắc lặp lại khi quá hạn |
| Bật nhắc qua email | Tắt | Lead bật/tắt cho nhóm |

**Business rules:**
- Cấu hình nhóm chỉ tạo reminder **tự động** — member vẫn có thể tạo thêm reminder thủ công
- System config là fallback nếu Lead không cấu hình gì cho nhóm
- Lead không thể tắt reminder hệ thống (ví dụ: OVERDUE notification) — chỉ thêm/sửa mốc

---

### 7.10 Cấu hình Ngưỡng & Quy tắc chung (Manager)

| Tham số | Mặc định | Mô tả |
|---|---|---|
| Số Custom Category tối đa/Lead | 20 | Lead tạo tối đa N category |
| Số Extra Fields tối đa/Category | 10 | |
| Số bước tối đa/Multi-step Task | 20 | |
| Số ngày tối đa gia hạn deadline | 30 ngày | 1 lần gia hạn không quá N ngày |
| Số lần gia hạn tối đa/task | 3 lần | |
| Deadline FAST Task tối đa | Cuối ngày | Không cho đặt deadline FAST Task sang ngày hôm sau |
| Difficulty mặc định khi tạo task | 3 | |
| Estimate mặc định khi tạo Fast Task | 1h | |
| Thời điểm mở kỳ đánh giá (tự động) | Ngày 25 hàng tháng | Hệ thống tự mở kỳ nếu bật auto |
| Thời hạn tự đánh giá (ngày) | 2 | Từ ngày mở kỳ |
| Thời hạn Lead đánh giá (ngày) | 4 | |
| Thời hạn Manager chốt (ngày) | 5 | |
| % task thiếu difficulty cảnh báo | 20% | Cảnh báo khi >20% task không set difficulty |

---

## 6. Quản lý Danh mục Đầu việc (Category Management)

### 6.1 Tổng quan & Phân tích thiết kế

Hệ thống có **2 tầng category**:

```
Tầng 1 — System Category (Manager định nghĩa)
  └── Tầng 2 — Custom Category (Team Lead định nghĩa cho nhóm mình)
```

**Tầng 1 — System Category:**
- Do Manager tạo ra, áp dụng toàn hệ thống
- Có schema `categoryData` riêng với các trường đặc thù (xem mục 2.4)
- Ví dụ: Công văn, Cấp phát, Hợp đồng, Dấu thầu
- Không Lead nào có thể sửa hoặc xóa

**Tầng 2 — Custom Category:**
- Do Team Lead tạo ra, chỉ hiện trong dropdown cho nhóm của Lead đó
- Không có schema cứng — Lead tự định nghĩa các trường bổ sung qua `extraFields`
- Ví dụ: "Họp nội bộ", "Báo cáo định kỳ", "Đào tạo nhân viên"
- Lead chỉ sửa/xóa category do chính mình tạo

**Visibility khi tạo task:**
- Member thấy: System Category + Custom Category của Lead mà mình thuộc về
- Member thuộc nhiều Lead: thấy System + Custom của tất cả Lead đó (không trùng)
- Lead thấy: System Category + Custom Category của chính mình
- Manager thấy: tất cả

---

### 6.2 Cấu trúc dữ liệu Category

```json
{
  "id": "uuid",
  "code": "HOP_NOI_BO",
  "name": "Họp nội bộ",
  "description": "Các buổi họp định kỳ trong nhóm",
  "icon": "calendar",
  "color": "#4A90E2",
  "tier": "CUSTOM",                        // SYSTEM | CUSTOM
  "ownerId": "uuid-of-lead",               // null nếu SYSTEM
  "ownerName": "Nguyen Van Lead",
  "allowedTaskTypes": ["FAST", "MULTI_STEP"],   // loại task được phép dùng category này
  "extraFields": [],                        // field schema do Lead định nghĩa (xem 6.3)
  "sortOrder": 3,
  "status": "ACTIVE",                      // ACTIVE | INACTIVE
  "taskCount": 14,                         // readonly — số task đang dùng
  "createdAt": "2026-06-01T00:00:00Z",
  "updatedAt": "2026-06-30T00:00:00Z"
}
```

---

### 6.3 Extra Fields — Lead định nghĩa trường tuỳ chỉnh

Khi tạo category, Lead có thể khai báo thêm các trường bổ sung để member điền khi tạo task thuộc category đó.

**Loại field hỗ trợ:**

| Field Type | Mô tả | Ví dụ |
|---|---|---|
| `TEXT` | Ô nhập văn bản 1 dòng | "Địa điểm họp" |
| `TEXTAREA` | Ô nhập văn bản nhiều dòng | "Nội dung chương trình" |
| `NUMBER` | Số nguyên hoặc thập phân | "Số người tham gia" |
| `DATE` | Chọn ngày | "Ngày diễn ra" |
| `DATETIME` | Chọn ngày + giờ | "Thời gian bắt đầu" |
| `SELECT` | Dropdown chọn 1 giá trị | "Hình thức: Online / Offline" |
| `MULTISELECT` | Dropdown chọn nhiều giá trị | "Phòng ban tham gia" |
| `CHECKBOX` | Tick box | "Đã chuẩn bị tài liệu?" |

**Cấu trúc 1 extra field:**
```json
{
  "fieldId": "uuid",
  "label": "Địa điểm họp",
  "fieldType": "TEXT",
  "required": true,
  "placeholder": "Ví dụ: Phòng họp A, Tầng 3",
  "defaultValue": null,
  "options": [],              // chỉ dùng với SELECT / MULTISELECT
  "sortOrder": 1,
  "visibleInList": false      // có hiển thị thành cột riêng trên danh sách task không
}
```

**Ví dụ category "Họp nội bộ":**
```json
{
  "name": "Họp nội bộ",
  "extraFields": [
    { "label": "Địa điểm", "fieldType": "TEXT", "required": true },
    { "label": "Hình thức", "fieldType": "SELECT", "required": true,
      "options": ["Online", "Offline", "Hybrid"] },
    { "label": "Số người tham gia (dự kiến)", "fieldType": "NUMBER", "required": false },
    { "label": "Đã gửi lịch mời?", "fieldType": "CHECKBOX", "required": false }
  ]
}
```

**Business rules về Extra Fields:**
- Tối đa 10 extra fields trên 1 category
- Sau khi category đã có task: **không được xóa field** đã tồn tại, chỉ được thêm mới hoặc sửa `label` / `placeholder`
- Field mới thêm vào category đang có task: bắt buộc `required = false` (task cũ không có data field này)
- **Không được đổi `fieldType`** của field đang có data — phải tạo field mới thay thế
- **Không được xóa option trong SELECT/MULTISELECT** khi đã có task chọn option đó

---

### 6.4 Business Rules

**Tạo category:**
- Tên không được trùng trong cùng scope: Lead không được đặt trùng với System Category hoặc trùng với category khác của chính mình
- `code` tự động sinh: uppercase + underscore + suffix số nếu trùng. VD: "Họp nội bộ" → `HOP_NOI_BO`, nếu trùng → `HOP_NOI_BO_2`
- Lead chỉ tạo được Custom Category (tier = CUSTOM)
- Tối đa 20 Custom Category mỗi Lead

**Sửa category:**
- Lead chỉ sửa category do chính mình tạo
- Sửa `name`: cập nhật display name ngay; `code` không thay đổi để tránh ảnh hưởng data
- Sửa `allowedTaskTypes`: không ảnh hưởng task cũ, chỉ áp dụng cho task mới
- Sửa `color` / `icon` / `description` / `sortOrder`: tức thì, không ràng buộc
- Mỗi lần sửa ghi log (ai sửa, lúc nào, sửa field nào)

**Xóa / Vô hiệu hoá:**
- `taskCount > 0` → **chỉ cho phép vô hiệu hoá** (`status = INACTIVE`): ẩn khỏi dropdown khi tạo task mới, task cũ không ảnh hưởng
- `taskCount = 0` → cho phép xóa cứng
- Khi vô hiệu hoá: hệ thống cảnh báo số task đang dùng và yêu cầu xác nhận
- Manager có thể vô hiệu hoá bất kỳ Custom Category nào (kể cả không phải của mình)

**Phân quyền:**

| Hành động | Manager | Lead (owner) | Lead (khác) | Member |
|---|---|---|---|---|
| Xem System Category | ✓ | ✓ | ✓ | ✓ |
| Tạo / Sửa System Category | ✓ | ✗ | ✗ | ✗ |
| Xem Custom Category của nhóm mình | ✓ | ✓ | ✗ | ✓ |
| Xem tất cả Custom Category | ✓ | ✗ | ✗ | ✗ |
| Tạo Custom Category | ✓ | ✓ | — | ✗ |
| Sửa Custom Category | ✓ | Chỉ của mình | ✗ | ✗ |
| Vô hiệu hoá Custom Category | ✓ | Chỉ của mình | ✗ | ✗ |
| Xóa Custom Category (`taskCount=0`) | ✓ | Chỉ của mình | ✗ | ✗ |
| Định nghĩa / Sửa Extra Fields | ✓ | Chỉ của mình | ✗ | ✗ |

---

### 6.5 Luồng xử lý

**Lead tạo category mới:**
```
[Lead vào Settings → Tab "Danh mục đầu việc"]
        ↓
[Nhấn "+ Thêm danh mục"]
        ↓
[Nhập: tên, mô tả, chọn màu, chọn icon]
        ↓
[Chọn loại task được phép dùng]
        ↓
[Tuỳ chọn: Thêm extra fields (form builder, kéo-thả sắp xếp)]
        ↓
[Nhấn "Lưu" → category ACTIVE ngay, hiện trong dropdown nhóm]
```

**Member tạo task với Custom Category:**
```
[Member mở modal Tạo task]
        ↓
[Dropdown "Loại đầu việc" hiện: System Category + Custom của Lead]
        ↓
[Member chọn "Họp nội bộ"]
        ↓
[Form tự động render extra fields đã được Lead định nghĩa]
        ↓
[Member điền extra fields + trường thông thường → Lưu]
        ↓
[Task lưu kèm extraData: { "Địa điểm": "Phòng A", "Hình thức": "Offline", ... }]
```

**Lead vô hiệu hoá category đang có task:**
```
[Lead nhấn "Vô hiệu hoá"]
        ↓
[Hệ thống check taskCount = 14]
        ↓
[Hiện cảnh báo xác nhận:
  "14 task đang dùng danh mục này.
   Danh mục sẽ ẩn khỏi dropdown nhưng task cũ không bị ảnh hưởng.
   Xác nhận?"]
        ↓ [Xác nhận]
[status = INACTIVE → ẩn dropdown → task cũ giữ nguyên]
```

**Lead thêm extra field vào category đang có task:**
```
[Lead mở form sửa category đang có 14 task]
        ↓
[Nhấn "+ Thêm trường" → chọn type, nhập label]
        ↓
[Hệ thống tự động set required = false, hiện cảnh báo:
  "Field mới không thể bắt buộc vì 14 task cũ không có dữ liệu này."]
        ↓
[Nhấn "Lưu" → field thêm vào schema, render cho task mới từ đây]
        ↓
[Task cũ: field mới hiển thị là trống (null), không báo lỗi]
```

---

# API Specification — Chi Tiết Màn Hình & Endpoint

> **Quy ước:**
> - Base URL: `/api/v1`
> - Auth: Bearer JWT token trong header `Authorization`
> - Mọi response thành công trả về `{ "success": true, "data": {...} }`
> - Mọi response lỗi trả về `{ "success": false, "error": { "code": "...", "message": "..." } }`
> - Phân trang: `?page=0&size=20&sort=createdAt,desc`

---

## API 1 — Quản lý Người dùng

### Màn hình: Danh sách người dùng (Manager xem)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load danh sách user | `GET` | `/users` | `?role=&groupId=&status=active&page=&size=` | `{ data: [User], total, page }` |
| Tìm kiếm user | `GET` | `/users` | `?keyword=nguyen&role=MEMBER` | `{ data: [User] }` |
| **Nút "Thêm người dùng"** | `POST` | `/users` | `{ fullName, email, phone, role, teamLeadId?, password }` | `{ data: User }` |
| **Nút "Sửa"** (icon bút) | `GET` | `/users/{id}` | — | `{ data: User }` (load form) |
| **Nút "Lưu"** (trong form sửa) | `PUT` | `/users/{id}` | `{ fullName, phone, role, teamLeadId? }` | `{ data: User }` |
| **Nút "Vô hiệu hoá"** | `PATCH` | `/users/{id}/status` | `{ status: "INACTIVE" }` | `{ data: { id, status } }` |
| **Nút "Kích hoạt lại"** | `PATCH` | `/users/{id}/status` | `{ status: "ACTIVE" }` | `{ data: { id, status } }` |
| **Nút "Reset mật khẩu"** | `POST` | `/users/{id}/reset-password` | — | `{ data: { tempPassword } }` |
| **Nút "Xoá"** | `DELETE` | `/users/{id}` | — | 409 nếu còn task đang chạy |
| **Nút "Phân nhóm"** (gán Lead) | `POST` | `/users/{memberId}/assign-lead` | `{ teamLeadId }` | `{ data: { memberId, teamLeadId } }` |
| **Nút "Gỡ khỏi nhóm"** | `DELETE` | `/users/{memberId}/assign-lead/{teamLeadId}` | — | `{ success: true }` |
| Xem lịch sử hoạt động | `GET` | `/users/{id}/activity-log` | `?from=&to=&page=` | `{ data: [ActivityLog] }` |

**User object:**
```json
{
  "id": "uuid",
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0901234567",
  "role": "MEMBER",          // MANAGER | TEAM_LEAD | MEMBER
  "status": "ACTIVE",        // ACTIVE | INACTIVE
  "teamLeads": [{ "id", "fullName" }],
  "avatarUrl": "...",
  "createdAt": "2026-01-01T00:00:00Z"
}
```

---

## API 2 — Quản lý Task (Unified — 1 màn hình chung)

> Tất cả loại task (Fast / Multi-step) đều dùng chung endpoint `/tasks`.
> `type` là field phân loại, không phải màn hình riêng.
> UI hiển thị tab/filter: **Tất cả | Fast | Multi-step**

---

### 2.1 Màn hình: Danh sách Task

#### Khu vực Filter & Tab

| Nút / Hành động | Method | Endpoint | Params | Response |
|---|---|---|---|---|
| Tab "Tất cả" | `GET` | `/tasks` | `?page=0&size=20&sort=deadline,asc` | `{ data: [Task], total }` |
| Tab "Fast Task" | `GET` | `/tasks` | `?type=FAST` | `{ data: [Task] }` |
| Tab "Multi-step" | `GET` | `/tasks` | `?type=MULTI_STEP` | `{ data: [Task] }` |
| Filter "Trạng thái" (dropdown) | `GET` | `/tasks` | `?status=IN_PROGRESS` | `{ data: [Task] }` |
| Filter "Loại đầu việc" (dropdown) | `GET` | `/tasks` | `?category=CONG_VAN` | `{ data: [Task] }` |
| Filter "Người thực hiện" (dropdown) | `GET` | `/tasks` | `?assigneeId=uuid` | `{ data: [Task] }` |
| Filter "Ngày deadline" (date picker) | `GET` | `/tasks` | `?deadlineFrom=2026-06-30&deadlineTo=2026-06-30` | `{ data: [Task] }` |
| Ô tìm kiếm (search bar) | `GET` | `/tasks` | `?keyword=công+văn` | `{ data: [Task] }` |
| Kết hợp nhiều filter | `GET` | `/tasks` | `?type=FAST&status=OVERDUE&assigneeId=uuid&page=0` | `{ data: [Task] }` |

#### Khu vực Action trên List

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| **Nút "Tạo nhanh"** (quick-add inline) | `POST` | `/tasks/quick` | `{ title, type, assigneeId, estimateHours }` | `{ data: Task }` |
| **Nút "+ Tạo task"** (mở modal form đầy đủ) | — | — | *(mở modal, chọn type trước)* | — |
| Click vào row task | `GET` | `/tasks/{id}` | — | `{ data: TaskDetail }` (mở side panel / màn hình detail) |

---

### 2.2 Modal: Tạo Task (chung cho cả 3 loại)

> Bước 1: Chọn loại task → form thay đổi theo loại

#### Các trường chung (mọi loại)

```json
// POST /tasks
{
  "title": "Tên task",
  "description": "Mô tả chi tiết",
  "type": "FAST",              // FAST | MULTI_STEP
  "category": "CONG_VAN",     // CONG_VAN | CAP_PHAT | HOP_DONG | DAU_THAU | OTHER
  "priority": "HIGH",         // LOW | MEDIUM | HIGH | URGENT
  "assigneeId": "uuid",
  "estimateHours": 2,
  "deadline": "2026-06-30T17:00:00Z",
  "categoryData": {},         // object tuỳ category (xem API 5)

  // Chỉ có khi type = MULTI_STEP
  "templateId": null,         // null = tạo từ đầu, có value = tạo từ template
  "steps": [
    {
      "order": 1,
      "name": "Soạn thảo",
      "description": "...",
      "estimateHours": 4,
      "deadlineOffset": 2,    // số ngày kể từ ngày bắt đầu bước
      "assigneeId": "uuid",
      "dependencyType": "SEQUENTIAL"  // SEQUENTIAL | PARALLEL
    }
  ]
}
```

| Nút / Hành động | Method | Endpoint | Request | Response |
|---|---|---|---|---|
| **Nút "Lưu"** (tạo mới) | `POST` | `/tasks` | *(payload trên)* | `{ data: Task }` |
| **Nút "Chọn template"** (type=MULTI_STEP) | `GET` | `/task-templates` | `?category=` | `{ data: [Template] }` |
| Chọn 1 template → load step mẫu | `GET` | `/task-templates/{id}` | — | `{ data: Template }` |

---

### 2.3 Màn hình / Side Panel: Chi tiết Task

> Cùng 1 màn hình detail dùng cho cả 3 loại. Panel bên phải render thêm section tuỳ theo `type`.

#### Header Actions (hiện với mọi loại task)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| **Nút "Sửa"** (tiêu đề, mô tả, priority, estimate) | `PUT` | `/tasks/{id}` | `{ title?, description?, priority?, estimateHours? }` | `{ data: Task }` |
| **Nút "Gán lại người thực hiện"** | `PATCH` | `/tasks/{id}/assign` | `{ assigneeId }` | `{ data: { id, assignee } }` |
| **Nút "Gia hạn deadline"** *(Manager/Lead only)* | `PATCH` | `/tasks/{id}/extend` | `{ newDeadline, reason }` | `{ data: { id, deadline, extendLog } }` |
| **Nút "Xoá task"** *(Manager/Lead only)* | `DELETE` | `/tasks/{id}` | — | `{ success: true }` |
| **Nút "Export PDF"** | `GET` | `/tasks/{id}/export` | `?format=PDF` | file download |

#### Section: Tiến độ — chỉ hiện khi `type = FAST`

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| **Nút "Bắt đầu"** (PENDING → IN_PROGRESS) | `PATCH` | `/tasks/{id}/start` | — | `{ data: { id, status: "IN_PROGRESS", startedAt } }` |
| **Nút "Hoàn thành"** | `PATCH` | `/tasks/{id}/complete` | `{ resultNote?, actualHours? }` | `{ data: { id, status: "DONE", completedAt } }` |

#### Section: Các bước — chỉ hiện khi `type = MULTI_STEP`

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load danh sách bước | `GET` | `/tasks/{id}/steps` | — | `{ data: [Step] }` |
| Xem Gantt / Timeline các bước | `GET` | `/tasks/{id}/steps/timeline` | — | `{ data: [{ id, name, start, end, status }] }` |
| **Nút "+ Thêm bước"** *(chỉ bước chưa bắt đầu)* | `POST` | `/tasks/{id}/steps` | `{ order, name, description, estimateHours, deadlineOffset, assigneeId, dependencyType }` | `{ data: Step }` |
| **Nút "Sửa bước"** (icon bút trên từng bước) | `PUT` | `/tasks/{id}/steps/{stepId}` | `{ name?, description?, estimateHours?, deadlineOffset? }` | `{ data: Step }` |
| **Nút "Xoá bước"** *(chỉ bước PENDING)* | `DELETE` | `/tasks/{id}/steps/{stepId}` | — | `{ success: true }` |
| **Nút "Gán người"** (trên từng bước) | `PATCH` | `/tasks/{id}/steps/{stepId}/assign` | `{ assigneeId, reason? }` | `{ data: Step }` |
| **Nút "Bắt đầu bước"** *(Member — khi bước được mở khóa)* | `PATCH` | `/tasks/{id}/steps/{stepId}/start` | — | `{ data: { stepId, status: "IN_PROGRESS" } }` |
| **Nút "Hoàn thành bước"** *(Member)* | `PATCH` | `/tasks/{id}/steps/{stepId}/complete` | `{ resultNote?, attachments? }` | `{ data: Step }` → tự động mở bước kế |
| **Nút "Bỏ qua"** *(Skip — Lead/Manager only)* | `PATCH` | `/tasks/{id}/steps/{stepId}/skip` | `{ reason }` | `{ data: { stepId, status: "SKIPPED" } }` |
| Xem lịch sử 1 bước | `GET` | `/tasks/{id}/steps/{stepId}/history` | — | `{ data: [ChangeLog] }` |

#### Section: Trao đổi & File (chung mọi loại)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| **Nút "Đính kèm file"** | `POST` | `/tasks/{id}/attachments` | `multipart/form-data: file` | `{ data: { fileId, fileName, url } }` |
| **Nút "Xoá file"** | `DELETE` | `/tasks/{id}/attachments/{fileId}` | — | `{ success: true }` |
| **Nút "Gửi comment"** | `POST` | `/tasks/{id}/comments` | `{ content, attachments? }` | `{ data: Comment }` |
| Load comments | `GET` | `/tasks/{id}/comments` | `?page=` | `{ data: [Comment] }` |
| **Nút "Xoá comment"** *(của mình)* | `DELETE` | `/tasks/{id}/comments/{commentId}` | — | `{ success: true }` |
| **Nút "Ghi chú riêng tư"** (chỉ mình xem) | `PUT` | `/tasks/{id}/private-notes/me` | `{ content }` | `{ data: PrivateNote }` |
| Xem lịch sử thay đổi task | `GET` | `/tasks/{id}/history` | — | `{ data: [ChangeLog] }` |

---

### 2.4 Màn hình: Quản lý Template (Multi-step — Manager)

> Truy cập từ menu cài đặt hoặc khi tạo task type=MULTI_STEP

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Xem danh sách template | `GET` | `/task-templates` | `?category=` | `{ data: [Template] }` |
| **Nút "Tạo template"** | `POST` | `/task-templates` | `{ name, description, category, steps: [StepDef] }` | `{ data: Template }` |
| **Nút "Sửa template"** | `PUT` | `/task-templates/{id}` | `{ name?, description?, steps? }` | `{ data: Template }` |
| **Nút "Xoá template"** | `DELETE` | `/task-templates/{id}` | — | `{ success: true }` |

---

### Task & Step — Response Objects

**Task object (trong list):**
```json
{
  "id": "uuid",
  "title": "Trả lời công văn 123",
  "type": "FAST",              // FAST | MULTI_STEP
  "category": "CONG_VAN",
  "status": "IN_PROGRESS",    // DRAFT | PENDING | IN_PROGRESS | DONE | OVERDUE | CLOSED_LATE | PAUSED
  "priority": "HIGH",
  "assignee": { "id": "uuid", "fullName": "Nguyen Van A" },
  "creator": { "id": "uuid", "fullName": "Tran Thi B" },
  "estimateHours": 2,
  "actualHours": null,
  "deadline": "2026-06-30T17:00:00Z",
  "hasEstimate": true,
  "difficulty": 3,             // 1-5, mặc định 3, do Lead/Manager set
  "taskWeight": 6.0,           // readonly — difficulty × effectiveHours × priorityMul × typeMul
  "progress": null,            // null với FAST, % với MULTI_STEP
  "commentCount": 3,
  "attachmentCount": 1,
  "createdAt": "2026-06-30T08:00:00Z"
}
```

**TaskDetail object (khi mở chi tiết — bao gồm thêm):**
```json
{
  // ...Task fields trên +
  "description": "...",
  "categoryData": {},          // object tuỳ category
  "attachments": [],
  "steps": [],                 // chỉ có khi type=MULTI_STEP (mỗi step có difficulty riêng)
  "extendLog": [],             // lịch sử gia hạn deadline
  "difficultyLog": [],         // lịch sử thay đổi difficulty (ai đổi, lúc nào, lý do)
  "startedAt": null,
  "completedAt": null
}
```

**Step object:**
```json
{
  "stepId": "uuid",
  "order": 1,
  "name": "Soạn thảo",
  "description": "...",
  "status": "IN_PROGRESS",    // PENDING | IN_PROGRESS | DONE | OVERDUE | SKIPPED
  "assignee": { "id", "fullName" },
  "estimateHours": 4,
  "deadlineOffset": 2,
  "actualDeadline": "2026-07-02T17:00:00Z",
  "dependencyType": "SEQUENTIAL",
  "startedAt": null,
  "completedAt": null,
  "resultNote": null,
  "attachments": []
}
```

---

## API 3 — Quản lý Danh mục Đầu việc (Category)

### Màn hình: Danh sách Category (Settings → Danh mục đầu việc)

> Lead truy cập từ menu Settings. Manager truy cập từ Admin Settings.

| Nút / Hành động | Method | Endpoint | Params / Body | Response |
|---|---|---|---|---|
| Load danh sách (của mình + system) | `GET` | `/categories` | `?tier=&status=ACTIVE` | `{ data: [Category] }` |
| Load tất cả (Manager) | `GET` | `/categories` | `?ownerId=&tier=&status=` | `{ data: [Category] }` |
| Tab "System" | `GET` | `/categories` | `?tier=SYSTEM` | `{ data: [Category] }` |
| Tab "Của tôi" | `GET` | `/categories` | `?tier=CUSTOM&ownerId=me` | `{ data: [Category] }` |
| **Nút "+ Thêm danh mục"** | — | — | mở modal form | — |
| **Nút "Sửa"** (icon bút) | `GET` | `/categories/{id}` | — | `{ data: Category }` (load form) |
| **Nút "Vô hiệu hoá"** | `PATCH` | `/categories/{id}/status` | `{ status: "INACTIVE" }` | `{ data: { id, status, taskCount } }` — 409 kèm `taskCount` nếu cần confirm |
| **Nút "Kích hoạt lại"** | `PATCH` | `/categories/{id}/status` | `{ status: "ACTIVE" }` | `{ data: { id, status } }` |
| **Nút "Xoá"** | `DELETE` | `/categories/{id}` | — | 200 nếu `taskCount=0`; 409 kèm `taskCount` nếu còn task |
| Kéo-thả sắp xếp (drag & drop) | `PATCH` | `/categories/reorder` | `{ orderedIds: ["uuid1","uuid2",...] }` | `{ success: true }` |
| Xem số task đang dùng | `GET` | `/categories/{id}/stats` | — | `{ data: { taskCount, activeTaskCount } }` |

### Modal: Tạo / Sửa Category

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| **Nút "Lưu"** (tạo mới) | `POST` | `/categories` | *(xem Category Request)* | `{ data: Category }` |
| **Nút "Lưu"** (cập nhật) | `PUT` | `/categories/{id}` | `{ name?, description?, color?, icon?, allowedTaskTypes?, sortOrder? }` | `{ data: Category }` |
| Kiểm tra trùng tên (real-time khi nhập) | `GET` | `/categories/check-name` | `?name=Họp+nội+bộ&excludeId=` | `{ data: { available: true } }` |

**Category Request (POST /categories):**
```json
{
  "name": "Họp nội bộ",
  "description": "Các buổi họp định kỳ trong nhóm",
  "icon": "calendar",
  "color": "#4A90E2",
  "allowedTaskTypes": ["FAST", "MULTI_STEP"],
  "extraFields": [
    {
      "label": "Địa điểm",
      "fieldType": "TEXT",
      "required": true,
      "placeholder": "Phòng họp / link online",
      "sortOrder": 1,
      "visibleInList": false
    },
    {
      "label": "Hình thức",
      "fieldType": "SELECT",
      "required": true,
      "options": ["Online", "Offline", "Hybrid"],
      "sortOrder": 2,
      "visibleInList": true
    }
  ]
}
```

### Sub-screen / Tab: Quản lý Extra Fields (trong form sửa Category)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load danh sách field | `GET` | `/categories/{id}/fields` | — | `{ data: [ExtraField] }` |
| **Nút "+ Thêm trường"** | `POST` | `/categories/{id}/fields` | `{ label, fieldType, required, placeholder?, options?, sortOrder, visibleInList }` | `{ data: ExtraField }` — 400 nếu `required=true` và `taskCount>0` |
| **Nút "Sửa trường"** | `PUT` | `/categories/{id}/fields/{fieldId}` | `{ label?, placeholder?, required?, visibleInList? }` | `{ data: ExtraField }` — 400 nếu đổi `fieldType` |
| **Nút "Xoá trường"** | `DELETE` | `/categories/{id}/fields/{fieldId}` | — | 200 nếu `taskCount=0`; 409 nếu field đang có data |
| Kéo-thả sắp xếp field | `PATCH` | `/categories/{id}/fields/reorder` | `{ orderedFieldIds: ["uuid1","uuid2"] }` | `{ success: true }` |
| Thêm option vào SELECT | `POST` | `/categories/{id}/fields/{fieldId}/options` | `{ value: "Hybrid" }` | `{ data: ExtraField }` |
| Xóa option | `DELETE` | `/categories/{id}/fields/{fieldId}/options/{value}` | — | 200 nếu không task nào chọn option này; 409 nếu đang dùng |

### Dropdown Category khi tạo task (gọi từ modal Tạo Task)

| Nút / Hành động | Method | Endpoint | Params | Response |
|---|---|---|---|---|
| Load danh sách category hợp lệ | `GET` | `/categories/available` | `?taskType=FAST` | `{ data: { system: [Category], custom: [Category] } }` |
| Load extra fields khi chọn category | `GET` | `/categories/{id}/fields` | — | `{ data: [ExtraField] }` — FE render form động |
| Validate extra field data trước khi submit | `POST` | `/categories/{id}/fields/validate` | `{ fieldValues: { "fieldId": value } }` | `{ data: { valid: true } }` hoặc `{ errors: [...] }` |

> `/categories/available?taskType=FAST` trả về **chỉ category** có `FAST` trong `allowedTaskTypes` và `status=ACTIVE` — tách riêng `system` và `custom` để FE có thể hiển thị group trong dropdown.

### Extra Data trên Task (khi task đã tạo với custom category)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Xem extra data của task | `GET` | `/tasks/{id}` | — | `task.extraData` trong response |
| Sửa extra data (trong detail task) | `PATCH` | `/tasks/{id}/extra-data` | `{ fieldValues: { "fieldId": value } }` | `{ data: { extraData } }` |

**Task response khi có extraData:**
```json
{
  "id": "uuid",
  "title": "Họp review tháng 6",
  "category": {
    "id": "uuid",
    "code": "HOP_NOI_BO",
    "name": "Họp nội bộ",
    "color": "#4A90E2",
    "tier": "CUSTOM"
  },
  "extraData": {
    "field-uuid-1": "Phòng họp A, Tầng 3",
    "field-uuid-2": "Offline"
  },
  "extraDataResolved": {
    "Địa điểm": "Phòng họp A, Tầng 3",
    "Hình thức": "Offline"
  }
}
```

> `extraData` lưu theo `fieldId` (bền vững khi đổi label). `extraDataResolved` là phiên bản đã map label hiện tại — dùng để hiển thị.

---

## API 5 — Loại đầu việc (Category-specific fields cho System Category)

Các trường bổ sung được gửi kèm trong `POST /tasks` hoặc `POST /multistep-tasks` qua trường `categoryData`.

### 5.1 Công văn (`category: CONG_VAN`)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Tạo task công văn | `POST` | `/tasks` | `{ ...taskBase, categoryData: CongVanData }` | `{ data: Task }` |
| Cập nhật kết quả | `PATCH` | `/tasks/{id}/category-data` | `{ replyContent, replyFileUrl, repliedAt }` | `{ data: Task }` |

**CongVanData:**
```json
{
  "documentCode": "CV-2026-001",
  "receivedDate": "2026-06-30",
  "senderOrg": "Bộ Tài chính",
  "summary": "Yêu cầu báo cáo quý II",
  "urgencyLevel": "KHAN",      // THUONG | KHAN | HOA_TOC
  "replyContent": null,
  "replyFileUrl": null,
  "repliedAt": null
}
```

### 5.2 Cấp phát (`category: CAP_PHAT`)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Tạo task cấp phát | `POST` | `/multistep-tasks` | `{ ...taskBase, categoryData: CapPhatData }` | `{ data: MultistepTask }` |
| Xác nhận tồn kho đủ (Bước 2) | `PATCH` | `/multistep-tasks/{id}/steps/{stepId}/complete` | `{ stockConfirmed: true, note }` | `{ data: Step }` |
| Upload phiếu có chữ ký (Bước 7) | `POST` | `/tasks/{id}/attachments` | `multipart: file` | `{ data: Attachment }` |

**CapPhatData:**
```json
{
  "requestCode": "CP-2026-015",
  "requesterName": "Phòng Kỹ thuật",
  "capPhatType": "BO",         // BO | THUONG_XUYEN
  "items": [
    { "name": "Máy in", "quantity": 2, "unit": "cái" }
  ],
  "requiredDate": "2026-07-05",
  "stockConfirmed": false
}
```

### 5.3 Hợp đồng (`category: HOP_DONG`)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Tạo task hợp đồng | `POST` | `/multistep-tasks` | `{ ...taskBase, categoryData: HopDongData }` | `{ data: MultistepTask }` |
| Cập nhật thông tin sau ký kết | `PATCH` | `/tasks/{id}/category-data` | `{ contractCode, signedDate, expiryDate, signedFileUrl }` | `{ data: Task }` |
| **Nút "Gia hạn hợp đồng"** | `POST` | `/tasks/{id}/contract-extensions` | `{ newExpiryDate, reason }` | `{ data: ContractExtension }` |
| Xem lịch sử gia hạn | `GET` | `/tasks/{id}/contract-extensions` | — | `{ data: [ContractExtension] }` |

**HopDongData:**
```json
{
  "contractCode": "HD-2026-042",
  "partnerName": "Công ty XYZ",
  "contractType": "DICH_VU",   // DICH_VU | MUA_BAN | LAO_DONG | KHAC
  "value": 500000000,
  "signedDate": null,
  "expiryDate": "2027-06-30",
  "signedFileUrl": null,
  "milestones": [
    { "name": "Nghiệm thu giai đoạn 1", "date": "2026-12-31" }
  ]
}
```

### 5.4 Dấu thầu (`category: DAU_THAU`)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Tạo task dấu thầu | `POST` | `/multistep-tasks` | `{ ...taskBase, categoryData: DauThauData }` | `{ data: MultistepTask }` |
| Xác nhận hồ sơ đủ điều kiện (Bước 2) | `PATCH` | `/multistep-tasks/{id}/steps/{stepId}/complete` | `{ eligible: true, note }` | `{ data: Step }` |
| Upload hồ sơ đã đóng dấu (Bước 7) | `POST` | `/tasks/{id}/attachments` | `multipart: file` | `{ data: Attachment }` |

**DauThauData:**
```json
{
  "bidCode": "DT-2026-007",
  "packageName": "Gói thầu mua sắm thiết bị",
  "requesterUnit": "Ban quản lý dự án A",
  "receivedDate": "2026-06-30",
  "returnDeadline": "2026-07-02T10:00:00Z",
  "documentList": ["HSMT", "Bảo lãnh dự thầu", "Năng lực tài chính"],
  "eligible": null
}
```

---

## API 6 — Quản lý Đánh giá & KPI

### Màn hình: Kỳ đánh giá (Manager)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Xem danh sách kỳ đánh giá | `GET` | `/evaluations` | `?year=2026&quarter=&month=` | `{ data: [EvaluationPeriod] }` |
| **Nút "Mở kỳ đánh giá"** | `POST` | `/evaluations` | `{ period: "2026-06", selfDeadline, leadDeadline, managerDeadline }` | `{ data: EvaluationPeriod }` |
| **Nút "Đóng kỳ đánh giá"** | `PATCH` | `/evaluations/{id}/close` | — | `{ data: { id, status: "CLOSED" } }` |
| **Nút "Mở lại"** (sau khi đã chốt) | `PATCH` | `/evaluations/{id}/reopen` | `{ reason }` | `{ data: EvaluationPeriod }` |
| Xem tiến trình nộp đánh giá | `GET` | `/evaluations/{id}/progress` | — | `{ data: { totalUsers, selfDone, leadDone, managerDone } }` |

### Màn hình: Tự đánh giá (Member)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load form tự đánh giá | `GET` | `/evaluations/{id}/self-review/me` | — | `{ data: SelfReview \| null }` |
| **Nút "Lưu nháp"** | `PUT` | `/evaluations/{id}/self-review` | `{ scores: {criteria: score}, summary, strengths, improvements, nextGoals, isDraft: true }` | `{ data: SelfReview }` |
| **Nút "Nộp đánh giá"** | `PUT` | `/evaluations/{id}/self-review` | `{ ...scores, isDraft: false }` | `{ data: SelfReview }` — locked sau khi nộp |

**SelfReview Request:**
```json
{
  "scores": {
    "workQuality": 8,
    "responsibility": 9,
    "teamwork": 7,
    "initiative": 8,
    "discipline": 9
  },
  "summary": "Trong tháng 6, tôi đã hoàn thành...",
  "strengths": "Xử lý công văn nhanh...",
  "improvements": "Cần cải thiện ước lượng thời gian...",
  "nextGoals": "Hoàn thành đào tạo kỹ năng X",
  "isDraft": false
}
```

### Màn hình: Đánh giá member (Team Lead)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load danh sách member cần đánh giá | `GET` | `/evaluations/{id}/lead-reviews/pending` | — | `{ data: [{ member, selfReviewDone, leadReviewDone }] }` |
| Xem tự đánh giá của member | `GET` | `/evaluations/{id}/lead-reviews/{memberId}/self-review` | — | `{ data: SelfReview }` |
| **Nút "Lưu nháp"** | `PUT` | `/evaluations/{id}/lead-reviews/{memberId}` | `{ scores, comment, isDraft: true }` | `{ data: LeadReview }` |
| **Nút "Nộp đánh giá"** | `PUT` | `/evaluations/{id}/lead-reviews/{memberId}` | `{ scores, comment, isDraft: false }` | `{ data: LeadReview }` |

### Màn hình: Chốt điểm (Manager)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load bảng tổng hợp | `GET` | `/evaluations/{id}/summary` | — | `{ data: [{ member, selfScore, leadScore, autoScore, kpiPreview }] }` |
| **Nút "Chỉnh sửa điểm"** (override) | `PATCH` | `/evaluations/{id}/manager-override/{memberId}` | `{ adjustedScore, reason }` | `{ data: EvalResult }` |
| **Nút "Chốt tất cả"** | `POST` | `/evaluations/{id}/finalize` | — | `{ data: { id, status: "FINALIZED", results: [EvalResult] } }` |
| Xem kết quả sau chốt | `GET` | `/evaluations/{id}/results` | `?userId=` | `{ data: [EvalResult] }` |

### Màn hình: Thiết lập trọng số công thức KPI (Manager — Admin Settings)

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load cấu hình trọng số hiện tại | `GET` | `/kpi/config` | — | `{ data: KpiConfig }` |
| **Nút "Lưu cấu hình"** | `PUT` | `/kpi/config` | `{ autoScoreWeight, leadScoreWeight, managerScoreWeight, wcrWeight, viWeight, eaWeight, priorityMultipliers, typeMultipliers }` | `{ data: KpiConfig }` |
| Xem preview ảnh hưởng lên kỳ hiện tại | `GET` | `/kpi/config/preview` | `?period=2026-06` | `{ data: [{ userId, currentKpi, previewKpi, delta }] }` |

**KpiConfig object:**
```json
{
  "autoScoreWeight": 0.40,
  "leadScoreWeight": 0.35,
  "managerScoreWeight": 0.25,
  "wcrWeight": 0.60,
  "viWeight": 0.25,
  "eaWeight": 0.15,
  "priorityMultipliers": {
    "LOW": 0.8, "MEDIUM": 1.0, "HIGH": 1.2, "URGENT": 1.5
  },
  "typeMultipliers": {
    "FAST": 1.0, "MULTI_STEP": 1.3
  },
  "effectiveHoursCap": 8,
  "updatedAt": "2026-06-30T00:00:00Z",
  "updatedBy": "manager-uuid"
}
```

### Endpoint: Difficulty trên Task

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| **Nút "Đặt độ khó"** (khi tạo task) | trong `POST /tasks` | — | `{ ..., "difficulty": 4 }` | *(trong Task response)* |
| **Nút "Sửa độ khó"** (trước In Progress — Lead/Manager) | `PATCH` | `/tasks/{id}/difficulty` | `{ difficulty: 4 }` | `{ data: { id, difficulty, taskWeight } }` |
| **Nút "Sửa độ khó"** (sau In Progress — Manager only) | `PATCH` | `/tasks/{id}/difficulty` | `{ difficulty: 4, reason: "Phức tạp hơn dự kiến" }` | `{ data: { id, difficulty, taskWeight } }` — 403 nếu caller là Lead |
| Xem lịch sử thay đổi độ khó | `GET` | `/tasks/{id}/difficulty-log` | — | `{ data: [{ changedBy, fromValue, toValue, reason, changedAt }] }` |
| **Difficulty cho step** (Multi-step) | `PATCH` | `/tasks/{id}/steps/{stepId}` | `{ difficulty: 3 }` | `{ data: Step }` |

### Widget: KPI realtime (mọi màn hình)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load KPI cá nhân | `GET` | `/users/{id}/kpi` | `?period=2026-06` | `{ data: KpiSummary }` |
| Load KPI nhóm | `GET` | `/groups/{leadId}/kpi` | `?period=2026-06` | `{ data: { lead, members: [KpiSummary] } }` |
| Load KPI toàn hệ thống | `GET` | `/kpi/overview` | `?period=2026-06` | `{ data: { groups: [GroupKpi] } }` |
| **So sánh workload nhóm** (Manager) | `GET` | `/groups/{leadId}/kpi/workload` | `?period=2026-06` | `{ data: [WorkloadComparison] }` |

**KpiSummary:**
```json
{
  "userId": "uuid",
  "period": "2026-06",

  "totalTasks": 24,
  "doneTasks": 20,
  "closedLateTasks": 2,
  "overdueTasks": 2,
  "tasksWithoutEstimate": 1,

  "rawCompletionRate": 83.3,        // doneTasks / totalTasks × 100 (không weight)

  "wcr": 80.0,                      // Weighted Completion Rate
  "volumeIndex": 98.5,              // Volume Index (% giờ làm / giờ kỳ)
  "estimateAccuracy": 72.0,         // Estimate Accuracy
  "autoScore": 78.4,                // WCR×60% + VI×25% + EA×15%

  "totalTaskWeight": 160.0,         // Σ taskWeight của kỳ
  "completedTaskWeight": 128.0,     // Σ taskWeight của task Done/ClosedLate
  "totalEffectiveHours": 98.5,      // Σ effectiveHours đã làm

  "leadScore": null,                // điền sau khi Lead đánh giá
  "managerScore": null,
  "finalKpi": null,                 // điền sau khi Manager chốt

  "rank": "TOT",                    // XUAT_SAC | TOT | DAT | CAN_CAI_THIEN | KHONG_DAT
  "trend": "+5.2"                   // so với kỳ trước (finalKpi)
}
```

**WorkloadComparison (so sánh công bằng trong nhóm):**
```json
{
  "member": { "id": "uuid", "fullName": "Nguyen Van A" },
  "totalTaskWeight": 160.0,
  "avgDifficulty": 4.2,
  "totalEffectiveHours": 98.5,
  "wcr": 80.0,
  "rawCompletionRate": 80.0,
  "autoScore": 78.4,
  "workloadRank": 1             // hạng khối lượng trong nhóm (1 = nặng nhất)
}
```

> `WorkloadComparison` giúp Manager thấy: ai đang gánh nhiều nhất, ai WCR thấp nhưng vì task nặng, ai WCR cao nhưng task toàn nhẹ.

---

## API 7 — Thông báo & Nhắc việc

### Màn hình: Chuông thông báo (header)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load thông báo chưa đọc | `GET` | `/notifications` | `?read=false&page=0&size=10` | `{ data: [Notification], unreadCount }` |
| Load tất cả thông báo | `GET` | `/notifications` | `?page=0&size=20` | `{ data: [Notification] }` |
| **Nút "Đánh dấu đã đọc"** (1 cái) | `PATCH` | `/notifications/{id}/read` | — | `{ data: { id, read: true } }` |
| **Nút "Đọc tất cả"** | `PATCH` | `/notifications/read-all` | — | `{ data: { updatedCount } }` |
| **Nút "Xoá thông báo"** | `DELETE` | `/notifications/{id}` | — | `{ success: true }` |
| Click vào notification | `GET` | (redirect đến task/entity liên quan) | — | — |

### Màn hình: Cài đặt thông báo

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load cài đặt | `GET` | `/notifications/settings` | — | `{ data: NotifSettings }` |
| **Nút "Lưu cài đặt"** | `PUT` | `/notifications/settings` | `{ email: { taskAssigned: true, taskDue: true, ... }, inApp: {...} }` | `{ data: NotifSettings }` |

### Màn hình: Reminder cá nhân

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load danh sách reminder | `GET` | `/reminders` | `?upcoming=true&page=` | `{ data: [Reminder] }` |
| **Nút "Tạo nhắc nhở"** | `POST` | `/reminders` | `{ content, remindAt, repeat?, taskId? }` | `{ data: Reminder }` |
| **Nút "Sửa"** | `PUT` | `/reminders/{id}` | `{ content?, remindAt?, repeat? }` | `{ data: Reminder }` |
| **Nút "Bỏ qua" (Dismiss)** | `PATCH` | `/reminders/{id}/dismiss` | — | `{ data: { id, dismissed: true } }` |
| **Nút "Xoá"** | `DELETE` | `/reminders/{id}` | — | `{ success: true }` |

**Reminder object:**
```json
{
  "id": "uuid",
  "content": "Nhớ gọi điện xác nhận với đối tác",
  "remindAt": "2026-07-01T09:00:00Z",
  "repeat": "NONE",            // NONE | DAILY | WEEKLY
  "taskId": "uuid",            // nullable
  "dismissed": false,
  "createdAt": "..."
}
```

### Ghi chú riêng tư trên Task

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load ghi chú của tôi trên task | `GET` | `/tasks/{id}/private-notes/me` | — | `{ data: PrivateNote \| null }` |
| **Nút "Lưu ghi chú"** | `PUT` | `/tasks/{id}/private-notes/me` | `{ content }` | `{ data: PrivateNote }` |
| **Nút "Xoá ghi chú"** | `DELETE` | `/tasks/{id}/private-notes/me` | — | `{ success: true }` |

---

## API 8 — Dashboard & Báo cáo

### Màn hình: Dashboard (load lần đầu)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load toàn bộ dashboard | `GET` | `/dashboard` | `?role=MEMBER\|TEAM_LEAD\|MANAGER` | `{ data: DashboardData }` |
| Widget: task hôm nay | `GET` | `/dashboard/tasks-today` | — | `{ data: { total, done, overdue, inProgress } }` |
| Widget: KPI tháng này | `GET` | `/dashboard/kpi-current` | — | `{ data: KpiSummary }` |
| Widget: cảnh báo | `GET` | `/dashboard/alerts` | — | `{ data: [Alert] }` |
| Widget: task sắp đến hạn | `GET` | `/dashboard/upcoming-tasks` | `?hours=24` | `{ data: [Task] }` |
| Widget: timeline Multi-step | `GET` | `/dashboard/multistep-overview` | — | `{ data: [{ task, progress, nextStep }] }` |
| Load biểu đồ tiến độ | `GET` | `/dashboard/progress-chart` | `?period=MONTH&from=2026-01&to=2026-06` | `{ data: [{ month, done, overdue, total }] }` |

### Màn hình: Timeline Gantt (§5.5)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load Gantt (chưa hoàn thành) | `GET` | `/dashboard/timeline` | `?status=UNFINISHED&from=2026-06-01&to=2026-07-31&groupId=&view=WEEK` | `{ data: [TimelineRow] }` |
| Load Gantt (tất cả task) | `GET` | `/dashboard/timeline` | `?status=ALL&from=&to=&groupId=` | `{ data: [TimelineRow] }` |
| **Click "Xem" → Timeline popup** | `GET` | `/dashboard/timeline/{userId}` | `?from=2026-06-01&to=2026-07-31&includeCompleted=true` | `{ data: UserTimeline }` |
| Đổi khoảng thời gian (trong popup) | `GET` | `/dashboard/timeline/{userId}` | `?from=&to=&includeCompleted=true` | `{ data: UserTimeline }` |

**TimelineRow object:**
```json
{
  "userId": "uuid",
  "name": "Nguyễn Văn A",
  "role": "MEMBER",
  "groupName": "Nhóm Kỹ thuật",
  "avatar": "url",
  "tasks": [
    {
      "id": "uuid",
      "name": "Trả lời CV 123/2026",
      "type": "FAST",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "startDate": "2026-06-25",
      "deadline": "2026-06-30",
      "completedAt": null
    }
  ]
}
```

**UserTimeline object:**
```json
{
  "user": { "id": "uuid", "name": "...", "role": "MEMBER", "groupName": "..." },
  "period": { "from": "2026-06-01", "to": "2026-07-31" },
  "tasks": [
    {
      "id": "uuid", "name": "...", "type": "FAST|MULTI_STEP",
      "status": "PENDING|IN_PROGRESS|DONE|OVERDUE|CLOSED_LATE",
      "startDate": "...", "deadline": "...", "completedAt": "...",
      "priority": "LOW|MEDIUM|HIGH|URGENT",
      "kpiContribution": 6.0
    }
  ],
  "summary": { "total": 30, "done": 28, "overdue": 2, "inProgress": 0 }
}
```

### Màn hình: Members KPI List (§5.6)

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| Load danh sách thành viên + KPI | `GET` | `/dashboard/members-kpi` | `?period=2026-06&groupId=&status=ALL&sort=kpi&page=0&size=20` | `{ data: [MemberKpiSummary], meta: SummaryMeta }` |
| Tìm kiếm thành viên | `GET` | `/dashboard/members-kpi` | `?period=&q=Nguyen&groupId=` | `{ data: [MemberKpiSummary], meta }` |
| **Click "Chi tiết" → KPI popup** | `GET` | `/dashboard/members-kpi/{userId}` | `?period=2026-06` | `{ data: MemberKpiDetail }` |
| Export KPI cá nhân (trong popup) | `GET` | `/reports/kpi/export` | `?userId=&period=&format=PDF` | file download |
| Chuyển thành viên trước/sau (popup) | `GET` | `/dashboard/members-kpi/{userId}` | `?period=2026-06` | `{ data: MemberKpiDetail }` |

**MemberKpiSummary object:**
```json
{
  "userId": "uuid",
  "name": "Nguyễn Văn A",
  "role": "MEMBER",
  "groupName": "Nhóm Kỹ thuật",
  "avatar": "url",
  "kpiScore": 87.5,
  "kpiTrend": +3.2,
  "kpiStatus": "GOOD",
  "fastTask":      { "assigned": 30, "doneOnTime": 28, "doneLate": 1, "notDone": 1 },
  "multiStepTask": { "total": 3, "done": 2, "completionRate": 0.67 },
  "totalDone": 48,
  "totalOverdue": 2
}
```

**MemberKpiDetail object** (extends MemberKpiSummary):
```json
{
  "...": "all MemberKpiSummary fields",
  "kpiBreakdown": {
    "autoScore": 74.5,
    "leadScore": 82.0,
    "managerOverride": null,
    "finalScore": 87.5,
    "weights": { "auto": 0.5, "lead": 0.3, "manager": 0.2 },
    "fastScore":      28.5,
    "multiStepScore": 28.0
  },
  "taskList": [
    {
      "taskId": "uuid", "name": "Trả lời CV 123", "type": "FAST",
      "status": "DONE", "deadline": "2026-06-30", "completedAt": "2026-06-29",
      "taskWeight": 6.0, "timelinessFactor": 1.0, "kpiContribution": 6.0
    }
  ],
  "period": { "from": "2026-06-01", "to": "2026-06-30" },
  "evalStatus": "LEAD_SCORED",
  "prevKpiScore": 84.3
}
```

**SummaryMeta object:**
```json
{
  "totalMembers": 8,
  "avgKpi": 78.4,
  "membersAboveTarget": 5,
  "membersBelowWarning": 1,
  "period": "2026-06"
}
```

**Alert object:**
```json
{
  "id": "uuid",
  "level": "CRITICAL",         // CRITICAL | WARNING | INFO
  "type": "TASK_OVERDUE",      // TASK_OVERDUE | CONTRACT_EXPIRING | NO_ESTIMATE | MEMBER_OVERLOADED | WORKLOAD_IMBALANCE | DIFFICULTY_NOT_SET
  "message": "3 task đã quá hạn chưa xử lý",
  "entityId": "uuid",
  "entityType": "TASK",
  "actionUrl": "/tasks/uuid",
  "createdAt": "..."
}
```

### Màn hình: Báo cáo & Xuất khẩu

| Nút / Hành động | Method | Endpoint | Request Body / Params | Response |
|---|---|---|---|---|
| **Nút "Xuất báo cáo tiến độ"** | `GET` | `/reports/progress/export` | `?from=&to=&groupId=&format=PDF\|EXCEL` | file download |
| **Nút "Xuất báo cáo KPI"** | `GET` | `/reports/kpi/export` | `?period=2026-06&userId=&format=PDF\|EXCEL` | file download |
| **Nút "Xuất danh sách task"** | `GET` | `/reports/tasks/export` | `?status=&category=&from=&to=&format=EXCEL` | file download |
| **Nút "Xuất lịch sử công văn"** | `GET` | `/reports/cong-van/export` | `?from=&to=&format=EXCEL` | file download |
| **Nút "Xuất hợp đồng sắp hết hạn"** | `GET` | `/reports/contracts/expiring/export` | `?withinDays=30&format=PDF` | file download |

---

## Tổng hợp Endpoint

| Module | Endpoint chính | Ghi chú |
|--------|---------------|---------|
| Người dùng | 11 | CRUD + assign + reset password |
| **Task (Unified)** | **~28** | List + CRUD + start/complete + attach + comment + history |
| — Multi-step: steps | +9 | Sub-resource của `/tasks/{id}/steps` |
| Template Multi-step | 4 | CRUD template |
| Category Data (công văn / cấp phát / hợp đồng / dấu thầu) | 8 | Patch category-data + sub-actions |
| Đánh giá & KPI | 14 | Kỳ đánh giá + self/lead/manager review + KPI query |
| Thông báo & Reminder | 14 | Notification + settings + reminder + private note |
| Dashboard & Báo cáo | 10 | Dashboard widgets + export |
| **Tổng** | **~104 endpoints** | |

---

## API 9 — Cấu hình Hệ thống (System Configuration)

> Tất cả endpoint dưới đây yêu cầu role `MANAGER` trừ khi ghi rõ Lead.
> Mọi `PUT` đều ghi audit log tự động.

---

### 9.1 Admin Settings — Trang chính

| Nút / Hành động | Method | Endpoint | Params | Response |
|---|---|---|---|---|
| Load toàn bộ config hiện tại | `GET` | `/config` | — | `{ data: SystemConfig }` (object lớn gộp tất cả) |
| Xem audit log thay đổi config | `GET` | `/config/audit-log` | `?from=&to=&section=&page=` | `{ data: [AuditEntry] }` |

---

### 9.2 Cấu hình Loại Task

#### Màn hình: Danh sách loại task

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load cấu hình 3 loại task | `GET` | `/config/task-types` | — | `{ data: [TaskTypeConfig] }` |
| **Nút "Sửa"** (mở form) | `GET` | `/config/task-types/{code}` | — | `{ data: TaskTypeConfig }` |
| **Nút "Lưu"** | `PUT` | `/config/task-types/{code}` | *(xem TaskTypeConfig Request)* | `{ data: TaskTypeConfig }` |
| **Nút "Bật / Tắt"** | `PATCH` | `/config/task-types/{code}/status` | `{ enabled: false }` | 409 nếu còn task đang chạy |

**TaskTypeConfig Request:**
```json
{
  "code": "FAST",
  "displayName": "Việc trong ngày",
  "icon": "zap",
  "color": "#FF6B35",
  "enabled": true,
  "typeMultiplier": 1.0,
  "defaultDeadlineOffset": "END_OF_DAY",  // END_OF_DAY | +Xh | +Xd
  "canMemberCreate": true,
  "maxSteps": null,                        // chỉ MULTI_STEP
  "allowParallelSteps": false,             // chỉ MULTI_STEP
  "effectiveHoursCap": 8
}
```

---

### 9.3 Cấu hình Trạng thái Task

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load danh sách status | `GET` | `/config/statuses` | — | `{ data: [StatusConfig] }` |
| **Nút "Sửa"** (tên, màu, icon, badge) | `PUT` | `/config/statuses/{code}` | `{ displayName, color, icon, badgeLabel, showInFilter }` | `{ data: StatusConfig }` |
| **Nút "Bật / Ẩn trong filter"** | `PATCH` | `/config/statuses/{code}` | `{ showInFilter: false }` | `{ data: StatusConfig }` |

> Không có POST / DELETE — status là fixed, chỉ sửa hiển thị.

---

### 9.4 Cấu hình Độ ưu tiên

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load danh sách priority | `GET` | `/config/priorities` | — | `{ data: [PriorityConfig] }` |
| **Nút "+ Thêm mức ưu tiên"** | `POST` | `/config/priorities` | `{ code, displayName, color, priorityMultiplier, deadlineOffsetDays, autoNotify, sortOrder }` | `{ data: PriorityConfig }` |
| **Nút "Sửa"** | `PUT` | `/config/priorities/{code}` | *(các field trên)* | `{ data: PriorityConfig }` |
| **Nút "Bật / Tắt"** | `PATCH` | `/config/priorities/{code}/status` | `{ enabled: false }` | 409 nếu đang có task dùng và không có fallback |
| Kéo-thả sắp xếp | `PATCH` | `/config/priorities/reorder` | `{ orderedCodes: ["URGENT","HIGH","MEDIUM","LOW"] }` | `{ success: true }` |

**PriorityConfig object:**
```json
{
  "code": "HIGH",
  "displayName": "Cao",
  "color": "#FF8C00",
  "priorityMultiplier": 1.2,
  "deadlineOffsetDays": 1,    // deadline mặc định = ngày tạo + 1 ngày làm việc
  "autoNotify": false,        // gửi notification ngay khi tạo task với priority này
  "sortOrder": 2,
  "enabled": true,
  "isSystem": true,           // URGENT, HIGH, MEDIUM, LOW là system — không xóa được
  "taskCount": 145
}
```

---

### 9.5 Cấu hình Tiêu chí Đánh giá

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load danh sách tiêu chí | `GET` | `/config/eval-criteria` | — | `{ data: [Criterion] }` |
| **Nút "+ Thêm tiêu chí"** | `POST` | `/config/eval-criteria` | `{ name, description, evaluators, weight, scoreType, applyFromPeriod }` | `{ data: Criterion }` |
| **Nút "Sửa"** | `PUT` | `/config/eval-criteria/{id}` | *(các field trên trừ applyFromPeriod)* | `{ data: Criterion }` |
| **Nút "Vô hiệu hoá"** | `PATCH` | `/config/eval-criteria/{id}/status` | `{ status: "INACTIVE" }` | 400 nếu < 3 tiêu chí còn active |
| Kéo-thả sắp xếp | `PATCH` | `/config/eval-criteria/reorder` | `{ orderedIds: [] }` | `{ success: true }` |
| Validate tổng trọng số | `GET` | `/config/eval-criteria/validate` | — | `{ data: { totalWeight: 100, valid: true } }` |

**Criterion object:**
```json
{
  "id": "uuid",
  "code": "WORK_QUALITY",
  "name": "Chất lượng công việc",
  "description": "Đánh giá mức độ chính xác, đầy đủ và chất lượng output",
  "evaluators": ["LEAD", "MANAGER"],  // LEAD | MANAGER | BOTH
  "weight": 25,                       // % — tổng active phải = 100
  "scoreType": "NUMBER",              // NUMBER (1-10) | LEVEL (4 mức)
  "levels": null,                     // chỉ khi scoreType=LEVEL: ["Không đạt","Đạt","Tốt","Xuất sắc"]
  "status": "ACTIVE",
  "isSystem": true,
  "applyFromPeriod": "2026-07"        // áp dụng từ kỳ nào
}
```

---

### 9.6 Cấu hình Công thức KPI

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load cấu hình công thức | `GET` | `/config/kpi-formula` | — | `{ data: KpiFormulaConfig }` |
| **Nút "Lưu"** | `PUT` | `/config/kpi-formula` | *(xem KpiFormulaConfig)* | `{ data: KpiFormulaConfig }` |
| **Nút "Preview ảnh hưởng"** (trước khi lưu) | `POST` | `/config/kpi-formula/preview` | *(KpiFormulaConfig mới)* + `{ period: "2026-06" }` | `{ data: [{ userId, fullName, currentKpi, previewKpi, delta }] }` |
| **Nút "Reset về mặc định"** | `DELETE` | `/config/kpi-formula` | — | `{ data: KpiFormulaConfig }` (default values) |
| Xem lịch sử thay đổi | `GET` | `/config/kpi-formula/history` | `?page=` | `{ data: [{ changedAt, changedBy, snapshot }] }` |

**KpiFormulaConfig:**
```json
{
  "autoScoreWeight": 40,
  "leadScoreWeight": 35,
  "managerScoreWeight": 25,

  "wcrWeight": 60,
  "viWeight": 25,
  "eaWeight": 15,
  "includeEA": true,

  "difficultyMultipliers": {
    "1": 1.0,
    "2": 2.0,
    "3": 3.0,
    "4": 4.5,
    "5": 8.0
  },

  "rankThresholds": {
    "XUAT_SAC": 90,
    "TOT": 75,
    "DAT": 60,
    "CAN_CAI_THIEN": 45
  },

  "updatedAt": "2026-06-30T00:00:00Z",
  "updatedBy": { "id": "uuid", "fullName": "Manager" }
}
```

---

### 9.7 Cấu hình Công thức Khối lượng

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load cấu hình | `GET` | `/config/workload-formula` | — | `{ data: WorkloadFormulaConfig }` |
| **Nút "Lưu"** | `PUT` | `/config/workload-formula` | *(xem WorkloadFormulaConfig)* | `{ data: WorkloadFormulaConfig }` |
| **Nút "Preview"** | `POST` | `/config/workload-formula/preview` | *(config mới)* + `{ period: "2026-06" }` | `{ data: [{ userId, currentVI, previewVI }] }` |

**WorkloadFormulaConfig:**
```json
{
  "workingHoursPerDay": 8,
  "effectiveHoursCap": 8,
  "viCap": 120,
  "includeClosedLateInVI": true,
  "closedLateViFactor": 0.8,
  "workloadAlertYellowMultiple": 1.5,
  "workloadAlertRedMultiple": 3.0
}
```

---

### 9.8 Cấu hình Difficulty Labels

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load nhãn difficulty | `GET` | `/config/difficulty-labels` | — | `{ data: [DifficultyLabel] }` |
| **Nút "Sửa nhãn"** | `PUT` | `/config/difficulty-labels/{level}` | `{ label, description, color, multiplier }` | `{ data: DifficultyLabel }` |

**DifficultyLabel:**
```json
{
  "level": 4,
  "label": "Khó",
  "description": "Nhiều bên liên quan, rủi ro cao, cần kinh nghiệm",
  "color": "#FF8C00",
  "multiplier": 4.5
}
```

---

### 9.9 Cấu hình Dropdown Hệ thống (Master Data)

#### Màn hình: Danh sách nhóm dropdown

| Nút / Hành động | Method | Endpoint | Params | Response |
|---|---|---|---|---|
| Load tất cả nhóm dropdown | `GET` | `/config/master-data` | — | `{ data: [{ type, label, entryCount, description }] }` |
| Load entries của 1 nhóm | `GET` | `/config/master-data/{type}` | `?status=ACTIVE` | `{ data: [MasterDataEntry] }` |
| **Nút "+ Thêm giá trị"** | `POST` | `/config/master-data/{type}` | `{ label, description?, color?, icon?, metadata? }` | `{ data: MasterDataEntry }` |
| **Nút "Sửa"** | `PUT` | `/config/master-data/{type}/{id}` | `{ label?, description?, color?, icon?, sortOrder?, metadata? }` | `{ data: MasterDataEntry }` |
| **Nút "Vô hiệu hoá"** | `PATCH` | `/config/master-data/{type}/{id}/status` | `{ status: "INACTIVE" }` | `{ data: MasterDataEntry }` |
| **Nút "Xóa"** | `DELETE` | `/config/master-data/{type}/{id}` | — | 200 nếu `refCount=0`; 409 kèm `refCount` nếu đang dùng |
| Kéo-thả sắp xếp | `PATCH` | `/config/master-data/{type}/reorder` | `{ orderedIds: [] }` | `{ success: true }` |
| Xem số lần được dùng | `GET` | `/config/master-data/{type}/{id}/usage` | — | `{ data: { refCount, recentTasks: [] } }` |

**Các `type` dropdown có trong hệ thống:**

| type | Dùng ở đâu |
|---|---|
| `CONG_VAN_TYPE` | Loại công văn — form tạo task Công văn |
| `CONG_VAN_URGENCY` | Mức độ khẩn công văn |
| `HOP_DONG_TYPE` | Loại hợp đồng |
| `CAP_PHAT_TYPE` | Loại cấp phát (Bộ / Thường xuyên) |
| `UNIT_OF_MEASURE` | Đơn vị tính vật tư (cái, bộ, kg, m...) |
| `DEPARTMENT` | Phòng ban / đơn vị |
| `EXTEND_REASON` | Lý do gia hạn deadline |
| `SKIP_REASON` | Lý do bỏ qua bước Multi-step |
| `DEACTIVATE_REASON` | Lý do vô hiệu hoá user |

---

### 9.10 Cấu hình Ngưỡng & Quy tắc chung

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load tất cả ngưỡng | `GET` | `/config/thresholds` | — | `{ data: ThresholdConfig }` |
| **Nút "Lưu"** | `PUT` | `/config/thresholds` | *(xem ThresholdConfig)* | `{ data: ThresholdConfig }` |

**ThresholdConfig:**
```json
{
  "maxCustomCategoriesPerLead": 20,
  "maxExtraFieldsPerCategory": 10,
  "maxStepsPerMultistepTask": 20,
  "maxDeadlineExtendDays": 30,
  "maxDeadlineExtendTimes": 3,
  "fastTaskDeadlineMaxOffset": "END_OF_DAY",
  "defaultDifficulty": 3,
  "defaultEstimateHours": 1,
  "evalAutoOpenDay": 25,         // ngày trong tháng tự động mở kỳ đánh giá
  "evalAutoOpen": true,
  "selfReviewDeadlineDays": 2,
  "leadReviewDeadlineDays": 4,
  "managerFinalizeDeadlineDays": 5,
  "missingDifficultyAlertPercent": 20
}
```

---

### 9.11 Cấu hình Reminder mặc định nhóm (Lead)

> Lead truy cập từ Group Settings, không phải Admin Settings.

| Nút / Hành động | Method | Endpoint | Request Body | Response |
|---|---|---|---|---|
| Load cấu hình reminder nhóm | `GET` | `/groups/{leadId}/reminder-config` | — | `{ data: GroupReminderConfig }` |
| **Nút "Lưu"** | `PUT` | `/groups/{leadId}/reminder-config` | *(xem GroupReminderConfig)* | `{ data: GroupReminderConfig }` |
| **Nút "Reset về mặc định hệ thống"** | `DELETE` | `/groups/{leadId}/reminder-config` | — | `{ data: GroupReminderConfig }` (system defaults) |

**GroupReminderConfig:**
```json
{
  "leadId": "uuid",
  "remindBeforeDeadlineHours": 24,
  "remindBeforeDeadlineHoursUrgent": 2,
  "remindOverdueImmediate": true,
  "remindOverdueNextMorning": true,
  "emailEnabled": false,
  "useSystemDefault": false
}
```

---

### Tổng hợp endpoint Config

| Sub-module | Số endpoint |
|---|---|
| Admin Settings tổng | 2 |
| Task Type Config | 4 |
| Status Config | 2 |
| Priority Config | 5 |
| Evaluation Criteria | 6 |
| KPI Formula | 5 |
| Workload Formula | 3 |
| Difficulty Labels | 2 |
| Master Data (Dropdown) | 8 |
| Thresholds | 2 |
| Group Reminder (Lead) | 3 |
| **Tổng Config** | **~42 endpoints** |

---

## §11 — Multi-step Task: Parallel Branches (Chi tiết)

> Phần này mở rộng §2.3 — định nghĩa cách tạo và thực thi các bước song song.

### 11.1 Khái niệm Parallel Branch

Multi-step Task hỗ trợ hai kiểu thực thi:

| Kiểu | Mô tả |
|---|---|
| **SEQUENTIAL** | Bước N phải DONE trước khi bước N+1 được mở khóa (mặc định) |
| **PARALLEL** | Nhiều bước trong cùng một nhóm được mở khóa đồng thời; nhánh hoàn thành khi **tất cả** bước trong nhóm DONE |

---

### 11.2 Cấu Trúc Dữ Liệu Step

```json
{
  "id": "uuid",
  "taskId": "uuid",
  "title": "string",
  "description": "string",
  "assigneeId": "uuid",
  "order": 1,
  "parallelGroupId": "group-A",
  "executionType": "PARALLEL | SEQUENTIAL",
  "dependsOn": ["stepId-1"],
  "status": "LOCKED | OPEN | IN_PROGRESS | DONE | SKIPPED",
  "requiredForCompletion": true,
  "conditionalSkip": {
    "ifStepId": "uuid",
    "ifOutcome": "APPROVED",
    "then": "SKIP"
  }
}
```

---

### 11.3 Logic Mở Khóa (Unlock Logic)

**Rule cơ bản:**
```
step.status = LOCKED
    → khi tất cả step trong step.dependsOn đều DONE (hoặc SKIPPED nếu optional)
    → chuyển sang OPEN

step trong parallel group:
    → tất cả step trong group.dependsOn group đều DONE
    → toàn bộ group mở khóa đồng thời
```

**Merge Point (điểm gộp):**
```
Parallel Group A: [Step-2a, Step-2b, Step-2c]
Sequential Step-3: dependsOn = [group-A]
    → Step-3 chỉ OPEN khi TẤT CẢ required steps trong Group A = DONE
    → Nếu Step-2b là optional (requiredForCompletion=false) và bị SKIPPED → vẫn OK
```

---

### 11.4 Conditional Branching

Hỗ trợ điều kiện đơn giản dựa trên **outcome** của bước trước:

**Ví dụ: Quy trình duyệt hợp đồng**
```
Step-1: Soạn hợp đồng                   (SEQUENTIAL)
Step-2: Trưởng phòng duyệt              (dependsOn=[Step-1])
   └── outcome: APPROVED | REJECTED

Step-3a: Gửi hợp đồng đi ký    conditionalSkip: { ifStep=2, ifOutcome=REJECTED, then=SKIP }
Step-3b: Soạn lại hợp đồng     conditionalSkip: { ifStep=2, ifOutcome=APPROVED, then=SKIP }
```

**Giới hạn:**
- Mỗi step chỉ có tối đa 1 `conditionalSkip`
- Chỉ hỗ trợ điều kiện đơn (một step, một outcome). Không hỗ trợ AND/OR phức tạp.
- `outcome` là string enum do Lead cấu hình khi tạo step (tối đa 5 outcomes)

---

### 11.5 Deadlock Prevention

Hệ thống validate khi Lead tạo / chỉnh sửa cấu trúc dependency:

1. **Circular dependency**: DFS trên directed graph → nếu tìm cycle → reject "Phát hiện vòng lặp: Step A → B → C → A"
2. **Orphan step**: `dependsOn` trỏ đến step không tồn tại trong task → reject
3. **Parallel group với 1 member**: cảnh báo (không phải lỗi)
4. **Max depth > 10**: cảnh báo hiệu năng

**API validate (dry-run):**
```
POST /tasks/{id}/steps/validate-structure
Body: { steps: [...] }
Response: { valid: bool, errors: [], warnings: [] }
```

---

### 11.6 Task Completion Logic với Parallel

Task MULTI_STEP → `DONE` khi:
```
ALL steps where requiredForCompletion=true → status IN (DONE, SKIPPED)
```

Nếu Lead/Manager đánh dấu DONE thủ công khi còn step LOCKED:
→ System auto-SKIP tất cả step LOCKED & OPEN còn lại
→ Log: `{ action: "FORCE_COMPLETE", skippedSteps: [...] }`

---

### 11.7 KPI Tính Partial Completion

Nếu task bị CANCELLED hoặc FORCE_COMPLETE với một số step chưa xong:

```
partialCompletionRatio = (số required steps DONE) / (tổng required steps)
taskWeight_effective = taskWeight × partialCompletionRatio
```

---

### 11.8 API Endpoints — Steps

| Hành động | Method | Endpoint | Body |
|---|---|---|---|
| Tạo step | `POST` | `/tasks/{id}/steps` | `StepCreateRequest` |
| Sửa step | `PUT` | `/tasks/{id}/steps/{stepId}` | `StepUpdateRequest` |
| Xóa step | `DELETE` | `/tasks/{id}/steps/{stepId}` | — |
| Reorder steps | `PUT` | `/tasks/{id}/steps/reorder` | `{ stepIds: [] }` |
| Validate structure | `POST` | `/tasks/{id}/steps/validate-structure` | `{ steps: [] }` |
| Hoàn thành step | `POST` | `/tasks/{id}/steps/{stepId}/complete` | `{ outcome?, note }` |
| Skip step (Lead) | `POST` | `/tasks/{id}/steps/{stepId}/skip` | `{ reason }` |
| Reassign step | `PATCH` | `/tasks/{id}/steps/{stepId}/assignee` | `{ newAssigneeId }` |
| Lấy toàn bộ steps | `GET` | `/tasks/{id}/steps` | — |

---

## §12 — Deadline Extension: Request & Approval Flow

> Phần này mở rộng §2 và §9.10 — định nghĩa đầy đủ quy trình xin & duyệt gia hạn deadline.

### 12.1 Tổng Quan Flow

```
Member / Lead
     │
     │  POST /tasks/{id}/deadline-extensions
     ▼
┌─────────────────┐
│  DeadlineExt    │  status = PENDING
│  PENDING        │
└─────────────────┘
     │
     ├─────────────────────────────────────────┐
     │  Lead review (nếu Member xin)           │  Manager review (nếu Lead xin)
     ▼                                         ▼
┌──────────┐  ┌──────────┐         ┌──────────┐  ┌──────────┐
│ APPROVED │  │ REJECTED │         │ APPROVED │  │ REJECTED │
└──────────┘  └──────────┘         └──────────┘  └──────────┘
     │
     ▼
task.deadline = newDeadline
```

---

### 12.2 Ai Có Thể Xin Gia Hạn?

| Role | Điều kiện |
|---|---|
| **Member** | Task được assign cho mình, status không phải DONE/CANCELLED |
| **Lead** | Task trong group của mình (bất kể assignee) |
| **Manager** | Có thể tự approve — xem §12.4 |

---

### 12.3 Request Object

```json
{
  "id": "uuid",
  "taskId": "uuid",
  "requestedBy": "userId",
  "requestedByRole": "MEMBER | LEAD | MANAGER",
  "currentDeadline": "2026-06-20T17:00:00",
  "requestedDeadline": "2026-06-25T17:00:00",
  "reason": "string (bắt buộc, min 10 ký tự)",
  "status": "PENDING | APPROVED | REJECTED | EXPIRED",
  "reviewedBy": "userId | null",
  "reviewedAt": "datetime | null",
  "reviewNote": "string | null",
  "extensionNumber": 2,
  "createdAt": "datetime",
  "expiresAt": "datetime"
}
```

---

### 12.4 Validation Trước Khi Tạo Request

| Rule | Mô tả |
|---|---|
| **Max lần gia hạn** | `extensionNumber <= maxDeadlineExtendTimes` (mặc định 3) |
| **Max ngày gia hạn** | `requestedDeadline <= originalDeadline + maxDeadlineExtendDays` (mặc định 30 ngày) |
| **Không overlap** | Không được có request PENDING khác cho cùng task |
| **Deadline hợp lệ** | `requestedDeadline > currentDeadline` |
| **Task đang active** | Task không ở trạng thái DONE, CANCELLED |
| **FAST Task** | FAST Task không hỗ trợ gia hạn |

---

### 12.5 Auto-expire

Nếu Lead/Manager không review trong `deadlineExtensionReviewHours` (config, mặc định 48h):
- Request → `EXPIRED`
- Task deadline **không thay đổi**
- `extensionNumber` **không tăng** (EXPIRED không tính vào số lần)

---

### 12.6 Sau Khi APPROVED

1. `task.deadline = request.requestedDeadline`
2. `task.extensionCount += 1`
3. Notification gửi requester + assignee
4. Audit log: `{ action: "DEADLINE_EXTENDED", oldDeadline, newDeadline, approvedBy }`
5. Nếu task đang OVERDUE → tự động chuyển về IN_PROGRESS

---

### 12.7 Manager Self-Approve

Manager xin gia hạn cho task trong hệ thống → tự approve ngay:
- System auto-APPROVE với `reviewedBy = managerId`
- Ghi log: `{ approvalType: "SELF_APPROVED_BY_MANAGER" }`

---

### 12.8 API Endpoints

| Hành động | Method | Endpoint | Body |
|---|---|---|---|
| Tạo yêu cầu gia hạn | `POST` | `/tasks/{id}/deadline-extensions` | `{ requestedDeadline, reason }` |
| Xem yêu cầu hiện tại | `GET` | `/tasks/{id}/deadline-extensions/pending` | — |
| Lịch sử gia hạn | `GET` | `/tasks/{id}/deadline-extensions` | — |
| Duyệt yêu cầu | `POST` | `/deadline-extensions/{extId}/approve` | `{ note? }` |
| Từ chối yêu cầu | `POST` | `/deadline-extensions/{extId}/reject` | `{ note (bắt buộc) }` |
| Lead xem pending của group | `GET` | `/groups/{leadId}/deadline-extensions/pending` | — |
| Manager xem tất cả pending | `GET` | `/deadline-extensions/pending` | — |

---

## §13 — Member Thuộc Nhiều Nhóm (Multi-Group) & KPI Split

> Phần này xử lý trường hợp một Member được assign vào N nhóm do N Lead quản lý khác nhau.

### 13.1 Mô Hình Dữ Liệu

```
Member X
  ├── GroupMembership { groupId: G1, leadId: L1, isPrimary: true }
  └── GroupMembership { groupId: G2, leadId: L2, isPrimary: false }
```

- **Primary Group**: nhóm chính hiển thị trong dashboard mặc định. Member tự chọn hoặc Manager chỉ định.
- Tối đa **5 nhóm** per member (config `maxGroupsPerMember`, mặc định 5).
- Manager quản lý việc thêm / xóa member khỏi group.

---

### 13.2 Visibility — Ai Thấy Gì

| Role | Task thấy |
|---|---|
| **Lead L1** | Chỉ thấy task do L1 tạo hoặc task trong Group G1 |
| **Lead L2** | Chỉ thấy task do L2 tạo hoặc task trong Group G2 |
| **Manager** | Thấy tất cả task của Member X trong mọi group |
| **Member X** | Thấy tất cả task của mình từ tất cả group |

> Lead **không thể** xem task của Member X thuộc nhóm Lead khác.

---

### 13.3 KPI Tính Riêng theo (Member, Group, Period)

KPI **không gộp** cross-group. Mỗi kỳ đánh giá sinh **một bộ KPI riêng** per group:

```
EvaluationRecord { memberId: X, groupId: G1, leadId: L1, period: 2026-06,
  autoScore, leadScore (←L1 chấm), managerScore, kpiFinal }

EvaluationRecord { memberId: X, groupId: G2, leadId: L2, period: 2026-06,
  autoScore, leadScore (←L2 chấm), managerScore, kpiFinal }
```

**Dashboard Member**: hiển thị KPI theo từng nhóm. Tab "Tổng hợp" hiển thị weighted average.

---

### 13.4 autoScore Tính Riêng

`autoScore` của Member X trong Group G1 chỉ tính task có `task.groupId = G1 AND task.assigneeId = X`. Task trong G2 không ảnh hưởng.

---

### 13.5 Lead Chấm Điểm

- L1 chỉ chấm tasks trong G1 → `leadScore` cho record (X, G1)
- L2 chỉ chấm tasks trong G2 → `leadScore` cho record (X, G2)
- Hai Lead độc lập, không thấy điểm của nhau

---

### 13.6 Manager Finalize

Manager thấy tất cả records của Member X:
- Set `managerScore` riêng cho từng group
- Có thể ghi `crossGroupNote` (không ảnh hưởng KPI, chỉ để tham khảo)
- Có thể tạo **Bonus Task** (không thuộc group) tính vào `managerScore` thủ công

---

### 13.7 Edge Cases

| Case | Xử lý |
|---|---|
| Member bị remove khỏi G1 giữa kỳ | EvaluationRecord (X, G1) vẫn tồn tại với tasks đã hoàn thành. Lead L1 vẫn chấm được. |
| Group G2 bị giải tán giữa kỳ | Record (X, G2) freeze tại thời điểm giải tán. Manager finalize. |
| Member chuyển nhóm (G1 → G3) | Task cũ vẫn thuộc G1. Task mới thuộc G3. Không retroactive. |
| Hai Lead cùng assign task cho cùng Member | Không thể — `task.groupId` là unique. |

---

### 13.8 API Endpoints — Multi-Group

| Hành động | Method | Endpoint | Body |
|---|---|---|---|
| Lấy danh sách group của member | `GET` | `/members/{id}/groups` | — |
| Thêm member vào group | `POST` | `/groups/{groupId}/members` | `{ memberId, isPrimary? }` |
| Xóa member khỏi group | `DELETE` | `/groups/{groupId}/members/{memberId}` | — |
| Đặt primary group | `PATCH` | `/members/{id}/primary-group` | `{ groupId }` |
| KPI summary theo group | `GET` | `/members/{id}/kpi-summary?period=` | — |
| Manager xem cross-group note | `GET` | `/members/{id}/cross-group-notes?period=` | — |
| Manager ghi cross-group note | `PUT` | `/members/{id}/cross-group-notes/{period}` | `{ note }` |

---

## §14 — Nghỉ Phép & Handover (Leave & Handover)

> Hệ thống ghi nhận nghỉ phép để tự động gợi ý handover task và xử lý KPI trong kỳ nghỉ.

### 14.1 Leave Request

**Enum LeaveType:** `SICK | ANNUAL | UNPAID | MATERNITY | PATERNITY | BEREAVEMENT | OTHER`

> Hệ thống **không tính số ngày phép** (đó là nghiệp vụ HR). Chỉ dùng để xử lý task và KPI.

**LeaveRequest object:**
```json
{
  "id": "uuid",
  "memberId": "uuid",
  "leaveType": "ANNUAL",
  "startDate": "2026-07-10",
  "endDate": "2026-07-14",
  "reason": "string",
  "status": "PENDING | APPROVED | REJECTED",
  "reviewedBy": "userId",
  "reviewedAt": "datetime",
  "handoverStatus": "NONE | PARTIAL | COMPLETE",
  "createdAt": "datetime"
}
```

---

### 14.2 Flow Duyệt Nghỉ Phép

```
Member tạo LeaveRequest
       ↓
Lead review (APPROVED / REJECTED)
       ↓ (nếu APPROVED)
System scan task của Member trong [startDate, endDate]
       ↓
Generate HandoverSuggestions
       ↓
Lead nhận notification + danh sách gợi ý → confirm handover từng task
```

---

### 14.3 HandoverSuggestions — Logic Sinh

| Task Type | Có deadline trong kỳ nghỉ? | Gợi ý |
|---|---|---|
| **FAST** | Có | Reassign sang member có workload thấp nhất trong group |
| **MULTI_STEP** | Có step deadline trong kỳ | Reassign step đó; task không đổi assignee |

**HandoverSuggestion object:**
```json
{
  "taskId": "uuid",
  "taskType": "FAST | MULTI_STEP",
  "entityId": "uuid",
  "currentDeadline": "datetime",
  "suggestedAssigneeId": "uuid",
  "alternativeAssigneeIds": [],
  "action": "REASSIGN | EXTEND_DEADLINE | SKIP",
  "confirmed": false
}
```

---

### 14.4 Lead Confirm Handover

Lead có thể:
- **Accept gợi ý**: reassign như gợi ý
- **Custom reassign**: chọn người khác
- **Extend deadline**: kích hoạt flow §12
- **Skip / Archive**: task không còn cần thiết

Lead phải confirm ≥80% tasks có deadline trong kỳ nghỉ trước ngày `startDate` để `handoverStatus = COMPLETE`.

---

### 14.5 KPI Trong Kỳ Nghỉ

| Tình huống | Xử lý KPI |
|---|---|
| Task hoàn thành **trước** kỳ nghỉ | Tính bình thường |
| Task **reassigned** trong kỳ nghỉ | Loại khỏi KPI người nghỉ; tính cho người nhận |
| Task không reassigned, bị OVERDUE khi về | Người nghỉ vẫn chịu penalty |

---

### 14.6 Tạm Quyền (Delegation)

**Manager ủy quyền Lead tạm thời:**
```json
{
  "type": "LEAD_DELEGATION",
  "fromLeadId": "uuid", "toLeadId": "uuid",
  "scope": "FULL",
  "startDate": "date", "endDate": "date",
  "grantedBy": "managerId"
}
```

**Lead ủy quyền Member cấp cao:**
```json
{
  "type": "MEMBER_DELEGATION",
  "fromLeadId": "uuid", "toMemberId": "uuid",
  "scope": "APPROVE_ONLY",
  "startDate": "date", "endDate": "date"
}
```

**Giới hạn:**
- Lead không thể ủy quyền: tạo task, chỉnh KPI formula, xóa category
- Member cấp cao chỉ được APPROVE_ONLY (không assign task mới)
- Delegation tự hết hạn `endDate + 1 ngày`, không auto-renew

---

### 14.7 API Endpoints — Leave & Handover

| Hành động | Method | Endpoint | Body |
|---|---|---|---|
| Tạo yêu cầu nghỉ phép | `POST` | `/leave-requests` | `LeaveRequest` |
| Duyệt / từ chối | `PATCH` | `/leave-requests/{id}/status` | `{ status, note }` |
| Xem handover suggestions | `GET` | `/leave-requests/{id}/handover-suggestions` | — |
| Confirm handover item | `POST` | `/leave-requests/{id}/handover/{suggestionId}/confirm` | `{ action, newAssigneeId? }` |
| Xem leave của member | `GET` | `/members/{id}/leave-requests?year=` | — |
| Lead xem pending requests | `GET` | `/groups/{leadId}/leave-requests/pending` | — |
| Tạo delegation | `POST` | `/delegations` | `DelegationRequest` |
| Xem delegations active | `GET` | `/delegations/active` | — |
| Hủy delegation | `DELETE` | `/delegations/{id}` | — |

---

## §15 — Real-time & WebSocket

> Hệ thống hỗ trợ cập nhật real-time thông qua WebSocket để dashboard và boards luôn đồng bộ.

### 15.1 Endpoint Kết Nối

```
WebSocket: ws(s)://{host}/ws?token={jwtAccessToken}
```

**SSE Fallback**: `GET /sse/events?token={jwtToken}` — push-only, dùng cho notification; không dùng cho concurrent lock.

---

### 15.2 Topics & Events

| Topic | Điều kiện subscribe | Events |
|---|---|---|
| `task.{taskId}` | Có quyền xem task | `TASK_STATUS_CHANGED`, `TASK_DEADLINE_CHANGED`, `TASK_COMMENT_ADDED`, `TASK_STEP_DONE`, `TASK_ASSIGNED` |
| `board.{groupId}` | Lead / Manager của group | `TASK_CREATED`, `TASK_MOVED_STATUS`, `TASK_DELETED` |
| `notification.{userId}` | Chính user đó | `NEW_NOTIFICATION` |
| `dashboard.{userId}` | Chính user đó | `KPI_SCORE_UPDATED`, `WORKLOAD_CHANGED` |
| `eval.{period}.{groupId}` | Lead / Manager | `SELF_REVIEW_SUBMITTED`, `LEAD_SCORE_SAVED`, `PERIOD_FINALIZED` |

---

### 15.3 Message Format

**Client → Server:**
```json
{ "type": "SUBSCRIBE", "topics": ["task.abc123", "board.group789"] }
{ "type": "UNSUBSCRIBE", "topics": ["task.abc123"] }
{ "type": "PONG" }
{ "type": "AUTH_REFRESH", "token": "newJwt" }
```

**Server → Client:**
```json
{
  "type": "EVENT",
  "topic": "task.abc123",
  "event": "TASK_STATUS_CHANGED",
  "payload": { "taskId": "abc123", "oldStatus": "IN_PROGRESS", "newStatus": "DONE",
               "changedBy": "userId", "changedAt": "2026-07-01T10:30:00Z" },
  "serverTime": "2026-07-01T10:30:00.123Z"
}
{ "type": "PING", "serverTime": "..." }
{ "type": "TOKEN_EXPIRING", "expiresIn": 300 }
```

---

### 15.4 Connection Lifecycle

```
Client connect → xác thực JWT
    ├── Hợp lệ → ACK { userId, connectedAt }
    └── Invalid → CLOSE 4001 "Unauthorized"

Heartbeat: PING mỗi 30s → PONG trong 10s, nếu không → close
Reconnect: exponential backoff 1s → 2s → 4s → 8s → max 60s
```

---

### 15.5 Concurrent Edit Lock

Khi user mở Task Detail để chỉnh sửa → hệ thống đặt **soft lock** (Redis TTL 30s):

```json
{
  "entityType": "TASK", "entityId": "taskId",
  "lockedBy": "userId", "lockedByName": "Nguyễn Văn A",
  "expiresAt": "datetime + 30s"
}
```

- User B mở cùng task → nhận event `EDITING_LOCK` → frontend hiển thị banner cảnh báo
- Soft lock: user B vẫn có thể chỉnh sửa (được cảnh báo, không bị block)
- **Lock refresh**: gửi `{ type: "LOCK_REFRESH", entityId }` mỗi 20s nếu vẫn mở form
- **Lock release**: khi close form hoặc save → `{ type: "LOCK_RELEASE", entityId }`

---

### 15.6 Optimistic Update & Conflict Resolution

- Client áp dụng thay đổi local ngay → gọi REST API
- REST API trả 409 nếu version mismatch (`If-Match: {version}` header)
- Server gửi event `CONFLICT_DETECTED` qua WS với `serverVersion`
- Frontend hiển thị diff: user chọn giữ version mình hoặc lấy version server

---

### 15.7 Giới Hạn

| Config | Mặc định |
|---|---|
| Max connections per user | 3 |
| Max subscribed topics per connection | 50 |
| Message buffer (khi client offline) | 100 events, TTL 5 phút |

---

### 15.8 API — WebSocket Management

| Hành động | Method | Endpoint |
|---|---|---|
| Lấy active connections của user | `GET` | `/ws/connections` |
| Force disconnect (Admin) | `DELETE` | `/ws/connections/{connectionId}` |
| Kiểm tra lock của entity | `GET` | `/ws/locks/{entityType}/{entityId}` |
| Release lock thủ công | `DELETE` | `/ws/locks/{entityType}/{entityId}` |

---

## §16 — Audit Log

> Mọi thay đổi quan trọng trong hệ thống được ghi lại để truy vết, compliance, và debug.

### 16.1 Những Gì Được Log

| Nhóm | Các hành động ghi log |
|---|---|
| **Task** | CREATE, UPDATE (status, deadline, assignee, priority, title), DELETE, FORCE_COMPLETE, CANCEL |
| **Step (Multi-step)** | CREATE, UPDATE, COMPLETE, SKIP, REASSIGN |
| **Deadline Extension** | REQUEST, APPROVE, REJECT, EXPIRE |
| **KPI / Evaluation** | SELF_REVIEW_SUBMIT, LEAD_SCORE_SAVE, MANAGER_FINALIZE, PERIOD_OPEN, PERIOD_CLOSE |
| **Category** | CREATE, UPDATE, DELETE, ADD_EXTRA_FIELD, REMOVE_EXTRA_FIELD |
| **Config** | ANY PUT/DELETE trên /config/* |
| **User Management** | CREATE_USER, DEACTIVATE_USER, CHANGE_ROLE, ADD_TO_GROUP, REMOVE_FROM_GROUP |
| **Leave** | REQUEST, APPROVE, REJECT, HANDOVER_CONFIRM |
| **Delegation** | CREATE, REVOKE |
| **Auth** | LOGIN, LOGOUT, TOKEN_REFRESH, LOGIN_FAILED |

---

### 16.2 Log Record Format

```json
{
  "id": "uuid",
  "timestamp": "2026-07-01T10:30:00.123Z",
  "actorId": "userId",
  "actorName": "Nguyễn Văn A",
  "actorRole": "LEAD",
  "action": "TASK_STATUS_CHANGED",
  "entityType": "TASK",
  "entityId": "taskId",
  "groupId": "groupId",
  "oldValue": { "status": "IN_PROGRESS" },
  "newValue": { "status": "DONE" },
  "reason": "string | null",
  "ipAddress": "192.168.x.x",
  "userAgent": "Mozilla/5.0...",
  "requestId": "uuid",
  "source": "WEB | MOBILE | API | SYSTEM"
}
```

> `oldValue` / `newValue`: chỉ ghi các field thay đổi (delta).

---

### 16.3 Phân Quyền Xem Log

| Role | Xem được |
|---|---|
| **Manager** | Tất cả log |
| **Lead** | Log của group mình (`groupId` match), bao gồm members |
| **Member** | Log có `actorId = mình` HOẶC `entityId` là task/instance của mình |

---

### 16.4 Retention Policy

| Phase | Thời gian | Storage |
|---|---|---|
| **Hot** (searchable) | 12 tháng | Database (indexed) |
| **Warm** (compressed) | 12–36 tháng | Object storage (S3/MinIO) |
| **Cold** (archive) | 36 tháng+ | Glacier |
| **Delete** | Sau 5 năm | Purge hoàn toàn |

> Cấu hình bởi Manager: `auditLogRetentionMonths` trong Admin Settings.

---

### 16.5 Search & Filter

```
GET /audit-logs?
  actorId=       entityType=    entityId=
  groupId=       action=        source=
  from=          to=            q= (full-text)
  page=&size=50
```

---

### 16.6 Export CSV

```
GET /audit-logs/export?format=csv&from=2026-01-01&to=2026-06-30
```

- Chỉ Manager được export
- Max 10,000 records (sync). Trên 1,000 → async job: polling `GET /audit-logs/export/{jobId}`

**CSV columns:** `timestamp, actorName, actorRole, action, entityType, entityId, groupId, oldValue, newValue, reason, ipAddress, source`

---

### 16.7 API Endpoints — Audit Log

| Hành động | Method | Endpoint |
|---|---|---|
| Lấy danh sách log | `GET` | `/audit-logs` |
| Xem chi tiết record | `GET` | `/audit-logs/{id}` |
| Export CSV | `GET` | `/audit-logs/export` |
| Trạng thái export job | `GET` | `/audit-logs/export/{jobId}` |
| Log của task cụ thể | `GET` | `/tasks/{id}/audit-logs` |
| Log của user cụ thể | `GET` | `/members/{id}/audit-logs` |

---

### 16.8 Tổng Hợp Endpoint Bổ Sung

| Module mới | Số endpoint |
|---|---|
| Multi-step Steps bổ sung (§11) | 9 |
| Deadline Extension (§12) | 7 |
| Multi-Group Management (§13) | 7 |
| Leave & Handover (§14) | 9 |
| WebSocket Management (§15) | 4 |
| Audit Log (§16) | 6 |
| **Tổng bổ sung** | **~42 endpoints** |

**Grand Total**: ~130 (cũ) + ~42 (mới) = **~172 endpoints**

---

*Cập nhật lần cuối: 2026-07-02*
