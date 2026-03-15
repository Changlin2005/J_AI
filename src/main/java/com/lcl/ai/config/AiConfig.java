package com.lcl.ai.config;


import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.lcl.ai.agent.hook.MemoryHook;
import com.lcl.ai.agent.hook.RAGMessagesHook;
import com.lcl.ai.agent.prompt.PromptDefine;
import com.lcl.ai.agent.tool.AgentTools;
import com.lcl.ai.agent.tool.TTSTool;
import com.lcl.ai.utils.RAG_Load;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class AiConfig {
//    @Autowired
//    private JdbcChatMemoryRepository jdbcChatMemoryRepository;
//    private RedisChatMemoryRepository redisChatMemoryRepository= new RedisChatMemoryRepository.RedisBuilder().build();
//    @Autowired
//    private RedisSaver redisSaver;
    @Autowired
    private MysqlSaver mysqlSaver;
    @Autowired
    private RAG_Load rag_load;
    @Autowired
    private VectorStore vectorStore;
    private final TTSTool ttsTool;

    @PostConstruct
    public void init(){
        rag_load.load_rag();
    }
    SkillRegistry registry = ClasspathSkillRegistry.builder()
            .classpathPath("skills")
            .build();
    SkillsAgentHook hook = SkillsAgentHook.builder()
            .skillRegistry(registry)
            .build();
//    @Bean
//    public ChatMemory chatMemory() {
//        return MessageWindowChatMemory.builder()
////                .chatMemoryRepository(redisChatMemoryRepository)
//                .chatMemoryRepository(jdbcChatMemoryRepository)
//                .maxMessages(1024) // 最多保留20条对话（可按需调整）
//                .build();
//    }


    @Bean(name = "qwenAgent")
    public ReactAgent qwenAgent(@Qualifier("qwen") ChatModel qwen)
    {
        // 创建 Agent
        return  ReactAgent.builder()
                .name("my_agent")
                .model(qwen)
                .systemPrompt(PromptDefine.system_prompt)
                .saver(mysqlSaver)
                .methodTools(ttsTool)
                .hooks(List.of(new MemoryHook(),new RAGMessagesHook(vectorStore)))
                .build();
    }

    @Bean(name = "ElysiaAgent")
    public ReactAgent ElysiaAgent(@Qualifier("Elysia") ChatModel qwen)
    {
        // 创建 Agent
        return  ReactAgent.builder()
                .name("Elysia_agent")
                .model(qwen)
                .saver(mysqlSaver)
                .methodTools(new AgentTools())
                .hooks(List.of(new MemoryHook(),new RAGMessagesHook(vectorStore)))
                .build();
    }

    @Bean(name = "Elysia")
    public ChatModel Elysia()
    {
        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl("http://localhost:11434").build())
                .defaultOptions(OllamaChatOptions.builder().model("Elysia_Mei").build())
                .build();
    }
    @Bean(name = "qwen")
    public ChatModel qwen()
    {
        return DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder()
                        .apiKey(System.getenv("Qwen_Api_Key"))
                        .build())
                .defaultOptions(DashScopeChatOptions.builder().model("qwen3-max-2026-01-23").build())
                .build();
    }
//    @Bean(name = "qwenChatClient")
//    public ChatClient qwenChatClient(@Qualifier("qwen") ChatModel qwen)
//    {
//        return ChatClient.builder(qwen)
//                        .defaultOptions(ChatOptions.builder().model("qwen_plus").build())
//                        .build();
//    }

    @Bean
    public ChatClient chatClient(@Qualifier("Elysia")ChatModel model, ChatMemory chatMemory) {
        return ChatClient.builder(model)
//                .defaultSystem("你现在是自定义聊天机器人")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
    }
}
