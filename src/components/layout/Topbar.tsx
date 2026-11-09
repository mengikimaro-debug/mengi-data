import React, { useState, useRef, useEffect } from 'react';
import {
  Menu,
  Calendar,
  Bell,
  Plus,
  ChevronDown,
  Languages,
  ArrowUpRight,
  ArrowDownLeft,
  FilePlus,
  Receipt
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { todayISO, dateLong, overdueBills, overdueInvoices } from '../../utils/format';

export const Topbar: React.FC = () => {
  const {
    state,
    language,
    setLanguage,
    t,
    activePage,
    setActivePage,
    setIsMobileSidebarOpen,
    openTxModal,
    openInvoiceBuilder,
    openBillModal,
    addToast
  } = useApp();

  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const overdueBillsList = overdueBills(state.bills);
  const overdueInvoicesList = overdueInvoices(state.invoices);
  const totalReminders = overdueBillsList.length + overdueInvoicesList.length;

  const pageTitles: Record<string, { eyebrow: string; title: string }> = {
    dashboard: { eyebrow: `Mengidata / ${t('Dashibodi')}`, title: t('Dashibodi') },
    transactions: { eyebrow: `Mengidata / ${t('Daftari la fedha')}`, title: t('Miamala') },
    invoices: { eyebrow: `Mengidata / ${t('Ankara & malipo')}`, title: t('Ankara') },
    bills: { eyebrow: `Mengidata / ${t('Malipo yanayokuja')}`, title: t('Bili') },
    reports: { eyebrow: `Mengidata / ${t('Uchambuzi')}`, title: t('Ripoti') },
    settings: { eyebrow: `Mengidata / ${t('Sanidi mfumo')}`, title: t('Mipangilio') }
  };

  const currentMeta = pageTitles[activePage] || pageTitles.dashboard;

  const handleNotificationClick = () => {
    setActivePage('bills');
    if (totalReminders > 0) {
      addToast(
        `${overdueInvoicesList.length} ${t('ankara na')} ${overdueBillsList.length} ${t('bili zimechelewa malipo.')}`,
        'error'
      );
    } else {
      addToast(t('Hakuna ankara au bili iliyochelewa. Kila kitu kiko salama!'), 'success');
    }
  };

  return (
    <header className="sticky top-0 z-30 h-[76px] lg:h-[84px] flex items-center gap-4 px-4 lg:px-8 backdrop-blur-xl border-b transition-all" style={{ background: 'var(--topbar-bg)', borderColor: 'var(--topbar-border)' }}>
      <div className="flex items-center gap-3 min-w-0">
        {/* Mobile menu trigger */}
        <button
          onClick={() => setIsMobileSidebarOpen(true)}
          className="lg:hidden w-[40px] h-[40px] rounded-[12px] border grid place-items-center active:scale-95 transition-all shadow-xs"
          style={{ borderColor: 'var(--topbar-btn-border)', backgroundColor: 'var(--topbar-btn-bg)', color: 'var(--topbar-btn-text)' }}
          aria-label={t('Fungua menyu')}
        >
          <Menu className="w-5 h-5" />
        </button>

        <div>
          <div className="flex items-center gap-1.5 text-[9px] tracking-[0.2em] uppercase font-extrabold leading-tight" style={{ color: 'var(--topbar-eyebrow)' }}>
            <span className="premium-pulse-dot" />
            {currentMeta.eyebrow}
          </div>
          <div className="text-[16px] font-black tracking-tight mt-0.5 premium-text-gradient">
            {currentMeta.title}
          </div>
        </div>
      </div>

      {/* Date display (hidden on small screens) */}
      <div className="hidden md:flex items-center gap-2 text-[11.5px] ml-4 font-medium whitespace-nowrap" style={{ color: 'var(--topbar-text)' }}>
        <Calendar className="w-3.5 h-3.5 opacity-80" />
        <span>{dateLong(todayISO(), language)}</span>
      </div>

      <div className="flex-1" />

      {/* Action buttons */}
      <div className="flex items-center gap-2.5">
        {/* Quick Language Swap Button */}
        <button
          type="button"
          onClick={() => setLanguage(language === 'sw' ? 'en' : 'sw')}
          className="inline-flex items-center gap-1.5 h-[38px] px-3 rounded-[12px] border border-[#1b5540]/35 bg-gradient-to-r from-[#123a2c] to-[#0c2b22] text-[#f4f9e8] text-[11px] font-bold shadow-[0_4px_14px_rgba(12,43,34,0.2)] hover:scale-[1.03] active:scale-[0.97] transition-all cursor-pointer"
          title={language === 'sw' ? t('Badilisha kuwa Kiingereza') : t('Badilisha kuwa Kiswahili')}
          aria-label={language === 'sw' ? t('Badilisha kuwa Kiingereza') : t('Badilisha kuwa Kiswahili')}
        >
          <Languages className="w-3.5 h-3.5 text-[#d8f36b]" />
          <span>{language === 'sw' ? 'SW' : 'EN'}</span>
          <span className="text-[#d8f36b] text-[12px] font-extrabold">⇄</span>
          <span className="text-[#a3baac] text-[10px]">{language === 'sw' ? 'EN' : 'SW'}</span>
        </button>

        {/* Notification bell */}
        <button
          onClick={handleNotificationClick}
          title={totalReminders > 0 ? `${totalReminders} ${t('malipo yanahitaji uangalifu')}` : t('Taarifa za kumbusho')}
          className="relative w-[38px] h-[38px] rounded-[11px] border grid place-items-center transition-all"
          style={{ borderColor: 'var(--topbar-btn-border)', backgroundColor: 'var(--topbar-btn-bg)', color: 'var(--topbar-btn-text)' }}
        >
          <Bell className="w-[17px] h-[17px]" />
          {totalReminders > 0 && (
            <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#bd5a3d]" style={{ ringColor: 'var(--topbar-btn-bg)' }} />
          )}
        </button>

        {/* Quick Add Dropdown */}
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setMenuOpen(!menuOpen)}
            className="inline-flex items-center justify-center gap-1.5 h-[38px] px-3.5 rounded-[11px] bg-[#0c2b22] text-[#f4f9e8] font-semibold text-[12px] shadow-[0_7px_17px_rgba(12,43,34,0.16)] hover:bg-[#123a2c] hover:-translate-y-0.5 transition-all"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span className="hidden sm:inline">{t('Ongeza')}</span>
            <ChevronDown className="w-3.5 h-3.5 opacity-80" />
          </button>

          {menuOpen && (
            <div className="premium-enter absolute right-0 top-[calc(100%+8px)] w-[230px] p-1.5 border rounded-[14px] shadow-[0_24px_70px_rgba(8,30,22,0.18)] z-50"
              style={{ backgroundColor: 'var(--topbar-dropdown-bg)', borderColor: 'var(--topbar-btn-border)' }}>
              <button
                onClick={() => {
                  setMenuOpen(false);
                  openTxModal('income');
                }}
                className="flex items-center gap-2.5 w-full p-2 rounded-[9px] text-[12px] text-left transition-colors"
                style={{ color: 'var(--topbar-dropdown-text)' }}
                onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'var(--topbar-dropdown-hover)'; e.currentTarget.style.color = 'var(--topbar-dropdown-hover-text)'; }}
                onMouseLeave={e => { e.currentTarget.style.backgroundColor = ''; e.currentTarget.style.color = 'var(--topbar-dropdown-text)'; }}
              >
                <span className="w-7 h-7 rounded-[8px] grid place-items-center bg-[#e8f0e8] text-[#1b5540]">
                  <ArrowUpRight className="w-4 h-4" />
                </span>
                <div>
                  <div className="font-semibold">{t('Ingiza mapato')}</div>
                  <div className="text-[10px]" style={{ color: 'var(--topbar-text)' }}>{t('Mauzo au malipo ya mteja')}</div>
                </div>
              </button>

              <button
                onClick={() => {
                  setMenuOpen(false);
                  openTxModal('expense');
                }}
                className="flex items-center gap-2.5 w-full p-2 rounded-[9px] text-[12px] text-left transition-colors"
                style={{ color: 'var(--topbar-dropdown-text)' }}
                onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'var(--topbar-dropdown-hover)'; e.currentTarget.style.color = 'var(--topbar-dropdown-hover-text)'; }}
                onMouseLeave={e => { e.currentTarget.style.backgroundColor = ''; e.currentTarget.style.color = 'var(--topbar-dropdown-text)'; }}
              >
                <span className="w-7 h-7 rounded-[8px] grid place-items-center bg-[#f7e4db] text-[#9e432b]">
                  <ArrowDownLeft className="w-4 h-4" />
                </span>
                <div>
                  <div className="font-semibold">{t('Ingiza matumizi')}</div>
                  <div className="text-[10px]" style={{ color: 'var(--topbar-text)' }}>{t('Manunuzi au gharama za ofisi')}</div>
                </div>
              </button>

              <div className="h-[1px] my-1 mx-2" style={{ backgroundColor: 'var(--topbar-dropdown-divider)' }} />

              <button
                onClick={() => {
                  setMenuOpen(false);
                  openInvoiceBuilder();
                }}
                className="flex items-center gap-2.5 w-full p-2 rounded-[9px] text-[12px] text-left transition-colors"
                style={{ color: 'var(--topbar-dropdown-text)' }}
                onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'var(--topbar-dropdown-hover)'; e.currentTarget.style.color = 'var(--topbar-dropdown-hover-text)'; }}
                onMouseLeave={e => { e.currentTarget.style.backgroundColor = ''; e.currentTarget.style.color = 'var(--topbar-dropdown-text)'; }}
              >
                <span className="w-7 h-7 rounded-[8px] grid place-items-center bg-[#f8edd3] text-[#946510]">
                  <FilePlus className="w-4 h-4" />
                </span>
                <div>
                  <div className="font-semibold">{t('Tengeneza ankara')}</div>
                  <div className="text-[10px]" style={{ color: 'var(--topbar-text)' }}>{t('Ankara ya PDF kwa mteja')}</div>
                </div>
              </button>

              <button
                onClick={() => {
                  setMenuOpen(false);
                  openBillModal();
                }}
                className="flex items-center gap-2.5 w-full p-2 rounded-[9px] text-[12px] text-left transition-colors"
                style={{ color: 'var(--topbar-dropdown-text)' }}
                onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'var(--topbar-dropdown-hover)'; e.currentTarget.style.color = 'var(--topbar-dropdown-hover-text)'; }}
                onMouseLeave={e => { e.currentTarget.style.backgroundColor = ''; e.currentTarget.style.color = 'var(--topbar-dropdown-text)'; }}
              >
                <span className="w-7 h-7 rounded-[8px] grid place-items-center bg-[#e8f0e8] text-[#4b6f8b]">
                  <Receipt className="w-4 h-4" />
                </span>
                <div>
                  <div className="font-semibold">{t('Ongeza bili')}</div>
                  <div className="text-[10px]" style={{ color: 'var(--topbar-text)' }}>{t('Kodi, umeme, mishahara')}</div>
                </div>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
