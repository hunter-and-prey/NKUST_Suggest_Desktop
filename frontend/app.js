// Wails Runtime Bridge 與介面互動控制邏輯
let selectedFiles = [];
let metadata = null;
let currentConfig = null;

// 多語言字典 (中 / 英 / 日)
const I18N = {
  zh: {
    badge: "中",
    title: "高科大校務建言桌面版",
    status_ready: "連線就緒",
    tab_compose: "撰寫與送出建言",
    tab_settings: "常駐個資與信箱授權",
    card_basic_info: "建言基本資訊",
    label_subject: "建言主旨 (必填)",
    placeholder_subject: "請簡要敘述建言核心主旨 (最多20字)",
    label_unit: "受理單位 (必選)",
    loading_units: "載入單位清單中...",
    card_content_info: "建言詳細內容",
    label_content: "內容說明 (限1000字，包含校區、人、事、時、地、物)",
    placeholder_content: "請完整說明反映事項之所屬校區、人、事、時、地、物，俾利承辦單位有效處理及回覆...",
    hint_content_calc: "依高科大建言系統規範：內容每處換行自動以 2 個字元計算。",
    label_attachment: "附件檔案 (最多3個，支援 .pdf, .docx, .txt, 圖片，單檔上限 30MB)",
    btn_choose_files: "點擊選取檔案 (支援多選)",
    card_user_info: "建言人常駐個資 (本地自動記憶，免重複輸入)",
    label_name: "姓名 (必填)",
    placeholder_name: "例如: 王小明",
    label_guest_type: "身分別 (必填)",
    label_email: "接收確認信之信箱 (必填)",
    placeholder_email: "例如: C110123456@nkust.edu.tw 或個人信箱",
    hint_email: "系統將發送一封關鍵確認信至此信箱，完成信箱驗證後建言始正式生效。",
    label_phone: "聯絡電話 (必填)",
    placeholder_phone: "例如: 0912345678",
    label_secrecy: "個資是否保密",
    label_done_open: "結案是否公開查詢",
    card_auth_info: "高科大學生 Google 信箱自動驗證授權",
    label_mail_password: "Google 應用程式密碼 (App Password)",
    placeholder_mail_password: "16碼 Google 應用程式密碼 (例如: abcd efgh ijkl mnop)",
    hint_mail_password: "若使用 Google 託管之學生信箱，請至 Google 帳戶安全性建立「應用程式密碼」填入。密碼僅於本機記憶體驗證，安全保密。",
    btn_check_email: "立即檢查信箱並自動確認",
    btn_save_config: "💾 儲存設定至本機",
    footer_status_default: "✨ 填寫完成後點擊右方按鈕即可一鍵提交與驗證",
    btn_cancel_verify: "取消監聽",
    btn_submit_cta: "一鍵送出建言並自動確認",
    checking_email: "正在連接信箱檢查確認信...",
    prompt_email_required: "請先輸入您的信箱地址！",
    prompt_pwd_required: "請輸入 Google 應用程式密碼 (16碼 App Password)！",
    prompt_subject_required: "請輸入建言主旨！",
    prompt_unit_required: "請選擇受理單位！",
    prompt_content_required: "請填寫建言內容！",
    prompt_profile_required: "請先在『常駐個資與信箱授權』分頁填寫您的姓名、信箱與電話！",
    msg_saved_local: "設定已在本機暫存！",
    counter_remain: "剩餘",
    counter_words: "字",
    counter_over: "已超出",
    units_map: {
      "AA00": "教務處", "SA00": "學務處", "GA00": "總務處", "RA00": "研發處",
      "SD00": "永續發展處", "RE00": "產學處", "SG00": "財務處", "SH00": "海洋科技發展處",
      "RB00": "國際事務處", "SL00": "教推與經管處", "YG00": "綜合業務處", "SJ00": "海訓處",
      "SK00": "實習船營運辦公室", "SE00": "秘書室", "LB00": "圖書館", "PE00": "人事室",
      "AC00": "主計室", "PH00": "體育室", "RD00": "校友服務就業中心", "GB00": "環境安全衛生中心",
      "IC00": "電算與網路中心", "IE00": "創新創業發展處", "UY00": "智慧機電學院", "UU00": "工學院",
      "IN00": "國際學院", "UB00": "電資學院", "US00": "水圈學院", "UT00": "商業智慧學院",
      "UR00": "海事學院", "UW00": "管理學院", "UV00": "海商學院", "UP00": "財金學院",
      "UH00": "人文社會學院", "UJ00": "外語學院", "AT00": "進修學院", "XB00": "共同教育學院",
      "UA00": "創新設計學院", "ZZ00": "其他單位"
    },
    guest_types: { "1": "學生", "2": "教職同仁", "4": "校友", "5": "家長", "6": "民眾" },
    secrecy_types: { "9": "不保密 (預設)", "1": "保密，個資去識別化", "2": "保密，同意承辦單位聯繫" },
    done_open_types: { "1": "同意公開 (預設)", "2": "不同意公開" }
  },
  en: {
    badge: "EN",
    title: "NKUST Suggest Desktop",
    status_ready: "Connected & Ready",
    tab_compose: "Compose & Submit",
    tab_settings: "Profile & Mail Auth",
    card_basic_info: "Basic Information",
    label_subject: "Subject (Required)",
    placeholder_subject: "Briefly summarize your suggestion (max 20 chars)",
    label_unit: "Recipient Department (Required)",
    loading_units: "Loading departments...",
    card_content_info: "Detailed Suggestion",
    label_content: "Content (Max 1000 words; include campus, who, what, when, where)",
    placeholder_content: "Please clearly describe the campus, people, events, time, and location involved...",
    hint_content_calc: "Per NKUST policy: each line break is calculated as 2 characters.",
    label_attachment: "Attachments (Max 3 files; supports .pdf, .docx, .txt, images, max 30MB each)",
    btn_choose_files: "Click to select files (Multi-select supported)",
    card_user_info: "Resident User Profile (Auto-saved locally)",
    label_name: "Full Name (Required)",
    placeholder_name: "e.g. John Doe",
    label_guest_type: "Identity (Required)",
    label_email: "Verification Email (Required)",
    placeholder_email: "e.g. C110123456@nkust.edu.tw or personal email",
    hint_email: "The system sends a crucial verification email to this address. Verification activates the case.",
    label_phone: "Contact Phone (Required)",
    placeholder_phone: "e.g. 0912345678",
    label_secrecy: "Privacy / Confidentiality",
    label_done_open: "Public Query on Closed Case",
    card_auth_info: "NKUST Student Google Mail Auto-Verification",
    label_mail_password: "Google App Password (16 chars)",
    placeholder_mail_password: "16-character Google App Password (e.g. abcd efgh ijkl mnop)",
    hint_mail_password: "For Google-hosted student mail, generate an App Password in your Google Account Security settings.",
    btn_check_email: "Check Mailbox & Auto-Confirm",
    btn_save_config: "💾 Save Profile Locally",
    footer_status_default: "✨ Fill out the form and click the button on the right to submit and verify",
    btn_cancel_verify: "Cancel Listening",
    btn_submit_cta: "Submit Suggestion & Auto-Confirm",
    checking_email: "Connecting to mailbox to check confirmation email...",
    prompt_email_required: "Please enter your email address first!",
    prompt_pwd_required: "Please enter your 16-character Google App Password!",
    prompt_subject_required: "Please enter a subject for your suggestion!",
    prompt_unit_required: "Please select a recipient department!",
    prompt_content_required: "Please fill in the suggestion content!",
    prompt_profile_required: "Please fill in your name, email, and phone in the Profile & Auth tab first!",
    msg_saved_local: "Settings saved locally!",
    counter_remain: "Remaining",
    counter_words: "chars",
    counter_over: "Exceeded by",
    units_map: {
      "AA00": "Office of Academic Affairs", "SA00": "Office of Student Affairs", "GA00": "Office of General Affairs",
      "RA00": "Office of R&D", "SD00": "Office of Sustainable Development", "RE00": "Industry-Academia Collaboration",
      "SG00": "Office of Finance", "SH00": "Marine Technology Development", "RB00": "Office of International Affairs",
      "SL00": "Continuing Education & Management", "YG00": "Comprehensive Affairs", "SJ00": "Maritime Training Office",
      "SK00": "Training Ship Operations", "SE00": "Secretariat", "LB00": "Library", "PE00": "Personnel Office",
      "AC00": "Accounting Office", "PH00": "Physical Education Office", "RD00": "Alumni Service & Career Center",
      "GB00": "Environmental Safety & Health Center", "IC00": "Computer & Network Center", "IE00": "Innovation & Entrepreneurship",
      "UY00": "College of Mechatronics", "UU00": "College of Engineering", "IN00": "International College",
      "UB00": "College of Electrical & Computer Eng.", "US00": "College of Hydrosphere Science", "UT00": "College of Business Intelligence",
      "UR00": "College of Maritime", "UW00": "College of Management", "UV00": "College of Marine Commerce",
      "UP00": "College of Finance & Banking", "UH00": "College of Humanities & Social Sciences", "UJ00": "College of Foreign Languages",
      "AT00": "College of Continuing Education", "XB00": "General Education College", "UA00": "College of Creative Design",
      "ZZ00": "Other Departments"
    },
    guest_types: { "1": "Student", "2": "Faculty / Staff", "4": "Alumni", "5": "Parent", "6": "Public" },
    secrecy_types: { "9": "Not Confidential (Default)", "1": "Confidential (Anonymized)", "2": "Confidential (Contact allowed)" },
    done_open_types: { "1": "Publicly Viewable (Default)", "2": "Not Publicly Viewable" }
  },
  ja: {
    badge: "日",
    title: "高科大校務提言デスクトップ",
    status_ready: "接続完了・待機中",
    tab_compose: "提言作成・送信",
    tab_settings: "登録情報・メール認証設定",
    card_basic_info: "提言基本情報",
    label_subject: "提言件名 (必須)",
    placeholder_subject: "提言の核心テーマを簡潔に入力 (最大20文字)",
    label_unit: "受付窓口・部署 (必須)",
    loading_units: "部署リスト読み込み中...",
    card_content_info: "提言詳細内容",
    label_content: "内容詳細 (最大1000文字。キャンパス、関係者、日時、場所等)",
    placeholder_content: "関係するキャンパス、関係者、事象、日時、場所を具体的に記載してください...",
    hint_content_calc: "NKUSTシステム規約：改行1回につき2文字として換算されます。",
    label_attachment: "添付ファイル (最大3件、.pdf, .docx, .txt, 画像対応、各30MB以内)",
    btn_choose_files: "ファイルを選択 (複数選択可)",
    card_user_info: "基本個人情報 (ローカル自動保存)",
    label_name: "氏名 (必須)",
    placeholder_name: "例: 山田 太郎",
    label_guest_type: "身分別 (必須)",
    label_email: "認証メール受信先 (必須)",
    placeholder_email: "例: C110123456@nkust.edu.tw または 個人アドレス",
    hint_email: "システムから本アドレスへ重要な認証メールが送信されます。認証完了後に提言が正式受理されます。",
    label_phone: "電話番号 (必須)",
    placeholder_phone: "例: 0912345678",
    label_secrecy: "個人情報の保護設定",
    label_done_open: "完了後の公開可否",
    card_auth_info: "学生Googleメール自動認証設定",
    label_mail_password: "Google アプリパスワード (16文字)",
    placeholder_mail_password: "16桁の Google アプリパスワード (例: abcd efgh ijkl mnop)",
    hint_mail_password: "大学のGoogleアカウントで「アプリ パスワード」を発行して入力してください。本機でのみ安全に利用されます。",
    btn_check_email: "メールを確認して自動認証",
    btn_save_config: "💾 設定をローカルに保存",
    footer_status_default: "✨ 入力完了後、右側のボタンをクリックして一括送信・自動認証",
    btn_cancel_verify: "受信待機を中止",
    btn_submit_cta: "提言を送信して自動確認",
    checking_email: "メールサーバーに接続して確認メールを検索中...",
    prompt_email_required: "メールアドレスを入力してください！",
    prompt_pwd_required: "Google アプリパスワード (16桁) を入力してください！",
    prompt_subject_required: "提言の件名を入力してください！",
    prompt_unit_required: "受付窓口を選択してください！",
    prompt_content_required: "提言内容を入力してください！",
    prompt_profile_required: "先に「登録情報・メール認証設定」で氏名、メール、電話番号を入力してください！",
    msg_saved_local: "設定を本機に保存しました！",
    counter_remain: "残り",
    counter_words: "文字",
    counter_over: "超過",
    units_map: {
      "AA00": "教務処 (教務課)", "SA00": "学務処 (学生課)", "GA00": "総務処 (総務課)",
      "RA00": "研究開発処", "SD00": "持続可能開発処", "RE00": "産学連携処",
      "SG00": "財務処", "SH00": "海洋科学技術発展処", "RB00": "国際事務処 (国際交流課)",
      "SL00": "生涯教育・経営処", "YG00": "総合業務処", "SJ00": "海洋訓練処",
      "SK00": "実習船運航オフィス", "SE00": "秘書室", "LB00": "図書館", "PE00": "人事室",
      "AC00": "主計室 (会計課)", "PH00": "体育室", "RD00": "同窓会・就職支援センター",
      "GB00": "環境安全衛生センター", "IC00": "情報ネットワークセンター", "IE00": "イノベーション創業処",
      "UY00": "知能メカトロニクス学部", "UU00": "工学部", "IN00": "国際学部",
      "UB00": "電気・情報工学部", "US00": "水圏科学部", "UT00": "ビジネスインテリジェンス学部",
      "UR00": "海事学部", "UW00": "経営管理学部", "UV00": "海洋商学部",
      "UP00": "金融・ファイナンス学部", "UH00": "人文社会学部", "UJ00": "外国語学部",
      "AT00": "生涯学習学部", "XB00": "教養教育学部", "UA00": "イノベーションデザイン学部",
      "ZZ00": "その他の窓口"
    },
    guest_types: { "1": "学生", "2": "教職員", "4": "卒業生", "5": "保護者", "6": "一般" },
    secrecy_types: { "9": "非公開にしない (デフォルト)", "1": "個人情報を匿名化", "2": "案件対応時の連絡に同意" },
    done_open_types: { "1": "一般公開に同意 (デフォルト)", "2": "非公開を希望" }
  }
};

