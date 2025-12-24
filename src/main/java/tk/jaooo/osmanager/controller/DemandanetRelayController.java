package tk.jaooo.osmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.*;
import tk.jaooo.osmanager.services.DemandanetClientService;
import tk.jaooo.osmanager.services.DemandanetParserService;
import tk.jaooo.osmanager.services.DemandanetSessionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandanet")
public class DemandanetRelayController {

    private static final Logger logger = LoggerFactory.getLogger(DemandanetRelayController.class);

    private final HttpClient httpClient;
    private final DemandanetSessionManager sessionManager;
    private final DemandanetParserService parserService;
    private final DemandanetClientService clientService; // Adicionado

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    @Value("${demandanet.idescola}")
    private String idEscolaConfig;

    private static final String LEGACY_PHP_PATH = "/ordem_servico_gerencia/src/php/read.php";

    public DemandanetRelayController(DemandanetSessionManager sessionManager,
                                     DemandanetParserService parserService,
                                     DemandanetClientService clientService) {
        this.sessionManager = sessionManager;
        this.parserService = parserService;
        this.clientService = clientService;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @GetMapping("/listar-ordens")
    public ResponseEntity<List<ConsultedOrderDTO>> listarOrdens(
            @RequestParam(defaultValue = "5") int situacao,
            @AuthenticationPrincipal Tecnico solicitante) {

        String path = LEGACY_PHP_PATH + "?situacao=" + situacao + "&funcao=listar";
        String html = executeGetRequest(path, solicitante);

        List<ConsultedOrderDTO> ordens = parserService.parseOrderList(html);

        if (ordens.isEmpty()) {
            logger.debug("Lista vazia ou HTML inesperado. Tamanho: {}", html.length());
            if (html.contains("Not Found")) {
                logger.error("Erro 404 no legado. URL gerada provavelmente incorreta.");
            }
        }

        return ResponseEntity.ok(ordens);
    }

    @GetMapping("/detalhes-ordem/{idOrdem}")
    public ResponseEntity<ConsultedOrderDetailsDTO> detalhesOrdem(
            @PathVariable String idOrdem,
            @AuthenticationPrincipal Tecnico solicitante) {

        String path = LEGACY_PHP_PATH + "?funcao=detalheOrdem&idOrdem=" + idOrdem;
        String html = executeGetRequest(path, solicitante);

        if (html.contains("Nenhuma ordem de serviço encontrada") || html.contains("Not Found")) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(parserService.parseOrderDetails(html));
    }

    @PostMapping("/concluir")
    public ResponseEntity<?> concluirOrdem(
            @RequestBody @Valid ConcludeOrderRequestDTO dto,
            @AuthenticationPrincipal Tecnico solicitante) {

        try {
            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            // 1. Tenta executar a ação
            String responseBody = clientService.concludeOrder(cookie, dto.osNumber(), dto.observacao()).block();

            // 2. Verifica se a sessão expirou
            if (sessionManager.isSessionExpiredResponse(responseBody)) {
                logger.info("Sessão expirada ao concluir OS {}. Renovando e tentando novamente...", dto.osNumber());

                // 3. Renova e Tenta novamente
                cookie = sessionManager.refreshSessionForOwner(owner);
                responseBody = clientService.concludeOrder(cookie, dto.osNumber(), dto.observacao()).block();

                // Se falhar novamente, retorna erro
                if (sessionManager.isSessionExpiredResponse(responseBody)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Falha ao renovar sessão com o sistema legado.");
                }
            }

            return ResponseEntity.ok("Ordem concluída com sucesso.");

        } catch (Exception e) {
            logger.error("Erro ao concluir OS {}", dto.osNumber(), e);
            return ResponseEntity.internalServerError().body("Erro ao processar solicitação: " + e.getMessage());
        }
    }

    @GetMapping("/funcionarios-agendamento/{idOrdem}")
    public ResponseEntity<List<DemandanetEmployeeDTO>> listarFuncionariosParaAgendamento(
            @PathVariable String idOrdem,
            @AuthenticationPrincipal Tecnico solicitante) {

        try {
            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            List<DemandanetEmployeeDTO> funcionarios = clientService.getEmployeesForScheduling(cookie, idOrdem).block();
            return ResponseEntity.ok(funcionarios);

        } catch (Exception e) {
            logger.error("Erro ao buscar funcionários", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/atualizar-situacao")
    public ResponseEntity<?> atualizarSituacaoEmMassa(
            @RequestBody @Valid BatchStatusUpdateRequestDTO dto,
            @AuthenticationPrincipal Tecnico solicitante) {

        List<String> erros = new ArrayList<>();
        List<String> sucessos = new ArrayList<>();

        try {
            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            for (String osId : dto.ids()) {
                try {
                    String responseBody = clientService.updateOrderStatus(
                            cookie,
                            osId,
                            dto.situacao(),
                            dto.funcionarioId(),
                            dto.dataPrevisao(),
                            dto.observacao()
                    ).block();

                    if (sessionManager.isSessionExpiredResponse(responseBody)) {
                        cookie = sessionManager.refreshSessionForOwner(owner);

                        responseBody = clientService.updateOrderStatus(
                                cookie,
                                osId,
                                dto.situacao(),
                                dto.funcionarioId(),
                                dto.dataPrevisao(),
                                dto.observacao() // <- Passando o novo campo
                        ).block();

                        if (sessionManager.isSessionExpiredResponse(responseBody)) {
                            erros.add("Falha de sessão na OS " + osId);
                            continue;
                        }
                    }
                    sucessos.add(osId);

                } catch (IllegalArgumentException e) {
                    erros.add("Erro na OS " + osId + ": " + e.getMessage());
                    break;
                } catch (Exception e) {
                    logger.error("Erro ao atualizar OS {}", osId, e);
                    erros.add("Erro na OS " + osId + ": " + e.getMessage());
                }
                Thread.sleep(200);
            }

            if (sucessos.isEmpty() && !erros.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(erros);
            }

            return ResponseEntity.ok()
                    .body(java.util.Map.of(
                            "mensagem", "Processamento finalizado",
                            "sucessos", sucessos.size(),
                            "falhas", erros
                    ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro crítico: " + e.getMessage());
        }
    }

    @GetMapping("/contagem-status")
    public ResponseEntity<?> getStatusCounts(@AuthenticationPrincipal Tecnico solicitante) {
        try {
            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            Map<String, Integer> counts = clientService.getOrderStatusCounts(cookie).block();
            return ResponseEntity.ok(counts);

        } catch (Exception e) {
            logger.error("Erro ao buscar contagem de status", e);
            try {
                Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
                String cookie = sessionManager.refreshSessionForOwner(owner);
                Map<String, Integer> counts = clientService.getOrderStatusCounts(cookie).block();
                return ResponseEntity.ok(counts);
            } catch (Exception refreshException) {
                logger.error("Erro ao buscar contagem de status mesmo após renovar sessão", refreshException);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Falha ao comunicar com o sistema legado.");
            }
        }
    }

    @RequestMapping("/proxy/**")
    public ResponseEntity<byte[]> relayRequest(
            @RequestBody(required = false) byte[] body,
            HttpServletRequest request,
            @AuthenticationPrincipal Tecnico solicitante) {

        String prefix = "/api/demandanet/proxy";
        String uri = request.getRequestURI();
        String legacyPath = uri.substring(prefix.length());
        if (request.getQueryString() != null) {
            legacyPath += "?" + request.getQueryString();
        }

        return executeRawRequest(request.getMethod(), legacyPath, body, request, solicitante);
    }

    // --- Métodos Auxiliares ---

    private String executeGetRequest(String path, Tecnico solicitante) {
        try {
            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            URI targetUri = UriComponentsBuilder.fromUriString(demandanetBaseUrl + path)
                    .queryParam("idEscola", idEscolaConfig)
                    .build().toUri();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .header("Cookie", cookie)
                    .header("Referer", demandanetBaseUrl + "/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));

            if (sessionManager.isSessionExpiredResponse(response.body())) {
                logger.warn("Sessão expirada detectada no HTML. Renovando...");
                cookie = sessionManager.refreshSessionForOwner(owner);
                request = HttpRequest.newBuilder(targetUri)
                        .header("Cookie", cookie)
                        .header("Referer", demandanetBaseUrl + "/")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .GET()
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
            }

            return response.body();

        } catch (Exception e) {
            throw new RuntimeException("Falha ao buscar dados do Demandanet", e);
        }
    }

    private ResponseEntity<byte[]> executeRawRequest(String method, String path, byte[] body, HttpServletRequest originalRequest, Tecnico solicitante) {
        Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
        String cookie = sessionManager.getSessionForOwner(owner);

        try {
            HttpResponse<byte[]> response = executeInternal(method, path, body, cookie, originalRequest);

            if (isTextResponse(response)) {
                String responseBodyStr = new String(response.body(), StandardCharsets.ISO_8859_1);
                if (sessionManager.isSessionExpiredResponse(responseBodyStr)) {
                    logger.warn("Sessão Demandanet expirada (SQL Error). Renovando...");
                    cookie = sessionManager.refreshSessionForOwner(owner);
                    response = executeInternal(method, path, body, cookie, originalRequest);
                }
            }

            org.springframework.http.HttpHeaders responseHeaders = new org.springframework.http.HttpHeaders();
            response.headers().map().forEach(responseHeaders::addAll);

            return new ResponseEntity<>(response.body(), responseHeaders, response.statusCode());

        } catch (Exception e) {
            logger.error("Falha crítica no Relay Demandanet", e);
            return ResponseEntity.internalServerError().body(("Erro interno: " + e.getMessage()).getBytes());
        }
    }

    private HttpResponse<byte[]> executeInternal(String method, String path, byte[] body, String cookie, HttpServletRequest originalRequest) throws Exception {
        URI targetUri = UriComponentsBuilder.fromUriString(demandanetBaseUrl + path)
                .queryParam("idEscola", idEscolaConfig)
                .build()
                .toUri();

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(targetUri);

        if (originalRequest != null) {
            Collections.list(originalRequest.getHeaderNames()).forEach(headerName -> {
                String lower = headerName.toLowerCase();
                if (!lower.equals("cookie") && !lower.equals("host") && !lower.equals("content-length")) {
                    builder.header(headerName, originalRequest.getHeader(headerName));
                }
            });
        }

        builder.header("Cookie", cookie);
        builder.header("Referer", demandanetBaseUrl + "/ordem_servico_gerencia/?idescola=" + idEscolaConfig);

        HttpRequest.BodyPublisher bodyPublisher = (body != null && body.length > 0)
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        builder.method(method, bodyPublisher);

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private boolean isTextResponse(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type")
                .map(ct -> ct.contains("text") || ct.contains("json") || ct.contains("xml"))
                .orElse(false);
    }
}
