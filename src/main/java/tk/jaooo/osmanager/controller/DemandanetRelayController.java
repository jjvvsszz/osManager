package tk.jaooo.osmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDTO;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDetailsDTO;
import tk.jaooo.osmanager.services.DemandanetParserService;
import tk.jaooo.osmanager.services.DemandanetSessionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/demandanet")
public class DemandanetRelayController {

    private static final Logger logger = LoggerFactory.getLogger(DemandanetRelayController.class);

    private final HttpClient httpClient;
    private final DemandanetSessionManager sessionManager;
    private final DemandanetParserService parserService;

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    @Value("${demandanet.idescola}")
    private String idEscolaConfig;

    private static final String LEGACY_PHP_PATH = "/ordem_servico_gerencia/src/php/read.php";

    public DemandanetRelayController(DemandanetSessionManager sessionManager, DemandanetParserService parserService) {
        this.sessionManager = sessionManager;
        this.parserService = parserService;
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
