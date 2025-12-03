# SIRIO MCP Server

A Model Context Protocol (MCP) server implementation that exposes powerful Petri Net modeling and analysis capabilities through the SIRIO/OrisTool library. This server enables AI assistants (like Claude, Gemini) to create, manipulate, and analyze Generalized Stochastic Petri Nets (GSPNs) through a standardized tool-calling interface.

## Purpose

This project bridges the gap between AI-powered development tools and formal modeling techniques by:

- **Enabling AI-Driven Petri Net Modeling**: AI assistants can programmatically create and manipulate Petri Nets through natural language interactions
- **Formal Analysis Integration**: Provides access to steady-state and transient analysis capabilities for stochastic systems
- **Educational Tool**: Helps students and researchers learn Petri Net concepts through interactive AI-guided modeling
- **Rapid Prototyping**: Allows quick creation and validation of concurrent system models without manual tool usage

The server uses the [SIRIO/OrisTool](https://github.com/oris-tool/sirio) library, a powerful Java framework for modeling and analyzing stochastic systems.

## Requirements

### Software Prerequisites

- **Java Development Kit (JDK) 21 or higher** (tested with JDK 25)
- **Apache Maven 3.6+** (included via Maven Wrapper)
- **Visual Studio Code** with GitHub Copilot extension
- **Windows PowerShell** (for Windows users) or equivalent shell

### Maven Dependencies

The project uses Spring Boot 3.5.7 and includes:
- `spring-ai-starter-mcp-server` - MCP protocol implementation
- `sirio` (org.oris-tool) v2.0.5 - Petri Net modeling and analysis
- `jackson-databind` - JSON serialization for analysis results

## Building the Project

### 1. Clone the Repository

```powershell
git clone https://github.com/NicMen99/SIRIO_mcp_server.git
cd SIRIO_mcp_server
```

### 2. Build with Maven

Use the Maven Wrapper to build the project:

```powershell
.\mvnw.cmd clean package
```

This command will:
- Compile the Java source files
- Run tests (if available)
- Package the application into an executable JAR file
- Output the JAR to `target/sirio_mcp_server-0.0.1-SNAPSHOT.jar`

### 3. Verify the Build

Check that the JAR file was created successfully:

```powershell
ls target\sirio_mcp_server-0.0.1-SNAPSHOT.jar
```

## Setting Up MCP in VS Code

To enable GitHub Copilot to communicate with the SIRIO MCP server, you need to configure the MCP connection using STDIO transport.

### 1. Create MCP Configuration Directory

Create the `.vscode` directory in your project root if it doesn't exist:

```powershell
mkdir .vscode -Force
```

### 2. Create mcp.json Configuration

Create a file named `mcp.json` in the `.vscode` directory with the following content:

```json
{
  "servers": {
    "sirio": {
      "command": "C:\\Program Files\\Java\\jdk-25\\bin\\java.exe",  // or wherever you have the java executable for the desired version
      "args": [
        "-jar",
        "C:\\PATH-TO-YOUR-PROJECT\\target\\sirio_mcp_server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

**Important Configuration Notes:**

- **Absolute Paths**: Use the full absolute path to both the Java executable and the JAR file
- **Escaped Backslashes**: Use double backslashes (`\\`) in Windows paths
- **Java Location**: Update the `command` path to match your JDK installation
- **JAR Location**: Update the JAR path to match your project location
- **Server Name**: The key `"sirio"` can be changed, but it identifies this server in VS Code

### 3. Debug Mode (Optional)

To enable remote debugging, uncomment the debug args in `mcp.json`:

```json
{
  "servers": {
    "sirio": {
      "command": "C:\\Program Files\\Java\\jdk-25\\bin\\java.exe",  // or wherever you have the java executable for the desired version
      "args": [
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005",
        "-jar",
        "C:\\PATH-TO-YOUR-PROJECT\\target\\sirio_mcp_server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

This will allow you to attach a debugger on port 5005.

### 4. Restart VS Code

After creating the configuration:
1. Reload VS Code window (`Ctrl+Shift+P` → "Developer: Reload Window")
2. The SIRIO server will now be available to GitHub Copilot
3. You can verify the connection in the MCP status indicator in VS Code

## How the Code Works

### Architecture Overview

The server follows a Spring Boot application architecture with MCP tool registration. The main components are:

```
SirioMcpServerApplication.java  ← Entry point & tool registration
         ↓
    SirioService.java           ← Core tool implementations (@Service)
         ↓
   PetriNetUtils.java           ← Helper utilities for Petri Net operations
```

### 1. SirioMcpServerApplication.java

**Purpose**: Bootstrap the Spring Boot application and register MCP tools.

```java
@SpringBootApplication
public class SirioMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SirioMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider SIRIOTools(SirioService service) {
        return MethodToolCallbackProvider.builder().toolObjects(service).build();
    }
}
```

**Key Responsibilities:**

- **Spring Boot Entry Point**: The `main` method launches the Spring application context
- **Tool Registration**: The `@Bean` method creates a `ToolCallbackProvider` that scans `SirioService` for methods annotated with `@Tool`
- **MCP Bridge**: The `MethodToolCallbackProvider` automatically exposes annotated methods as MCP tools that can be called via STDIO

The application runs as a **non-web** Spring Boot application that communicates through standard input/output streams, making it compatible with the MCP protocol.

### 2. SirioService.java

**Purpose**: Provides all Petri Net manipulation and analysis tools as MCP-callable methods.

#### State Management

The service maintains two core state objects:

```java
private PetriNet petriNet = null;   // The Petri Net structure
private Marking marking = null;     // Current token distribution
```

These represent the **in-memory model** that AI assistants manipulate through tool calls.

#### Tool Categories

**A. Net Creation & Structure**

- `create()` - Initialize an empty Petri Net
- `add_places(List<String>)` - Add places to the net
- `remove_places(List<String>)` - Remove places and connected arcs
- `add_transitions(List<String>)` - Add transitions
- `remove_transitions(List<String>)` - Remove transitions and connected arcs

**B. Transition Types**

Transitions define the timing behavior of the system:

- `add_IMM(String)` - **Immediate transition** (fires instantly, zero delay)
- `add_DET(String, double, Double, Double)` - **Deterministic transition** (fixed delay)
- `add_EXP(String, double, Double, Double)` - **Exponential transition** (stochastic delay with rate λ)
- `add_UNI(String, double, double, Double)` - **Uniform transition** (random delay in [EFT, LFT])

**Optional parameters:**
- `clockRate` - Scales the timing expression
- `weight` - Used for conflict resolution in stochastic transitions

**C. Arcs & Connections**

- `add_precondition(String place, String transition)` - Input arc (place → transition)
- `add_postcondition(String transition, String place)` - Output arc (transition → place)
- `add_inhibitor_arc(String place, String transition)` - Inhibitor arc (prevents firing when place has tokens)
- `remove_preconditions()`, `remove_postconditions()`, `remove_inhibitor_arc()` - Remove connections

**D. Marking & Token Game**

- `add_tokens(String place, int num)` - Add tokens to a place
- `remove_tokens(String place, int num)` - Remove tokens from a place
- `reset_marking()` - Clear all tokens
- `get_enabled_transitions()` - List currently firable transitions
- `fire_transition(String)` - Execute a transition (token game step)

**E. Advanced Features**

- `add_enabling_function(String condition, String transition)` - Add guard condition (e.g., `"place1 == 2"`)
- `set_transition_priority(String, int)` - Set firing priority for conflict resolution
- `get_transition_features(String)` - Inspect transition parameters via reflection

**F. Analysis Tools**

- `execute_steady_state_analysis()` - Compute steady-state probabilities for GSPNs
  - Returns: `Map<Marking, Double>` - probability of each reachable marking
  - Requirements: Only immediate or exponential transitions

- `execute_transient_analysis(List<Double> timePoints)` - Compute time-dependent probabilities
  - Returns: `Map<Double, Map<Marking, Double>>` - probabilities at each time point
  - Example: `[0.0, 0.5, 1.0, 2.0]` analyzes the system at t=0, 0.5, 1, 2 seconds

**G. Utility Methods**

- `show_net()` - Returns string representation of current net and marking

#### Tool Annotation Pattern

Each tool follows this pattern:

```java
@Tool(
    name = "tool_name",  // MCP identifier
    description = "Clear description for AI understanding"
)
public ReturnType methodName(
    @ToolParam(description = "Parameter description") Type paramName
) {
    PetriNetUtils.checkNetAndMarking(petriNet, marking);  // Validate state
    // ... implementation
}
```

The `@Tool` and `@ToolParam` annotations enable automatic discovery and validation by the MCP framework.

### 3. PetriNetUtils.java

**Purpose**: Provide reusable utility methods for common Petri Net operations.

```java
public final class PetriNetUtils {
    // Find a place by name, throw exception if not found
    public static Place findPlaceByName(PetriNet pn, String name)
    
    // Find a transition by name, throw exception if not found
    public static Transition findTransitionByName(PetriNet pn, String name)
    
    // Find or create a transition (idempotent operation)
    public static Transition findOrCreateTransitionByName(PetriNet pn, String name)
    
    // Validate that net and marking are initialized
    public static void checkNetAndMarking(PetriNet pn, Marking marking)
}
```

**Design Rationale:**

- **Null Safety**: `checkNetAndMarking()` prevents operations on uninitialized state
- **Error Handling**: `find*` methods throw `IllegalArgumentException` with clear messages
- **Idempotency**: `findOrCreateTransitionByName()` prevents duplicate transitions when adding features
- **Reusability**: Reduces code duplication across `SirioService` methods

**Common Usage Pattern:**

```java
// In SirioService methods:
PetriNetUtils.checkNetAndMarking(petriNet, marking);  // Ensure initialized
Transition t = PetriNetUtils.findOrCreateTransitionByName(petriNet, "t1");
Place p = PetriNetUtils.findPlaceByName(petriNet, "p1");
```

## Example Usage with AI

Once configured, you can interact with GitHub Copilot in VS Code to create and analyze Petri Nets:

```
You: "Create a producer-consumer Petri Net with one producer, one consumer, and a buffer of size 3"

Copilot: [Uses SIRIO tools to create the model]
- Calls create()
- Calls add_places(["producer_ready", "buffer", "consumer_ready"])
- Calls add_transitions(["produce", "consume"])
- Adds preconditions and postconditions
- Sets initial marking with add_tokens()

You: "Run a transient analysis from 0 to 5 seconds with 0.5 second intervals"

Copilot: [Executes analysis]
- Calls execute_transient_analysis([0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0])
- Returns probability distributions at each time point
```

## Project Structure (Partial)

```
SIRIO_mcp_server/
├── .vscode/
│   └── mcp.json                          # MCP server configuration
├── src/
│   └── main/
│       ├── java/
│       │   └── org/swam/
│       │       ├── sirio_mcp_server/
│       │       │   ├── SirioMcpServerApplication.java   # Entry point
│       │       │   └── SirioService.java                # MCP tools
│       │       └── pn_utils/
│       │           └── PetriNetUtils.java               # Utilities
│       └── resources/
│           └── application.properties    # Spring configuration
├── target/
│   └── sirio_mcp_server-0.0.1-SNAPSHOT.jar   # Executable JAR
├── pom.xml                               # Maven configuration
└── README.md                             # This file
```

## Contributing

Contributions are welcome! When adding new tools:

1. Add methods to `SirioService` with `@Tool` and `@ToolParam` annotations
2. Follow the existing naming convention (lowercase_with_underscores)
3. Always validate state with `PetriNetUtils.checkNetAndMarking()`
4. Provide clear descriptions for AI understanding
5. Update this README with new tool documentation

## License

[Specify your license here]

## Acknowledgments

- Built with [SIRIO/OrisTool](https://github.com/oris-tool/sirio) - A powerful library for stochastic Petri Net analysis
- Uses [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/) for MCP protocol implementation
- Inspired by the Model Context Protocol initiative for AI-tool integration

## Troubleshooting

**Issue**: Server not appearing in VS Code MCP list
- Verify `mcp.json` is in `.vscode/` directory
- Check that all paths use absolute paths with escaped backslashes
- Reload VS Code window
- Check VS Code developer console for errors

**Issue**: "Petri net and marking must be created first"
- Always call the `create` tool before any other operations
- The server maintains state in memory - restart if state is corrupted

**Issue**: Build fails with compilation errors
- Ensure JDK 21+ is installed
- Run `.\mvnw.cmd clean install` to refresh dependencies
- Check that JAVA_HOME environment variable is set correctly

**Issue**: Analysis methods return empty results
- Verify that transitions are properly configured (IMM or EXP for GSPN analysis)
- Ensure the net has at least one enabled transition
- Check that initial marking has tokens in appropriate places

## Contact

For questions or issues, please open an issue on the [GitHub repository](https://github.com/NicMen99/SIRIO_mcp_server).
