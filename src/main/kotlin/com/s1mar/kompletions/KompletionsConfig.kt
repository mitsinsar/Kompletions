package com.s1mar.kompletions

/**
 * Configuration for Kompletions client.
 */
data class KompletionsConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val provider: Provider = Provider.CUSTOM,
    val appUrl: String? = null,
    val appName: String? = null,
    val timeout: Long = 60_000,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * OpenAI configuration
         */
        fun openai(apiKey: String): KompletionsConfig {
            return KompletionsConfig(
                baseUrl = "https://api.openai.com/v1",
                apiKey = apiKey,
                provider = Provider.OPENAI
            )
        }

        /**
         * Ollama configuration
         */
        fun ollama(baseUrl: String = "http://localhost:11434/v1"): KompletionsConfig {
            return KompletionsConfig(
                baseUrl = baseUrl,
                provider = Provider.OLLAMA
            )
        }

        /**
         * OpenRouter configuration
         */
        fun openRouter(
            apiKey: String,
            appUrl: String? = null,
            appName: String? = null
        ): KompletionsConfig {
            return KompletionsConfig(
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = apiKey,
                provider = Provider.OPENROUTER,
                appUrl = appUrl,
                appName = appName
            )
        }

        /**
         * Custom OpenAI-compatible provider
         */
        fun custom(baseUrl: String, apiKey: String? = null): KompletionsConfig {
            return KompletionsConfig(
                baseUrl = baseUrl,
                apiKey = apiKey,
                provider = Provider.CUSTOM
            )
        }
    }
}

/**
 * Supported providers
 */
enum class Provider {
    OPENAI,
    OLLAMA,
    OPENROUTER,
    CUSTOM
}
