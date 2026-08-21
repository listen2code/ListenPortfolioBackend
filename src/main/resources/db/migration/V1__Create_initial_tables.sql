-- ===================================================================
-- Portfolio 应用数据库初始化脚本
-- 版本: V1
-- 说明: 创建项目的基础表结构（包含国际化多语言扩展字段）
-- ===================================================================

-- ===================================================================
-- 用户表 (users)
-- ===================================================================
-- 说明: 存储用户基本信息和认证数据，扩展多语言字段
-- 索引: email (唯一)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
    name VARCHAR(255) NOT NULL COMMENT '用户姓名',
    email VARCHAR(255) UNIQUE NOT NULL COMMENT '邮箱地址，唯一',
    password VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密',
    location VARCHAR(255) COMMENT '所在地 (默认/英文)',
    location_zh VARCHAR(255) COMMENT '所在地 (中文)',
    location_ja VARCHAR(255) COMMENT '所在地 (日语)',
    avatar_url LONGTEXT COMMENT '头像URL或Base64数据',
    status VARCHAR(255) COMMENT '用户状态',
    job_title VARCHAR(255) COMMENT '职位头衔 (默认/英文)',
    job_title_zh VARCHAR(255) COMMENT '职位头衔 (中文)',
    job_title_ja VARCHAR(255) COMMENT '职位头衔 (日语)',
    bio TEXT COMMENT '个人简介 (默认/英文)',
    bio_zh TEXT COMMENT '个人简介 (中文)',
    bio_ja TEXT COMMENT '个人简介 (日语)',
    graduation_year VARCHAR(10) COMMENT '毕业年份',
    github_url VARCHAR(255) COMMENT 'GitHub链接',
    major VARCHAR(255) COMMENT '专业 (默认/英文)',
    major_zh VARCHAR(255) COMMENT '专业 (中文)',
    major_ja VARCHAR(255) COMMENT '专业 (日语)',
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
CREATE TABLE IF NOT EXISTS user_certifications (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    certification_name VARCHAR(255) NOT NULL COMMENT '认证名称',
    PRIMARY KEY (user_id, certification_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户认证表';

-- ===================================================================
-- 项目表 (projects)
-- ===================================================================
-- 说明: 项目信息表，扩展多语言字段
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID，主键自增',
    business_id VARCHAR(255) UNIQUE COMMENT '业务逻辑ID',
    title VARCHAR(255) NOT NULL COMMENT '项目标题 (默认/英文)',
    title_zh VARCHAR(255) COMMENT '项目标题 (中文)',
    title_ja VARCHAR(255) COMMENT '项目标题 (日语)',
    subtitle VARCHAR(255) COMMENT '项目副标题 (默认/英文)',
    subtitle_zh VARCHAR(255) COMMENT '项目副标题 (中文)',
    subtitle_ja VARCHAR(255) COMMENT '项目副标题 (日语)',
    project_desc TEXT COMMENT '项目描述 (默认/英文)',
    project_desc_zh TEXT COMMENT '项目描述 (中文)',
    project_desc_ja TEXT COMMENT '项目描述 (日语)',
    image_url VARCHAR(255) COMMENT '项目图片URL',
    github_url VARCHAR(255) COMMENT 'GitHub仓库URL',
    
    INDEX idx_projects_business_id (business_id),
    INDEX idx_projects_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目表';

-- ===================================================================
-- 项目技术栈表 (project_tech_stack)
-- ===================================================================
-- 说明: 项目技术栈关联表
CREATE TABLE IF NOT EXISTS project_tech_stack (
    project_id BIGINT NOT NULL COMMENT '项目ID',
    tech_name VARCHAR(255) NOT NULL COMMENT '技术名称',
    PRIMARY KEY (project_id, tech_name),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目技术栈表';

-- ===================================================================
-- 工作经历表 (experiences)
-- ===================================================================
-- 说明: 用户工作经历，扩展多语言字段
CREATE TABLE IF NOT EXISTS experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '经历ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '职位标题 (默认/英文)',
    title_zh VARCHAR(255) COMMENT '职位标题 (中文)',
    title_ja VARCHAR(255) COMMENT '职位标题 (日语)',
    company VARCHAR(255) COMMENT '公司名称 (默认/英文)',
    company_zh VARCHAR(255) COMMENT '公司名称 (中文)',
    company_ja VARCHAR(255) COMMENT '公司名称 (日语)',
    period VARCHAR(255) COMMENT '工作时间段',
    description TEXT COMMENT '工作描述 (默认/英文)',
    description_zh TEXT COMMENT '工作描述 (中文)',
    description_ja TEXT COMMENT '工作描述 (日语)',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_experiences_user_id (user_id),
    INDEX idx_experiences_company (company)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='工作经历表';

-- ===================================================================
-- 教育经历表 (education)
-- ===================================================================
-- 说明: 用户教育背景，扩展多语言字段
CREATE TABLE IF NOT EXISTS education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教育ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    degree VARCHAR(255) COMMENT '学位 (默认/英文)',
    degree_zh VARCHAR(255) COMMENT '学位 (中文)',
    degree_ja VARCHAR(255) COMMENT '学位 (日语)',
    school VARCHAR(255) COMMENT '学校名称 (默认/英文)',
    school_zh VARCHAR(255) COMMENT '学校名称 (中文)',
    school_ja VARCHAR(255) COMMENT '学校名称 (日语)',
    period VARCHAR(255) COMMENT '学习时间段',
    description TEXT COMMENT '教育描述 (默认/英文)',
    description_zh TEXT COMMENT '教育描述 (中文)',
    description_ja TEXT COMMENT '教育描述 (日语)',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_education_user_id (user_id),
    INDEX idx_education_school (school)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='教育经历表';

-- ===================================================================
-- 技能表 (skills)
-- ===================================================================
-- 说明: 用户技能分类
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '技能ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category VARCHAR(255) NOT NULL COMMENT '技能分类',
    score INT DEFAULT 85 COMMENT '技能评分 (0-100)',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_skills_user_id (user_id),
    INDEX idx_skills_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='技能表';

-- ===================================================================
-- 技能项目表 (skill_items)
-- ===================================================================
-- 说明: 技能具体项目
CREATE TABLE IF NOT EXISTS skill_items (
    skill_id BIGINT NOT NULL COMMENT '技能ID',
    item_name VARCHAR(255) NOT NULL COMMENT '技能项目名称',
    PRIMARY KEY (skill_id, item_name),
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='技能项目表';

-- ===================================================================
-- 语言表 (languages)
-- ===================================================================
-- 说明: 用户语言能力，扩展多语言字段
CREATE TABLE IF NOT EXISTS languages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '语言ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(255) COMMENT '语言名称 (默认/英文)',
    name_zh VARCHAR(255) COMMENT '语言名称 (中文)',
    name_ja VARCHAR(255) COMMENT '语言名称 (日语)',
    level VARCHAR(255) COMMENT '语言水平 (默认/英文)',
    level_zh VARCHAR(255) COMMENT '语言水平 (中文)',
    level_ja VARCHAR(255) COMMENT '语言水平 (日语)',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_languages_user_id (user_id),
    INDEX idx_languages_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='语言表';

-- ===================================================================
-- 统计表 (stats)
-- ===================================================================
-- 说明: 用户统计数据
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
CREATE TABLE IF NOT EXISTS stat_tags (
    stat_id BIGINT NOT NULL COMMENT '统计ID',
    tag_name VARCHAR(255) NOT NULL COMMENT '标签名称',
    PRIMARY KEY (stat_id, tag_name),
    FOREIGN KEY (stat_id) REFERENCES stats(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='统计标签表';
