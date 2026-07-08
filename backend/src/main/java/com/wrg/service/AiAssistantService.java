package com.wrg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrg.model.WeeklyReport;
import com.wrg.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final WeeklyReportRepository reportRepository;

    @Value("${app.ai.gemini-api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gemini-2.5-flash}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();

    public String ask(String question) {
        String context = buildContext(8);
        String systemPrompt = """
                You are a helpful assistant for an engineering manager reviewing their team's
                weekly status reports. Answer ONLY using the report data provided below.
                Be concise, use bullet points where useful, and call out recurring blockers
                or workload imbalances if relevant to the question. Project names double as
                team/workstream names when the manager asks about "a team". If the data
                doesn't contain the answer, say so plainly instead of guessing.

                TEAM REPORTS (last 8 weeks):
                %s
                """.formatted(context);

        return callGemini(systemPrompt, question, context);
    }

    /**
     * AI-generated team summary: completed work, recurring blockers, and
     * workload imbalances, in three fixed sections.
     */
    public String generateSummary() {
        String context = buildContext(4);
        String systemPrompt = """
                You are summarizing an engineering team's weekly status reports for a manager.
                Using ONLY the report data below, produce a concise summary with exactly these
                three headed sections:

                ## Completed work
                Key things finished this period, grouped by project where useful.

                ## Recurring blockers
                Blockers that show up more than once, or block multiple people. If none, say so.

                ## Workload imbalances
                Note any team member who appears to be significantly more loaded, has missed
                submissions, or is concentrated on one project while others are spread thin.
                If nothing stands out, say so plainly rather than inventing a concern.

                Keep each section to a few bullet points. Do not include information that
                isn't grounded in the report data below.

                TEAM REPORTS (last 4 weeks):
                %s
                """.formatted(context);

        return callGemini(systemPrompt, "Generate the team summary now.", context);
    }

    private String buildContext(int weeks) {
        LocalDate since = LocalDate.now().minusWeeks(weeks - 1L);
        List<WeeklyReport> reports = reportRepository.findAllSince(since);

        if (reports.isEmpty()) {
            return "(No reports found in this time window.)";
        }

        return reports.stream()
                .map(r -> """
                        [%s | %s | week of %s | %s]
                        Completed: %s
                        Planned: %s
                        Blockers: %s
                        """.formatted(
                        r.getUser().getFullName(),
                        r.getProject().getName(),
                        r.getWeekStartDate(),
                        r.getStatus(),
                        blankSafe(r.getTasksCompleted()),
                        blankSafe(r.getTasksPlanned()),
                        blankSafe(r.getBlockers())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String callGemini(String systemPrompt, String userMessage, String rawContextFallback) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI assistant is not configured. Set app.ai.gemini-api-key (env GEMINI_API_KEY) "
                    + "to enable this feature. In the meantime, here is the raw matching context:\n\n" + rawContextFallback;
        }

        RestClient client = RestClient.create("https://generativelanguage.googleapis.com");

        String requestBody;
        try {
            Map<String, Object> body = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", userMessage))
                    )),
                    "generationConfig", Map.of("maxOutputTokens", 1000)
            );
            requestBody = mapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Failed to build AI request", e);
            return "Failed to build AI request: " + e.getMessage();
        }

        try {
            String response = client.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                // Most commonly: the prompt or response was blocked by safety filters.
                String blockReason = root.path("promptFeedback").path("blockReason").asText(null);
                if (blockReason != null) {
                    return "The assistant couldn't answer that (blocked: " + blockReason + "). Try rephrasing.";
                }
                log.warn("Gemini returned no candidates. Raw response: {}", response);
                return "The assistant didn't return a response. Please try again.";
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    sb.append(part.path("text").asText());
                }
            }
            return sb.length() > 0 ? sb.toString() : "The assistant returned an empty response. Please try again.";
        } catch (Exception e) {
            log.error("AI assistant call failed", e);
            return "AI assistant call failed: " + e.getMessage();
        }
    }

    private String blankSafe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
