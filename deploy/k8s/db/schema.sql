-- =====================================================================
--  EduVoyage MySQL schema (R2DBC sql.init in dev; Flyway-over-JDBC in prod)
--  Charset utf8mb4; PK = application-generated Snowflake BIGINT.
--  Logical FKs (indexed) enforced at the application layer for R2DBC/shard
--  friendliness. All business tables carry audit columns + logical delete.
-- =====================================================================

-- ============ identity: user / RBAC / org ============
CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT       PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL,
  password      VARCHAR(100) NOT NULL,
  real_name     VARCHAR(64),
  email         VARCHAR(128),
  phone         VARCHAR(20),
  avatar_url    VARCHAR(512),
  gender        TINYINT      DEFAULT 0,
  status        TINYINT      NOT NULL DEFAULT 1,
  class_id      BIGINT,
  last_login_at DATETIME,
  created_by    BIGINT,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_phone (phone),
  KEY idx_user_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT      PRIMARY KEY,
  code        VARCHAR(64) NOT NULL,
  name        VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  created_by  BIGINT,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_permission (
  id          BIGINT       PRIMARY KEY,
  code        VARCHAR(100) NOT NULL,
  name        VARCHAR(100) NOT NULL,
  type        TINYINT      NOT NULL DEFAULT 1,
  parent_id   BIGINT       DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_perm_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_ur_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  KEY idx_rp_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限';

CREATE TABLE IF NOT EXISTS org_department (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64),
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dept_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院系';

CREATE TABLE IF NOT EXISTS org_major (
  id BIGINT PRIMARY KEY,
  department_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64),
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_major_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业';

CREATE TABLE IF NOT EXISTS org_class (
  id BIGINT PRIMARY KEY,
  major_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  grade INT,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_class_major (major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

-- ============ course: course / chapter / knowledge node ============
CREATE TABLE IF NOT EXISTS course (
  id           BIGINT PRIMARY KEY,
  title        VARCHAR(200) NOT NULL,
  cover_url    VARCHAR(512),
  intro        TEXT,
  credit       DECIMAL(4,1) DEFAULT 0,
  teacher_id   BIGINT NOT NULL,
  visibility   TINYINT NOT NULL DEFAULT 0,
  status       TINYINT NOT NULL DEFAULT 0,
  start_date   DATE,
  end_date     DATE,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_course_teacher (teacher_id),
  KEY idx_course_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

CREATE TABLE IF NOT EXISTS course_teacher (
  course_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  role TINYINT DEFAULT 1,
  PRIMARY KEY (course_id, teacher_id),
  KEY idx_ct_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程-教师';

CREATE TABLE IF NOT EXISTS course_class_scope (
  course_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程可见班级';

CREATE TABLE IF NOT EXISTS course_chapter (
  id          BIGINT PRIMARY KEY,
  course_id   BIGINT NOT NULL,
  parent_id   BIGINT NOT NULL DEFAULT 0,
  title       VARCHAR(200) NOT NULL,
  sort_no     INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_chapter_course (course_id),
  KEY idx_chapter_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节';

CREATE TABLE IF NOT EXISTS knowledge_graph (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_graph_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱';

CREATE TABLE IF NOT EXISTS knowledge_node (
  id            BIGINT PRIMARY KEY,
  course_id     BIGINT NOT NULL,
  chapter_id    BIGINT,
  graph_id      BIGINT NOT NULL,
  name          VARCHAR(200) NOT NULL,
  description   TEXT,
  learn_goal    VARCHAR(512),
  est_minutes   INT DEFAULT 0,
  pos_x         DOUBLE,
  pos_y         DOUBLE,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_node_course (course_id),
  KEY idx_node_graph (graph_id),
  KEY idx_node_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点节点';

CREATE TABLE IF NOT EXISTS knowledge_edge (
  id        BIGINT PRIMARY KEY,
  graph_id  BIGINT NOT NULL,
  from_id   BIGINT NOT NULL,
  to_id     BIGINT NOT NULL,
  type      VARCHAR(20) NOT NULL,
  weight    DOUBLE DEFAULT 1,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_edge (from_id, to_id, type),
  KEY idx_edge_from (from_id, to_id),
  KEY idx_edge_graph (graph_id),
  KEY idx_edge_to (to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点关系边';

CREATE TABLE IF NOT EXISTS courseware (
  id           BIGINT PRIMARY KEY,
  node_id      BIGINT NOT NULL,
  title        VARCHAR(200) NOT NULL,
  type         TINYINT NOT NULL,
  content_ref  VARCHAR(64),
  file_id      BIGINT,
  duration_sec INT,
  sort_no INT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_cw_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件';

CREATE TABLE IF NOT EXISTS course_enrollment (
  id BIGINT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  progress DECIMAL(5,2) DEFAULT 0,
  enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_enroll (course_id, student_id),
  KEY idx_enroll_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课';

CREATE TABLE IF NOT EXISTS course_favorite (
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程收藏';

-- ============ knowledgegraph: mastery ============
CREATE TABLE IF NOT EXISTS knowledge_mastery (
  id BIGINT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  node_id BIGINT NOT NULL,
  mastery_level TINYINT NOT NULL DEFAULT 0,
  score DECIMAL(5,2) DEFAULT 0,
  learn_progress DECIMAL(5,2) DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_mastery (student_id, node_id),
  KEY idx_mastery_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点掌握度';

-- ============ assessment: question / homework / submission ============
CREATE TABLE IF NOT EXISTS question (
  id          BIGINT PRIMARY KEY,
  course_id   BIGINT,
  type        TINYINT NOT NULL,
  stem        TEXT NOT NULL,
  answer      TEXT,
  analysis    TEXT,
  difficulty  TINYINT DEFAULT 1,
  node_id     BIGINT,
  lang        VARCHAR(20),
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_q_course (course_id),
  KEY idx_q_node (node_id),
  KEY idx_q_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目';

CREATE TABLE IF NOT EXISTS question_option (
  id BIGINT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(8) NOT NULL,
  content TEXT NOT NULL,
  is_correct TINYINT NOT NULL DEFAULT 0,
  sort_no INT DEFAULT 0,
  KEY idx_opt_q (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项';

CREATE TABLE IF NOT EXISTS homework (
  id          BIGINT PRIMARY KEY,
  course_id   BIGINT NOT NULL,
  title       VARCHAR(200) NOT NULL,
  total_score DECIMAL(6,2) DEFAULT 100,
  time_limit  INT,
  deadline    DATETIME,
  max_attempts INT DEFAULT 1,
  shuffle     TINYINT DEFAULT 0,
  anti_switch TINYINT DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_hw_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业/试卷';

CREATE TABLE IF NOT EXISTS homework_question (
  homework_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  score DECIMAL(5,2) NOT NULL DEFAULT 0,
  sort_no INT DEFAULT 0,
  PRIMARY KEY (homework_id, question_id),
  KEY idx_hq_q (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷-题目';

CREATE TABLE IF NOT EXISTS submission (
  id           BIGINT PRIMARY KEY,
  homework_id  BIGINT NOT NULL,
  student_id   BIGINT NOT NULL,
  attempt_no   INT NOT NULL DEFAULT 1,
  status       TINYINT NOT NULL DEFAULT 0,
  total_score  DECIMAL(6,2),
  submitted_at DATETIME,
  started_at DATETIME,
  switch_count INT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_sub_student_hw (student_id, homework_id),
  KEY idx_sub_hw (homework_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录';

CREATE TABLE IF NOT EXISTS submission_answer (
  id BIGINT PRIMARY KEY,
  submission_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer TEXT,
  score DECIMAL(5,2),
  is_correct TINYINT,
  comment VARCHAR(512),
  KEY idx_sa_sub (submission_id),
  KEY idx_sa_q (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作答明细';

CREATE TABLE IF NOT EXISTS wrong_book (
  id BIGINT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  node_id BIGINT,
  wrong_count INT DEFAULT 1,
  last_wrong_at DATETIME,
  mastered TINYINT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wrong (student_id, question_id),
  KEY idx_wrong_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- ============ drive: netdisk (dir tree + physical file) ============
CREATE TABLE IF NOT EXISTS drive_node (
  id          BIGINT PRIMARY KEY,
  owner_id    BIGINT NOT NULL,
  space_type  TINYINT NOT NULL DEFAULT 1,
  course_id   BIGINT,
  parent_id   BIGINT NOT NULL DEFAULT 0,
  name        VARCHAR(255) NOT NULL,
  is_dir      TINYINT NOT NULL DEFAULT 0,
  file_id     BIGINT,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_drive_owner (owner_id),
  KEY idx_drive_parent (parent_id),
  KEY idx_drive_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网盘节点';

CREATE TABLE IF NOT EXISTS drive_file (
  id        BIGINT PRIMARY KEY,
  sha256    CHAR(64) NOT NULL,
  size      BIGINT NOT NULL,
  mime      VARCHAR(128),
  bucket    VARCHAR(64) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  ref_count INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_file_sha (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理文件';

CREATE TABLE IF NOT EXISTS drive_share (
  id BIGINT PRIMARY KEY,
  node_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  token VARCHAR(64) NOT NULL,
  extract_code VARCHAR(16),
  expire_at DATETIME,
  view_count INT DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_share_token (token),
  KEY idx_share_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享';

CREATE TABLE IF NOT EXISTS drive_quota (
  user_id BIGINT PRIMARY KEY,
  total_bytes BIGINT NOT NULL,
  used_bytes BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储配额';