let currentLang = localStorage.getItem('app_lang') || 'zh';
const LANG_CYCLE = ['zh', 'en', 'ja'];

document.addEventListener('DOMContentLoaded', async () => {
  setupLanguage();
  setupTheme();
  setupTabs();
  setupCounters();
  await loadAppData();
  setupEventListeners();
});

// 多語言循環切換 (中 -> 英 -> 日 -> 中)
function setupLanguage() {
  const btn = document.getElementById('btnLangToggle');
  applyLanguage(currentLang);

  if (btn) {
    btn.addEventListener('click', () => {
      const idx = LANG_CYCLE.indexOf(currentLang);
      currentLang = LANG_CYCLE[(idx + 1) % LANG_CYCLE.length];
      localStorage.setItem('app_lang', currentLang);
      applyLanguage(currentLang);
      renderMetadataOptions();
      updateSubjectCount();
      updateContentCount();
    });
  }
}

function applyLanguage(lang) {
  const dict = I18N[lang] || I18N.zh;
  const langText = document.getElementById('langText');
  if (langText) langText.textContent = dict.badge;

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key]) el.textContent = dict[key];
  });

  document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
    const key = el.getAttribute('data-i18n-placeholder');
    if (dict[key]) el.placeholder = dict[key];
  });

  document.title = dict.title;
}

