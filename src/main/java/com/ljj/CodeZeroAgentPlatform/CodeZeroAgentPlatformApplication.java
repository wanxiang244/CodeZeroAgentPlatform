package com.ljj.CodeZeroAgentPlatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ljj.CodeZeroAgentPlatform.mapper")
public class CodeZeroAgentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeZeroAgentPlatformApplication.class, args);
    }

}
