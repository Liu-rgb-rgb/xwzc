package com.xiuwen.system.dto;

import lombok.Data;

/**
 * 修改个人资料请求。
 */
@Data
public class UpdateProfileRequest {
    private String nickname;
    private String phone;
    private String email;
    private String gender;
    private String birthday;
    private String intro;
}
