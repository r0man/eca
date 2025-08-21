# Models

The models capabilities and configurations are retrieved from [models.dev](https://models.dev) API.

## Built-in providers and capabilities

| model               | tools (MCP) | reasoning / thinking | prompt caching | web_search |
|---------------------|-------------|----------------------|----------------|------------|
| OpenAI              | √           | √                    | √              | √          |
| Anthropic           | √           | √                    | √              | √          |
| Github Copilot      | √           | √                    | √              | X          |
| Ollama local models | √           | √                    | X              | X          |

### Adding and Configuring Models

#### Setting up your first model

To start using ECA, you need to configure at least one model with your API key. Here's how to set up a model:

1. **Choose your model**: Pick from [OpenAI](#openai), [Anthropic](#anthropic), or [Ollama](#ollama) models
2. **Set your API key**: Create a configuration file with your credentials
3. **Start using ECA**: The model will be available in your editor

#### Setting up API keys

Create a configuration file at `.eca/config.json` in your project root or at `~/.config/eca/config.json` globally:

```javascript
{
  "providers": {
      "openai": {"key": "your-openai-api-key-here"},
      "anthropic": {"key": "your-anthropic-api-key-here"}
  }
}
```

**Environment Variables**: You can also set API keys using environment variables:
- `OPENAI_API_KEY` for OpenAI
- `ANTHROPIC_API_KEY` for Anthropic

#### Adding new models

You can add new models or override existing ones in your configuration:

```javascript
{
  "providers": {
      "openai": {"key": "your-openai-api-key-here"}
  },
  "models": {
    "gpt-5": {},
    "claude-3-5-sonnet-20241022": {}
  }
}
```

#### Customizing model behavior

You can customize model parameters like temperature, reasoning effort, etc.:

```javascript
{
  "providers": {
      "openai": {"key": "your-openai-api-key-here"}
  },
  "models": {
    "gpt-5": {
      "extraPayload": {
        "temperature": 0.7,
        "reasoning_effort": "high",
        "max_tokens": 4000
      }
    }
  }
}
```

This config will be merged with current default used by ECA.

## Custom model providers

ECA allows you to configure custom LLM providers that follow API schemas similar to OpenAI or Anthropic. This is useful when you want to use:

- Self-hosted LLM servers (like LiteLLM)
- Custom company LLM endpoints
- Additional cloud providers not natively supported

### API Types for Custom Providers

When configuring custom providers, choose the appropriate API type:

- **`anthropic`**: Anthropic's native API for Claude models.
- **`openai-responses`**: OpenAI's new responses API endpoint (`/v1/responses`). Best for OpenAI models with enhanced features like reasoning and web search.
- **`openai-chat`**: Standard OpenAI Chat Completions API (`/v1/chat/completions`). Use this for most third-party providers:
    - OpenRouter
    - DeepSeek
    - Together AI
    - Groq
    - Local LiteLLM servers
    - Any OpenAI-compatible provider

Most third-party providers use the `openai-chat` API for compatibility with existing tools and libraries.

### Setting up a custom provider

It's possible to configure ECA to be aware of custom LLM providers if they follow a API schema similar to currently supported ones (openai-responses, openai-chat or anthropic), example for a custom hosted litellm server:

Example:

`~/.config/eca/config.json`
```javascript
{
  "customProviders": {
    "my-company": {
       "api": "openai-chat",
       "urlEnv": "MY_COMPANY_API_URL", // or "url"
       "keyEnv": "MY_COMPANY_API_KEY", // or "key"
       "models": ["gpt-5", "deepseek-r1"]
    }
  }
}
```

### Custom provider configuration options

| Option | Type | Description | Required |
|--------|------|-------------|----------|
| `api` | string | The API schema to use (`"openai-responses"`, `"openai-chat"`, or `"anthropic"`) | Yes |
| `urlEnv` | string | Environment variable name containing the API URL | Yes* |
| `url` | string | Direct API URL (use instead of `urlEnv`) | Yes* |
| `keyEnv` | string | Environment variable name containing the API key | Yes* |
| `key` | string | Direct API key (use instead of `keyEnv`) | Yes* |
| `models` | array | List of available model names | Yes |
| `completionUrlRelativePath` | string | Custom endpoint path for completions | No |

_* Either the `url` or `urlEnv` option is required, and either the `key` or `keyEnv` option is required._


After configuring custom providers, the models will be available as `provider/model` (e.g., `openrouter/anthropic/claude-3.5-sonnet`, `deepseek/deepseek-chat`).

### Providers setup

=== "Github Copilot"
    
    1. Login to Github copilot via the chat command `/login github-copilot`.
    2. Authenticate in Github with the given code.
    3. Type anything in the chat to continue and done!
    
    _Tip: check [Your Copilot plan](https://github.com/settings/copilot/features) to enable models to your account._

=== "LiteLLM"

    ```javascript
    {
      "customProviders": {
        "litellm": {
        "api": "openai-responses",
        "url": "https://litellm.my-company.com",
        "key": "your-api-key",
        "models": ["gpt-5", "claude-3-sonnet-20240229", "llama-3-70b"]
        }
      }
    }
    ```

=== "OpenRouter"

    [OpenRouter](https://openrouter.ai) provides access to many models through a unified API:

    ```javascript
    {
      "customProviders": {
        "openrouter": {
          "api": "openai-chat",
          "url": "https://openrouter.ai/api/v1",
          "keyEnv": "OPENROUTER_API_KEY",
          "models": ["anthropic/claude-3.5-sonnet", "openai/gpt-4-turbo", "meta-llama/llama-3.1-405b"]
        }
      }
    }
    ```

=== "DeepSeek"

    [DeepSeek](https://deepseek.com) offers powerful reasoning and coding models:

    ```javascript
    {
      "customProviders": {
        "deepseek": {
          "api": "openai-chat",
          "url": "https://api.deepseek.com",
          "keyEnv": "DEEPSEEK_API_KEY",
          "models": ["deepseek-chat", "deepseek-coder", "deepseek-reasoner"]
        }
      }
    }
    ```
