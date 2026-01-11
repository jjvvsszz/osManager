package tk.jaooo.osmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "Gera PDF Customizado em lote",
            description = "Gera um PDF unificado com layout otimizado (2 vias por página). " +
                    "**Importante:** Apenas ordens com situação **'Agendada'** ou **'Em Andamento'** serão processadas. " +
                    "Ordens com outros status (ex: Concluída, Aberta) serão ignoradas silenciosamente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Nenhuma das OS informadas estava apta para impressão (status inválido ou não encontrada)",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor",
                    content = @Content)
    })
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
                    ConsultedOrderDetailsDTO dto = parserService.parseOrderDetails(html);

                    // --- VERIFICAÇÃO DE STATUS ADICIONADA AQUI ---
                    if (isStatusImprimivel(dto.situacao())) {
                        detailsList.add(dto);
                    } else {
                        logger.warn("OS {} ignorada na impressão. Status atual: {}", osId, dto.situacao());
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao processar OS {} para impressão", osId, e);
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

    @Operation(
            summary = "Baixa e mescla PDFs originais do sistema legado",
            description = "Baixa os arquivos PDF gerados pelo Demandanet e os une em um único arquivo. " +
                    "O sistema legado retorna o PDF apenas se a OS estiver **'Agendada'** ou **'Em Andamento'**."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF mesclado gerado com sucesso",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Nenhum PDF pôde ser baixado (provavelmente status inválidos)",
                    content = @Content)
    })
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

                if (pdf != null && pdf.length > 4 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F') {
                    collectedPdfs.add(pdf);
                } else {
                    if (pdf != null) {
                        String content = new String(pdf);
                        if (sessionManager.isSessionExpiredResponse(content)) {
                            logger.info("Sessão expirada (Legacy PDF). Renovando...");
                            cookie = sessionManager.refreshSessionForOwner(owner);
                            byte[] retryPdf = clientService.downloadLegacyPdf(cookie, osId).block();
                            if (retryPdf != null && retryPdf.length > 4 && retryPdf[0] == '%') {
                                collectedPdfs.add(retryPdf);
                            }
                        } else {
                            logger.warn("OS {} não retornou um PDF. Provavelmente status inválido.", osId);
                        }
                    }
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

    private boolean isStatusImprimivel(String situacao) {
        if (situacao == null) return false;
        String s = situacao.trim().toLowerCase();
        return s.contains("agendada") || s.contains("andamento");
    }
}