// 深色模式與主題切換
function setupTheme() {
  const toggleBtn = document.getElementById('btnThemeToggle');
  const themeIcon = document.getElementById('themeIcon');
  const savedTheme = localStorage.getItem('theme');
  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

  const currentTheme = savedTheme || (prefersDark ? 'dark' : 'light');
  applyTheme(currentTheme);

  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const activeTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      applyTheme(activeTheme);
      localStorage.setItem('theme', activeTheme);
    });
  }
}

function applyTheme(theme) {
  const themeIcon = document.getElementById('themeIcon');
  if (theme === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark');
    if (themeIcon) themeIcon.textContent = '☀️';
  } else {
    document.documentElement.removeAttribute('data-theme');
    if (themeIcon) themeIcon.textContent = '🌙';
  }
}

// 分頁切換
function setupTabs() {
  const tabs = document.querySelectorAll('.tab-btn');
  tabs.forEach(btn => {
    btn.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const target = btn.getAttribute('data-tab');
      document.getElementById(target).classList.add('active');
    });
  });
}

// 即時字數統計、換行折算與草稿自動暫存
function updateSubjectCount() {
  const inputSubject = document.getElementById('inputSubject');
  const subjectCounter = document.getElementById('subjectCounter');
  if (!inputSubject || !subjectCounter) return;
  const dict = I18N[currentLang] || I18N.zh;
  const len = inputSubject.value.length;
  subjectCounter.textContent = `${len} / 20 ${dict.counter_words}`;
  if (len >= 20) {
    subjectCounter.className = 'label-counter limit-near';
  } else {
    subjectCounter.className = 'label-counter';
  }
}

