package com.sanjay.aisecurity.ai;

import com.sanjay.aisecurity.entity.ChatHistory;
import com.sanjay.aisecurity.entity.Vulnerability;
import java.util.List;

/**
 * Reusable utility class to construct clean and structured prompts for the AI model.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public class PromptBuilder {

    private static final int MAX_HISTORY_MESSAGES = 6;

    public static String buildEnrichmentPrompt(Vulnerability vuln) {
        String owasp = vuln.getOwaspCategory() != null ? vuln.getOwaspCategory() : "N/A";
        String cwe = vuln.getCweId() != null ? vuln.getCweId() : "N/A";

        return """
                You are an expert cybersecurity engineer and code auditor.
                
                Analyze the following security vulnerability found in source code and return a JSON object.
                
                Vulnerability Type: %s
                Severity: %s
                File: %s
                Line: %d
                OWASP Category (DO NOT CHANGE): %s
                CWE ID (DO NOT CHANGE): %s
                Code Snippet: %s
                Description: %s
                
                CRITICAL INSTRUCTION 1: Analyze ONLY the evidence provided. Do not invent vulnerabilities, APIs, CWE IDs, OWASP categories, or behavior not supported by the code. If uncertain about mappings, state that it could not be confidently determined.
                CRITICAL INSTRUCTION 2: You MUST use the exact OWASP and CWE values provided above. DO NOT invent or override them.
                CRITICAL INSTRUCTION 3: You MUST generate Secure Code Examples based on the detected language. Never use generic pseudocode. Provide practical secure code that preserves original intent.
                
                Return ONLY valid JSON with exactly these 4 keys (no markdown formatting for the JSON block itself, no extra text, just raw JSON).
                However, INSIDE the string values of the JSON, use Markdown formatting (## Headers, bullet points, code blocks) to provide deep, structured explanations covering the 9 required topics.
                
                Use exactly this structure for the JSON values:
                
                {
                  "explanation": "## Executive Summary\\nProvide an evidence-based executive summary. Avoid exaggerated wording.\\n\\n## Detection Summary\\nExplain exactly why the rule flagged this snippet, referencing the specific variables and API calls.\\n\\n## OWASP Mapping\\nExplain the OWASP Category.\\n\\n## CWE Mapping\\nExplain the CWE ID.",
                  "businessImpact": "## Severity Justification\\nExplain exactly why this finding received its assigned severity level.\\n\\n## Attack Scenario\\nDescribe how an attacker could exploit this.\\n\\n## Security Impact\\nDetail the technical security impact.\\n\\n## Business Impact\\nDescribe the potential real-world damage (data loss, compliance failure).",
                  "rootCause": "## Deep Technical Explanation\\nExplain the vulnerability at a deep technical level.\\n\\n## Root Cause\\nExplain the core programming mistake that led to this issue.\\n\\n## Developer Notes\\nActionable guidance tied directly to the detected code.",
                  "secureCodeExample": "## Remediation Guide\\nExplain WHY the fix works without changing unrelated functionality.\\n\\n## Step-by-step Fix\\n...\\n\\n## Secure Code Example\\n```[language]\\n[secure code]\\n```\\n\\n## Prevention Checklist\\n- [ ] ...\\n- [ ] ...\\n\\n## References\\n..."
                }
                """.formatted(
                vuln.getVulnerabilityType(),
                vuln.getSeverity().name(),
                vuln.getFileName(),
                vuln.getLineNumber(),
                owasp,
                cwe,
                vuln.getCodeSnippet() != null ? vuln.getCodeSnippet() : "N/A",
                vuln.getDescription()
        );
    }

    /**
     * Constructs the system prompt for chatbot conversations including constraints.
     * When hasPdfContext is true, relaxes topic-filtering to allow discussion of the
     * attached document (e.g. security-related research papers, vulnerability reports).
     */
    public static String buildChatSystemPrompt(boolean hasPdfContext) {
        String pdfNote = hasPdfContext
                ? "\n- The user has attached a document for analysis. You MUST use the document content to answer all follow-up questions about it — treat every question as being about that document's security topics. Never say you have not seen the document."
                : "";
        return """
                You are a professional cybersecurity AI assistant integrated into an AI Security Analysis Platform.
                Your role is to help developers understand security vulnerabilities found in their code, explain remediation steps, and answer security-related questions. You also analyze security research papers, reports, and documents shared by the user.
                
                CRITICAL INSTRUCTIONS:
                - If the user asks who founded, built, created, or developed you, you MUST answer that you were built by Sanjay Dharmarajou.
                - ONLY answer questions related to software security, vulnerability analysis, secure coding guidelines (OWASP, etc.), encryption, authentication, authorization (OAuth2, JWT, Spring Security), cybersecurity concepts, and the content of any user-provided security documents.
                - If the user's question clearly refers to an attached document (e.g. 'from the pdf', 'explain this', 'give me the code blocks', 'summarize'), always answer using the document context provided. NEVER refuse because the question seems short or vague if document context is present.
                - If the user asks about ANY topic that is NOT related to cybersecurity, secure software, or their attached document (excluding basic conversational greetings like "hi" or "hello", and questions about your creator, which you should answer politely), politely decline with: 'I am designed to assist only with cybersecurity and secure software development queries.'
                - Keep explanations clear, structured, and actionable. Provide step-by-step guidance and secure code examples when appropriate.
                """ + pdfNote;
    }

    /**
     * Constructs the system prompt without PDF context (backward compat).
     */
    public static String buildChatSystemPrompt() {
        return buildChatSystemPrompt(false);
    }

    /**
     * Constructs the full chat prompt with context, PDF context, and history.
     *
     * <p>Context budget priority (highest to lowest):
     * <ol>
     *   <li>System prompt + current user question</li>
     *   <li>Scan context (usually small)</li>
     *   <li>PDF context (capped at MAX_PDF_CHARS; trimmed if needed)</li>
     *   <li>Recent conversation history (oldest turns dropped first)</li>
     * </ol>
     *
     * @param userMessage  the current user message
     * @param scanContext  optional scan vulnerability context
     * @param pdfContext   extracted PDF text (empty string if none)
     * @param history      prior conversation turns
     */
    public static String buildChatPrompt(String userMessage, String scanContext, String pdfContext, List<ChatHistory> history) {
        // Character safety ceiling: ~24 000 chars ≈ 6 000 tokens (leaves 2 500 for completion)
        final int MAX_PROMPT_CHARS = 24_000;
        // Minimum PDF chars to retain even when trimming — preserves the most important intro
        final int MIN_PDF_CHARS    = 1_500;
        // Max PDF chars to inject per turn
        final int MAX_PDF_CHARS_INJECT = 5_000;

        boolean hasPdf = pdfContext != null && !pdfContext.isBlank();

        // Truncate PDF to safe size before first assembly
        String safePdf = hasPdf
                ? (pdfContext.length() > MAX_PDF_CHARS_INJECT ? pdfContext.substring(0, MAX_PDF_CHARS_INJECT) : pdfContext)
                : "";

        // --- First pass: build with full history ----------------------------------
        String assembled = assembleChatPrompt(userMessage, scanContext, safePdf, history, hasPdf);

        // --- Safety trim: if still too large, drop oldest history turns ----------
        if (assembled.length() > MAX_PROMPT_CHARS && history != null && !history.isEmpty()) {
            int start = 1; // drop progressively from oldest
            while (assembled.length() > MAX_PROMPT_CHARS && start <= history.size()) {
                List<ChatHistory> trimmed = history.subList(
                        Math.min(start, history.size()),
                        history.size());
                assembled = assembleChatPrompt(userMessage, scanContext, safePdf, trimmed, hasPdf);
                start++;
            }
        }

        // --- Emergency trim: if still too large, reduce PDF context --------------
        if (assembled.length() > MAX_PROMPT_CHARS && hasPdf) {
            int pdfLen = Math.max(MIN_PDF_CHARS, MAX_PDF_CHARS_INJECT / 2);
            String shorterPdf = safePdf.substring(0, Math.min(safePdf.length(), pdfLen));
            assembled = assembleChatPrompt(userMessage, scanContext, shorterPdf, null, hasPdf);
        }

        return assembled;
    }

    /** Internal helper: assembles the prompt string from its parts. */
    private static String assembleChatPrompt(String userMessage, String scanContext,
                                              String pdfContext, List<ChatHistory> history,
                                              boolean hasPdf) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildChatSystemPrompt(hasPdf));
        sb.append("\n");

        if (scanContext != null && !scanContext.isBlank()) {
            sb.append(scanContext).append("\n");
        }

        // Always re-inject the PDF context before the conversation history
        // so the AI never loses track of the document across turns
        if (hasPdf && pdfContext != null && !pdfContext.isBlank()) {
            sb.append("--- Attached Document Context (available for the entire conversation) ---\n");
            sb.append(pdfContext).append("\n");
            sb.append("--- End of Document Context ---\n\n");
        }

        if (history != null && !history.isEmpty()) {
            sb.append("--- Previous conversation (for context) ---\n");
            int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
            for (int i = start; i < history.size(); i++) {
                ChatHistory h = history.get(i);
                sb.append("User: ").append(h.getUserMessage()).append("\n");
                sb.append("Assistant: ").append(h.getAiResponse()).append("\n\n");
            }
            sb.append("---\n");
        }

        sb.append("User: ").append(userMessage);
        return sb.toString();
    }

    /**
     * Backward-compatible overload without PDF context.
     */
    public static String buildChatPrompt(String userMessage, String scanContext, List<ChatHistory> history) {
        return buildChatPrompt(userMessage, scanContext, "", history);
    }

    /**
     * Constructs the prompt for generating a project-level AI summary.
     */
    public static String buildProjectSummaryPrompt(int totalScannedFiles, int totalVulnerabilities, int crit, int high, int med, int low, List<Vulnerability> vulns) {
        StringBuilder vulnTypes = new StringBuilder();
        // Extract unique vulnerability types to give the AI context of what was found
        vulns.stream()
                .map(Vulnerability::getVulnerabilityType)
                .distinct()
                .forEach(type -> vulnTypes.append("- ").append(type).append("\n"));

        return """
                You are an expert cybersecurity auditor. 
                Generate a concise, professional executive summary for a recent source code scan.
                
                Scan Statistics:
                Total Files Scanned: %d
                Total Vulnerabilities: %d
                Critical: %d
                High: %d
                Medium: %d
                Low: %d
                
                Types of issues found:
                %s
                
                Provide a professional, analytical executive summary of the overall security posture. 
                Example format: 'The scan analyzed X files and identified Y confirmed vulnerabilities across [Categories]. Z Critical findings require immediate remediation. The overall project security score reflects these findings based on deterministic rule matching.'
                Do NOT use JSON. Respond ONLY with professional markdown text.
                """.formatted(totalScannedFiles, totalVulnerabilities, crit, high, med, low, vulnTypes.toString());
    }
}
