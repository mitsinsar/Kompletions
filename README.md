```text
 ██╗  ██╗                 _      _   _
 ██║ ██╔╝ ___  _ __  _ __ | | ___| |_(_) ___  _ __  ___
 █████╔╝ / _ \| '  \| '_ \| |/ _ \  _| |/ _ \| '_ \/ __|
 ██╔═██╗| (_) | | | | |_) | |  __/ |_| | (_) | | | \__ \
 ██║  ██╗\___/|_| |_| .__/|_|\___|\__|_|\___/|_| |_|___/
 ╚═╝  ╚═╝           |_|
```

# Kompletions

[![CI](https://github.com/s1mar/Kompletions/actions/workflows/ci.yml/badge.svg)](https://github.com/s1mar/Kompletions/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.s1mar/kompletions)](https://central.sonatype.com/artifact/io.github.s1mar/kompletions)

A lightweight Kotlin client for OpenAI-compatible chat completion APIs. Works with OpenAI, Ollama, OpenRouter, and any provider that implements the OpenAI API format.

## Features

- **Kotlin DSL** for building chat requests
- **Multi-provider** support (OpenAI, Ollama, OpenRouter, custom)
- **Streaming** via `Flow<ChatCompletionChunk>` (SSE)
- **Tool calling** — full round-trip: define tools, receive calls, provide results
- **Structured outputs** — JSON mode and JSON Schema enforcement
- **Vision / multi-modal** — send text + images in a single message
- **Conversation management** with message history, rollback on failure, and concurrency safety
- **Custom headers** for authentication proxies, observability, etc.
- **Minimal dependencies** — Ktor + kotlinx-serialization only

## Why Kompletions?

- **DSL-first design** — Chat requests read like native Kotlin, not Java builder chains
- **Provider-agnostic** — One client for OpenAI, Ollama, OpenRouter, Azure, or any OpenAI-compatible endpoint. Swap a URL, not your codebase
- **Focused scope** — Chat completions done right, not a sprawling API wrapper
- **Small codebase** — Auditable and readable end-to-end in an afternoon
- **Low floor, high ceiling** — A beginner can make their first AI call in 5 lines. A senior can build streaming tool-calling agents with structured outputs

## Setup

### Prerequisites

- JDK 17+
- Gradle 8+

### Build

```bash
./gradlew build
```

### Tests

66 tests across 6 test suites covering serialization, DSL builders, conversation management, SSE parsing, concurrency safety, and provider configuration. All tests use mocked HTTP responses — no API keys or network access required.

```bash
./gradlew test
```

### Gradle dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.s1mar:kompletions:0.1.0")
}
```

For local development, add as a composite build:

```kotlin
// settings.gradle.kts
includeBuild("path/to/Kompletions")
```

## Quick Start

### One-liner

```kotlin
KompletionClient.ollama().use { client ->
    val response = client.sendMessage("llama2", "What is Kotlin?")
    println(response.choices.first().message.textContent)
}
```

### DSL Builder

```kotlin
KompletionClient.openai("api-key").use { client ->
    val response = client.chatCompletion {
        model = "gpt-4"
        temperature = 0.7
        maxTokens = 200

        system("You are a helpful assistant.")
        user("Explain Kotlin coroutines in two sentences.")
    }
    println(response.choices.first().message.textContent)
}
```

### Conversation (multi-turn)

```kotlin
KompletionClient.ollama().use { client ->
    val conversation = client.conversation("llama2", systemPrompt = "You are a tutor.")

    val a1 = conversation.send("What is a data class?")
    val a2 = conversation.send("Show me an example.")

    // Access full response
    val fullResponse = conversation.sendFull("Explain inheritance.")
    println("Tokens: ${fullResponse.usage?.totalTokens}")

    // Inspect history
    conversation.getHistory().forEach { msg ->
        println("${msg.role}: ${msg.textContent}")
    }
}
```

### Resuming a conversation

```kotlin
// Save history from a previous session
val savedHistory = previousConversation.getHistory()

// Resume later
val conversation = client.conversation(
    model = "llama2",
    initialHistory = savedHistory
)
conversation.send("Where were we?")
```

### Streaming

```kotlin
KompletionClient.openai("api-key").use { client ->
    client.streamChatCompletion {
        model = "gpt-4"
        user("Write a haiku about Kotlin.")
    }.collect { chunk ->
        chunk.choices.firstOrNull()?.delta?.content?.let { print(it) }
    }
    println()
}
```

## Tool Calling

Full round-trip tool calling: define tools, receive calls from the model, provide results, and get the final answer.

```kotlin
val weatherParams = Json.parseToJsonElement("""
    {
        "type": "object",
        "properties": {
            "location": { "type": "string", "description": "City name" }
        },
        "required": ["location"]
    }
""".trimIndent())

// Step 1: Send request with tool definitions
val response = client.chatCompletion {
    model = "gpt-4"
    user("What's the weather in London?")
    tool("get_weather", "Get current weather", weatherParams)
    toolChoice = "auto"
}

val assistantMsg = response.choices.first().message
val toolCall = assistantMsg.toolCalls?.firstOrNull()

if (toolCall != null) {
    // Step 2: Provide the tool result
    val followUp = client.chatCompletion {
        model = "gpt-4"
        user("What's the weather in London?")
        assistantToolCalls(assistantMsg.toolCalls!!)
        toolResult(toolCall.id, """{"temperature": 18, "condition": "cloudy"}""")
    }
    println(followUp.choices.first().message.textContent)
}
```

### Tool calling with Conversation

```kotlin
val conversation = client.conversation("gpt-4")
val reply = conversation.send("What's the weather in London?")

val history = conversation.getHistory()
val lastMsg = history.last()
val call = lastMsg.toolCalls?.firstOrNull()

if (call != null) {
    // addToolResult sends the result and gets the follow-up in one call
    val answer = conversation.addToolResult(call.id, """{"temp": 18}""")
    println(answer)
}
```

## Structured Outputs

### JSON Mode

```kotlin
val response = client.chatCompletion {
    model = "gpt-4"
    system("Always respond in JSON.")
    user("List three colors.")
    jsonMode()
}
```

### JSON Schema (guaranteed structure)

```kotlin
val schema = Json.parseToJsonElement("""
    {
        "type": "object",
        "properties": {
            "name": { "type": "string" },
            "age": { "type": "integer" }
        },
        "required": ["name", "age"],
        "additionalProperties": false
    }
""".trimIndent())

val response = client.chatCompletion {
    model = "gpt-4o"
    system("Extract person information.")
    user("John Smith is 35 years old.")
    structuredOutput("person", schema)
}
```

## Vision / Multi-modal

Send text and images together using `userWithImages`:

```kotlin
val response = client.chatCompletion {
    model = "gpt-4o"
    userWithImages(
        "What do you see in this image?",
        listOf("https://example.com/photo.png"),
        detail = "high"  // "auto", "low", or "high"
    )
}
println(response.choices.first().message.textContent)
```

Multiple images:

```kotlin
client.chatCompletion {
    model = "gpt-4o"
    userWithImages(
        "Compare these two images.",
        listOf("https://example.com/a.png", "https://example.com/b.png")
    )
}
```

## Provider Setup

### OpenAI

```kotlin
val client = KompletionClient.openai("api-key")
```

### Ollama (local)

```kotlin
val client = KompletionClient.ollama()                          // default localhost:11434
val client = KompletionClient.ollama("http://192.168.1.5:11434/v1")  // custom host
```

### OpenRouter

```kotlin
val client = KompletionClient.openRouter("api-key")
```

### Custom provider

```kotlin
val client = KompletionClient.custom(
    baseUrl = "https://api.example.com/v1",
    apiKey = "your-key"
)
```

### Custom headers and timeout

```kotlin
val client = KompletionClient(
    KompletionConfig.openai("api-key").copy(
        timeout = 120_000,
        headers = mapOf("X-Request-Id" to "abc-123")
    )
)
```

## Error Handling

All Kompletions errors extend `KompletionException`:

```kotlin
try {
    val response = client.chat(request)
} catch (e: KompletionApiException) {
    // HTTP error from the API (e.g., 401, 429, 500)
    println("HTTP ${e.statusCode}: ${e.responseBody}")
} catch (e: KompletionException) {
    // Other Kompletions errors (e.g., no choices returned)
    println("Error: ${e.message}")
}
```

Conversation methods automatically roll back the user message from history on failure, keeping the conversation state clean.

## API Reference

### ChatRequest parameters (DSL)

| DSL Property        | Wire Name            | Type              | Description                        |
|---------------------|----------------------|-------------------|------------------------------------|
| `model`             | `model`              | `String`          | Model name (required)              |
| *(built via DSL)*   | `messages`           | `List<Message>`   | Conversation messages (required)   |
| `temperature`       | `temperature`        | `Double?`         | Randomness (0.0-2.0)              |
| `maxTokens`         | `max_tokens`         | `Int?`            | Maximum response length            |
| `topP`              | `top_p`              | `Double?`         | Nucleus sampling                   |
| `frequencyPenalty`  | `frequency_penalty`  | `Double?`         | Reduce repetition                  |
| `presencePenalty`   | `presence_penalty`   | `Double?`         | Encourage new topics               |
| `stop`              | `stop`               | `List<String>?`   | Stop sequences                     |
| `n`                 | `n`                  | `Int?`            | Number of completions              |
| `endUser`           | `user`               | `String?`         | End-user identifier                |
| `responseFormat`    | `response_format`    | `ResponseFormat?` | Response format (text/json/schema) |
| `tools`             | `tools`              | `List<Tool>?`     | Function calling tools             |
| `toolChoice`        | `tool_choice`        | `String?`         | Tool selection strategy            |
| `seed`              | `seed`               | `Int?`            | Seed for reproducibility           |
| `streamOptions`     | `stream_options`     | `StreamOptions?`  | Stream config (include_usage)      |
| `parallelToolCalls` | `parallel_tool_calls`| `Boolean?`        | Allow parallel tool calls          |

### DSL helper methods

| Method                                       | Description                                        |
|----------------------------------------------|----------------------------------------------------|
| `system(content)`                            | Add a system message                               |
| `user(content)`                              | Add a user message                                 |
| `assistant(content)`                         | Add an assistant message                           |
| `userWithImages(text, imageUrls, detail?)`   | Add a user message with text + images              |
| `tool(name, description?, parameters?)`      | Define a tool the model can call                   |
| `toolResult(toolCallId, content)`            | Add a tool result message                          |
| `assistantToolCalls(toolCalls)`              | Replay an assistant tool-call turn                 |
| `jsonMode()`                                 | Enable JSON object response format                 |
| `structuredOutput(name, schema, strict?)`    | Enable JSON Schema response format                 |
| `messages(history)`                          | Prepopulate from existing message history          |

### Message structure

```
Message
├── role: String
├── content: Content?          // String or multi-part (text + images)
│   ├── Content.Text(text)     // serializes as JSON string
│   └── Content.Parts(parts)   // serializes as JSON array
│       ├── ContentPart.TextPart(text)
│       └── ContentPart.ImagePart(imageUrl)
├── name: String?
├── toolCalls: List<ToolCall>? // present on assistant tool-call messages
│   ├── id: String
│   ├── type: String
│   └── function: ToolCallFunction(name, arguments)
└── toolCallId: String?        // present on tool-result messages
```

Use `message.textContent` to get the text content as a `String?` regardless of content type.

### ChatResponse structure

```
ChatResponse
├── id: String
├── object: String
├── created: Long
├── model: String
├── choices: List<Choice>
│   ├── index: Int
│   ├── message: Message
│   └── finishReason: String?
└── usage: Usage?
    ├── promptTokens: Int
    ├── completionTokens: Int
    └── totalTokens: Int
```

### Conversation API

| Method                              | Returns        | Description                                        |
|-------------------------------------|----------------|----------------------------------------------------|
| `send(message)`                     | `String`       | Send message, get assistant text (or `""`)         |
| `sendFull(message)`                 | `ChatResponse` | Send message, get full API response                |
| `addToolResult(toolCallId, content)`| `String`       | Add tool result, send, get follow-up text          |
| `addToolResultFull(toolCallId, content)` | `ChatResponse` | Add tool result, send, get full response      |
| `addMessage(role, content?, name?)` | —              | Inject a message without an API call               |
| `addMessage(message)`               | —              | Inject a pre-built Message without an API call     |
| `getHistory()`                      | `List<Message>`| Snapshot of current message history                |
| `clearHistory()`                    | —              | Remove all messages                                |

## License

[MIT](LICENSE)
