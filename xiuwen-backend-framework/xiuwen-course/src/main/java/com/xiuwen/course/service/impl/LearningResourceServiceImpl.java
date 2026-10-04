package com.xiuwen.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.course.entity.LearningResource;
import com.xiuwen.course.mapper.LearningResourceMapper;
import com.xiuwen.course.service.LearningResourceService;
import org.springframework.stereotype.Service;

/**
 * learning_resource 表服务实现。
 */
@Service
public class LearningResourceServiceImpl extends ServiceImpl<LearningResourceMapper, LearningResource> implements LearningResourceService {
}
