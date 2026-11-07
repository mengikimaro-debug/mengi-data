import React from 'react';
import { AlertTriangle, Info } from 'lucide-react';
import { useApp } from '../../context/AppContext';

export const ConfirmModal: React.FC = () => {
  const { confirmDialog, closeConfirm, t } = useApp();

  if (!confirmDialog) return null;

  const isDanger = confirmDialog.danger !== false;

  const handleConfirm = () => {
    confirmDialog.onConfirm();
    closeConfirm();
  };

  return (
    <div className="premium-backdrop fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#081e16]/60 backdrop-blur-xs">
      <div className="premium-enter relative w-full max-w-md bg-[#fcfaf4] border border-[#e2dccd] rounded-[20px] shadow-[0_24px_70px_rgba(8,30,22,0.22)] overflow-hidden">
        <div className="p-6">
          <div
            className={`w-11 h-11 rounded-[12px] grid place-items-center mb-3.5 ${
              isDanger ? 'bg-[#f7e4db] text-[#9e432b]' : 'bg-[#e8f0e8] text-[#4b6f8b]'
            }`}
          >
            {isDanger ? <AlertTriangle className="w-5 h-5" /> : <Info className="w-5 h-5" />}
          </div>

          <h3 className="font-display text-[20px] font-semibold text-[#14231c] tracking-tight">
            {confirmDialog.title}
          </h3>

          <p className="text-[12px] text-[#746e7d] mt-2 leading-relaxed">
            {confirmDialog.message}
          </p>
        </div>

        <div className="flex items-center justify-end gap-2 px-6 py-3.5 bg-[#f8f4ea]/60 border-t border-[#e2dccd]">
          <button
            type="button"
            onClick={closeConfirm}
            className="px-3.5 py-2 rounded-[10px] text-[12px] font-semibold text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c] transition-colors"
          >
            {t('Ghairi')}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            className={`px-4 py-2 rounded-[10px] text-white text-[12px] font-semibold transition-all ${
              isDanger
                ? 'bg-[#bd5a3d] hover:bg-[#9e432b]'
                : 'bg-[#0c2b22] hover:bg-[#123a2c]'
            }`}
          >
            {t(confirmDialog.okLabel || 'Endelea')}
          </button>
        </div>
      </div>
    </div>
  );
};
