package tk.jaooo.osmanager.services;

import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDetailsDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font FONT_VIA = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8);

    private static final float PAGE_HEIGHT = PageSize.A4.getHeight();
    private static final float PAGE_WIDTH = PageSize.A4.getWidth();
    private static final float MARGIN = 20f;
    private static final float USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2);

    private static final float MIDDLE_Y = PAGE_HEIGHT / 2;

    private static final float VIA_HEIGHT = (PAGE_HEIGHT / 2) - (MARGIN * 2) - 10f;

    public byte[] generateCustomPdfBatch(List<ConsultedOrderDetailsDTO> orders) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            for (ConsultedOrderDetailsDTO order : orders) {
                document.newPage();

                drawDottedLine(writer);

                PdfPTable via1 = buildViaTable(order, "1ª Via - Escola / Setor");
                document.add(via1);

                float currentY = writer.getVerticalPosition(true);
                float gap = currentY - MIDDLE_Y + 10f;

                PdfPTable spacer = new PdfPTable(1);
                spacer.setTotalWidth(USABLE_WIDTH);
                spacer.setLockedWidth(true);
                PdfPCell spaceCell = new PdfPCell(new Phrase(" "));
                spaceCell.setBorder(Rectangle.NO_BORDER);
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("\n"));

                PdfPTable via2 = buildViaTable(order, "2ª Via - Equipe Técnica");
                document.add(via2);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF customizado", e);
        }
    }

    private PdfPTable buildViaTable(ConsultedOrderDetailsDTO order, String viaLabel) {
        PdfPTable container = new PdfPTable(1);
        container.setTotalWidth(USABLE_WIDTH);
        container.setLockedWidth(true);
        container.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        container.getDefaultCell().setPadding(0);

        PdfPTable header = createHeader(viaLabel);
        float hHeader = calculateHeight(header);

        PdfPTable info = createInfoTable(order);
        float hInfo = calculateHeight(info);

        PdfPTable desc = createDescriptionTable(order);
        float hDesc = calculateHeight(desc);

        PdfPTable signatures = createSignaturesTable();
        float hSig = calculateHeight(signatures);

        float usedHeight = hHeader + hInfo + hDesc + hSig;
        float paddingSafety = 15f;

        float remainingHeight = VIA_HEIGHT - usedHeight - paddingSafety;

        if (remainingHeight < 40f) remainingHeight = 40f;

        PdfPTable solution = createSolutionTable(remainingHeight);

        container.addCell(wrapInCell(header));
        container.addCell(createSpacerCell(3f));
        container.addCell(wrapInCell(info));
        container.addCell(createSpacerCell(3f));
        container.addCell(wrapInCell(desc));
        container.addCell(createSpacerCell(3f));
        container.addCell(wrapInCell(solution));
        container.addCell(createSpacerCell(3f));
        container.addCell(wrapInCell(signatures));

        return container;
    }

    private PdfPTable createHeader(String viaLabel) {
        PdfPTable table = new PdfPTable(2);
        try {
            table.setTotalWidth(USABLE_WIDTH);
            table.setLockedWidth(true);
            table.setWidths(new float[]{3f, 1f});

            PdfPCell title = new PdfPCell(new Phrase("COMPROVANTE DE EXECUÇÃO DE SERVIÇO - TI", FONT_TITLE));
            title.setBorder(Rectangle.NO_BORDER);
            table.addCell(title);

            PdfPCell label = new PdfPCell(new Phrase(viaLabel, FONT_VIA));
            label.setHorizontalAlignment(Element.ALIGN_RIGHT);
            label.setBorder(Rectangle.NO_BORDER);
            table.addCell(label);
        } catch (Exception e) { logger.error("Erro header", e); }
        return table;
    }

    private PdfPTable createInfoTable(ConsultedOrderDetailsDTO order) {
        PdfPTable table = new PdfPTable(4);
        try {
            table.setTotalWidth(USABLE_WIDTH);
            table.setLockedWidth(true);
            table.setWidths(new float[]{1f, 2f, 1f, 2f});

            addCell(table, "Nº OS:", order.osNumber());
            addCell(table, "Data:", order.dataCadastro());

            PdfPCell lEscola = createLabelCell("Local/Escola:");
            table.addCell(lEscola);
            PdfPCell vEscola = createValueCell(order.requisitante());
            vEscola.setColspan(3);
            table.addCell(vEscola);

            addCell(table, "Técnico:", order.tecnicoResponsavel());
            addCell(table, "Patrimônio:", order.patrimonio());

            addCell(table, "Tipo:", order.tipoServico());
            // Célula vazia para fechar a linha
            table.addCell(createLabelCell(""));
            table.addCell(createValueCell(""));

        } catch (Exception e) { logger.error("Erro info", e); }
        return table;
    }

    private PdfPTable createDescriptionTable(ConsultedOrderDetailsDTO order) {
        PdfPTable table = new PdfPTable(1);
        table.setTotalWidth(USABLE_WIDTH);
        table.setLockedWidth(true);

        PdfPCell label = createLabelCell("Defeito Relatado / Descrição:");
        label.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        table.addCell(label);

        String fullText = "Defeito: " + order.defeito() + "\nDetalhes: " + order.descricao();
        PdfPCell value = new PdfPCell(new Phrase(fullText, FONT_VALUE));
        value.setBorder(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT);
        value.setPadding(5);
        table.addCell(value);

        return table;
    }

    private PdfPTable createSolutionTable(float fixedHeight) {
        PdfPTable table = new PdfPTable(1);
        table.setTotalWidth(USABLE_WIDTH);
        table.setLockedWidth(true);

        PdfPCell label = createLabelCell("Solução Técnica / Observações:");
        label.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        table.addCell(label);

        PdfPCell space = new PdfPCell(new Phrase(" "));
        space.setBorder(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT);
        space.setFixedHeight(fixedHeight); // Aqui está a mágica: Altura calculada
        table.addCell(space);

        return table;
    }

    private PdfPTable createSignaturesTable() {
        PdfPTable table = new PdfPTable(2);
        table.setTotalWidth(USABLE_WIDTH);
        table.setLockedWidth(true);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(new Phrase("\n", FONT_SMALL));
        table.addCell(new Phrase("\n", FONT_SMALL));

        table.addCell(new Phrase("_________________________________", FONT_SMALL));
        table.addCell(new Phrase("_________________________________", FONT_SMALL));
        table.addCell(new Phrase("Assinatura do Técnico", FONT_SMALL));
        table.addCell(new Phrase("Carimbo/Assinatura da Escola", FONT_SMALL));

        return table;
    }

    private float calculateHeight(PdfPTable table) {
        return table.calculateHeights(true);
    }

    private void drawDottedLine(PdfWriter writer) {
        PdfContentByte canvas = writer.getDirectContent();
        canvas.saveState();
        canvas.setLineWidth(1f);
        canvas.setLineDash(3f, 3f);
        canvas.setColorStroke(java.awt.Color.GRAY);

        canvas.moveTo(MARGIN, MIDDLE_Y);
        canvas.lineTo(PAGE_WIDTH - MARGIN, MIDDLE_Y);

        canvas.stroke();
        canvas.restoreState();
    }

    private PdfPCell wrapInCell(PdfPTable table) {
        PdfPCell cell = new PdfPCell(table);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private PdfPCell createSpacerCell(float height) {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(height);
        return cell;
    }

    private void addCell(PdfPTable table, String label, String value) {
        table.addCell(createLabelCell(label));
        table.addCell(createValueCell(value));
    }

    private PdfPCell createLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_LABEL));
        cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        cell.setPadding(3);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
        return cell;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FONT_VALUE));
        cell.setPadding(3);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    public byte[] mergePdfs(List<byte[]> pdfs) {
        if (pdfs.isEmpty()) return new byte[0];
        PDFMergerUtility merger = new PDFMergerUtility();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            merger.setDestinationStream(out);
            for (byte[] pdfBytes : pdfs) {
                if (pdfBytes != null && pdfBytes.length > 0) {
                    merger.addSource(new RandomAccessReadBuffer(pdfBytes));
                }
            }
            merger.mergeDocuments(null);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao mesclar PDFs legados", e);
        }
    }
}
