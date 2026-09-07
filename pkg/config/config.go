package config

import (
	"encoding/json"
	"os"
	"path/filepath"

	"NKUST_Suggest_Desktop/pkg/models"
)

const ConfigFileName = "user_config.json"

func GetConfigPath() string {
	home, err := os.UserConfigDir()
	if err != nil {
		home = "."
	}
	appDir := filepath.Join(home, "NKUST_Suggest_Desktop")
	_ = os.MkdirAll(appDir, 0755)
	return filepath.Join(appDir, ConfigFileName)
}

func DefaultConfig() models.UserConfig {
	return models.UserConfig{
		Name:          "",
		GuestType:     "1",  // 學生
		Email:         "",
		Phone:         "",
		SecrecyType:   "9",  // 不保密
		DoneOpen:      "1",  // 同意公開
		DefaultUnit:   "AA00", // 教務處
		MailPassword:  "",
		HasGoogleAuth: false,
	}
}

func LoadConfig() models.UserConfig {
	cfgPath := GetConfigPath()
	data, err := os.ReadFile(cfgPath)
	if err != nil {
		return DefaultConfig()
	}

	cfg := DefaultConfig()
	if err := json.Unmarshal(data, &cfg); err != nil {
		return DefaultConfig()
	}
	return cfg
}

func SaveConfig(cfg models.UserConfig) error {
	cfgPath := GetConfigPath()
	data, err := json.MarshalIndent(cfg, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(cfgPath, data, 0600)
}
