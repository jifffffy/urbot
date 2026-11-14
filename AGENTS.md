# AGENTS.md

Urbot: Spring Boot 4.1 backend chat service built on the Embabel Agent framework, with Neo4j/Drivine RAG and DICE semantic memory. Java 21. Extensible via Spring profiles without editing core code. The Vaadin UI has been removed; there is currently no web UI or chat HTTP endpoint.

## Run / build

- Neo4j is required before startup: `docker compose up -d` (Bolt `7891`, HTTP `8892`; not the default 7687/7474). `DrivineStore` calls `provision()` at bean creation, so the app fails without it.
- Provider is chosen by Maven profile activation on env vars, not runtime config: export `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`, or `GOOGLE_API_KEY` *before* running so the matching `embabel-agent-starter-*` is on the classpath. Ollama is always on the classpath.
- Default models are local Ollama `gemma4:e4b` (chat) and `nomic-embed-text:v1.5` (embeddings), overridable with `URBOT_CHAT_MODEL`, `URBOT_MEMORY_MODEL`, `URBOT_FAST_MODEL`, and `URBOT_EMBEDDING_MODEL`. Do not reintroduce hard-coded OpenAI model IDs.
- `./mvnw spring-boot:run` (default generic assistant)
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=<bot>` (custom bot; default `globalDocuments` bean is `@Profile("default")`, so it disappears)
- Package without the old frontend build: `./mvnw -DskipTests package`.
- No lint/formatter plugin is configured. Verify with compile + tests.
- `./aot.sh` uses plain `mvn` (not `./mvnw`), needs Java 24+, and deletes `target/` and `extracted/`.

## Tests

- `./mvnw test` runs unit tests only; no DB or API key needed.
- Single test: `./mvnw -Dtest=TemplateRenderingTest test`.
- `*IT.java` are excluded from test compilation and Surefire in `pom.xml`, so Maven/CI never runs them. Run from the IDE (or temporarily remove both `testExcludes`/`excludes`). They need live Neo4j + an API key, use `@ActiveProfiles("it")`, `TestUrbotApplication` (security excluded), and clean up their isolated contexts.
- After changing prompts, run `TemplateRenderingTest`: it renders every Jinja template and catches broken `{% include %}`s (including ones from the `dice` dependency).
- `ByokFactoryClasspathTest` guards Embabel version-skew startup failures. Keep Embabel artifacts aligned through the `embabel-agent-dependencies` BOM; several artifacts are snapshots, so use `-U` to refresh.
- Smoke-test startup after dependency bumps: the Drivine RAG module (`embabel-agent-rag-neo-drivine`) has no release, only a `0.1.2-SNAPSHOT` built against old Embabel, while Embabel 1.5.x requires Spring Framework 7 / Boot 4. `mvn test` will not catch wiring/classpath failures.

## Extension model (don't modify core for a new bot)

- Core code: `com.embabel.urbot`. Custom bots: `com.embabel.bot.<name>`, with every bean gated by `@Profile("<name>")`.
- A bot package is invisible unless listed in `urbot.bot-packages`; `BotPackageScanConfiguration` scans its beans and `PropositionConfiguration` also builds the DICE entity schema from the same packages.
- `Tool`, `LlmReference`, `ToolishRag`, and `Subagent` beans are auto-collected by `ChatActions` via `List<...>` injection — no explicit wiring.
- Entity extraction is only typed for `NamedEntity` subinterfaces in `urbot.bot-packages` or `urbot.memory.entity-packages`. No domain interfaces are active by default (`entity-packages: []`); only `com.embabel.domain.common.Person` remains, as a base type for `UrbotUser`.
- Config lives under `urbot.chat.*` (`persona`, `objective`, `behaviour`, `llm.model`, `max-words`). `src/main/java/com/embabel/bot/README.md` shows stale flat names like `urbot.persona` — trust `application.yml` and `ChatbotOptions`.
- Templates are name-resolved from `src/main/resources/prompts/{personas,objectives,behaviours}/<name>.jinja`.

## RAG / memory scoping

- Every chunk carries metadata key `context` (`DocumentService.Context.CONTEXT_KEY`).
- Embedding dimensions are fixed per vector index, so `VectorIndexSupport` drops mismatched indexes before provisioning (e.g. 1536→768 when switching OpenAI to `nomic-embed-text`). Documents must be re-ingested after such a switch.
- Personal context = `<userId>_<contextName>` (default context name is `personal`, e.g. `1_personal`); global context = literal `global`.
- `RagConfiguration.globalDocuments` filters `context=global`; `UrbotUser.references()` filters `context=effectiveContext()`. Preserve these filters when adding retrieval.

## Gotchas

- Login users come from `DummyUrbotUserService`: `alice` / `ben`, password `test` for both. The README's `admin`/`user` credential table is stale; Spring Security's generated form login is the only remaining entry point.
- `NullRequestCache` in `SecurityConfiguration` is deliberate: it prevents phantom JSESSIONID creation from stale requests that caused spontaneous logouts. Do not remove; see `notes/LOGOUT.md`.
- Neo4j actuator health is intentionally disabled because Drivine manages its own connection.
- MCP client is enabled by default and Brave search shells out to `docker run -i --rm mcp/brave-search`, so Docker must be running even with an empty `BRAVE_API_KEY`; disable via `spring.ai.mcp.client.enabled=false` if not needed.
- `org.jspecify:jspecify` is now an explicit dependency (Vaadin previously supplied it transitively); keep it if nullness annotations are used.
- Spring Security 7 removed `AntPathRequestMatcher`; use `PathPatternRequestMatcher` for request matchers (see `SecurityConfiguration`).
