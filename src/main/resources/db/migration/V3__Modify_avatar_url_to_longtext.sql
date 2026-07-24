-- ===================================================================
-- Modify avatar_url column to LONGTEXT to support Base64 images
-- ===================================================================
ALTER TABLE users MODIFY COLUMN avatar_url LONGTEXT COMMENT '头像URL或Base64数据';
