package tk.jaooo.osmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tk.jaooo.osmanager.services.GeminiService;
import tk.jaooo.osmanager.services.OciSecretsService;

@SpringBootTest
@ActiveProfiles("test")
class OsManagerApplicationTests {

    @MockitoBean
    private OciSecretsService ociSecretsService;

    @Test
    void contextLoads() {
    }

}
