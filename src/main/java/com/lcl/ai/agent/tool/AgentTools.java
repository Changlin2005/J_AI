package com.lcl.ai.agent.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.model.ToolContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;



@Component
@RequiredArgsConstructor
public class AgentTools {

    // 内存缓存：namespace + key → value
    private final Map<String, Map<String, Object>> memoryCache = new ConcurrentHashMap<>();
    // MD 文件路径（可配置化）
    private static final String MD_FILE_PATH = "/lcl-project/AI/src/main/resources/data/user_memory.md";
    // 记忆响应结构体
    public record MemoryResponse(String message, Map<String, Object> value) {}

    // ==================== 保存记忆 Tool ====================
    @Tool(description = "保存到长期记忆")
    public MemoryResponse saveMemory(
            List<String> namespace,
            String key,
            Map<String, Object> value,
            ToolContext context
    ) {
        try {
            // 1. 生成缓存键（namespace 拼接为唯一标识）
            String cacheKey = buildCacheKey(namespace, key);

            // 2. 写入内存缓存
            memoryCache.put(cacheKey, value);

            // 3. 写入 MD 文件（追加/更新模式）
            writeToMdFile(namespace, key, value);

            return new MemoryResponse("已保存到记忆库", value);
        } catch (Exception e) {
            return new MemoryResponse("保存失败：" + e.getMessage(), Map.of());
        }
    }

    // ==================== 获取记忆 Tool ====================
    @Tool(description = "从长期记忆获取")
    public MemoryResponse getMemory(
            List<String> namespace,
            String key,
            ToolContext context
    ) {
        String cacheKey = buildCacheKey(namespace, key);

        // 1. 优先从缓存读取
        if (memoryCache.containsKey(cacheKey)) {
            return new MemoryResponse("从缓存获取", memoryCache.get(cacheKey));
        }

        // 2. 缓存未命中，从 MD 文件读取
        Map<String, Object> value = readFromMdFile(namespace, key);
        if (!value.isEmpty()) {
            // 写入缓存，下次直接读取
            memoryCache.put(cacheKey, value);
            return new MemoryResponse("从MD文件获取", value);
        }

        return new MemoryResponse("未找到记忆", Map.of());
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建缓存唯一键：namespace 用 "/" 拼接 + key
     */
    private String buildCacheKey(List<String> namespace, String key) {
        String ns = String.join("/", namespace);
        return ns + "::" + key;
    }

    /**
     * 写入 MD 文件（格式：## [namespace/...] key\n 键: 值\n...）
     */
    private void writeToMdFile(List<String> namespace, String key, Map<String, Object> value) throws IOException {
        Path mdPath = Paths.get(MD_FILE_PATH);
        // 如果文件不存在，创建新文件
        if (!Files.exists(mdPath)) {
            Files.createFile(mdPath);
        }

        // 构建 MD 格式内容
        String nsStr = String.join("/", namespace);
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(nsStr).append(" ").append(key).append("\n");
        value.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        sb.append("\n");

        // 追加写入文件
        Files.writeString(mdPath, sb.toString(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    /**
     * 从 MD 文件读取指定记忆
     */
    private Map<String, Object> readFromMdFile(List<String> namespace, String key) {
        Path mdPath = Paths.get(MD_FILE_PATH);
        if (!Files.exists(mdPath)) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        String targetNs = String.join("/", namespace);
        String targetBlock = "## " + targetNs + " " + key;
        boolean isTargetBlock = false;

        try (BufferedReader br = Files.newBufferedReader(mdPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // 匹配目标记忆块开头
                if (line.equals(targetBlock)) {
                    isTargetBlock = true;
                    continue;
                }
                // 匹配下一个记忆块，结束读取
                if (isTargetBlock && line.startsWith("## ")) {
                    break;
                }
                // 解析键值对
                if (isTargetBlock && line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        result.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}