function updateContentCount() {
  const inputContent = document.getElementById('inputContent');
  const contentCounter = document.getElementById('contentCounter');
  if (!inputContent || !contentCounter) return;
  const dict = I18N[currentLang] || I18N.zh;
  const val = inputContent.value;
  const calcLen = val.replace(/\r\n/g, "  ").replace(/\n/g, "  ").replace(/\r/g, "  ").length;
  const remain = 1000 - calcLen;

  if (remain < 0) {
    contentCounter.textContent = `${dict.counter_over} ${-remain} ${dict.counter_words} (${calcLen}/1000)`;
    contentCounter.className = 'label-counter limit-over';
  } else if (remain < 100) {
    contentCounter.textContent = `${dict.counter_remain} ${remain} ${dict.counter_words} (${calcLen}/1000)`;
    contentCounter.className = 'label-counter limit-near';
  } else {
    contentCounter.textContent = `${dict.counter_remain} ${remain} ${dict.counter_words}`;
    contentCounter.className = 'label-counter';
  }
}

function setupCounters() {
  const inputSubject = document.getElementById('inputSubject');
  const inputContent = document.getElementById('inputContent');

  // 自動恢復草稿
  const savedSubject = localStorage.getItem('draft_subject');
  if (savedSubject && !inputSubject.value) {
    inputSubject.value = savedSubject;
  }
  const savedContent = localStorage.getItem('draft_content');
  if (savedContent && !inputContent.value) {
    inputContent.value = savedContent;
  }

  updateSubjectCount();
  updateContentCount();

  inputSubject.addEventListener('input', () => {
    updateSubjectCount();
    localStorage.setItem('draft_subject', inputSubject.value);
  });

  inputContent.addEventListener('input', () => {
    updateContentCount();
    localStorage.setItem('draft_content', inputContent.value);
  });
}

