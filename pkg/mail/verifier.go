package mail

import (
	"context"
	"crypto/tls"
	"encoding/base64"
	"errors"
	"fmt"
	"io"
	"mime"
	"mime/multipart"
	"mime/quotedprintable"
	netmail "net/mail"
	"net/http"
	"net/url"
	"regexp"
	"strings"
	"time"

	"github.com/emersion/go-imap"
	"github.com/emersion/go-imap/client"
)

type Verifier struct {
	IMAPHost string
	IMAPPort int
	Username string
	Password string // 支援 Google 應用程式密碼或學校信箱密碼
}

func NewVerifier(host string, port int, user, pwd string) *Verifier {
	if host == "" {
		host = "imap.gmail.com"
	}
	if port == 0 {
		port = 993
	}
	// 去除密碼中常見的空格 (例如 Google 應用程式密碼常以 4 個一組顯示)
	pwd = strings.ReplaceAll(pwd, " ", "")
	return &Verifier{
		IMAPHost: host,
		IMAPPort: port,
		Username: strings.TrimSpace(user),
		Password: pwd,
	}
}

// 測試連線
func (v *Verifier) TestLogin() error {
	addr := fmt.Sprintf("%s:%d", v.IMAPHost, v.IMAPPort)
	tlsConfig := &tls.Config{
		ServerName:         v.IMAPHost,
		InsecureSkipVerify: false,
	}
	c, err := client.DialTLS(addr, tlsConfig)
	if err != nil {
		return fmt.Errorf("無法連線至郵件伺服器 %s: %w", addr, err)
	}
	defer c.Logout()

	if err := c.Login(v.Username, v.Password); err != nil {
		return fmt.Errorf("信箱登入失敗 (若為 Google 信箱請使用 16 碼應用程式密碼): %w", err)
	}
	return nil
}

// 解析郵件本體 (完整支援 Quoted-Printable, Base64 及 Multipart 解碼)
func parseEmailBody(r io.Reader) string {
	msg, err := netmail.ReadMessage(r)
	if err != nil {
		buf, _ := io.ReadAll(r)
		return string(buf)
	}

	contentType := msg.Header.Get("Content-Type")
	mediaType, params, err := mime.ParseMediaType(contentType)
	if err != nil {
		buf, _ := io.ReadAll(msg.Body)
		return string(buf)
	}

	decodeContent := func(src io.Reader, encoding string) io.Reader {
		enc := strings.ToLower(strings.TrimSpace(encoding))
		switch enc {
		case "quoted-printable":
			return quotedprintable.NewReader(src)
		case "base64":
			return base64.NewDecoder(base64.StdEncoding, src)
		default:
			return src
		}
	}

	if strings.HasPrefix(mediaType, "multipart/") && params["boundary"] != "" {
		mr := multipart.NewReader(decodeContent(msg.Body, msg.Header.Get("Content-Transfer-Encoding")), params["boundary"])
		var sb strings.Builder
		for {
			p, err := mr.NextPart()
			if err != nil {
				break
			}
			partReader := decodeContent(p, p.Header.Get("Content-Transfer-Encoding"))
			data, _ := io.ReadAll(partReader)
			sb.Write(data)
			sb.WriteString("\n")
		}
		return sb.String()
	}

	dr := decodeContent(msg.Body, msg.Header.Get("Content-Transfer-Encoding"))
	data, _ := io.ReadAll(dr)
	return string(data)
}

