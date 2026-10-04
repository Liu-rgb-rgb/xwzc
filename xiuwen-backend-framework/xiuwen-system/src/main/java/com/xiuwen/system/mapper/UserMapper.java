package com.xiuwen.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.system.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * user 表 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
