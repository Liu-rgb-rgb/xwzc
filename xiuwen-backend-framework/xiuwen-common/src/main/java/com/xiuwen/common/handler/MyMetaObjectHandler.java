package com.xiuwen.common.handler;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * ClassName: MyMetaObjectHandler
 * Package: com.xiuwen.common.handler
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/22 10:32
 * @Version 1.0
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
@Override
    public void insertFill(MetaObject metaObject) {
    this.strictInsertFill(metaObject,"createdAt", LocalDateTime.class,LocalDateTime.now());
    this.strictUpdateFill(metaObject,"updatedAt", LocalDateTime.class,LocalDateTime.now());
}
@Override
    public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject,"updatedAt", LocalDateTime.class,LocalDateTime.now());

}
}
