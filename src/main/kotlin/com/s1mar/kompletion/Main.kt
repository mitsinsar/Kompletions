package com.s1mar.kompletion

import kotlinx.coroutines.runBlocking

/**
 * Quick demo of Kompletion.
 * Connects to a local Ollama instance by default.
 */
fun main() = runBlocking {
    KompletionClient.ollama().use { client ->
        // Simple one-liner
        val response = client.sendMessage(
            model = "gemma3:4b",
            message = "What is Kotlin in one sentence?"
        )
        println(response.choices.first().message.content)

        // DSL builder
        val dslResponse = client.chatCompletion {
            model = "gemma3:4b"
            temperature = 0.7
            maxTokens = 100

            system("You are a helpful assistant.")
            user("What is a data class in Kotlin?")
        }
        println(dslResponse.choices.first().message.content)
    }
}
