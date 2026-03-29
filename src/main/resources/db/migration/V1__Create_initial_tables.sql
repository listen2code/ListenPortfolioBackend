-- ===================================================================
-- Portfolio 应用数据库初始化脚本
-- 版本: V1
-- 说明: 创建项目的基础表结构，基于 db/schema.sql 和 db/data.sql
-- ===================================================================

-- ===================================================================
-- 用户表 (users)
-- ===================================================================
-- 说明: 存储用户基本信息和认证数据
-- 基于: db/schema.sql 中的 users 表结构
-- 索引: email (唯一)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
    name VARCHAR(255) NOT NULL COMMENT '用户姓名',
    email VARCHAR(255) UNIQUE NOT NULL COMMENT '邮箱地址，唯一',
    password VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密',
    location VARCHAR(255) COMMENT '所在地',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    status VARCHAR(255) COMMENT '用户状态',
    job_title VARCHAR(255) COMMENT '职位头衔',
    bio TEXT COMMENT '个人简介',
    graduation_year VARCHAR(10) COMMENT '毕业年份',
    github_url VARCHAR(255) COMMENT 'GitHub链接',
    major VARCHAR(255) COMMENT '专业',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '软删除标记',
    
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户表';

-- ===================================================================
-- 用户认证表 (user_certifications)
-- ===================================================================
-- 说明: 用户认证证书信息
-- 基于: db/schema.sql 中的 user_certifications 表
CREATE TABLE IF NOT EXISTS user_certifications (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    certification_name VARCHAR(255) NOT NULL COMMENT '认证名称',
    PRIMARY KEY (user_id, certification_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户认证表';

-- ===================================================================
-- 项目表 (projects)
-- ===================================================================
-- 说明: 项目信息表
-- 基于: db/schema.sql 中的 projects 表
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID，主键自增',
    business_id VARCHAR(255) UNIQUE COMMENT '业务逻辑ID',
    title VARCHAR(255) NOT NULL COMMENT '项目标题',
    subtitle VARCHAR(255) COMMENT '项目副标题',
    project_desc TEXT COMMENT '项目描述',
    image_url VARCHAR(255) COMMENT '项目图片URL',
    github_url VARCHAR(255) COMMENT 'GitHub仓库URL',
    
    INDEX idx_projects_business_id (business_id),
    INDEX idx_projects_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目表';

-- ===================================================================
-- 项目技术栈表 (project_tech_stack)
-- ===================================================================
-- 说明: 项目技术栈关联表
-- 基于: db/schema.sql 中的 project_tech_stack 表
CREATE TABLE IF NOT EXISTS project_tech_stack (
    project_id BIGINT NOT NULL COMMENT '项目ID',
    tech_name VARCHAR(255) NOT NULL COMMENT '技术名称',
    PRIMARY KEY (project_id, tech_name),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目技术栈表';

-- ===================================================================
-- 工作经历表 (experiences)
-- ===================================================================
-- 说明: 用户工作经历
-- 基于: db/schema.sql 中的 experiences 表
CREATE TABLE IF NOT EXISTS experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '经历ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '职位标题',
    company VARCHAR(255) COMMENT '公司名称',
    period VARCHAR(255) COMMENT '工作时间段',
    description TEXT COMMENT '工作描述',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_experiences_user_id (user_id),
    INDEX idx_experiences_company (company)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='工作经历表';

-- ===================================================================
-- 教育经历表 (education)
-- ===================================================================
-- 说明: 用户教育背景
-- 基于: db/schema.sql 中的 education 表
CREATE TABLE IF NOT EXISTS education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教育ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    degree VARCHAR(255) COMMENT '学位',
    school VARCHAR(255) COMMENT '学校名称',
    period VARCHAR(255) COMMENT '学习时间段',
    description TEXT COMMENT '教育描述',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_education_user_id (user_id),
    INDEX idx_education_school (school)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='教育经历表';

-- ===================================================================
-- 技能表 (skills)
-- ===================================================================
-- 说明: 用户技能分类
-- 基于: db/schema.sql 中的 skills 表
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '技能ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category VARCHAR(255) NOT NULL COMMENT '技能分类',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_skills_user_id (user_id),
    INDEX idx_skills_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='技能表';

-- ===================================================================
-- 技能项目表 (skill_items)
-- ===================================================================
-- 说明: 技能具体项目
-- 基于: db/schema.sql 中的 skill_items 表
CREATE TABLE IF NOT EXISTS skill_items (
    skill_id BIGINT NOT NULL COMMENT '技能ID',
    item_name VARCHAR(255) NOT NULL COMMENT '技能项目名称',
    PRIMARY KEY (skill_id, item_name),
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='技能项目表';

-- ===================================================================
-- 语言表 (languages)
-- ===================================================================
-- 说明: 用户语言能力
-- 基于: db/schema.sql 中的 languages 表
CREATE TABLE IF NOT EXISTS languages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '语言ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(255) COMMENT '语言名称',
    level VARCHAR(255) COMMENT '语言水平',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_languages_user_id (user_id),
    INDEX idx_languages_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='语言表';

-- ===================================================================
-- 统计表 (stats)
-- ===================================================================
-- 说明: 用户统计数据
-- 基于: db/schema.sql 中的 stats 表
CREATE TABLE IF NOT EXISTS stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    business_id VARCHAR(255) NOT NULL COMMENT '业务逻辑ID',
    year VARCHAR(255) COMMENT '年份',
    label VARCHAR(255) COMMENT '统计标签',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_stats_user_id (user_id),
    INDEX idx_stats_business_id (business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='统计表';

-- ===================================================================
-- 统计标签表 (stat_tags)
-- ===================================================================
-- 说明: 统计标签关联
-- 基于: db/schema.sql 中的 stat_tags 表
CREATE TABLE IF NOT EXISTS stat_tags (
    stat_id BIGINT NOT NULL COMMENT '统计ID',
    tag_name VARCHAR(255) NOT NULL COMMENT '标签名称',
    PRIMARY KEY (stat_id, tag_name),
    FOREIGN KEY (stat_id) REFERENCES stats(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='统计标签表';

-- ===================================================================
-- 插入初始数据
-- ===================================================================
-- 基于: db/data.sql 的初始数据

-- 插入用户数据
INSERT IGNORE INTO users (id, name, email, password, location, avatar_url, status, job_title, bio, graduation_year, github_url, major) VALUES
(1, 'Listen', 'listen2code@gmail.com', '$2a$10$3Fa2JeWy.qEFQulYDtYhGO4g/gHg8nKgkSkp0KvEmGiZZIJqbdVIK', 'Japan / Tokyo', 'https://api.dicebear.com/7.x/avataaars/png?seed=Listen', 'available', 'Full Stack Mobile Architect', 'A seasoned mobile developer with over 10 years of experience in Android and 2+ years in Flutter. Specialized in high-performance application development, clean architecture, and reactive programming. Proven track record of leading cross-functional teams and delivering complex enterprise solutions.', '2013', 'https://github.com/listen2code', 'softwareEngineering');

-- 插入项目数据
INSERT IGNORE INTO projects (id, business_id, title, subtitle, project_desc, image_url, github_url) VALUES
(1, 'lportfolio-flutter', 'lPortfolio Flutter', 'Current Project', 'My personal portfolio app (this one!). Demonstrating Clean Architecture, MVI pattern, and advanced Riverpod state management in Flutter.', 'localhost/images/project1.jpg', 'https://github.com/listen2code/ListenPortfolioFlutter'),
(2, 'listen-core-flutter', 'Listen Core Flutter', 'Framework', 'A foundational framework for Flutter projects providing base classes for MVI, standardized network wrappers, and lifecycle management.', 'localhost/images/project2.jpg', 'https://github.com/listen2code/ListenCoreFlutter'),
(3, 'listen-ui-kit', 'Listen UI Kit', 'Common Library', 'A comprehensive UI component library for consistent branding and rapid development across multiple Flutter applications.', 'localhost/images/project3.jpg', 'https://github.com/listen2code/ListenUikitFlutter'),
(4, 'portfolio-backend', 'Portfolio Backend', 'Cloud Infrastructure', 'The server-side implementation for this portfolio, managing user data, projects, and dynamic configurations.', 'localhost/images/project4.jpg', 'https://github.com/listen2code/ListenPortfolioBackend'),
(5, 'tech-knowledge-base', 'Tech Knowledge Base', 'Articles & Docs', 'A curated collection of my technical articles, architecture notes, and development experiences over the past 10 years.', 'localhost/images/project5.jpg', 'https://github.com/listen2code/article');

-- 插入项目技术栈数据
INSERT IGNORE INTO project_tech_stack (project_id, tech_name) VALUES
(1, 'Flutter'), (1, 'Riverpod'), (1, 'Clean Architecture'), (1, 'MVI'),
(2, 'Dart'), (2, 'Riverpod'), (2, 'Dio'), (2, 'Architecture'),
(3, 'Flutter'), (3, 'Design System'), (3, 'CustomPainter'),
(4, 'Spring Boot'), (4, 'JPA'), (4, 'AWS'), (4, 'MySQL'),
(5, 'Markdown'), (5, 'Documentation'), (5, 'Knowledge Sharing');

-- 插入工作经历数据
INSERT IGNORE INTO experiences (id, user_id, title, company, period, description) VALUES
(1, 1, 'Senior Mobile Architect', 'Global Tech Solutions', '2021 - Present', 'Leading the migration of core native apps to Flutter, optimizing CI/CD pipelines, and establishing mobile engineering best practices.'),
(2, 1, 'Lead Android Developer', 'Innovation Hub', '2015 - 2021', 'Designed and developed large-scale financial applications with millions of active users. Implemented robust security protocols.'),
(3, 1, 'Junior Developer', 'Start-up Inc.', '2013 - 2015', 'Focusing on UI/UX implementation and RESTful API integration for Android platform.');

-- 插入教育经历数据
INSERT IGNORE INTO education (id, user_id, degree, school, period, description) VALUES
(1, 1, 'Bachelor of Computer Science', 'Tech University', '2009 - 2013', 'Specialized in Software Engineering and Mobile Systems.');

-- 插入技能数据
INSERT IGNORE INTO skills (id, user_id, category) VALUES
(1, 1, 'Mobile'),
(2, 1, 'Architecture'),
(3, 1, 'Backend & DevOps');

-- 插入技能项目数据
INSERT IGNORE INTO skill_items (skill_id, item_name) VALUES
(1, 'Flutter'), (1, 'Android Native'), (1, 'Dart'), (1, 'Kotlin'), (1, 'Java'),
(2, 'Clean Architecture'), (2, 'MVI'), (2, 'MVVM'), (2, 'SOLID'),
(3, 'Spring Boot'), (3, 'SQL'), (3, 'Docker'), (3, 'CI/CD');

-- 插入语言数据
INSERT IGNORE INTO languages (id, user_id, name, level) VALUES
(1, 1, 'English', 'CET4'),
(2, 1, 'Japanese', 'N1'),
(3, 1, 'Chinese', 'Native');

-- 插入用户认证数据
INSERT IGNORE INTO user_certifications (user_id, certification_name) VALUES
(1, 'jlptN1'),
(1, 'bjtJ2');

-- 插入统计数据
INSERT IGNORE INTO stats (id, user_id, business_id, year, label) VALUES
(1, 1, 'android', '10', 'androidExp'),
(2, 1, 'flutter', '2', 'flutterExp'),
(3, 1, 'java_web', '1', 'javaWeb');

-- 插入统计标签数据
INSERT IGNORE INTO stat_tags (stat_id, tag_name) VALUES
(1, 'archDesign'),
(1, 'perfOptimization'),
(2, 'stateManagement'),
(2, 'riverpod'),
(2, 'cleanArchitecture'),
(3, 'springBoot'),
(3, 'jpa'),
(3, 'aws');

-- ===================================================================
-- 性能优化说明
-- ===================================================================
-- 1. 外键索引：MySQL 会自动为外键创建索引
-- 2. 表定义中已包含必要的索引
-- 3. 额外索引可在后续版本的迁移脚本中添加
