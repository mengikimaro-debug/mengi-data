import React from 'react';
import {
  LayoutDashboard,
  ArrowLeftRight,
  FileText,
  Receipt,
  PiggyBank
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { PageKey } from '../../types';

export const MobileNav: React.FC = () => {
  const { activePage, setActivePage, t } = useApp();

  const items: { id: PageKey; label: string; icon: React.ReactNode }[] = [
    { id: 'dashboard', label: t('Dashibodi'), icon: <LayoutDashboard className="w-[18px] h-[18px]" /> },
    { id: 'transactions', label: t('Miamala'), icon: <ArrowLeftRight className="w-[18px] h-[18px]" /> },
    { id: 'invoices', label: t('Ankara'), icon: <FileText className="w-[18px] h-[18px]" /> },
    { id: 'bills', label: t('Bili'), icon: <Receipt className="w-[18px] h-[18px]" /> },
    { id: 'saver', label: t('Akiba'), icon: <PiggyBank className="w-[18px] h-[18px]" /> }
  ];

  return (
    <nav
      className="mobile-nav-surface fixed inset-x-3 z-40 lg:hidden flex items-center justify-around p-1.5 bg-[#fcfaf4]/95 backdrop-blur-md border border-[#e2dccd] rounded-[18px] shadow-[0_12px_32px_rgba(8,30,22,0.16)]"
      style={{ bottom: 'calc(0.75rem + env(safe-area-inset-bottom, 0px))' }}
      aria-label={t('Menyu')}
    >
      {items.map(item => {
        const isActive = activePage === item.id;
        return (
          <button
            key={item.id}
            onClick={() => setActivePage(item.id)}
            aria-label={item.label}
            aria-current={isActive ? 'page' : undefined}
            className={`mobile-nav-item flex-1 py-1.5 px-1 rounded-[12px] flex flex-col items-center gap-0.5 text-[9px] font-semibold transition-all ${
              isActive ? 'bg-[#0c2b22] text-[#d8f36b]' : 'text-[#6d7b72] hover:text-[#14231c]'
            }`}
          >
            {item.icon}
            <span>{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
};
