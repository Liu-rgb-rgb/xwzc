package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.framework.service.OssFileService;
import com.xiuwen.system.entity.FileResource;
import com.xiuwen.system.service.FileResourceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 用户端定制预览图上传接口。 */
@RestController
@RequestMapping("/api/files")
public class UserFileUploadController {
    private static final String CUSTOM_BIZ_TYPE = "CUSTOM";

    private final FileResourceService fileResourceService;
    private final OssFileService ossFileService;

    public UserFileUploadController(FileResourceService fileResourceService,
                                    OssFileService ossFileService) {
        this.fileResourceService = fileResourceService;
        this.ossFileService = ossFileService;
    }

    @PostMapping("/upload")
    public Result<FileResource> upload(@RequestParam("file") MultipartFile file) {
        Long userId = LoginUserHolder.getRequiredUserId();
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String fileUrl = ossFileService.upload(file, CUSTOM_BIZ_TYPE);
        FileResource resource = new FileResource();
        resource.setUserId(userId);
        resource.setBizType(CUSTOM_BIZ_TYPE);
        resource.setOriginalName(file.getOriginalFilename());
        resource.setFileUrl(fileUrl);
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        fileResourceService.save(resource);
        return Result.success(resource);
    }
}
