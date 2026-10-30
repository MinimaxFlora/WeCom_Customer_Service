package xlike.top.kn_ai_chat.dto;

import org.junit.jupiter.api.Test;
import xlike.top.kn_ai_chat.domain.AiConfig;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigViewTest {

    @Test
    void masksApiKeys() {
        AiConfig config = new AiConfig();
        config.setExternalUserId("default");
        config.setAiApiKey("sk-1234567890abcdef");
        config.setRagApiKey("sk-abcdef1234567890");
        config.setTemperature(0.3);
        config.setMaxTokens(2000);
        config.setAnswerPolicy("STRICT");
        config.setHandoffPolicy("SUGGEST");

        AiConfigView view = AiConfigView.from(config);
        assertThat(view.getAiApiKey()).isEqualTo("sk-****cdef");
        assertThat(view.getRagApiKey()).isEqualTo("sk-****7890");
    }
}
