# Configuration

## Ways to configure

Check all available configs [here](../src/eca/config.clj#L17).
There are 3 ways to configure ECA following this order of priority:

### InitializationOptions (convenient for editors)

Client editors can pass custom settings when sending the `initialize` request via the `initializationOptions` object:

```javascript
"initializationOptions": {
  "chatBehavior": "chat"
}
```

### Local Config file (convenient for users)

`.eca/config.json`
```javascript
{
  "chatBehavior": "chat"
}
```

### Global config file (convenient for users and multiple projects)

`~/.config/eca/config.json`
```javascript
{
  "chatBehavior": "chat"
}
```

### Env Var

Via env var during server process spawn:

```bash
ECA_CONFIG='{"myConfig": "my_value"}' eca server
```

## Rules

Rules are contexts that are passed to the LLM during a prompt.
There are 2 possible ways following this order of priority:

### Project file

A `.eca/rules` folder from the workspace root containing `.md` files with the rules.

`.eca/rules/talk_funny.md`
```markdown
--- 
name: Funny rule
---

- Talk funny like Mickey!
```

### Config 

Just add to your config the `:rules` pointing to `.md` files that will be searched from the workspace root if not an absolute path:

```javascript
{
  "rules": [{"path": "my-rule.md"}]
}
```

## MCP

For MCP servers configuration, use the `mcpServers` config, example:

`.eca/config.json`
```javascript
{
  "mcpServers": {
    "filesystem": {
        "command": "npx",
        "args": ["-y", "@modelcontextprotocol/server-filesystem", "/home/greg/dev/eca-emacs"
      ]
    }
  }
}
```

## All configs

### Schema

```typescript
interface Config {
    openaiApiKey?: string;
    anthropicApiKey?: string;
    ollama?: {
        host: string;
        port: string;
        useTools: boolean;
    }
    chat?: {
        welcomeMessage: string;
    }
    index?: {
        ignoreFiles: [{
            type: string;
        }]
    }
}
```

### Default values

```javascript
{
  "openaiApiKey" : null,
  "anthropicApiKey" : null,
  "rules" : [ ],
  "nativeTools": {"filesystem": {"enabled": true}
                  "shell": {"enabled": true
                            "excludeCommands": []}},
  "mcpTimeoutSeconds" : 10,
  "mcpServers" : [ ],
  "ollama" : {
    "host" : "http://localhost",
    "port" : 11434,
    "useTools": false
  },
  "chat" : {
    "welcomeMessage" : "Welcome to ECA! What you have in mind?\n\n"
  },
  "index" : {
    "ignoreFiles" : [ {
      "type" : "gitignore"
    } ]
  }
}
```
