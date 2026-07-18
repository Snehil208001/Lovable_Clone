package com.snehil.auracode.mainui.workspace.preview

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

@Serializable
data class SandpackFile(val code: String)

@Serializable
data class PreviewPayload(
    val files: Map<String, SandpackFile>,
    val dependencies: Map<String, String>,
    val entry: String,
    val externalResources: List<String>,
    val activeFile: String
)

/**
 * Kotlin port of the web app's buildSandpackFiles (frontend/src/lib/workspace/sandpack.ts).
 * Turns the project's flat file list into a Sandpack (react-ts) file map: strips build
 * configs the browser bundler cannot handle, rewrites Tailwind directives, and synthesizes
 * the entry point and package.json when the generated app lacks them.
 */
object SandpackBuilder {

    const val ENTRY = "/src/main.tsx"

    private const val TOKENS_PATH = "/src/__preview-tokens.css"

    val EXTERNAL_RESOURCES = listOf(
        "https://cdn.tailwindcss.com",
        "https://cdn.jsdelivr.net/npm/daisyui@4.7.2/dist/full.min.css",
        "https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@400;500;600;700;800&family=Fraunces:ital,wght@0,400;0,500;0,600;0,700;1,400&family=Sora:wght@300;400;500;600;700&family=Outfit:wght@300;400;500;600;700&family=Newsreader:ital,wght@0,400;0,500;0,600;1,400&family=Unbounded:wght@400;600;800&family=Manrope:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap"
    )

    val DEPENDENCIES: Map<String, String> = linkedMapOf(
        "react" to "^18.2.0",
        "react-dom" to "^18.2.0",
        "react-scripts" to "^5.0.1",
        "lucide-react" to "^0.300.0",
        "react-router-dom" to "^6.22.0",
        "@tanstack/react-query" to "^5.0.0",
        "clsx" to "^2.1.0",
        "tailwind-merge" to "^2.2.1",
        "uuid" to "^9.0.1"
    )

    private val TOKENS_CSS = """
.font-display { font-family: "Bricolage Grotesque", sans-serif; }
.font-serif-display { font-family: "Fraunces", serif; }
.font-body { font-family: "Sora", sans-serif; }
.font-outfit { font-family: "Outfit", sans-serif; }
.font-newsreader { font-family: "Newsreader", serif; }
.font-unbounded { font-family: "Unbounded", sans-serif; }
.font-manrope { font-family: "Manrope", sans-serif; }
.font-mono { font-family: "JetBrains Mono", monospace; }

@keyframes preview-fade-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes preview-slide-up {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes preview-scale-in {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
.animate-fade-in { animation: preview-fade-in 0.6s ease-out both; }
.animate-slide-up { animation: preview-slide-up 0.6s cubic-bezier(0.22, 1, 0.36, 1) both; }
.animate-scale-in { animation: preview-scale-in 0.4s ease-out both; }
""".trim()

    private val moduleSpecifierRegex = Regex(
        """(?:\bimport\s*\(\s*|\brequire\s*\(\s*|\b(?:import|export)\s[^'"()]*?from\s*|\bimport\s*)['"]([^'"]+)['"]"""
    )

    private val prettyJson = Json { prettyPrint = true }

    /** [rawFiles] are backend (path, content) pairs; paths may or may not start with "/". */
    fun build(rawFiles: List<Pair<String, String>>): PreviewPayload {
        val files = LinkedHashMap<String, String>()

        rawFiles.forEach { (rawPath, rawContent) ->
            val path = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
            if (path.contains("postcss.config") ||
                path.contains("tailwind.config") ||
                path.contains("vite.config")
            ) return@forEach

            var content = rawContent
            if (path.endsWith(".css")) {
                content = content
                    .replace(Regex("""@import\s+['"]tailwindcss.*?['"];?"""), "/* tailwind import removed for CDN */")
                    .replace(Regex("""@tailwind\s+base;?"""), "")
                    .replace(Regex("""@tailwind\s+components;?"""), "")
                    .replace(Regex("""@tailwind\s+utilities;?"""), "")
            }
            files[path] = content
        }

        files.remove("/index.html")
        files.remove("/public/index.html")

        files[TOKENS_PATH] = TOKENS_CSS

        if (!files.containsKey("/src/main.tsx") && !files.containsKey("/src/index.tsx")) {
            val possibleAppPaths = listOf("/src/App.tsx", "/src/App.jsx", "/src/Index.tsx", "/src/Index.jsx")
            val existingAppPath = possibleAppPaths.firstOrNull { files.containsKey(it) }
            val importPath = existingAppPath
                ?.replace("/src/", "./")
                ?.replace(".tsx", "")
                ?.replace(".jsx", "")
            val appComponent = if (existingAppPath != null) "<App />" else "<div><h2>No App Component Found</h2></div>"
            files["/src/main.tsx"] = buildString {
                appendLine("import React from \"react\";")
                appendLine("import ReactDOM from \"react-dom/client\";")
                if (existingAppPath != null) appendLine("import App from \"$importPath\";")
                appendLine()
                appendLine("ReactDOM.createRoot(document.getElementById(\"root\")!).render(")
                appendLine("  <React.StrictMode>")
                appendLine("    $appComponent")
                appendLine("  </React.StrictMode>")
                appendLine(");")
            }
        }

        val entryPath = if (files.containsKey("/src/main.tsx")) "/src/main.tsx" else "/src/index.tsx"
        val tokensImport = "import \".${TOKENS_PATH.replace("/src", "")}\";"
        var entryCode = files[entryPath].orEmpty()
        if (!entryCode.contains(tokensImport)) {
            entryCode = "$tokensImport\n$entryCode"
        }
        // Generated apps often use <Link>/<Routes> without wrapping <BrowserRouter>.
        // Sandpack iframe needs a router provider or the preview crashes blank.
        entryCode = ensureRouterProvider(entryCode, files)
        files[entryPath] = entryCode

        val dependencies = LinkedHashMap<String, String>()
        DEPENDENCIES.forEach { (k, v) -> dependencies[k] = v }
        collectExternalPackages(files).forEach { pkg ->
            if (!dependencies.containsKey(pkg)) dependencies[pkg] = "latest"
        }
        // Always include react-router-dom when we injected MemoryRouter.
        if (entryCode.contains("MemoryRouter") && !dependencies.containsKey("react-router-dom")) {
            dependencies["react-router-dom"] = "latest"
        }

        val pkgJson: JsonObject = buildJsonObject {
            put("name", "generated-preview")
            put("main", ENTRY)
            put("private", true)
            put("version", "0.0.0")
            putJsonObject("dependencies") { dependencies.forEach { (k, v) -> put(k, v) } }
            putJsonObject("scripts") {
                put("start", "react-scripts start")
                put("build", "react-scripts build")
                put("test", "react-scripts test")
                put("eject", "react-scripts eject")
            }
        }
        files["/package.json"] = prettyJson.encodeToString(JsonObject.serializer(), pkgJson)

        return PreviewPayload(
            files = files.mapValues { SandpackFile(it.value) },
            dependencies = dependencies,
            entry = ENTRY,
            externalResources = EXTERNAL_RESOURCES,
            activeFile = activeFile(files)
        )
    }

