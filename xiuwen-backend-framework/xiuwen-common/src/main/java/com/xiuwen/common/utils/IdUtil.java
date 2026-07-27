package com.xiuwen.common.utils;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClassName: IdUtil
 * Package: com.xiuwen.common.utils
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/24 16:16
 * @Version 1.0
 */
@Data
public class IdUtil {
    //定义时间格式
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
   //原子计数器,保证线程安全
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    public static String generateOrderNo(){
        //获取当前字符串
        String timeStr = LocalDateTime.now().format(FORMATTER);
       //获取并重置计数器(0-9999)
        int count = COUNTER.incrementAndGet();
        if(count > 9999){
            COUNTER.set(0);
            count = 0;
        }
        //拼接
        return "ORD" + timeStr + String.format("%04d",count);
    }
}
