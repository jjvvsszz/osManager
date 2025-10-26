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
import tk.jaooo.osmanager.model.DemandanetSessionDetails;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandanet")
public class DemandanetRelayController {

    private static final Logger logger = LoggerFactory.getLogger(DemandanetRelayController.class);

    private final HttpClient httpClient;

    private static final String API_PREFIX = "/api/demandanet";

    @Value("${demandanet.base-url}")
    private String demandanetBaseUrl;

    public DemandanetRelayController() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> relayRequestToDemandanet(
            @RequestBody(required = false) byte[] body,
            HttpServletRequest request,
            @AuthenticationPrincipal DemandanetSessionDetails sessionDetails) {

        final URI targetUri = buildTargetUri(request, sessionDetails.getIdEscola());
        logger.info("Relay {} request to: {}", request.getMethod(), targetUri);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(targetUri);

            copyHeadersFromRequest(request, requestBuilder, sessionDetails.getSessionCookie(), sessionDetails.getIdEscola());

            HttpRequest.BodyPublisher bodyPublisher;

            if (request.getContentType() != null && request.getContentType().contains("multipart/form-data")) {
                String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
                requestBuilder.header("Content-Type", "multipart/form-data; boundary=" + boundary);

                List<byte[]> byteArrays = new ArrayList<>();
                request.getParameterMap().forEach((key, values) -> {
                    for (String value : values) {
                        byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                        byteArrays.add(("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        byteArrays.add((value + "\r\n").getBytes(StandardCharsets.UTF_8));
                    }
                });
                if (!request.getParameterMap().containsKey("imagens[]")) {
                    byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                    byteArrays.add(("Content-Disposition: form-data; name=\"imagens[]\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    byteArrays.add(("\r\n").getBytes(StandardCharsets.UTF_8));
                }

                byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

                bodyPublisher = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
            } else {
                bodyPublisher = (body != null && body.length > 0) ? HttpRequest.BodyPublishers.ofByteArray(body) : HttpRequest.BodyPublishers.noBody();
            }

            requestBuilder.method(request.getMethod(), bodyPublisher);

            HttpResponse<byte[]> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

            HttpHeaders responseHeaders = new HttpHeaders();
            response.headers().map().forEach(responseHeaders::addAll);

            return new ResponseEntity<>(response.body(), responseHeaders, response.statusCode());

        } catch (Exception e) {
            logger.error("Falha ao fazer relay da requisição: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(e.getMessage().getBytes());
        }
    }

    private URI buildTargetUri(HttpServletRequest request, String idEscola) {
        String originalPath = request.getRequestURI().substring(API_PREFIX.length());
        String queryString = request.getQueryString() == null ? "" : "?" + request.getQueryString();

        if (request.getMethod().equalsIgnoreCase("POST") && originalPath.endsWith("update.php")) {
            return URI.create(demandanetBaseUrl + originalPath + queryString);
        }

        return UriComponentsBuilder.fromHttpUrl(demandanetBaseUrl)
                .path(originalPath)
                .queryParam("idEscola", idEscola)
                .query(request.getQueryString())
                .build(true)
                .toUri();
    }

    private void copyHeadersFromRequest(HttpServletRequest request, HttpRequest.Builder builder, String sessionCookie, String idEscola) {
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            String lowerCaseHeader = headerName.toLowerCase();
            if (!lowerCaseHeader.equals("host") &&
                    !lowerCaseHeader.equals("authorization") &&
                    !lowerCaseHeader.equals("cookie") &&
                    !lowerCaseHeader.equals("content-length") &&
                    !lowerCaseHeader.equals("content-type") &&
                    !lowerCaseHeader.equals("connection")) {
                builder.header(headerName, request.getHeader(headerName));
            }
        });

        builder.header("Cookie", sessionCookie);
        builder.header("Referer", demandanetBaseUrl + "/ordem_servico_gerencia/?idescola=" + idEscola);
    }
}
