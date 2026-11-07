import React, { useState, useMemo } from 'react';
import {
  Plus,
  Search,
  Check,
  RotateCcw,
  Edit2,
  Trash2,
  Receipt,
  Clock,
  AlertTriangle,
  CheckCircle2
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  fmt,
  money,
  dateLabel,
  daysFromToday,
  billStatus,
  monthKey,
  todayISO,
  num
} from '../../utils/format';
import { Bill, BillFilterKey } from '../../types';

export const BillsPage: React.FC = () => {
  const {
    state,
    language,
    t,
    openBillModal,
    deleteBill,
    toggleBillPaid
  } = useApp();

  const [filter, setFilter] = useState<BillFilterKey>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Statistics
  const unpaidBills = state.bills.filter(b => !b.paid);
  const overdueBills = unpaidBills.filter(b => b.dueDate < todayISO());
  const currentMonthKey = monthKey(todayISO());
  const paidThisMonthBills = state.bills.filter(
    b => b.paid && b.paidDate && monthKey(b.paidDate) === currentMonthKey
  );

  const totalUnpaid = unpaidBills.reduce((s, b) => s + num(b.amount), 0);
  const totalOverdue = overdueBills.reduce((s, b) => s + num(b.amount), 0);
  const totalPaidThisMonth = paidThisMonthBills.reduce((s, b) => s + num(b.amount), 0);

  // Filtered bills
  const filteredBills = useMemo(() => {
    let list = state.bills.slice();

    if (filter === 'unpaid') {
      list = list.filter(b => !b.paid);
    } else if (filter === 'paid') {
      list = list.filter(b => b.paid);
    }

    const q = searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter(b =>
        [b.name, b.category, b.payee, b.notes]
          .join(' ')
          .toLowerCase()
          .includes(q)
      );
    }

    // Sort: unpaid first sorted by due date, then paid sorted by paid date
    return list.sort((a, b) => {
      if (a.paid !== b.paid) return a.paid ? 1 : -1;
      if (!a.paid) return a.dueDate.localeCompare(b.dueDate);
      return (b.paidDate || '').localeCompare(a.paidDate || '');
    });
  }, [state.bills, filter, searchQuery]);

  return (
    <div className="space-y-5 pb-12">
      {/* Page Heading & Action */}
      <div className="page-heading flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-[9px] font-bold tracking-[0.19em] uppercase text-[#1b5540]">
            <span className="w-4 h-[1px] bg-[#1b5540]/50" />
            {t('Malipo yanayokuja')}
          </div>
          <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
            {t('Bili')}
          </h1>
          <p className="text-[12.5px] text-[#746e7d] mt-1 max-w-xl">
            {t('Kumbuka kila bili na tarehe yake ya malipo — ili biashara isikwame kwa kusahau.')}
          </p>
        </div>

        <button
          onClick={() => openBillModal()}
          className="inline-flex items-center gap-1.5 h-[38px] px-4 rounded-[11px] bg-[#0c2b22] text-white font-semibold text-[12px] hover:bg-[#123a2c] transition-all shadow-[0_7px_17px_rgba(12,43,34,0.16)] self-start sm:self-auto"
        >
          <Plus className="w-4 h-4 stroke-[2.5]" />
          <span>{t('Ongeza bili')}</span>
        </button>
      </div>

      {/* Summary Strip (3 Stats) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
        <div className="p-4 rounded-[16px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_2px_8px_rgba(20,35,28,0.04)] hover:shadow-md transition-all flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-[12px] grid place-items-center bg-[#f8edd3] text-[#946510] shrink-0 shadow-xs">
            <Clock className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Hazijalipwa')}
            </div>
            <div className="text-[17px] font-bold text-[#14231c] font-mono-numbers mt-0.5">
              {money(totalUnpaid)}
            </div>
          </div>
        </div>

        <div className="p-4 rounded-[16px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_2px_8px_rgba(20,35,28,0.04)] hover:shadow-md transition-all flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-[12px] grid place-items-center bg-[#f7e4db] text-[#9e432b] shrink-0 shadow-xs">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Zimechelewa')}
            </div>
            <div className="text-[17px] font-bold text-[#9e432b] font-mono-numbers mt-0.5">
              {money(totalOverdue)}
            </div>
          </div>
        </div>

        <div className="p-4 rounded-[16px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_2px_8px_rgba(20,35,28,0.04)] hover:shadow-md transition-all flex items-center gap-3.5">
          <div className="w-10 h-10 rounded-[12px] grid place-items-center bg-[#e8f0e8] text-[#1b5540] shrink-0 shadow-xs">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <div className="text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Zilizolipwa mwezi huu')}
            </div>
            <div className="text-[17px] font-bold text-[#1b5540] font-mono-numbers mt-0.5">
              {money(totalPaidThisMonth)}
            </div>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Filter chips */}
        <div className="premium-segment" role="group" aria-label={t('Chuja bili')}>
          {(
            [
              { id: 'all', label: t('Zote') },
              { id: 'unpaid', label: t('Hazijalipwa') },
              { id: 'paid', label: t('Zimelipwa') }
            ] as { id: BillFilterKey; label: string }[]
          ).map(f => (
            <button
              key={f.id}
              onClick={() => setFilter(f.id)}
              aria-pressed={filter === f.id}
              className="premium-segment-option text-[11.5px] whitespace-nowrap"
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Search input */}
        <div className="relative w-full sm:w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#98a39b]" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder={t('Tafuta bili...')}
            className="w-full h-[38px] pl-9 pr-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] placeholder-[#98a39b] focus:outline-none focus:border-[#1b5540] transition-colors"
          />
        </div>
      </div>

      {/* Bills Table & Cards */}
      <div className="rounded-[18px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] overflow-hidden">
        {filteredBills.length === 0 ? (
          <div className="py-14 px-4 text-center">
            <div className="w-12 h-12 rounded-[14px] bg-[#e8f0e8] text-[#4b6f8b] grid place-items-center mx-auto mb-3">
              <Receipt className="w-6 h-6" />
            </div>
            <div className="text-[14px] font-bold text-[#14231c]">
              {t('Hakuna bili zinazolingana')}
            </div>
            <p className="text-[12px] text-[#746e7d] max-w-sm mx-auto mt-1 mb-4">
              {t('Bili mpya ya pango, umeme, mishahara au kodi ya biashara.')}
            </p>
            <button
              onClick={() => openBillModal()}
              className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold"
            >
              {t('Ongeza bili mpya')}
            </button>
          </div>
        ) : (
          <>
            {/* Desktop Table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-[#e2dccd] text-[9px] font-bold uppercase tracking-wider text-[#746e7d]">
                    <th className="py-3 px-4">Bili & Mlipwaji</th>
                    <th className="py-3 px-4">Kundi</th>
                    <th className="py-3 px-4">Tarehe ya Malipo</th>
                    <th className="py-3 px-4">Hali</th>
                    <th className="py-3 px-4 text-right">Kiasi (TZS)</th>
                    <th className="py-3 px-4 text-right">Vitendo</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#ece7da] text-[12px]">
                  {filteredBills.map(b => {
                    const st = billStatus(b);
                    const days = daysFromToday(b.dueDate);

                    return (
                      <tr key={b.id} className="hover:bg-[#f8f4ea] transition-colors">
                        <td className="py-3 px-4 min-w-[180px]">
                          <div className="font-semibold text-[#14231c]">{b.name}</div>
                          <div className="text-[10.5px] text-[#746e7d] flex items-center gap-1.5 mt-0.5">
                            <span>{b.payee || '—'}</span>
                            {b.recurring && (
                              <>
                                <span>·</span>
                                <span className="text-[#1b5540] font-semibold">kila mwezi</span>
                              </>
                            )}
                          </div>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap">
                          <span className="px-2 py-0.5 rounded-full bg-[#ece7da] text-[#34483e] text-[10px] font-semibold">
                            {t(b.category)}
                          </span>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap text-[#746e7d]">
                          <div className="font-medium text-[#14231c]">{dateLabel(b.dueDate, language)}</div>
                          <div className={`text-[10px] ${days < 0 && !b.paid ? 'text-[#9e432b] font-bold' : ''}`}>
                            {b.paid
                              ? `${t('Imelipwa')} ${dateLabel(b.paidDate, language)}`
                              : days < 0
                              ? `${t('Imechelewa siku')} ${Math.abs(days)}`
                              : days === 0
                              ? t('Leo')
                              : `${t('Siku')} ${days}`}
                          </div>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap">
                          <span
                            className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              st.key === 'paid'
                                ? 'bg-[#e8f0e8] text-[#1b5540]'
                                : st.key === 'due'
                                ? 'bg-[#f8edd3] text-[#946510]'
                                : st.key === 'overdue'
                                ? 'bg-[#f7e4db] text-[#9e432b]'
                                : 'bg-[#f3efe5] text-[#746e7d]'
                            }`}
                          >
                            <span className="w-1.5 h-1.5 rounded-full bg-current" />
                            {t(st.label)}
                          </span>
                        </td>

                        <td className="py-3 px-4 text-right whitespace-nowrap font-mono-numbers font-bold text-[13px] text-[#14231c]">
                          {fmt(b.amount)}
                        </td>

                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1">
                            <button
                              onClick={() => toggleBillPaid(b.id)}
                              title={b.paid ? t('Tendua malipo') : t('Weka imelipwa')}
                              className={`w-7 h-7 rounded-lg grid place-items-center transition-colors ${
                                b.paid
                                  ? 'text-[#746e7d] hover:bg-[#ece7da]'
                                  : 'text-[#1b5540] hover:bg-[#e8f0e8]'
                              }`}
                            >
                              {b.paid ? <RotateCcw className="w-3.5 h-3.5" /> : <Check className="w-3.5 h-3.5" />}
                            </button>

                            <button
                              onClick={() => openBillModal(b)}
                              title={t('Hariri bili')}
                              className="w-7 h-7 rounded-lg grid place-items-center text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c] transition-colors"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>

                            <button
                              onClick={() => deleteBill(b.id)}
                              title={t('Futa bili')}
                              className="w-7 h-7 rounded-lg grid place-items-center text-[#746e7d] hover:bg-[#f7e4db] hover:text-[#9e432b] transition-colors"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile Cards */}
            <div className="md:hidden divide-y divide-[#ece7da]">
              {filteredBills.map(b => {
                const st = billStatus(b);
                const days = daysFromToday(b.dueDate);

                return (
                  <div key={b.id} className="p-3.5 space-y-2">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="text-[13px] font-bold text-[#14231c]">
                          {b.name}
                        </div>
                        <div className="text-[10.5px] text-[#746e7d]">
                          {t(b.category)} · {b.payee || dateLabel(b.dueDate, language)}
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <div className="font-mono-numbers font-bold text-[13.5px] text-[#14231c]">
                          {fmt(b.amount)}
                          <span className="text-[9px] text-[#746e7d] ml-0.5">TZS</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-1 border-t border-[#ece7da]/60">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9.5px] font-bold ${
                          st.key === 'paid'
                            ? 'bg-[#e8f0e8] text-[#1b5540]'
                            : st.key === 'due'
                            ? 'bg-[#f8edd3] text-[#946510]'
                            : st.key === 'overdue'
                            ? 'bg-[#f7e4db] text-[#9e432b]'
                            : 'bg-[#ece7da] text-[#746e7d]'
                        }`}
                      >
                        {t(st.label)}
                      </span>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => toggleBillPaid(b.id)}
                          className="px-2.5 py-1 rounded bg-[#e8f0e8] text-[#1b5540] text-[10.5px] font-bold inline-flex items-center gap-1"
                        >
                          {b.paid ? <RotateCcw className="w-3 h-3" /> : <Check className="w-3 h-3" />}
                          <span>{b.paid ? 'Tendua' : 'Lipa'}</span>
                        </button>

                        <button
                          onClick={() => openBillModal(b)}
                          className="w-7 h-7 rounded grid place-items-center text-[#746e7d] hover:bg-[#ece7da]"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => deleteBill(b.id)}
                          className="w-7 h-7 rounded grid place-items-center text-[#9e432b] hover:bg-[#f7e4db]"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Table Foot */}
            <div className="p-3.5 px-4 bg-[#fcfaf4] border-t border-[#e2dccd] flex items-center justify-between text-[11px] text-[#746e7d]">
              <div>
                Inaonyesha <strong className="text-[#14231c]">{filteredBills.length}</strong> kati ya {state.bills.length} bili
              </div>
              <div>
                {overdueBills.length > 0 ? (
                  <span className="text-[#9e432b] font-bold">{overdueBills.length} zimechelewa</span>
                ) : (
                  <span>Hakuna bili iliyochelewa</span>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
