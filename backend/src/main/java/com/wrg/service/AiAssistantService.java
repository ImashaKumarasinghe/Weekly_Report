package com.wrg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrg.model.WeeklyReport;
import com.wrg.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Good to Have" AI Chat Assistant.
 *
 * Approach: retrieval-light RAG. We pull the relevant weekly reports
 * straight from the database (no vector store needed at this scale -
 * a few dozen/hundred reports fits comfortably in a single prompt),
 * flatten them into plain text, and pass that as context to Claude
 * along with the manager's question. The model never receives
 * anything beyond report text (tasks/blockers/notes) and member/project
 * names - no credentials, emails, or account data are sent.
 *
 * Data-privacy notes (see also the presentation):
 *  - Only report content for the requested time window is sent, not the
 *    whole database.
 *  - No PII beyond full name is included; emails/passwords are never
 *    part of the prompt.
 *  - This endpoint is restricted to MANAGER role via SecurityConfig.
 */
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final WeeklyReportRepository reportRepository;

    @Value("${app.ai.anthropic-api-key:}")
    private String apiKey;

    @Value("${app.ai.model:claude-sonnet-4-6}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();

    public String ask(String question) {
        LocalDate since = LocalDate.now().minusWeeks(8);
        List<WeeklyReport> reports = reportRepository.findAllSince(since);

        String context = reports.stream()
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

        if (apiKey == null || apiKey.isBlank()) {
            return "AI assistant is not configured. Set app.ai.anthropic-api-key (env ANTHROPIC_API_KEY) "
                    + "to enable this feature. In the meantime, here is the raw matching context:\n\n" + context;
        }

        String systemPrompt = """
                You are a helpful assistant for an engineering manager reviewing their team's
                weekly status reports. Answer ONLY using the report data provided below.
                Be concise, use bullet points where useful, and call out recurring blockers
                or workload imbalances if relevant to the question. If the data doesn't
                contain the answer, say so plainly instead of guessing.

                TEAM REPORTS (last 8 weeks):
                %s
                """.formatted(context);

        RestClient client = RestClient.create("https://api.anthropic.com");

        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 1000,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", question))
            ));
        } catch (Exception e) {
            return "Failed to build AI request: " + e.getMessage();
        }

        try {
            String response = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);
            JsonNode contentArray = root.path("content");
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "AI assistant call failed: " + e.getMessage();
        }
    }

    private String blankSafe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
