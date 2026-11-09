import { PeriodKey, Invoice, Bill, Transaction } from '../types';

export const MONTHS = [
  'Januari', 'Februari', 'Machi', 'Aprili', 'Mei', 'Juni',
  'Julai', 'Agosti', 'Septemba', 'Oktoba', 'Novemba', 'Desemba'
];

export const MONTHS_SHORT = [
  'Jan', 'Feb', 'Mac', 'Apr', 'Mei', 'Jun',
  'Jul', 'Ago', 'Sep', 'Okt', 'Nov', 'Des'
];

export const WEEKDAYS = [
  'Jumapili', 'Jumatatu', 'Jumanne', 'Jumatano', 'Alhamisi', 'Ijumaa', 'Jumamosi'
];

export const INCOME_CATEGORIES = [
  'Mauzo ya bidhaa',
  'Huduma',
  'Ushauri',
  'Mafunzo',
  'Uwekezaji',
  'Nyingine'
];

export const EXPENSE_CATEGORIES = [
  'Matumizi ya kila siku',
  'Bili za nyumbani',
  'Manunuzi / stock',
  'Kodi ya pango',
  'Umeme & maji',
  'Internet & simu',
  'Mishahara',
  'Usafiri',
  'Kodi & vibali',
  'Matengenezo',
  'Masoko & matangazo',
  'Nyingine'
];

export const BILL_CATEGORIES = [
  'Kodi ya pango',
  'Umeme',
  'Maji',
  'Umeme & maji',
  'Internet & simu',
  'Chakula',
  'Malipo ya fundi',
  'Matengenezo',
  'Mishahara',
  'Usafiri',
  'Mafuta',
  'Manunuzi / stock',
  'Bili za nyumbani',
  'Afya',
  'Elimu',
  'Kodi & vibali',
  'Bima',
  'Mkopo',
  'Masoko & matangazo',
  'Usafi',
  'Ulinzi',
  'Usafirishaji',
  'Ada za benki',
  'Vifaa vya kazi',
  'Nyingine'
];

export const METHODS = [
  'Cash',
  'M-Pesa',
  'Tigo Pesa',
  'Airtel Money',
  'Benki',
  'Cheque',
  'Nyingine'
];

export const CHART_COLORS = [
  '#1b5540',
  '#bd5a3d',
  '#c58a21',
  '#7ca18b',
  '#4b6f8b',
  '#8a7a5c',
  '#756a82'
];

const nf0 = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 });
const nf2 = new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 });

export function num(v: unknown, fallback = 0): number {
  const n = Number(v);
  return Number.isFinite(n) ? n : fallback;
}

export function fmt(n: number | string): string {
  return nf0.format(Math.round(num(n)));
}

export function fmt2(n: number | string): string {
  return nf2.format(num(n));
}

export function money(n: number | string): string {
  return 'TZS ' + fmt(n);
}

export function moneyCompact(n: number | string): string {
  const v = num(n);
  const a = Math.abs(v);
  if (a >= 1000000000) return (v / 1000000000).toFixed(a >= 10000000000 ? 0 : 1).replace(/\.0$/, '') + 'B';
  if (a >= 1000000) return (v / 1000000).toFixed(a >= 10000000 ? 0 : 1).replace(/\.0$/, '') + 'M';
  if (a >= 1000) return (v / 1000).toFixed(a >= 10000 ? 0 : 1).replace(/\.0$/, '') + 'K';
  return fmt(v);
}

export function uid(prefix = 'id'): string {
  return prefix + '_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 7);
}

export function pad(n: number): string {
  return String(n).padStart(2, '0');
}

export function localISO(d: Date): string {
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

export function todayISO(): string {
  return localISO(new Date());
}

export function parseISO(value: string | undefined | null): Date | null {
  if (!value) return null;
  const m = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!m) return null;
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), 12, 0, 0);
}

export function addDaysISO(iso: string, days: number): string {
  const d = parseISO(iso) || new Date();
  d.setDate(d.getDate() + Number(days || 0));
  return localISO(d);
}

export function daysFromToday(iso: string): number {
  const d = parseISO(iso);
  if (!d) return 0;
  const t = parseISO(todayISO());
  if (!t) return 0;
  return Math.round((d.getTime() - t.getTime()) / 86400000);
}

export function dateLabel(iso: string | undefined | null, language: 'sw' | 'en' = 'sw'): string {
  const d = parseISO(iso);
  if (!d) return '—';
  if (language === 'en') {
    return new Intl.DateTimeFormat('en-US', { day: 'numeric', month: 'short', year: 'numeric' }).format(d);
  }
  return d.getDate() + ' ' + MONTHS_SHORT[d.getMonth()] + ' ' + d.getFullYear();
}

