package cn.edu.shmtu.eduvoyage.shared.config;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void boxedLongsAreSerializedAsStringsWithoutChangingPrimitiveLongs() throws Exception {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfig().longIdStringJsonCustomizer().customize(builder);

        String json = builder.build().writeValueAsString(new Sample(
                327169258939334657L,
                1710000000000L));

        assertThat(json).contains("\"id\":\"327169258939334657\"");
        assertThat(json).contains("\"timestamp\":1710000000000");
    }

    private record Sample(Long id, long timestamp) {
    }
}
