package ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AdminAiService {

    private final ChatClient chatClient;
    private final AdminAiTools adminAiTools;

    public AdminAiService(ChatClient.Builder builder, AdminAiTools adminAiTools) {
        this.chatClient = builder.build();
        this.adminAiTools = adminAiTools;
    }

    public String processAdminQuery(String userQuery) {
        return chatClient.prompt()
                .user(userQuery)
                .tools(adminAiTools) // Передаем экземпляр бина AdminAiTools
                .call()
                .content();
    }
}