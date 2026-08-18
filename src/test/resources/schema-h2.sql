-- H2 Database Schema for Unit & Integration Testing

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    location_zh VARCHAR(255),
    location_ja VARCHAR(255),
    avatar_url CLOB,
    status VARCHAR(255),
    job_title VARCHAR(255),
    job_title_zh VARCHAR(255),
    job_title_ja VARCHAR(255),
    bio CLOB,
    bio_zh CLOB,
    bio_ja CLOB,
    graduation_year VARCHAR(10),
    github_url VARCHAR(255),
    major VARCHAR(255),
    major_zh VARCHAR(255),
    major_ja VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_certifications (
    user_id BIGINT NOT NULL,
    certification_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, certification_name)
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_id VARCHAR(255) UNIQUE,
    title VARCHAR(255) NOT NULL,
    title_zh VARCHAR(255),
    title_ja VARCHAR(255),
    subtitle VARCHAR(255),
    subtitle_zh VARCHAR(255),
    subtitle_ja VARCHAR(255),
    project_desc CLOB,
    project_desc_zh CLOB,
    project_desc_ja CLOB,
    image_url VARCHAR(255),
    github_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS project_tech_stack (
    project_id BIGINT NOT NULL,
    tech_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (project_id, tech_name)
);

CREATE TABLE IF NOT EXISTS experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    title_zh VARCHAR(255),
    title_ja VARCHAR(255),
    company VARCHAR(255),
    company_zh VARCHAR(255),
    company_ja VARCHAR(255),
    period VARCHAR(255),
    description CLOB,
    description_zh CLOB,
    description_ja CLOB
);

CREATE TABLE IF NOT EXISTS education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    degree VARCHAR(255),
    degree_zh VARCHAR(255),
    degree_ja VARCHAR(255),
    school VARCHAR(255),
    school_zh VARCHAR(255),
    school_ja VARCHAR(255),
    period VARCHAR(255),
    description CLOB,
    description_zh CLOB,
    description_ja CLOB
);

CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS skill_items (
    skill_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (skill_id, item_name)
);

CREATE TABLE IF NOT EXISTS languages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    name_zh VARCHAR(255),
    name_ja VARCHAR(255),
    level VARCHAR(255),
    level_zh VARCHAR(255),
    level_ja VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    business_id VARCHAR(255) NOT NULL,
    year VARCHAR(255),
    label VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS stat_tags (
    stat_id BIGINT NOT NULL,
    tag_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (stat_id, tag_name)
);
