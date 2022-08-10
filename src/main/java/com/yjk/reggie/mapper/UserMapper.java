package com.yjk.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yjk.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
