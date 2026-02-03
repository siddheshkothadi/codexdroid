# codexdroid

codexdroid is a modern, lightweight Android client for the **Codex** server. It provides a mobile interface to interact with your Codex instance, allowing you to manage connections and continue your development sessions on the go.

## Features

- **Connection Management**: Easily switch between multiple Codex server instances.
- **Threads & Sessions**: View, resume, and start new conversation threads.
- **Rich Message Support**: 
    - **Reasoning**: View the agent's internal thought process.
    - **Command Execution**: Monitor terminal commands and their outputs.
    - **MCP Tool Calls**: Track Model Context Protocol tool interactions.
    - **File Changes**: Review patches and file modifications suggested by the agent.
- **Real-time Updates**: Interactive UI with typing indicators and status updates.

## Getting Started

### Connections

To connect codexdroid to your Codex server:
1. Open the app and navigate to the **Setup** screen.
2. Provide a **Name** for the connection (e.g., "My Home Server").
3. Enter the **Base URL** where your Codex instance is reachable.
4. (Optional) Provide the **Secret** if your server requires the `x-codex-secret` header for authentication.

### Threads

Once connected, you will see a list of your recent threads. You can:
- **Start a new thread** to begin a fresh task.
- **Resume an existing thread** to pick up where you left off.
- codexdroid uses the Codex RPC protocol to sync messages, tool calls, and system status in real-time.

## Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Dependency Injection**: Dagger Hilt
- **Database**: Room (Local storage for connections and thread metadata)
- **Networking**: Ktor with OkHttp engine
- **Serialization**: Kotlinx Serialization (JSON)
- **Markdown**: `compose-markdown` for rich message rendering
