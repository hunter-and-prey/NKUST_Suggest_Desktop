#!/usr/bin/env bash
set -e

echo "=== 正在建置 高科大校務建言桌面版 (macOS Universal 版) ==="

# 檢查是否安裝 Go
if ! command -v go &> /dev/null; then
    echo "未檢測到 Go 環境，請先使用 Homebrew 安裝: brew install go"
    exit 1
fi

# 檢查是否安裝 Wails CLI
if ! command -v wails &> /dev/null; then
    echo "正在安裝 Wails CLI..."
    go install github.com/wailsapp/wails/v2/cmd/wails@latest
    export PATH="$(go env GOPATH)/bin:$PATH"
fi

# 打包支援 Intel 與 Apple Silicon 的 Universal 雙架構 App
echo "開始編譯 macOS 原生應用程式 (Intel x86_64 + Apple Silicon M系列)..."
wails build -platform darwin/universal -clean

echo ""
echo "=== 🎉 建置成功！ ==="
echo "應用程式已產出於: build/bin/高科大校務建言桌面版.app"
echo "在 Finder 中直接雙擊即可執行！"
