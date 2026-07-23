-- 用户信息表 - 增量变更脚本 V1.0.1
-- 变更内容：
-- 1. 新增 avatar 字段（头像 URL）
-- 2. 新增 department_id 字段（部门 ID）
-- 3. 新增 role_id 字段（角色 ID）
-- 4. 插入样例数据

-- ============================================
-- 1. 新增字段
-- ============================================

-- 添加 avatar 字段（头像）
ALTER TABLE `idap_user_info`
ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL' AFTER `note`;

-- 添加 department_id 字段（部门 ID）
ALTER TABLE `idap_user_info`
ADD COLUMN `department_id` VARCHAR(32) DEFAULT NULL COMMENT '部门 ID' AFTER `mobile`;

-- 添加 role_id 字段（角色 ID）
ALTER TABLE `idap_user_info`
ADD COLUMN `role_id` VARCHAR(32) DEFAULT NULL COMMENT '角色 ID' AFTER `department_id`;

-- ============================================
-- 2. 新增索引
-- ============================================

-- 添加部门 ID 索引
ALTER TABLE `idap_user_info`
ADD INDEX `idx_department_id` (`department_id`);

-- 添加角色 ID 索引
ALTER TABLE `idap_user_info`
ADD INDEX `idx_role_id` (`role_id`);

-- ============================================
-- 3. 插入样例数据
-- ============================================

-- 清理现有测试数据（可选，生产环境请注释）
DELETE FROM `idap_user_info` WHERE `deleted` = 0;

-- 插入管理员用户
INSERT INTO `idap_user_info`
(`user_id`, `user_name`, `email`, `mobile`, `sex`, `note`, `status`, `avatar`, `department_id`, `role_id`, `create_user`, `update_user`)
VALUES
('U000000000000000001', 'admin', 'admin@idap.com', '13800000000', 1, '系统管理员', 1,
 'https://example.com/avatar/admin.png', 'DEPT001', 'ROLE_ADMIN', 'system', 'system');

-- 插入普通用户样例
INSERT INTO `idap_user_info`
(`user_id`, `user_name`, `email`, `mobile`, `sex`, `note`, `status`, `avatar`, `department_id`, `role_id`, `create_user`, `update_user`)
VALUES
('U000000000000000002', 'zhangsan', 'zhangsan@idap.com', '13800000001', 1, '普通用户', 1,
 NULL, 'DEPT002', 'ROLE_USER', 'system', 'system'),
('U000000000000000003', 'lisi', 'lisi@idap.com', '13800000002', 2, '普通用户', 1,
 NULL, 'DEPT002', 'ROLE_USER', 'system', 'system'),
('U000000000000000004', 'wangwu', 'wangwu@idap.com', '13800000003', 1, '普通用户', 1,
 NULL, 'DEPT003', 'ROLE_USER', 'system', 'system'),
('U000000000000000005', 'zhaoliu', 'zhaoliu@idap.com', '13800000004', 2, '普通用户', 0,
 NULL, 'DEPT003', 'ROLE_USER', 'system', 'system');

-- 批量插入测试数据（使用 INSERT IGNORE 避免重复）
INSERT IGNORE INTO `idap_user_info`
(`user_id`, `user_name`, `email`, `mobile`, `sex`, `note`, `status`, `create_user`, `update_user`)
VALUES
('U100000000000000001', '测试用户 001', 'test001@test.com', '13900000001', 1, '测试账号', 1, 'system', 'system'),
('U100000000000000002', '测试用户 002', 'test002@test.com', '13900000002', 2, '测试账号', 1, 'system', 'system'),
('U100000000000000003', '测试用户 003', 'test003@test.com', '13900000003', 1, '测试账号', 1, 'system', 'system'),
('U100000000000000004', '测试用户 004', 'test004@test.com', '13900000004', 2, '测试账号', 1, 'system', 'system'),
('U100000000000000005', '测试用户 005', 'test005@test.com', '13900000005', 1, '测试账号', 1, 'system', 'system'),
('U100000000000000006', '测试用户 006', 'test006@test.com', '13900000006', 2, '测试账号', 0, 'system', 'system'),
('U100000000000000007', '测试用户 007', 'test007@test.com', '13900000007', 1, '测试账号', 1, 'system', 'system'),
('U100000000000000008', '测试用户 008', 'test008@test.com', '13900000008', 2, '测试账号', 1, 'system', 'system'),
('U100000000000000009', '测试用户 009', 'test009@test.com', '13900000009', 1, '测试账号', 1, 'system', 'system'),
('U100000000000000010', '测试用户 010', 'test010@test.com', '13900000010', 2, '测试账号', 1, 'system', 'system');
