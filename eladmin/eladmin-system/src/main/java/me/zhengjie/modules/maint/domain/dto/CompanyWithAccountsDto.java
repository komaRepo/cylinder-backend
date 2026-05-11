package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;

import java.util.List;

/**
 * 企业列表DTO，包含绑定的账号信息
 * @author koma at cylinder-backend
 * @since 2026/5/11
 */
@Data
@Schema(description = "企业列表DTO")
public class CompanyWithAccountsDto {

    @Schema(description = "企业信息")
    private Company company;

    @Schema(description = "绑定的账号列表")
    private List<UserInfoDTO> boundAccounts;
}
