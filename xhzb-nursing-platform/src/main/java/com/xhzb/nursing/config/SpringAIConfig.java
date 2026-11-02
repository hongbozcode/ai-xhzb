package com.xhzb.nursing.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    /**
     * 创建并返回一个ChatClient的Spring Bean实例。
     * @param openAiChatModel
     * @return
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient
                .builder(openAiChatModel)
                .defaultSystem("您是一家名为“星海智伴”的职业养老机构的客户聊天助手，你的名字叫小智。请以友好、乐于助人和愉快的方式解答用户的各种问题。")
                .build();
    }

    /**
     * AI评估对话示例
     */
    @Bean
    public ChatClient chatClientHealth(OpenAiChatModel openAiChatModel){
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个健康评估专家，专门用来评估老人的健康情况")
                .build();
    }


}