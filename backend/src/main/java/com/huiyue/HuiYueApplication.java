package com.huiyue;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.huiyue.mapper")
public class HuiYueApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuiYueApplication.class, args);
    }
}
