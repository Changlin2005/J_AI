package com.lcl.ai.utils;

import cn.hutool.crypto.SecureUtil;
import com.google.common.collect.Lists;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
@Component
public class RAG_Load {
    @Autowired
    public VectorStore vectorStore;
    @jakarta.annotation.Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("classpath:/data/Elysia_RAG.md")
    private Resource ElysiaFile;
    public void load_rag(){

        TextReader textReader =  new TextReader(ElysiaFile);
        textReader.setCharset(Charset.defaultCharset());

        // 1. 读取MD原始内容，并处理转义字符
        String Elysia_RAG = textReader.read().toString()
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("\\\\r", "\r");

// 2. 将字符串包装为Document对象
        Document document = new Document(Elysia_RAG);
        List<Document> docList = Collections.singletonList(document);

// 3. 拆分Document列表
        List<Document> list = new TokenTextSplitter().split(docList);

//        List<Document> list=new TokenTextSplitter().transform(textReader.read());
        //4 去重复版本
        String sourceMetadata = (String)textReader.getCustomMetadata().get("source");
        String textHash = SecureUtil.md5(sourceMetadata);
        String redisKey = "Elysia:" + textHash;
// 判断是否存入过,redisKey如果可以成功插入表示以前没有过，可以加入向量数据
        Boolean retFlag = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1");
        if(Boolean.TRUE.equals(retFlag))
        {
            List<List<Document>> batchedDocs = Lists.partition(list, 10);
            for (List<Document> batch : batchedDocs) {
                vectorStore.add(batch);
            }
        }else {
            //键已存在，跳过或者报错
            //throw new RuntimeException("---重复操作");
            System.out.println("------向量初始化数据已经加载过，请不要重复操作");
        }
    }
}
