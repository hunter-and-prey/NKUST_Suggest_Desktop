package models

// 使用者常駐基本設定
type UserConfig struct {
	Name         string `json:"name"`
	GuestType    string `json:"guest_type"`
	Email        string `json:"email"`
	Phone        string `json:"phone"`
	SecrecyType  string `json:"secrecy_type"`
	DoneOpen     string `json:"done_open"`
	DefaultUnit  string `json:"default_unit"`
	MailPassword string `json:"mail_password"`
	HasGoogleAuth bool  `json:"has_google_auth"`
}

// 送出建言請求結構
type SubmitRequest struct {
	Subject        string   `json:"subject"`
	UnitID         string   `json:"unit_id"`
	MessageContent string   `json:"message_content"`
	FilePaths      []string `json:"file_paths"`
	// 可隨請求覆蓋基本個資
	Name        string `json:"name"`
	GuestType   string `json:"guest_type"`
	Email       string `json:"email"`
	Phone       string `json:"phone"`
	SecrecyType string `json:"secrecy_type"`
	DoneOpen    string `json:"done_open"`
}

// 執行結果回應
type OperationResponse struct {
	Success    bool   `json:"success"`
	Message    string `json:"message"`
	ConfirmURL string `json:"confirm_url,omitempty"`
	Step       int    `json:"step"`
}

// 即時狀態事件回報結構 (透過 Wails Event 發送給前端)
type StatusEvent struct {
	Step       int    `json:"step"`       // 1: 提交表單中, 2: 輪詢確認信中, 3: 自動完成驗證中, 4: 完工
	TotalSteps int    `json:"total_steps"`
	Message    string `json:"message"`
	Progress   int    `json:"progress"`   // 0 - 100
	IsError    bool   `json:"is_error"`
	Done       bool   `json:"done"`
}
