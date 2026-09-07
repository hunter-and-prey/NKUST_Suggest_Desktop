package nkust

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"NKUST_Suggest_Desktop/pkg/constants"
	"NKUST_Suggest_Desktop/pkg/models"
)

type Client struct {
	httpClient *http.Client
}

type SubmitResponseData struct {
	Result  bool   `json:"result"`
	Message string `json:"message"`
}

func NewClient() (*Client, error) {
	jar, err := cookiejar.New(nil)
	if err != nil {
		return nil, err
	}

	return &Client{
		httpClient: &http.Client{
			Jar:     jar,
			Timeout: 30 * time.Second,
		},
	}, nil
}

// 取得 Create 頁面的 CSRF Token
func (c *Client) FetchCSRFToken() (string, error) {
	req, err := http.NewRequest("GET", constants.CreateURL, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("連線校務系統失敗: %w", err)
	}
	defer resp.Body.Close()

	bodyBytes, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	htmlContent := string(bodyBytes)
	re := regexp.MustCompile(`name="__RequestVerificationToken"\s+type="hidden"\s+value="([^"]+)"|value="([^"]+)"\s+name="__RequestVerificationToken"`)
	matches := re.FindStringSubmatch(htmlContent)

	token := ""
	if len(matches) > 1 && matches[1] != "" {
		token = matches[1]
	} else if len(matches) > 2 && matches[2] != "" {
		token = matches[2]
	}

	if token == "" {
		// 備用匹配
		reSimple := regexp.MustCompile(`__RequestVerificationToken["'][^>]*value=["']([^"']+)["']`)
		m := reSimple.FindStringSubmatch(htmlContent)
		if len(m) > 1 {
			token = m[1]
		}
	}

	if token == "" {
		return "", errors.New("無法在校務系統頁面中擷取防偽權杖 (__RequestVerificationToken)")
	}

	return token, nil
}

// 提交建言表單 (包含附件檔案 multipart/form-data)
func (c *Client) SubmitSuggestion(req models.SubmitRequest) (*SubmitResponseData, error) {
	token, err := c.FetchCSRFToken()
	if err != nil {
		return nil, err
	}

	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	fields := map[string]string{
		"__RequestVerificationToken": token,
		"Name":                       strings.TrimSpace(req.Name),
		"GuestType":                  req.GuestType,
		"Email":                      strings.TrimSpace(req.Email),
		"Phone":                      strings.TrimSpace(req.Phone),
		"SecrecyType":                req.SecrecyType,
		"DoneOpen":                   req.DoneOpen,
		"Subject":                    strings.TrimSpace(req.Subject),
		"UnitId":                     req.UnitID,
		"MessageContent":             req.MessageContent,
	}

	for k, v := range fields {
		if err := writer.WriteField(k, v); err != nil {
			return nil, err
		}
	}

	// 處理附件 (最多 3 個)
	allowedExts := map[string]bool{
		".txt": true, ".doc": true, ".docx": true,
		".pdf": true, ".jpg": true, ".bmp": true, ".png": true,
	}
	const maxFileSize = 30 * 1024 * 1024

	attachedCount := 0
	for _, fpath := range req.FilePaths {
		if attachedCount >= 3 {
			break
		}
		if fpath == "" {
			continue
		}

		ext := strings.ToLower(filepath.Ext(fpath))
		if !allowedExts[ext] {
			continue
		}

		fi, err := os.Stat(fpath)
		if err != nil || fi.Size() > maxFileSize {
			continue
		}

		file, err := os.Open(fpath)
		if err != nil {
			continue
		}

		part, err := writer.CreateFormFile("files", filepath.Base(fpath))
		if err != nil {
			file.Close()
			continue
		}
		_, _ = io.Copy(part, file)
		file.Close()
		attachedCount++
	}

	// ASP.NET MVC 規格：form input 欄位為 3 個 files
	// 若未滿 3 個檔案，必須補足空的 files 欄位以符合後端 IEnumerable<HttpPostedFileBase> 模型驗證
	for attachedCount < 3 {
		_, _ = writer.CreateFormFile("files", "")
		attachedCount++
	}

	if err := writer.Close(); err != nil {
		return nil, err
	}

	httpReq, err := http.NewRequest("POST", constants.CreateURL, body)
	if err != nil {
		return nil, err
	}

	// 關鍵核心修復：
	// ASP.NET MVC 的 AntiForgery 權杖由 Cookie 權杖與表單/標頭權杖構成。
	// 在高科大系統中，input 隱藏欄位包含以冒號分隔的 (CookieToken:FormToken)。
	// 伺服器必須同時接收到名為 __RequestVerificationToken 的 Cookie，否則會報 HTTP 500！
	tokenParts := strings.Split(token, ":")
	cookieToken := tokenParts[0]

	targetURL, _ := url.Parse(constants.CreateURL)
	if c.httpClient.Jar != nil && targetURL != nil {
		c.httpClient.Jar.SetCookies(targetURL, []*http.Cookie{
			{
				Name:  "__RequestVerificationToken",
				Value: cookieToken,
				Path:  "/",
			},
		})
	}

	httpReq.AddCookie(&http.Cookie{
		Name:  "__RequestVerificationToken",
		Value: cookieToken,
		Path:  "/",
	})

	httpReq.Header.Set("Content-Type", writer.FormDataContentType())
	httpReq.Header.Set("X-Requested-With", "XMLHttpRequest")
	httpReq.Header.Set("Referer", constants.CreateURL)
	httpReq.Header.Set("__RequestVerificationToken", token)
	httpReq.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("提交請求失敗: %w", err)
	}
	defer resp.Body.Close()

	respBytes, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	var jsonResult SubmitResponseData
	if err := json.Unmarshal(respBytes, &jsonResult); err == nil {
		return &jsonResult, nil
	}

	// 若伺服器未回傳標準 JSON，依關鍵字判斷
	respStr := string(respBytes)
	if strings.Contains(respStr, "成功") || strings.Contains(respStr, "已建立") {
		return &SubmitResponseData{
			Result:  true,
			Message: "建言已成功提交！",
		}, nil
	}

	return &SubmitResponseData{
		Result:  false,
		Message: fmt.Sprintf("伺服器回應異常 (HTTP %d): %s", resp.StatusCode, respStr),
	}, nil
}
