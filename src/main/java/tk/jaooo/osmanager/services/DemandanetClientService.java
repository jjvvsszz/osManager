package tk.jaooo.osmanager.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDTO;
import tk.jaooo.osmanager.model.dto.DemandanetEmployeeDTO;
import tk.jaooo.osmanager.model.dto.DemandanetStatusCountDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DemandanetClientService {

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    @Value("${demandanet.idescola}")
    private String idEscolaConfig;

    private WebClient webClient;
    private final DemandanetParserService parserService;
    private final ObjectMapper objectMapper;

    public DemandanetClientService(DemandanetParserService parserService, ObjectMapper objectMapper) {
        this.parserService = parserService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void initialize() {
        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(this.demandanetBaseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/146.0")
                .exchangeStrategies(strategies)
                .build();
    }

    public Mono<List<ConsultedOrderDTO>> searchOrdersByPatrimony(String sessionCookie, String termoBusca) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/read.php")
                        .queryParam("funcao", "listar")
                        .queryParam("situacao", "5")
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .retrieve()
                .bodyToMono(String.class)
                .map(html -> {
                    List<ConsultedOrderDTO> todas = parserService.parseOrderList(html);
                    String termo = termoBusca.toLowerCase();

                    return todas.stream()
                            .filter(os -> (os.patrimonio() != null && os.patrimonio().toLowerCase().contains(termo)) ||
                                    (os.defeito() != null && os.defeito().toLowerCase().contains(termo)))
                            .collect(Collectors.toList());
                });
    }

    public Mono<Map<String, Integer>> getOrderStatusCounts(String sessionCookie) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/read.php")
                        .queryParam("funcao", "recontarSituacao")
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(bodyAsString -> {
                    if (isSessionError(bodyAsString)) {
                        return Mono.error(new IllegalStateException("Sessão expirada detectada na resposta de contagem."));
                    }
                    try {
                        DemandanetStatusCountDTO response = objectMapper.readValue(bodyAsString, DemandanetStatusCountDTO.class);
                        Map<String, Integer> counts = response.data().listaSituacao().stream()
                                .collect(Collectors.toMap(
                                        DemandanetStatusCountDTO.StatusItem::numero,
                                        item -> Integer.parseInt(item.qtd())
                                ));
                        return Mono.just(counts);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Falha ao processar a resposta JSON do legado.", e));
                    }
                });
    }

    public Mono<List<DemandanetEmployeeDTO>> getEmployeesForScheduling(String sessionCookie, String osId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/read.php")
                        .queryParam("funcao", "alteraEstadoOrdem")
                        .queryParam("idOrdem", osId)
                        .queryParam("tipoEstado", "0")
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .retrieve()
                .bodyToMono(String.class)
                .map(parserService::parseEmployeesFromScheduleForm);
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

    public Mono<String> updateOrderStatus(String sessionCookie, String osId, int targetStatus, String funcionarioId, String dataPrevisao, String observacao) {
        if (targetStatus == 3) {
            return executeStateChange(sessionCookie, osId, targetStatus, null, null, null);
        }

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/read.php")
                        .queryParam("funcao", "detalheOrdem")
                        .queryParam("idOrdem", osId)
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(html -> {
                    if (isSessionError(html)) return Mono.just(html);

                    var details = parserService.parseOrderDetails(html);
                    boolean isDeleted = "Apagada".equalsIgnoreCase(details.situacao()) || "Excluída".equalsIgnoreCase(details.situacao());

                    boolean needsPreliminaryRestore = isDeleted && targetStatus != 4;
                    Mono<String> preliminaryStep = needsPreliminaryRestore ? executeUpdate(sessionCookie, osId, "restaurarOrdem", null) : Mono.just("OK");

                    return preliminaryStep.flatMap(prelimResponse -> {
                        if (isSessionError(prelimResponse)) return Mono.just(prelimResponse);
                        return executeStateChange(sessionCookie, osId, targetStatus, funcionarioId, dataPrevisao, observacao);
                    });
                });
    }

    public Mono<String> concludeOrder(String sessionCookie, String osId, String observacao) {
        MultiValueMap<String, String> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("idOrdem", osId);
        multipartData.add("observacao", observacao);
        multipartData.add("imagens[]", "");
        multipartData.add("funcao", "concluirOrdem");
        return executeMultipartUpdate(sessionCookie, multipartData);
    }

    private Mono<String> executeStateChange(String sessionCookie, String osId, int targetStatus, String funcionarioId, String dataPrevisao, String observacao) {
        return switch (targetStatus) {
            case 0 -> {
                if (funcionarioId == null || funcionarioId.isBlank()) {
                    yield Mono.error(new IllegalArgumentException("ID do funcionário é obrigatório para agendamento."));
                }
                yield scheduleOrder(sessionCookie, osId, funcionarioId, dataPrevisao);
            }
            case 1 -> // Em Andamento
                    startOrder(sessionCookie, osId);
            case 2 -> // Concluída
                    concludeOrder(sessionCookie, osId, observacao != null ? observacao : "");
            case 3 -> // Apagada
                    executeUpdate(sessionCookie, osId, "excluirOrdem", null);
            case 4 -> // Solicitada (Restaurar)
                    executeUpdate(sessionCookie, osId, "restaurarOrdem", null);
            default ->
                    Mono.error(new UnsupportedOperationException("A situação de destino '" + targetStatus + "' não é uma ação válida."));
        };
    }

    private Mono<String> scheduleOrder(String sessionCookie, String osId, String funcionarioId, String dataPrevisao) {
        MultiValueMap<String, String> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("funcao", "agendarOrdem");
        multipartData.add("idOrdem", osId);
        multipartData.add("funcionario", funcionarioId);
        multipartData.add("dataPrevisao", dataPrevisao == null ? "" : dataPrevisao);
        return executeMultipartUpdate(sessionCookie, multipartData);
    }

    private Mono<String> startOrder(String sessionCookie, String osId) {
        MultiValueMap<String, String> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("funcao", "iniciarOrdem");
        multipartData.add("idOrdem", osId);
        return executeMultipartUpdate(sessionCookie, multipartData);
    }

    private Mono<String> executeUpdate(String sessionCookie, String osId, String funcao, String situacao) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("idOrdem", osId);
        formData.add("funcao", funcao);
        if (situacao != null) {
            formData.add("situacao", situacao);
        }

        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/update.php")
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("Referer", demandanetBaseUrl + "/ordem_servico_gerencia/?idescola=" + idEscolaConfig)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", demandanetBaseUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<String> executeMultipartUpdate(String sessionCookie, MultiValueMap<String, String> multipartData) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/update.php")
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("Referer", demandanetBaseUrl + "/ordem_servico_gerencia/?idescola=" + idEscolaConfig)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", demandanetBaseUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .retrieve()
                .bodyToMono(String.class);
    }

    private boolean isSessionError(String html) {
        return html != null && (html.contains("telaAcesso.php") || html.contains("Erro no comando sql") || html.contains("You have an error in your SQL syntax"));
    }

    public Mono<byte[]> downloadLegacyPdf(String sessionCookie, String osId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/documentos_escolares/lista.php")
                        .queryParam("tipo", "202108120811") // Tipo fixo observado no cURL
                        .queryParam("idOrdem", osId)
                        .queryParam("idescola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("Referer", demandanetBaseUrl + "/ordem_servico_gerencia/?idescola=" + idEscolaConfig)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/146.0")
                .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class);
    }

    public Mono<String> getOrderDetailsHtml(String sessionCookie, String osId) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ordem_servico_gerencia/src/php/read.php")
                        .queryParam("funcao", "detalheOrdem")
                        .queryParam("idOrdem", osId)
                        .queryParam("idEscola", idEscolaConfig)
                        .build())
                .header("Cookie", sessionCookie)
                .header("X-Requested-With", "XMLHttpRequest")
                .retrieve()
                .bodyToMono(String.class);
    }
}
