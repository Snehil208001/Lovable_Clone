import type { SandpackFiles } from "@codesandbox/sandpack-react";

export const SANDPACK_ENTRY = "/src/main.tsx";

// Injected as <script>/<link> tags into the preview iframe via Sandpack's
// options.externalResources — the react-ts template ignores custom head
// content in /public/index.html, so this is the only way to load the
// Tailwind Play CDN and daisyUI inside the preview.
export const SANDPACK_EXTERNAL_RESOURCES = [
  "https://cdn.tailwindcss.com",
  "https://cdn.jsdelivr.net/npm/daisyui@4.7.2/dist/full.min.css",
  "https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@400;500;600;700;800&family=Fraunces:ital,wght@0,400;0,500;0,600;0,700;1,400&family=Sora:wght@300;400;500;600;700&family=Outfit:wght@300;400;500;600;700&family=Newsreader:ital,wght@0,400;0,500;0,600;1,400&family=Unbounded:wght@400;600;800&family=Manrope:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap",
];

const PREVIEW_TOKENS_CSS_PATH = "/src/__preview-tokens.css";

// The font/animation utilities the AI system prompt advertises (font-display,
// animate-slide-up, ...). Generated tailwind.config.js files are stripped and
// the Play CDN's inline config cannot run from externalResources, so these are
// provided as plain CSS classes instead.
const PREVIEW_TOKENS_CSS = `
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
`;

export const SANDPACK_DEPENDENCIES = {
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "react-scripts": "^5.0.1",
  "lucide-react": "^0.300.0",
  "react-router-dom": "^6.22.0",
  "@tanstack/react-query": "^5.0.0",
  "clsx": "^2.1.0",
  "tailwind-merge": "^2.2.1",
  // Not in the prompt whitelist, but the model reaches for it constantly;
  // v9 ships both CJS and ESM so it bundles cleanly.
  "uuid": "^9.0.1",
};

