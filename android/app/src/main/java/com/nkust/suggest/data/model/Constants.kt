package com.nkust.suggest.data.model

object Constants {
    const val BASE_URL = "https://suggest.nkust.edu.tw"
    const val CREATE_URL = "https://suggest.nkust.edu.tw/Message/Create"
    const val IMAP_HOST = "imap.gmail.com"
    const val IMAP_PORT = 993

    val UNITS = listOf(
        UnitItem("AA00", "教務處", "Office of Academic Affairs", "教務処 (教務課)"),
        UnitItem("SA00", "學務處", "Office of Student Affairs", "学務処 (学生課)"),
        UnitItem("GA00", "總務處", "Office of General Affairs", "総務処 (総務課)"),
        UnitItem("RA00", "研發處", "Office of R&D", "研究開発処"),
        UnitItem("SD00", "永續發展處", "Office of Sustainable Development", "持続可能開発処"),
        UnitItem("RE00", "產學處", "Industry-Academia Collaboration", "産学連携処"),
        UnitItem("SG00", "財務處", "Office of Finance", "財務処"),
        UnitItem("SH00", "海洋科技發展處", "Marine Technology Development", "海洋科学技術発展処"),
        UnitItem("RB00", "國際事務處", "Office of International Affairs", "国際事務処 (国際交流課)"),
        UnitItem("SL00", "教推與經管處", "Continuing Education & Management", "生涯教育・経営処"),
        UnitItem("YG00", "綜合業務處", "Comprehensive Affairs", "総合業務処"),
        UnitItem("SJ00", "海訓處", "Maritime Training Office", "海洋訓練処"),
        UnitItem("SK00", "實習船營運辦公室", "Training Ship Operations", "実習船運航オフィス"),
        UnitItem("SE00", "秘書室", "Secretariat", "秘書室"),
        UnitItem("LB00", "圖書館", "Library", "図書館"),
        UnitItem("PE00", "人事室", "Personnel Office", "人事室"),
        UnitItem("AC00", "主計室", "Accounting Office", "主計室 (会計課)"),
        UnitItem("PH00", "體育室", "Physical Education Office", "体育室"),
        UnitItem("RD00", "校友服務就業中心", "Alumni Service & Career Center", "同窓会・就職支援センター"),
        UnitItem("GB00", "環境安全衛生中心", "Environmental Safety & Health Center", "環境安全衛生センター"),
        UnitItem("IC00", "電算與網路中心", "Computer & Network Center", "情報ネットワークセンター"),
        UnitItem("IE00", "創新創業發展處", "Innovation & Entrepreneurship", "イノベーション創業処"),
        UnitItem("UY00", "智慧機電學院", "College of Mechatronics", "知能メカトロニクス学部"),
        UnitItem("UU00", "工學院", "College of Engineering", "工学部"),
        UnitItem("IN00", "國際學院", "International College", "国際学部"),
        UnitItem("UB00", "電資學院", "College of Electrical & Computer Eng.", "電気・情報工学部"),
        UnitItem("US00", "水圈學院", "College of Hydrosphere Science", "水圏科学部"),
        UnitItem("UT00", "商業智慧學院", "College of Business Intelligence", "ビジネスインテリジェンス学部"),
        UnitItem("UR00", "海事學院", "College of Maritime", "海事学部"),
        UnitItem("UW00", "管理學院", "College of Management", "経営管理学部"),
        UnitItem("UV00", "海商學院", "College of Marine Commerce", "海洋商学部"),
        UnitItem("UP00", "財金學院", "College of Finance & Banking", "金融・ファイナンス学部"),
        UnitItem("UH00", "人文社會學院", "College of Humanities & Social Sciences", "人文社会学部"),
        UnitItem("UJ00", "外語學院", "College of Foreign Languages", "外国語学部"),
        UnitItem("AT00", "進修學院", "College of Continuing Education", "生涯学習学部"),
        UnitItem("XB00", "共同教育學院", "General Education College", "教養教育学部"),
        UnitItem("UA00", "創新設計學院", "College of Creative Design", "イノベーションデザイン学部"),
        UnitItem("ZZ00", "其他單位", "Other Departments", "その他の窓口")
    )

    val GUEST_TYPES = listOf(
        KeyValueItem("1", "學生", "Student", "学生"),
        KeyValueItem("2", "教職同仁", "Faculty / Staff", "教職員"),
        KeyValueItem("4", "校友", "Alumni", "卒業生"),
        KeyValueItem("5", "家長", "Parent", "保護者"),
        KeyValueItem("6", "民眾", "Public", "一般")
    )

    val SECRECY_TYPES = listOf(
        KeyValueItem("9", "不保密 (預設)", "Not Confidential (Default)", "非公開にしない (デフォルト)"),
        KeyValueItem("1", "保密，個資去識別化", "Confidential (Anonymized)", "個人情報を匿名化"),
        KeyValueItem("2", "保密，同意承辦單位聯繫", "Confidential (Contact allowed)", "案件対応時の連絡に同意")
    )

    val DONE_OPEN_TYPES = listOf(
        KeyValueItem("1", "同意公開 (預設)", "Publicly Viewable (Default)", "一般公開に同意 (デフォルト)"),
        KeyValueItem("2", "不同意公開", "Not Publicly Viewable", "非公開を希望")
    )
}