// 載入應用程式初始資料
async function loadAppData() {
  try {
    // 檢查是否有 Wails 後端綁定
    if (window.go && window.go.main && window.go.main.App) {
      metadata = await window.go.main.App.GetMetadata();
      currentConfig = await window.go.main.App.LoadConfig();

      // 監聽進度廣播事件
      window.runtime.EventsOn('progress_update', onProgressUpdate);
    } else {
      // 離線預設資料 (供瀏覽器單純預覽時使用)
      metadata = {
        units: [{ id: "AA00", name: "教務處" }, { id: "SA00", name: "學務處" }, { id: "GA00", name: "總務處" }],
        guest_types: [{ key: "1", label: "學生" }, { key: "2", label: "教職同仁" }, { key: "4", label: "校友" }],
        secrecy_types: [{ key: "9", label: "不保密 (預設)" }, { key: "1", label: "保密，去識別化" }, { key: "2", label: "保密，同意聯繫" }],
        done_open: [{ key: "1", label: "同意公開 (預設)" }, { key: "2", label: "不同意公開" }]
      };
      currentConfig = {
        name: "", guest_type: "1", email: "", phone: "", secrecy_type: "9", done_open: "1", default_unit: "AA00"
      };
    }

    renderMetadataOptions();
    populateConfigFields();
  } catch (err) {
    console.error("載入資料失敗:", err);
  }
}

