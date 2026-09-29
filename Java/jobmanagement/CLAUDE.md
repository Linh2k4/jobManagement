# Multi-Project Workspace

Workspace chứa các dự án Java (backend, Spring Boot), Angular (frontend), Golang, Python. Mỗi project có thể có CLAUDE.md riêng override cấu hình chung này.

## Quy ước chung

- **Ngôn ngữ**: Java, TypeScript/Angular, Golang, Python
- **Full guides**: `docs/ARCHITECTURE.md`, `docs/GIT_WORKFLOW.md`, `docs/TESTING.md`, `docs/CODE_STYLE.md` (chi tiết hơn bản quick-reference ở `.claude/rules/RULE.md`)
- **Commit message**: Tiếng Anh, tối đa 70 ký tự, mô tả "why" không phải "what"
- **Code style**: Sử dụng formatter mặc định của từng ngôn ngữ
- **Testing**: Unit tests bắt buộc cho logic, integration tests cho các thay đổi cơ sở dữ liệu

## Quy tắc xử lý

1. **Trước khi edit**: Luôn read file trước, không assume cấu trúc
2. **Parallel calls**: Sử dụng parallel khi tasks độc lập
3. **Không tạo file không cần thiết**: Prefer edit existing
4. **Safety first**: Luôn confirm trước khi push, force push, hoặc xóa

## Tools & Hooks

- `.claude/settings.json`: Cấu hình permissions, env vars
- `.claude/hooks.json`: Pre-commit, post-merge hooks
- `.claude/MEMORY.md`: Nhật ký quyết định và learning
- Từng project có thể override bằng `.claude/settings.local.json`

## Structure - Shared Setup for All Children

```
Project/ (Parent - Single Source of Truth)
├── CLAUDE.md (shared config)
├── docs/ (ARCHITECTURE, GIT_WORKFLOW, TESTING, CODE_STYLE — full guides)
├── .claude/
│   ├── settings.json, settings.local.json (root permissions, hooks, env)
│   ├── rules/ (shared, symlinked by every child)
│   ├── scripts/ (shared, symlinked by every child)
│   ├── skills/ (analy, recall, remember, memories, archive, search, summary —
│   │            symlinked *individually* into every child's skills/, alongside
│   │            each child's own local skill(s))
│   └── mcp-server/ (Postgres/pgvector-backed `memory` MCP server, see .mcp.json)
│
├── Java/
│   ├── CLAUDE.md
│   ├── .claude/
│   │   ├── rules, scripts → symlinks to ../../.claude/{rules,scripts}
│   │   └── skills/ → analy, recall, ... symlinked individually + maven-gradle/ (local)
│   └── JobManagement/  (Spring Boot, modules: api, iam, model, common, cache, storage-db, storage-minio)
│
├── Angular/
│   ├── CLAUDE.md
│   ├── .claude/ → same symlink pattern + angular-tools/ (local skill)
│   └── JobManagement/  (Angular 18 FE, consumes Java `api` module via OpenAPI-generated client)
│
├── GoLang/
│   ├── CLAUDE.md
│   └── .claude/ → same symlink pattern + go-tools/ (local skill)
│
├── Python/
│   ├── CLAUDE.md
│   └── .claude/ → same symlink pattern + python-tools/ (local skill)
│
└── BA/
    └── JobManagement/
        ├── DESIGN_DESCRIPTION.md   (screens, tokens, cách làm việc với design)
        ├── design-data/            (export cũ: tokens, component list, document snapshot — tham khảo)
        ├── screenshots/            (ảnh chụp tham khảo/inspiration)
        └── _archive/               (workflow Figma cũ — xem bên dưới)
```

**Lưu ý:** GoLang/Python hiện là khung sườn (chưa có code thật), giữ nguyên cấu hình để sẵn sàng khi bắt đầu code. `BA/` không phải code project nên không có `.claude/` riêng — dùng chung skill/memory qua root như mọi thư mục khác trong workspace.

### Figma (BA/design work)

3 cơ chế từng tồn tại song song — **chuẩn hiện tại là Figma MCP**:

| Cơ chế | Trạng thái |
|---|---|
| **Figma MCP** (`mcp__claude_ai_Figma__*`) | ✅ Chuẩn — đã kết nối sẵn trong Claude Code, không cần token/script. Dùng cho mọi việc đọc/sửa design từ nay. |
| `BA/JobManagement/_archive/` (script paste Figma Console + plugin) | ❌ Legacy — giữ tham khảo tên component/token, không dùng để build mới |
| `.claude/scripts/figma_*.py` (REST API, token trong `.claude/.env.figma`) | ❌ Legacy — chưa source vào shell, không phải hướng chuẩn |

Chi tiết: `BA/JobManagement/DESIGN_DESCRIPTION.md`.

### What's Shared Between All Children

