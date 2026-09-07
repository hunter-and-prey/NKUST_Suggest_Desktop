package main

import (
	"context"
	"errors"
	"fmt"
	"net/url"
	"strings"

	"NKUST_Suggest_Desktop/pkg/config"
	"NKUST_Suggest_Desktop/pkg/constants"
	"NKUST_Suggest_Desktop/pkg/mail"
	"NKUST_Suggest_Desktop/pkg/models"
	"NKUST_Suggest_Desktop/pkg/nkust"

	"github.com/pkg/browser"
	wailsRuntime "github.com/wailsapp/wails/v2/pkg/runtime"
)

type App struct {
	ctx          context.Context
	cancelVerify context.CancelFunc
}

func NewApp() *App {
	return &App{}
}

func (a *App) startup(ctx context.Context) {
	a.ctx = ctx
}

// 取得常數清單供前端下拉選單渲染
func (a *App) GetMetadata() map[string]interface{} {
	return map[string]interface{}{
		"units":         constants.Units,
		"guest_types":   constants.GuestTypes,
		"secrecy_types": constants.SecrecyTypes,
		"done_open":     constants.DoneOpenTypes,
	}
}

// 載入使用者本機設定
func (a *App) LoadConfig() models.UserConfig {
	return config.LoadConfig()
}

// 儲存使用者設定
func (a *App) SaveConfig(cfg models.UserConfig) models.OperationResponse {
	if err := config.SaveConfig(cfg); err != nil {
		return models.OperationResponse{
			Success: false,
			Message: fmt.Sprintf("儲存設定失敗: %v", err),
		}
	}
	return models.OperationResponse{
		Success: true,
		Message: "設定已成功儲存！",
	}
}

// 呼叫原生檔案選擇器 (最多 3 個附件)
func (a *App) SelectFiles() []string {
	selection, err := wailsRuntime.OpenMultipleFilesDialog(a.ctx, wailsRuntime.OpenDialogOptions{
		Title: "選擇附件 (最多3個，限30MB)",
		Filters: []wailsRuntime.FileFilter{
			{DisplayName: "允許的檔案 (*.pdf, *.doc, *.docx, *.txt, *.jpg, *.png)", Pattern: "*.pdf;*.doc;*.docx;*.txt;*.jpg;*.png;*.bmp"},
			{DisplayName: "所有檔案 (*.*)", Pattern: "*.*"},
		},
	})
	if err != nil {
		return []string{}
	}
	if len(selection) > 3 {
		selection = selection[:3]
	}
	return selection
}

// 取消當前確認信監聽
func (a *App) CancelVerification() {
	if a.cancelVerify != nil {
		a.cancelVerify()
		a.cancelVerify = nil
	}
}

// 獨立功能：隨時手動檢查信箱中的最新確認信並自動執行點擊確認
func (a *App) CheckAndVerifyLatestEmail(mailUser, mailPassword string) models.OperationResponse {
	if mailUser == "" || mailPassword == "" {
		return models.OperationResponse{
			Success: false,
			Message: "請先在「常駐個資與信箱授權」分頁填寫信箱帳號與 Google 16 碼應用程式密碼！",
		}
	}

	emitProgress := func(msg string, pct int, done, isErr bool) {
		wailsRuntime.EventsEmit(a.ctx, "progress_update", models.StatusEvent{
			Step:       2,
			TotalSteps: 2,
			Message:    msg,
			Progress:   pct,
			IsError:    isErr,
			Done:       done,
		})
	}

	emitProgress("正在連接信箱並搜尋最新建言確認信...", 30, false, false)
	verifier := mail.NewVerifier("imap.gmail.com", 993, mailUser, mailPassword)
	confirmURL, subj, err := verifier.CheckLatestConfirmationURL(func(st string) {
		emitProgress(st, 60, false, false)
	})

	if err != nil {
		emitProgress(fmt.Sprintf("搜尋失敗: %v", err), 100, true, true)
		return models.OperationResponse{
			Success: false,
			Message: fmt.Sprintf("未能在信箱中找到或解析確認信: %v", err),
		}
	}

	emitProgress(fmt.Sprintf("找到確認信「%s」，正在自動發送驗證...", subj), 85, false, false)
	if err := mail.ConfirmURLDirectly(confirmURL); err != nil {
		_ = openBrowser(confirmURL)
		emitProgress("已在瀏覽器開啟確認頁面！", 100, true, false)
		return models.OperationResponse{
			Success:    true,
			Message:    fmt.Sprintf("成功找到確認信！已為您在瀏覽器開啟確認連結。\n\n主旨: %s", subj),
			ConfirmURL: confirmURL,
		}
	}

	emitProgress("🎉 信件已自動完成驗證！案件正式生效。", 100, true, false)
	return models.OperationResponse{
		Success:    true,
		Message:    fmt.Sprintf("🎉 成功找到確認信並全自動完成驗證！\n\n主旨: %s\n驗證連結: %s", subj, confirmURL),
		ConfirmURL: confirmURL,
	}
}

