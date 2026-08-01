package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.common.utils.Md5Utils;
import com.xiuwen.framework.config.FileUploadProperties;
import com.xiuwen.framework.security.LoginUserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiuwen.system.dto.AddressSaveRequest;
import com.xiuwen.system.dto.ChangePasswordRequest;
import com.xiuwen.system.dto.UpdateProfileRequest;
import com.xiuwen.system.entity.FileResource;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.entity.UserAddress;
import com.xiuwen.system.service.FileResourceService;
import com.xiuwen.system.service.UserAddressService;
import com.xiuwen.system.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户个人中心接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserService userService;
    private final UserAddressService userAddressService;
    private final FileResourceService fileResourceService;
    private final FileUploadProperties fileUploadProperties;

    public UserProfileController(UserService userService,
                                 UserAddressService userAddressService,
                                 FileResourceService fileResourceService,
                                 FileUploadProperties fileUploadProperties) {
        this.userService = userService;
        this.userAddressService = userAddressService;
        this.fileResourceService = fileResourceService;
        this.fileUploadProperties = fileUploadProperties;
    }

    /** [2.1] 查看个人资料 */
    @GetMapping("/profile")
    public Result<User> profile() {
        Long userId = LoginUserHolder.getRequiredUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPasswordHash(null);
        return Result.success(user);
    }

    /** [2.2] 修改昵称、邮箱、性别、生日、简介等资料 */
    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = LoginUserHolder.getRequiredUserId();
        User user = new User();
        BeanUtils.copyProperties(request, user);
        if (request.getBirthday() != null && !request.getBirthday().isEmpty()) {
            user.setBirthday(LocalDate.parse(request.getBirthday()));
        }
        user.setId(userId);
        userService.updateById(user);
        User updated = userService.getById(userId);
        updated.setPasswordHash(null);
        return Result.success(updated);
    }

    /** [2.3] 上传或更换头像 */
    @PostMapping("/avatar")
    public Result<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = LoginUserHolder.getRequiredUserId();
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        File uploadDir = new File(fileUploadProperties.getUploadPath());
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            log.error("无法创建上传目录: {}", uploadDir.getAbsolutePath());
            throw new BusinessException("上传目录创建失败");
        }

        File dest = new File(uploadDir, storedName);
        try {
            Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("头像文件写入失败: {}", dest.getAbsolutePath(), e);
            throw new BusinessException("头像上传失败，请重试");
        }

        String accessPrefix = fileUploadProperties.getAccessPrefix();
        if (!accessPrefix.startsWith("/")) {
            accessPrefix = "/" + accessPrefix;
        }
        if (accessPrefix.endsWith("/")) {
            accessPrefix = accessPrefix.substring(0, accessPrefix.length() - 1);
        }
        String fileUrl = accessPrefix + "/" + storedName;

        FileResource resource = new FileResource();
        resource.setUserId(userId);
        resource.setBizType("AVATAR");
        resource.setOriginalName(originalName);
        resource.setFileUrl(fileUrl);
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        fileResourceService.save(resource);

        User update = new User();
        update.setId(userId);
        update.setAvatar(fileUrl);
        userService.updateById(update);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fileId", resource.getId());
        data.put("avatar", fileUrl);
        return Result.success(data);
    }

    /** [2.4] 修改密码 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = LoginUserHolder.getRequiredUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!Md5Utils.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        User update = new User();
        update.setId(userId);
        update.setPasswordHash(Md5Utils.md5(request.getNewPassword()));
        userService.updateById(update);
        return Result.success();
    }

    /** [3.1] 收货地址列表 */
    @GetMapping("/addresses")
    public Result<java.util.List<UserAddress>> addressList() {
        Long userId = LoginUserHolder.getRequiredUserId();
        java.util.List<UserAddress> list = userAddressService.list(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt));
        return Result.success(list);
    }

    /** [3.2] 新增收货地址 */
    @PostMapping("/addresses")
    public Result<UserAddress> addAddress(@Valid @RequestBody AddressSaveRequest request) {
        Long userId = LoginUserHolder.getRequiredUserId();
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        }
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(request, address);
        address.setUserId(userId);
        userAddressService.save(address);
        return Result.success(address);
    }

    /** [3.3] 修改收货地址 */
    @PutMapping("/addresses/{id}")
    public Result<UserAddress> updateAddress(@PathVariable Long id,
                                             @Valid @RequestBody AddressSaveRequest request) {
        UserAddress existing = userAddressService.getById(id);
        if (existing == null) {
            throw new BusinessException("地址不存在");
        }
        Long userId = LoginUserHolder.getRequiredUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此地址");
        }
        if (request.getIsDefault() != null && request.getIsDefault() == 1
                && (existing.getIsDefault() == null || existing.getIsDefault() != 1)) {
            clearDefaultAddress(userId);
        }
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(request, address);
        address.setId(id);
        userAddressService.updateById(address);
        UserAddress updated = userAddressService.getById(id);
        return Result.success(updated);
    }

    /** [3.4] 删除收货地址 */
    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        UserAddress existing = userAddressService.getById(id);
        if (existing == null) {
            throw new BusinessException("地址不存在");
        }
        Long userId = LoginUserHolder.getRequiredUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此地址");
        }
        userAddressService.removeById(id);
        return Result.success();
    }

    /** [3.5] 设置默认收货地址 */
    @PutMapping("/addresses/{addressId}/default")
    public Result<UserAddress> setDefaultAddress(@PathVariable Long addressId) {
        UserAddress existing = userAddressService.getById(addressId);
        if (existing == null) {
            throw new BusinessException("地址不存在");
        }
        Long userId = LoginUserHolder.getRequiredUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此地址");
        }
        clearDefaultAddress(userId);
        UserAddress update = new UserAddress();
        update.setId(addressId);
        update.setIsDefault(1);
        userAddressService.updateById(update);
        UserAddress updated = userAddressService.getById(addressId);
        return Result.success(updated);
    }

    private void clearDefaultAddress(Long userId) {
        UserAddress update = new UserAddress();
        update.setIsDefault(0);
        userAddressService.update(update,
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getIsDefault, 1));
    }

}
