# ja-netfilter

**Version:** 2026.1.4  
**Author:** LimonTH  
**License:** GNU General Public License v3.0

ja-netfilter is a powerful Java agent that enables runtime bytecode filtering and transformation. It allows you to hook into class loading and modify bytecode dynamically without restarting the JVM.

## Features

- **Dynamic Class Transformation** - Hook into class loading and transform bytecode at runtime
- **Plugin System** - Extensible architecture with hot-reloadable plugins
- **Multiple Rule Types** - Support for prefix, suffix, keyword, and regex-based filtering
- **Attach Mode** - Attach to running JVMs without restart
- **Javaagent Mode** - Use as `-javaagent` for startup-time instrumentation
- **Hot Reload** - Reload plugins without JVM restart
- **Dry-run Mode** - Test transformations without applying changes
- **Built-in Transformer Registry** - Enable/disable transformers at runtime

## Quick Start

### Prerequisites

- Java 8 or higher
- Gradle 7.0+ (for building)

### Building

```bash
./gradlew build
```

The agent JAR will be created in `build/libs/` directory.

### Usage

#### As Java Agent

```bash
java -javaagent:ja-netfilter.jar -jar your-application.jar
```

#### Attach to Running JVM

```bash
java -jar ja-netfilter.jar
```

This will display a list of running JVMs and allow you to select one to attach to.

#### Command Line Options

- `--version` or `-v` - Display version information

## Configuration

ja-netfilter uses a configuration file-based system for defining filtering rules.

### Configuration File Format

```ini
[section_name]
PREFIX,com.example.
SUFFIX,.class
KEYWORD,important
EQUAL,exact_match
REGEXP,^.*\.test\..*$
```

### Rule Types

| Type | Description | Example |
|------|-------------|---------|
| `PREFIX` | Case-sensitive prefix matching | `PREFIX,com.example.` |
| `PREFIX_IC` | Case-insensitive prefix matching | `PREFIX_IC,com.example.` |
| `SUFFIX` | Case-sensitive suffix matching | `SUFFIX,.class` |
| `SUFFIX_IC` | Case-insensitive suffix matching | `SUFFIX_IC,.class` |
| `KEYWORD` | Case-sensitive keyword matching | `KEYWORD,important` |
| `KEYWORD_IC` | Case-insensitive keyword matching | `KEYWORD_IC,important` |
| `EQUAL` | Case-sensitive exact match | `EQUAL,com.example.MyClass` |
| `EQUAL_IC` | Case-insensitive exact match | `EQUAL_IC,com.example.MyClass` |
| `REGEXP` | Regular expression match | `REGEXP,^com\.example\..*` |

## Plugin Development

### Creating a Plugin

1. Create a class that implements `PluginEntry` interface
2. Add the plugin entry to your JAR manifest:
   ```
   JANF-Plugin-Entry: com.yourcompany.YourPlugin
   ```
3. Package your plugin as a JAR file
4. Place it in the `plugins/` directory

### Plugin Example

```java
package com.yourcompany;

import com.janetfilter.core.Environment;
import com.janetfilter.core.plugin.*;

public class YourPlugin implements PluginEntry {
    
    @Override
    public void init(Environment environment, PluginConfig config) {
        // Initialize your plugin
    }
    
    @Override
    public String getName() {
        return "Your Plugin Name";
    }
    
    @Override
    public String getAuthor() {
        return "Your Name";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Description of your plugin";
    }
    
    @Override
    public List<MyTransformer> getTransformers() {
        return List.of(new YourTransformer());
    }
}
```

### Creating a Transformer

Transformers implement the `MyTransformer` interface to hook into class transformation:

```java
public class YourTransformer implements MyTransformer {
    
    @Override
    public String getHookClassName() {
        // Return specific class name to hook, or null for global transformer
        return "com/example/TargetClass";
    }
    
    @Override
    public byte[] transform(String className, byte[] classBytes, int order) {
        // Transform the class bytecode here
        // Use ASM library or similar to modify bytecode
        return classBytes; // Return modified bytes
    }
}
```

### Transformer Lifecycle

For **global transformers** (where `getHookClassName()` returns `null`), you can hook into multiple stages:

1. `before()` - Called before transformation starts
2. `preTransform()` - Modify bytes before main transform
3. `transform()` - Main transformation logic
4. `postTransform()` - Modify bytes after main transform
5. `after()` - Called after transformation completes

## Directory Structure

```
ja-netfilter/
├── config/              # Configuration files
├── config-<app>/        # App-specific configurations
├── plugins/             # Plugin JAR files
├── plugins-<app>/       # App-specific plugins
├── logs/                # Log files
└── logs-<app>/          # App-specific logs
```

## Debugging

Set environment variables or system properties to enable debug output:

```bash
# Enable debug logging (levels: 1=DEBUG, 2=INFO, 3=WARN, 4=ERROR)
export JANF_DEBUG=1

# Configure output (bitmask: 1=console, 2=file, 4=with PID)
export JANF_OUTPUT=7
```

Or as JVM arguments:
```bash
java -Djanf.debug=1 -Djanf.output=7 -javaagent:ja-netfilter.jar
```

## Architecture

### Core Components

- **Launcher** - Entry point for both attach and javaagent modes
- **Environment** - Runtime context and configuration
- **Dispatcher** - Routes class transformations to registered transformers
- **PluginManager** - Loads and manages plugins
- **Initializer** - Sets up the agent environment and transformers

### Transformation Pipeline

```
Class Loading → Dispatcher → Transformer.before() → Transformer.preTransform()
             → Transformer.transform() → Transformer.postTransform() → Transformer.after()
             → Modified Class
```

## Advanced Features

### Dry-run Mode

Use `DryRunTransformer` to log transformations without applying them:

```java
DryRunTransformer dryRun = new DryRunTransformer(actualTransformer);
dispatcher.addTransformer(dryRun);
```

### Hot Reload

Enable automatic plugin reloading when files change:

```java
PluginHotReloader reloader = new PluginHotReloader(pluginManager, instrumentation);
reloader.start(pluginsDirectory);
```

### Built-in Transformer Registry

Register and manage transformers with the ability to enable/disable:

```java
BuiltinTransformerRegistry.register("my-transformer", transformer);
BuiltinTransformerRegistry.disable("my-transformer");
BuiltinTransformerRegistry.enable("my-transformer");
```

## Environment Detection

The agent can detect and log the runtime environment:
- Container (Docker)
- IntelliJ IDEA
- CI/CD systems

## Common Issues

### Classes Not Being Transformed

- Ensure your transformer is registered with the dispatcher
- Check that the class name format uses `/` not `.` (e.g., `com/example/MyClass`)
- Verify the transformer is active (not disabled)
- Enable debug logging to see transformation attempts

### Plugin Not Loading

- Ensure plugin JAR has `JANF-Plugin-Entry` in manifest
- Plugin class must implement `PluginEntry` interface
- Check logs directory for error messages
- Verify plugin is in the correct `plugins/` directory

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Original code by Neo Peng (pengzhile@gmail.com)
- Modifications and updates by LimonTH (2026)