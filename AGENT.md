AGENT GUIDE (for coding agents)

Build/Run
- Prereqs: Clojure CLI (tools.deps) and Babashka (CI uses CLI 1.12.0.1530, bb 1.12.200).
- Local dev binary (nREPL-enabled): bb debug-cli (produces ./eca[.bat]).
- Production-like embedded JVM binary: bb prod-cli; Fat JAR: bb prod-jar; Native (GraalVM): set GRAALVM_HOME, then bb native-cli.

Test
- Unit tests (Kaocha): bb test (equivalent to clojure -M:test).
- Run a single test ns: clojure -M:test --focus eca.shared-test (regex allowed, e.g. --focus "eca.features.*").
- Run a single test var: clojure -M:test --focus "eca.shared-test/some-test".
- Integration tests (require ./eca binary): bb prod-cli && bb integration-test.

Lint/Format
- Lint with clj-kondo (config in .clj-kondo/): clj-kondo --lint src test integration-test.
- No repo-enforced auto-formatter; follow idiomatic Clojure style (2-space indent, align let bindings, wrap at ~100 cols).

Code Style
- Namespace preamble: (set! *warn-on-reflection* true); prefer require with :as aliases (e.g., [clojure.string :as string], [eca.features.commands :as f.commands]); use :refer only for a few, explicit symbols.
- Naming: kebab-case for vars/fns, namespaces as eca.<area>[.<subarea>]; predicates end with ? (e.g., clear-history-after-finished?).
- Data: use plain maps with clear keys; prefer assoc-some (eca.shared) to avoid nil assigns; thread with -> / ->> for pipelines.
- Errors/logging: throw ex-info with a data map; log via eca.logger/{debug,info,warn,error}; avoid println to stdout (stderr logging is handled).
- APIs: favor pure functions; side effects live in features/tools or adapters; keep tool I/O and messaging through eca.messenger/f.tools.

Notes
- CI runs: bb test, bb prod-cli, bb integration-test, and a GraalVM build. Ensure these pass locally before PRs.
- Cursor/Copilot: no .cursor/rules, .cursorrules, or .github/copilot-instructions.md present at this time.
