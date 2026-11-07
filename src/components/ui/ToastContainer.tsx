import React from 'react';
import { CheckCircle2, AlertTriangle, Info, X } from 'lucide-react';
import { useApp } from '../../context/AppContext';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast, t } = useApp();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-16 sm:bottom-6 right-3 sm:right-6 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none px-2 sm:px-0">
      {toasts.map(toast => {
        const isError = toast.type === 'error';
        const isInfo = toast.type === 'info';

        return (
          <div
            key={toast.id}
            className={`premium-enter pointer-events-auto flex items-center gap-2.5 p-3 px-3.5 rounded-[12px] shadow-[0_14px_38px_rgba(8,30,22,0.28)] text-[12px] text-white ${
              isError
                ? 'bg-[#4a1d13] border border-[#bd5a3d]/40'
                : 'bg-[#081e16] border border-white/10'
            }`}
          >
            <div className="shrink-0">
              {isError ? (
                <AlertTriangle className="w-4 h-4 text-[#f0a58d]" />
              ) : isInfo ? (
                <Info className="w-4 h-4 text-[#d8f36b]" />
              ) : (
                <CheckCircle2 className="w-4 h-4 text-[#d8f36b]" />
              )}
            </div>

            <div className="flex-1 font-medium leading-tight">
              {t(toast.message)}
            </div>

            {toast.action && (
              <button
                onClick={() => {
                  toast.action?.onClick();
                  removeToast(toast.id);
                }}
                className="px-2 py-0.5 rounded-[5px] bg-[#d8f36b]/20 text-[#d8f36b] hover:bg-[#d8f36b]/30 font-bold text-[10.5px] transition-colors shrink-0"
              >
                {t(toast.action.label)}
              </button>
            )}

            <button
              onClick={() => removeToast(toast.id)}
              className="w-5 h-5 rounded grid place-items-center text-white/50 hover:text-white shrink-0"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        );
      })}
    </div>
  );
};
