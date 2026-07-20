package com.snehil.auracode.mainui.workspace.preview

/** HTML shell that boots Sandpack inside the Android WebView preview. */
internal object PreviewHtml {

    fun render(payloadJson: String): String {
        val safeJson = payloadJson
            .replace("</", "<\\/")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=yes" />
<style>
  html, body {
    height: 100%; width: 100%; margin: 0; padding: 0;
    overflow-x: hidden; overflow-y: auto;
    background: #0b1220; -webkit-overflow-scrolling: touch;
  }
  #root {
    width: 100%; min-height: 100vh; min-height: 100dvh;
  }
  .sp-wrapper, .sp-layout, .sp-stack,
  .sp-preview-container, .sp-preview, .sp-preview-actions {
    width: 100% !important; border: none !important; margin: 0 !important;
    flex: 1 1 auto !important;
  }
  .sp-layout { display: flex !important; flex-direction: column !important; }
  .sp-preview-container { position: relative !important; flex: 1 1 auto !important; }
  iframe.sp-preview-iframe, .sp-preview-iframe, iframe {
    display: block !important; width: 100% !important; border: 0 !important;
    background: #fff; pointer-events: auto !important; touch-action: auto !important;
  }
  .sp-overlay, .sp-loading, .sp-cube-wrapper, .sp-bridge-frame {
    pointer-events: none !important;
  }
</style>
<script>
  window.__AURA_PAYLOAD__ = $safeJson;
  window.__auraFailed = false;
  function auraLog(m) {
    try { AndroidPreview.postLog(String(m)); } catch (e) {}
  }
  function auraFail(m) {
    window.__auraFailed = true;
    auraLog("FAIL: " + m);
    try { AndroidPreview.postError(String(m)); } catch (e) {}
  }
  function auraStatus(m) {
    auraLog(m);
    try { AndroidPreview.postStatus(String(m)); } catch (e) {}
  }
  function auraViewportHeight() {
    var h = window.innerHeight || document.documentElement.clientHeight || 0;
    if (window.visualViewport && window.visualViewport.height > 0) {
      h = Math.max(h, window.visualViewport.height);
    }
    return Math.max(h, 320);
  }
  function auraFixLayout() {
    var h = auraViewportHeight();
    var root = document.getElementById("root");
    if (root) {
      root.style.minHeight = h + "px";
    }
    var selectors = [
      ".sp-wrapper", ".sp-layout", ".sp-stack",
      ".sp-preview", ".sp-preview-container", ".sp-preview-actions"
    ];
    selectors.forEach(function (sel) {
      document.querySelectorAll(sel).forEach(function (el) {
        el.style.minHeight = h + "px";
        el.style.flex = "1 1 auto";
      });
    });
    document.querySelectorAll("iframe.sp-preview-iframe, .sp-preview-iframe, iframe").forEach(function (iframe) {
      iframe.style.width = "100%";
      iframe.style.height = h + "px";
      iframe.style.minHeight = h + "px";
      iframe.style.display = "block";
      iframe.setAttribute("scrolling", "yes");
      iframe.style.overflow = "auto";
    });
    return h;
  }
  function auraEnableScroll() {
    auraFixLayout();
    document.documentElement.style.overflowY = "auto";
    document.body.style.overflowY = "auto";
  }
  // Keep Sandpack parent mounted — navigating to the bundler URL alone
  // fails with "Can't detect sandbox ID from the current URL".
  function auraUnlockPreview() {
    auraFixLayout();
    document.querySelectorAll(
      ".sp-overlay, .sp-loading, .sp-cube-wrapper, .sp-preview-actions"
    ).forEach(function (el) {
      el.style.pointerEvents = "none";
      el.style.display = "none";
    });
    var iframe = document.querySelector("iframe.sp-preview-iframe, .sp-preview-iframe, iframe");
    if (!iframe) return false;
    iframe.style.pointerEvents = "auto";
    iframe.style.touchAction = "auto";
    iframe.style.zIndex = "10";
    iframe.setAttribute("scrolling", "yes");
    // Let the iframe receive touches; don't steal them at the wrapper.
    [".sp-wrapper", ".sp-layout", ".sp-stack", ".sp-preview", ".sp-preview-container"].forEach(function (sel) {
      document.querySelectorAll(sel).forEach(function (el) {
        el.style.pointerEvents = "auto";
        el.style.touchAction = "auto";
      });
    });
    try { AndroidPreview.onPreviewReady(auraProbeIframe()); } catch (e) {}
    return true;
  }
  function auraProbeIframe() {
    auraFixLayout();
    var iframe = document.querySelector("iframe.sp-preview-iframe, .sp-preview-iframe, iframe");
    if (!iframe) return "iframe=none";
    var r = iframe.getBoundingClientRect();
    return "iframe=" + Math.round(r.width) + "x" + Math.round(r.height);
  }
  function auraIframeReady(probe) {
    if (probe.indexOf("iframe=none") !== -1) return false;
    var m = probe.match(/iframe=(\d+)x(\d+)/);
    if (!m) return false;
    return parseInt(m[2], 10) > 48;
  }
</script>
</head>
<body>
<div id="root"></div>
<script type="module">

