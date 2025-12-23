package tk.jaooo.osmanager.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DemandanetClientService {

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    @Value("${demandanet.idescola}")
    private String idEscolaConfig;

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

    public Mono<String> concludeOrder(String sessionCookie, String osId, String observacao) {
        MultiValueMap<String, String> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("idOrdem", osId);
        multipartData.add("observacao", observacao);
        multipartData.add("imagens[]", "");
        multipartData.add("funcao", "concluirOrdem");

        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/update.php")
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("Referer", demandanetBaseUrl + "/")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .retrieve()
                .bodyToMono(String.class);
    }
}
