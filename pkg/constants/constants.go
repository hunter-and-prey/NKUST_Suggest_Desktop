package constants

const (
	CreateURL = "https://suggest.nkust.edu.tw/Message/Create"
	BaseURL   = "https://suggest.nkust.edu.tw"
)

type UnitItem struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type KeyValueItem struct {
	Key   string `json:"key"`
	Label string `json:"label"`
}

// 身分別
var GuestTypes = []KeyValueItem{
	{Key: "1", Label: "學生"},
	{Key: "2", Label: "教職同仁"},
	{Key: "4", Label: "校友"},
	{Key: "5", Label: "家長"},
	{Key: "6", Label: "民眾"},
}

// 保密設定
var SecrecyTypes = []KeyValueItem{
	{Key: "9", Label: "不保密 (預設)"},
	{Key: "1", Label: "保密，個資去識別化處理"},
	{Key: "2", Label: "保密，若案件處理需要，同意承辦單位與我聯繫"},
}

// 結案是否公開
var DoneOpenTypes = []KeyValueItem{
	{Key: "1", Label: "同意公開 (預設)"},
	{Key: "2", Label: "不同意公開"},
}

// 高科大 38 個受理事處學院
var Units = []UnitItem{
	{ID: "AA00", Name: "教務處"},
	{ID: "SA00", Name: "學務處"},
	{ID: "GA00", Name: "總務處"},
	{ID: "RA00", Name: "研發處"},
	{ID: "SD00", Name: "永續發展處"},
	{ID: "RE00", Name: "產學處"},
	{ID: "SG00", Name: "財務處"},
	{ID: "SH00", Name: "海洋科技發展處"},
	{ID: "RB00", Name: "國際事務處"},
	{ID: "SL00", Name: "教推與經管處"},
	{ID: "YG00", Name: "綜合業務處"},
	{ID: "SJ00", Name: "海訓處"},
	{ID: "SK00", Name: "實習船營運辦公室"},
	{ID: "SE00", Name: "秘書室"},
	{ID: "LB00", Name: "圖書館"},
	{ID: "PE00", Name: "人事室"},
	{ID: "AC00", Name: "主計室"},
	{ID: "PH00", Name: "體育室"},
	{ID: "RD00", Name: "校友服務就業中心"},
	{ID: "GB00", Name: "環境安全衛生中心"},
	{ID: "IC00", Name: "電算與網路中心"},
	{ID: "IE00", Name: "創新創業發展處"},
	{ID: "UY00", Name: "智慧機電學院"},
	{ID: "UU00", Name: "工學院"},
	{ID: "IN00", Name: "國際學院(任編)"},
	{ID: "UB00", Name: "電資學院"},
	{ID: "US00", Name: "水圈學院"},
	{ID: "UT00", Name: "商業智慧學院"},
	{ID: "UR00", Name: "海事學院"},
	{ID: "UW00", Name: "管理學院"},
	{ID: "UV00", Name: "海商學院"},
	{ID: "UP00", Name: "財金學院"},
	{ID: "UH00", Name: "人文社會學院"},
	{ID: "UJ00", Name: "外語學院"},
	{ID: "AT00", Name: "進修學院"},
	{ID: "XB00", Name: "共同教育學院"},
	{ID: "UA00", Name: "創新設計學院"},
	{ID: "ZZ00", Name: "其他單位"},
}