// 從解碼文字中提取確認 URL
func findConfirmURLs(text string) []string {
	// 消除 Quoted-Printable 殘留字元及 HTML Entities
	cleanedText := strings.ReplaceAll(text, "=\r\n", "")
	cleanedText = strings.ReplaceAll(cleanedText, "=\n", "")
	cleanedText = strings.ReplaceAll(cleanedText, "=3D", "=")
	cleanedText = strings.ReplaceAll(cleanedText, "=3d", "=")

	urlRegex := regexp.MustCompile(`https?://suggest\.nkust\.edu\.tw[^\s'"<>]+`)
	matches := urlRegex.FindAllString(cleanedText, -1)

	var filtered []string
	for _, u := range matches {
		uClean := strings.TrimRight(u, ".,;!。，；\\\"')]>")
		uClean = strings.ReplaceAll(uClean, "=3D", "=")
		uClean = strings.ReplaceAll(uClean, "=3d", "=")
		uClean = strings.ReplaceAll(uClean, "&amp;", "&")

		if strings.Contains(uClean, "Confirm") || strings.Contains(uClean, "Message") || strings.Contains(uClean, "Verify") || strings.Contains(uClean, "token") {
			filtered = append(filtered, uClean)
		}
	}
	return filtered
}

// 輪詢收件匣尋找校務建言確認信，並提取確認 URL
func (v *Verifier) PollConfirmationURL(ctx context.Context, timeoutSec int, onProgress func(string)) (string, error) {
	addr := fmt.Sprintf("%s:%d", v.IMAPHost, v.IMAPPort)
	tlsConfig := &tls.Config{
		ServerName:         v.IMAPHost,
		InsecureSkipVerify: false,
	}

	onProgress("正在連線郵件伺服器...")
	c, err := client.DialTLS(addr, tlsConfig)
	if err != nil {
		return "", fmt.Errorf("連線至 IMAP 失敗: %w", err)
	}
	defer c.Logout()

	if err := c.Login(v.Username, v.Password); err != nil {
		return "", fmt.Errorf("登入信箱失敗: %w", err)
	}

	deadline := time.Now().Add(time.Duration(timeoutSec) * time.Second)
	ticker := time.NewTicker(3 * time.Second)
	defer ticker.Stop()

	section := &imap.BodySectionName{}
	items := []imap.FetchItem{section.FetchItem(), imap.FetchEnvelope}

	for {
		select {
		case <-ctx.Done():
			return "", errors.New("已取消郵件監聽")
		case now := <-ticker.C:
			if now.After(deadline) {
				return "", fmt.Errorf("監聽超時 (%d 秒)，未收到確認信件", timeoutSec)
			}
			remain := int(deadline.Sub(now).Seconds())
			onProgress(fmt.Sprintf("正在信箱中搜尋確認信... (剩餘 %d 秒)", remain))

			mbox, err := c.Select("INBOX", false)
			if err != nil || mbox.Messages == 0 {
				continue
			}

			// 抓取最後 15 封信
			from := uint32(1)
			if mbox.Messages > 15 {
				from = mbox.Messages - 14
			}
			to := mbox.Messages
			seqSet := new(imap.SeqSet)
			seqSet.AddRange(from, to)

			messages := make(chan *imap.Message, 20)
			done := make(chan error, 1)
			go func() {
				done <- c.Fetch(seqSet, items, messages)
			}()

			var foundURL string
			var foundSubject string

			for msg := range messages {
				if msg == nil || msg.Envelope == nil {
					continue
				}

				subj := msg.Envelope.Subject
				fromStr := ""
				if len(msg.Envelope.From) > 0 {
					fromStr = msg.Envelope.From[0].PersonalName + msg.Envelope.From[0].MailboxName + "@" + msg.Envelope.From[0].HostName
				}

				isSuggestMail := strings.Contains(subj, "建言") ||
					strings.Contains(subj, "NKUST") ||
					strings.Contains(subj, "校務") ||
					strings.Contains(fromStr, "suggest") ||
					strings.Contains(fromStr, "bboffice")

				if !isSuggestMail {
					continue
				}

				r := msg.GetBody(section)
				if r == nil {
					continue
				}

				bodyText := parseEmailBody(r)
				urls := findConfirmURLs(bodyText)
				if len(urls) > 0 {
					foundURL = urls[len(urls)-1] // 取該信件中最後或最新匹配之 URL
					foundSubject = subj
				}
			}

			_ = <-done

			if foundURL != "" {
				onProgress(fmt.Sprintf("🎉 成功找到確認信！主旨: %s", foundSubject))
				return foundURL, nil
			}
		}
	}
}