// 核心功能：一鍵提交建言並全自動郵件確認
func (a *App) SubmitSuggestion(req models.SubmitRequest, mailUser, mailPassword string) models.OperationResponse {
	// 前端即時回報輔助函式
	emitProgress := func(step int, msg string, pct int, done, isErr bool) {
		wailsRuntime.EventsEmit(a.ctx, "progress_update", models.StatusEvent{
			Step:       step,
			TotalSteps: 3,
			Message:    msg,
			Progress:   pct,
			IsError:    isErr,
			Done:       done,
		})
	}

	// 1. 驗證字數規則 (以高科大標準：換行折算 2 字元)
	contentCalc := len(strings.ReplaceAll(strings.ReplaceAll(req.MessageContent, "\r\n", "  "), "\n", "  "))
	if contentCalc > 1000 {
		return models.OperationResponse{
			Success: false,
			Message: fmt.Sprintf("建言內容已超過 1000 字上限 (目前折算為 %d 字)，請適度精簡或改以附件上傳！", contentCalc),
		}
	}
	if len([]rune(req.Subject)) > 20 {
		return models.OperationResponse{
			Success: false,
			Message: "建言主旨請勿超過 20 字！",
		}
	}

	emitProgress(1, "【步驟 1/3】正在連線高科大校務系統並取得 CSRF 權杖...", 20, false, false)

	client, err := nkust.NewClient()
	if err != nil {
		emitProgress(1, fmt.Sprintf("初始化 HTTP 客戶端失敗: %v", err), 20, true, true)
		return models.OperationResponse{Success: false, Message: err.Error()}
	}

	submitRes, err := client.SubmitSuggestion(req)
	if err != nil || (submitRes != nil && !submitRes.Result) {
		errMsg := "建言提交失敗"
		if err != nil {
			errMsg = fmt.Sprintf("建言提交失敗: %v", err)
		} else if submitRes != nil {
			errMsg = fmt.Sprintf("校務系統回應: %s", submitRes.Message)
		}
		emitProgress(1, errMsg, 20, true, true)
		return models.OperationResponse{Success: false, Message: errMsg}
	}

	emitProgress(2, "【步驟 2/3】建言已成功送達校務系統！正在信箱中自動搜尋確認信...", 50, false, false)

	// 若使用者未提供信箱帳密，則完成提交並提醒手動收信
	if mailUser == "" || mailPassword == "" {
		emitProgress(2, "建言已成功提交！(因未填寫信箱密碼，請至信箱點擊確認信完成受理)", 100, true, false)
		return models.OperationResponse{
			Success: true,
			Message: "建言已成功送出！請至您的信箱手動開啟確認信以完成生效。",
		}
	}

	// 啟動信箱監聽
	ctx, cancel := context.WithCancel(context.Background())
	a.cancelVerify = cancel
	defer func() {
		a.cancelVerify = nil
	}()

	verifier := mail.NewVerifier("imap.gmail.com", 993, mailUser, mailPassword)
	confirmURL, err := verifier.PollConfirmationURL(ctx, 90, func(st string) {
		emitProgress(2, fmt.Sprintf("【步驟 2/3】%s", st), 75, false, false)
	})

	if err != nil {
		emitProgress(2, fmt.Sprintf("建言已送出，但信件自動確認超時或取消: %v", err), 100, true, false)
		return models.OperationResponse{
			Success: true,
			Message: fmt.Sprintf("建言已成功送達！自動確認信件提示：%v。請手動開啟信箱點擊確認連結。", err),
		}
	}

	// 3. 自動以 HTTP GET 打通確認連結
	emitProgress(3, fmt.Sprintf("【步驟 3/3】正在全自動發送確認請求: %s", confirmURL), 90, false, false)
	if err := mail.ConfirmURLDirectly(confirmURL); err != nil {
		// 若直接 GET 異常，喚起本機瀏覽器開啟確認
		_ = openBrowser(confirmURL)
		emitProgress(3, "已在瀏覽器開啟確認頁面完成驗證！", 100, true, false)
		return models.OperationResponse{
			Success:    true,
			Message:    "建言已成功送出，並已為您開啟瀏覽器完成最終確認！",
			ConfirmURL: confirmURL,
		}
	}

	emitProgress(3, "🎉 建言已成功提交並全自動完成信箱確認！", 100, true, false)
	return models.OperationResponse{
		Success:    true,
		Message:    "🎉 建言已全自動提交並完成信箱確認！校務系統已正式受理該案。",
		ConfirmURL: confirmURL,
	}
}

func openBrowser(targetURL string) error {
	u, err := url.Parse(targetURL)
	if err != nil {
		return fmt.Errorf("無效的 URL 格式: %w", err)
	}
	if u.Scheme != "https" && u.Scheme != "http" {
		return errors.New("僅允許開啟 HTTP/HTTPS 協議之連結")
	}
	if u.Host != "suggest.nkust.edu.tw" {
		return fmt.Errorf("拒絕開啟非校務系統官方域名之連結 (%s)", u.Host)
	}
	return browser.OpenURL(targetURL)
}
