package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.FileResource;
import com.xiuwen.system.mapper.FileResourceMapper;
import com.xiuwen.system.service.FileResourceService;
import org.springframework.stereotype.Service;

/**
 * file_resource 表服务实现。
 */
@Service
public class FileResourceServiceImpl extends ServiceImpl<FileResourceMapper, FileResource> implements FileResourceService {
}
