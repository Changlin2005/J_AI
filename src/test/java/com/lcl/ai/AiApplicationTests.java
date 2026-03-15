package com.lcl.ai;

import com.lcl.ai.service.IGraphCheckpointService;
import com.lcl.ai.service.impl.GraphCheckpointServiceImpl;
import com.lcl.ai.utils.TTS;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiApplicationTests {


    @Test
    void contextLoads() {
        GraphCheckpointServiceImpl iGraphCheckpointService=new GraphCheckpointServiceImpl();
        System.out.println(iGraphCheckpointService.queryUserHistory("1"));
    }

}
