package cn.edu.shmtu.eduvoyage.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新令牌请求")
public record RefreshRequest(
        @Schema(description = "刷新令牌")
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) {
}
