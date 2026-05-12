/**
 * Opens an incognito popup window and resolves with the final redirect URL
 * once `redirectMatch` returns true for a navigated tab URL, or rejects if
 * the user closes the popup or an error URL is detected.
 *
 * @param url          - The URL to open in the popup.
 * @param redirectMatch - Predicate; return true when the tab URL signals completion.
 * @param errorMatch   - Optional predicate; return true when the tab URL signals
 *                       an error (defaults to checking for "error=" in the URL).
 * @returns            The matching redirect URL.
 */
export async function awaitRedirectInIncognitoPopup(
  url: string,
  redirectMatch: (tabUrl: string) => boolean,
  errorMatch: (tabUrl: string) => boolean = (u) => u.includes('error='),
): Promise<string> {
  const popup = await new Promise<chrome.windows.Window>((resolve, reject) =>
    chrome.windows.create(
      { url, type: 'popup', width: 600, height: 700, incognito: true, focused: true },
      (win) => {
        if (!win) return reject(new Error('Failed to create incognito popup window'));
        resolve(win);
      },
    ),
  );

  const tabId = popup.tabs?.[0]?.id;
  if (tabId == null) {
    chrome.windows.remove(popup.id!);
    throw new Error('Could not obtain tab ID from incognito popup');
  }

  return new Promise<string>((resolve, reject) => {
    // ── helpers ────────────────────────────────────────────────────────────
    function cleanup() {
      chrome.tabs.onUpdated.removeListener(onTabUpdated);
      chrome.windows.onRemoved.removeListener(onWindowRemoved);
    }

    function settle(fn: () => void) {
      cleanup();
      // Best-effort close; ignore if already gone
      chrome.windows.remove(popup.id!, () => void chrome.runtime.lastError);
      fn();
    }

    // ── tab navigation listener ────────────────────────────────────────────
    function onTabUpdated(
      updatedTabId: number,
      _changeInfo: chrome.tabs.TabChangeInfo,
      tab: chrome.tabs.Tab,
    ) {
      if (updatedTabId !== tabId) return;

      const tabUrl = tab.url ?? '';

      if (redirectMatch(tabUrl)) {
        settle(() => resolve(tabUrl));
        return;
      }

      if (errorMatch(tabUrl)) {
        settle(() => reject(new Error(`Auth error in redirect: ${tabUrl}`)));
      }
    }

    // ── window-closed listener ─────────────────────────────────────────────
    function onWindowRemoved(winId: number) {
      if (winId !== popup.id) return;
      cleanup(); // window is already gone – skip remove()
      reject(new Error('Operation cancelled by user'));
    }

    chrome.tabs.onUpdated.addListener(onTabUpdated);
    chrome.windows.onRemoved.addListener(onWindowRemoved);
  });
}