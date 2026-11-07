/**
 * Bridge between the React app and the native Android shell.
 *
 * The Android build serves the bundled web app from a local, offline origin and
 * exposes `window.MengiNative`. In a normal browser that object does not exist,
 * so every helper here falls back to the standard web behaviour.
 */

export interface NativeBridge {
  isNativeApp(): boolean;
  /** Called once React has mounted, so the Android shell can hide its error screen. */
  appReady(): void;
  getVersion(): string;
  /** Returns data from the earlier native Android app for one-time migration. */
  getLegacyData(): string;
  /** Saves a file (PDF, CSV, JSON) to the phone's Downloads folder. */
  saveBase64(filename: string, mime: string, base64: string): void;
  /** Opens the Android print dialog (which can also "Save as PDF"). */
  printHtml(html: string, title: string): void;
  getSystemTheme(): 'light' | 'dark';
  animationsEnabled(): boolean;
  setThemeMode(mode: 'system' | 'light' | 'dark'): void;
  getAutoUpdatesEnabled(): boolean;
  setAutoUpdatesEnabled(enabled: boolean): void;
  checkForUpdate(): void;
  getUpdateStatus(): string;
  downloadAndInstallUpdate(): void;
  openUpdateInstallSettings(): void;
  exitApp(): void;
}

export interface UpdateStatusEvent extends CustomEvent<{
  state: 'idle' | 'checking' | 'available' | 'up-to-date' | 'error' | 'downloading' | 'installing' | 'permission-required';
  version?: string;
  notes?: string;
  message?: string;
}> {}

export interface SystemThemeChangeEvent extends CustomEvent<{
  dark: boolean;
  animationsEnabled?: boolean;
}> {}

declare global {
  interface WindowEventMap {
    'mengi-system-theme-change': SystemThemeChangeEvent;
    'mengi-update-status': UpdateStatusEvent;
  }

  interface Window {
    MengiNative?: NativeBridge;
    __androidBack?: () => boolean;
  }
}

export function getNativeBridge(): NativeBridge | null {
  const bridge = window.MengiNative;
  if (!bridge || typeof bridge.isNativeApp !== 'function') return null;
  try {
    return bridge.isNativeApp() ? bridge : null;
  } catch {
    return null;
  }
}

export function isNativeApp(): boolean {
  return getNativeBridge() !== null;
}

export function nativeAppVersion(): string {
  try {
    return getNativeBridge()?.getVersion() || '';
  } catch {
    return '';
  }
}

function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const comma = result.indexOf(',');
      resolve(comma >= 0 ? result.slice(comma + 1) : '');
    };
    reader.onerror = () => reject(reader.error || new Error('read failed'));
    reader.readAsDataURL(blob);
  });
}

function browserDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

/**
 * Saves a blob on the device. Android opens its file picker; browsers download
 * the file directly.
 */
export async function saveFile(blob: Blob, filename: string): Promise<void> {
  const bridge = getNativeBridge();
  if (!bridge) {
    browserDownload(blob, filename);
    return;
  }
  const base64 = await blobToBase64(blob);
  try {
    bridge.saveBase64(filename, blob.type || 'application/octet-stream', base64);
  } catch (err) {
    // Last-resort fallback so nothing is lost if the native side rejects the call.
    console.error('Native save failed, falling back to browser download', err);
    browserDownload(blob, filename);
  }
}

/** Builds the print popup document used by both the browser and Android paths. */
function buildPrintDocument(html: string, title: string): string {
  const safeTitle = title.replace(/[<>&]/g, ch => (ch === '<' ? '&lt;' : ch === '>' ? '&gt;' : '&amp;'));
  return `<!DOCTYPE html>
<html lang="sw">
<head>
<meta charset="utf-8">
<title>${safeTitle}</title>
<style>
body { font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #fff; color: #14231c; padding: 24px; margin: 0; font-size: 13px; line-height: 1.5; }
.sheet-top { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #123a2c; padding-bottom: 16px; margin-bottom: 20px; }
.sheet-brand { display: flex; align-items: center; gap: 12px; }
.sheet-mark { width: 40px; height: 40px; border-radius: 10px; background: #123a2c; color: #d8f36b; display: grid; place-items: center; font-size: 24px; font-weight: 700; font-family: serif; }
.sheet-biz-name { font-size: 20px; font-weight: 700; color: #123a2c; }
.sheet-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 20px; }
table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
th { background: #eef9bd; color: #1b5540; padding: 8px; text-align: left; font-size: 11px; text-transform: uppercase; font-weight: 700; border-bottom: 1px solid #ece7da; }
th.r, td.r { text-align: right; }
td { padding: 9px 8px; border-bottom: 1px solid #eef9bd; }
.totals-box { border: 1px solid #ece7da; border-radius: 8px; overflow: hidden; margin-top: 16px; width: 280px; margin-left: auto; }
.total-row { display: flex; justify-content: space-between; padding: 6px 12px; border-bottom: 1px solid #eef9bd; font-size: 11px; }
.total-row.grand { background: #123a2c; color: #fff; font-weight: 700; font-size: 12px; }
.total-row.grand span:last-child { color: #d8f36b; }
.footer { margin-top: 28px; padding-top: 12px; border-top: 1px solid #ece7da; font-size: 10px; color: #6d7b72; display: flex; justify-content: space-between; }
@page { size: A4; margin: 10mm; }
</style>
</head>
<body>
${html}
</body>
</html>`;
}

/** Prints / saves as PDF. Android shows the system print dialog. */
export function printDocument(html: string, title: string): void {
  const doc = buildPrintDocument(html, title);

  const bridge = getNativeBridge();
  if (bridge) {
    try {
      bridge.printHtml(doc, title);
      return;
    } catch (err) {
      console.error('Native print failed', err);
    }
  }

  const win = window.open('', '_blank', 'width=940,height=1000');
  if (!win) {
    alert('Tafadhali ruhusu pop-up kwenye kivinjari ili kuchapisha hati hii.');
    return;
  }
  win.document.open();
  win.document.write(doc);
  win.document.close();
  win.focus();
  setTimeout(() => {
    try {
      win.print();
    } catch {
      // ignore
    }
  }, 400);
}