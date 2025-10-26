package tk.jaooo.osmanager.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DemandanetClientService {

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    private WebClient webClient;

    public DemandanetClientService() {
    }

    @PostConstruct
    private void initialize() {
        this.webClient = WebClient.builder()
                .baseUrl(this.demandanetBaseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/144.0")
                .build();
    }

    public Mono<String> loginAndGetSessionCookie(String username, String password) {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("loginType", "admin");
        formData.add("user", username);
        formData.add("password", password);

        return this.webClient.post()
                .uri("/telaAcesso.php")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchangeToMono(loginResponse -> {
                    if (loginResponse.statusCode().is2xxSuccessful()) {
                        return loginResponse.bodyToMono(String.class)
                                .flatMap(htmlBody -> {
                                    if (htmlBody != null && htmlBody.contains("id=iduser")) {
                                        ResponseCookie sessionCookie = loginResponse.cookies().getFirst("PHPSESSID");
                                        if (sessionCookie != null) {
                                            String cookieHeaderValue = sessionCookie.getName() + "=" + sessionCookie.getValue();
                                            return Mono.just(cookieHeaderValue);
                                        } else {
                                            return Mono.error(new RuntimeException("Login parece bem-sucedido, mas o cookie de sessão não foi retornado."));
                                        }
                                    } else {
                                        return Mono.error(new RuntimeException("Falha no login com o Demandanet: credenciais inválidas."));
                                    }
                                });
                    } else {
                        return Mono.error(new RuntimeException("O servidor do Demandanet respondeu com um erro inesperado: " + loginResponse.statusCode()));
                    }
                });
    }
}
