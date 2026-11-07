import React, { useEffect } from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { Sidebar } from './components/layout/Sidebar';
import { Topbar } from './components/layout/Topbar';
import { MobileNav } from './components/layout/MobileNav';
import { DashboardPage } from './components/dashboard/DashboardPage';
import { TransactionsPage } from './components/transactions/TransactionsPage';
import { InvoicesPage } from './components/invoices/InvoicesPage';
import { BillsPage } from './components/bills/BillsPage';
import { ReportsPage } from './components/reports/ReportsPage';
import { SettingsPage } from './components/settings/SettingsPage';
import { TransactionModal } from './components/modals/TransactionModal';
import { InvoiceBuilderModal } from './components/invoices/InvoiceBuilderModal';
import { InvoiceViewModal } from './components/invoices/InvoiceViewModal';
import { BillModal } from './components/modals/BillModal';
import { ConfirmModal } from './components/modals/ConfirmModal';
import { ToastContainer } from './components/ui/ToastContainer';

const MainLayout: React.FC = () => {
  const {
    activePage,
    setActivePage,
    isMobileSidebarOpen,
    setIsMobileSidebarOpen,
    confirmDialog,
    closeConfirm,
    openInvoiceBuilder,
    txModalOpen,
    invoiceBuilderOpen,
    invoiceViewOpen,
    billModalOpen,
    closeTxModal,
    closeInvoiceBuilder,
    closeInvoiceView,
    closeBillModal
  } = useApp();

  // Tell the Android shell that the UI mounted successfully.
  useEffect(() => {
    try {
      window.MengiNative?.appReady?.();
    } catch {
      // ignore — the app also works in a normal browser
    }
  }, []);

  // Keyboard shortcut: Cmd/Ctrl + K to create new invoice, ESC to close top modal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        openInvoiceBuilder();
      } else if (e.key === 'Escape') {
        if (invoiceViewOpen) closeInvoiceView();
        else if (invoiceBuilderOpen) closeInvoiceBuilder();
        else if (txModalOpen) closeTxModal();
        else if (billModalOpen) closeBillModal();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [
    openInvoiceBuilder,
    invoiceViewOpen,
    invoiceBuilderOpen,
    txModalOpen,
    billModalOpen,
    closeInvoiceView,
    closeInvoiceBuilder,
    closeTxModal,
    closeBillModal
  ]);

  // Android hardware/gesture back button: close the top-most layer, then go
  // home inside the app. Returning false lets the native shell exit the app.
  useEffect(() => {
    window.__androidBack = () => {
      if (invoiceViewOpen) {
        closeInvoiceView();
        return true;
      }
      if (invoiceBuilderOpen) {
        closeInvoiceBuilder();
        return true;
      }
      if (txModalOpen) {
        closeTxModal();
        return true;
      }
      if (billModalOpen) {
        closeBillModal();
        return true;
      }
      if (confirmDialog) {
        closeConfirm();
        return true;
      }
      if (isMobileSidebarOpen) {
        setIsMobileSidebarOpen(false);
        return true;
      }
      if (activePage !== 'dashboard') {
        setActivePage('dashboard');
        return true;
      }
      return false;
    };
    return () => {
      window.__androidBack = undefined;
    };
  }, [
    activePage,
    setActivePage,
    isMobileSidebarOpen,
    setIsMobileSidebarOpen,
    confirmDialog,
    closeConfirm,
    invoiceViewOpen,
    invoiceBuilderOpen,
    txModalOpen,
    billModalOpen,
    closeInvoiceView,
    closeInvoiceBuilder,
    closeTxModal,
    closeBillModal
  ]);

  return (
    <div className="min-h-screen flex text-[#14231c]">
      {/* Desktop & Mobile Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 lg:pl-[248px]">
        {/* Top Sticky Header */}
        <Topbar />

        {/* Dynamic Page Router */}
        <main className="flex-1 max-w-[1380px] w-full mx-auto px-4 sm:px-6 lg:px-8 pt-4 pb-20 lg:pb-12">
          <div key={activePage} className="page-transition">
            {activePage === 'dashboard' && <DashboardPage />}
            {activePage === 'transactions' && <TransactionsPage />}
            {activePage === 'invoices' && <InvoicesPage />}
            {activePage === 'bills' && <BillsPage />}
            {activePage === 'reports' && <ReportsPage />}
            {activePage === 'settings' && <SettingsPage />}
          </div>
        </main>
      </div>

      {/* Bottom bar on Mobile */}
      <MobileNav />

      {/* All Overlay Modals & Toasts */}
      <TransactionModal />
      <InvoiceBuilderModal />
      <InvoiceViewModal />
      <BillModal />
      <ConfirmModal />
      <ToastContainer />
    </div>
  );
};

export default function App() {
  return (
    <AppProvider>
      <MainLayout />
    </AppProvider>
  );
}
