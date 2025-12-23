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

    public DemandanetRelayController(DemandanetSessionManager sessionManager, DemandanetParserService parserService) {
        this.sessionManager = sessionManager;
        this.parserService = parserService;
        this.httpClient = HttpClient.newBuilder().build();
    }

    /**
     * Endpoint NOVO: Retorna JSON com a lista de ordens (backend faz o parse).
     */
    @GetMapping("/listar-ordens")
    public ResponseEntity<List<ConsultedOrderDTO>> listarOrdens(
            @RequestParam(defaultValue = "5") int situacao,
            @AuthenticationPrincipal Tecnico solicitante) {

        String path = "/read.php?situacao=" + situacao + "&funcao=listar";
        String html = executeGetRequest(path, solicitante);

        return ResponseEntity.ok(parserService.parseOrderList(html));
    }

    /**
     * Endpoint NOVO: Retorna JSON com detalhes da ordem (backend faz o parse).
     */
    @GetMapping("/detalhes-ordem/{idOrdem}")
    public ResponseEntity<ConsultedOrderDetailsDTO> detalhesOrdem(
            @PathVariable String idOrdem,
            @AuthenticationPrincipal Tecnico solicitante) {

        String path = "/read.php?funcao=detalheOrdem&idOrdem=" + idOrdem;
        String html = executeGetRequest(path, solicitante);

        if (html.contains("Nenhuma ordem de serviço encontrada")) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(parserService.parseOrderDetails(html));
    }

    /**
     * Mantém o endpoint genérico para updates/posts ou recursos não mapeados ainda.
     */
    @RequestMapping("/proxy/**")
    public ResponseEntity<byte[]> relayRequest(
            @RequestBody(required = false) byte[] body,
            HttpServletRequest request,
            @AuthenticationPrincipal Tecnico solicitante) {

        // Remove o prefixo /api/demandanet/proxy para pegar o path real do legado
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
            // Cria um request dummy apenas para aproveitar a lógica do executeRawRequest
            // Em uma refatoração maior, extrairíamos a lógica de sessão para um método comum
            Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
            String cookie = sessionManager.getSessionForOwner(owner);

            URI targetUri = UriComponentsBuilder.fromUriString(demandanetBaseUrl)
                    .path(path)
                    .queryParam("idEscola", idEscolaConfig)
                    .build().toUri();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .header("Cookie", cookie)
                    .header("Referer", demandanetBaseUrl + "/")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1)); // Demandanet é legado, provável ISO-8859-1

            if (sessionManager.isSessionExpiredResponse(response.body())) {
                logger.warn("Sessão expirada. Renovando...");
                cookie = sessionManager.refreshSessionForOwner(owner);
                request = HttpRequest.newBuilder(targetUri).header("Cookie", cookie).GET().build();
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

            if (isTextResponse(response) && sessionManager.isSessionExpiredResponse(new String(response.body(), StandardCharsets.ISO_8859_1))) {
                logger.warn("Sessão Demandanet expirada (SQL Error). Renovando...");
                cookie = sessionManager.refreshSessionForOwner(owner);
                response = executeInternal(method, path, body, cookie, originalRequest);
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
        URI targetUri = UriComponentsBuilder.fromUriString(demandanetBaseUrl)
                .path(path)
                .queryParam("idEscola", idEscolaConfig)
                .build(true) // Encoding automático
                .toUri();

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(targetUri);

        // Copia headers importantes
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
