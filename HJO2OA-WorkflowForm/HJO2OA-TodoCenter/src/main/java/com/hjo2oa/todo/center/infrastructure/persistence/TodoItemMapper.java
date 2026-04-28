package com.hjo2oa.todo.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoItemMapper extends BaseMapper<TodoItemEntity> {
}
