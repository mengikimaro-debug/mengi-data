/**
 * Build shim for constrained Windows environments (including the DSH sandbox).
 *
 * Vite probes Windows network drives with `child_process.exec("net use")` while
 * resolving real paths. In sandboxes that forbid pipe creation this throws
 * EPERM synchronously and aborts the whole build. Vite already treats a failed
 * probe as "no network drives", so we simply surface the failure through the
 * callback instead of letting it escape.
 */
const cp = require('node:child_process');

const originalExec = cp.exec;
if (!originalExec.__mengiPatched) {
  function safeExec(command, options, callback) {
    try {
      return originalExec.apply(cp, arguments);
    } catch (err) {
      let cb = callback;
      if (typeof options === 'function' && typeof callback !== 'function') cb = options;
      if (typeof cb === 'function') cb(err, '', '');
      return { on() {}, kill() {}, unref() {} };
    }
  }
  safeExec.__mengiPatched = true;
  cp.exec = safeExec;
}
