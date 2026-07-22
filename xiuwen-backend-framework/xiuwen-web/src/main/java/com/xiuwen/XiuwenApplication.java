package com.xiuwen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 绣纹智创后端启动类
 */
@SpringBootApplication(scanBasePackages = "com.xiuwen")
@MapperScan(basePackages = {
        "com.xiuwen.system.mapper",
        "com.xiuwen.pattern.mapper",
        "com.xiuwen.product.mapper",
        "com.xiuwen.order.mapper",
        "com.xiuwen.course.mapper"
})
public class XiuwenApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiuwenApplication.class, args);
    }
}