package com.xiuwen.framework.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.config.FileUploadProperties;
import com.xiuwen.framework.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务。
 */
@Slf4j
@Service
public class OssFileService {

    private final OssProperties ossProperties;
    private final FileUploadProperties fileUploadProperties;

    public OssFileService(OssProperties ossProperties, FileUploadProperties fileUploadProperties) {
        this.ossProperties = ossProperties;
        this.fileUploadProperties = fileUploadProperties;
    }

    /**
     * 上传文件到 OSS，返回完整的访问 URL。
     *
     * @param file   上传的文件
     * @param bizType 业务类型，用于组织目录结构
     * @return OSS 完整访问 URL
     */
    public String upload(MultipartFile file, String bizType) {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String safeBizType = bizType == null ? "COMMON" : bizType.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "_");
        if (safeBizType.isEmpty()) {
            safeBizType = "COMMON";
        }
        String objectName = safeBizType + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        if (!isOssConfigured()) {
            return uploadLocally(file, safeBizType, objectName.substring(objectName.indexOf('/') + 1));
        }

        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream);
        } catch (IOException e) {
            log.error("OSS 文件上传失败: {}", objectName, e);
            throw new BusinessException("文件上传失败，请重试");
        } finally {
            ossClient.shutdown();
        }

        String domain = ossProperties.getDomain();
        if (domain == null || domain.trim().isEmpty()) {
            domain = "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint();
        }
        if (!domain.endsWith("/")) {
            domain = domain + "/";
        }
        return domain + objectName;
    }

    private boolean isOssConfigured() {
        return hasText(ossProperties.getEndpoint())
                && hasText(ossProperties.getAccessKeyId())
                && hasText(ossProperties.getAccessKeySecret())
                && hasText(ossProperties.getBucketName());
    }

    private String uploadLocally(MultipartFile file, String bizType, String fileName) {
        Path uploadRoot = Paths.get(fileUploadProperties.getUploadPath()).toAbsolutePath().normalize();
        Path targetDirectory = uploadRoot.resolve(bizType).normalize();
        Path targetFile = targetDirectory.resolve(fileName).normalize();
        if (!targetFile.startsWith(uploadRoot)) {
            throw new BusinessException("无效的文件保存路径");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(targetDirectory);
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("本地文件上传失败: {}", targetFile, e);
            throw new BusinessException("文件上传失败，请重试");
        }
        String prefix = fileUploadProperties.getAccessPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "/uploads";
        }
        prefix = prefix.trim();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        log.info("OSS 未配置，文件已保存到本地: {}", targetFile);
        return prefix + "/" + bizType + "/" + fileName;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 获取 OSS 对外访问域名（带末尾斜杠）。
     */
    public String getOssDomain() {
        String domain = ossProperties.getDomain();
        if (domain == null || domain.trim().isEmpty()) {
            domain = "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint();
        }
        if (!domain.endsWith("/")) {
            domain = domain + "/";
        }
        return domain;
    }
}
