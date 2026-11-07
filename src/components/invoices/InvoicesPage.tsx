import React, { useState, useMemo } from 'react';
import {
  Plus,
  Search,
  Eye,
  Download,
  Edit2,
  Check,
  Trash2,
  FileText,
  Clock,
  CheckCircle2
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  fmt,
  money,
  dateLabel,
  initials,
  invoiceMath,
  invoiceStatus,
  num
} from '../../utils/format';
import { buildInvoicePDF } from '../../utils/pdfGenerator';
import { downloadBlob } from '../../utils/export';
import { Invoice, InvoiceFilterKey } from '../../types';

export const InvoicesPage: React.FC = () => {
  const {
    state,
    language,
    t,
    openInvoiceBuilder,
    openInvoiceView,
    deleteInvoice,
    markInvoicePaid,
    addToast
  } = useApp();

  const [filter, setFilter] = useState<InvoiceFilterKey>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Totals calculations across all non-draft invoices
  const totals = useMemo(() => {
    let issued = 0;
    let paid = 0;
    let due = 0;

    state.invoices.forEach(inv => {
      if (inv.state === 'draft') return;
      const m = invoiceMath(inv);
      issued += m.total;
      paid += m.paid;
      due += m.balance;
    });

    return { issued, paid, due };
  }, [state.invoices]);

  // Filtered invoices
  const filteredInvoices = useMemo(() => {
    let list = state.invoices.slice();

    if (filter === 'draft') {
      list = list.filter(i => i.state === 'draft');
    } else if (filter === 'paid') {
      list = list.filter(i => i.state !== 'draft' && invoiceMath(i).balance <= 0);
    } else if (filter === 'due') {
      list = list.filter(i => i.state !== 'draft' && invoiceMath(i).balance > 0);
    }

    const q = searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter(i =>
        [
          i.number,
          i.customerName,
          i.customerPhone,
          i.customerEmail,
          i.customerAddress,
          i.notes
        ]
          .join(' ')
          .toLowerCase()
          .includes(q)
      );
    }

    // Sort newest issue date first
    return list.sort((a, b) => {
      const byDate = b.issueDate.localeCompare(a.issueDate);
      if (byDate !== 0) return byDate;
      return b.number.localeCompare(a.number);
    });
  }, [state.invoices, filter, searchQuery]);

  const handleDownloadPDF = (inv: Invoice) => {
    try {
      const blob = buildInvoicePDF(inv, state.settings);
      const safeName = (`Ankara-${inv.number}-${inv.customerName}`)
        .replace(/[^a-z0-9\-_]+/gi, '-')
        .replace(/-+/g, '-');
      downloadBlob(blob, `${safeName}.pdf`);
      addToast(`${t('Ankara')} ${inv.number} ${t('imepakuliwa kama PDF.')}`);
    } catch (e) {
      console.error(e);
      addToast('PDF haikuweza kuundwa. Tafadhali jaribu tena.', 'error');
    }
  };

  const overdueCount = state.invoices.filter(i => {
    if (i.state === 'draft') return false;
    return invoiceMath(i).balance > 0 && i.dueDate < new Date().toISOString().slice(0, 10);
  }).length;

  return (
    <div className="space-y-5 pb-12">
      {/* Page Heading & Action */}
      <div className="page-heading flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-[9px] font-bold tracking-[0.19em] uppercase text-[#1b5540]">
            <span className="w-4 h-[1px] bg-[#1b5540]/50" />
            {t('Ankara & malipo')}
          </div>
          <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
            {t('Ankara')}
          </h1>
          <p className="text-[12.5px] text-[#746e7d] mt-1 max-w-xl">
            {t('Tengeneza ankara za kitaalamu, pakua PDF, na fuatilia kila malipo kwa urahisi.')}
          </p>
        </div>

        <button
          onClick={() => openInvoiceBuilder()}
          className="inline-flex items-center gap-1.5 h-[38px] px-4 rounded-[11px] bg-[#0c2b22] text-white font-semibold text-[12px] hover:bg-[#123a2c] transition-all shadow-[0_7px_17px_rgba(12,43,34,0.16)] self-start sm:self-auto"
        >
          <Plus className="w-4 h-4 stroke-[2.5]" />
          <span>{t('Ankara mpya')}</span>
        </button>
      </div>

      {/* Summary Strip (3 Stats) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="p-3.5 px-4 rounded-[14px] bg-[#fcfaf4] border border-[#e2dccd] flex items-center gap-3">
          <div className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#e8f0e8] text-[#1b5540] shrink-0">
            <FileText className="w-4 h-4" />
          </div>
          <div>
            <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Jumla iliyotolewa')}
            </div>
            <div className="text-[16px] font-bold text-[#14231c] font-mono-numbers">
              {money(totals.issued)}
            </div>
          </div>
        </div>

        <div className="p-3.5 px-4 rounded-[14px] bg-[#fcfaf4] border border-[#e2dccd] flex items-center gap-3">
          <div className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#e8f0e8] text-[#1b5540] shrink-0">
            <CheckCircle2 className="w-4 h-4" />
          </div>
          <div>
            <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Iliyolipwa')}
            </div>
            <div className="text-[16px] font-bold text-[#1b5540] font-mono-numbers">
              {money(totals.paid)}
            </div>
          </div>
        </div>

        <div className="p-3.5 px-4 rounded-[14px] bg-[#fcfaf4] border border-[#e2dccd] flex items-center gap-3">
          <div className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#f8edd3] text-[#946510] shrink-0">
            <Clock className="w-4 h-4" />
          </div>
          <div>
            <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
              {t('Inayodaiwa')}
            </div>
            <div className="text-[16px] font-bold text-[#946510] font-mono-numbers">
              {money(totals.due)}
            </div>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Filter chips */}
        <div className="premium-segment" role="group" aria-label={t('Chuja ankara')}>
          {(
            [
              { id: 'all', label: t('Zote') },
              { id: 'due', label: t('Zinazodaiwa') },
              { id: 'paid', label: t('Zimelipwa') },
              { id: 'draft', label: t('Rasimu') }
            ] as { id: InvoiceFilterKey; label: string }[]
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

        {/* Search Input */}
        <div className="relative w-full sm:w-[260px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#98a39b]" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder={t('Tafuta mteja au namba...')}
            className="w-full h-[38px] pl-9 pr-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] placeholder-[#98a39b] focus:outline-none focus:border-[#1b5540] transition-colors"
          />
        </div>
      </div>

      {/* Invoices List */}
      <div className="rounded-[18px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] overflow-hidden">
        {filteredInvoices.length === 0 ? (
          <div className="py-14 px-4 text-center">
            <div className="w-12 h-12 rounded-[14px] bg-[#f8edd3] text-[#946510] grid place-items-center mx-auto mb-3">
              <FileText className="w-6 h-6" />
            </div>
            <div className="text-[14px] font-bold text-[#14231c]">
              Hakuna ankara zinazolingana
            </div>
            <p className="text-[12px] text-[#746e7d] max-w-sm mx-auto mt-1 mb-4">
              Tengeneza ankara mpya kwa mteja au ubadilishe kichujio kilichochaguliwa.
            </p>
            <button
              onClick={() => openInvoiceBuilder()}
              className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold"
            >
              Tengeneza ankara mpya
            </button>
          </div>
        ) : (
          <>
            {/* Desktop Table */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-[#e2dccd] text-[9px] font-bold uppercase tracking-wider text-[#746e7d]">
                    <th className="py-3 px-4">Ankara</th>
                    <th className="py-3 px-4">Mteja</th>
                    <th className="py-3 px-4">Tarehe</th>
                    <th className="py-3 px-4">Hali</th>
                    <th className="py-3 px-4 text-right">Kiasi</th>
                    <th className="py-3 px-4 text-right">Salio</th>
                    <th className="py-3 px-4 text-right">Vitendo</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#ece7da] text-[12px]">
                  {filteredInvoices.map(inv => {
                    const m = invoiceMath(inv);
                    const st = invoiceStatus(inv);
                    const isDraft = inv.state === 'draft';

                    return (
                      <tr key={inv.id} className="hover:bg-[#f8f4ea] transition-colors">
                        <td className="py-3 px-4 whitespace-nowrap">
                          <div className="font-bold text-[#14231c]">{inv.number}</div>
                          <div className="text-[10px] text-[#746e7d]">
                            {isDraft ? t('Rasimu') : `${t('Imetolewa')} ${dateLabel(inv.issueDate, language)}`}
                          </div>
                        </td>

                        <td className="py-3 px-4 min-w-[180px]">
                          <div className="flex items-center gap-2.5">
                            <div className="w-7 h-7 rounded-[8px] bg-[#f8f4ea] border border-[#e2dccd] text-[#1b5540] font-bold text-[10px] grid place-items-center shrink-0">
                              {initials(inv.customerName)}
                            </div>
                            <div className="min-w-0">
                              <div className="font-semibold text-[#14231c] truncate">
                                {inv.customerName}
                              </div>
                              <div className="text-[10px] text-[#746e7d] truncate">
                                {inv.customerPhone || inv.customerEmail || '—'}
                              </div>
                            </div>
                          </div>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap text-[#746e7d]">
                          <div className="font-medium text-[#14231c]">{dateLabel(inv.issueDate, language)}</div>
                          <div className="text-[10px]">{t('Malipo:')} {dateLabel(inv.dueDate, language)}</div>
                        </td>

                        <td className="py-3 px-4 whitespace-nowrap">
                          <span
                            className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              st.key === 'paid'
                                ? 'bg-[#e8f0e8] text-[#1b5540]'
                                : st.key === 'partial'
                                ? 'bg-[#f8edd3] text-[#946510]'
                                : st.key === 'overdue'
                                ? 'bg-[#f7e4db] text-[#9e432b]'
                                : st.key === 'draft'
                                ? 'bg-[#ece7da] text-[#746e7d]'
                                : 'bg-[#f3efe5] text-[#746e7d]'
                            }`}
                          >
                            <span className="w-1.5 h-1.5 rounded-full bg-current" />
                            {st.label}
                          </span>
                        </td>

                        <td className="py-3 px-4 text-right whitespace-nowrap font-mono-numbers font-bold text-[13px] text-[#14231c]">
                          {fmt(m.total)}
                          <span className="text-[9px] font-normal text-[#98a39b] ml-1">TZS</span>
                        </td>

                        <td className="py-3 px-4 text-right whitespace-nowrap font-mono-numbers font-bold text-[13px]">
                          {m.balance > 0 ? (
                            <span className="text-[#9e432b]">
                              {fmt(m.balance)}
                              <span className="text-[9px] font-normal text-[#98a39b] ml-1">TZS</span>
                            </span>
                          ) : (
                            <span className="text-[#746e7d] font-normal text-[11px]">
                              imekamilika
                            </span>
                          )}
                        </td>

                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1">
                            <button
                              onClick={() => openInvoiceView(inv)}
                              title="Tazama ankara"
                              className="w-7 h-7 rounded-lg grid place-items-center text-[#1b5540] hover:bg-[#e8f0e8] transition-colors"
                            >
                              <Eye className="w-3.5 h-3.5" />
                            </button>

                            <button
                              onClick={() => handleDownloadPDF(inv)}
                              title="Pakua PDF"
                              className="w-7 h-7 rounded-lg grid place-items-center text-[#34483e] hover:bg-[#ece7da] transition-colors"
                            >
                              <Download className="w-3.5 h-3.5" />
                            </button>

                            <button
                              onClick={() => openInvoiceBuilder(inv)}
                              title="Hariri ankara"
                              className="w-7 h-7 rounded-lg grid place-items-center text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c] transition-colors"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>

                            {m.balance > 0 && !isDraft && (
                              <button
                                onClick={() => markInvoicePaid(inv.id)}
                                title="Weka imelipwa"
                                className="w-7 h-7 rounded-lg grid place-items-center text-[#1b5540] hover:bg-[#e8f0e8] transition-colors"
                              >
                                <Check className="w-3.5 h-3.5" />
                              </button>
                            )}

                            <button
                              onClick={() => deleteInvoice(inv.id)}
                              title="Futa ankara"
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
              {filteredInvoices.map(inv => {
                const m = invoiceMath(inv);
                const st = invoiceStatus(inv);
                const isDraft = inv.state === 'draft';

                return (
                  <div key={inv.id} className="p-3.5 space-y-2.5">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="text-[13px] font-bold text-[#14231c]">
                          {inv.customerName}
                        </div>
                        <div className="text-[10.5px] text-[#746e7d]">
                          {inv.number} · {dateLabel(inv.issueDate, language)}
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <div className="font-mono-numbers font-bold text-[13.5px] text-[#14231c]">
                          {fmt(m.total)}
                          <span className="text-[9px] text-[#746e7d] ml-0.5">TZS</span>
                        </div>
                        {m.balance > 0 && (
                          <div className="text-[10px] text-[#9e432b] font-semibold">
                            Salio: {fmt(m.balance)} TZS
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-1 border-t border-[#ece7da]/60">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9.5px] font-bold ${
                          st.key === 'paid'
                            ? 'bg-[#e8f0e8] text-[#1b5540]'
                            : st.key === 'partial'
                            ? 'bg-[#f8edd3] text-[#946510]'
                            : st.key === 'overdue'
                            ? 'bg-[#f7e4db] text-[#9e432b]'
                            : 'bg-[#ece7da] text-[#746e7d]'
                        }`}
                      >
                        {st.label}
                      </span>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => openInvoiceView(inv)}
                          className="w-7 h-7 rounded grid place-items-center text-[#1b5540] hover:bg-[#e8f0e8]"
                          title="Tazama"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => handleDownloadPDF(inv)}
                          className="w-7 h-7 rounded grid place-items-center text-[#34483e] hover:bg-[#ece7da]"
                          title="Pakua PDF"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => openInvoiceBuilder(inv)}
                          className="w-7 h-7 rounded grid place-items-center text-[#746e7d] hover:bg-[#ece7da]"
                          title="Hariri"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>

                        {m.balance > 0 && !isDraft && (
                          <button
                            onClick={() => markInvoicePaid(inv.id)}
                            className="w-7 h-7 rounded grid place-items-center text-[#1b5540] hover:bg-[#e8f0e8]"
                            title="Lipa"
                          >
                            <Check className="w-3.5 h-3.5" />
                          </button>
                        )}

                        <button
                          onClick={() => deleteInvoice(inv.id)}
                          className="w-7 h-7 rounded grid place-items-center text-[#9e432b] hover:bg-[#f7e4db]"
                          title="Futa"
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
                Inaonyesha <strong className="text-[#14231c]">{filteredInvoices.length}</strong> kati ya {state.invoices.length} ankara
              </div>
              <div>
                {overdueCount > 0 ? (
                  <span className="text-[#9e432b] font-bold">{overdueCount} zimechelewa</span>
                ) : (
                  <span>Zote ziko kwenye wakati</span>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
