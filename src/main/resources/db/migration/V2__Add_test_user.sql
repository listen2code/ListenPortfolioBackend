-- ===================================================================
-- Portfolio 应用初始测试数据填充脚本
-- 版本: V2
-- 说明: 插入基础用户、简历、项目经历及多语言测试数据
-- ===================================================================

-- 1. 插入用户数据 (包含英文默认、中文 _zh、日语 _ja)
INSERT IGNORE INTO users (id, name, email, password, location, location_zh, location_ja, avatar_url, status, job_title, job_title_zh, job_title_ja, bio, bio_zh, bio_ja, graduation_year, github_url, major, major_zh, major_ja) VALUES
(1, 'Listen', 'listen2code@gmail.com', '$2a$10$3Fa2JeWy.qEFQulYDtYhGO4g/gHg8nKgkSkp0KvEmGiZZIJqbdVIK',
 'Japan / Tokyo', '日本 / 东京', '日本 / 東京',
 'https://api.dicebear.com/10.x/bottts/svg?seed=Listen', 'available',
 'Senior Android / Flutter Engineer', '资深 Android / Flutter 开发专家', 'シニア Android / Flutter エンジニア',
 'Senior Android Engineer with 11+ years of mobile development experience and 3+ years in Flutter. Expertise in client architecture (componentization, plugin systems), performance optimization, and APM infrastructure. Key achievements include reducing Feed timeout rates from 1.5% to 0.3%, building full-stack APM monitoring platforms, and leading Flutter app development for securities trading at Rakuten. JLPT N1, BJT J2 certified, currently based in Tokyo, Japan.',
 '具有 11 年以上移动端开发经验及 3 年以上 Flutter 经验的资深工程师。精通客户端架构（组件化、插件化体系）、性能优化及 APM 监控基础设施。主要成就包括将 Feed 流超时率从 1.5% 降低至 0.3%，构建全栈 APM 性能监控平台，以及在乐天领衔开发证券交易 Flutter 应用。持日语 N1 与 BJT J2 证书，现定居日本东京。',
 '11年以上のモバイル開発経験と3年以上のFlutter経験を持つシニアエンジニア。クライアントアーキテクチャ（コンポーネント化、プラグイン構造）、パフォーマンス最適化、APMインフラに精通。Feedタイムアウト率を1.5%から0.3%に削減、フルスタックAPM監視プラットフォームの構築、楽天での証券取引Flutterアプリ開発をリード。JLPT N1、BJT J2取得、東京在住。',
 '2013', 'https://github.com/listen2code',
 'softwareEngineering', '软件工程', 'ソフトウェア工学'),

(2, 'Listen2', 'listen4Future@gmail.com', '$2a$10$3Fa2JeWy.qEFQulYDtYhGO4g/gHg8nKgkSkp0KvEmGiZZIJqbdVIK',
 'Test Location', '测试地点', 'テスト地域',
 'https://api.dicebear.com/10.x/bottts/svg?seed=Listen2', 'active',
 'Test Engineer', '测试工程师', 'テストエンジニア',
 'This is a test user for demonstration and multi-language verification.',
 '这是一个用于演示和多语言验证的测试用户。',
 'これはデモおよび多言語検証用のテストユーザーです。',
 '2023', 'https://github.com/testuser',
 'Computer Science', '计算机科学', 'コンピュータサイエンス');

-- 2. 插入项目数据 (包含英文默认、中文 _zh、日语 _ja)
INSERT IGNORE INTO projects (id, business_id, title, title_zh, title_ja, subtitle, subtitle_zh, subtitle_ja, project_desc, project_desc_zh, project_desc_ja, image_url, github_url) VALUES
(1, 'lportfolio-flutter', 'lPortfolio Flutter', 'lPortfolio Flutter 客户端', 'lPortfolio Flutter アプリ',
 'Current Project', '当前项目', '現在のプロジェクト',
 'My personal portfolio app (this one!). Demonstrating Clean Architecture, MVI pattern, and advanced Riverpod state management in Flutter.',
 '个人作品集客户端应用（即本 App）。全面展示 Flutter 下的 Clean 架构、MVI 模式及高级 Riverpod 状态管理。',
 '個人ポートフォリオアプリ（本アプリ）。FlutterにおけるClean Architecture、MVIパターン、高度なRiverpod状態管理を展示。',
 'localhost/images/project1.jpg', 'https://github.com/listen2code/ListenPortfolioFlutter'),