// 渲染單位與單選按鈕 (依使用者要求：去除代號，僅顯示單位名稱；並依當前語言動態顯示)
function renderMetadataOptions() {
  const selectUnit = document.getElementById('selectUnit');
  if (!selectUnit || !metadata) return;
  const currentVal = selectUnit.value;
  selectUnit.replaceChildren();

  const dict = I18N[currentLang] || I18N.zh;

  metadata.units.forEach(u => {
    const opt = document.createElement('option');
    opt.value = u.id;
    // 依使用者需求：旁邊的代號 (AA00 等) 不需要，僅呈現純淨名稱，並支援多語切換
    const localizedName = (dict.units_map && dict.units_map[u.id]) ? dict.units_map[u.id] : u.name;
    opt.textContent = localizedName;
    selectUnit.appendChild(opt);
  });

  if (currentVal) {
    selectUnit.value = currentVal;
  }

  // 渲染多語言身分、保密、結案公開選項
  const localizedGuestTypes = metadata.guest_types.map(item => ({
    key: item.key,
    label: (dict.guest_types && dict.guest_types[item.key]) || item.label
  }));
  const localizedSecrecyTypes = metadata.secrecy_types.map(item => ({
    key: item.key,
    label: (dict.secrecy_types && dict.secrecy_types[item.key]) || item.label
  }));
  const localizedDoneOpen = metadata.done_open.map(item => ({
    key: item.key,
    label: (dict.done_open_types && dict.done_open_types[item.key]) || item.label
  }));

  const savedGuest = getRadioValue('guest_type');
  const savedSec = getRadioValue('secrecy_type');
  const savedDone = getRadioValue('done_open');

  renderRadios('guestTypesContainer', 'guest_type', localizedGuestTypes);
  renderRadios('secrecyTypesContainer', 'secrecy_type', localizedSecrecyTypes);
  renderRadios('doneOpenTypesContainer', 'done_open', localizedDoneOpen);

  if (savedGuest) setRadioValue('guest_type', savedGuest);
  if (savedSec) setRadioValue('secrecy_type', savedSec);
  if (savedDone) setRadioValue('done_open', savedDone);
}

function renderRadios(containerId, name, items) {
  const container = document.getElementById(containerId);
  container.replaceChildren();
  items.forEach(item => {
    const lbl = document.createElement('label');
    lbl.className = 'radio-label';

    const input = document.createElement('input');
    input.type = 'radio';
    input.name = name;
    input.value = item.key;

    const span = document.createElement('span');
    span.textContent = item.label;

    lbl.appendChild(input);
    lbl.appendChild(span);
    container.appendChild(lbl);
  });
}

