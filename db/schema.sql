-- Users table based on user.json and aboutMe.json
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    avatar_url VARCHAR(255),
    status VARCHAR(255),
    job_title VARCHAR(255),
    bio TEXT,
    graduation_year VARCHAR(4),
    github_url VARCHAR(255),
    major VARCHAR(255)
);

-- Projects table based on projects.json
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    description TEXT,
    image_url VARCHAR(255),
    github_url VARCHAR(255)
);

-- Tech stack for projects (many-to-many relationship)
CREATE TABLE project_tech_stack (
    project_id BIGINT,
    tech_name VARCHAR(255),
    PRIMARY KEY (project_id, tech_name),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Experiences table based on aboutMe.json
CREATE TABLE experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    title VARCHAR(255),
    company VARCHAR(255),
    period VARCHAR(255),
    description TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Education table based on aboutMe.json
CREATE TABLE education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    degree VARCHAR(255),
    school VARCHAR(255),
    period VARCHAR(255),
    description TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Skills table based on aboutMe.json
CREATE TABLE skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    category VARCHAR(255),
    items TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Languages table based on aboutMe.json
CREATE TABLE languages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    name VARCHAR(255),
    level VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Certifications table based on aboutMe.json
CREATE TABLE certifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    name VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Stats table based on aboutMe.json
CREATE TABLE stats (
    id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT,
    year VARCHAR(255),
    label VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tags for stats (many-to-many relationship)
CREATE TABLE stat_tags (
    stat_id VARCHAR(255),
    tag_name VARCHAR(255),
    PRIMARY KEY (stat_id, tag_name),
    FOREIGN KEY (stat_id) REFERENCES stats(id) ON DELETE CASCADE
);
