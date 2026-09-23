package com.xiuwen.framework.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务。
 */
@Slf4j
@Service
public class OssFileService {

    private final OssProperties ossProperties;

    public OssFileService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
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
        try (InputStream inputStream = file.getInputStream()) {
            return doUpload(bizType, ext, inputStream);
        } catch (IOException e) {
            log.error("OSS 文件上传失败: 读取文件流异常", e);
            throw new BusinessException("文件上传失败，请重试");
        }
    }

    /**
     * 上传字节数组到 OSS，返回完整的访问 URL。
     *
     * @param data      文件字节内容
     * @param extension 文件扩展名（含点，如 ".png"），为空则不附加
     * @param bizType   业务类型，用于组织目录结构
     * @return OSS 完整访问 URL
     */
    public String uploadBytes(byte[] data, String extension, String bizType) {
        String ext = (extension == null || extension.trim().isEmpty()) ? "" : extension;
        return doUpload(bizType, ext, new ByteArrayInputStream(data));
    }

    /**
     * OSS 上传核心逻辑：写入对象并返回访问 URL。
     */
    private String doUpload(String bizType, String ext, InputStream inputStream) {
        String objectName = bizType + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );

        try {
            ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream);
        } finally {
            ossClient.shutdown();
        }

        return getOssDomain() + objectName;
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
