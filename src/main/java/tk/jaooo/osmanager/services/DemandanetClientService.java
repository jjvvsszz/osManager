package tk.jaooo.osmanager.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
                                    // Valida sucesso real do login
                                    if (htmlBody != null && htmlBody.contains("id=iduser")) {
                                        var sessionCookie = loginResponse.cookies().getFirst("PHPSESSID");
                                        if (sessionCookie != null) {
                                            return Mono.just(sessionCookie.getName() + "=" + sessionCookie.getValue());
                                        }
                                    }
                                    return Mono.error(new RuntimeException("Credenciais rejeitadas ou cookie não gerado."));
                                });
                    }
                    return Mono.error(new RuntimeException("Erro HTTP ao contatar Demandanet: " + loginResponse.statusCode()));
                });
    }
}
