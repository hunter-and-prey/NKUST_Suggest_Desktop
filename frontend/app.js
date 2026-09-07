// Wails Runtime Bridge 與介面互動控制邏輯
let selectedFiles = [];
let metadata = null;
let currentConfig = null;

document.addEventListener('DOMContentLoaded', async () => {
  setupTheme();
  setupTabs();
  setupCounters();
  await loadAppData();
  setupEventListeners();
});

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
  const len = inputSubject.value.length;
  subjectCounter.textContent = `${len} / 20 字`;
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
  const val = inputContent.value;
  const calcLen = val.replace(/\r\n/g, "  ").replace(/\n/g, "  ").replace(/\r/g, "  ").length;
  const remain = 1000 - calcLen;

  if (remain < 0) {
    contentCounter.textContent = `已超出 ${-remain} 字！(換行折算後: ${calcLen}/1000)`;
    contentCounter.className = 'label-counter limit-over';
  } else if (remain < 100) {
    contentCounter.textContent = `剩餘 ${remain} 字 (換行折算後: ${calcLen}/1000)`;
    contentCounter.className = 'label-counter limit-near';
  } else {
    contentCounter.textContent = `剩餘 ${remain} 字`;
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

// 渲染單位與單選按鈕
function renderMetadataOptions() {
  const selectUnit = document.getElementById('selectUnit');
  selectUnit.replaceChildren();
  metadata.units.forEach(u => {
    const opt = document.createElement('option');
    opt.value = u.id;
    opt.textContent = `${u.name} (${u.id})`;
    selectUnit.appendChild(opt);
  });

  renderRadios('guestTypesContainer', 'guest_type', metadata.guest_types);
  renderRadios('secrecyTypesContainer', 'secrecy_type', metadata.secrecy_types);
  renderRadios('doneOpenTypesContainer', 'done_open', metadata.done_open);
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
      const email = document.getElementById('inputEmail').value.trim();
      const mailPassword = document.getElementById('inputMailPassword').value.trim();

      if (!email) {
        alert("請先輸入您的信箱地址！");
        return;
      }
      if (!mailPassword) {
        alert("請輸入 Google 應用程式密碼 (16碼 App Password)！");
        return;
      }

      btnCheckLatestEmail.disabled = true;
      const progressContainer = document.getElementById('progressContainer');
      const progressBar = document.getElementById('progressBar');
      const statusText = document.getElementById('actionStatus');

      progressContainer.style.display = 'block';
      progressBar.style.width = '30%';
      statusText.textContent = '正在連接信箱檢查確認信...';
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
  const subject = document.getElementById('inputSubject').value.trim();
  const unitId = document.getElementById('selectUnit').value;
  const content = document.getElementById('inputContent').value.trim();

  const name = document.getElementById('inputName').value.trim();
  const email = document.getElementById('inputEmail').value.trim();
  const phone = document.getElementById('inputPhone').value.trim();
  const mailPassword = document.getElementById('inputMailPassword').value.trim();

  if (!subject) {
    alert("請輸入建言主旨！");
    return;
  }
  if (!unitId) {
    alert("請選擇受理單位！");
    return;
  }
  if (!content) {
    alert("請填寫建言內容！");
    return;
  }
  if (!name || !email || !phone) {
    alert("請先在『常駐個資與信箱授權』分頁填寫您的姓名、信箱與電話！");
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
