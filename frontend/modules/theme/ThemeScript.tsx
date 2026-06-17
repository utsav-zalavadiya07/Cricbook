import { DEFAULT_THEME_MODE, THEME_STORAGE_KEY } from "./themeTokens";

export default function ThemeScript() {
  const script = `
    (function() {
      try {
        var stored = localStorage.getItem('${THEME_STORAGE_KEY}') || '${DEFAULT_THEME_MODE}';
        var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        var resolved = stored === 'system' ? (prefersDark ? 'dark' : 'light') : stored;
        document.documentElement.dataset.themeMode = stored;
        document.documentElement.dataset.theme = resolved;
        document.documentElement.style.colorScheme = resolved;
      } catch (error) {
        document.documentElement.dataset.themeMode = '${DEFAULT_THEME_MODE}';
        document.documentElement.dataset.theme = 'dark';
        document.documentElement.style.colorScheme = 'dark';
      }
    })();
  `;

  return <script dangerouslySetInnerHTML={{ __html: script }} />;
}
