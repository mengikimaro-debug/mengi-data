import React, { useState, useMemo } from 'react';
import {
  Download,
  Plus,
  Search,
  ArrowUpRight,
  ArrowDownLeft,
  Edit2,
  Trash2,
  ExternalLink,
  Wallet
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  fmt,
  money,
  dateLabel,
  monthKey,
  monthLabel,
  lastMonths,
  sortTxNewest,
  num
} from '../../utils/format';
import { exportTransactionsCSV } from '../../utils/export';
import { Transaction, TransactionFilterType } from '../../types';

export const TransactionsPage: React.FC = () => {
  const {
    state,
    language,
    t,
    openTxModal,
    deleteTransaction,
    openInvoiceView,
    openBillModal
  } = useApp();
  const tr = t;

  const [typeFilter, setTypeFilter] = useState<TransactionFilterType>('all');
  const [monthFilter, setMonthFilter] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Collect months list
  const monthOptions = useMemo(() => {
    const list = lastMonths(18, language);
    const seen = new Set(list.map(m => m.key));
    state.transactions.forEach(t => {
      const k = monthKey(t.date);
      if (k && !seen.has(k)) {
        seen.add(k);
        list.push({
          key: k,
          label: k,
          full: monthLabel(k, language),
          year: parseInt(k.slice(0, 4)),
          start: new Date(),
          end: new Date()
        });
      }
    });
    return list;
  }, [state.transactions, language]);

  // Filtered transactions
  const filteredTransactions = useMemo(() => {
    let result = state.transactions.slice();

    if (monthFilter !== 'all') {
      result = result.filter(t => monthKey(t.date) === monthFilter);
    }

    if (typeFilter === 'pending') {
      result = result.filter(t => (t.reference || '').includes('INASUBIRI') || (t.notes || '').includes('INASUBIRI'));
    } else if (typeFilter !== 'all') {
      result = result.filter(t => t.type === typeFilter);
    }

    const q = searchQuery.trim().toLowerCase();
    if (q) {
      result = result.filter(t =>
        [
          t.description,
          t.party,
          t.category,
          t.method,
          t.reference,
          String(t.amount)
        ]
          .join(' ')
          .toLowerCase()
          .includes(q)
      );
    }

    return result.sort(sortTxNewest);
  }, [state.transactions, monthFilter, typeFilter, searchQuery]);

  // Totals for filtered set
  const totalIn = filteredTransactions
    .filter(t => t.type === 'income')
    .reduce((s, t) => s + num(t.amount), 0);
  const totalOut = filteredTransactions
    .filter(t => t.type === 'expense')
    .reduce((s, t) => s + num(t.amount), 0);
  const totalNet = totalIn - totalOut;

  const handleSourceClick = (t: Transaction) => {
    if (t.source === 'invoice' && t.linkedId) {
      const inv = state.invoices.find(i => i.id === t.linkedId);
      if (inv) openInvoiceView(inv);
    } else if (t.source === 'bill' && t.linkedId) {
      const bill = state.bills.find(b => b.id === t.linkedId);
      if (bill) openBillModal(bill);
    }
  };

  const hasActiveFilters = typeFilter !== 'all' || monthFilter !== 'all' || searchQuery.trim() !== '';

  const clearFilters = () => {
    setTypeFilter('all');
    setMonthFilter('all');
    setSearchQuery('');
  };

  return (
    <div className="space-y-5 pb-12">
      {/* Page Heading & Actions */}
      <div className="page-heading flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-[9px] font-bold tracking-[0.19em] uppercase text-[#1b5540]">
            <span className="w-4 h-[1px] bg-[#1b5540]/50" />
            {t('Daftari la fedha')}
          </div>
          <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
            {t('Miamala')}
          </h1>
          <p className="text-[12.5px] text-[#746e7d] mt-1 max-w-xl">
            {t('Kila shilingi inayoingia na kutoka — kwenye rekodi moja safi na rahisi kufuatilia.')}
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <button
            onClick={() => exportTransactionsCSV(filteredTransactions)}
            className="inline-flex items-center gap-1.5 h-[38px] px-3.5 rounded-[11px] bg-[#fcfaf4] border border-[#d2cab7] text-[#14231c] font-semibold text-[12px] hover:bg-white transition-all shadow-xs"
          >
            <Download className="w-4 h-4 text-[#746e7d]" />
            <span>CSV</span>
          </button>

          <button
            onClick={() => openTxModal('income')}
            className="inline-flex items-center gap-1.5 h-[38px] px-4 rounded-[11px] bg-[#0c2b22] text-white font-semibold text-[12px] hover:bg-[#123a2c] transition-all shadow-[0_7px_17px_rgba(12,43,34,0.16)]"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>{t('Ongeza miamala')}</span>
          </button>
        </div>
      </div>

      {/* Summary Strip (3 Stats) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="p-3.5 px-4 rounded-[14px] bg-[#fcfaf4] border border-[#e2dccd] flex items-center gap-3">
          <div className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#e8f0e8] text-[#1b5540] shrink-0">
            <ArrowUpRight className="w-4 h-4" />
          </div>
          <div>
            <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Fedha ndani')}
            </div>
            <div className="text-[16px] font-bold text-[#14231c] font-mono-numbers">
              {money(totalIn)}
            </div>
          </div>
        </div>

        <div className="p-3.5 px-4 rounded-[14px] bg-[#fcfaf4] border border-[#e2dccd] flex items-center gap-3">
          <div className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#f7e4db] text-[#9e432b] shrink-0">
            <ArrowDownLeft className="w-4 h-4" />
          </div>
          <div>
            <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Fedha nje')}
            </div>
            <div className="text-[16px] font-bold text-[#14231c] font-mono-numbers">
              {money(totalOut)}
            </div>
          </div>
        </div>

        <div className="p-3.5 px-4 rounded-[14px] bg-[#fcfaf4] border border-[#e2dccd] flex items-center gap-3">
          <div className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#e8f0e8] text-[#4b6f8b] shrink-0">
            <Wallet className="w-4 h-4" />
          </div>
          <div>
            <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Salio la kipindi')}
            </div>
            <div className={`text-[16px] font-bold font-mono-numbers ${totalNet >= 0 ? 'text-[#1b5540]' : 'text-[#9e432b]'}`}>
              {money(totalNet)}
            </div>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Type tabs */}
        <div className="premium-segment" role="group" aria-label={t('Chuja miamala')}>
          {(
            [
              { id: 'all', label: t('Zote') },
              { id: 'income', label: t('Fedha ndani') },
              { id: 'expense', label: t('Fedha nje') },
              { id: 'pending', label: t('Inasubiri (Baada ya kazi)') }
            ] as { id: TransactionFilterType; label: string }[]
          ).map(f => (
            <button
              key={f.id}
              onClick={() => setTypeFilter(f.id)}
              aria-pressed={typeFilter === f.id}
              className="premium-segment-option text-[11.5px] whitespace-nowrap"
            >
              {f.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2.5 w-full sm:w-auto">
          {/* Month selector */}
          <select
            value={monthFilter}
            onChange={e => setMonthFilter(e.target.value)}
            className="h-[38px] px-3 pr-8 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] font-medium appearance-none focus:outline-none focus:border-[#1b5540]"
            style={{
              backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%236d7b72' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E")`,
              backgroundRepeat: 'no-repeat',
              backgroundPosition: 'right 10px center'
            }}
          >
            <option value="all">{t('Miezi yote')}</option>
            {monthOptions.map(m => (
              <option key={m.key} value={m.key}>
                {m.full}
              </option>
            ))}
          </select>

          {/* Search Input */}
          <div className="relative flex-1 sm:w-[220px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#98a39b]" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder={t('Tafuta maelezo, mteja...')}
              className="w-full h-[38px] pl-9 pr-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] placeholder-[#98a39b] focus:outline-none focus:border-[#1b5540] transition-colors"
            />
          </div>
        </div>
      </div>

      {/* Transactions Table & Mobile Cards */}
      <div className="rounded-[18px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] overflow-hidden">
        {filteredTransactions.length === 0 ? (
          <div className="py-14 px-4 text-center">
            <div className="w-12 h-12 rounded-[14px] bg-[#e8f0e8] text-[#1b5540] grid place-items-center mx-auto mb-3">
              <Wallet className="w-6 h-6" />
            </div>
            <div className="text-[14px] font-bold text-[#14231c]">
              {t('Hakuna miamala inayolingana')}
            </div>
            <p className="text-[12px] text-[#746e7d] max-w-sm mx-auto mt-1 mb-4">
              {hasActiveFilters
                ? t('Hakuna rekodi inayolingana na vichujio ulivyochagua. Jaribu kubadilisha vigezo vya utafutaji.')
                : t('Bado hujaongeza miamala. Anza kwa kurekodi mauzo au matumizi ya kwanza.')}
            </p>
            {hasActiveFilters ? (
              <button
                onClick={clearFilters}
                className="px-3.5 py-1.5 rounded-[9px] bg-[#f3efe5] border border-[#d2cab7] text-[11px] font-bold text-[#14231c] hover:bg-white"
              >
                {t('Ondoa vichujio vyote')}
              </button>
            ) : (
              <button
                onClick={() => openTxModal('income')}
                className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold"
              >
                {t('Ongeza miamala')}
              </button>
            )}
          </div>
        ) : (
          <>
            {/* Desktop Table View */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-[#e2dccd] text-[9px] font-bold uppercase tracking-wider text-[#746e7d]">
                    <th className="py-3 px-4">{t('Tarehe')}</th>
                            <th className="py-3 px-4">{t('Maelezo & Mhusika')}</th>
                            <th className="py-3 px-4">{t('Kundi')}</th>
                            <th className="py-3 px-4">{t('Njia')}</th>
                            <th className="py-3 px-4 text-right">{t('Kiasi (TZS)')}</th>
                            <th className="py-3 px-4 text-right">{t('Vitendo')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#ece7da] text-[12px]">
                  {filteredTransactions.map(t => {
                    const isInc = t.type === 'income';
                    const isManual = t.source === 'manual';

                    return (
                      <tr key={t.id} className="hover:bg-[#f8f4ea] transition-colors">
                        <td className="py-3 px-4 whitespace-nowrap text-[#746e7d]">
                          <div className="font-semibold text-[#14231c]">{dateLabel(t.date, language)}</div>
                          <div className="text-[10px] text-[#98a39b]">{t.date}</div>
                        </td>

                        <td className="py-3 px-4 min-w-[200px]">
                          <div className="font-semibold text-[#14231c] leading-tight">
                            {t.description}
                          </div>
                          <div className="text-[10.5px] text-[#746e7d] flex items-center gap-1.5 mt-0.5">
                            <span>{t.party || '—'}</span>
                            {t.reference && (
                              <>
                                <span>·</span>
                                <span className="font-mono text-[9.5px]">{t.reference}</span>
                              </>
                            )}
                          </div>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap">
                          <span className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                            isInc ? 'bg-[#e8f0e8] text-[#1b5540]' : 'bg-[#ece7da] text-[#34483e]'
                          }`}>
                            {tr(t.category)}
                          </span>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap text-[#746e7d] text-[11px]">
                          {t.method}
                        </td>

                        <td className={`py-3 px-4 text-right whitespace-nowrap font-mono-numbers font-bold text-[13px] ${
                          isInc ? 'text-[#1b5540]' : 'text-[#9e432b]'
                        }`}>
                          {isInc ? '+' : '−'}{fmt(t.amount)}
                        </td>

                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1">
                            {!isManual ? (
                              <button
                                onClick={() => handleSourceClick(t)}
                                title={t.source === 'invoice' ? tr('Fungua ankara husika') : tr('Fungua bili husika')}
                                className="w-7 h-7 rounded-lg grid place-items-center text-[#1b5540] hover:bg-[#e8f0e8] transition-colors"
                              >
                                <ExternalLink className="w-3.5 h-3.5" />
                              </button>
                            ) : (
                              <>
                                <button
                                  onClick={() => openTxModal(t.type, t)}
                                  title={tr('Hariri miamala')}
                                  className="w-7 h-7 rounded-lg grid place-items-center text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c] transition-colors"
                                >
                                  <Edit2 className="w-3.5 h-3.5" />
                                </button>
                                <button
                                  onClick={() => deleteTransaction(t.id)}
                                  title={tr('Futa miamala')}
                                  className="w-7 h-7 rounded-lg grid place-items-center text-[#746e7d] hover:bg-[#f7e4db] hover:text-[#9e432b] transition-colors"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile Cards View */}
            <div className="md:hidden divide-y divide-[#ece7da]">
              {filteredTransactions.map(t => {
                const isInc = t.type === 'income';
                const isManual = t.source === 'manual';

                return (
                  <div key={t.id} className="p-3.5 space-y-2">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="text-[12.5px] font-bold text-[#14231c] leading-snug">
                          {t.description}
                        </div>
                        <div className="text-[10.5px] text-[#746e7d] mt-0.5">
                          {dateLabel(t.date, language)} · {t.party || t.category}
                        </div>
                      </div>
                      <div className={`text-right shrink-0 font-mono-numbers font-bold text-[13.5px] ${
                        isInc ? 'text-[#1b5540]' : 'text-[#9e432b]'
                      }`}>
                        {isInc ? '+' : '−'}{fmt(t.amount)}
                        <span className="text-[9px] text-[#746e7d] ml-0.5">TZS</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-1 border-t border-[#ece7da]/60 text-[10.5px] text-[#746e7d]">
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 rounded-full bg-[#ece7da] font-medium text-[9.5px]">
                          {tr(t.category)}
                        </span>
                        <span>{t.method}</span>
                      </div>

                      <div className="flex items-center gap-1">
                        {!isManual ? (
                          <button
                            onClick={() => handleSourceClick(t)}
                            className="px-2 py-1 rounded bg-[#e8f0e8] text-[#1b5540] text-[10px] font-semibold inline-flex items-center gap-1"
                          >
                            <span>{tr('Chanzo')}</span>
                            <ExternalLink className="w-3 h-3" />
                          </button>
                        ) : (
                          <>
                            <button
                              onClick={() => openTxModal(t.type, t)}
                              className="w-7 h-7 rounded grid place-items-center text-[#746e7d] hover:bg-[#ece7da]"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() => deleteTransaction(t.id)}
                              className="w-7 h-7 rounded grid place-items-center text-[#9e432b] hover:bg-[#f7e4db]"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Table Footer */}
            <div className="p-3.5 px-4 bg-[#fcfaf4] border-t border-[#e2dccd] flex items-center justify-between text-[11px] text-[#746e7d]">
              <div>
                {t('Inaonyesha')} <strong className="text-[#14231c]">{filteredTransactions.length}</strong> {t('kati ya')} {state.transactions.length} {t('miamala')}
              </div>
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="font-bold text-[#1b5540] hover:underline"
                >
                  {t('Ondoa vichujio')}
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};
