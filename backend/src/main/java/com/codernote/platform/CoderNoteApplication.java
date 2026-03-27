package com.codernote.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.codernote.platform.mapper")
@EnableScheduling
public class CoderNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoderNoteApplication.class, args);
    }
}
