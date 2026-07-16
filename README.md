# netbeans-golang

Go language support for Apache NetBeans, powered by [gopls](https://go.dev/gopls/) — the
official Go language server.

Apache NetBeans ships no Go support, and the existing community plugins predate both the
Language Server Protocol and modern Go tooling. This plugin wires NetBeans' built-in LSP
client to gopls, so Go files get the same language intelligence the Go team maintains for
every other editor.

## Features

- **Syntax highlighting** for `.go`, `go.mod`, and `go.sum` (TextMate grammars)
- **Code completion** with documentation
- **Hover** for types, functions, and docs
- **Go to declaration** (Ctrl+click / Ctrl+B)
- **Find usages**
- **Diagnostics** — compile errors and vet warnings as you type, with quick fixes
- **Formatting** — `gofmt` via Alt+Shift+F

Everything above comes from gopls, so behavior matches the Go tooling you already use.

## Requirements

- Apache NetBeans 24 or later
- JDK 17+ (NetBeans' own baseline)
- [Go](https://go.dev/dl/) and gopls installed:

```
go install golang.org/x/tools/gopls@latest
```

The plugin locates `gopls` in this order:

1. an explicit path you configure in the plugin settings
2. `gopls` on your `PATH`
3. `$GOBIN`
4. `$GOPATH/bin` (default `~/go/bin`, or `%USERPROFILE%\go\bin` on Windows)

If gopls cannot be found, the plugin shows a notification with the install command rather
than failing silently.

## Installing

Download the `.nbm` from [Releases](https://github.com/jamov-amd/netbeans-golang/releases),
then in NetBeans: **Tools > Plugins > Downloaded > Add Plugins…**, pick the file, and install.
The NBM is unsigned, so NetBeans will show a validation warning — that is expected.

## Usage

No NetBeans project type is needed. Open a folder containing your Go module (the **Favorites**
window works well), open a `.go` file, and gopls starts automatically. gopls locates your
`go.mod` on its own.

## Building from source

```
mvn package
```

Produces `target/netbeans-golang-<version>.nbm`.

To try it in a throwaway IDE instance, layered over an existing NetBeans install:

```
mvn package nbm:cluster nbm:run-ide -Dnetbeans.installation="/path/to/netbeans"
```

`nbm:cluster` is required — `nbm:run-ide` on its own fails with "No clusters to include in
execution found", because invoking the goal directly skips the lifecycle that builds the
cluster. The instance uses its own `target/userdir`, so it will not disturb your everyday IDE
settings. On Windows PowerShell, quote the whole argument — `"-Dnetbeans.installation=C:\Program
Files\NetBeans-24"` — or the space in the path splits it into two arguments.

## Roadmap

- A `go.mod`-based project type, giving gopls a proper module-root workspace for multi-module
  repositories
- Run / test / debug actions
- An options panel for the gopls path and server flags
- Publication to the Apache NetBeans Plugin Portal

## License

[Apache License 2.0](LICENSE).

Bundled TextMate grammars are MIT-licensed and come from
[worlpaker/go-syntax](https://github.com/worlpaker/go-syntax) (Go) and
[golang/vscode-go](https://github.com/golang/vscode-go) (`go.mod`, `go.sum`) — see [NOTICE](NOTICE).
gopls is a separate program, installed by the user, and is not distributed with this plugin.
