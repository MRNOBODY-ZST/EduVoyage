-- =====================================================================
--  EduVoyage seed data (dev profile). Idempotent via INSERT IGNORE.
--  Default accounts (CHANGE in any non-dev environment):
--    admin   / Admin@123    (ADMIN)
--    teacher / Teacher@123  (TEACHER)
--    student / Student@123  (STUDENT)
--  Passwords are BCrypt ($2a$10$...) hashes.
-- =====================================================================

-- ---- roles ----
INSERT IGNORE INTO sys_role (id, code, name, description, created_by) VALUES
  (1, 'ADMIN',   '平台管理员', '平台最高权限', 0),
  (2, 'TEACHER', '教师',     '建课、组卷、教学分析', 0),
  (3, 'STUDENT', '学生',     '选课、学习、作业', 0);

-- ---- permissions (representative set; extended per module in Phase 3) ----
INSERT IGNORE INTO sys_permission (id, code, name, type, parent_id) VALUES
  (100, 'user:read',     '查看用户',   3, 0),
  (101, 'user:create',   '创建用户',   3, 0),
  (102, 'user:update',   '编辑用户',   3, 0),
  (103, 'user:delete',   '删除用户',   3, 0),
  (110, 'org:manage',    '组织管理',   3, 0),
  (200, 'course:read',   '查看课程',   3, 0),
  (201, 'course:create', '创建课程',   3, 0),
  (202, 'course:update', '编辑课程',   3, 0),
  (203, 'course:delete', '删除课程',   3, 0),
  (210, 'course:enroll', '选课',      3, 0),
  (300, 'graph:read',    '查看图谱',   3, 0),
  (301, 'graph:edit',    '编辑图谱',   3, 0),
  (400, 'homework:read',  '查看作业',  3, 0),
  (401, 'homework:create','布置作业',  3, 0),
  (402, 'homework:grade', '批改作业',  3, 0),
  (410, 'homework:submit','提交作业',  3, 0),
  (500, 'drive:read',     '查看网盘',  3, 0),
  (501, 'drive:write',    '网盘读写',  3, 0),
  (600, 'analytics:view', '查看分析',  3, 0);

-- ---- role-permission bindings ----
-- ADMIN: all permissions
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
  SELECT 1, id FROM sys_permission;

-- TEACHER: course/graph/homework authoring + analytics + drive + read users
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
  (2, 100),
  (2, 200), (2, 201), (2, 202), (2, 203),
  (2, 300), (2, 301),
  (2, 400), (2, 401), (2, 402),
  (2, 500), (2, 501),
  (2, 600);

-- STUDENT: read course/graph, enroll, submit homework, drive, self analytics
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
  (3, 200), (3, 210),
  (3, 300),
  (3, 400), (3, 410),
  (3, 500), (3, 501),
  (3, 600);

-- ---- sample organization ----
INSERT IGNORE INTO org_department (id, name, code, created_by) VALUES
  (1, '信息工程学院', 'SIE', 0);
INSERT IGNORE INTO org_major (id, department_id, name, code, created_by) VALUES
  (1, 1, '计算机科学与技术', 'CS', 0);
INSERT IGNORE INTO org_class (id, major_id, name, grade, created_by) VALUES
  (1, 1, '计算机2401班', 2024, 0);

-- ---- default users ----
INSERT IGNORE INTO sys_user (id, username, password, real_name, email, status, class_id, created_by) VALUES
  (1, 'admin',   '$2a$10$sXQ0edQ4Ef/F9W1gx5AmQOqd8rMIpSPaRSR10PfOqPCIb3NhPAYLu', '系统管理员', 'admin@shmtu.edu.cn',   1, NULL, 0),
  (2, 'teacher', '$2a$10$mIpUtX9hMjFRDHlNQV53/ub8b8XE4pysFwMqpRmUpVqfFORsvEqU6', '示例教师',   'teacher@shmtu.edu.cn', 1, NULL, 0),
  (3, 'student', '$2a$10$216x2aGkb6QT2Slwi2Vgxu1Nn.GRUcYBQ8QQv9ial6452iwyitWJC', '示例学生',   'student@shmtu.edu.cn', 1, 1,    0);

-- ---- user-role bindings ----
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3);

-- ---- default storage quotas ----
INSERT IGNORE INTO drive_quota (user_id, total_bytes, used_bytes) VALUES
  (1, 53687091200, 0),
  (2, 10737418240, 0),
  (3, 2147483648, 0);