    private fun activeFile(files: Map<String, String>): String = when {
        files.containsKey("/src/App.tsx") -> "/src/App.tsx"
        files.containsKey("/src/main.tsx") -> "/src/main.tsx"
        else -> ENTRY
    }

    /**
     * Generated apps often use Link/Routes without wrapping a Router.
     * Sandpack then crashes with: useRoutes() may be used only in the context of a <Router>.
     */
    private fun ensureRouterProvider(entryCode: String, files: Map<String, String>): String {
        fun usesRouter(code: String): Boolean {
            if (Regex("""from\s+['"]react-router(-dom)?['"]""").containsMatchIn(code)) return true
            return Regex(
                """\b(BrowserRouter|HashRouter|MemoryRouter|Routes|Route|Link|NavLink|Outlet|useNavigate|useParams|useLocation|useRoutes)\b"""
            ).containsMatchIn(code)
        }

        fun hasRouterProvider(code: String): Boolean =
            Regex("""<\s*(BrowserRouter|HashRouter|MemoryRouter|RouterProvider|Router)\b""")
                .containsMatchIn(code)

        val needsRouter = files.values.any { usesRouter(it) } || usesRouter(entryCode)
        if (!needsRouter) return entryCode
        if (files.values.any { hasRouterProvider(it) } || hasRouterProvider(entryCode)) return entryCode

        var code = entryCode
        if (!Regex("""\bMemoryRouter\b""").containsMatchIn(code)) {
            code = "import { MemoryRouter } from \"react-router-dom\";\n$code"
        }

        // Wrap self-closing <App /> (typical Vite entry)
        val appSelfClosing = Regex("""<App(?:\s[^>]*)?\s*/>""")
        if (appSelfClosing.containsMatchIn(code)) {
            return appSelfClosing.replace(code, "<MemoryRouter><App /></MemoryRouter>")
        }

        // Wrap StrictMode / React.StrictMode children
        val strict = Regex(
            """(<(?:React\.)?StrictMode>\s*)([\s\S]*?)(\s*</(?:React\.)?StrictMode>)"""
        )
        if (strict.containsMatchIn(code)) {
            return strict.replace(code) { m ->
                val inner = m.groupValues[2].trim()
                if (inner.contains("MemoryRouter")) m.value
                else "${m.groupValues[1]}\n    <MemoryRouter>\n      $inner\n    </MemoryRouter>${m.groupValues[3]}"
            }
        }

        return code
    }

    private fun collectExternalPackages(files: Map<String, String>): List<String> {
        val packages = LinkedHashSet<String>()
        files.forEach { (path, code) ->
            if (!Regex("""\.(?:tsx?|jsx?|mjs)$""").containsMatchIn(path)) return@forEach
            moduleSpecifierRegex.findAll(code).forEach { match ->
                packageNameFromSpecifier(match.groupValues[1])?.let { packages.add(it) }
            }
        }
        return packages.toList()
    }

    private fun packageNameFromSpecifier(specifier: String): String? {
        if (specifier.startsWith(".") || specifier.startsWith("/") || specifier.contains(":")) return null
        val parts = specifier.split("/")
        return if (specifier.startsWith("@")) {
            if (parts.size > 1) "${parts[0]}/${parts[1]}" else null
        } else {
            parts[0]
        }
    }

    /** Leaf paths only: drop entries that are prefixes of other entries. */
    fun leafPaths(paths: List<String>): List<String> =
        paths.filter { p -> paths.none { it != p && it.startsWith("$p/") } }
}
