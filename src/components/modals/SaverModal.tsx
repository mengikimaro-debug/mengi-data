import React, { useState, useEffect } from 'react';
import { PiggyBank, Target, Calendar, FileText } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { todayISO } from '../../utils/format';

export const SaverModal: React.FC = () => {
  const {
    saverModalOpen,
    saverGoalToEdit,
    saverTxnTarget,
    closeSaverModal,
    closeSaverTxnModal,
    addSavingsGoal,
    updateSavingsGoal,
    recordSavingsTxn,
    addToast,
    t
  } = useApp();

  const [title, setTitle] = useState('');
  const [targetAmount, setTargetAmount] = useState('');
  const [initialAmount, setInitialAmount] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [notes, setNotes] = useState('');

  // Txn modal state
  const [txnAmount, setTxnAmount] = useState('');
  const [txnDate, setTxnDate] = useState(todayISO());
  const [txnNote, setTxnNote] = useState('');

  useEffect(() => {
    if (saverGoalToEdit) {
      setTitle(saverGoalToEdit.title);
      setTargetAmount(String(saverGoalToEdit.targetAmount));
      setInitialAmount(String(saverGoalToEdit.currentAmount));
      setTargetDate(saverGoalToEdit.targetDate || '');
      setNotes(saverGoalToEdit.notes || '');
    } else {
      setTitle('');
      setTargetAmount('');
      setInitialAmount('0');
      setTargetDate('');
      setNotes('');
    }
  }, [saverGoalToEdit, saverModalOpen]);

  useEffect(() => {
    if (saverTxnTarget) {
      setTxnAmount('');
      setTxnDate(todayISO());
      setTxnNote('');
    }
  }, [saverTxnTarget]);

  if (!saverModalOpen && !saverTxnTarget) return null;

  const handleSaveGoal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      addToast(t('Tafadhali ingiza jina la akiba.'), 'error');
      return;
    }
    const target = Number(targetAmount) || 0;
    if (target <= 0) {
      addToast(t('Ingiza lengo la kiasi lililo sahihi.'), 'error');
      return;
    }

    if (saverGoalToEdit) {
      updateSavingsGoal(saverGoalToEdit.id, {
        title: title.trim(),
        targetAmount: target,
        targetDate: targetDate.trim(),
        notes: notes.trim()
      });
    } else {
      const initial = Number(initialAmount) || 0;
      addSavingsGoal({
        title: title.trim(),
        targetAmount: target,
        currentAmount: initial,
        targetDate: targetDate.trim(),
        notes: notes.trim()
      });
    }
    closeSaverModal();
  };

  const handleSaveTxn = (e: React.FormEvent) => {
    e.preventDefault();
    if (!saverTxnTarget) return;
    const amt = Number(txnAmount) || 0;
    if (amt <= 0) {
      addToast(t('Ingiza kiasi sahihi.'), 'error');
      return;
    }
    if (saverTxnTarget.type === 'withdraw' && amt > saverTxnTarget.goal.currentAmount) {
      addToast(t('Kiasi unachotaka kutoa kinazidi akiba uliyotunza!'), 'error');
      return;
    }

    recordSavingsTxn(
      saverTxnTarget.goal.id,
      saverTxnTarget.type,
      amt,
      txnDate,
      txnNote.trim()
    );
    closeSaverTxnModal();
  };

  return (
    <>
      {/* Goal Add/Edit Modal */}
      {saverModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#081e16]/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-[#fcfaf4] rounded-[24px] border border-[#e2dccd] shadow-[0_24px_60px_rgba(8,30,22,0.25)] overflow-hidden">
            <div className="px-6 py-4 border-b border-[#e2dccd] flex items-center justify-between bg-[#f5f2e9]">
              <div className="flex items-center gap-2">
                <PiggyBank className="w-5 h-5 text-[#1b5540]" />
                <h3 className="font-display text-[18px] font-bold text-[#14231c]">
                  {saverGoalToEdit ? t('Hariri Lengo la Akiba') : t('Ongeza Lengo la Akiba')}
                </h3>
              </div>
              <button
                onClick={closeSaverModal}
                className="w-8 h-8 rounded-full bg-[#e2dccd] text-[#14231c] grid place-items-center hover:bg-[#d5d0c0] font-bold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleSaveGoal} className="p-6 space-y-4">
              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {t('Jina la Lengo / Akiba *')}
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={e => setTitle(e.target.value)}
                  placeholder={t('Mfano: Mfuko wa Dharura')}
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {t('Lengo la Kiasi (TZS) *')}
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  step="any"
                  value={targetAmount}
                  onChange={e => setTargetAmount(e.target.value)}
                  placeholder="0"
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              {!saverGoalToEdit && (
                <div>
                  <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                    {t('Akiba ya Kuanzia (TZS)')}
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="any"
                    value={initialAmount}
                    onChange={e => setInitialAmount(e.target.value)}
                    placeholder="0"
                    className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                  />
                </div>
              )}

              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {t('Tarehe ya Lengo')}
                </label>
                <input
                  type="date"
                  value={targetDate}
                  onChange={e => setTargetDate(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {t('Maelezo / Madhumuni')}
                </label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                  placeholder={t('Maelezo zaidi...')}
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-[#e2dccd]">
                <button
                  type="button"
                  onClick={closeSaverModal}
                  className="px-4 py-2.5 rounded-[12px] bg-[#e2dccd] text-[#14231c] text-[12.5px] font-semibold hover:bg-[#d5d0c0]"
                >
                  {t('Ghairi')}
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-[12px] bg-[#1b5540] text-[#d8f36b] text-[12.5px] font-semibold hover:bg-[#123d2e]"
                >
                  {t('Hifadhi')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Deposit / Withdraw Modal */}
      {saverTxnTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#081e16]/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-[#fcfaf4] rounded-[24px] border border-[#e2dccd] shadow-[0_24px_60px_rgba(8,30,22,0.25)] overflow-hidden">
            <div className="px-6 py-4 border-b border-[#e2dccd] flex items-center justify-between bg-[#f5f2e9]">
              <div className="flex items-center gap-2">
                <PiggyBank className="w-5 h-5 text-[#1b5540]" />
                <h3 className="font-display text-[18px] font-bold text-[#14231c]">
                  {saverTxnTarget.type === 'deposit' ? t('Weka Akiba') : t('Toa Akiba')} — {saverTxnTarget.goal.title}
                </h3>
              </div>
              <button
                onClick={closeSaverTxnModal}
                className="w-8 h-8 rounded-full bg-[#e2dccd] text-[#14231c] grid place-items-center hover:bg-[#d5d0c0] font-bold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleSaveTxn} className="p-6 space-y-4">
              <div className="p-3 rounded-[12px] bg-[#1b5540]/10 text-[#1b5540] text-[12.5px] font-semibold">
                {t('Akiba sasa:')} {saverTxnTarget.goal.currentAmount.toLocaleString()} TZS
              </div>

              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {saverTxnTarget.type === 'deposit' ? t('Kiasi cha Kuweka (TZS) *') : t('Kiasi cha Kutoa (TZS) *')}
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  step="any"
                  value={txnAmount}
                  onChange={e => setTxnAmount(e.target.value)}
                  placeholder="0"
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {t('Tarehe')}
                </label>
                <input
                  type="date"
                  value={txnDate}
                  onChange={e => setTxnDate(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[11.5px] font-bold uppercase text-[#6d7b72] mb-1">
                  {t('Maelezo / Chanzo')}
                </label>
                <textarea
                  rows={2}
                  value={txnNote}
                  onChange={e => setTxnNote(e.target.value)}
                  placeholder={t('Maelezo ya muamala...')}
                  className="w-full px-3.5 py-2.5 rounded-[12px] bg-white border border-[#e2dccd] text-[13px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-[#e2dccd]">
                <button
                  type="button"
                  onClick={closeSaverTxnModal}
                  className="px-4 py-2.5 rounded-[12px] bg-[#e2dccd] text-[#14231c] text-[12.5px] font-semibold hover:bg-[#d5d0c0]"
                >
                  {t('Ghairi')}
                </button>
                <button
                  type="submit"
                  className={`px-5 py-2.5 rounded-[12px] text-white text-[12.5px] font-semibold ${
                    saverTxnTarget.type === 'deposit' ? 'bg-[#1b5540] hover:bg-[#123d2e]' : 'bg-[#b94a39] hover:bg-[#9d3a2c]'
                  }`}
                >
                  {saverTxnTarget.type === 'deposit' ? t('Weka Akiba') : t('Toa Akiba')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};
