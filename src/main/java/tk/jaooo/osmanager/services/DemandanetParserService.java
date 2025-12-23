package tk.jaooo.osmanager.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDTO;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDetailsDTO;

import java.util.ArrayList;
import java.util.List;

@Service
public class DemandanetParserService {

    public List<ConsultedOrderDTO> parseOrderList(String html) {
        Document doc = Jsoup.parse(html);
        List<ConsultedOrderDTO> orders = new ArrayList<>();

        // Seletor baseado no seu frontend: #tabelaOrdem tbody tr
        Elements rows = doc.select("#tabelaOrdem tbody tr");

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() >= 6) {
                orders.add(new ConsultedOrderDTO(
                        cells.get(0).text().trim(), // ID
                        cells.get(1).text().trim(), // Escola
                        cells.get(2).text().trim(), // Tipo
                        cells.get(3).text().trim(), // Defeito
                        cells.get(4).text().trim(), // Patrimonio
                        cells.get(5).text().trim()  // Responsavel
                ));
            }
        }
        return orders;
    }

    public ConsultedOrderDetailsDTO parseOrderDetails(String html) {
        Document doc = Jsoup.parse(html);

        // Lógica de extração baseada no App.tsx
        String osNumber = getValueOrText(doc, "#idOrdem");
        String requisitante = getValueOrText(doc, "#escolaOut");
        String tipoServico = getValueOrText(doc, "#tipo");
        String defeito = getValueOrText(doc, "#defeitoOut");
        String descricao = getValueOrText(doc, "#descricaoOut");
        String patrimonio = getValueOrText(doc, "#patrimonioOut");
        String situacao = getValueOrText(doc, "#situacaoOut");
        String dataCadastro = getValueOrText(doc, "#dataCadastroOut");

        if (defeito.isEmpty()) defeito = "Não encontrado";
        if (descricao.isEmpty()) descricao = "Não encontrado";
        if (patrimonio.isEmpty()) patrimonio = "Não encontrado";

        return new ConsultedOrderDetailsDTO(
                osNumber, requisitante, tipoServico, defeito,
                descricao, patrimonio, situacao, dataCadastro
        );
    }

    private String getValueOrText(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        if (el == null) return "";

        // Tenta pegar o atributo value (input/textarea), senão pega o texto (div/span)
        if (el.hasAttr("value")) {
            return el.attr("value").trim();
        }
        return el.text().trim();
    }
}
