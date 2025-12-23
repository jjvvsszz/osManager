package tk.jaooo.osmanager.model.dto.gemini;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    public String getFirstCandidateText() {
        if (candidates != null && !candidates.isEmpty()) {
            var firstCandidate = candidates.getFirst();
            if (firstCandidate.content() != null &&
                    firstCandidate.content().parts() != null &&
                    !firstCandidate.content().parts().isEmpty()) {
                return firstCandidate.content().parts().getFirst().text();
            }
        }
        return "";
    }
}