| Component | Parent Location | Child Access | Ghi chú |
|-----------|-----------------|--------------|---------|
| **Skills** (analy, recall, remember, ...) | `.claude/skills/<name>/` | Symlink từng skill riêng lẻ | Không symlink cả thư mục `skills/` để không đè skill cục bộ của từng project |
| **Scripts** | `.claude/scripts/` | Symlink | recall-search, save-memory, sync-fe-be, docs-to-md,... |
| **Rules** | `.claude/rules/` | Symlink | Git, code style, testing rules — quick-ref; full guide ở `docs/` |
| **Semantic memory** | Postgres/pgvector qua MCP server `memory` (`.mcp.json`, `http://localhost:8765/mcp`) | Kết nối qua network, không phải filesystem | Dùng qua skill `recall`/`remember`, không phải `.claude/projects/` (đã bỏ — dangling) |
| **Auto-memory (native)** | `%USERPROFILE%\\.claude\\projects\\<project-cwd-slug>\\memory\\` | Global, theo cwd | Xem biến `MEMORY_STORAGE` trong `.claude/settings.json`; trên Windows dùng `%USERPROFILE%` thay cho `~`. |
| **Code intelligence** | CodeGraph MCP server (`codegraph init` chạy 1 lần ở root, index toàn bộ Java+Angular+Go+Python) | MCP tool, không phải filesystem | Xem mục Helper Tools bên dưới |

## Windows Environment

- **OS**: Windows 10/11
- **Shell**: PowerShell
- **Path format**: dùng đường dẫn Windows, ví dụ `D:\\THUCTAP\\Java\\jobmanagement`.
- **Không dùng** các đường dẫn macOS như `/Users/...`, `~/...`, `/opt/homebrew/...`.
- **Không dùng** Homebrew (`brew`); ưu tiên `winget`, installer chính thức hoặc package manager phù hợp với Windows.
- **Symlink**: khi cần chia sẻ `rules`, `scripts`, `skills` giữa các project, ưu tiên Windows junction/symlink phù hợp với quyền của máy. Không hard-code đường dẫn `/Users/macbook/...`.
- **Docker**: project chạy qua Docker Desktop + Linux containers. Đường dẫn `build.context` trong `docker-compose.yml` phải là đường dẫn tương đối từ file compose hoặc đường dẫn Windows hợp lệ; không để path kiểu `/Users/macbook/Documents/...`.
- **Claude/MCP**: mọi đường dẫn filesystem trong `.mcp.json`, `.claude/settings.json`, `docker-compose.yml` và script phải được kiểm tra để không còn tham chiếu máy Mac cũ.
- **Git**: dùng Git Bash hoặc PowerShell; không giả định các lệnh shell Unix như `brew`, `install_name_tool` hoặc path `/Users/...`.

## Helper CLI Tools

| Tool | Dùng khi | Lệnh |
|------|----------|------|
| **CodeGraph** | Trace call graph / dependency giữa Java ↔ Angular ↔ Go/Python trong 1 câu hỏi thay vì nhiều lượt grep. Index ở root, bao trùm cả workspace | `codegraph status` (health check), `codegraph init` (rebuild nếu cần) |
| **markitdown** | Convert nhanh `.docx/.pptx/.xlsx/.pdf` (SRS, tài liệu BA) sang Markdown, giữ cấu trúc, để Claude đọc hiệu quả hơn đọc file gốc | `.claude/scripts/docs-to-md.sh <file>` hoặc `markitdown <file> -o out.md` |
| **pandoc** | Convert 2 chiều, nhiều định dạng hơn markitdown (kể cả md → docx/pdf), cần khi cần control sâu format đầu ra | `.claude/scripts/docs-to-md.sh --pandoc <file>` hoặc `pandoc <file> -o out.md` |

**Lưu ý cài đặt Windows:** `pandoc` có thể cài bằng `winget install JohnMacFarlane.Pandoc` hoặc cài thủ công từ trang chính thức. `markitdown` nên cài trong Python virtual environment để tránh ảnh hưởng Python hệ thống. Ví dụ PowerShell: `py -3.12 -m venv .venv-markitdown` rồi `.venv-markitdown\\Scripts\\python.exe -m pip install "markitdown[all]"`. Nếu muốn dùng lệnh `markitdown` trực tiếp, kích hoạt venv bằng `.venv-markitdown\\Scripts\\Activate.ps1` trước khi chạy. Không dùng các lệnh Homebrew, `install_name_tool`, `~/.local/bin` hoặc đường dẫn `/Users/...` trên Windows.

## Plugins & Skills

### Memory System (Always enabled)
```bash
/analy limit=200          # Auto-analyze & save
/remember "text"          # Save insight
/recall query="..."       # Search memories
/memories category=...    # List memories
/archive conversation_id=...  # Archive (lazy-load)
/search query="..."           # Search archives (lazy-load)
```

### Build Tools (Lazy-load when needed)
```bash
/build-java               # Build Java
/build-golang            # Build Go
/build-python            # Build Python
```

### Optional Skills (Disabled by default)
```bash
# Enable in settings if needed:
# /design-api
# /export-to-obsidian
# /setup-logging
```

### Plugin Loading
- **Always load**: memory-system (core skills)
- **Lazy-load**: build-tools (project-specific)
- **Disabled**: api-design, obsidian-export, logging-setup (rarely used)

Configure in `.claude/settings.local.json` via `skillOverrides`