// 帶入常駐設定
function populateConfigFields() {
  if (!currentConfig) return;
  document.getElementById('inputName').value = currentConfig.name || '';
  document.getElementById('inputEmail').value = currentConfig.email || '';
  document.getElementById('inputPhone').value = currentConfig.phone || '';
  document.getElementById('inputMailPassword').value = currentConfig.mail_password || '';

  setRadioValue('guest_type', currentConfig.guest_type || '1');
  setRadioValue('secrecy_type', currentConfig.secrecy_type || '9');
  setRadioValue('done_open', currentConfig.done_open || '1');

  if (currentConfig.default_unit) {
    document.getElementById('selectUnit').value = currentConfig.default_unit;
  }
}

function setRadioValue(name, val) {
  const r = document.querySelector(`input[name="${name}"][value="${val}"]`);
  if (r) r.checked = true;
}

function getRadioValue(name) {
  const r = document.querySelector(`input[name="${name}"]:checked`);
  return r ? r.value : '';
}

// 事件監聽綁定
function setupEventListeners() {
  // 選取附件檔案
  document.getElementById('btnChooseFiles').addEventListener('click', async () => {
    if (window.go && window.go.main && window.go.main.App) {
      const files = await window.go.main.App.SelectFiles();
      if (files && files.length > 0) {
        selectedFiles = files;
        renderSelectedFiles();
      }
    } else {
      alert("桌面檔案選擇器需要於原生視窗環境執行！");
    }
  });

  // 儲存設定
  document.getElementById('btnSaveConfig').addEventListener('click', async () => {
    const cfg = {
      name: document.getElementById('inputName').value.trim(),
      guest_type: getRadioValue('guest_type'),
      email: document.getElementById('inputEmail').value.trim(),
      phone: document.getElementById('inputPhone').value.trim(),
      secrecy_type: getRadioValue('secrecy_type'),
      done_open: getRadioValue('done_open'),
      default_unit: document.getElementById('selectUnit').value,
      mail_password: document.getElementById('inputMailPassword').value.trim()
    };

    if (window.go && window.go.main && window.go.main.App) {
      const res = await window.go.main.App.SaveConfig(cfg);
      alert(res.message);
    } else {
      alert("設定已在本機暫存！");
    }
  });

  // 立即檢查信箱並自動確認最新信件
  const btnCheckLatestEmail = document.getElementById('btnCheckLatestEmail');
  if (btnCheckLatestEmail) {
    btnCheckLatestEmail.addEventListener('click', async () => {
      const dict = I18N[currentLang] || I18N.zh;
      const email = document.getElementById('inputEmail').value.trim();
      const mailPassword = document.getElementById('inputMailPassword').value.trim();

      if (!email) {
        alert(dict.prompt_email_required);
        return;
      }
      if (!mailPassword) {
        alert(dict.prompt_pwd_required);
        return;
      }

      btnCheckLatestEmail.disabled = true;
      const progressContainer = document.getElementById('progressContainer');
      const progressBar = document.getElementById('progressBar');
      const statusText = document.getElementById('actionStatus');

      progressContainer.style.display = 'block';
      progressBar.style.width = '30%';
      statusText.textContent = dict.checking_email;
      statusText.style.color = '#3b82f6';

      try {
        if (window.go && window.go.main && window.go.main.App) {
          const res = await window.go.main.App.CheckAndVerifyLatestEmail(email, mailPassword);
          alert(res.message);
        } else {
          alert("模擬檢查完成！(需在原生視窗中連線信箱)");
        }
      } catch (err) {
        alert("檢查信箱發生錯誤: " + err);
      } finally {
        btnCheckLatestEmail.disabled = false;
        progressContainer.style.display = 'none';
      }
    });
  }

  // 取消驗證
  document.getElementById('btnCancelVerify').addEventListener('click', () => {
    if (window.go && window.go.main && window.go.main.App) {
      window.go.main.App.CancelVerification();
    }
  });

  // 一鍵送出建言
  document.getElementById('btnSubmit').addEventListener('click', handleSubmit);
}

