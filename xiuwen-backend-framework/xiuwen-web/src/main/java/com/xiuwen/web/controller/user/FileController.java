package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/admin/files")
public class FileController {


    @PostMapping("/upload")
    public Result<Void> upload() { return Result.todo("文件上传"); }

}