// 立即檢查收件匣中最近的一封建言確認信 (支援手動點選「立即檢查並確認」)
func (v *Verifier) CheckLatestConfirmationURL(onProgress func(string)) (string, string, error) {
	addr := fmt.Sprintf("%s:%d", v.IMAPHost, v.IMAPPort)
	tlsConfig := &tls.Config{
		ServerName:         v.IMAPHost,
		InsecureSkipVerify: false,
	}

	onProgress("正在連線郵件伺服器...")
	c, err := client.DialTLS(addr, tlsConfig)
	if err != nil {
		return "", "", fmt.Errorf("連線至 IMAP 失敗: %w", err)
	}
	defer c.Logout()

	if err := c.Login(v.Username, v.Password); err != nil {
		return "", "", fmt.Errorf("登入信箱失敗: %w", err)
	}

	mbox, err := c.Select("INBOX", false)
	if err != nil || mbox.Messages == 0 {
		return "", "", errors.New("收件匣為空或無法讀取")
	}

	from := uint32(1)
	if mbox.Messages > 20 {
		from = mbox.Messages - 19
	}
	to := mbox.Messages
	seqSet := new(imap.SeqSet)
	seqSet.AddRange(from, to)

	section := &imap.BodySectionName{}
	items := []imap.FetchItem{section.FetchItem(), imap.FetchEnvelope}

	messages := make(chan *imap.Message, 25)
	done := make(chan error, 1)
	go func() {
		done <- c.Fetch(seqSet, items, messages)
	}()

	var foundURL string
	var foundSubject string

	for msg := range messages {
		if msg == nil || msg.Envelope == nil {
			continue
		}

		subj := msg.Envelope.Subject
		fromStr := ""
		if len(msg.Envelope.From) > 0 {
			fromStr = msg.Envelope.From[0].PersonalName + msg.Envelope.From[0].MailboxName + "@" + msg.Envelope.From[0].HostName
		}

		isSuggestMail := strings.Contains(subj, "建言") ||
			strings.Contains(subj, "NKUST") ||
			strings.Contains(subj, "校務") ||
			strings.Contains(fromStr, "suggest") ||
			strings.Contains(fromStr, "bboffice")

		if !isSuggestMail {
			continue
		}

		r := msg.GetBody(section)
		if r == nil {
			continue
		}

		bodyText := parseEmailBody(r)
		urls := findConfirmURLs(bodyText)
		if len(urls) > 0 {
			foundURL = urls[len(urls)-1]
			foundSubject = subj
		}
	}
	_ = <-done

	if foundURL == "" {
		return "", "", errors.New("在最近 20 封郵件中未找到任何高科大校務建言驗證信")
	}

	return foundURL, foundSubject, nil
}

// 自動以 HTTP GET 請求打通確認連結
func ConfirmURLDirectly(targetURL string) error {
	u, err := url.Parse(targetURL)
	if err != nil {
		return fmt.Errorf("無效的確認連結網址: %w", err)
	}
	if u.Scheme != "https" && u.Scheme != "http" {
		return errors.New("僅允許 HTTP/HTTPS 協議之確認連結")
	}
	if u.Host != "suggest.nkust.edu.tw" {
		return fmt.Errorf("拒絕向非高科大官方網域發送確認請求 (%s)", u.Host)
	}

	req, err := http.NewRequest("GET", targetURL, nil)
	if err != nil {
		return err
	}
	req.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

	client := &http.Client{Timeout: 15 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("發送確認請求失敗: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 200 && resp.StatusCode < 400 {
		return nil
	}
	return fmt.Errorf("確認連結回傳非預期代碼: HTTP %d", resp.StatusCode)
}