// Matches the module specifier of static imports/re-exports, side-effect
// imports, dynamic import() and require() calls.
const MODULE_SPECIFIER_RE =
  /(?:\bimport\s*\(\s*|\brequire\s*\(\s*|\b(?:import|export)\s[^'"()]*?from\s*|\bimport\s*)['"]([^'"]+)['"]/g;

function packageNameFromSpecifier(specifier: string): string | null {
  if (specifier.startsWith(".") || specifier.startsWith("/") || specifier.includes(":")) return null;
  const parts = specifier.split("/");
  if (specifier.startsWith("@")) return parts.length > 1 ? `${parts[0]}/${parts[1]}` : null;
  return parts[0];
}

function collectExternalPackages(files: SandpackFiles): string[] {
  const packages = new Set<string>();
  for (const [path, file] of Object.entries(files)) {
    if (!/\.(?:tsx?|jsx?|mjs)$/.test(path)) continue;
    const code = typeof file === "string" ? file : file.code;
    for (const match of code.matchAll(MODULE_SPECIFIER_RE)) {
      const name = packageNameFromSpecifier(match[1]);
      if (name) packages.add(name);
    }
  }
  return Array.from(packages);
}

export interface SandpackBuildResult {
  files: SandpackFiles;
  error: string | null;
}

/**
 * Turns the project's flat file list into a Sandpack (react-ts template) file map:
 * strips build configs the browser bundler cannot handle, rewrites Tailwind CSS
 * directives (the Tailwind Play CDN is injected via /public/index.html instead),
 * and synthesizes the entry point and package.json when the generated app lacks them.
 */
export async function buildSandpackFiles(
  paths: string[],
  getFileContent: (path: string) => Promise<string>,
): Promise<SandpackBuildResult> {
  const filePathsOnly = paths.filter(p => !paths.some(other => other !== p && other.startsWith(p + '/')));

  if (!filePathsOnly.length) {
    return { files: {}, error: "No files available yet to render a preview." };
  }

  const fileEntries = await Promise.allSettled(
    filePathsOnly.map(async (path) => [path, await getFileContent(path)] as const),
  );

  const nextPreviewFiles: SandpackFiles = {};
  fileEntries.forEach((entry) => {
    if (entry.status === "fulfilled") {
      const [path, rawContent] = entry.value;
      let content = rawContent;
      const normalizedPath = path.startsWith("/") ? path : `/${path}`;

      // Strip out configs that crash the browser Webpack bundler
      if (
        normalizedPath.includes("postcss.config") ||
        normalizedPath.includes("tailwind.config") ||
        normalizedPath.includes("vite.config")
      ) {
        return;
      }

      // Prevent CSS loaders from trying to resolve Node.js tailwind modules
      if (normalizedPath.endsWith(".css")) {
        content = content
          .replace(/@import\s+['"]tailwindcss.*?['"];?/g, "/* tailwind import removed for CDN */")
          .replace(/@tailwind\s+base;?/g, "")
          .replace(/@tailwind\s+components;?/g, "")
          .replace(/@tailwind\s+utilities;?/g, "");
      }

      nextPreviewFiles[normalizedPath] = { code: content };
    }
  });

  if (!Object.keys(nextPreviewFiles).length) {
    return { files: {}, error: "Preview could not load file contents." };
  }

  // Remove generated index.html: the react-ts template's HTML shell is static
  // and custom markup is ignored (see SANDPACK_EXTERNAL_RESOURCES).
  if (nextPreviewFiles["/index.html"]) {
    delete nextPreviewFiles["/index.html"];
  }
  delete nextPreviewFiles["/public/index.html"];

  nextPreviewFiles[PREVIEW_TOKENS_CSS_PATH] = { code: PREVIEW_TOKENS_CSS };

  if (!nextPreviewFiles["/src/main.tsx"] && !nextPreviewFiles["/src/index.tsx"]) {
    const possibleAppPaths = ["/src/App.tsx", "/src/App.jsx", "/src/Index.tsx", "/src/Index.jsx"];
    const existingAppPath = possibleAppPaths.find(p => nextPreviewFiles[p]);
    let importPath = "./App";
    let appComponent = "<App />";

    if (existingAppPath) {
      importPath = existingAppPath.replace("/src/", "./").replace(".tsx", "").replace(".jsx", "");
    } else {
      appComponent = `<div><h2>No App Component Found</h2></div>`;
    }

    nextPreviewFiles["/src/main.tsx"] = {
      code: `import React from "react";
import ReactDOM from "react-dom/client";
${existingAppPath ? `import App from "${importPath}";` : ""}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    ${appComponent}
  </React.StrictMode>
);`,
    };
  }

  // Make sure the design tokens load no matter what the generated entry imports.
  const entryPath = nextPreviewFiles["/src/main.tsx"] ? "/src/main.tsx" : "/src/index.tsx";
  const entryFile = nextPreviewFiles[entryPath];
  const entryCode = typeof entryFile === "string" ? entryFile : entryFile.code;
  const tokensImport = `import ".${PREVIEW_TOKENS_CSS_PATH.replace("/src", "")}";`;
  if (!entryCode.includes(tokensImport)) {
    nextPreviewFiles[entryPath] = { code: `${tokensImport}\n${entryCode}` };
  }

  // Always synthesize package.json ourselves: start from any versions the
  // generated one pinned, let our known-good versions win, then auto-install
  // every other package the code imports at "latest" — the model ignores the
  // dependency whitelist often enough that a missing import must degrade to a
  // slower install, not a crashed preview ("Could not find dependency").
  const generatedPackageJson = nextPreviewFiles["/package.json"];
  let generatedDependencies: Record<string, string> = {};
  if (generatedPackageJson) {
    try {
      const raw = typeof generatedPackageJson === "string" ? generatedPackageJson : generatedPackageJson.code;
      generatedDependencies = JSON.parse(raw)?.dependencies ?? {};
    } catch {}
  }

  const dependencies: Record<string, string> = { ...generatedDependencies, ...SANDPACK_DEPENDENCIES };
  for (const pkg of collectExternalPackages(nextPreviewFiles)) {
    if (!dependencies[pkg]) dependencies[pkg] = "latest";
  }

  nextPreviewFiles["/package.json"] = {
    code: JSON.stringify(
      {
        name: "generated-preview",
        main: SANDPACK_ENTRY,
        private: true,
        version: "0.0.0",
        dependencies,
        scripts: {
          "start": "react-scripts start",
          "build": "react-scripts build",
          "test": "react-scripts test",
          "eject": "react-scripts eject",
        },
      },
      null,
      2,
    ),
  };

  return { files: nextPreviewFiles, error: null };
}

export function getActiveFileForSandpack(previewFiles: SandpackFiles, selectedFilePath: string | null): string {
  if (selectedFilePath) {
    const normalized = selectedFilePath.startsWith("/") ? selectedFilePath : `/${selectedFilePath}`;
    if (previewFiles[normalized]) return normalized;
  }
  if (previewFiles["/src/App.tsx"]) return "/src/App.tsx";
  if (previewFiles["/src/main.tsx"]) return "/src/main.tsx";
  return SANDPACK_ENTRY;
}
