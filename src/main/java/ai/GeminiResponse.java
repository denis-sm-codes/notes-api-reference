package ai;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public String getFirstCandidateText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate firstCandidate = candidates.get(0);
            if (firstCandidate != null && firstCandidate.content() != null) {
                List<Part> parts = firstCandidate.content().parts();
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).text();
                }
            }
        }
        return "";
    }
}