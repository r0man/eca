# Configuration

## Ways to configure

Check all available configs [here](../src/eca/config.clj#L17).
There are 3 ways to configure ECA following this order of priority:

### InitializationOptions (convenient for editors)

Client editors can pass custom settings when sending the `initialize` request via the `initializationOptions` object:

```javascript
"initializationOptions": {
  "chatBehavior": "agent"
}
```

### Local Config file (convenient for users)

`.eca/config.json`
```javascript
{
  "chatBehavior": "agent"
}
```

### Global config file (convenient for users and multiple projects)

`~/.config/eca/config.json`
```javascript
{
  "chatBehavior": "agent"
}
```

### Env Var

Via env var during server process spawn:

```bash
ECA_CONFIG='{"myConfig": "my_value"}' eca server
```

## Models

For models configuration check the [dedicated models section](./models.md#adding-and-configuring-models).

## Rules

Rules are contexts that are passed to the LLM during a prompt and are useful to tune prompts or LLM behavior.
Rules are Multi-Document context files (`.mdc`) and the following metadata is supported:

- `description`: a description used by LLM to decide whether to include this rule in context, absent means always include this rule.
- `globs`: list of globs separated by `,`. When present the rule will be applied only when files mentioned matches those globs.

There are 3 possible ways to configure rules following this order of priority:

### Project file

A `.eca/rules` folder from the workspace root containing `.mdc` files with the rules.

`.eca/rules/talk_funny.mdc`
```markdown
--- 
description: Use when responding anything
---

- Talk funny like Mickey!
```

### Global file

A `$XDG_CONFIG_HOME/eca/rules` or `~/.config/eca/rules` folder containing `.mdc` files with the rules.

`~/.config/eca/rules/talk_funny.mdc`
```markdown
--- 
description: Use when responding anything
---

- Talk funny like Mickey!
```

### Config

Just add to your config the `:rules` pointing to `.mdc` files that will be searched from the workspace root if not an absolute path:

```javascript
{
  "rules": [{"path": "my-rule.mdc"}]
}
```

## MCP

For MCP servers configuration, use the `mcpServers` config, example:

`.eca/config.json`
```javascript
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
    }
  }
}
```

## Custom command prompts

You can configure custom command prompts for project, global or via `commands` config pointing to the path of the commands.
Prompts can use variables like `$ARGS`, `$ARG1`, `ARG2`, to replace in the prompt during command call.

### Local custom commands

A `.eca/commands` folder from the workspace root containing `.md` files with the custom prompt.

`.eca/commands/check-performance.md`
```markdown
Check for performance issues in $ARG1 and optimize if needed.
```

### Global custom commands

A `$XDG_CONFIG_HOME/eca/commands` or `~/.config/eca/commands` folder containing `.md` files with the custom command prompt.

`~/.config/eca/commands/check-performance.mdc`
```markdown
Check for performance issues in $ARG1 and optimize if needed.
```

### Config

Just add to your config the `commands` pointing to `.md` files that will be searched from the workspace root if not an absolute path:

```javascript
{
  "commands": [{"path": "my-custom-prompt.md"}]
}
```

## All configs

### Schema

```typescript
interface Config {
    openaiApiKey?: string;
    openaiApiUrl?: string;
    anthropicApiKey?: string;
    anthropicApiUrl?: string;
    ollamaApiUrl: string;
    rules: [{path: string;}];
    commands: [{path: string;}];
    systemPromptTemplateFile?: string;
    nativeTools: {
        filesystem: {enabled: boolean};
        shell: {enabled: boolean,
                excludeCommands: string[]};
    };
    disabledTools: string[],
    toolCall?: {
      manualApproval?: boolean | string[], // manual approve all tools or the specified tools
    };
    mcpTimeoutSeconds: number;
    mcpServers: {[key: string]: {
        command: string;
        args?: string[];
        disabled?: boolean;
    }};
    customProviders: {[key: string]: {
        api: 'openai-responses' | 'openai-chat' | 'anthropic';
        models: string[];
        defaultModel?: string;
        url?: string;
        urlEnv?: string;
        completionUrlRelativePath?: string;
        key?: string;
        keyEnv?: string;
    }};
    models: {[key: string]: {
      extraPayload: {[key: string]: any}
    }};
    ollama?: {
        useTools: boolean;
        think: boolean;
    };
    chat?: {
        welcomeMessage: string;
    };
    agentFileRelativePath: string;
    index?: {
        ignoreFiles: [{
            type: string;
        }];
    };
}
```

### Custom Provider API Types

When configuring custom providers, choose the appropriate API type:

- **`openai-responses`**: OpenAI's new responses API endpoint (`/v1/responses`). Best for OpenAI models with enhanced features like reasoning and web search.

### Default values

```javascript
{
  "openaiApiKey" : null,
  "openaiApiUrl" : null,
  "anthropicApiKey" : null,
  "anthropicApiUrl" : null,
  "ollamaApiUrl": "http://localhost:11434"
  "rules" : [],
  "commands" : [],
  "nativeTools": {"filesystem": {"enabled": true},
                  "shell": {"enabled": true,
                            "excludeCommands": []}},
  "disabledTools": [],
  "toolCall": {
    "manualApproval": null,
  },
  "mcpTimeoutSeconds" : 60,
  "mcpServers" : {},
  "customProviders": {},
  "models": {},
  "ollama" : {
    "useTools": true,
    "think": true
  },
  "chat" : {
    "welcomeMessage" : "Welcome to ECA!\n\nType '/' for commands\n\n"
  },
  "agentFileRelativePath": "AGENT.md"
  "index" : {
    "ignoreFiles" : [ {
      "type" : "gitignore"
    } ]
  }
}
```
