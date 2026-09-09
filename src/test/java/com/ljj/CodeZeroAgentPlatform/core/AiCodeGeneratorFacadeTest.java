package com.ljj.CodeZeroAgentPlatform.core;

import com.ljj.CodeZeroAgentPlatform.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成一个登录页面，总共不超过 20 行代码", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }

   
}
