package xlike.top.kn_ai_chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
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

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 人工客服支持发送微信客服允许的图片/文件，data URL 会比原文件更大
        container.setMaxTextMessageBufferSize(32 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(32 * 1024 * 1024);
        container.setMaxSessionIdleTimeout(30L * 60 * 1000);
        return container;
    }
}
