# Installation

:warning: ECA is already automatically downloaded in all editor plugins, so you don't need to download it manually, even so, if you want that, follow below:

Eca is written in Clojure and compiled into a native binary via graalvm. You can download the [native binaries from Github Releases](https://github.com/editor-code-assistant/eca/releases) or use one of the methods below:

## Editor (recommended)

ECA is already downloaded automatically by your ECA editor plugin, so you just need to install the plugin for your editor:

- [Emacs](https://github.com/editor-code-assistant/eca-emacs)
- [VsCode](https://github.com/editor-code-assistant/eca-vscode)
- [Vim](https://github.com/editor-code-assistant/eca-nvim)
- Intellij: Planned, help welcome

## Script (recommended if manual installing)

Stable release:

```bash
bash <(curl -s https://raw.githubusercontent.com/editor-code-assistant/eca/master/install)
```

Or if facing issues with command above:
```bash
curl -s https://raw.githubusercontent.com/editor-code-assistant/eca/master/install | sudo bash
```

nightly build:

```bash
bash <(curl -s https://raw.githubusercontent.com/editor-code-assistant/eca/master/install) --version nightly --dir ~/
```

## Homebrew (MacOS and Linux)

We have a custom tap using the native compiled binaries for users that use homebrew:

```bash
brew install editor-code-assistant/brew/eca
```

