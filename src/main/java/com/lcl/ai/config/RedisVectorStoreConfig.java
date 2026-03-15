package com.lcl.ai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.CreateOption;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.knuddels.jtokkit.api.EncodingType;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

import javax.sql.DataSource;

@Configuration
public class RedisVectorStoreConfig {
    @Value("${spring.ai.openai.api-key}")
    private String dashscopeApiKey;

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;
    @Bean
    public MysqlSaver mysqlSaver(DataSource dataSource) {

        // 使用公开的 Builder 模式构建 MysqlSaver
        return MysqlSaver.builder().dataSource(dataSource)
                .createOption(CreateOption.CREATE_IF_NOT_EXISTS)
                // 如果需要自定义序列化器，可以在这里配置，例如 JacksonStateSerializer
                // .stateSerializer(new JacksonStateSerializer())
                .build();
    }
    @Bean
    public EmbeddingModel embeddingModel() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashscopeApiKey)
                .build();
        return new DashScopeEmbeddingModel(dashScopeApi);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单机模式配置（根据你的 Redis 实际地址修改）
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379") // Redis 地址
                .setDatabase(0) // 数据库编号
                .setPassword(null); // 如果有密码，填写你的密码，无则设为 null

        // 集群模式（可选，按需替换）
        // config.useClusterServers()
        //       .addNodeAddress("redis://127.0.0.1:6379", "redis://127.0.0.1:6380");

        return Redisson.create(config);
    }

    @Bean
    public RedisSaver redisSaver(RedissonClient redissonClient) {
        // 使用公开的 Builder 模式，而不是直接 new
        return RedisSaver.builder()
                .redisson(redissonClient)
                // 如果需要自定义序列化器，可以在这里配置，否则使用默认的
                // .stateSerializer(new YourCustomStateSerializer())
                .build();
    }
    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }
    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, DashScopeEmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled,embeddingModel)
                .indexName("custom-index")                // Optional: defaults to "spring-ai-index"
                .prefix("custom-prefix")                  // Optional: defaults to "embedding:"
                .metadataFields(                         // Optional: define metadata fields for filtering
                        RedisVectorStore.MetadataField.tag("country"),
                        RedisVectorStore.MetadataField.numeric("year"))
                .initializeSchema(true)                   // Optional: defaults to false
                .batchingStrategy(new TokenCountBatchingStrategy(
                        EncodingType.CL100K_BASE,
                        8192, // 单篇文档最大Token数（需匹配模型上限）
                        0.1
                ))
                .build();
    }
}