  window.onerror = function (m, src, line) {
    auraFail("window.onerror: " + m + " @" + line + " src=" + src);
  };
  window.addEventListener("unhandledrejection", function (ev) {
    var r = ev && ev.reason ? (ev.reason.stack || ev.reason.message || ev.reason) : "unknown";
    auraFail("unhandledrejection: " + r);
  });

  async function boot() {
    try {
      if (!window.__AURA_PAYLOAD__) {
        auraFail("Payload missing from HTML.");
        return;
      }
      auraStatus("import react@18.2.0 from esm.sh …");
      const ReactMod = await import("https://esm.sh/react@18.2.0");
      const React = ReactMod.default || ReactMod;
      auraLog("React loaded keys=" + Object.keys(ReactMod).slice(0,8).join(","));

      auraStatus("import react-dom/client …");
      const RDOM = await import("https://esm.sh/react-dom@18.2.0/client");
      const createRoot = RDOM.createRoot;
      if (!createRoot) { auraFail("createRoot missing"); return; }

      auraStatus("import sandpack-react@2.20.0 …");
      const SP = await import("https://esm.sh/@codesandbox/sandpack-react@2.20.0?deps=react@18.2.0,react-dom@18.2.0");
      auraLog("sandpack keys=" + Object.keys(SP).slice(0,12).join(","));
      const { SandpackProvider, SandpackLayout, SandpackPreview } = SP;
      if (!SandpackProvider) { auraFail("SandpackProvider missing from module"); return; }

      const payload = window.__AURA_PAYLOAD__;
      const fileKeys = Object.keys(payload.files || {});
      auraStatus("mounting Sandpack files=" + fileKeys.length + " entry=" + payload.entry);

      const previewHeight = auraViewportHeight();
      auraLog("viewport height=" + previewHeight);
      auraFixLayout();

      const rootEl = document.getElementById("root");
      const root = createRoot(rootEl);
      const layoutStyle = {
        height: previewHeight + "px",
        minHeight: previewHeight + "px",
        width: "100%",
        border: "none",
        background: "transparent",
        display: "flex",
        flexDirection: "column"
      };
      const previewStyle = {
        height: previewHeight + "px",
        minHeight: previewHeight + "px",
        width: "100%",
        flex: "1 1 auto"
      };
      root.render(
        React.createElement(
          SandpackProvider,
          {
            template: "react-ts",
            theme: "dark",
            files: payload.files,
            customSetup: { dependencies: payload.dependencies, entry: payload.entry },
            options: {
              activeFile: payload.activeFile,
              externalResources: payload.externalResources,
              autorun: true,
              recompileMode: "immediate"
            }
          },
          React.createElement(
            SandpackLayout,
            { style: layoutStyle },
            React.createElement(SandpackPreview, {
              showOpenInCodeSandbox: false,
              showRefreshButton: false,
              showNavigator: false,
              style: previewStyle
            })
          )
        )
      );

      auraStatus("Sandpack React tree mounted");
      try { AndroidPreview.onRendered(); } catch (e) {}

      window.addEventListener("resize", auraFixLayout);
      if (window.visualViewport) {
        window.visualViewport.addEventListener("resize", auraFixLayout);
      }

      // Watch Sandpack / runtime error surfaces and report once for auto-fix.
      var lastErr = "";
      function auraReportOnce(msg) {
        var t = String(msg || "").trim();
        if (!t || t === lastErr) return;
        lastErr = t;
        auraFail(t);
      }
      function auraScanErrors() {
        try {
          var nodes = document.querySelectorAll(
            ".sp-overlay, .sp-message, .sp-error-message, [class*='ErrorMessage'], [class*='error-message']"
          );
          for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            var style = window.getComputedStyle(el);
            if (style && (style.display === "none" || style.visibility === "hidden")) continue;
            var text = (el.innerText || el.textContent || "").trim();
            if (text.length > 24 && /error|invalid|undefined|failed|cannot|module/i.test(text)) {
              auraReportOnce(text.slice(0, 1200));
              return;
            }
          }
          var iframe = document.querySelector("iframe.sp-preview-iframe, .sp-preview-iframe, iframe");
          if (iframe) {
            try {
              var doc = iframe.contentDocument || (iframe.contentWindow && iframe.contentWindow.document);
              if (doc) {
                var bodyText = (doc.body && (doc.body.innerText || doc.body.textContent) || "").trim();
                if (/Element type is invalid|Check the render method|is not defined|Module not found|Failed to compile/i.test(bodyText)) {
                  auraReportOnce(bodyText.slice(0, 1200));
                }
              }
            } catch (cross) { /* cross-origin — rely on console bridge */ }
          }
        } catch (scanErr) {}
      }
      setInterval(auraScanErrors, 1500);

      var attempts = 0;
      var timer = setInterval(function () {
        attempts += 1;
        var probe = auraProbeIframe();
        auraLog("probe#" + attempts + " " + probe);
        var ready = auraIframeReady(probe);
        if (ready || attempts >= 24) {
          clearInterval(timer);
          if (!window.__auraFailed) {
            auraUnlockPreview();
          }
        }
      }, 250);
    } catch (e) {
      auraFail((e && (e.stack || e.message)) ? (e.stack || e.message) : String(e));
    }
  }

  boot();
</script>
</body>
</html>
""".trimIndent()
    }
}
