package com.lcl.ai.controller;


import com.lcl.ai.service.IGraphCheckpointService;
import com.lcl.ai.service.SpringAiChatMemoryService;
import com.lcl.ai.service.impl.GraphCheckpointServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lcl
 * @since 2026-02-23
 */
@RestController
@RequestMapping("/graph-checkpoint")
public class GraphCheckpointController {
    @Autowired
    private IGraphCheckpointService iGraphCheckpointService;
    @GetMapping
    public void test(){
        System.out.println("-----------------------------------");
        System.out.println(iGraphCheckpointService.queryUserHistory("1"));
    }
}
