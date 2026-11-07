import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  AppState,
  Transaction,
  Invoice,
  Bill,
  BusinessSettings,
  PageKey,
  PeriodKey,
  ToastMessage,
  ConfirmDialogOptions
} from '../types';
import {
  uid,
  todayISO,
  addDaysISO,
  invoiceMath,
  parseISO
} from '../utils/format';
import { getNativeBridge, SystemThemeChangeEvent } from '../native/bridge';
import { AppLanguage, translate } from '../utils/i18n';

const STORE_KEY = 'mengi_data_v1';
const THEME_KEY = 'mengi_data_theme';
const LANGUAGE_KEY = 'mengi_data_language';
const APP_VERSION = 1;

export type ThemePreference = 'system' | 'light' | 'dark';

function getStoredTheme(): ThemePreference {
  try {
    const storedTheme = localStorage.getItem(THEME_KEY);
    if (storedTheme === 'light' || storedTheme === 'dark' || storedTheme === 'system') {
      return storedTheme;
    }
  } catch (error) {
    console.warn('Failed to read theme preference', error);
  }
  return 'system';
}

function getSystemPrefersDark(): boolean {
  const bridge = getNativeBridge();
  if (bridge && typeof bridge.getSystemTheme === 'function') {
    try {
      return bridge.getSystemTheme() === 'dark';
    } catch (error) {
      console.warn('Failed to read the Android system theme', error);
    }
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

function getStoredLanguage(): AppLanguage {
  try {
    return localStorage.getItem(LANGUAGE_KEY) === 'en' ? 'en' : 'sw';
  } catch (error) {
    console.warn('Failed to read language preference', error);
    return 'sw';
  }
}

export const DEFAULT_SETTINGS: BusinessSettings = {
  businessName: 'Mengi Data',
  ownerName: 'Mengi',
  tagline: 'Ushauri, mauzo & huduma za kidijitali',
  address: 'Kimara Mwisho, Ubungo, Dar es Salaam',
  phone: '+255 754 000 000',
  email: 'hello@mengidata.co.tz',
  tin: '123-456-789',
  website: 'mengidata.co.tz',
  invoicePrefix: 'MD',
  vatRate: 0,
  paymentDetails: 'M-Pesa Lipa Namba: 5521400 (Mengi Data)\nBenki: CRDB Bank — A/C 0150 2200 4400 (Tawi la Holland House)',
  invoiceNote: 'Asante kwa kufanya biashara nasi. Malipo yafanyike kabla ya tarehe ya mwisho.',
  currency: 'TZS'
};

function seedDate(monthOffset: number, day: number): string {
  const now = new Date();
  const d = new Date(now.getFullYear(), now.getMonth() + monthOffset, 1, 12, 0, 0);
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(day, last));
  return d.toISOString().slice(0, 10);
}

export function generateSeedState(): AppState {
  const year = new Date().getFullYear();

  const transactions: Transaction[] = [
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(0, 3),
      description: 'Mauzo ya vifaa vya ofisi & kompyuta',
      party: 'Tumaini Stores',
      category: 'Mauzo ya bidhaa',
      amount: 1850000,
      method: 'M-Pesa',
      reference: 'MP-884210',
      notes: 'Malipo ya fedha taslimu ya mzigo wa kompyuta',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(0, 1),
      description: 'Huduma ya ufungaji wa mfumo wa mtandao',
      party: 'Green Valley Hotel',
      category: 'Huduma',
      amount: 950000,
      method: 'Benki',
      reference: 'CRDB-221045',
      notes: 'Awamu ya pili ya huduma ya WiFi',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(0, 2),
      description: 'Manunuzi ya stock — vifaa vya ofisi',
      party: 'Kariakoo Electronics Co.',
      category: 'Manunuzi / stock',
      amount: 620000,
      method: 'Cash',
      reference: 'RCP-9941',
      notes: 'Kebo, swichi na adapters',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(0, 4),
      description: 'Mafuta na usafiri wa kikazi',
      party: 'Puma Energy',
      category: 'Usafiri',
      amount: 180000,
      method: 'Cash',
      reference: '',
      notes: 'Safari za wateja mjini',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    // Historical months
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(-1, 14),
      description: 'Ushauri wa mfumo wa mauzo na TEHAMA',
      party: 'Sahara Agro Ltd',
      category: 'Ushauri',
      amount: 2100000,
      method: 'Benki',
      reference: 'NMB-77812',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(-1, 18),
      description: 'Usafiri wa kikazi — Mwanza & Morogoro',
      party: 'Air Tanzania',
      category: 'Usafiri',
      amount: 350000,
      method: 'M-Pesa',
      reference: 'ATCL-114',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(-2, 11),
      description: 'Mauzo ya printer na toner za ofisi',
      party: 'Mlimani Fabrics',
      category: 'Mauzo ya bidhaa',
      amount: 1450000,
      method: 'Benki',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(-2, 19),
      description: 'Matengenezo ya gari la kazi',
      party: 'Garage ya Mjini',
      category: 'Matengenezo',
      amount: 280000,
      method: 'Cash',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(-3, 8),
      description: 'Mradi wa mfumo wa stoo na data',
      party: 'Bahari Logistics',
      category: 'Huduma',
      amount: 3200000,
      method: 'Benki',
      reference: 'CRDB-8812',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(-3, 20),
      description: 'Manunuzi ya stock ya vifaa',
      party: 'China Mall Dar',
      category: 'Manunuzi / stock',
      amount: 510000,
      method: 'Benki',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(-4, 15),
      description: 'Ufungaji wa mifumo ya CCTV',
      party: 'Kigamboni Flats',
      category: 'Huduma',
      amount: 1100000,
      method: 'M-Pesa',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(-4, 22),
      description: 'Umeme na maji ya ofisi',
      party: 'TANESCO & DAWASA',
      category: 'Umeme & maji',
      amount: 190000,
      method: 'M-Pesa',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'income',
      date: seedDate(-5, 9),
      description: 'Mauzo ya vifaa vya mtandao na routers',
      party: 'TechPoint Systems',
      category: 'Mauzo ya bidhaa',
      amount: 1750000,
      method: 'Benki',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    },
    {
      id: uid('txn'),
      type: 'expense',
      date: seedDate(-5, 25),
      description: 'Masoko na matangazo ya mitandao',
      party: 'Meta Ads',
      category: 'Masoko & matangazo',
      amount: 300000,
      method: 'Benki',
      reference: '',
      notes: '',
      source: 'manual',
      linkedId: null,
      createdAt: new Date().toISOString()
    }
  ];

  const invoices: Invoice[] = [
    {
      id: uid('inv'),
      number: `MD-${year}-006`,
      customerName: 'Bahari Logistics Ltd',
      customerPhone: '+255 713 220 110',
      customerEmail: 'accounts@bahari.co.tz',
      customerAddress: 'Kurasini Bandari, Dar es Salaam',
      issueDate: addDaysISO(todayISO(), -2),
      dueDate: addDaysISO(todayISO(), 12),
      items: [
        { description: 'Mfumo wa kufuatilia mizigo na maghala', qty: 1, price: 2400000 },
        { description: 'Mafunzo ya timu ya stoo na ripoti', qty: 1, price: 600000 }
      ],
      discount: 0,
      vatRate: 0,
      amountPaid: 0,
      state: 'draft',
      paidDate: '',
      notes: 'Rasimu inayosubiri ukaguzi wa mwisho na saini ya mteja.',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('inv'),
      number: `MD-${year}-005`,
      customerName: 'Kilimanjaro Coffee Co.',
      customerPhone: '+255 754 889 200',
      customerEmail: 'orders@kilimanjarocoffee.co.tz',
      customerAddress: 'Moshi Mjini, Kilimanjaro',
      issueDate: addDaysISO(todayISO(), -7),
      dueDate: addDaysISO(todayISO(), 7),
      items: [
        { description: 'Ubunifu na uchapishaji wa vifungashio', qty: 1, price: 1250000 },
        { description: 'Ushauri wa chapa na masoko ya mtandao', qty: 1, price: 700000 }
      ],
      discount: 0,
      vatRate: 0,
      amountPaid: 0,
      state: 'issued',
      paidDate: '',
      notes: 'Malipo yafanyike moja kwa moja kupitia akaunti yetu ya benki ya CRDB.',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('inv'),
      number: `MD-${year}-004`,
      customerName: 'Tumaini Stores',
      customerPhone: '+255 765 330 440',
      customerEmail: 'tumaini.stores@gmail.com',
      customerAddress: 'Kariakoo Msimbazi, Dar es Salaam',
      issueDate: addDaysISO(todayISO(), -16),
      dueDate: addDaysISO(todayISO(), -2),
      items: [
        { description: 'Kuweka mfumo wa mauzo (POS)', qty: 1, price: 1800000 },
        { description: 'Mafunzo ya wafanyakazi wa kaunta', qty: 1, price: 500000 }
      ],
      discount: 0,
      vatRate: 0,
      amountPaid: 2300000,
      state: 'issued',
      paidDate: addDaysISO(todayISO(), -2),
      notes: 'Imelipwa yote kupitia M-Pesa.',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('inv'),
      number: `MD-${year}-003`,
      customerName: 'Sahara Agro Tech',
      customerPhone: '+255 786 112 900',
      customerEmail: 'finance@saharaagro.co.tz',
      customerAddress: 'Kihonda, Morogoro',
      issueDate: addDaysISO(todayISO(), -35),
      dueDate: addDaysISO(todayISO(), -14),
      items: [
        { description: 'Mfumo wa usimamizi wa stock na wakulima', qty: 1, price: 3200000 }
      ],
      discount: 0,
      vatRate: 0,
      amountPaid: 1600000,
      state: 'issued',
      paidDate: addDaysISO(todayISO(), -20),
      notes: 'Malipo ya awamu ya kwanza (50%) yamepokelewa. Awamu ya pili inasubiriwa.',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('inv'),
      number: `MD-${year}-002`,
      customerName: 'Green Valley Hotel',
      customerPhone: '+255 715 404 200',
      customerEmail: 'info@greenvalley.co.tz',
      customerAddress: 'Njiro, Arusha',
      issueDate: addDaysISO(todayISO(), -55),
      dueDate: addDaysISO(todayISO(), -40),
      items: [
        { description: 'Ufungaji wa mfumo wa mtandao wa kasi (Fiber WiFi)', qty: 1, price: 1150000 }
      ],
      discount: 0,
      vatRate: 0,
      amountPaid: 1150000,
      state: 'issued',
      paidDate: addDaysISO(todayISO(), -40),
      notes: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('inv'),
      number: `MD-${year}-001`,
      customerName: 'Mlimani Fabrics',
      customerPhone: '+255 712 660 800',
      customerEmail: 'mlimani@fabrics.co.tz',
      customerAddress: 'Mwenge, Dar es Salaam',
      issueDate: addDaysISO(todayISO(), -100),
      dueDate: addDaysISO(todayISO(), -85),
      items: [
        { description: 'Uundaji wa tovuti ya biashara na katalogi', qty: 1, price: 890000 }
      ],
      discount: 0,
      vatRate: 0,
      amountPaid: 890000,
      state: 'issued',
      paidDate: addDaysISO(todayISO(), -85),
      notes: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    }
  ];

  const bills: Bill[] = [
    {
      id: uid('bill'),
      name: `Kodi ya pango — ${new Date().toLocaleString('sw', { month: 'long' })}`,
      category: 'Kodi ya pango',
      payee: 'Mengi Plaza Limited',
      amount: 700000,
      dueDate: seedDate(0, 5),
      paid: true,
      paidDate: seedDate(0, 2),
      method: 'Benki',
      recurring: true,
      notes: 'Pango la ofisi ya Ubungo',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: `Mishahara ya timu — ${new Date().toLocaleString('sw', { month: 'long' })}`,
      category: 'Mishahara',
      payee: 'Timu ya Mengi Data (Wafanyakazi 3)',
      amount: 1200000,
      dueDate: seedDate(0, 28),
      paid: false,
      paidDate: '',
      method: 'Benki',
      recurring: true,
      notes: 'Malipo ya kila mwisho wa mwezi',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: 'Bili ya Umeme — LUKU ya Ofisi',
      category: 'Umeme & maji',
      payee: 'TANESCO',
      amount: 85000,
      dueDate: seedDate(0, 8),
      paid: false,
      paidDate: '',
      method: 'M-Pesa',
      recurring: true,
      notes: 'Mita namba 241908821',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: 'Internet ya kasi (Fiber Optic 50Mbps)',
      category: 'Internet & simu',
      payee: 'Vodacom Business',
      amount: 120000,
      dueDate: seedDate(0, 10),
      paid: false,
      paidDate: '',
      method: 'M-Pesa',
      recurring: true,
      notes: '',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: 'Bili ya Maji ya Ofisi',
      category: 'Umeme & maji',
      payee: 'DAWASA',
      amount: 45000,
      dueDate: seedDate(0, 7),
      paid: false,
      paidDate: '',
      method: 'M-Pesa',
      recurring: true,
      notes: '',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: 'Kodi ya Mapato (TRA Makadirio)',
      category: 'Kodi & vibali',
      payee: 'Mamlaka ya Mapato Tanzania (TRA)',
      amount: 450000,
      dueDate: seedDate(0, 15),
      paid: false,
      paidDate: '',
      method: 'Benki',
      recurring: false,
      notes: 'Robo ya kwanza ya mwaka',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: 'Mishahara ya timu — Mwezi uliopita',
      category: 'Mishahara',
      payee: 'Timu ya Mengi Data',
      amount: 1200000,
      dueDate: seedDate(-1, 28),
      paid: true,
      paidDate: seedDate(-1, 27),
      method: 'Benki',
      recurring: true,
      notes: 'Imekamilika',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    },
    {
      id: uid('bill'),
      name: 'Bima ya Afya ya Timu',
      category: 'Bima',
      payee: 'NHIF Tanzania',
      amount: 180000,
      dueDate: seedDate(-1, 25),
      paid: true,
      paidDate: seedDate(-1, 24),
      method: 'Benki',
      recurring: false,
      notes: 'Kadi zimesasishwa',
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    }
  ];

  return {
    version: APP_VERSION,
    transactions,
    invoices,
    bills,
    settings: { ...DEFAULT_SETTINGS },
    meta: {
      demo: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  };
}

export function generateEmptyState(): AppState {
  return {
    version: APP_VERSION,
    transactions: [],
    invoices: [],
    bills: [],
    settings: { ...DEFAULT_SETTINGS },
    meta: {
      demo: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
  };
}

interface AppContextValue {
  state: AppState;
  language: AppLanguage;
  setLanguage: (language: AppLanguage) => void;
  t: (text: string) => string;
  theme: ThemePreference;
  setTheme: (theme: ThemePreference) => void;
  activePage: PageKey;
  setActivePage: (page: PageKey) => void;
  period: PeriodKey;
  setPeriod: (p: PeriodKey) => void;
  isMobileSidebarOpen: boolean;
  setIsMobileSidebarOpen: (open: boolean) => void;
  toasts: ToastMessage[];
  addToast: (msg: string, type?: 'success' | 'error' | 'info', action?: { label: string; onClick: () => void }) => void;
  removeToast: (id: string) => void;
  confirmDialog: ConfirmDialogOptions | null;
  showConfirm: (opts: ConfirmDialogOptions) => void;
  closeConfirm: () => void;
  // Modals
  txModalOpen: boolean;
  editingTx: Transaction | null;
  defaultTxType: 'income' | 'expense';
  openTxModal: (type?: 'income' | 'expense', tx?: Transaction | null) => void;
  closeTxModal: () => void;
  invoiceBuilderOpen: boolean;
  editingInvoice: Invoice | null;
  openInvoiceBuilder: (inv?: Invoice | null) => void;
  closeInvoiceBuilder: () => void;
  invoiceViewOpen: boolean;
  viewingInvoice: Invoice | null;
  openInvoiceView: (inv: Invoice) => void;
  closeInvoiceView: () => void;
  billModalOpen: boolean;
  editingBill: Bill | null;
  openBillModal: (bill?: Bill | null) => void;
  closeBillModal: () => void;
  // Actions
  addTransaction: (tx: Omit<Transaction, 'id' | 'createdAt'>) => void;
  updateTransaction: (id: string, updates: Partial<Transaction>) => void;
  deleteTransaction: (id: string) => void;
  addInvoice: (inv: Omit<Invoice, 'id' | 'createdAt' | 'updatedAt' | 'linkedTxnId'>) => Invoice;
  updateInvoice: (id: string, updates: Partial<Invoice>) => void;
  deleteInvoice: (id: string) => void;
  markInvoicePaid: (id: string) => void;
  addBill: (bill: Omit<Bill, 'id' | 'createdAt' | 'linkedTxnId'>) => void;
  updateBill: (id: string, updates: Partial<Bill>) => void;
  deleteBill: (id: string) => void;
  toggleBillPaid: (id: string) => void;
  updateSettings: (newSettings: Partial<BusinessSettings>) => void;
  resetToCleanData: () => void;
  loadDemoData: () => void;
  restoreFromJSON: (parsed: AppState) => void;
  getNextInvoiceNumber: () => string;
}

const AppContext = createContext<AppContextValue | null>(null);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<ThemePreference>(getStoredTheme);
  const [language, setLanguage] = useState<AppLanguage>(getStoredLanguage);
  const [systemPrefersDark, setSystemPrefersDark] = useState(getSystemPrefersDark);
  const [state, setState] = useState<AppState>(() => {
    try {
      const stored = localStorage.getItem(STORE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && Array.isArray(parsed.transactions) && Array.isArray(parsed.invoices)) {
          return {
            ...generateEmptyState(),
            ...parsed,
            settings: { ...DEFAULT_SETTINGS, ...(parsed.settings || {}) }
          };
        }
      }
      const legacyData = getNativeBridge()?.getLegacyData();
      if (legacyData) {
        const parsed = JSON.parse(legacyData);
        if (parsed && Array.isArray(parsed.transactions) && Array.isArray(parsed.invoices)) {
          return {
            ...generateEmptyState(),
            ...parsed,
            settings: { ...DEFAULT_SETTINGS, ...(parsed.settings || {}) }
          };
        }
      }
    } catch {
      // ignore
    }
    return generateSeedState();
  });

  useEffect(() => {
    const mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)');
    const handleChange = (event: MediaQueryListEvent) => setSystemPrefersDark(event.matches);
    const handleNativeChange = (event: SystemThemeChangeEvent) => {
      setSystemPrefersDark(event.detail.dark);
      if (typeof event.detail.animationsEnabled === 'boolean') {
        if (event.detail.animationsEnabled) delete document.documentElement.dataset.reduceMotion;
        else document.documentElement.dataset.reduceMotion = 'true';
      }
    };
    mediaQuery?.addEventListener('change', handleChange);
    window.addEventListener('mengi-system-theme-change', handleNativeChange);
    const bridge = getNativeBridge();
    if (bridge && typeof bridge.animationsEnabled === 'function') {
      try {
        if (bridge.animationsEnabled()) delete document.documentElement.dataset.reduceMotion;
        else document.documentElement.dataset.reduceMotion = 'true';
      } catch (error) {
        console.warn('Failed to read the Android animation setting', error);
      }
    }
    return () => {
      mediaQuery?.removeEventListener('change', handleChange);
      window.removeEventListener('mengi-system-theme-change', handleNativeChange);
    };
  }, []);

  useEffect(() => {
    const resolvedTheme = theme === 'system' ? (systemPrefersDark ? 'dark' : 'light') : theme;
    document.documentElement.dataset.theme = resolvedTheme;
    const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (themeColor) themeColor.content = resolvedTheme === 'dark' ? '#071a14' : '#0c2b22';
    const bridge = getNativeBridge();
    if (bridge && typeof bridge.setThemeMode === 'function') {
      try {
        bridge.setThemeMode(theme);
      } catch (error) {
        console.warn('Failed to synchronize the theme with Android', error);
      }
    }
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch (error) {
      console.warn('Failed to persist theme preference', error);
    }
  }, [theme, systemPrefersDark]);

  useEffect(() => {
    document.documentElement.lang = language;
    try {
      localStorage.setItem(LANGUAGE_KEY, language);
    } catch (error) {
      console.warn('Failed to persist language preference', error);
    }
  }, [language]);

  const t = useCallback((text: string) => translate(language, text), [language]);

  const [activePage, setActivePage] = useState<PageKey>('dashboard');
  const [period, setPeriod] = useState<PeriodKey>('month');
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [confirmDialog, setConfirmDialog] = useState<ConfirmDialogOptions | null>(null);

  // Modals state
  const [txModalOpen, setTxModalOpen] = useState(false);
  const [editingTx, setEditingTx] = useState<Transaction | null>(null);
  const [defaultTxType, setDefaultTxType] = useState<'income' | 'expense'>('income');

  const [invoiceBuilderOpen, setInvoiceBuilderOpen] = useState(false);
  const [editingInvoice, setEditingInvoice] = useState<Invoice | null>(null);

  const [invoiceViewOpen, setInvoiceViewOpen] = useState(false);
  const [viewingInvoice, setViewingInvoice] = useState<Invoice | null>(null);

  const [billModalOpen, setBillModalOpen] = useState(false);
  const [editingBill, setEditingBill] = useState<Bill | null>(null);

  // Sync to local storage
  useEffect(() => {
    try {
      localStorage.setItem(STORE_KEY, JSON.stringify(state));
    } catch (e) {
      console.warn('Failed to persist to localStorage', e);
    }
  }, [state]);

  const addToast = useCallback((msg: string, type: 'success' | 'error' | 'info' = 'success', action?: { label: string; onClick: () => void }) => {
    const id = uid('toast');
    setToasts(prev => [...prev, { id, message: msg, type, action }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, action ? 6500 : 4200);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  const showConfirm = useCallback((opts: ConfirmDialogOptions) => {
    setConfirmDialog(opts);
  }, []);

  const closeConfirm = useCallback(() => {
    setConfirmDialog(null);
  }, []);

  // Modal openers
  const openTxModal = useCallback((type: 'income' | 'expense' = 'income', tx: Transaction | null = null) => {
    setDefaultTxType(type);
    setEditingTx(tx);
    setTxModalOpen(true);
  }, []);

  const closeTxModal = useCallback(() => {
    setTxModalOpen(false);
    setEditingTx(null);
  }, []);

  const openInvoiceBuilder = useCallback((inv: Invoice | null = null) => {
    setEditingInvoice(inv);
    setInvoiceBuilderOpen(true);
  }, []);

  const closeInvoiceBuilder = useCallback(() => {
    setInvoiceBuilderOpen(false);
    setEditingInvoice(null);
  }, []);

  const openInvoiceView = useCallback((inv: Invoice) => {
    setViewingInvoice(inv);
    setInvoiceViewOpen(true);
  }, []);

  const closeInvoiceView = useCallback(() => {
    setInvoiceViewOpen(false);
    setViewingInvoice(null);
  }, []);

  const openBillModal = useCallback((bill: Bill | null = null) => {
    setEditingBill(bill);
    setBillModalOpen(true);
  }, []);

  const closeBillModal = useCallback(() => {
    setBillModalOpen(false);
    setEditingBill(null);
  }, []);

  // Sync Invoice payment to transaction
  const syncInvoicePayment = (inv: Invoice, currentTxns: Transaction[]): Transaction[] => {
    const m = invoiceMath(inv);
    const amount = inv.state === 'issued' ? m.paid : 0;
    if (amount <= 0) {
      if (inv.linkedTxnId) {
        return currentTxns.filter(t => t.id !== inv.linkedTxnId);
      }
      return currentTxns;
    }
    const date = inv.paidDate || inv.issueDate || todayISO();
    const existing = inv.linkedTxnId ? currentTxns.find(t => t.id === inv.linkedTxnId) : null;
    if (existing) {
      return currentTxns.map(t =>
        t.id === existing.id
          ? {
              ...t,
              date,
              description: `Malipo ya ankara ${inv.number}`,
              party: inv.customerName,
              amount,
              reference: inv.number,
              source: 'invoice',
              linkedId: inv.id
            }
          : t
      );
    } else {
      const newTx: Transaction = {
        id: uid('txn'),
        type: 'income',
        date,
        description: `Malipo ya ankara ${inv.number}`,
        party: inv.customerName,
        category: 'Mauzo ya bidhaa',
        amount,
        method: 'Benki',
        reference: inv.number,
        notes: 'Imeundwa kiotomatiki kutoka ankara.',
        source: 'invoice',
        linkedId: inv.id,
        createdAt: new Date().toISOString()
      };
      inv.linkedTxnId = newTx.id;
      return [newTx, ...currentTxns];
    }
  };

  // Sync Bill payment to transaction
  const syncBillPayment = (bill: Bill, currentTxns: Transaction[]): Transaction[] => {
    if (!bill.paid) {
      if (bill.linkedTxnId) {
        return currentTxns.filter(t => t.id !== bill.linkedTxnId);
      }
      return currentTxns;
    }
    const date = bill.paidDate || bill.dueDate || todayISO();
    const existing = bill.linkedTxnId ? currentTxns.find(t => t.id === bill.linkedTxnId) : null;
    if (existing) {
      return currentTxns.map(t =>
        t.id === existing.id
          ? {
              ...t,
              date,
              description: `Malipo: ${bill.name}`,
              party: bill.payee,
              category: bill.category,
              amount: bill.amount,
              method: bill.method || 'Benki',
              source: 'bill',
              linkedId: bill.id
            }
          : t
      );
    } else {
      const newTx: Transaction = {
        id: uid('txn'),
        type: 'expense',
        date,
        description: `Malipo: ${bill.name}`,
        party: bill.payee,
        category: bill.category,
        amount: bill.amount,
        method: bill.method || 'Benki',
        reference: '',
        notes: 'Imeundwa kiotomatiki kutoka bili.',
        source: 'bill',
        linkedId: bill.id,
        createdAt: new Date().toISOString()
      };
      bill.linkedTxnId = newTx.id;
      return [newTx, ...currentTxns];
    }
  };

  // Transaction CRUD
  const addTransaction = useCallback((tx: Omit<Transaction, 'id' | 'createdAt'>) => {
    const newTx: Transaction = {
      ...tx,
      id: uid('txn'),
      createdAt: new Date().toISOString()
    };
    setState(prev => ({
      ...prev,
      transactions: [newTx, ...prev.transactions],
      meta: { ...prev.meta, updatedAt: new Date().toISOString() }
    }));
    addToast('Miamala imehifadhiwa vizuri.');
  }, [addToast]);

  const updateTransaction = useCallback((id: string, updates: Partial<Transaction>) => {
    setState(prev => ({
      ...prev,
      transactions: prev.transactions.map(t => (t.id === id ? { ...t, ...updates } : t)),
      meta: { ...prev.meta, updatedAt: new Date().toISOString() }
    }));
    addToast('Miamala imehaririwa.');
  }, [addToast]);

  const deleteTransaction = useCallback((id: string) => {
    const snapshot = state;
    const target = state.transactions.find(t => t.id === id);
    if (!target) return;
    setState(prev => ({
      ...prev,
      transactions: prev.transactions.filter(t => t.id !== id),
      meta: { ...prev.meta, updatedAt: new Date().toISOString() }
    }));
    addToast('Miamala imefutwa.', 'success', {
      label: 'Tendua',
      onClick: () => {
        setState(snapshot);
        addToast('Imetenduliwa.');
      }
    });
  }, [state, addToast]);

  // Invoice CRUD
  const getNextInvoiceNumber = useCallback(() => {
    const year = new Date().getFullYear();
    const prefix = (state.settings.invoicePrefix || 'MD').toUpperCase().replace(/[^A-Z0-9]/g, '') || 'MD';
    const start = `${prefix}-${year}-`;
    let max = 0;
    state.invoices.forEach(inv => {
      if (String(inv.number).startsWith(start)) {
        const n = parseInt(String(inv.number).slice(start.length), 10);
        if (Number.isFinite(n)) max = Math.max(max, n);
      }
    });
    return `${start}${String(max + 1).padStart(3, '0')}`;
  }, [state.settings.invoicePrefix, state.invoices]);

  const addInvoice = useCallback((invData: Omit<Invoice, 'id' | 'createdAt' | 'updatedAt' | 'linkedTxnId'>): Invoice => {
    const newInv: Invoice = {
      ...invData,
      id: uid('inv'),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      linkedTxnId: null
    };

    setState(prev => {
      let updatedTxns = prev.transactions;
      updatedTxns = syncInvoicePayment(newInv, updatedTxns);
      return {
        ...prev,
        invoices: [newInv, ...prev.invoices],
        transactions: updatedTxns,
        meta: { ...prev.meta, updatedAt: new Date().toISOString() }
      };
    });

    addToast(newInv.state === 'draft' ? 'Rasimu ya ankara imehifadhiwa.' : 'Ankara imeundwa kikamilifu.');
    return newInv;
  }, [addToast]);

  const updateInvoice = useCallback((id: string, updates: Partial<Invoice>) => {
    setState(prev => {
      const oldInv = prev.invoices.find(i => i.id === id);
      if (!oldInv) return prev;
      const updatedInv: Invoice = {
        ...oldInv,
        ...updates,
        updatedAt: new Date().toISOString()
      };
      let updatedTxns = prev.transactions;
      updatedTxns = syncInvoicePayment(updatedInv, updatedTxns);
      return {
        ...prev,
        invoices: prev.invoices.map(i => (i.id === id ? updatedInv : i)),
        transactions: updatedTxns,
        meta: { ...prev.meta, updatedAt: new Date().toISOString() }
      };
    });
    addToast('Ankara imesasishwa.');
  }, [addToast]);

  const deleteInvoice = useCallback((id: string) => {
    const inv = state.invoices.find(i => i.id === id);
    if (!inv) return;
    showConfirm({
      title: 'Futa ankara?',
      message: `Ankara ${inv.number} ya "${inv.customerName}" itaondolewa kabisa pamoja na miamala ya malipo iliyounganishwa nayo.`,
      okLabel: 'Futa ankara',
      danger: true,
      onConfirm: () => {
        setState(prev => ({
          ...prev,
          invoices: prev.invoices.filter(i => i.id !== id),
          transactions: inv.linkedTxnId ? prev.transactions.filter(t => t.id !== inv.linkedTxnId) : prev.transactions,
          meta: { ...prev.meta, updatedAt: new Date().toISOString() }
        }));
        addToast('Ankara imefutwa.');
      }
    });
  }, [state.invoices, showConfirm, addToast]);

  const markInvoicePaid = useCallback((id: string) => {
    const inv = state.invoices.find(i => i.id === id);
    if (!inv) return;
    const m = invoiceMath(inv);
    if (m.total <= 0 || m.balance <= 0) {
      addToast('Ankara hii tayari imelipwa kikamilifu.');
      return;
    }
    const snapshot = state;
    setState(prev => {
      const target = prev.invoices.find(i => i.id === id);
      if (!target) return prev;
      const updatedInv: Invoice = {
        ...target,
        state: 'issued',
        amountPaid: m.total,
        paidDate: todayISO(),
        updatedAt: new Date().toISOString()
      };
      let updatedTxns = prev.transactions;
      updatedTxns = syncInvoicePayment(updatedInv, updatedTxns);
      return {
        ...prev,
        invoices: prev.invoices.map(i => (i.id === id ? updatedInv : i)),
        transactions: updatedTxns,
        meta: { ...prev.meta, updatedAt: new Date().toISOString() }
      };
    });
    addToast('Ankara imewekwa kama imelipwa na fedha zimeingizwa kwenye miamala.', 'success', {
      label: 'Tendua',
      onClick: () => {
        setState(snapshot);
        addToast('Imetenduliwa.');
      }
    });
  }, [state, addToast]);

  // Bill CRUD
  const addBill = useCallback((billData: Omit<Bill, 'id' | 'createdAt' | 'linkedTxnId'>) => {
    const newBill: Bill = {
      ...billData,
      id: uid('bill'),
      createdAt: new Date().toISOString(),
      linkedTxnId: null
    };
    setState(prev => {
      let updatedTxns = prev.transactions;
      updatedTxns = syncBillPayment(newBill, updatedTxns);
      return {
        ...prev,
        bills: [newBill, ...prev.bills],
        transactions: updatedTxns,
        meta: { ...prev.meta, updatedAt: new Date().toISOString() }
      };
    });
    addToast('Bili imehifadhiwa.');
  }, [addToast]);

  const updateBill = useCallback((id: string, updates: Partial<Bill>) => {
    setState(prev => {
      const oldBill = prev.bills.find(b => b.id === id);
      if (!oldBill) return prev;
      const updatedBill: Bill = {
        ...oldBill,
        ...updates
      };
      let updatedTxns = prev.transactions;
      updatedTxns = syncBillPayment(updatedBill, updatedTxns);
      return {
        ...prev,
        bills: prev.bills.map(b => (b.id === id ? updatedBill : b)),
        transactions: updatedTxns,
        meta: { ...prev.meta, updatedAt: new Date().toISOString() }
      };
    });
    addToast('Bili imesasishwa.');
  }, [addToast]);

  const deleteBill = useCallback((id: string) => {
    const bill = state.bills.find(b => b.id === id);
    if (!bill) return;
    showConfirm({
      title: 'Futa bili?',
      message: `Bili ya "${bill.name}" itaondolewa. Miamala iliyounganishwa nayo itaondolewa pia.`,
      okLabel: 'Futa bili',
      danger: true,
      onConfirm: () => {
        setState(prev => ({
          ...prev,
          bills: prev.bills.filter(b => b.id !== id),
          transactions: bill.linkedTxnId ? prev.transactions.filter(t => t.id !== bill.linkedTxnId) : prev.transactions,
          meta: { ...prev.meta, updatedAt: new Date().toISOString() }
        }));
        addToast('Bili imefutwa.');
      }
    });
  }, [state.bills, showConfirm, addToast]);

  const toggleBillPaid = useCallback((id: string) => {
    const bill = state.bills.find(b => b.id === id);
    if (!bill) return;
    const snapshot = state;
    setState(prev => {
      const target = prev.bills.find(b => b.id === id);
      if (!target) return prev;
      const newPaid = !target.paid;
      const updatedBill: Bill = {
        ...target,
        paid: newPaid,
        paidDate: newPaid ? todayISO() : ''
      };
      let updatedTxns = prev.transactions;
      updatedTxns = syncBillPayment(updatedBill, updatedTxns);
      return {
        ...prev,
        bills: prev.bills.map(b => (b.id === id ? updatedBill : b)),
        transactions: updatedTxns,
        meta: { ...prev.meta, updatedAt: new Date().toISOString() }
      };
    });
    addToast(
      bill.paid ? 'Malipo ya bili yametenduliwa.' : 'Bili imewekwa kama imelipwa na kurekodiwa kwenye matumizi.',
      'success',
      {
        label: 'Tendua',
        onClick: () => {
          setState(snapshot);
          addToast('Imetenduliwa.');
        }
      }
    );
  }, [state, addToast]);

  // Settings
  const updateSettings = useCallback((newSettings: Partial<BusinessSettings>) => {
    setState(prev => ({
      ...prev,
      settings: { ...prev.settings, ...newSettings },
      meta: { ...prev.meta, updatedAt: new Date().toISOString() }
    }));
    addToast('Mipangilio imehifadhiwa.');
  }, [addToast]);

  // Data management
  const resetToCleanData = useCallback(() => {
    setState(prev => ({
      ...generateEmptyState(),
      settings: prev.settings,
      meta: {
        demo: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    }));
    addToast('Data ya mfano imefutwa. Uko tayari kuanza na data yako.');
  }, [addToast]);

  const loadDemoData = useCallback(() => {
    setState(generateSeedState());
    addToast('Data ya mfano imepakiwa.');
  }, [addToast]);

  const restoreFromJSON = useCallback((parsed: AppState) => {
    setState({
      ...generateEmptyState(),
      ...parsed,
      settings: { ...DEFAULT_SETTINGS, ...(parsed.settings || {}) },
      meta: {
        demo: false,
        createdAt: parsed.meta?.createdAt || new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    });
    addToast('Data imerejeshwa vizuri kutoka kwenye faili la nakala rudufu.');
  }, [addToast]);

  return (
    <AppContext.Provider
      value={{
        state,
        language,
        setLanguage,
        t,
        theme,
        setTheme,
        activePage,
        setActivePage,
        period,
        setPeriod,
        isMobileSidebarOpen,
        setIsMobileSidebarOpen,
        toasts,
        addToast,
        removeToast,
        confirmDialog,
        showConfirm,
        closeConfirm,
        txModalOpen,
        editingTx,
        defaultTxType,
        openTxModal,
        closeTxModal,
        invoiceBuilderOpen,
        editingInvoice,
        openInvoiceBuilder,
        closeInvoiceBuilder,
        invoiceViewOpen,
        viewingInvoice,
        openInvoiceView,
        closeInvoiceView,
        billModalOpen,
        editingBill,
        openBillModal,
        closeBillModal,
        addTransaction,
        updateTransaction,
        deleteTransaction,
        addInvoice,
        updateInvoice,
        deleteInvoice,
        markInvoicePaid,
        addBill,
        updateBill,
        deleteBill,
        toggleBillPaid,
        updateSettings,
        resetToCleanData,
        loadDemoData,
        restoreFromJSON,
        getNextInvoiceNumber
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
};
