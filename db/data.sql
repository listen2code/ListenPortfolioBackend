-- Data for users table
-- Columns are reordered to match schema.sql for consistency
INSERT INTO users (id, name, email, password, location, avatar_url, status, job_title, bio, graduation_year, github_url, major) VALUES
(1, 'Listen', 'listen2code@gmail.com', '$2a$10$3Fa2JeWy.qEFQulYDtYhGO4g/gHg8nKgkSkp0KvEmGiZZIJqbdVIK', 'Japan / Tokyo', 'https://api.dicebear.com/7.x/avataaars/png?seed=Listen', 'available', 'Full Stack Mobile Architect', 'A seasoned mobile developer with over 10 years of experience in Android and 2+ years in Flutter. Specialized in high-performance application development, clean architecture, and reactive programming. Proven track record of leading cross-functional teams and delivering complex enterprise solutions.', '2013', 'https://github.com/listen2code', 'softwareEngineering');

-- Data for projects table
-- Added business_id to match schema
INSERT INTO projects (id, business_id, title, subtitle, project_desc, image_url, github_url) VALUES
(1, 'lportfolio-flutter', 'lPortfolio Flutter', 'Current Project', 'My personal portfolio app (this one!). Demonstrating Clean Architecture, MVI pattern, and advanced Riverpod state management in Flutter.', 'localhost/images/project1.jpg', 'https://github.com/listen2code/ListenPortfolioFlutter'),
(2, 'listen-core-flutter', 'Listen Core Flutter', 'Framework', 'A foundational framework for Flutter projects providing base classes for MVI, standardized network wrappers, and lifecycle management.', 'localhost/images/project2.jpg', 'https://github.com/listen2code/ListenCoreFlutter'),
(3, 'listen-ui-kit', 'Listen UI Kit', 'Common Library', 'A comprehensive UI component library for consistent branding and rapid development across multiple Flutter applications.', 'localhost/images/project3.jpg', 'https://github.com/listen2code/ListenUikitFlutter'),
(4, 'portfolio-backend', 'Portfolio Backend', 'Cloud Infrastructure', 'The server-side implementation for this portfolio, managing user data, projects, and dynamic configurations.', 'localhost/images/project4.jpg', 'https://github.com/listen2code/ListenPortfolioBackend'),
(5, 'tech-knowledge-base', 'Tech Knowledge Base', 'Articles & Docs', 'A curated collection of my technical articles, architecture notes, and development experiences over the past 10 years.', 'localhost/images/project5.jpg', 'https://github.com/listen2code/article');

-- Data for project_tech_stack table
INSERT INTO project_tech_stack (project_id, tech_name) VALUES
(1, 'Flutter'), (1, 'Riverpod'), (1, 'Clean Architecture'), (1, 'MVI'),
(2, 'Dart'), (2, 'Riverpod'), (2, 'Dio'), (2, 'Architecture'),
(3, 'Flutter'), (3, 'Design System'), (3, 'CustomPainter'),
(4, 'Spring Boot'), (4, 'JPA'), (4, 'AWS'), (4, 'MySQL'),
(5, 'Markdown'), (5, 'Documentation'), (5, 'Knowledge Sharing');

-- Data for experiences table
INSERT INTO experiences (id, user_id, title, company, period, description) VALUES
(1, 1, 'Senior Mobile Architect', 'Global Tech Solutions', '2021 - Present', 'Leading the migration of core native apps to Flutter, optimizing CI/CD pipelines, and establishing mobile engineering best practices.'),
(2, 1, 'Lead Android Developer', 'Innovation Hub', '2015 - 2021', 'Designed and developed large-scale financial applications with millions of active users. Implemented robust security protocols.'),
(3, 1, 'Junior Developer', 'Start-up Inc.', '2013 - 2015', 'Focusing on UI/UX implementation and RESTful API integration for Android platform.');

-- Data for education table
INSERT INTO education (id, user_id, degree, school, period, description) VALUES
(1, 1, 'Bachelor of Computer Science', 'Tech University', '2009 - 2013', 'Specialized in Software Engineering and Mobile Systems.');

-- Data for skills table
-- This has been corrected to follow the normalized schema
INSERT INTO skills (id, user_id, category) VALUES
(1, 1, 'Mobile'),
(2, 1, 'Architecture'),
(3, 1, 'Backend & DevOps');

-- Data for skill_items table
-- This new section populates the many-to-many relationship for skills
INSERT INTO skill_items (skill_id, item_name) VALUES
(1, 'Flutter'), (1, 'Android Native'), (1, 'Dart'), (1, 'Kotlin'), (1, 'Java'),
(2, 'Clean Architecture'), (2, 'MVI'), (2, 'MVVM'), (2, 'SOLID'),
(3, 'Spring Boot'), (3, 'SQL'), (3, 'Docker'), (3, 'CI/CD');

-- Data for languages table
INSERT INTO languages (id, user_id, name, level) VALUES
(1, 1, 'English', 'CET4'),
(2, 1, 'Japanese', 'N1'),
(3, 1, 'Chinese', 'Native');

-- Data for user_certifications table
-- Corrected table name from 'certifications' to 'user_certifications' and column 'name' to 'certification_name'
INSERT INTO user_certifications (user_id, certification_name) VALUES
(1, 'jlptN1'),
(1, 'bjtJ2');

-- Data for stats table
-- Corrected to use auto-incrementing ID and added business_id
INSERT INTO stats (id, user_id, business_id, year, label) VALUES
(1, 1, 'android', '10', 'androidExp'),
(2, 1, 'flutter', '2', 'flutterExp'),
(3, 1, 'java_web', '1', 'javaWeb');

-- Data for stat_tags table
-- Corrected to use the new integer IDs from the stats table
INSERT INTO stat_tags (stat_id, tag_name) VALUES
(1, 'archDesign'),
(1, 'perfOptimization'),
(2, 'stateManagement'),
(3, 'fullStack');
