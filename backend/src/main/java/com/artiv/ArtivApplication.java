package com.artiv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.artiv.mapper")
public class ArtivApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtivApplication.class, args);
    }
}
