package tk.jaooo.osmanager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDetailsDTO;
import tk.jaooo.osmanager.services.DemandanetClientService;
import tk.jaooo.osmanager.services.DemandanetParserService;
import tk.jaooo.osmanager.services.DemandanetSessionManager;
import tk.jaooo.osmanager.services.PdfService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    private static final Logger logger = LoggerFactory.getLogger(PrintController.class);

    private final DemandanetSessionManager sessionManager;
    private final DemandanetClientService clientService;
    private final DemandanetParserService parserService;
    private final PdfService pdfService;

    public PrintController(DemandanetSessionManager sessionManager,
                           DemandanetClientService clientService,
                           DemandanetParserService parserService,
                           PdfService pdfService) {
        this.sessionManager = sessionManager;
        this.clientService = clientService;
        this.parserService = parserService;
        this.pdfService = pdfService;
    }

    @PostMapping("/custom")
    public ResponseEntity<byte[]> printCustomPdf(
            @RequestBody List<String> osIds,
            @AuthenticationPrincipal Tecnico solicitante) {

        Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
        String cookie = sessionManager.getSessionForOwner(owner);

        List<ConsultedOrderDetailsDTO> detailsList = new ArrayList<>();

        for (String osId : osIds) {
            try {
                String html = clientService.getOrderDetailsHtml(cookie, osId).block();

                if (sessionManager.isSessionExpiredResponse(html)) {
                    cookie = sessionManager.refreshSessionForOwner(owner);
                    html = clientService.getOrderDetailsHtml(cookie, osId).block();
                }

                if (html != null && !html.contains("Nenhuma ordem")) {
                    detailsList.add(parserService.parseOrderDetails(html));
                }
            } catch (Exception e) {
                logger.error("Erro ao buscar dados da OS {} para impressão customizada", osId, e);
            }
        }

        if (detailsList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = pdfService.generateCustomPdfBatch(detailsList);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=os_custom_batch.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/legacy")
    public ResponseEntity<byte[]> printLegacyPdf(
            @RequestBody List<String> osIds,
            @AuthenticationPrincipal Tecnico solicitante) {

        Tecnico owner = sessionManager.resolveCredentialOwner(solicitante);
        String cookie = sessionManager.getSessionForOwner(owner);

        List<byte[]> collectedPdfs = new ArrayList<>();

        for (String osId : osIds) {
            try {
                byte[] pdf = clientService.downloadLegacyPdf(cookie, osId).block();

                if (pdf != null && pdf.length > 4 && pdf[0] != 0x25) {
                    String content = new String(pdf);
                    if (sessionManager.isSessionExpiredResponse(content)) {
                        logger.info("Sessão expirada ao baixar PDF da OS {}. Renovando...", osId);
                        cookie = sessionManager.refreshSessionForOwner(owner);
                        pdf = clientService.downloadLegacyPdf(cookie, osId).block();
                    }
                }

                if (pdf != null && pdf.length > 0 && pdf[0] == 0x25) {
                    collectedPdfs.add(pdf);
                } else {
                    logger.warn("PDF da OS {} retornou vazio ou inválido (possível status não permitido pelo legado)", osId);
                }

            } catch (Exception e) {
                logger.error("Erro ao baixar PDF legado da OS {}", osId, e);
            }
        }

        if (collectedPdfs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] mergedPdf = pdfService.mergePdfs(collectedPdfs);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=os_legacy_merged.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(mergedPdf);
    }
}
