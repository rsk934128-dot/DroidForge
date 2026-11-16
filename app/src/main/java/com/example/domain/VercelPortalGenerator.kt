package com.example.domain

enum class PortalAccent(val label: String, val hex: String, val onHex: String) {
  EMERALD("Emerald", "#00E59B", "#002114"),
  CYAN("Cyan", "#38BDF8", "#002638"),
  AMBER("Amber", "#FBBF24", "#2E1E00"),
}

data class VercelPortalConfig(
  val appName: String = "DroidForge",
  val tagline: String = "Cloud-built Android APK directly from GitHub Actions CI/CD.",
  val repoFullName: String = "",
  val branch: String = "main",
  val accent: PortalAccent = PortalAccent.EMERALD,
  val useSubfolder: Boolean = true,
)

object VercelPortalGenerator {

  fun generateVercelJson(): String =
    """
    {
      "cleanUrls": true,
      "trailingSlash": false
    }
    """.trimIndent()

  fun generateIndexHtml(config: VercelPortalConfig): String {
    val safeName = escapeHtml(config.appName.trim().ifEmpty { "DroidForge" })
    val safeTagline =
      escapeHtml(
        config.tagline.trim().ifEmpty {
          "Cloud-built Android APK directly from GitHub Actions CI/CD."
        }
      )
    val repoSlug = config.repoFullName.trim().ifEmpty { "owner/repo" }
    val safeRepo = escapeHtml(repoSlug)
    val releasesUrl = "https://github.com/$safeRepo/releases/latest"
    val repoUrl = "https://github.com/$safeRepo"
    val accentHex = config.accent.hex
    val onAccentHex = config.accent.onHex

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>$safeName — Android APK Portal</title>
  <style>
    :root {
      --bg: #0b1117;
      --surface: #111923;
      --surface-high: #1a2532;
      --border: #2e3f52;
      --text: #f1f5f9;
      --muted: #94a3b8;
      --accent: $accentHex;
      --on-accent: $onAccentHex;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: radial-gradient(circle at top, #132234 0%, var(--bg) 60%);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }
    .card {
      width: 100%;
      max-width: 480px;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 24px;
      padding: 32px 26px;
      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
    }
    .badge {
      display: inline-block;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      padding: 6px 12px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.06);
      color: var(--accent);
      margin-bottom: 16px;
    }
    h1 { font-size: 28px; line-height: 1.2; margin-bottom: 10px; }
    p.desc { color: var(--muted); font-size: 15px; line-height: 1.6; margin-bottom: 22px; }
    .meta {
      background: var(--surface-high);
      border-radius: 14px;
      padding: 14px 16px;
      margin-bottom: 22px;
      font-size: 13px;
      color: var(--muted);
      display: flex;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }
    .meta strong { color: var(--text); font-family: ui-monospace, monospace; }
    .btn {
      display: block;
      width: 100%;
      text-align: center;
      padding: 15px 20px;
      border-radius: 14px;
      background: var(--accent);
      color: var(--on-accent);
      font-weight: 700;
      font-size: 16px;
      text-decoration: none;
      transition: transform 0.15s ease, opacity 0.15s ease;
    }
    .btn:hover { transform: translateY(-1px); opacity: 0.94; }
    .secondary {
      display: block;
      text-align: center;
      margin-top: 14px;
      color: var(--muted);
      font-size: 13px;
      text-decoration: none;
    }
    .secondary:hover { color: var(--text); }
  </style>
</head>
<body>
  <main class="card">
    <span class="badge">Android APK · Vercel Portal</span>
    <h1>$safeName</h1>
    <p class="desc">$safeTagline</p>
    <div class="meta">
      <span>Repo: <strong>$safeRepo</strong></span>
      <span>Latest: <strong id="version-tag">Checking...</strong></span>
    </div>
    <a id="download-btn" class="btn" href="$releasesUrl" target="_blank" rel="noopener">
      Download Latest APK
    </a>
    <a class="secondary" href="$repoUrl" target="_blank" rel="noopener">
      View Source on GitHub
    </a>
  </main>
  <script>
    (async function () {
      const repo = "$safeRepo";
      const tagEl = document.getElementById("version-tag");
      const btnEl = document.getElementById("download-btn");
      if (!repo.includes("/") || repo === "owner/repo") {
        tagEl.textContent = "Releases";
        return;
      }
      try {
        const res = await fetch("https://api.github.com/repos/" + repo + "/releases/latest");
        if (!res.ok) throw new Error("No release");
        const data = await res.json();
        tagEl.textContent = data.tag_name || "Latest";
        const apk = (data.assets || []).find(a => a.name && a.name.toLowerCase().endsWith(".apk"));
        if (apk && apk.browser_download_url) {
          const mb = (apk.size / (1024 * 1024)).toFixed(2);
          btnEl.href = apk.browser_download_url;
          btnEl.textContent = "Download APK (" + mb + " MB)";
        }
      } catch (e) {
        tagEl.textContent = "GitHub Releases";
      }
    })();
  </script>
</body>
</html>
    """.trimIndent()
  }

  private fun escapeHtml(input: String): String =
    input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
}
