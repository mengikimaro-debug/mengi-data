import React from 'react';
import {
  LayoutDashboard,
  ArrowLeftRight,
  FileText,
  Receipt,
  PiggyBank,
  BarChart3,
  Settings,
  TrendingUp,
  TrendingDown,
  Minus
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { PageKey } from '../../types';
import { fmt, initials, totalsFor, periodBounds } from '../../utils/format';

export const Sidebar: React.FC = () => {
  const {
    state,
    t,
    activePage,
    setActivePage,
    isMobileSidebarOpen,
    setIsMobileSidebarOpen
  } = useApp();

  const unpaidBillsCount = state.bills.filter(b => !b.paid).length;
  const monthTotals = totalsFor('month', state.transactions);
  const prevMonthNet = monthTotals.prevIncome - monthTotals.prevExpense;
  const currentNet = monthTotals.profit;

  // Trend from last month
  let trendPct = 0;
  let isUp = true;
  let isFlat = false;
  if (!prevMonthNet && !currentNet) {
    isFlat = true;
  } else if (!prevMonthNet) {
    isUp = currentNet >= 0;
    trendPct = 100;
  } else {
    trendPct = ((currentNet - prevMonthNet) / Math.abs(prevMonthNet)) * 100;
    isUp = trendPct >= 0;
    isFlat = Math.abs(trendPct) < 0.1;
  }

  const navItems: { id: PageKey; label: string; icon: React.ReactNode; badge?: number }[] = [
    {
      id: 'dashboard',
      label: t('Dashibodi'),
      icon: <LayoutDashboard className="w-[18px] height-[18px] opacity-90" />
    },
    {
      id: 'transactions',
      label: t('Miamala'),
      icon: <ArrowLeftRight className="w-[18px] height-[18px] opacity-90" />,
      badge: state.transactions.length
    },
    {
      id: 'invoices',
      label: t('Ankara'),
      icon: <FileText className="w-[18px] height-[18px] opacity-90" />,
      badge: state.invoices.length
    },
    {
      id: 'bills',
      label: t('Bili'),
      icon: <Receipt className="w-[18px] height-[18px] opacity-90" />,
      badge: unpaidBillsCount
    },
    {
      id: 'saver',
      label: t('Akiba'),
      icon: <PiggyBank className="w-[18px] height-[18px] opacity-90" />,
      badge: state.savings.length
    },
    {
      id: 'reports',
      label: t('Ripoti'),
      icon: <BarChart3 className="w-[18px] height-[18px] opacity-90" />
    }
  ];

  return (
    <>
      {/* Mobile scrim backdrop */}
      {isMobileSidebarOpen && (
        <div
          onClick={() => setIsMobileSidebarOpen(false)}
          className="fixed inset-0 backdrop-blur-xs z-40 lg:hidden transition-opacity"
          style={{ backgroundColor: 'var(--sidebar-scrim)' }}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 w-[248px] z-50 flex flex-col px-3.5 pt-5 pb-4 border-r border-white/5 transition-transform duration-250 ease-out lg:translate-x-0 ${
          isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{
          color: 'var(--sidebar-text)',
          background:
            `radial-gradient(120% 55% at 10% 0%, var(--sidebar-bg-radial), transparent 55%), linear-gradient(180deg, var(--sidebar-bg-linear-1) 0%, var(--sidebar-bg-linear-2) 55%, var(--sidebar-bg-linear-3) 100%)`
        }}
      >
        {/* Brand header */}
        <div className="flex items-center gap-3 px-2 pb-6 pt-1">
          <img
            src="/mengi-data-icon.svg"
            alt=""
            aria-hidden="true"
            className="w-[42px] h-[42px] shrink-0 rounded-full shadow-[0_8px_22px_rgba(216,243,107,.16)]"
          />
          <div>
            <div className="text-[12px] font-bold tracking-tight text-white leading-tight">
              MENGI<em className="not-italic text-[#d8f36b]">DATA</em>
            </div>
            <div className="text-[9px] tracking-[0.2em] uppercase font-semibold mt-1" style={{ color: 'var(--sidebar-label)' }}>
              {t('Dashibodi ya fedha')}
            </div>
          </div>
        </div>

        {/* Navigation list */}
        <div className="text-[9px] tracking-[0.19em] uppercase font-bold px-3 mb-2" style={{ color: 'var(--sidebar-label)' }}>
          {t('Menyu')}
        </div>
        <nav className="flex flex-col gap-1">
          {navItems.map(item => {
            const isActive = activePage === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActivePage(item.id);
                  setIsMobileSidebarOpen(false);
                }}
                aria-current={isActive ? 'page' : undefined}
                className={`relative flex items-center gap-3 w-full px-3 py-2.5 rounded-[10px] text-[13px] font-medium text-left transition-colors duration-150 ${
                  isActive
                    ? 'bg-gradient-to-r from-[#d8f36b]/15 to-[#d8f36b]/5 shadow-[inset_2.5px_0_0_#d8f36b]'
                    : 'hover:bg-white/[0.06]'
                }`}
                style={isActive
                  ? { color: 'var(--sidebar-active-text)' }
                  : { color: 'var(--sidebar-inactive-text)' }
                }
              >
                <span style={{ color: isActive ? 'var(--sidebar-icon-active)' : 'var(--sidebar-icon-inactive)' }}>
                  {item.icon}
                </span>
                <span>{item.label}</span>
                {item.badge !== undefined && (
                  <span
                    className={`ml-auto min-w-[20px] h-[19px] px-1.5 rounded-full inline-flex items-center justify-center text-[10px] font-bold ${
                      isActive ? 'bg-[#d8f36b] text-[#081e16]' : 'bg-white/10'
                    }`}
                    style={isActive ? {} : { color: 'var(--sidebar-profile-text)' }}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        <div className="flex-1" />

        {/* Sidebar Monthly Balance Card */}
        <div className="mx-1 my-3 p-3.5 rounded-[15px]" style={{ background: 'linear-gradient(135deg, var(--sidebar-card-bg), rgba(255,255,255,0.025))', borderColor: 'var(--sidebar-card-border)' }}>
          <div className="flex items-center justify-between gap-2 mb-2">
            <span className="text-[9px] tracking-[0.15em] uppercase font-bold" style={{ color: 'var(--sidebar-card-label)' }}>
              {t('Salio la mwezi')}
            </span>
            <span className="w-1.5 h-1.5 rounded-full bg-[#d8f36b] shadow-[0_0_0_4px_rgba(216,243,107,.15)]" />
          </div>
          <div className="font-display text-[22px] font-bold tracking-tight leading-tight" style={{ color: 'var(--sidebar-card-text)' }}>
            {fmt(currentNet)} <small className="font-sans text-[9px] tracking-wider font-bold ml-1" style={{ color: 'var(--sidebar-card-label)' }}>TZS</small>
          </div>
          <div className="flex items-center gap-1.5 mt-2 text-[10.5px]" style={{ color: 'var(--sidebar-card-muted)' }}>
            <span className={`font-bold inline-flex items-center gap-0.5 ${isUp ? 'text-[#d8f36b]' : 'text-[#f0a58d]'}`}>
              {isFlat ? (
                <Minus className="w-3 h-3" />
              ) : isUp ? (
                <TrendingUp className="w-3 h-3" />
              ) : (
                <TrendingDown className="w-3 h-3" />
              )}
              {isFlat ? '0%' : `${isUp ? '+' : ''}${trendPct.toFixed(0)}%`}
            </span>
            <span>{t('kutoka mwezi uliopita')}</span>
          </div>
        </div>

        {/* User profile foot */}
        <div className="flex items-center gap-2.5 px-2 pt-3 border-t border-white/[0.08]">
          <div className="w-[32px] h-[32px] rounded-[9px] grid place-items-center text-[11px] font-bold border border-white/10 shrink-0"
            style={{ backgroundColor: 'var(--sidebar-profile-bg)', color: 'var(--sidebar-profile-text)' }}>
            {initials(state.settings.businessName)}
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-[11.5px] font-semibold truncate leading-tight" style={{ color: 'var(--sidebar-profile-name)' }}>
              {state.settings.businessName || 'Mengi Data'}
            </div>
            <div className="text-[9px] truncate mt-0.5" style={{ color: 'var(--sidebar-profile-muted)' }}>
              {state.settings.ownerName || t('Mmiliki')} · Dar es Salaam
            </div>
          </div>
          <button
            onClick={() => {
              setActivePage('settings');
              setIsMobileSidebarOpen(false);
            }}
            title={t('Mipangilio')}
            aria-label={t('Mipangilio')}
            className="w-7 h-7 rounded-lg grid place-items-center hover:bg-white/[0.08] hover:text-white transition-colors"
            style={{ color: 'var(--sidebar-icon-inactive)' }}
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>
      </aside>
    </>
  );
};
