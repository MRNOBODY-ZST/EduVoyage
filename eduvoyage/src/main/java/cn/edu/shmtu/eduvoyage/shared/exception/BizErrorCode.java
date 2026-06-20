package cn.edu.shmtu.eduvoyage.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Central catalogue of business error codes. Code ranges by module keep them
 * collision-free as the platform grows:
 *
 * <pre>
 *   10xxx  common / framework
 *   11xxx  identity (auth, user, org, rbac)
 *   12xxx  course / chapter / knowledge node
 *   13xxx  knowledge graph (edges, algorithms)
 *   14xxx  assessment (question, homework, submission)
 *   15xxx  drive (storage, quota, share)
 *   16xxx  interaction (discussion, notification)
 *   17xxx  analytics
 * </pre>
 *
 * <p>{@link #httpStatus} lets the global handler map a business error onto an
 * appropriate HTTP status while {@link #code} stays stable for clients.</p>
 */
public enum BizErrorCode {

    // ---- common (10xxx) ----
    SYSTEM_ERROR(10000, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR),
    PARAM_INVALID(10001, "参数校验失败", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(10002, "资源不存在", HttpStatus.NOT_FOUND),
    OPERATION_NOT_ALLOWED(10003, "操作不被允许", HttpStatus.FORBIDDEN),
    RATE_LIMITED(10004, "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),
    DATA_CONFLICT(10005, "数据冲突", HttpStatus.CONFLICT),

    // ---- identity (11xxx) ----
    UNAUTHENTICATED(11000, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(11001, "无访问权限", HttpStatus.FORBIDDEN),
    BAD_CREDENTIALS(11002, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(11003, "账号已被锁定，请稍后再试", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(11004, "账号已被禁用", HttpStatus.FORBIDDEN),
    TOKEN_INVALID(11005, "令牌无效", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(11006, "令牌已过期", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(11007, "刷新令牌无效或已被吊销", HttpStatus.UNAUTHORIZED),
    CAPTCHA_INVALID(11008, "验证码错误或已过期", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTS(11009, "用户名已存在", HttpStatus.CONFLICT),
    EMAIL_EXISTS(11010, "邮箱已被注册", HttpStatus.CONFLICT),
    PHONE_EXISTS(11011, "手机号已被注册", HttpStatus.CONFLICT),

    // ---- knowledge graph (13xxx) ----
    GRAPH_CYCLE(13000, "新增前置关系会形成环，已拒绝", HttpStatus.CONFLICT),
    GRAPH_NODE_NOT_FOUND(13001, "知识点不存在", HttpStatus.NOT_FOUND),
    GRAPH_EDGE_EXISTS(13002, "该关系已存在", HttpStatus.CONFLICT),

    // ---- assessment (14xxx) ----
    HOMEWORK_CLOSED(14000, "作业已截止，无法提交", HttpStatus.FORBIDDEN),
    SUBMISSION_LIMIT_EXCEEDED(14001, "已超过最大提交次数", HttpStatus.FORBIDDEN),
    SUBMISSION_EXPIRED(14002, "答题时间已结束", HttpStatus.FORBIDDEN),

    // ---- drive (15xxx) ----
    QUOTA_EXCEEDED(15000, "存储空间不足", HttpStatus.CONTENT_TOO_LARGE),
    FILE_NOT_FOUND(15001, "文件不存在", HttpStatus.NOT_FOUND),
    SHARE_EXPIRED(15002, "分享链接已失效", HttpStatus.GONE),
    SHARE_CODE_INVALID(15003, "提取码错误", HttpStatus.FORBIDDEN),
    UPLOAD_SESSION_INVALID(15004, "上传会话无效或已过期", HttpStatus.BAD_REQUEST),
    STORAGE_ERROR(15005, "存储服务异常", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    BizErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
