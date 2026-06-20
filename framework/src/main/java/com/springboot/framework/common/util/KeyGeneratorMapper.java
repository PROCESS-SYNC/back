package com.springboot.framework.common.util;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KeyGeneratorMapper {

  @Select("SELECT FN_GENERATE_KEY(#{jobType})")
  String generateKey(@Param("jobType") String jobType);
  
}
