package com.artiv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.artiv.mapper")
@EnableScheduling
public class ArtivApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtivApplication.class, args);
    }
}
