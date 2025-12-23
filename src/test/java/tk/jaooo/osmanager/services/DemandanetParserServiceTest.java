package tk.jaooo.osmanager.services;

import org.junit.jupiter.api.Test;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDTO;
import tk.jaooo.osmanager.model.dto.ConsultedOrderDetailsDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemandanetParserServiceTest {

    private final DemandanetParserService parserService = new DemandanetParserService();

    @Test
    void deveParsearListaDeOrdensCorretamente() {
        String htmlSimulado = """
                <html>
                <body>
                    <table id="tabelaOrdem">
                        <tbody>
                            <tr>
                                <td>12345</td>
                                <td>Escola Estadual A</td>
                                <td>Manutenção</td>
                                <td>Computador não liga</td>
                                <td>PAT-001</td>
                                <td>Jose Tecnico</td>
                            </tr>
                            <tr>
                                <td>12346</td>
                                <td>Escola Municipal B</td>
                                <td>Rede</td>
                                <td>Sem internet</td>
                                <td>PAT-002</td>
                                <td>Maria Tecnica</td>
                            </tr>
                        </tbody>
                    </table>
                </body>
                </html>
                """;

        List<ConsultedOrderDTO> ordens = parserService.parseOrderList(htmlSimulado);

        assertEquals(2, ordens.size());

        ConsultedOrderDTO primeira = ordens.get(0);
        assertEquals("12345", primeira.id());
        assertEquals("Escola Estadual A", primeira.escola());
        assertEquals("PAT-001", primeira.patrimonio());

        ConsultedOrderDTO segunda = ordens.get(1);
        assertEquals("Maria Tecnica", segunda.responsavel());
    }

    @Test
    void deveParsearDetalhesDaOrdemCorretamente() {
        String htmlSimulado = """
                <html>
                <body>
                    <input id="idOrdem" value="12345">
                    <input id="escolaOut" value="Escola Teste">
                    <input id="tipo" value="Hardware">
                    <textarea id="defeitoOut">Tela Azul</textarea>
                    <textarea id="descricaoOut">Ocorreu após atualização</textarea>
                    <input id="patrimonioOut" value="XYZ-999">
                    <input id="situacaoOut" value="Em Aberto">
                    <input id="dataCadastroOut" value="22/12/2025">
                </body>
                </html>
                """;

        ConsultedOrderDetailsDTO detalhes = parserService.parseOrderDetails(htmlSimulado);

        assertEquals("12345", detalhes.osNumber());
        assertEquals("Escola Teste", detalhes.requisitante());
        assertEquals("Tela Azul", detalhes.defeito());
        assertEquals("XYZ-999", detalhes.patrimonio());
        assertEquals("22/12/2025", detalhes.dataCadastro());
    }

    @Test
    void deveLidarComCamposVazios() {
        String htmlVazio = "<html><body></body></html>";
        ConsultedOrderDetailsDTO detalhes = parserService.parseOrderDetails(htmlVazio);

        assertEquals("Não encontrado", detalhes.defeito());
        assertEquals("", detalhes.osNumber());
    }
}