(2, 'listen-core-flutter', 'Listen Core Flutter', 'Listen Core 核心框架', 'Listen Core 核心フレームワーク',
 'Framework', '核心基础库', 'コアライブラリ',
 'A foundational framework for Flutter projects providing base classes for MVI, standardized network wrappers, and lifecycle management.',
 '为 Flutter 项目打造的底座框架，提供 MVI 基类、标准化网络请求封装及生命周期管理机制。',
 'Flutterプロジェクト向けの基盤フレームワーク。MVI基底クラス、標準化ネットワークラッパー、ライフサイクル管理を提供。',
 'localhost/images/project2.jpg', 'https://github.com/listen2code/ListenCoreFlutter'),

(3, 'listen-ui-kit', 'Listen UI Kit', 'Listen UI 组件库', 'Listen UI コンポーネント集',
 'Common Library', '公共 UI 库', '共通UIライブラリ',
 'A comprehensive UI component library for consistent branding and rapid development across multiple Flutter applications.',
 '高复用的通用 UI 组件库，用于保持多款 Flutter 应用间视觉规范一致与快速迭代开发。',
 '複数のFlutterアプリで一貫したブランディングと迅速な開発を実現する包括的なUIコンポーネントライブラリ。',
 'localhost/images/project3.jpg', 'https://github.com/listen2code/ListenUikitFlutter'),

(4, 'portfolio-backend', 'Portfolio Backend', '服务端后台架构', 'バックエンドインフラ',
 'Cloud Infrastructure', '云端微服务', 'クラウドバックエンド',
 'The server-side implementation for this portfolio, managing user data, projects, and dynamic configurations.',
 '本作品集的服务端实现，采用 Spring Boot + MySQL + Redis 架构，支持多语言数据动态分发与安全防护。',
 '本ポートフォリオのバックエンド実装。Spring Boot + MySQL + Redis構成でユーザーデータや多言語動的配信を管理。',
 'localhost/images/project4.jpg', 'https://github.com/listen2code/ListenPortfolioBackend'),

(5, 'tech-knowledge-base', 'Tech Knowledge Base', '技术知识库文章', '技術ナレッジベース',
 'Articles & Docs', '技术文章与文档', '技術記事・ドキュメント',
 'A curated collection of my technical articles, architecture notes, and development experiences over the past 10 years.',
 '近 10 年移动端与全栈开发的精选技术文章、架构设计笔记及实践经验总结。',
 '過去10年間にわたる技術記事、アーキテクチャノート、開発ノウハウの厳選コレクション。',
 'localhost/images/project5.jpg', 'https://github.com/listen2code/article');

-- 3. 插入项目技术栈数据
INSERT IGNORE INTO project_tech_stack (project_id, tech_name) VALUES
(1, 'Flutter'), (1, 'Riverpod'), (1, 'Clean Architecture'), (1, 'MVI'),
(2, 'Dart'), (2, 'Riverpod'), (2, 'Dio'), (2, 'Architecture'),
(3, 'Flutter'), (3, 'Design System'), (3, 'CustomPainter'),
(4, 'Spring Boot'), (4, 'MySQL'), (4, 'Redis'), (4, 'Docker'),
(5, 'Markdown'), (5, 'Documentation'), (5, 'Knowledge Sharing');

-- 4. 插入工作经历数据 (包含英文默认、中文 _zh、日语 _ja)
INSERT IGNORE INTO experiences (id, user_id, title, title_zh, title_ja, company, company_zh, company_ja, period, description, description_zh, description_ja) VALUES
(1, 1, 'Android / Flutter Engineer', 'Android / Flutter 开发专家', 'Android / Flutter エンジニア',
 'LYC Corp. (Rakuten Securities Project)', 'LYC 株式会社（乐天证券项目）', 'LYC 株式会社（楽天証券プロジェクト）', '2023.02 - Present',
 'Lead developer for new securities Flutter app: architecture design, framework development, FIDO2 authentication integration, and Flutter version upgrades. Maintaining Android stock trading app and conducting code reviews for team members.',
 '全新证券 Flutter 应用核心主导人：负责整体架构设计、基础框架搭建、FIDO2 生物识别认证集成及 Flutter 大版本平滑升级；同时维护 Android 股票交易 App 并负责团队 Code Review。',
 '新規証券Flutterアプリの主導開発者：アーキテクチャ設計、基盤開発、FIDO2認証統合、Flutterバージョンアップを推進。Android株取引アプリの保守およびコードレビューを担当。'),

