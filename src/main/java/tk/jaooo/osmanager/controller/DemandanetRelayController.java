package tk.jaooo.osmanager.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.services.DemandanetSessionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RestController
@RequestMapping("/api/demandanet")
public class DemandanetRelayController {

    private static final Logger logger = LoggerFactory.getLogger(DemandanetRelayController.class);

    private final HttpClient httpClient;
    private final DemandanetSessionManager sessionManager;

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    @Value("${demandanet.idescola}")
    private String idEscolaConfig;

    public DemandanetRelayController(DemandanetSessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.httpClient = HttpClient.newBuilder().build();
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> relayRequest(
            @RequestBody(required = false) byte[] body,
            HttpServletRequest request,
            @AuthenticationPrincipal Tecnico solicitante) { // Injetado pelo Spring Security Local

        // 1. Resolve quem paga a conta (Credencial)
        Tecnico credentialOwner = sessionManager.resolveCredentialOwner(solicitante);

        logger.info("Relay iniciado. Solicitante: {} (ID {}). Credencial usada: {} (ID {})",
                solicitante.getUsername(), solicitante.getId(),
                credentialOwner.getUsername(), credentialOwner.getId());

        String cookie = sessionManager.getSessionForOwner(credentialOwner);

        try {
            // TENTATIVA 1
            HttpResponse<byte[]> response = executeInternal(request, body, cookie);

            // Verificação de Erro Silencioso (Fake 200)
            if (isTextResponse(response) && sessionManager.isSessionExpiredResponse(new String(response.body(), StandardCharsets.UTF_8))) {

                logger.warn("Sessão Demandanet expirada (SQL Error). Renovando credencial de {}", credentialOwner.getUsername());

                // RENOVAÇÃO
                cookie = sessionManager.refreshSessionForOwner(credentialOwner);

                // TENTATIVA 2
                response = executeInternal(request, body, cookie);
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            response.headers().map().forEach(responseHeaders::addAll);

            return new ResponseEntity<>(response.body(), responseHeaders, response.statusCode());

        } catch (Exception e) {
            logger.error("Falha crítica no Relay Demandanet", e);
            return ResponseEntity.internalServerError().body(("Erro interno: " + e.getMessage()).getBytes());
        }
    }

    private HttpResponse<byte[]> executeInternal(HttpServletRequest request, byte[] body, String cookie) throws Exception {
        URI targetUri = buildTargetUri(request);

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(targetUri);

        // Copia headers (exceto sensíveis)
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            String lower = headerName.toLowerCase();
            if (!lower.equals("cookie") && !lower.equals("host") && !lower.equals("content-length")) {
                builder.header(headerName, request.getHeader(headerName));
            }
        });

        // Injeta sessão gerenciada
        builder.header("Cookie", cookie);
        // O Referer é vital para o Demandanet aceitar requisições
        builder.header("Referer", demandanetBaseUrl + "/ordem_servico_gerencia/?idescola=" + idEscolaConfig);

        HttpRequest.BodyPublisher bodyPublisher = (body != null && body.length > 0)
                ? HttpRequest.BodyPublishers.ofByteArray(body)
                : HttpRequest.BodyPublishers.noBody();

        builder.method(request.getMethod(), bodyPublisher);

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private URI buildTargetUri(HttpServletRequest request) {
        String originalPath = request.getRequestURI().substring("/api/demandanet".length());
        return UriComponentsBuilder.fromUriString(demandanetBaseUrl)
                .path(originalPath)
                .query(request.getQueryString())
                .build(true)
                .toUri();
    }

    private boolean isTextResponse(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type")
                .map(ct -> ct.contains("text") || ct.contains("json") || ct.contains("xml"))
                .orElse(false);
    }
}
