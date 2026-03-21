-- Users table for authentication and basic info
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Added for login
    location VARCHAR(255),
    avatar_url VARCHAR(255),
    status VARCHAR(255),
    job_title VARCHAR(255),
    bio TEXT,
    graduation_year VARCHAR(10),
    github_url VARCHAR(255),
    major VARCHAR(255)
);

-- Collection table for user certifications (One-to-Many)
CREATE TABLE user_certifications (
    user_id BIGINT NOT NULL,
    certification_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, certification_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Projects table
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_id VARCHAR(255) UNIQUE, -- For business logic ID from JSON
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    project_desc TEXT, -- Renamed to avoid conflict with 'desc' keyword
    image_url VARCHAR(255),
    github_url VARCHAR(255)
);

-- Collection table for project tech stack (One-to-Many)
CREATE TABLE project_tech_stack (
    project_id BIGINT NOT NULL,
    tech_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (project_id, tech_name),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Experiences table (Many-to-One with users)
CREATE TABLE experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    company VARCHAR(255),
    period VARCHAR(255),
    description TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Education table (Many-to-One with users)
CREATE TABLE education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    degree VARCHAR(255),
    school VARCHAR(255),
    period VARCHAR(255),
    description TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Skills table (Many-to-One with users)
CREATE TABLE skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Collection table for skill items (One-to-Many)
CREATE TABLE skill_items (
    skill_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (skill_id, item_name),
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- Languages table (Many-to-One with users)
CREATE TABLE languages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    level VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Stats table (Many-to-One with users)
CREATE TABLE stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    business_id VARCHAR(255) NOT NULL, -- For business logic ID from JSON
    year VARCHAR(255),
    label VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Collection table for stat tags (One-to-Many)
CREATE TABLE stat_tags (
    stat_id BIGINT NOT NULL,
    tag_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (stat_id, tag_name),
    FOREIGN KEY (stat_id) REFERENCES stats(id) ON DELETE CASCADE
);
