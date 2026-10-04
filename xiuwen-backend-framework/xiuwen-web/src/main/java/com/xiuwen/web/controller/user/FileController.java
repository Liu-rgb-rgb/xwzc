package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.framework.service.OssFileService;
import com.xiuwen.system.entity.FileResource;
import com.xiuwen.system.service.FileResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/files")
public class FileController {

    private final FileResourceService fileResourceService;
    private final OssFileService ossFileService;

    public FileController(FileResourceService fileResourceService,
                          OssFileService ossFileService) {
        this.fileResourceService = fileResourceService;
        this.ossFileService = ossFileService;
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

        String fileUrl = ossFileService.upload(file, bizType.trim());

        FileResource resource = new FileResource();
        resource.setUserId(userId);
        resource.setBizType(bizType.trim());
        resource.setOriginalName(file.getOriginalFilename());
        resource.setFileUrl(fileUrl);
        resource.setFileSize(file.getSize());
        resource.setMimeType(file.getContentType());
        fileResourceService.save(resource);

        return Result.success(resource);
    }
}
