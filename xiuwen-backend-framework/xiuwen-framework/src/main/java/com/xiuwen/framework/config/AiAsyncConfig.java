package com.xiuwen.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 生图异步线程池配置。
 *
 * 生图耗时长(单张数十秒, 多张可达数分钟), 不能占用 HTTP 请求线程,
 * 统一丢到该线程池后台执行, 前端通过轮询接口获取进度。
 */
@Configuration
public class AiAsyncConfig {

    @Bean("aiGenerationExecutor")
    public Executor aiGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ai-gen-");
        // 池满时由提交线程兜底执行, 宁可慢也不丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
