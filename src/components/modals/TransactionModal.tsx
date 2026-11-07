import React, { useState, useEffect } from 'react';
import { X, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { TransactionType } from '../../types';
import {
  todayISO,
  INCOME_CATEGORIES,
  EXPENSE_CATEGORIES,
  METHODS,
  num
} from '../../utils/format';

export const TransactionModal: React.FC = () => {
  const {
    txModalOpen,
    editingTx,
    defaultTxType,
    closeTxModal,
    addTransaction,
    updateTransaction,
    addToast,
    t
  } = useApp();

  const [type, setType] = useState<TransactionType>(defaultTxType);
  const [amount, setAmount] = useState<string>('');
  const [date, setDate] = useState<string>(todayISO());
  const [description, setDescription] = useState<string>('');
  const [party, setParty] = useState<string>('');
  const [category, setCategory] = useState<string>(INCOME_CATEGORIES[0]);
  const [method, setMethod] = useState<string>('M-Pesa');
  const [reference, setReference] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [isPending, setIsPending] = useState<boolean>(false);

  useEffect(() => {
    if (editingTx) {
      setType(editingTx.type);
      setAmount(String(editingTx.amount));
      setDate(editingTx.date);
      setDescription(editingTx.description);
      setParty(editingTx.party || '');
      setCategory(editingTx.category);
      setMethod(editingTx.method || 'M-Pesa');
      setReference(editingTx.reference || '');
      setNotes(editingTx.notes || '');
      setIsPending((editingTx.reference || '').includes('INASUBIRI') || (editingTx.notes || '').includes('INASUBIRI'));
    } else {
      setType(defaultTxType);
      setAmount('');
      setDate(todayISO());
      setDescription('');
      setParty('');
      setCategory(defaultTxType === 'income' ? INCOME_CATEGORIES[0] : EXPENSE_CATEGORIES[0]);
      setMethod(defaultTxType === 'income' ? 'M-Pesa' : 'Cash');
      setReference('');
      setNotes('');
      setIsPending(false);
    }
  }, [editingTx, defaultTxType, txModalOpen]);

  const handleTypeChange = (newType: TransactionType) => {
    setType(newType);
    if (!editingTx) {
      setCategory(newType === 'income' ? INCOME_CATEGORIES[0] : EXPENSE_CATEGORIES[0]);
      setMethod(newType === 'income' ? 'M-Pesa' : 'Cash');
    }
  };

  if (!txModalOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanAmount = Math.round(num(amount));
    if (!cleanAmount || cleanAmount <= 0) {
      addToast(t('Tafadhali weka kiasi kikubwa kuliko sifuri.'), 'error');
      return;
    }
    if (!description.trim()) {
      addToast(t('Tafadhali weka maelezo ya miamala.'), 'error');
      return;
    }

    const finalRef = isPending
      ? (reference.trim() ? `${reference.trim()} [INASUBIRI]` : '[INASUBIRI_MALIPO]')
      : reference.trim();

    const payload = {
      type,
      date: date || todayISO(),
      description: description.trim(),
      party: party.trim(),
      category,
      amount: cleanAmount,
      method,
      reference: finalRef,
      notes: notes.trim(),
      source: (editingTx ? editingTx.source : 'manual') as 'manual',
      linkedId: editingTx ? editingTx.linkedId : null
    };

    if (editingTx) {
      updateTransaction(editingTx.id, payload);
    } else {
      addTransaction(payload);
    }

    closeTxModal();
  };

  const categories = type === 'income' ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

  return (
    <div className="premium-backdrop fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-[#081e16]/60 backdrop-blur-xs overflow-y-auto">
      <div className="premium-enter relative w-full max-w-[520px] max-h-[90vh] flex flex-col bg-[#f3efe5] border border-white/20 rounded-[22px] shadow-[0_24px_70px_rgba(8,30,22,0.22)] overflow-hidden">
        {/* Head */}
        <div className="flex items-center justify-between px-5 sm:px-6 py-4 border-b border-[#e2dccd] bg-[#fcfaf4]/75 shrink-0">
          <div>
            <div className="text-[9px] font-bold tracking-[0.18em] uppercase text-[#1b5540]">
              {t('Daftari la fedha')}
            </div>
            <h2 className="font-display text-[20px] font-semibold text-[#14231c] tracking-tight">
              {editingTx ? t('Hariri miamala') : t('Ongeza miamala')}
            </h2>
          </div>
          <button
            onClick={closeTxModal}
            className="w-8 h-8 rounded-[9px] border border-[#e2dccd] bg-[#fcfaf4] text-[#746e7d] hover:text-[#14231c] hover:bg-white grid place-items-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="overflow-y-auto flex-1">
          <div className="p-4 sm:p-6 space-y-4">
            {/* Type Toggle */}
            <div className="grid grid-cols-2 gap-1.5 p-1 bg-[#ece7da]/70 border border-[#e2dccd] rounded-[11px]">
              <button
                type="button"
                onClick={() => handleTypeChange('income')}
                className={`py-2 px-3 rounded-[8px] flex items-center justify-center gap-1.5 text-[12px] font-semibold transition-all ${
                  type === 'income'
                    ? 'bg-white text-[#1b5540] shadow-xs'
                    : 'text-[#746e7d] hover:text-[#14231c]'
                }`}
              >
                <ArrowUpRight className="w-4 h-4" />
                <span>{t('Fedha ndani (Mapato)')}</span>
              </button>

              <button
                type="button"
                onClick={() => handleTypeChange('expense')}
                className={`py-2 px-3 rounded-[8px] flex items-center justify-center gap-1.5 text-[12px] font-semibold transition-all ${
                  type === 'expense'
                    ? 'bg-white text-[#9e432b] shadow-xs'
                    : 'text-[#746e7d] hover:text-[#14231c]'
                }`}
              >
                <ArrowDownLeft className="w-4 h-4" />
                <span>{t('Fedha nje (Matumizi)')}</span>
              </button>
            </div>

            {/* Payment Status Segment: Imekamilika vs Inasubiri Malipo */}
            <div>
              <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                {t('Hali ya Malipo')}
              </label>
              <div className="grid grid-cols-2 gap-1.5 p-1 bg-[#f8f4ea] border border-[#d2cab7] rounded-[11px]">
                <button
                  type="button"
                  onClick={() => setIsPending(false)}
                  className={`py-1.5 px-2.5 rounded-[8px] text-[11px] font-semibold transition-all ${
                    !isPending
                      ? 'bg-[#0c2b22] text-[#d8f36b] shadow-xs'
                      : 'text-[#746e7d] hover:text-[#14231c]'
                  }`}
                >
                  ✓ {t('Imekamilika (Tayari Imelipwa)')}
                </button>
                <button
                  type="button"
                  onClick={() => setIsPending(true)}
                  className={`py-1.5 px-2.5 rounded-[8px] text-[11px] font-semibold transition-all ${
                    isPending
                      ? 'bg-[#c58a21] text-white shadow-xs'
                      : 'text-[#746e7d] hover:text-[#14231c]'
                  }`}
                >
                  ⏳ {t('Inasubiri Malipo (Baada ya kazi)')}
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Amount */}
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
                    className="w-full h-[40px] pl-10 pr-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono font-bold text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    required
                    autoFocus
                  />
                </div>
              </div>

              {/* Date */}
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Tarehe *')}
                </label>
                <input
                  type="date"
                  value={date}
                  onChange={e => setDate(e.target.value)}
                  className="w-full h-[40px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                  required
                />
              </div>

              {/* Description */}
              <div className="sm:col-span-2">
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Maelezo ya miamala *')}
                </label>
                <input
                  type="text"
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  placeholder={t('Mfano: Mauzo ya vifaa vya ofisi au Malipo ya umeme')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                  required
                />
              </div>

              {/* Party */}
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {type === 'income' ? t('Mteja / Mlipaji') : t('Msambazaji / Mlipwaji')}
                </label>
                <input
                  type="text"
                  value={party}
                  onChange={e => setParty(e.target.value)}
                  placeholder={t('Jina la mtu au kampuni')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              {/* Category */}
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Kundi')}
                </label>
                <select
                  value={category}
                  onChange={e => setCategory(e.target.value)}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                >
                  {categories.map(c => (
                    <option key={c} value={c}>
                      {t(c)}
                    </option>
                  ))}
                </select>
              </div>

              {/* Method */}
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Njia ya malipo')}
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

              {/* Reference */}
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Namba ya kumbukumbu (Ref)')}
                </label>
                <input
                  type="text"
                  value={reference}
                  onChange={e => setReference(e.target.value)}
                  placeholder={t('Mfano: MP-9941 au CRDB-201')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              {/* Notes */}
              <div className="sm:col-span-2">
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Maelezo ya ziada (Hiari)')}
                </label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                  placeholder={t('Maelezo yoyote mengine kuhusu miamala hii...')}
                  className="w-full p-2.5 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>
            </div>
          </div>

          {/* Foot */}
          <div className="flex items-center justify-between px-6 py-3.5 border-t border-[#e2dccd] bg-[#fcfaf4]/75">
            <span className="text-[10px] text-[#746e7d] hidden sm:inline">
              {t('Miamala hii itaonekana kwenye dashibodi mara moja.')}
            </span>
            <div className="flex items-center gap-2 ml-auto">
              <button
                type="button"
                onClick={closeTxModal}
                className="px-3.5 py-2 rounded-[10px] text-[12px] font-semibold text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c]"
              >
                {t('Ghairi')}
              </button>
              <button
                type="submit"
                className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold hover:bg-[#123a2c] shadow-[0_7px_17px_rgba(12,43,34,0.16)] transition-all"
              >
                {t('Hifadhi miamala')}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
