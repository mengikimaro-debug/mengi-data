import React from 'react';
import {
  Download,
  Printer,
  FileText,
  Clock
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  fmt,
  money,
  lastMonths,
  inBounds,
  parseISO,
  todayISO,
  dateLong,
  num
} from '../../utils/format';
import { exportTransactionsCSV, printHTML } from '../../utils/export';

export const ReportsPage: React.FC = () => {
  const { state, language, t } = useApp();
  const currentYear = new Date().getFullYear();

  // Transactions in current calendar year
  const yearTxns = state.transactions.filter(t => {
    const d = parseISO(t.date);
    return d && d.getFullYear() === currentYear;
  });

  const totalIncome = yearTxns
    .filter(t => t.type === 'income')
    .reduce((s, t) => s + num(t.amount), 0);

  const totalExpense = yearTxns
    .filter(t => t.type === 'expense')
    .reduce((s, t) => s + num(t.amount), 0);

  const netProfit = totalIncome - totalExpense;
  const profitMargin = totalIncome > 0 ? (netProfit / totalIncome) * 100 : 0;

  // Invoices stats
  const issuedInvoices = state.invoices.filter(i => i.state !== 'draft');
  const outstandingInvoices = issuedInvoices.filter(i => {
    const total = i.items.reduce((s, it) => s + num(it.qty) * num(it.price), 0);
    const tax = (total - num(i.discount)) * (num(i.vatRate) / 100);
    const bal = total - num(i.discount) + tax - num(i.amountPaid);
    return bal > 0;
  });

  const totalReceivable = outstandingInvoices.reduce((sum, inv) => {
    const subtotal = inv.items.reduce((s, it) => s + num(it.qty) * num(it.price), 0);
    const tax = (subtotal - num(inv.discount)) * (num(inv.vatRate) / 100);
    return sum + Math.max(0, subtotal - num(inv.discount) + tax - num(inv.amountPaid));
  }, 0);

  // 12-Month breakdown
  const months = lastMonths(12, language).map(m => {
    let inc = 0;
    let exp = 0;
    state.transactions.forEach(t => {
      if (!inBounds(t.date, m.start, m.end)) return;
      if (t.type === 'income') inc += num(t.amount);
      else exp += num(t.amount);
    });
    const net = inc - exp;
    const margin = inc > 0 ? (net / inc) * 100 : 0;
    return {
      ...m,
      inc,
      exp,
      net,
      margin
    };
  });

  const activeMonths = months.filter(m => m.inc > 0 || m.exp > 0);
  const displayMonths = activeMonths.length ? activeMonths : months.slice(-1);

  const tableTotalInc = displayMonths.reduce((s, m) => s + m.inc, 0);
  const tableTotalExp = displayMonths.reduce((s, m) => s + m.exp, 0);
  const tableTotalNet = tableTotalInc - tableTotalExp;
  const tableTotalMargin = tableTotalInc > 0 ? (tableTotalNet / tableTotalInc) * 100 : 0;

  // Expense categories
  const expenseByCategory: Record<string, number> = {};
  yearTxns
    .filter(t => t.type === 'expense')
    .forEach(t => {
      const cat = t.category || 'Nyingine';
      expenseByCategory[cat] = (expenseByCategory[cat] || 0) + num(t.amount);
    });
  const expenseCategoryList = Object.entries(expenseByCategory)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  const maxExpenseCatVal = expenseCategoryList[0]?.value || 1;

  // Income categories
  const incomeByCategory: Record<string, number> = {};
  yearTxns
    .filter(t => t.type === 'income')
    .forEach(t => {
      const cat = t.category || 'Nyingine';
      incomeByCategory[cat] = (incomeByCategory[cat] || 0) + num(t.amount);
    });
  const incomeCategoryList = Object.entries(incomeByCategory)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  const maxIncomeCatVal = incomeCategoryList[0]?.value || 1;

  const handlePrint = () => {
    const rows = displayMonths
      .map(
        m => `
      <tr>
        <td>${m.full}</td>
        <td style="text-align:right">${fmt(m.inc)}</td>
        <td style="text-align:right">${fmt(m.exp)}</td>
        <td style="text-align:right;font-weight:700;color:${m.net >= 0 ? '#1b5540' : '#9e432b'}">${fmt(m.net)}</td>
        <td style="text-align:right">${m.inc > 0 ? m.margin.toFixed(0) + '%' : '—'}</td>
      </tr>
    `
      )
      .join('');

    const html = `
      <div style="max-width:760px;margin:0 auto;padding:20px;font-family:sans-serif;color:#14231c">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #123a2c;padding-bottom:14px">
          <div>
            <div style="font-size:24px;font-weight:700;color:#123a2c">${state.settings.businessName}</div>
            <div style="font-size:10px;text-transform:uppercase;letter-spacing:1px;color:#746e7d;margin-top:2px">
              ${t('RIPOTI YA KIFEDHA YA MWAKA')} ${currentYear}
            </div>
          </div>
          <div style="text-align:right;font-size:10px;color:#746e7d">
            ${state.settings.address}<br>
            Simu: ${state.settings.phone}
          </div>
        </div>

        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin:20px 0;padding:16px;background:#f8f4ea;border:1px solid #ece7da;border-radius:10px">
          <div>
            <div style="font-size:9px;text-transform:uppercase;color:#746e7d">${t('Mapato Yote')}</div>
            <div style="font-size:18px;font-weight:700;color:#1b5540">TZS ${fmt(totalIncome)}</div>
          </div>
          <div>
            <div style="font-size:9px;text-transform:uppercase;color:#746e7d">${t('Matumizi Yote')}</div>
            <div style="font-size:18px;font-weight:700;color:#9e432b">TZS ${fmt(totalExpense)}</div>
          </div>
          <div>
            <div style="font-size:9px;text-transform:uppercase;color:#746e7d">${t('Faida Halisi')}</div>
            <div style="font-size:18px;font-weight:700;color:#14231c">TZS ${fmt(netProfit)} (${profitMargin.toFixed(1)}%)</div>
          </div>
        </div>

        <table>
          <thead>
            <tr>
              <th>${t('Mwezi')}</th>
              <th style="text-align:right">${t('Mapato (TZS)')}</th>
              <th style="text-align:right">${t('Matumizi (TZS)')}</th>
              <th style="text-align:right">${t('Faida (TZS)')}</th>
              <th style="text-align:right">${t('Margin')}</th>
            </tr>
          </thead>
          <tbody>
            ${rows}
          </tbody>
          <tfoot>
            <tr style="font-weight:700;border-top:2px solid #123a2c">
              <td>${t('JUMLA')}</td>
              <td style="text-align:right">${fmt(tableTotalInc)}</td>
              <td style="text-align:right">${fmt(tableTotalExp)}</td>
              <td style="text-align:right">${fmt(tableTotalNet)}</td>
              <td style="text-align:right">${tableTotalMargin.toFixed(0)}%</td>
            </tr>
          </tfoot>
        </table>

        <div style="margin-top:24px;padding-top:10px;border-top:1px solid #ece7da;font-size:9px;color:#6d7b72;display:flex;justify-content:space-between">
          <span>${t('Imetolewa:')} ${dateLong(todayISO(), language)}</span>
          <span>${t('Mengidata — Mfumo wa Fedha')}</span>
        </div>
      </div>
    `;

    printHTML(html, `Ripoti ya Mengidata ${currentYear}`);
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Page Heading & Actions */}
      <div className="page-heading flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-[9px] font-bold tracking-[0.19em] uppercase text-[#1b5540]">
            <span className="w-4 h-[1px] bg-[#1b5540]/50" />
            {t('Uchambuzi wa biashara')}
          </div>
          <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
            {t('Ripoti')}
          </h1>
          <p className="text-[12.5px] text-[#746e7d] mt-1 max-w-xl">
            {t('Picha kamili ya utendaji wa biashara yako — mwezi kwa mwezi, kundi kwa kundi.')}
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <button
            onClick={() => exportTransactionsCSV(yearTxns)}
            className="inline-flex items-center gap-1.5 h-[38px] px-3.5 rounded-[11px] bg-[#fcfaf4] border border-[#d2cab7] text-[#14231c] font-semibold text-[12px] hover:bg-white transition-all shadow-xs"
          >
            <Download className="w-4 h-4 text-[#746e7d]" />
            <span>{t('Pakua CSV')}</span>
          </button>

          <button
            onClick={handlePrint}
            className="inline-flex items-center gap-1.5 h-[38px] px-4 rounded-[11px] bg-[#0c2b22] text-white font-semibold text-[12px] hover:bg-[#123a2c] transition-all shadow-[0_7px_17px_rgba(12,43,34,0.16)]"
          >
            <Printer className="w-4 h-4" />
            <span>{t('Chapisha ripoti')}</span>
          </button>
        </div>
      </div>

      {/* Hero Overview Banner (Dark Green + 2 Side Cards) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Main Hero Card */}
        <div className="lg:col-span-2 relative overflow-hidden rounded-[20px] bg-gradient-to-br from-[#123a2c] to-[#091f17] text-[#f1f7ec] p-6 border border-[#123a2c] shadow-[0_14px_34px_rgba(8,30,22,0.16)] flex flex-col justify-between">
          <div>
            <div className="text-[9px] uppercase tracking-[0.18em] text-[#a3baac] font-bold">
              {t('Ripoti ya mwaka')} {currentYear}
            </div>

            <div className="font-display text-[36px] sm:text-[44px] font-bold text-[#d8f36b] tracking-tight leading-none mt-3 mb-2 font-mono-numbers">
              <span className="font-sans text-[12px] tracking-wider text-[#a3baac] font-bold mr-2">TZS</span>
              {fmt(netProfit)}
            </div>

            <p className="text-[12px] text-[#aac0b2] max-w-lg leading-relaxed">
              {totalIncome > 0
                ? `${t('Biashara imetengeneza faida halisi ya')}${profitMargin.toFixed(1)}% ${t('kutokana na mapato ya')}${money(totalIncome)} ${t('mwaka huu. Endelea kufuatilia miamala ili kupata picha sahihi kila siku.')}`
                : t('Bado hakuna miamala iliyorekodiwa mwaka huu. Ongeza miamala au ankara ili ripoti ianze kuonyesha mwenendo wako.')}
            </p>
          </div>

          <div className="flex flex-wrap gap-6 pt-5 mt-4 border-t border-white/10 text-[12px]">
            <div>
              <span className="text-[9px] uppercase tracking-wider text-[#92aa9c] font-bold block">
                {t('Mapato yote')}
              </span>
              <strong className="text-[16px] font-bold text-[#eef6e8] font-mono-numbers mt-0.5 block">
                {money(totalIncome)}
              </strong>
            </div>

            <div>
              <span className="text-[9px] uppercase tracking-wider text-[#92aa9c] font-bold block">
                {t('Matumizi yote')}
              </span>
              <strong className="text-[16px] font-bold text-[#f7e4db] font-mono-numbers mt-0.5 block">
                {money(totalExpense)}
              </strong>
            </div>

            <div>
              <span className="text-[9px] uppercase tracking-wider text-[#92aa9c] font-bold block">
                {t('Margin ya faida')}
              </span>
              <strong className="text-[16px] font-bold text-[#d8f36b] font-mono-numbers mt-0.5 block">
                {profitMargin.toFixed(1)}%
              </strong>
            </div>
          </div>
        </div>

        {/* Side Cards: Invoices stats */}
        <div className="flex flex-col gap-4">
          <div className="p-5 rounded-[18px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] flex items-center gap-3.5 flex-1">
            <div className="w-10 h-10 rounded-[11px] grid place-items-center bg-[#e8f0e8] text-[#1b5540] shrink-0">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
                {t('Ankara zilizotolewa')}
              </div>
              <div className="text-[20px] font-bold text-[#14231c] font-mono-numbers mt-0.5">
                {issuedInvoices.length} {t('malengo ya akiba yamesajiliwa'.replace('malengo ya akiba yamesajiliwa', 'ankara'))}
              </div>
            </div>
          </div>

          <div className="p-5 rounded-[18px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] flex items-center gap-3.5 flex-1">
            <div className="w-10 h-10 rounded-[11px] grid place-items-center bg-[#f7e4db] text-[#9e432b] shrink-0">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
                {t('Ankara zinazodaiwa')}
              </div>
              <div className="text-[20px] font-bold text-[#9e432b] font-mono-numbers mt-0.5">
                {money(totalReceivable)}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Grid: 12-Month Table + Category Progress Bars */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Monthly Financial Table (2 Columns on Desktop) */}
        <div className="lg:col-span-2 p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] overflow-hidden">
          <div className="mb-4">
            <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">
              {t('Mapato, matumizi na faida kwa miezi')}
            </h3>
            <p className="text-[11px] text-[#746e7d]">
              {displayMonths.length} {t('miezi ya hivi karibuni yenye rekodi')}
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-[12px]">
              <thead>
                <tr className="border-b border-[#e2dccd] text-[9px] uppercase tracking-wider font-bold text-[#746e7d]">
                  <th className="py-2.5 px-3">{t('Mwezi')}</th>
                  <th className="py-2.5 px-3 text-right">{t('Mapato')}</th>
                  <th className="py-2.5 px-3 text-right">{t('Matumizi')}</th>
                  <th className="py-2.5 px-3 text-right">{t('Faida')}</th>
                  <th className="py-2.5 px-3 text-right">Margin</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#ece7da]">
                {displayMonths.map(m => (
                  <tr key={m.key} className="hover:bg-[#f8f4ea] transition-colors">
                    <td className="py-2.5 px-3 font-semibold text-[#14231c]">{m.full}</td>
                    <td className="py-2.5 px-3 text-right font-mono font-medium">{fmt(m.inc)}</td>
                    <td className="py-2.5 px-3 text-right font-mono font-medium">{fmt(m.exp)}</td>
                    <td className={`py-2.5 px-3 text-right font-mono font-bold ${
                      m.net >= 0 ? 'text-[#1b5540]' : 'text-[#9e432b]'
                    }`}>
                      {m.net < 0 ? '−' : ''}{fmt(Math.abs(m.net))}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono text-[#746e7d]">
                      {m.inc > 0 ? `${m.margin.toFixed(0)}%` : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t-2 border-[#123a2c] font-bold text-[#14231c]">
                  <td className="py-3 px-3">{t('Jumla')}</td>
                  <td className="py-3 px-3 text-right font-mono">{fmt(tableTotalInc)}</td>
                  <td className="py-3 px-3 text-right font-mono">{fmt(tableTotalExp)}</td>
                  <td className={`py-3 px-3 text-right font-mono ${
                    tableTotalNet >= 0 ? 'text-[#1b5540]' : 'text-[#9e432b]'
                  }`}>
                    {tableTotalNet < 0 ? '−' : ''}{fmt(Math.abs(tableTotalNet))}
                  </td>
                  <td className="py-3 px-3 text-right font-mono">
                    {tableTotalInc > 0 ? `${tableTotalMargin.toFixed(0)}%` : '—'}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Category Breakdown Progress Bars (1 Column) */}
        <div className="space-y-4">
          {/* Expenses by Category */}
          <div className="p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)]">
            <h3 className="text-[13.5px] font-bold text-[#14231c] tracking-tight mb-1">
              {t('Matumizi kwa kundi')}
            </h3>
            <p className="text-[10.5px] text-[#746e7d] mb-3">{t('Mwaka huu')} {currentYear}</p>

            {expenseCategoryList.length === 0 ? (
              <div className="py-6 text-center text-[11px] text-[#746e7d]">
                {t('Hakuna matumizi yaliyorekodiwa mwaka huu.')}
              </div>
            ) : (
              <div className="space-y-2.5">
                {expenseCategoryList.slice(0, 5).map(cat => {
                  const pct = (cat.value / maxExpenseCatVal) * 100;
                  return (
                    <div key={cat.name} className="text-[11px]">
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-[#34483e] font-medium truncate max-w-[130px]">{cat.name}</span>
                        <span className="font-mono font-bold text-[#14231c]">{money(cat.value)}</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-[#ece7da] overflow-hidden">
                        <div
                          className="h-full rounded-full bg-[#bd5a3d] transition-all duration-500"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Income by Category */}
          <div className="p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)]">
            <h3 className="text-[13.5px] font-bold text-[#14231c] tracking-tight mb-1">
              {t('Mapato kwa kundi')}
            </h3>
            <p className="text-[10.5px] text-[#746e7d] mb-3">{t('Mwaka huu')} {currentYear}</p>

            {incomeCategoryList.length === 0 ? (
              <div className="py-6 text-center text-[11px] text-[#746e7d]">
                {t('Hakuna mapato yaliyorekodiwa mwaka huu.')}
              </div>
            ) : (
              <div className="space-y-2.5">
                {incomeCategoryList.slice(0, 5).map(cat => {
                  const pct = (cat.value / maxIncomeCatVal) * 100;
                  return (
                    <div key={cat.name} className="text-[11px]">
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-[#34483e] font-medium truncate max-w-[130px]">{cat.name}</span>
                        <span className="font-mono font-bold text-[#14231c]">{money(cat.value)}</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-[#ece7da] overflow-hidden">
                        <div
                          className="h-full rounded-full bg-[#1b5540] transition-all duration-500"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
