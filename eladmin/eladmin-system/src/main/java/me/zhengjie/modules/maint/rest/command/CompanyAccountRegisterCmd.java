package me.zhengjie.modules.maint.rest.command;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CompanyAccountRegisterCmd {

    @NotNull(message = "企业ID不能为空")
    private Long companyId;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    private String gender = "男";
}