function renderSelectedFiles() {
  const container = document.getElementById('fileTagsContainer');
  container.replaceChildren();
  selectedFiles.forEach((path, idx) => {
    const filename = path.split('\\').pop().split('/').pop();
    const tag = document.createElement('div');
    tag.className = 'file-tag';

    const spanText = document.createElement('span');
    spanText.textContent = `📎 ${filename}`;

    const spanRemove = document.createElement('span');
    spanRemove.className = 'file-tag-remove';
    spanRemove.textContent = '✕';
    spanRemove.setAttribute('data-idx', String(idx));
    spanRemove.addEventListener('click', () => {
      selectedFiles.splice(idx, 1);
      renderSelectedFiles();
    });

    tag.appendChild(spanText);
    tag.appendChild(spanRemove);
    container.appendChild(tag);
  });
}

// 提交處理
async function handleSubmit() {
  const dict = I18N[currentLang] || I18N.zh;
  const subject = document.getElementById('inputSubject').value.trim();
  const unitId = document.getElementById('selectUnit').value;
  const content = document.getElementById('inputContent').value.trim();

  const name = document.getElementById('inputName').value.trim();
  const email = document.getElementById('inputEmail').value.trim();
  const phone = document.getElementById('inputPhone').value.trim();
  const mailPassword = document.getElementById('inputMailPassword').value.trim();

  if (!subject) {
    alert(dict.prompt_subject_required);
    return;
  }
  if (!unitId) {
    alert(dict.prompt_unit_required);
    return;
  }
  if (!content) {
    alert(dict.prompt_content_required);
    return;
  }
  if (!name || !email || !phone) {
    alert(dict.prompt_profile_required);
    return;
  }

  const req = {
    subject,
    unit_id: unitId,
    message_content: content,
    file_paths: selectedFiles,
    name,
    guest_type: getRadioValue('guest_type'),
    email,
    phone,
    secrecy_type: getRadioValue('secrecy_type'),
    done_open: getRadioValue('done_open')
  };

  const btnSubmit = document.getElementById('btnSubmit');
  const btnCancel = document.getElementById('btnCancelVerify');
  const progressContainer = document.getElementById('progressContainer');
  const progressBar = document.getElementById('progressBar');

  btnSubmit.disabled = true;
  btnCancel.style.display = 'inline-block';
  progressContainer.style.display = 'block';
  progressBar.style.width = '10%';

  try {
    if (window.go && window.go.main && window.go.main.App) {
      const res = await window.go.main.App.SubmitSuggestion(req, email, mailPassword);
      alert(res.message);
      if (res.success) {
        document.getElementById('inputSubject').value = '';
        document.getElementById('inputContent').value = '';
        localStorage.removeItem('draft_subject');
        localStorage.removeItem('draft_content');
        selectedFiles = [];
        renderSelectedFiles();
        // 立即同步重設剩餘字數指示器，避免數字停留在送出前的狀態
        updateSubjectCount();
        updateContentCount();
      }
    } else {
      alert("模擬提交成功！(需於原生視窗中發送實體 HTTP 與信箱連線)");
      document.getElementById('inputSubject').value = '';
      document.getElementById('inputContent').value = '';
      localStorage.removeItem('draft_subject');
      localStorage.removeItem('draft_content');
      selectedFiles = [];
      renderSelectedFiles();
      updateSubjectCount();
      updateContentCount();
    }
  } catch (err) {
    alert("執行過程發生錯誤: " + err);
  } finally {
    btnSubmit.disabled = false;
    btnCancel.style.display = 'none';
    progressContainer.style.display = 'none';
  }
}

// 接收後端即時進度廣播
function onProgressUpdate(ev) {
  const statusText = document.getElementById('actionStatus');
  const progressBar = document.getElementById('progressBar');
  statusText.textContent = ev.message;
  progressBar.style.width = `${ev.progress}%`;

  if (ev.is_error) {
    statusText.style.color = '#ef4444';
  } else if (ev.done) {
    statusText.style.color = '#10b981';
  } else {
    statusText.style.color = '#64748b';
  }
}
