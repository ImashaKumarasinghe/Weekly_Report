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

/**
 * "Good to Have" AI Chat Assistant - backed by the Google Gemini API.
 *
 * APPROACH
 * --------
 * Lightweight, retrieval-based RAG - no vector database. At this scale (a
 * team's worth of weekly reports, a handful of months of history) the entire
 * relevant context comfortably fits inside a single prompt, so we simply:
 *   1. Query MySQL directly for reports in the requested time window.
 *   2. Flatten each report into a compact plain-text block (member, project,
 *      week, status, completed/planned/blockers).
 *   3. Pass that block to Gemini as a system instruction, alongside either
 *      the manager's free-form question (Q&A mode) or a fixed instruction
 *      asking for a structured summary (Summary mode).
 * This keeps the integration simple, cheap, and easy to audit - every answer
 * is traceable back to real rows in the reports table, and there's no
 * embedding pipeline or extra infrastructure to maintain.
 *
 * PROMPT DESIGN
 * -------------
 * The system prompt explicitly instructs the model to answer ONLY from the
 * supplied report data and to say so plainly if the data doesn't cover the
 * question, rather than guessing - this keeps answers grounded and avoids
 * hallucinated project/people details. The summary prompt additionally asks
 * for three fixed sections (Completed work / Recurring blockers / Workload
 * imbalances) so the output is predictable and easy to scan.
 *
 * DATA-PRIVACY CONSIDERATIONS
 * ----------------------------
 *  - Endpoint is restricted to MANAGER role only (enforced in SecurityConfig).
 *  - Only report content for the requested window (default: last 8 weeks) is
 *    sent - never the whole database, and never other managers' or team
 *    members' account data.
 *  - The only personal identifier included is full name. Emails, password
 *    hashes, and auth tokens are never part of the prompt.
 *  - Nothing is written back to the database - this is a read-only feature.
 *  - Conversation history is kept client-side only (in the browser widget's
 *    React state) and is never persisted server-side; each request to this
 *    service is stateless and independent.
 *  - If the Gemini API key isn't configured, the feature degrades gracefully
 *    instead of failing the rest of the app.
 *  - Note: on Gemini's free tier, prompt/response data may be used by Google
 *    to improve their products (see Gemini API terms). If that's a concern,
 *    switch to a paid-tier key, which carries a different data-use contract.
 */
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
