/**
 * AI Integration package.
 *
 * <p>Contains the provider-independent AI service abstraction and concrete
 * Google Gemini implementation. The architecture supports adding future
 * providers (OpenAI, Claude, Ollama) without changing business logic.</p>
 *
 * <p>Key Classes: {@code AIProvider} (interface), {@code GeminiProvider},
 * {@code PromptBuilder}, {@code PromptTemplates}, {@code AIResponseParser}.</p>
 */
package com.sanjay.aisecurity.ai;
