-- V2 迁移：添加测试用户
-- 说明：向 users 表插入一条测试数据
INSERT INTO users (name, email, password, location, avatar_url, status, job_title, bio, graduation_year, github_url, major) 
VALUES ('Listn2', 'listen2@gmail.com', '$2a$10$3Fa2JeWy.qEFQulYDtYhGO4g/gHg8nKgkSkp0KvEmGiZZIJqbdVIK', 'Test Location', 'https://example.com/avatar.jpg', 'active', 'Test Engineer', 'This is a test user for demonstration', '2023', 'https://github.com/testuser', 'Computer Science');