export function dateLong(iso: string | undefined | null, language: 'sw' | 'en' = 'sw'): string {
  const d = parseISO(iso);
  if (!d) return '—';
  if (language === 'en') {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(d);
  }
  return WEEKDAYS[d.getDay()] + ', ' + d.getDate() + ' ' + MONTHS[d.getMonth()] + ' ' + d.getFullYear();
}

export function monthKey(iso: string | undefined | null): string {
  return (iso || '').slice(0, 7);
}

export function monthLabel(key: string, language: 'sw' | 'en' = 'sw'): string {
  if (!key) return '—';
  const parts = key.split('-');
  const m = Number(parts[1]) - 1;
  if (language === 'en') {
    return new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(new Date(Number(parts[0]), m, 1));
  }
  return (MONTHS[m] || '') + ' ' + parts[0];
}

export function relTime(iso: string | undefined | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  const diff = Date.now() - d.getTime();
  if (diff < 60000) return 'sasa hivi';
  if (diff < 3600000) return Math.floor(diff / 60000) + ' dakika zilizopita';
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' saa zilizopita';
  if (diff < 604800000) return Math.floor(diff / 86400000) + ' siku zilizopita';
  return dateLabel(localISO(d));
}

export function initials(name: string | undefined | null): string {
  const parts = String(name || 'Mengi Data').trim().split(/\s+/).filter(Boolean);
  return ((parts[0] || 'M').slice(0, 1) + (parts[1] || '').slice(0, 1)).toUpperCase();
}

export function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n));
}

export function periodBounds(period: PeriodKey) {
  const now = new Date();
  const y = now.getFullYear();
  const m = now.getMonth();
  const d = now.getDate();

  if (period === 'day') {
    const today = new Date(y, m, d, 0, 0, 0);
    const todayEnd = new Date(y, m, d, 23, 59, 59);
    const prev = new Date(y, m, d - 1, 0, 0, 0);
    const prevEnd = new Date(y, m, d - 1, 23, 59, 59);
    return {
      key: 'day',
      start: today,
      end: todayEnd,
      prevStart: prev,
      prevEnd: prevEnd,
      label: 'Leo',
      prevLabel: 'Jana'
    };
  }
  if (period === 'week') {
    const dayOfWeek = now.getDay();
    const startOfWeek = new Date(y, m, d - dayOfWeek, 0, 0, 0);
    const endOfWeek = new Date(y, m, d + (6 - dayOfWeek), 23, 59, 59);
    const prevStart = new Date(y, m, d - dayOfWeek - 7, 0, 0, 0);
    const prevEnd = new Date(y, m, d - dayOfWeek - 1, 23, 59, 59);
    return {
      key: 'week',
      start: startOfWeek,
      end: endOfWeek,
      prevStart,
      prevEnd,
      label: 'Wiki hii',
      prevLabel: 'Wiki iliyopita'
    };
  }
  if (period === 'months6') {
    return {
      key: 'months6',
      start: new Date(y, m - 5, 1, 0, 0, 0),
      end: new Date(y, m + 1, 0, 23, 59, 59),
      prevStart: new Date(y, m - 11, 1, 0, 0, 0),
      prevEnd: new Date(y, m - 5, 0, 23, 59, 59),
      label: 'Miezi 6 iliyopita',
      prevLabel: 'Miezi 6 ya awali'
    };
  }
  if (period === 'year') {
    return {
      key: 'year',
      start: new Date(y, 0, 1, 0, 0, 0),
      end: new Date(y, 11, 31, 23, 59, 59),
      prevStart: new Date(y - 1, 0, 1, 0, 0, 0),
      prevEnd: new Date(y - 1, 11, 31, 23, 59, 59),
      label: 'Mwaka ' + y,
      prevLabel: 'Mwaka uliopita'
    };
  }
  return {
    key: 'month',
    start: new Date(y, m, 1, 0, 0, 0),
    end: new Date(y, m + 1, 0, 23, 59, 59),
    prevStart: new Date(y, m - 1, 1, 0, 0, 0),
    prevEnd: new Date(y, m, 0, 23, 59, 59),
    label: MONTHS[m] + ' ' + y,
    prevLabel: MONTHS[(m + 11) % 12]
  };
}

export function inBounds(iso: string, start: Date | null, end: Date | null): boolean {
  if (!start || !end) return true;
  const d = parseISO(iso);
  if (!d) return false;
  return d >= start && d <= end;
}

