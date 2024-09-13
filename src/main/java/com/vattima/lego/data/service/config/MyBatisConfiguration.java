package com.vattima.lego.data.service.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackages = "net.lego.data.v2")
@MapperScan(basePackages = {"net.lego.data.v2.mybatis.mapper"})
@EnableTransactionManagement
public class MyBatisConfiguration {
}
