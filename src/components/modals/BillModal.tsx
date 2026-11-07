import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  todayISO,
  addDaysISO,
  BILL_CATEGORIES,
  METHODS,
  num
} from '../../utils/format';

export const BillModal: React.FC = () => {
  const {
    billModalOpen,
    editingBill,
    closeBillModal,
    addBill,
    updateBill,
    addToast,
    t
  } = useApp();

  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [dueDate, setDueDate] = useState(addDaysISO(todayISO(), 7));
  const [category, setCategory] = useState(BILL_CATEGORIES[0]);
  const [payee, setPayee] = useState('');
  const [method, setMethod] = useState('M-Pesa');
  const [paid, setPaid] = useState(false);
  const [paidDate, setPaidDate] = useState('');
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (editingBill) {
      setName(editingBill.name);
      setAmount(String(editingBill.amount));
      setDueDate(editingBill.dueDate);
      setCategory(editingBill.category);
      setPayee(editingBill.payee || '');
      setMethod(editingBill.method || 'M-Pesa');
      setPaid(editingBill.paid);
      setPaidDate(editingBill.paidDate || '');
      setNotes(editingBill.notes || '');
    } else {
      setName('');
      setAmount('');
      setDueDate(addDaysISO(todayISO(), 7));
      setCategory(BILL_CATEGORIES[0]);
      setPayee('');
      setMethod('M-Pesa');
      setPaid(false);
      setPaidDate('');
      setNotes('');
    }
  }, [editingBill, billModalOpen]);

  if (!billModalOpen) return null;

  const handlePaidToggle = (checked: boolean) => {
    setPaid(checked);
    if (checked && !paidDate) {
      setPaidDate(todayISO());
    } else if (!checked) {
      setPaidDate('');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanAmount = Math.round(num(amount));
    if (!name.trim()) {
      addToast(t('Tafadhali weka jina la bili.'), 'error');
      return;
    }
    if (!cleanAmount || cleanAmount <= 0) {
      addToast(t('Tafadhali weka kiasi cha bili.'), 'error');
      return;
    }

    const payload = {
      name: name.trim(),
      category,
      payee: payee.trim(),
      amount: cleanAmount,
      dueDate,
      paid,
      paidDate: paid ? (paidDate || todayISO()) : '',
      method,
      recurring: editingBill ? editingBill.recurring : false,
      notes: notes.trim()
    };

    if (editingBill) {
      updateBill(editingBill.id, payload);
    } else {
      addBill(payload);
    }

    closeBillModal();
  };

  return (
    <div className="premium-backdrop fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-[#081e16]/60 backdrop-blur-xs overflow-y-auto">
      <div className="premium-enter relative w-full max-w-lg bg-[#f3efe5] border border-white/20 rounded-[22px] shadow-[0_24px_70px_rgba(8,30,22,0.22)] overflow-hidden">
        {/* Head */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#e2dccd] bg-[#fcfaf4]/75">
          <div>
            <div className="text-[9px] font-bold tracking-[0.18em] uppercase text-[#1b5540]">
              {t('Malipo yanayokuja')}
            </div>
            <h2 className="font-display text-[20px] font-semibold text-[#14231c] tracking-tight">
              {editingBill ? t('Hariri bili') : t('Ongeza bili')}
            </h2>
          </div>
          <button
            onClick={closeBillModal}
            className="w-8 h-8 rounded-[9px] border border-[#e2dccd] bg-[#fcfaf4] text-[#746e7d] hover:text-[#14231c] hover:bg-white grid place-items-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit}>
          <div className="p-5 sm:p-6 space-y-3.5">
            <div>
              <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                {t('Jina la bili *')}
              </label>
              <input
                type="text"
                value={name}
                onChange={e => setName(e.target.value)}
                placeholder={t('Mfano: Kodi ya pango — Novemba au LUKU ya Ofisi')}
                className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                required
                autoFocus
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Kiasi (TZS) *')}
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[10px] font-bold text-[#746e7d]">
                    TZS
                  </span>
                  <input
                    type="number"
                    min="1"
                    step="any"
                    value={amount}
                    onChange={e => setAmount(e.target.value)}
                    placeholder="0"
                    className="w-full h-[38px] pl-10 pr-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono font-bold text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Tarehe ya mwisho ya malipo *')}
                </label>
                <input
                  type="date"
                  value={dueDate}
                  onChange={e => setDueDate(e.target.value)}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                  required
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Kundi la bili')}
                </label>
                <select
                  value={category}
                  onChange={e => setCategory(e.target.value)}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                >
                  {BILL_CATEGORIES.map(c => (
                    <option key={c} value={c}>
                      {c === 'Kodi ya pango' && t(c) !== 'Rent' ? `${t(c)} (Rent)` : t(c)}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Unamlipa nani? (Payee)')}
                </label>
                <input
                  type="text"
                  value={payee}
                  onChange={e => setPayee(e.target.value)}
                  placeholder={t('Jina la mwenye nyumba, TANESCO, n.k.')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Njia ya kulipa')}
                </label>
                <select
                  value={method}
                  onChange={e => setMethod(e.target.value)}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                >
                  {METHODS.map(m => (
                    <option key={m} value={m}>
                      {t(m)}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Tarehe iliyolipwa (kama tayari)')}
                </label>
                <input
                  type="date"
                  value={paidDate}
                  onChange={e => setPaidDate(e.target.value)}
                  disabled={!paid}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540] disabled:opacity-50"
                />
              </div>
            </div>

            {/* Paid status switch */}
            <div className="pt-2">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={paid}
                  onChange={e => handlePaidToggle(e.target.checked)}
                  className="w-4 h-4 rounded text-[#1b5540] focus:ring-[#1b5540] border-[#d2cab7]"
                />
                <span className="text-[12px] font-semibold text-[#14231c]">
                  {t('Bili hii imelipwa tayari')}
                </span>
              </label>
            </div>

            <div>
              <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                {t('Maelezo ya ziada (Hiari)')}
              </label>
              <textarea
                rows={2}
                value={notes}
                onChange={e => setNotes(e.target.value)}
                placeholder={t('Namba ya mita, akaunti ya benki, masharti ya bili...')}
                className="w-full p-2.5 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
              />
            </div>
          </div>

          {/* Foot */}
          <div className="flex items-center justify-end gap-2 px-6 py-3.5 border-t border-[#e2dccd] bg-[#fcfaf4]/75">
            <button
              type="button"
              onClick={closeBillModal}
              className="px-3.5 py-2 rounded-[10px] text-[12px] font-semibold text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c]"
            >
              {t('Ghairi')}
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold hover:bg-[#123a2c] shadow-[0_7px_17px_rgba(12,43,34,0.16)] transition-all"
            >
              {t('Hifadhi bili')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
