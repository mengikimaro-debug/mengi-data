export type TransactionType = 'income' | 'expense';

export type TransactionSource = 'manual' | 'invoice' | 'bill';

export interface Transaction {
  id: string;
  type: TransactionType;
  date: string; // YYYY-MM-DD
  description: string;
  party: string; // Customer or Supplier
  category: string;
  amount: number;
  method: string;
  reference: string;
  notes: string;
  source: TransactionSource;
  linkedId: string | null;
  createdAt: string;
}

export interface InvoiceItem {
  description: string;
  qty: number;
  price: number;
}

export type InvoiceState = 'issued' | 'draft';

export interface Invoice {
  id: string;
  number: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  customerAddress: string;
  issueDate: string;
  dueDate: string;
  items: InvoiceItem[];
  discount: number;
  vatRate: number; // percentage e.g. 18 or 0
  amountPaid: number;
  state: InvoiceState;
  paidDate: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  linkedTxnId: string | null;
}

export interface Bill {
  id: string;
  name: string;
  category: string;
  payee: string;
  amount: number;
  dueDate: string;
  paid: boolean;
  paidDate: string;
  method: string;
  recurring: boolean;
  notes: string;
  createdAt: string;
  linkedTxnId: string | null;
}

export interface BusinessSettings {
  businessName: string;
  ownerName: string;
  tagline: string;
  address: string;
  phone: string;
  email: string;
  tin: string;
  website: string;
  invoicePrefix: string;
  vatRate: number;
  paymentDetails: string;
  invoiceNote: string;
  currency: string;
}

export interface AppMeta {
  demo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AppState {
  version: number;
  transactions: Transaction[];
  invoices: Invoice[];
  bills: Bill[];
  settings: BusinessSettings;
  meta: AppMeta;
}

export type PageKey = 'dashboard' | 'transactions' | 'invoices' | 'bills' | 'reports' | 'settings';
export type PeriodKey = 'month' | 'quarter' | 'year' | 'all';
export type InvoiceFilterKey = 'all' | 'due' | 'paid' | 'draft';
export type BillFilterKey = 'all' | 'unpaid' | 'pending' | 'paid';
export type TransactionFilterType = 'all' | 'income' | 'expense' | 'pending';

export interface ToastMessage {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info';
  action?: {
    label: string;
    onClick: () => void;
  };
}

export interface ConfirmDialogOptions {
  title: string;
  message: string;
  okLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
}
