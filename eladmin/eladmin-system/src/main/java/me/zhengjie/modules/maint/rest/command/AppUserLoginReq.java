package me.zhengjie.modules.maint.rest.command;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class AppUserLoginReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}