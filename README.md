# netbeans-golang

[![build](https://github.com/jamov-amd/netbeans-golang/actions/workflows/build.yml/badge.svg)](https://github.com/jamov-amd/netbeans-golang/actions/workflows/build.yml)

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

Go modules also open as projects, with **Build**, **Clean**, **Rebuild**, **Run** and **Test**
running the `go` commands you would type yourself and reporting to the Output window, where
errors are clickable:

| Action | Command |
|---|---|
| Build | `go build ./...` |
| Clean | `go clean ./...` |
| Rebuild | `go build -a ./...` |
| Run | `go run .` — or the module's one `main` package, if it is not at the root |
| Test | `go test ./...` |

Run needs a `main` package to aim at. It uses the module root when that is a command, otherwise
the module's only command; if a module has several commands and none at its root, Run says so
rather than guessing. Test output is plain text in the Output window — there is no Test Results
tree yet.

Build and Clean only bring the Output window forward when something fails. Run and Test always
show it, since their output is the point.

**Run arguments** live in the project's **Properties** (right-click the project). They are passed
to your program after the package, exactly as if typed after `go run .`, and relative paths
resolve against the project directory:

```
-config launcher.conf -verbose
```

## Requirements

- Apache NetBeans 24 or later
- JDK 17+ (NetBeans' own baseline)
- [Go](https://go.dev/dl/) and gopls installed:

```
go install golang.org/x/tools/gopls@latest
```

The plugin locates `gopls` in this order:

1. the path set in **Tools > Options > Miscellaneous > Go**, if you set one
2. `gopls` on your `PATH`
3. `$GOBIN`
4. `$GOPATH/bin` (default `~/go/bin`, or `%USERPROFILE%\go\bin` on Windows)

If gopls cannot be found, the plugin shows a notification with the install command rather
than failing silently.

## Settings

**Tools > Options > Miscellaneous > Go** lets you point the plugin at a specific gopls — useful
if it lives outside the search path above, or if you keep several versions around. Leave the
field empty to use auto-detection; the hint under it always shows which executable the current
setting resolves to.

Changing the path restarts the language server, so it takes effect without restarting the IDE.

## Installing

Download the `.nbm` from [Releases](https://github.com/jamov-amd/netbeans-golang/releases),
then in NetBeans: **Tools > Plugins > Downloaded > Add Plugins…**, pick the file, and install.
The NBM is unsigned, so NetBeans will show a validation warning — that is expected.

## Usage

**File > Open Project** and pick any directory containing a `go.mod` — it is recognised as a Go
project, no wizard and no IDE-specific metadata. A `go.work` directory opens as a project too,
so a multi-module workspace opens once at its root. Open a `.go` file and gopls starts
automatically.

Opening the module as a project is worth doing rather than browsing to a file: the whole module
becomes one gopls workspace. Individual files still work without a project — **File > Open
File**, or the **Favorites** window (Ctrl+3) — but then each directory becomes its own workspace
root, which is fine for a single-package module and wasteful for anything larger.

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

- `go test` results in the Test Results window, instead of plain Output text
- More run configuration — environment variables, and a choice of `main` package
- Run / test for a single file or package
- Debugging, via [Delve](https://github.com/go-delve/delve) and the IDE's DAP support
- Server settings beyond the executable path (`gofumpt`, `staticcheck`, analyses)
- Publication to the Apache NetBeans Plugin Portal

## Troubleshooting

**`go build ./...` fails with "Access is denied" on a temp `a.out.exe`.** Anti-virus, not the
plugin — check by running the same command in a terminal, where it fails identically. When
`go build` covers several packages it discards each binary after linking, and create-then-delete
of an executable is a pattern anti-ransomware tools block; the scanner still holds the file when
Go tries to remove it. Anti-malware in Acronis, Defender and others all do this. Add your Go
installation (`go.exe`) as a trusted process, or exclude the build directories:

```
<GOCACHE>              (go env GOCACHE)
<TEMP>/go-build*       (linker scratch space)
```

It affects every Go build on the machine, not just builds started from the IDE.

## License

[Apache License 2.0](LICENSE).

Bundled TextMate grammars are MIT-licensed and come from
[worlpaker/go-syntax](https://github.com/worlpaker/go-syntax) (Go) and
[golang/vscode-go](https://github.com/golang/vscode-go) (`go.mod`, `go.sum`) — see [NOTICE](NOTICE).
gopls is a separate program, installed by the user, and is not distributed with this plugin.
