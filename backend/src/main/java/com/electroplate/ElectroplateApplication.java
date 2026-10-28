package com.electroplate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 电镀配方库（本地资料库）后端服务启动类
 */
@SpringBootApplication
@MapperScan("com.electroplate.mapper")
public class ElectroplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElectroplateApplication.class, args);
    }
}
