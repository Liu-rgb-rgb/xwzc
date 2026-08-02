package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.config.FileUploadProperties;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.system.entity.FileResource;
import com.xiuwen.system.service.FileResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件上传接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/files")
public class FileController {

    private final FileResourceService fileResourceService;
    private final FileUploadProperties fileUploadProperties;

    public FileController(FileResourceService fileResourceService,
                          FileUploadProperties fileUploadProperties) {
        this.fileResourceService = fileResourceService;
        this.fileUploadProperties = fileUploadProperties;
    }

    /** [15.1] 商家后台文件上传 */
    @PostMapping("/upload")
    public Result<FileResource> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam("bizType") String bizType) {
        Long userId = LoginUserHolder.getRequiredUserId();
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (bizType == null || bizType.trim().isEmpty()) {
            throw new BusinessException("业务类型不能为空");
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
            log.error("文件写入失败: {}", dest.getAbsolutePath(), e);
            throw new BusinessException("文件上传失败，请重试");
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
        resource.setBizType(bizType.trim());
        resource.setOriginalName(originalName);
        resource.setFileUrl(fileUrl);
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        fileResourceService.save(resource);

        return Result.success(resource);
    }
}
