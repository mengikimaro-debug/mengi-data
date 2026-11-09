import React, { useState } from 'react';
import {
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
  ArrowDownLeft,
  Clock,
  AlertTriangle,
  ChevronRight,
  Sparkles,
  ArrowRight,
  Plus,
  Receipt
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  fmt,
  money,
  moneyCompact,
  periodBounds,
  inBounds,
  lastMonths,
  daysFromToday,
  dateLabel,
  sortTxNewest,
  CHART_COLORS,
  num
} from '../../utils/format';
import { PeriodKey, Transaction } from '../../types';

export const DashboardPage: React.FC = () => {
  const {
    state,
    language,
    t,
    period,
    setPeriod,
    setActivePage,
    resetToCleanData,
    openTxModal,
    openBillModal
  } = useApp();

  const [hoveredMonth, setHoveredMonth] = useState<{
    full: string;
    income: number;
    expense: number;
    net: number;
    x: number;
    y: number;
  } | null>(null);

  const bounds = periodBounds(period);

  // Filter transactions for selected period
  const periodTxns = state.transactions.filter(t => inBounds(t.date, bounds.start, bounds.end));
  const currentIncome = periodTxns
    .filter(t => t.type === 'income')
    .reduce((sum, t) => sum + num(t.amount), 0);
  const currentExpense = periodTxns
    .filter(t => t.type === 'expense')
    .reduce((sum, t) => sum + num(t.amount), 0);
  const currentProfit = currentIncome - currentExpense;
  const currentMargin = currentIncome > 0 ? (currentProfit / currentIncome) * 100 : 0;

  // Previous period comparison
  let prevIncome = 0;
  let prevExpense = 0;
  if (bounds.prevStart) {
    for (const t of state.transactions) {
      if (!inBounds(t.date, bounds.prevStart, bounds.prevEnd)) continue;
      if (t.type === 'income') prevIncome += num(t.amount);
      else prevExpense += num(t.amount);
    }
  }

  const calcTrend = (current: number, prev: number, invert = false) => {
    if (!prev && !current) return { text: '—', isGood: true, hasPrev: false };
    if (!prev) return { text: 'mpya', isGood: !invert, hasPrev: false };
    const pct = ((current - prev) / Math.abs(prev)) * 100;
    const isUp = pct >= 0;
    const isGood = invert ? !isUp : isUp;
    return {
      text: `${Math.abs(pct).toFixed(1)}%`,
      isGood,
      isUp,
      hasPrev: true
    };
  };

  const incomeTrend = calcTrend(currentIncome, prevIncome);
  const expenseTrend = calcTrend(currentExpense, prevExpense, true);

  // Outstanding receivables
  const unpaidInvoices = state.invoices.filter(i => {
    if (i.state === 'draft') return false;
    const total = i.items.reduce((s, it) => s + num(it.qty) * num(it.price), 0);
    const balance = total - num(i.discount) + (total - num(i.discount)) * (num(i.vatRate) / 100) - num(i.amountPaid);
    return balance > 0;
  });

  const totalReceivable = unpaidInvoices.reduce((sum, inv) => {
    const subtotal = inv.items.reduce((s, it) => s + num(it.qty) * num(it.price), 0);
    const tax = (subtotal - num(inv.discount)) * (num(inv.vatRate) / 100);
    const bal = Math.max(0, subtotal - num(inv.discount) + tax - num(inv.amountPaid));
    return sum + bal;
  }, 0);

  const overdueInvoicesCount = unpaidInvoices.filter(
    i => i.dueDate && i.dueDate < new Date().toISOString().slice(0, 10)
  ).length;

  // Last 6 months for cashflow chart
  const sixMonths = lastMonths(6, language).map(m => {
    let inc = 0;
    let exp = 0;
    state.transactions.forEach(t => {
      if (!inBounds(t.date, m.start, m.end)) return;
      if (t.type === 'income') inc += num(t.amount);
      else exp += num(t.amount);
    });
    return {
      ...m,
      income: inc,
      expense: exp,
      net: inc - exp
    };
  });

  const maxVal = Math.max(1, ...sixMonths.map(m => Math.max(m.income, m.expense))) * 1.15;

  // Upcoming bills
  const upcomingBills = state.bills
    .filter(b => !b.paid)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
    .slice(0, 4);

  const totalUnpaidBills = state.bills
    .filter(b => !b.paid)
    .reduce((s, b) => s + num(b.amount), 0);

  // Recent transactions
  const recentTxns = state.transactions
    .slice()
    .sort(sortTxNewest)
    .slice(0, 5);

  // Donut chart calculations for expenses
  const expenseByCategory: Record<string, number> = {};
  periodTxns
    .filter(t => t.type === 'expense')
    .forEach(t => {
      const cat = t.category || 'Nyingine';
      expenseByCategory[cat] = (expenseByCategory[cat] || 0) + num(t.amount);
    });

  const expenseCategoriesList = Object.entries(expenseByCategory)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  const totalExpenseInPeriod = expenseCategoriesList.reduce((s, c) => s + c.value, 0);

  return (
    <div className="space-y-6 pb-12">
      {/* Demo data banner */}
      {state.meta.demo && (
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-3.5 px-4 rounded-[14px] border border-[#e8d5a8] bg-gradient-to-r from-[#fbf1d7] to-[#faf6e9] text-[#7d5c13] text-[12px] shadow-xs">
          <div className="flex items-center gap-2.5">
            <Sparkles className="w-4 h-4 shrink-0 text-[#c58a21]" />
            <div>
              <strong className="font-bold text-[#6e4f0d]">{t('Data ya mfano.')}</strong> {t('Tazama jinsi Mengidata inavyofanya kazi, kisha futa na uanze data yako halisi.')}
            </div>
          </div>
          <button
            onClick={resetToCleanData}
            className="px-3 py-1.5 rounded-[9px] bg-[#fff9ea] border border-[#e3cca0] text-[#7a5812] text-[11px] font-bold hover:bg-white transition-colors whitespace-nowrap"
          >
            {t('Anza data yangu')}
          </button>
        </div>
      )}

      {/* Page Heading & Period Switcher */}
      <div className="page-heading flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-[9px] font-bold tracking-[0.19em] uppercase text-[#1b5540]">
            <span className="w-4 h-[1px] bg-[#1b5540]/50" />
            {t('Muhtasari wa biashara')}
          </div>
          <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
            {t('Habari, ')}<em className="italic text-[#1b5540]">{state.settings.ownerName || 'Mengi'}</em>.
          </h1>
          <p className="text-[12.5px] text-[#746e7d] mt-1 max-w-xl">
            {t('Hapa ndipo pesa zako zinapokutana. Tazama mapato, matumizi na ankara kwa mtazamo mmoja.')}
          </p>
        </div>

        {/* Period Selector Tabs */}
        <div className="premium-segment self-start sm:self-auto" role="group" aria-label={t('Chagua kipindi')}>
          {(
            [
              { id: 'day', label: t('Siku') },
              { id: 'week', label: t('Wiki') },
              { id: 'month', label: t('Mwezi') },
              { id: 'months6', label: t('Miezi') },
              { id: 'year', label: t('Mwaka') }
            ] as { id: PeriodKey; label: string }[]
          ).map(p => {
            const isActive = period === p.id;
            return (
              <button
                key={p.id}
                onClick={() => setPeriod(p.id)}
                aria-pressed={isActive}
                className="premium-segment-option text-[11.5px] whitespace-nowrap"
              >
                {p.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* 4 KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
        {/* Income Card */}
        <div className="premium-glow-card relative overflow-hidden p-4 rounded-[18px] flex flex-col justify-between min-h-[135px]">
          <div className="flex items-center justify-between">
            <span className="text-[9px] font-bold tracking-[0.14em] uppercase text-[#746e7d]">
              {t('Mapato')}
            </span>
            <span className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#e8f0e8] text-[#1b5540] shadow-xs">
              <ArrowUpRight className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="font-display text-[26px] sm:text-[28px] font-bold text-[#14231c] tracking-tight leading-none font-mono-numbers">
              <span className="font-sans text-[10px] font-bold text-[#746e7d] mr-1.5 tracking-wider">TZS</span>
              {fmt(currentIncome)}
            </div>
            <div className="flex items-center gap-1.5 mt-2 text-[10.5px] text-[#746e7d]">
              <span className={`font-bold inline-flex items-center gap-0.5 ${incomeTrend.isGood ? 'text-[#1b5540]' : 'text-[#9e432b]'}`}>
                {incomeTrend.isUp ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                {incomeTrend.text}
              </span>
              <span>{bounds.key === 'all' ? t('yote') : `${t('kutoka ')}${bounds.prevLabel}`}</span>
            </div>
          </div>
        </div>

        {/* Expense Card */}
        <div className="premium-glow-card relative overflow-hidden p-4 rounded-[18px] flex flex-col justify-between min-h-[135px]">
          <div className="flex items-center justify-between">
            <span className="text-[9px] font-bold tracking-[0.14em] uppercase text-[#746e7d]">
              {t('Matumizi')}
            </span>
            <span className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#f7e4db] text-[#9e432b] shadow-xs">
              <ArrowDownLeft className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="font-display text-[26px] sm:text-[28px] font-bold text-[#14231c] tracking-tight leading-none font-mono-numbers">
              <span className="font-sans text-[10px] font-bold text-[#746e7d] mr-1.5 tracking-wider">TZS</span>
              {fmt(currentExpense)}
            </div>
            <div className="flex items-center gap-1.5 mt-2 text-[10.5px] text-[#746e7d]">
              <span className={`font-bold inline-flex items-center gap-0.5 ${expenseTrend.isGood ? 'text-[#1b5540]' : 'text-[#9e432b]'}`}>
                {expenseTrend.isUp ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                {expenseTrend.text}
              </span>
              <span>{bounds.key === 'all' ? t('yote') : `${t('kutoka ')}${bounds.prevLabel}`}</span>
            </div>
          </div>
        </div>

        {/* Net Profit Card (Deep Forest Green Accent) */}
        <div className="relative overflow-hidden p-4 rounded-[18px] bg-gradient-to-br from-[#123a2c] to-[#0a261d] text-[#f3f8ee] border border-[#123a2c] shadow-[0_14px_34px_rgba(8,30,22,.16)] flex flex-col justify-between min-h-[135px] hover:scale-[1.01] transition-all">
          <div className="flex items-center justify-between">
            <span className="text-[9px] font-bold tracking-[0.14em] uppercase text-[#a3baac]">
              {t('Faida halisi')}
            </span>
            <span className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#d8f36b]/15 text-[#d8f36b] shadow-xs">
              <TrendingUp className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="font-display text-[26px] sm:text-[28px] font-bold text-[#d8f36b] tracking-tight leading-none font-mono-numbers">
              <span className="font-sans text-[10px] font-bold text-[#a3baac] mr-1.5 tracking-wider">TZS</span>
              {fmt(currentProfit)}
            </div>
            <div className="flex items-center gap-1.5 mt-2 text-[10.5px] text-[#a5bcae]">
              <span className="font-bold text-[#d8f36b]">
                {currentMargin.toFixed(1)}%
              </span>
              <span>{t('margin ya faida')}</span>
            </div>
          </div>
        </div>

        {/* Outstanding Invoices Card */}
        <div className="premium-glow-card relative overflow-hidden p-4 rounded-[18px] flex flex-col justify-between min-h-[135px]">
          <div className="flex items-center justify-between">
            <span className="text-[9px] font-bold tracking-[0.14em] uppercase text-[#746e7d]">
              {t('Ankara zinazodaiwa')}
            </span>
            <span className="w-8 h-8 rounded-[9px] grid place-items-center bg-[#f8edd3] text-[#946510] shadow-xs">
              <Clock className="w-4 h-4" />
            </span>
          </div>
          <div className="mt-3">
            <div className="font-display text-[26px] sm:text-[28px] font-bold text-[#14231c] tracking-tight leading-none font-mono-numbers">
              <span className="font-sans text-[10px] font-bold text-[#746e7d] mr-1.5 tracking-wider">TZS</span>
              {fmt(totalReceivable)}
            </div>
            <div className="flex items-center gap-2 mt-2 text-[10.5px] text-[#746e7d]">
              <span>{unpaidInvoices.length} {t('hazijalipwa')}</span>
              {overdueInvoicesCount > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-[#f7e4db] text-[#9e432b] text-[9.5px] font-bold">
                  {overdueInvoicesCount} {t('zimechelewa')}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Main Dashboard Grid: Cashflow Chart + Upcoming Bills */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Cashflow Chart (2 Columns on Desktop) */}
        <div className="lg:col-span-2 p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] flex flex-col">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
            <div>
              <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">{t('Mtiririko wa fedha')}</h3>
              <p className="text-[11px] text-[#746e7d]">{t('Miezi 6 iliyopita · mapato na matumizi')}</p>
            </div>
            <div className="flex items-center gap-3 text-[10px] text-[#746e7d] font-semibold">
              <span className="inline-flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-[3px] bg-[#1b5540]" />
                {t('Mapato')}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-[3px] bg-[#bd5a3d]" />
                {t('Matumizi')}
              </span>
            </div>
          </div>

          {/* SVG Cashflow Chart */}
          <div className="relative flex-1 min-h-[220px] pt-4">
            {hoveredMonth && (
              <div
                className="absolute z-10 pointer-events-none p-2.5 rounded-[10px] bg-[#081e16] text-[#f4f8ed] text-[10.5px] shadow-lg -translate-x-1/2 -translate-y-full whitespace-nowrap"
                style={{ left: `${hoveredMonth.x}px`, top: `${hoveredMonth.y - 8}px` }}
              >
                <div className="text-[9px] uppercase tracking-wider text-[#a3baac] font-bold mb-1">
                  {hoveredMonth.full}
                </div>
                <div className="flex justify-between gap-4">
                  <span className="text-[#a3baac]">{t('Mapato')}:</span>
                  <span className="font-bold text-[#d8f36b]">{money(hoveredMonth.income)}</span>
                </div>
                <div className="flex justify-between gap-4">
                  <span className="text-[#a3baac]">{t('Matumizi')}:</span>
                  <span className="font-bold text-[#f7e4db]">{money(hoveredMonth.expense)}</span>
                </div>
                <div className="flex justify-between gap-4 pt-1 border-t border-white/10 mt-1">
                  <span className="text-[#a3baac]">{t('Faida')}:</span>
                  <span className="font-bold text-white">{money(hoveredMonth.net)}</span>
                </div>
              </div>
            )}

            <div className="h-[180px] w-full flex items-end justify-between gap-2 sm:gap-6 px-2 sm:px-6 border-b border-[#e2dccd]">
              {sixMonths.map((m, idx) => {
                const incHeight = Math.max(4, (m.income / maxVal) * 160);
                const expHeight = Math.max(4, (m.expense / maxVal) * 160);

                return (
                  <div
                    key={m.key}
                    className="flex-1 flex flex-col items-center h-full justify-end group cursor-pointer"
                    onMouseEnter={e => {
                      const rect = e.currentTarget.getBoundingClientRect();
                      const parentRect = e.currentTarget.parentElement?.parentElement?.getBoundingClientRect();
                      if (parentRect) {
                        setHoveredMonth({
                          full: m.full,
                          income: m.income,
                          expense: m.expense,
                          net: m.net,
                          x: rect.left - parentRect.left + rect.width / 2,
                          y: 40
                        });
                      }
                    }}
                    onMouseLeave={() => setHoveredMonth(null)}
                  >
                    <div className="flex items-end gap-1 sm:gap-1.5 w-full justify-center">
                      {/* Income Bar */}
                      <div
                        className="w-2 sm:w-3.5 bg-[#1b5540] rounded-t-[4px] transition-all duration-300 group-hover:brightness-110"
                        style={{ height: `${incHeight}px` }}
                      />
                      {/* Expense Bar */}
                      <div
                        className="w-2 sm:w-3.5 bg-[#bd5a3d] rounded-t-[4px] transition-all duration-300 group-hover:brightness-110"
                        style={{ height: `${expHeight}px` }}
                      />
                    </div>
                    <span className="text-[10px] text-[#746e7d] font-semibold mt-2 group-hover:text-[#14231c]">
                      {m.label}
                    </span>
                  </div>
                );
              })}
            </div>

            <div className="flex justify-between items-center px-4 pt-2 text-[9px] text-[#98a39b] font-medium">
              <span>0 TZS</span>
              <span>{t('Kiwango cha juu:')} {moneyCompact(maxVal)}</span>
            </div>
          </div>
        </div>

        {/* Upcoming Bills Card (1 Column) */}
        <div className="p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <div>
                <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">{t('Bili zinazokaribia')}</h3>
                <p className="text-[11px] text-[#746e7d]">{t('Malipo yanayohitaji uangalifu')}</p>
              </div>
              <button
                onClick={() => setActivePage('bills')}
                className="text-[11px] font-bold text-[#1b5540] hover:underline inline-flex items-center gap-0.5"
              >
                Zote <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {upcomingBills.length === 0 ? (
              <div className="py-8 text-center text-[#746e7d] text-[11px]">
                <p>{t('Hakuna bili inayodaiwa kwa sasa.')}</p>
                <button
                  onClick={() => openBillModal()}
                  className="mt-3 px-3 py-1.5 rounded-[9px] bg-[#0c2b22] text-white text-[11px] font-semibold"
                >
                  {t('Ongeza bili mpya')}
                </button>
              </div>
            ) : (
              <div className="divide-y divide-[#ece7da]">
                {upcomingBills.map(b => {
                  const days = daysFromToday(b.dueDate);
                  const isLate = days < 0;
                  const isSoon = days >= 0 && days <= 3;

                  return (
                    <div
                      key={b.id}
                      onClick={() => openBillModal(b)}
                      className="py-2.5 flex items-center justify-between gap-3 hover:bg-[#f8f4ea] px-1 rounded-[8px] cursor-pointer transition-colors"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div
                          className={`w-7 h-7 rounded-[8px] grid place-items-center shrink-0 ${
                            isLate
                              ? 'bg-[#f7e4db] text-[#9e432b]'
                              : isSoon
                              ? 'bg-[#f8edd3] text-[#946510]'
                              : 'bg-[#f8f4ea] text-[#1b5540]'
                          }`}
                        >
                          {isLate ? <AlertTriangle className="w-3.5 h-3.5" /> : <Receipt className="w-3.5 h-3.5" />}
                        </div>
                        <div className="min-w-0">
                          <div className="text-[12px] font-bold text-[#14231c] truncate">
                            {b.name}
                          </div>
                          <div className="text-[10px] text-[#746e7d] flex items-center gap-1.5">
                            <span className={isLate ? 'text-[#9e432b] font-bold' : ''}>
                              {isLate
                                ? `${t('imechelewa siku')} ${Math.abs(days)}`
                                : days === 0
                                ? t('leo')
                                : `${t('siku')} ${days}`}
                            </span>
                            <span>·</span>
                            <span>{b.category}</span>
                          </div>
                        </div>
                      </div>

                      <div className="text-right shrink-0">
                        <div className="text-[12px] font-bold text-[#14231c] font-mono-numbers">
                          {moneyCompact(b.amount)}
                        </div>
                        <div className="text-[9px] text-[#98a39b]">TZS</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Bills Footer */}
          <div className="pt-3 mt-3 border-t border-[#e2dccd] flex items-center justify-between">
            <div>
              <div className="text-[9px] uppercase tracking-wider text-[#746e7d] font-semibold">
                {t('Jumla inayodaiwa')}
              </div>
              <div className="text-[14px] font-bold text-[#14231c] font-mono-numbers">
                {money(totalUnpaidBills)}
              </div>
            </div>
            <button
              onClick={() => setActivePage('bills')}
              className="px-3 py-1.5 rounded-[9px] bg-[#fcfaf4] border border-[#d2cab7] text-[#14231c] text-[11px] font-semibold hover:bg-white"
            >
              {t('Endesha bili')}
            </button>
          </div>
        </div>
      </div>

      {/* Secondary Row: Recent Transactions + Spending Donut */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Recent Transactions (2 Columns on Desktop) */}
        <div className="lg:col-span-2 p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)]">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">{t('Miamala ya karibuni')}</h3>
              <p className="text-[11px] text-[#746e7d]">{t('Fedha zilizoingia na kutoka hivi karibuni')}</p>
            </div>
            <button
              onClick={() => setActivePage('transactions')}
              className="text-[11px] font-bold text-[#1b5540] hover:underline inline-flex items-center gap-0.5"
            >
              {t('Ona yote')} <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {recentTxns.length === 0 ? (
            <div className="py-8 text-center text-[#746e7d] text-[11px]">
              <p>{t('Bado hakuna miamala iliyorekodiwa.')}</p>
              <button
                onClick={() => openTxModal('income')}
                className="mt-3 px-3 py-1.5 rounded-[9px] bg-[#0c2b22] text-white text-[11px] font-semibold"
              >
                {t('Ongeza miamala')}
              </button>
            </div>
          ) : (
            <div className="divide-y divide-[#ece7da]">
              {recentTxns.map(t => {
                const isInc = t.type === 'income';
                return (
                  <div key={t.id} className="py-2.5 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div
                        className={`w-7 h-7 rounded-[8px] grid place-items-center shrink-0 ${
                          isInc ? 'bg-[#e8f0e8] text-[#1b5540]' : 'bg-[#f7e4db] text-[#9e432b]'
                        }`}
                      >
                        {isInc ? <ArrowUpRight className="w-3.5 h-3.5" /> : <ArrowDownLeft className="w-3.5 h-3.5" />}
                      </div>
                      <div className="min-w-0">
                        <div className="text-[12px] font-semibold text-[#14231c] truncate">
                          {t.description}
                        </div>
                        <div className="text-[10px] text-[#746e7d] flex items-center gap-2">
                          <span>{dateLabel(t.date, language)}</span>
                          <span>·</span>
                          <span className="truncate">{t.party || t.category}</span>
                        </div>
                      </div>
                    </div>

                    <div className={`text-right shrink-0 font-mono-numbers font-bold text-[12.5px] ${isInc ? 'text-[#1b5540]' : 'text-[#9e432b]'}`}>
                      {isInc ? '+' : '−'}{fmt(t.amount)}
                      <span className="text-[9px] text-[#746e7d] ml-1 font-sans">TZS</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Expense Category Breakdown (1 Column) */}
        <div className="p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] flex flex-col justify-between">
          <div>
            <div className="mb-3">
              <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">{t('Matumizi kwa kundi')}</h3>
              <p className="text-[11px] text-[#746e7d]">{bounds.label}</p>
            </div>

            {expenseCategoriesList.length === 0 || totalExpenseInPeriod === 0 ? (
              <div className="py-10 text-center text-[#746e7d] text-[11px]">
                {t('Hakuna matumizi kwa kipindi hiki.')}
              </div>
            ) : (
              <div className="space-y-2.5">
                {expenseCategoriesList.slice(0, 5).map((cat, idx) => {
                  const pct = (cat.value / totalExpenseInPeriod) * 100;
                  const color = CHART_COLORS[idx % CHART_COLORS.length];
                  return (
                    <div key={cat.name} className="text-[11px]">
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-[#34483e] font-medium truncate max-w-[150px]">{cat.name}</span>
                        <div className="font-mono-numbers font-bold text-[#14231c]">
                          {moneyCompact(cat.value)}{' '}
                          <span className="text-[9.5px] font-normal text-[#98a39b]">
                            ({pct.toFixed(0)}%)
                          </span>
                        </div>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-[#ece7da] overflow-hidden">
                        <div
                          className="h-full rounded-full transition-all duration-500"
                          style={{ width: `${pct}%`, backgroundColor: color }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="pt-3 mt-4 border-t border-[#e2dccd] flex items-center justify-between">
            <span className="text-[11px] text-[#746e7d]">{t('Jumla ya matumizi')}</span>
            <span className="font-display text-[15px] font-bold text-[#14231c] font-mono-numbers">
              {money(totalExpenseInPeriod)}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
