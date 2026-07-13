export type ExplorerNode = {
  name: string;
  path: string;
  type: "folder" | "file";
  children?: ExplorerNode[];
};

export function buildExplorerTree(paths: string[]): ExplorerNode[] {
  const root: ExplorerNode = { name: "", path: "", type: "folder", children: [] };

  for (const fullPath of paths) {
    const parts = fullPath.split("/").filter(Boolean);
    let current = root;

    parts.forEach((part, index) => {
      const isFile = index === parts.length - 1;
      const currentPath = parts.slice(0, index + 1).join("/");

      let next = current.children?.find((node) => node.name === part && node.type === (isFile ? "file" : "folder"));
      if (!next) {
        next = {
          name: part,
          path: currentPath,
          type: isFile ? "file" : "folder",
          ...(isFile ? {} : { children: [] }),
        };
        current.children?.push(next);
      }

      if (next.type === "folder") {
        current = next;
      }
    });
  }

  const sortNodes = (nodes: ExplorerNode[]): ExplorerNode[] =>
    nodes
      .map((node) =>
        node.type === "folder"
          ? { ...node, children: sortNodes(node.children || []) }
          : node,
      )
      .sort((a, b) => {
        if (a.type !== b.type) {
          return a.type === "folder" ? -1 : 1;
        }
        return a.name.localeCompare(b.name);
      });

  return sortNodes(root.children || []);
}

export function collectFolderPaths(nodes: ExplorerNode[]): string[] {
  const result: string[] = [];
  for (const node of nodes) {
    if (node.type === "folder") {
      result.push(node.path);
      result.push(...collectFolderPaths(node.children || []));
    }
  }
  return result;
}

export function languageFromPath(path: string): string {
  const ext = path.split(".").pop()?.toLowerCase();

  switch (ext) {
    case "ts":
      return "typescript";
    case "tsx":
      return "tsx";
    case "js":
      return "javascript";
    case "jsx":
      return "jsx";
    case "java":
      return "java";
    case "json":
      return "json";
    case "css":
      return "css";
    case "html":
      return "html";
    case "yml":
    case "yaml":
      return "yaml";
    case "xml":
      return "xml";
    case "md":
      return "markdown";
    default:
      return "text";
  }
}
