-- Data for users table
INSERT INTO users (id, name, location, email, avatar_url, status, job_title, bio, graduation_year, github_url, major) VALUES
(1, 'Listen', 'Japan / Tokyo', 'listen2code@gmail.com', 'https://api.dicebear.com/7.x/avataaars/png?seed=Listen', 'available', 'Full Stack Mobile Architect', 'A seasoned mobile developer with over 10 years of experience in Android and 2+ years in Flutter. Specialized in high-performance application development, clean architecture, and reactive programming. Proven track record of leading cross-functional teams and delivering complex enterprise solutions.', '2013', 'https://github.com/listen2code', 'softwareEngineering');

-- Data for projects table
INSERT INTO projects (id, title, subtitle, description, image_url, github_url) VALUES
(1, 'lPortfolio Flutter', 'Current Project', 'My personal portfolio app (this one!). Demonstrating Clean Architecture, MVI pattern, and advanced Riverpod state management in Flutter.', 'localhost/resource/images/project1.jpg', 'https://github.com/listen2code/ListenPortfolioFlutter'),
(2, 'Listen Core Flutter', 'Framework', 'A foundational framework for Flutter projects providing base classes for MVI, standardized network wrappers, and lifecycle management.', 'localhost/resource/images/project2.jpg', 'https://github.com/listen2code/ListenCoreFlutter'),
(3, 'Listen UI Kit', 'Common Library', 'A comprehensive UI component library for consistent branding and rapid development across multiple Flutter applications.', 'localhost/resource/images/project3.jpg', 'https://github.com/listen2code/ListenUikitFlutter'),
(4, 'Portfolio Backend', 'Cloud Infrastructure', 'The server-side implementation for this portfolio, managing user data, projects, and dynamic configurations.', 'localhost/resource/images/project4.jpg', 'https://github.com/listen2code/ListenPortfolioBackend'),
(5, 'Tech Knowledge Base', 'Articles & Docs', 'A curated collection of my technical articles, architecture notes, and development experiences over the past 10 years.', 'localhost/resource/images/project5.jpg', 'https://github.com/listen2code/article');

-- Data for project_tech_stack table
INSERT INTO project_tech_stack (project_id, tech_name) VALUES
(1, 'Flutter'), (1, 'Riverpod'), (1, 'Clean Architecture'), (1, 'MVI'),
(2, 'Dart'), (2, 'Riverpod'), (2, 'Dio'), (2, 'Architecture'),
(3, 'Flutter'), (3, 'Design System'), (3, 'CustomPainter'),
(4, 'Node.js'), (4, 'Express'), (4, 'AWS'), (4, 'DynamoDB'),
(5, 'Markdown'), (5, 'Documentation'), (5, 'Knowledge Sharing');

-- Data for experiences table
INSERT INTO experiences (user_id, title, company, period, description) VALUES
(1, 'Senior Mobile Architect', 'Global Tech Solutions', '2021 - Present', 'Leading the migration of core native apps to Flutter, optimizing CI/CD pipelines, and establishing mobile engineering best practices.'),
(1, 'Lead Android Developer', 'Innovation Hub', '2015 - 2021', 'Designed and developed large-scale financial applications with millions of active users. Implemented robust security protocols.'),
(1, 'Junior Developer', 'Start-up Inc.', '2013 - 2015', 'Focusing on UI/UX implementation and RESTful API integration for Android platform.');

-- Data for education table
INSERT INTO education (user_id, degree, school, period, description) VALUES
(1, 'Bachelor of Computer Science', 'Tech University', '2009 - 2013', 'Specialized in Software Engineering and Mobile Systems.');

-- Data for skills table
INSERT INTO skills (user_id, category, items) VALUES
(1, 'Mobile', '["Flutter","Android Native","Dart","Kotlin","Java"]'),
(1, 'Architecture', '["Clean Architecture","MVI","MVVM","SOLID"]'),
(1, 'Backend & DevOps', '["Spring Boot","SQL","Docker","CI/CD"]');

-- Data for languages table
INSERT INTO languages (user_id, name, level) VALUES
(1, 'English', 'CET4'),
(1, 'Japanese', 'N1'),
(1, 'Chinese', 'Native');

-- Data for certifications table
INSERT INTO certifications (user_id, name) VALUES
(1, 'jlptN1'),
(1, 'bjtJ2');

-- Data for stats table
INSERT INTO stats (id, user_id, year, label) VALUES
('android', 1, '10', 'androidExp'),
('flutter', 1, '2', 'flutterExp'),
('java_web', 1, '1', 'javaWeb');

-- Data for stat_tags table
INSERT INTO stat_tags (stat_id, tag_name) VALUES
('android', 'archDesign'),
('android', 'perfOptimization');