(2, 1, 'Android Engineer — Mobile Infrastructure', 'Android 基础设施工程师', 'Android インフラエンジニア',
 'Youzan Technology', '有赞科技', 'Youzan テクノロジー', '2021.10 - 2022.07',
 'Built mobile APM stutter/ANR detection SDK with optimized data reporting and aggregation. Created full-stack monitoring dashboards (React/AntDesign frontend + Spring Boot backend with RESTful APIs). Participated in Commerce SDK Redux-pattern refactoring.',
 '主导移动端 APM 卡顿与 ANR 监控 SDK 建设，优化上报与数据聚合逻辑；搭建全栈性能监控平台（React 前端 + Spring Boot 后端）；参与微商城 SDK 的 Redux 架构重构。',
 'モバイルAPMのUIカクつき・ANR検知SDKを構築、データ上報・集計を最適化。フルスタック監視ダッシュボード（React + Spring Boot）を開発。EC SDKのReduxパターンリファクタリングに参加。'),

(3, 1, 'Android Engineer', 'Android 研发工程师', 'Android エンジニア',
 'Duolu (Yin''ai Network Technology)', '多路（印爱网络科技）', 'Duolu（印愛ネットワーク）', '2019.11 - 2021.10',
 'Established Feed monitoring system, reducing timeout rate from 1.5% to 0.3% and latency by 40%+. Led componentization (module + module_api + module_run) and plugin architecture (Shadow framework). Built dev-stage performance monitoring tools and automated testing (44 Feed cases via AirTest).',
 '建立 Feed 流专项监控体系，将超时率从 1.5% 降至 0.3%，时延降低 40%+；主导客户端组件化（module + module_api + module_run）与插件化（Shadow 框架）架构落地；搭建研发期监控工具与自动化测试体系。',
 'Feedタイムアウト率を1.5%から0.3%へ、レイテンシを40%+削減する監視システムを確立。コンポーネント化およびプラグイン構造（Shadow）を主導。開発期パフォーマンス測定ツールと自動テスト（AirTest 44ケース）を構築。'),

(4, 1, 'Android Engineer', 'Android 研发工程师', 'Android エンジニア',
 'Qibei Technology (Bike-sharing)', '骑呗科技（共享单车）', 'Qibei テクノロジー（シェアサイクル）', '2016.09 - 2019.08',
 'Developed bike-sharing apps (Qibei Bike, Dingda Transit) across 4+ major versions. Implemented hot-fix (Tinker), online performance monitoring (Matrix), and reduced build time by 30%+ via Gradle optimization. Set up Jenkins CI pipeline with wireless ADB deployment.',
 '负责骑呗单车与叮嗒出行 App 4 个以上大版本研发；集成 Tinker 热修复与 Matrix 线上监控；通过 Gradle 编译优化提升 30%+ 构建速度；搭建 Jenkins CI/CD 自动化构建与无线部署流水线。',
 'シェアサイクルアプリ（Qibei Bike、Dingda Transit）の4つ以上のメジャーバージョンを開発。Tinker熱修正、Matrix監視を導入し、Gradle最適化でビルド時間を30%+削減。Jenkins CIパイプラインを構築。'),

(5, 1, 'Android Engineer', 'Android 研发工程师', 'Android エンジニア',
 'Baidu (Waimai Delivery)', '百度（百度外卖）', 'Baidu（百度外売）', '2014.12 - 2016.06',
 'Independently maintained delivery rider app (Xiaodu Knight v1.4-2.9). Designed dynamic GPS tracking strategy reducing redundant uploads by 10%+. Developed PassSDK for unified B-side authentication with AES/JNI encryption. Built logistics development framework for multi-app scaffolding.',
 '独立负责百度骑士 App（v1.4-v2.9）研发；设计动态 GPS 上报策略降低 10%+ 冗余流量；开发 PassSDK（JNI/AES 加密）统一 B 端登录；搭建物流开发框架供多 App 复用。',
 '配達員アプリ（小度騎士 v1.4-2.9）を独立保守。動的GPS追跡戦略を設計し冗余アップロードを10%+削減。B端統一認証PassSDK（AES/JNI暗号化）および物流開発フレームワークを構築。'),

