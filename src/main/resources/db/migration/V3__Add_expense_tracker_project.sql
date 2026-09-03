-- ===================================================================
-- Portfolio 应用数据迁移脚本
-- 版本: V3
-- 说明: 新增原生 Android 极简记账应用 (ListenExpenseTracker) 及对应技术栈标签
-- ===================================================================

-- 1. 插入项目数据 (包含英文默认、中文 _zh、日语 _ja)
INSERT IGNORE INTO projects (id, business_id, title, title_zh, title_ja, subtitle, subtitle_zh, subtitle_ja, project_desc, project_desc_zh, project_desc_ja, image_url, github_url) VALUES
(6, 'listen-expense-tracker', 'Listen Expense Tracker', 'Listen Expense Tracker 原生记账', 'Listen Expense Tracker 家計簿アプリ',
 'Android App', '原生移动应用', 'ネイティブAndroidアプリ',
 'A modern native Android personal finance app built with Kotlin 2.x, Jetpack Compose, MVI, Room Local-First architecture, Google Credential Manager, and Google Drive cloud backup.',
 '基于 Kotlin 2.x + Jetpack Compose + MVI + Room 离线优先架构打造的原生 Android 极简记账应用。支持 Google Credential Manager 原生账户认证、Google Drive 云端备份恢复、多维 Canvas 统计图表及桌面小组件。',
 'Kotlin 2.x、Jetpack Compose、MVIパターン、Roomローカルファースト設計で構築されたネイティブAndroid家計簿アプリ。Google Credential Manager認証、Google Driveクラウドバックアップ、Canvasグラフ分析、AppWidgetを搭載。',
 'localhost/images/project6.jpg', 'https://github.com/listen2code/ListenExpenseTracker');

-- 2. 插入项目技术栈数据
INSERT IGNORE INTO project_tech_stack (project_id, tech_name) VALUES
(6, 'Kotlin'),
(6, 'Jetpack Compose'),
(6, 'MVI'),
(6, 'Room'),
(6, 'Google Drive API');
