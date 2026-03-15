package com.lcl.ai.agent.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class MemoryHook extends ModelHook {
    private final Map<String, Map<String, Object>> profileCache = new ConcurrentHashMap<>();
    @Override
    public String getName() {
        return "memory_hook";
    }


    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 从配置中获取用户ID
        String userId = "user_001";
        if (userId == null) {
            return CompletableFuture.completedFuture(Map.of());
        }

        Map<String, Object> profile = loadUserProfileFromMd(userId);
        if (profile != null && !profile.isEmpty()) { // 读取到用户画像时执行注入逻辑
            // 步骤3：构造用户上下文文本（格式化用户信息，增加空值处理）
            String userContext = String.format(
                    "【核心指令】你是用户的专属助手，以下是你已掌握的该用户的核心信息，不是对话中提到的内容：\n" +
                            "        - 姓名：%s\n" +
                            "        - 年龄：%s\n" +
                            "        - 邮箱：%s\n" +
                            "        - 偏好：%s\n" +
                            "        \n" +
                            "        【回答规则】\n" +
                            "        1. 当用户问“我的信息”“我是谁”“你知道我的什么信息”等问题时，必须直接使用上述信息回答，禁止说“不知道”“之前提到过”等表述；\n" +
                            "        2. 回答时保持你的人设风格（甜美元气），但必须准确引用用户信息；\n" +
                            "        3. 禁止提及“系统提示”“用户信息注入”等技术相关词汇。",
                    profile.getOrDefault("name", "未知"),
                    profile.getOrDefault("age", "未知"),
                    profile.getOrDefault("email", "未知"),
                    profile.getOrDefault("preferences", "无")
            );
            // 获取消息列表
            List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
            List<Message> newMessages = new ArrayList<>();

            // 查找是否已存在 SystemMessage
            SystemMessage existingSystemMessage = null;
            int systemMessageIndex = -1;
            for (int i = 0; i < messages.size(); i++) {
                Message msg = messages.get(i);
                if (msg instanceof SystemMessage) {
                    existingSystemMessage = (SystemMessage) msg;
                    systemMessageIndex = i;
                    break;
                }
            }

            // 如果找到 SystemMessage，更新它；否则创建新的
            SystemMessage enhancedSystemMessage;
            if (existingSystemMessage != null) {
                // 更新现有的 SystemMessage
                enhancedSystemMessage = new SystemMessage(
                        existingSystemMessage.getText() + " " + userContext
                );
            } else {
                // 创建新的 SystemMessage
                enhancedSystemMessage = new SystemMessage(userContext);
            }

            // 构建新的消息列表
            if (systemMessageIndex >= 0) {
                // 如果找到了 SystemMessage，替换它
                for (int i = 0; i < messages.size(); i++) {
                    if (i == systemMessageIndex) {
                        newMessages.add(enhancedSystemMessage);
                    } else {
                        newMessages.add(messages.get(i));
                    }
                }
            } else {
                // 如果没有找到 SystemMessage，在开头添加新的
                newMessages.add(enhancedSystemMessage);
                newMessages.addAll(messages);
            }

            return CompletableFuture.completedFuture(Map.of("messages", newMessages));
        }

        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 可以在这里实现对话后的记忆保存逻辑
        return CompletableFuture.completedFuture(Map.of());
    }

    // ========== 新增MD文件读取工具方法（放在MemoryHook类中） ==========
    /**
     * 从MD文件读取指定用户的画像信息
     * @param userId 用户ID
     * @return 用户画像Map（null表示读取失败/无该用户）
     */
    private Map<String, Object> loadUserProfileFromMd(String userId) {


        // 在loadUserProfileFromMd方法中先查缓存
        if (profileCache.containsKey(userId)) {
            return profileCache.get(userId);
        }

        // 1. 定义MD文件路径（建议配置化，这里先写死示例，可改为从配置读取）
        String mdFilePath = Paths.get("/lcl-project/AI/src/main/resources/data/user.md").toAbsolutePath().toString();

        // 2. 初始化返回结果
        Map<String, Object> profile = new HashMap<>();
        boolean isTargetUser = false; // 标记是否读取到目标用户的区块

        // 3. 读取并解析MD文件
        try (BufferedReader br = new BufferedReader(new FileReader(mdFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim(); // 去除首尾空格

                // 匹配用户区块开头（格式：## user_1001）
                if (line.startsWith("## ") && line.endsWith(userId)) {
                    isTargetUser = true;
                    continue; // 跳过区块标题行，读取后续内容
                }

                // 匹配下一个用户区块开头，结束当前用户读取
                if (isTargetUser && line.startsWith("## ")) {
                    break;
                }

                // 读取目标用户的键值对（格式：姓名: 张三）
                if (isTargetUser && line.contains(":")) {
                    String[] parts = line.split(":", 2); // 按第一个冒号分割
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        profile.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            // 打印异常日志（建议接入日志框架，如logback/log4j）
            System.err.println("读取用户MD文件失败：" + e.getMessage());
            return null;
        }
        // 读取文件后放入缓存
        profileCache.put(userId, profile);
        // 4. 无数据时返回null，避免空Map进入后续逻辑
        return profile.isEmpty() ? null : profile;
    }
}
