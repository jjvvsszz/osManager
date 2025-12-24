package tk.jaooo.osmanager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder; // Importar a classe correta
import tk.jaooo.osmanager.model.dto.gemini.GeminiRequest;
import tk.jaooo.osmanager.model.dto.gemini.GeminiResponse;

import java.net.URI; // Importar a classe URI

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.base-url}")
    private String baseUrl;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String gerarObservacaoTecnica(String defeito, String descricao) {
        String prompt = String.format(
                "Com base no defeito \"%s\" e na descrição \"%s\", crie uma observação técnica para o encerramento da ordem de serviço. " +
                        "A observação deve ser uma única frase, curta e simples, sem qualquer formatação (markdown, negrito, etc). " +
                        "Retorne apenas o texto da observação.",
                defeito, descricao
        );

        try {
            URI finalUri = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();
            // ------------------------------------

            GeminiResponse response = webClient.post()
                    .uri(finalUri)
                    .bodyValue(GeminiRequest.of(prompt))
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            if (response != null) {
                String text = response.getFirstCandidateText();
                return text.trim();
            }
        } catch (Exception e) {
            logger.error("Erro ao chamar API do Gemini", e);
            // Fallback em caso de erro da IA
            return "Observação gerada automaticamente indisponível.";
        }

        return "";
    }
}
