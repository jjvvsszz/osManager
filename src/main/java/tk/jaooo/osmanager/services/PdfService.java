package tk.jaooo.osmanager.services;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.DottedLineSeparator;
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

    public byte[] generateCustomPdfBatch(List<ConsultedOrderDetailsDTO> orders) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();

            for (ConsultedOrderDetailsDTO order : orders) {
                document.newPage();

                addViaContent(document, order, "1ª Via - Escola / Setor");

                document.add(new Paragraph(" "));
                DottedLineSeparator separator = new DottedLineSeparator();
                separator.setGap(2f);
                document.add(separator);
                document.add(new Paragraph(" "));

                addViaContent(document, order, "2ª Via - Equipe Técnica");
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF customizado", e);
        }
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

    private void addViaContent(Document doc, ConsultedOrderDetailsDTO order, String viaLabel) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3f, 1f});

        PdfPCell titleCell = new PdfPCell(new Phrase("COMPROVANTE DE EXECUÇÃO DE SERVIÇO - TI", FONT_TITLE));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        PdfPCell viaCell = new PdfPCell(new Phrase(viaLabel, FONT_VIA));
        viaCell.setBorder(Rectangle.NO_BORDER);
        viaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        viaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(viaCell);

        doc.add(headerTable);
        doc.add(new Paragraph(" ")); // Espaçamento

        PdfPTable infoTable = new PdfPTable(4); // 4 Colunas
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1f, 2f, 1f, 2f}); // Ajuste de larguras

        addCell(infoTable, "Nº OS:", order.osNumber());
        addCell(infoTable, "Data:", order.dataCadastro());

        PdfPCell labelEscola = createLabelCell("Local/Escola:");
        infoTable.addCell(labelEscola);
        PdfPCell valueEscola = createValueCell(order.requisitante());
        valueEscola.setColspan(3);
        infoTable.addCell(valueEscola);

        addCell(infoTable, "Técnico:", order.tecnicoResponsavel());
        addCell(infoTable, "Patrimônio:", order.patrimonio());

        addCell(infoTable, "Tipo:", order.tipoServico());
        PdfPCell emptyLabel = createLabelCell("");
        PdfPCell emptyValue = createValueCell("");
        infoTable.addCell(emptyLabel);
        infoTable.addCell(emptyValue);

        doc.add(infoTable);

        PdfPTable problemTable = new PdfPTable(1);
        problemTable.setWidthPercentage(100);
        problemTable.setSpacingBefore(5);

        PdfPCell defLabel = createLabelCell("Defeito Relatado / Descrição:");
        defLabel.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        problemTable.addCell(defLabel);

        String fullDesc = "Defeito: " + order.defeito() + "\nDetalhes: " + order.descricao();
        PdfPCell defValue = new PdfPCell(new Phrase(fullDesc, FONT_VALUE));
        defValue.setBorder(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT);
        defValue.setPadding(5);
        defValue.setMinimumHeight(40f); // Altura fixa mínima
        problemTable.addCell(defValue);

        doc.add(problemTable);

        PdfPTable solutionTable = new PdfPTable(1);
        solutionTable.setWidthPercentage(100);
        solutionTable.setSpacingBefore(5);

        PdfPCell solLabel = createLabelCell("Solução Técnica / Observações:");
        solLabel.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        solutionTable.addCell(solLabel);

        PdfPCell solSpace = new PdfPCell(new Phrase("\n\n\n\n")); // Espaço para caneta
        solSpace.setBorder(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT);
        solutionTable.addCell(solSpace);

        doc.add(solutionTable);

        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.setSpacingBefore(15);
        signatures.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        signatures.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

        signatures.addCell(new Phrase("_________________________________", FONT_SMALL));
        signatures.addCell(new Phrase("_________________________________", FONT_SMALL));
        signatures.addCell(new Phrase("Assinatura do Técnico", FONT_SMALL));
        signatures.addCell(new Phrase("Carimbo/Assinatura da Escola", FONT_SMALL));

        doc.add(signatures);
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
        return cell;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FONT_VALUE));
        cell.setPadding(3);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
