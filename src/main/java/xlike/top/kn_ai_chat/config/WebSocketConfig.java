package xlike.top.kn_ai_chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import xlike.top.kn_ai_chat.handler.websocket.AdminWebSocketHandler;

/**
 * @author xlike
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AdminWebSocketHandler adminWebSocketHandler;

    @Value("${app.websocket.allowed-origins:https://wechat.tatalvpai.vip,http://localhost:8081}")
    private String[] allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(adminWebSocketHandler, "/ws/admin")
                .addInterceptors(new AdminHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigins);
    }
}
