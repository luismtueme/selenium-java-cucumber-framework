package io.github.luismtueme.framework.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MaskingTest {

    @Test
    void recognizesSecretNamesInAnySpelling() {
        assertThat(Masking.isSecret("password")).isTrue();
        assertThat(Masking.isSecret("Authorization")).isTrue();
        assertThat(Masking.isSecret("X-Api-Key")).isTrue();
        assertThat(Masking.isSecret("api_key")).isTrue();
        assertThat(Masking.isSecret("accessToken")).isTrue();
        assertThat(Masking.isSecret("Set-Cookie")).isTrue();
        assertThat(Masking.isSecret("username")).isFalse();
        assertThat(Masking.isSecret("Content-Type")).isFalse();
    }

    @Test
    void masksSecretHeaders() {
        assertThat(Masking.headers(Map.of("Authorization", "Bearer abc", "Accept", "application/json")))
                .containsEntry("Authorization", Masking.MASK)
                .containsEntry("Accept", "application/json");
    }

    @Test
    void masksSecretFieldsAtAnyDepth() {
        String masked = Masking.json("""
                {"username":"demo","password":"hunter2","session":{"token":"abc","items":[{"apiKey":"k","name":"n"}]}}""");
        assertThat(masked)
                .doesNotContain("hunter2", "\"abc\"", "\"k\"")
                .contains("\"username\" : \"demo\"", "\"name\" : \"n\"", "\"password\" : \"***\"");
    }

    @Test
    void keepsObjectsUnderSecretNamesButMasksTheirSecrets() {
        assertThat(Masking.json("{\"token\":{\"type\":\"bearer\",\"secret\":\"s\"}}"))
                .contains("\"type\" : \"bearer\"")
                .doesNotContain("\"s\"");
    }

    @Test
    void leavesNonJsonBodiesUnchanged() {
        assertThat(Masking.json("<html>password</html>")).isEqualTo("<html>password</html>");
        assertThat(Masking.json("")).isEmpty();
        assertThat(Masking.json(null)).isNull();
    }
}