export function lastMonths(n: number, language: 'sw' | 'en' = 'sw') {
  const out = [];
  const now = new Date();
  for (let i = n - 1; i >= 0; i--) {
    const start = new Date(now.getFullYear(), now.getMonth() - i, 1, 0, 0, 0);
    const end = new Date(now.getFullYear(), now.getMonth() - i + 1, 0, 23, 59, 59);
    out.push({
      key: localISO(start).slice(0, 7),
      label: language === 'en'
        ? new Intl.DateTimeFormat('en-US', { month: 'short' }).format(start)
        : MONTHS_SHORT[start.getMonth()],
      full: language === 'en'
        ? new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(start)
        : MONTHS[start.getMonth()] + ' ' + start.getFullYear(),
      year: start.getFullYear(),
      start,
      end
    });
  }
  return out;
}

export interface InvoiceCalculations {
  subtotal: number;
  discount: number;
  vat: number;
  total: number;
  paid: number;
  balance: number;
}

export function invoiceMath(inv: Invoice | { items: { qty: number; price: number }[]; discount: number; vatRate: number; amountPaid: number }): InvoiceCalculations {
  const subtotal = (inv.items || []).reduce((sum, item) => sum + (num(item.qty) * num(item.price)), 0);
  const discount = clamp(num(inv.discount), 0, subtotal);
  const taxable = Math.max(0, subtotal - discount);
  const vat = taxable * (clamp(num(inv.vatRate), 0, 100) / 100);
  const total = Math.round(taxable + vat);
  const paid = clamp(Math.round(num(inv.amountPaid)), 0, total);
  return {
    subtotal: Math.round(subtotal),
    discount: Math.round(discount),
    vat: Math.round(vat),
    total,
    paid,
    balance: Math.max(0, total - paid)
  };
}

export type StatusKey = 'paid' | 'partial' | 'overdue' | 'unpaid' | 'draft';

export function invoiceStatus(inv: Invoice): { key: StatusKey; label: string } {
  if (inv.state === 'draft') return { key: 'draft', label: 'Rasimu' };
  const m = invoiceMath(inv);
  if (m.total > 0 && m.balance <= 0) return { key: 'paid', label: 'Imelipwa' };
  if (m.paid > 0) return { key: 'partial', label: 'Malipo ya sehemu' };
  if (inv.dueDate && inv.dueDate < todayISO()) return { key: 'overdue', label: 'Imechelewa' };
  return { key: 'unpaid', label: 'Haijalipwa' };
}

export function billStatus(bill: Bill): { key: 'paid' | 'overdue' | 'due' | 'unpaid'; label: string } {
  if (bill.paid) return { key: 'paid', label: 'Imelipwa' };
  const days = daysFromToday(bill.dueDate);
  if (days < 0) return { key: 'overdue', label: 'Imechelewa' };
  if (days <= 7) return { key: 'due', label: 'Inakaribia' };
  return { key: 'unpaid', label: 'Haijalipwa' };
}

export function sortTxNewest(a: Transaction, b: Transaction): number {
  const byDate = String(b.date).localeCompare(String(a.date));
  if (byDate !== 0) return byDate;
  return String(b.createdAt).localeCompare(String(a.createdAt));
}

export function totalsFor(period: PeriodKey, transactions: Transaction[]) {
  const b = periodBounds(period);
  let income = 0;
  let expense = 0;
  for (const t of transactions) {
    if (!inBounds(t.date, b.start, b.end)) continue;
    if (t.type === 'income') income += num(t.amount);
    else expense += num(t.amount);
  }

  let prevIncome = 0;
  let prevExpense = 0;
  if (b.prevStart) {
    for (const t of transactions) {
      if (!inBounds(t.date, b.prevStart, b.prevEnd)) continue;
      if (t.type === 'income') prevIncome += num(t.amount);
      else prevExpense += num(t.amount);
    }
  }

  return {
    income,
    expense,
    profit: income - expense,
    prevIncome,
    prevExpense,
    bounds: b
  };
}

export function overdueBills(bills: Bill[]): Bill[] {
  const today = todayISO();
  return bills.filter(b => !b.paid && b.dueDate < today);
}

export function overdueInvoices(invoices: Invoice[]): Invoice[] {
  const today = todayISO();
  return invoices.filter(i => {
    if (i.state === 'draft') return false;
    const m = invoiceMath(i);
    return m.balance > 0 && i.dueDate < today;
  });
}