(6, 1, 'Java Developer', 'Java 软件工程师', 'Java ソフトウェアエンジニア',
 'NewLand Software Engineering', '新大陆软件工程', 'NewLand ソフトウェア', '2013.05 - 2014.09',
 'Developed business management and analytics modules for China Mobile support system using J2EE, S2SH framework, and Oracle database.',
 '参与中国移动网管支撑系统业务管理与分析模块开发，使用 J2EE、S2SH 框架及 Oracle 数据库。',
 '中国移動（China Mobile）サポートシステムの業務管理および分析モジュールをJ2EE、S2SH、Oracleデータベースを用いて開発。');

-- 5. 插入教育经历数据 (包含英文默认、中文 _zh、日语 _ja)
INSERT IGNORE INTO education (id, user_id, degree, degree_zh, degree_ja, school, school_zh, school_ja, period, description, description_zh, description_ja) VALUES
(1, 1, 'Bachelor of Software Engineering', '软件工程 学士学位', 'ソフトウェア工学 学士',
 'Fujian University of Technology', '福建工程学院（现福建理工大学）', '福建工程学院', '2011.09 - 2013.06',
 'Outstanding Graduation Thesis: Design and Implementation of CRM System Based on Intelligent Evaluation System',
 '优秀毕业设计：《基于智能评估系统的 CRM 系统设计与实现》',
 '優秀卒業論文：『インテリジェント評価システムに基づくCRMシステムの実装と設計』'),

(2, 1, 'Associate in Computer Applications', '计算机应用技术 大专', 'コンピュータ応用 専門士',
 'Fujian Normal University (IT College)', '福建师范大学（软件学院/IT学院）', '福建師範大学（IT学院）', '2008.09 - 2011.06',
 'Provincial Outstanding Student, First-class Scholarship, Outstanding Student Cadre',
 '省级优秀毕业生、一等奖学金、优秀学生干部',
 '省級優秀卒業生、一等奨学金、優秀学生幹部');

-- 6. 插入技能数据
INSERT IGNORE INTO skills (id, user_id, category) VALUES
(1, 1, 'Mobile'),
(2, 1, 'Architecture'),
(3, 1, 'Performance & APM'),
(4, 1, 'Backend & DevOps');

-- 7. 插入技能项目数据
INSERT IGNORE INTO skill_items (skill_id, item_name) VALUES
(1, 'Flutter'), (1, 'Android Native'), (1, 'Dart'), (1, 'Kotlin'), (1, 'Java'),
(2, 'Clean Architecture'), (2, 'Componentization'), (2, 'Plugin Architecture'), (2, 'MVI'), (2, 'MVVM'), (2, 'SOLID'),
(3, 'Profiling'), (3, 'Memory Optimization'), (3, 'Feed Monitoring'), (3, 'Systrace'), (3, 'LeakCanary'),
(4, 'Spring Boot'), (4, 'MySQL'), (4, 'Redis'), (4, 'Docker'), (4, 'CI/CD');

-- 8. 插入语言能力数据 (包含英文默认、中文 _zh、日语 _ja)
INSERT IGNORE INTO languages (id, user_id, name, name_zh, name_ja, level, level_zh, level_ja) VALUES
(1, 1, 'Japanese', '日语', '日本語', 'JLPT N1 (131), BJT J2 (512)', 'JLPT N1 级 (131分), BJT J2 级 (512分)', 'JLPT N1 (131点), BJT J2 (512点)'),
(2, 1, 'Chinese', '中文', '中国語', 'Native', '母语', '母国語'),
(3, 1, 'English', '英语', '英語', 'CET-4', '大学英语四级 (CET-4)', 'CET-4');

-- 9. 插入用户认证数据
INSERT IGNORE INTO user_certifications (user_id, certification_name) VALUES
(1, 'jlptN1'),
(1, 'bjtJ2');

-- 10. 插入统计数据
INSERT IGNORE INTO stats (id, user_id, business_id, year, label) VALUES
(1, 1, 'android', '11', 'androidExp'),
(2, 1, 'flutter', '3', 'flutterExp'),
(3, 1, 'java_web', '1', 'javaWeb');

-- 11. 插入统计标签数据
INSERT IGNORE INTO stat_tags (stat_id, tag_name) VALUES
(1, 'archDesign'),
(1, 'perfOptimization'),
(1, 'componentization'),
(2, 'cleanArchitecture'),
(2, 'stateManagement'),
(2, 'riverpod'),
(3, 'springBoot'),
(3, 'restApi');
