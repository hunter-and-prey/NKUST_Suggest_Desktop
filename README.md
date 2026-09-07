# 高科大校務建言桌面版小助手 (NKUST Suggest Desktop)

專為國立高雄科技大學（NKUST）設計的高效能原生桌面版應用程式。採用 **Go (Golang)** 原生編譯與 **Wails v2 (Windows Edge WebView2)** 視窗架構。

---

## 🌟 亮點功能與架構特色
1. **極致效能與輕量體積**：
   - 使用 Go 語言原生編譯為單一二進位執行檔：**僅 11.02 MB**。
   - 執行時記憶體佔用極低（僅約 20~35 MB），冷啟動瞬間秒開。
   - 完全捨棄 Python 環境依賴與肥大的 node_modules，乾淨獨立。
2. **行為說服設計與防錯 (`ux-persuasion-engineer` + `ui-ux-pro-max`)**：
   - **智慧預設與資料記憶**：姓名、身分、電話、保密與公開選項本機自動記憶，每次開啟直奔建言主題。
   - **字數精準換算**：建言主旨限 20 字，建言內容限 1000 字（即時動態折算換行雙字元），超標視覺平滑漸層提示。
   - **視覺對比**：採用高科大教育深藍 (`#1E40AF`) 搭配行動引導琥珀橘 (`#EA580C`)，文字對比達 14:1。
   - **附件支援**：支援原生檔案對話框選取最多 3 個檔案（支援 `.pdf, .doc, .docx, .txt, 圖片` 等，單檔限 30MB）。
3. **全自動雙階段提交與驗證**：
   - **第 1 階段 (建言提交)**：Go 背景向校務系統抓取 ASP.NET MVC CSRF Token 與 Session，透過 `multipart/form-data` 秒速發送。
   - **第 2 階段 (信箱自動確認)**：若填寫信箱授權密碼，Go 後端以 Goroutine 非同步監聽收件匣，收到建言確認信後，**自動發送 HTTP GET 請求在背景直接完成驗證**，免開瀏覽器打擾使用者！

---

## 🚀 使用方式

### 下載執行檔 (Releases)
直接由 GitHub Releases 下載對應作業系統版本（Windows / macOS）解壓縮後即可執行。

---

## 🛠️ 本機編譯指南

### 環境需求
- **Go**: 1.22+
- **Wails CLI**: `go install github.com/wailsapp/wails/v2/cmd/wails@latest`

### 編譯指令
```bash
# Windows
go build -tags desktop,production -ldflags "-H windowsgui -s -w" -o NKUST_Suggest_Desktop.exe .

# macOS (在 Mac 環境下)
wails build
```
