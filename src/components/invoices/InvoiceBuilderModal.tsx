import React, { useState, useEffect, useMemo } from 'react';
import { X, Plus, Trash2, Download, Calculator } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { InvoiceItem, InvoiceState } from '../../types';
import {
  todayISO,
  addDaysISO,
  fmt,
  money,
  num,
  clamp
} from '../../utils/format';
import { buildInvoicePDF } from '../../utils/pdfGenerator';
import { downloadBlob } from '../../utils/export';

export const InvoiceBuilderModal: React.FC = () => {
  const {
    state,
    invoiceBuilderOpen,
    editingInvoice,
    closeInvoiceBuilder,
    addInvoice,
    updateInvoice,
    openInvoiceView,
    getNextInvoiceNumber,
    addToast,
    t
  } = useApp();

  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [customerEmail, setCustomerEmail] = useState('');
  const [customerAddress, setCustomerAddress] = useState('');

  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [invoiceState, setInvoiceState] = useState<InvoiceState>('issued');
  const [issueDate, setIssueDate] = useState(todayISO());
  const [dueDate, setDueDate] = useState(addDaysISO(todayISO(), 14));

  const [items, setItems] = useState<InvoiceItem[]>([
    { description: '', qty: 1, price: 0 }
  ]);

  const [discount, setDiscount] = useState<number>(0);
  const [vatRate, setVatRate] = useState<number>(state.settings.vatRate || 0);
  const [amountPaid, setAmountPaid] = useState<number>(0);
  const [paidDate, setPaidDate] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  // Autocomplete suggestions from existing invoices
  const customerSuggestions = useMemo(() => {
    const list: string[] = [];
    state.invoices.forEach(inv => {
      if (inv.customerName && !list.includes(inv.customerName)) {
        list.push(inv.customerName);
      }
    });
    return list;
  }, [state.invoices]);

  // Load editing invoice or defaults
  useEffect(() => {
    if (editingInvoice) {
      setCustomerName(editingInvoice.customerName);
      setCustomerPhone(editingInvoice.customerPhone);
      setCustomerEmail(editingInvoice.customerEmail);
      setCustomerAddress(editingInvoice.customerAddress);
      setInvoiceNumber(editingInvoice.number);
      setInvoiceState(editingInvoice.state);
      setIssueDate(editingInvoice.issueDate);
      setDueDate(editingInvoice.dueDate);
      setItems(
        editingInvoice.items.length
          ? editingInvoice.items.map(it => ({ ...it }))
          : [{ description: '', qty: 1, price: 0 }]
      );
      setDiscount(editingInvoice.discount);
      setVatRate(editingInvoice.vatRate);
      setAmountPaid(editingInvoice.amountPaid);
      setPaidDate(editingInvoice.paidDate || '');
      setNotes(editingInvoice.notes || '');
    } else {
      setCustomerName('');
      setCustomerPhone('');
      setCustomerEmail('');
      setCustomerAddress('');
      setInvoiceNumber(getNextInvoiceNumber());
      setInvoiceState('issued');
      setIssueDate(todayISO());
      setDueDate(addDaysISO(todayISO(), 14));
      setItems([{ description: '', qty: 1, price: 0 }]);
      setDiscount(0);
      setVatRate(state.settings.vatRate || 0);
      setAmountPaid(0);
      setPaidDate('');
      setNotes(state.settings.invoiceNote || '');
    }
  }, [editingInvoice, invoiceBuilderOpen, getNextInvoiceNumber, state.settings]);

  if (!invoiceBuilderOpen) return null;

  // Real-time calculations
  const subtotal = items.reduce((sum, item) => sum + num(item.qty) * num(item.price), 0);
  const cleanDiscount = clamp(num(discount), 0, subtotal);
  const taxable = Math.max(0, subtotal - cleanDiscount);
  const vatAmount = taxable * (clamp(num(vatRate), 0, 100) / 100);
  const grandTotal = Math.round(taxable + vatAmount);
  const cleanPaid = clamp(num(amountPaid), 0, grandTotal);
  const balance = Math.max(0, grandTotal - cleanPaid);

  let statusBadge = { label: t('Haijalipwa'), color: 'bg-[#f3efe5] text-[#746e7d]' };
  if (invoiceState === 'draft') {
    statusBadge = { label: t('Rasimu'), color: 'bg-[#ece7da] text-[#746e7d]' };
  } else if (grandTotal > 0 && balance === 0) {
    statusBadge = { label: t('Imelipwa'), color: 'bg-[#e8f0e8] text-[#1b5540]' };
  } else if (cleanPaid > 0) {
    statusBadge = { label: t('Malipo ya sehemu'), color: 'bg-[#f8edd3] text-[#946510]' };
  } else if (dueDate < todayISO()) {
    statusBadge = { label: t('Imechelewa'), color: 'bg-[#f7e4db] text-[#9e432b]' };
  }

  // Autofill customer contact when selecting or typing known customer
  const handleCustomerChange = (name: string) => {
    setCustomerName(name);
    const existing = state.invoices.find(
      i => i.customerName.toLowerCase() === name.trim().toLowerCase()
    );
    if (existing) {
      if (!customerPhone && existing.customerPhone) setCustomerPhone(existing.customerPhone);
      if (!customerEmail && existing.customerEmail) setCustomerEmail(existing.customerEmail);
      if (!customerAddress && existing.customerAddress) setCustomerAddress(existing.customerAddress);
    }
  };

  const handleItemChange = (index: number, field: keyof InvoiceItem, value: string | number) => {
    setItems(prev => {
      const copy = [...prev];
      copy[index] = {
        ...copy[index],
        [field]: field === 'description' ? String(value) : Math.max(0, num(value))
      };
      return copy;
    });
  };

  const addItemRow = () => {
    setItems(prev => [...prev, { description: '', qty: 1, price: 0 }]);
  };

  const removeItemRow = (index: number) => {
    setItems(prev => {
      if (prev.length <= 1) return [{ description: '', qty: 1, price: 0 }];
      return prev.filter((_, idx) => idx !== index);
    });
  };

  const handlePayFull = () => {
    setAmountPaid(grandTotal);
    if (!paidDate) setPaidDate(todayISO());
  };

  const handleSave = (asDraft = false, downloadPDF = false) => {
    if (!customerName.trim()) {
      addToast(t('Tafadhali weka jina la mteja.'), 'error');
      return;
    }
    if (!invoiceNumber.trim()) {
      addToast(t('Tafadhali weka namba ya ankara.'), 'error');
      return;
    }

    // Check duplicate invoice number
    const isDuplicate = state.invoices.some(
      i => i.number.toLowerCase() === invoiceNumber.trim().toLowerCase() && i.id !== editingInvoice?.id
    );
    if (isDuplicate) {
      addToast(t('Namba hii ya ankara tayari inatumika. Tumia namba tofauti.'), 'error');
      return;
    }

    const validItems = items.filter(i => i.description.trim() || i.price > 0);
    if (validItems.length === 0) {
      addToast(t('Tafadhali ongeza angalau bidhaa au huduma moja yenye maelezo.'), 'error');
      return;
    }

    const targetState: InvoiceState = asDraft ? 'draft' : invoiceState;
    const finalPaid = targetState === 'draft' ? 0 : cleanPaid;
    const finalPaidDate = targetState === 'draft' ? '' : (cleanPaid > 0 ? (paidDate || todayISO()) : '');

    const invoicePayload = {
      number: invoiceNumber.trim(),
      customerName: customerName.trim(),
      customerPhone: customerPhone.trim(),
      customerEmail: customerEmail.trim(),
      customerAddress: customerAddress.trim(),
      issueDate,
      dueDate,
      items: validItems,
      discount: cleanDiscount,
      vatRate: num(vatRate),
      amountPaid: finalPaid,
      state: targetState,
      paidDate: finalPaidDate,
      notes: notes.trim()
    };

    let savedInv;
    if (editingInvoice) {
      updateInvoice(editingInvoice.id, invoicePayload);
      savedInv = { ...editingInvoice, ...invoicePayload };
    } else {
      savedInv = addInvoice(invoicePayload);
    }

    closeInvoiceBuilder();
    openInvoiceView(savedInv);

    if (downloadPDF) {
      setTimeout(() => {
        try {
          const blob = buildInvoicePDF(savedInv, state.settings);
          const safeName = (`Ankara-${savedInv.number}-${savedInv.customerName}`)
            .replace(/[^a-z0-9\-_]+/gi, '-')
            .replace(/-+/g, '-');
          downloadBlob(blob, `${safeName}.pdf`);
        } catch (e) {
          console.error(e);
        }
      }, 200);
    }
  };

  return (
    <div className="premium-backdrop fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-[#081e16]/60 backdrop-blur-xs overflow-y-auto">
      <div className="premium-enter relative w-full max-w-4xl max-h-[92vh] flex flex-col bg-[#f3efe5] border border-white/20 rounded-[22px] shadow-[0_24px_70px_rgba(8,30,22,0.22)] overflow-hidden">
        {/* Modal Head */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#e2dccd] bg-[#fcfaf4]/75 shrink-0">
          <div>
            <div className="text-[9px] font-bold tracking-[0.18em] uppercase text-[#1b5540]">
              {t('Ankara & malipo')}
            </div>
            <h2 className="font-display text-[22px] font-semibold text-[#14231c] tracking-tight">
              {editingInvoice ? t('Hariri ankara') : t('Ankara mpya')}
            </h2>
          </div>
          <button
            onClick={closeInvoiceBuilder}
            className="w-8 h-8 rounded-[9px] border border-[#e2dccd] bg-[#fcfaf4] text-[#746e7d] hover:text-[#14231c] hover:bg-white grid place-items-center transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body: Split Form + Sticky Summary */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
            {/* Left 2 Columns: Input Sections */}
            <div className="lg:col-span-2 space-y-5">
              {/* Customer Details Section */}
              <div className="p-4 rounded-[15px] border border-[#e2dccd] bg-[#fcfaf4]/60 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold tracking-tight text-[#14231c]">
                    {t('Mteja')}
                  </span>
                  <span className="text-[10px] text-[#746e7d]">{t('Nani unamtolea ankara?')}</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Jina la mteja *')}
                    </label>
                    <input
                      type="text"
                      list="customer-suggestions"
                      value={customerName}
                      onChange={e => handleCustomerChange(e.target.value)}
                      placeholder={t('Jina la mtu au kampuni')}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                      required
                    />
                    <datalist id="customer-suggestions">
                      {customerSuggestions.map(s => (
                        <option key={s} value={s} />
                      ))}
                    </datalist>
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Simu')}
                    </label>
                    <input
                      type="text"
                      value={customerPhone}
                      onChange={e => setCustomerPhone(e.target.value)}
                      placeholder="+255 7..."
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Barua pepe')}
                    </label>
                    <input
                      type="email"
                      value={customerEmail}
                      onChange={e => setCustomerEmail(e.target.value)}
                      placeholder="mteja@barua.co.tz"
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Anwani / Eneo')}
                    </label>
                    <input
                      type="text"
                      value={customerAddress}
                      onChange={e => setCustomerAddress(e.target.value)}
                      placeholder={t('Mtaa, Mji, n.k.')}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>
                </div>
              </div>

              {/* Invoice Meta Section */}
              <div className="p-4 rounded-[15px] border border-[#e2dccd] bg-[#fcfaf4]/60 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold tracking-tight text-[#14231c]">
                    {t('Taarifa za ankara')}
                  </span>
                  <span className="text-[10px] text-[#746e7d]">{t('Namba, hali na tarehe')}</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Namba ya ankara *')}
                    </label>
                    <input
                      type="text"
                      value={invoiceNumber}
                      onChange={e => setInvoiceNumber(e.target.value)}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono text-[#14231c] font-bold focus:outline-none focus:border-[#1b5540]"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Hali ya ankara')}
                    </label>
                    <select
                      value={invoiceState}
                      onChange={e => setInvoiceState(e.target.value as InvoiceState)}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    >
                      <option value="issued">{t('Imetolewa (Rasmi)')}</option>
                      <option value="draft">{t('Rasimu (Inatayarishwa)')}</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Tarehe ya kutolea')}
                    </label>
                    <input
                      type="date"
                      value={issueDate}
                      onChange={e => setIssueDate(e.target.value)}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      {t('Tarehe ya mwisho wa malipo')}
                    </label>
                    <input
                      type="date"
                      value={dueDate}
                      onChange={e => setDueDate(e.target.value)}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Items Table Section */}
              <div className="p-4 rounded-[15px] border border-[#e2dccd] bg-[#fcfaf4]/60 space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-[11px] font-bold tracking-tight text-[#14231c]">
                      Bidhaa & Huduma
                    </div>
                    <div className="text-[10px] text-[#746e7d]">{t('Ongeza vitu unavyotoza')}</div>
                  </div>
                  <button
                    type="button"
                    onClick={addItemRow}
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-[7px] bg-[#fcfaf4] border border-[#d2cab7] text-[11px] font-semibold text-[#14231c] hover:bg-white"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>{t('Ongeza mstari')}</span>
                  </button>
                </div>

                {/* Items container */}
                <div className="rounded-[12px] border border-[#e2dccd] overflow-hidden bg-[#f8f4ea]">
                  <div className="hidden sm:grid grid-cols-12 gap-2 p-2 px-3 bg-[#ece7da]/60 border-b border-[#e2dccd] text-[9px] font-bold uppercase tracking-wider text-[#746e7d]">
                    <span className="col-span-5">{t('Maelezo')}</span>
                    <span className="col-span-2 text-right">{t('Idadi')}</span>
                    <span className="col-span-2 text-right">{t('Bei (TZS)')}</span>
                    <span className="col-span-2 text-right">{t('Jumla')}</span>
                    <span className="col-span-1" />
                  </div>

                  <div className="divide-y divide-[#ece7da]">
                    {items.map((item, idx) => {
                      const lineTotal = num(item.qty) * num(item.price);
                      return (
                        <div key={idx} className="grid grid-cols-2 sm:grid-cols-12 gap-2 p-3 sm:px-3 sm:py-2 items-center text-[12px]">
                          <div className="col-span-2 sm:col-span-5">
                            <label className="sm:hidden block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                              Maelezo
                            </label>
                            <input
                              type="text"
                              value={item.description}
                              onChange={e => handleItemChange(idx, 'description', e.target.value)}
                              placeholder={t('Maelezo ya bidhaa au huduma')}
                              aria-label="Maelezo ya bidhaa au huduma"
                              className="w-full h-[36px] sm:h-[34px] px-2 rounded-[6px] bg-white sm:bg-transparent hover:bg-white focus:bg-white border border-[#e2dccd] sm:border-transparent focus:border-[#1b5540] text-[12px] text-[#14231c]"
                            />
                          </div>
                          <div className="col-span-1 sm:col-span-2">
                            <label className="sm:hidden block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                              Idadi
                            </label>
                            <input
                              type="number"
                              min="0"
                              step="any"
                              value={item.qty}
                              onChange={e => handleItemChange(idx, 'qty', e.target.value)}
                              aria-label="Idadi"
                              className="w-full h-[36px] sm:h-[34px] px-2 text-right rounded-[6px] bg-white sm:bg-transparent hover:bg-white focus:bg-white border border-[#e2dccd] sm:border-transparent focus:border-[#1b5540] text-[12px] font-mono"
                            />
                          </div>
                          <div className="col-span-1 sm:col-span-2">
                            <label className="sm:hidden block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                              Bei (TZS)
                            </label>
                            <input
                              type="number"
                              min="0"
                              step="any"
                              value={item.price}
                              onChange={e => handleItemChange(idx, 'price', e.target.value)}
                              aria-label="Bei kwa kipande, TZS"
                              className="w-full h-[36px] sm:h-[34px] px-2 text-right rounded-[6px] bg-white sm:bg-transparent hover:bg-white focus:bg-white border border-[#e2dccd] sm:border-transparent focus:border-[#1b5540] text-[12px] font-mono"
                            />
                          </div>
                          <div className="col-span-1 sm:col-span-2 text-right">
                            <label className="sm:hidden block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1 text-left">
                              Jumla
                            </label>
                            <div className="h-[36px] sm:h-auto flex items-center justify-end font-mono font-bold text-[#14231c]">
                              {fmt(lineTotal)}
                            </div>
                          </div>
                          <div className="col-span-2 sm:col-span-1 flex justify-end">
                            <button
                              type="button"
                              onClick={() => removeItemRow(idx)}
                              aria-label={`Ondoa bidhaa ${idx + 1}`}
                              className="w-6 h-6 rounded grid place-items-center text-[#98a39b] hover:text-[#9e432b] hover:bg-[#f7e4db]"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="flex items-center justify-between gap-3 p-3 rounded-[10px] bg-[#e8f0e8] border border-[#e2dccd]">
                  <div className="flex items-center gap-2 text-[11px] text-[#34483e]">
                    <Calculator className="w-4 h-4 text-[#1b5540]" />
                    <span>{t('Jumla ya bidhaa (idadi × bei)')}</span>
                  </div>
                  <strong className="text-[12px] font-mono text-[#14231c]">{money(subtotal)}</strong>
                </div>
              </div>

              {/* Adjustments: Discount, VAT, Amount Paid, Notes */}
              <div className="p-4 rounded-[15px] border border-[#e2dccd] bg-[#fcfaf4]/60 space-y-3">
                <div className="text-[11px] font-bold tracking-tight text-[#14231c]">
                  Punguzo, Kodi & Malipo
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      Punguzo (TZS)
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      value={discount}
                      onChange={e => setDiscount(Math.max(0, num(e.target.value)))}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      VAT ya Serikali (%)
                    </label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      step="0.5"
                      value={vatRate}
                      onChange={e => setVatRate(clamp(num(e.target.value), 0, 100))}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                    <div className="text-[9.5px] text-[#98a39b] mt-0.5">{t('Weka 0 kama haujasajiliwa na VAT.')}</div>
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold">
                        Kiasi kilicholipwa (TZS)
                      </label>
                      <button
                        type="button"
                        onClick={handlePayFull}
                        className="text-[9.5px] font-bold text-[#1b5540] hover:underline"
                      >
                        {t('Weka kiasi chote')}
                      </button>
                    </div>
                    <input
                      type="number"
                      min="0"
                      step="any"
                      value={amountPaid}
                      onChange={e => setAmountPaid(Math.max(0, num(e.target.value)))}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>

                  <div>
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      Tarehe ya malipo
                    </label>
                    <input
                      type="date"
                      value={paidDate}
                      onChange={e => setPaidDate(e.target.value)}
                      className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>

                  <div className="sm:col-span-2">
                    <label className="block text-[9px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                      Maelezo ya ziada au masharti ya ankara
                    </label>
                    <textarea
                      rows={2}
                      value={notes}
                      onChange={e => setNotes(e.target.value)}
                      placeholder={t('Maelezo yataonekana chini ya ankara...')}
                      className="w-full p-2.5 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Right 1 Column: Real-time Live Summary Card */}
            <div className="sticky top-0 p-5 rounded-[18px] bg-gradient-to-br from-[#123a2c] to-[#0a261d] text-[#f2f8eb] shadow-[0_14px_35px_rgba(8,30,22,0.16)] space-y-3">
              <div className="text-[9px] font-bold uppercase tracking-[0.14em] text-[#a3baac]">
                {t('Jumla ya ankara')}
              </div>

              <div className="font-display text-[30px] font-bold text-[#d8f36b] tracking-tight leading-none font-mono-numbers">
                {money(grandTotal)}
              </div>

              <div className="pt-3 border-t border-white/10 space-y-2 text-[11.5px]">
                <div className="flex justify-between text-[#d2cab7]">
                  <span>{t('Jumla ndogo')}</span>
                  <span className="font-mono text-white font-semibold">{money(subtotal)}</span>
                </div>

                <div className="flex justify-between text-[#d2cab7]">
                  <span>{t('Punguzo')}</span>
                  <span className="font-mono text-white font-semibold">− {money(cleanDiscount)}</span>
                </div>

                <div className="flex justify-between text-[#d2cab7]">
                  <span>VAT ({vatRate}%)</span>
                  <span className="font-mono text-white font-semibold">{money(vatAmount)}</span>
                </div>

                <div className="pt-2 border-t border-white/10 flex justify-between font-bold text-white text-[12.5px]">
                  <span>{t('Jumla kuu')}</span>
                  <span className="font-mono text-[#d8f36b]">{money(grandTotal)}</span>
                </div>

                <div className="flex justify-between text-[#d2cab7]">
                  <span>{t('Kiasi kilicholipwa')}</span>
                  <span className="font-mono text-white font-semibold">{money(cleanPaid)}</span>
                </div>

                <div className="flex justify-between font-bold text-[12px]">
                  <span className="text-[#f7e4db]">{t('Salio linalodaiwa')}</span>
                  <span className="font-mono text-[#f7e4db]">{money(balance)}</span>
                </div>
              </div>

              <div className="pt-3 border-t border-white/10 flex items-center justify-between">
                <span className="text-[10px] text-[#a3baac]">{t('Hali ya malipo:')}</span>
                <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${statusBadge.color}`}>
                  {statusBadge.label}
                </span>
              </div>

              <div className="p-2.5 rounded-[10px] bg-white/[0.04] border border-white/10 text-[10px] text-[#a3baac] leading-relaxed">
                <strong>{t('Kidokezo:')}</strong> {t('Ankara iliyolipwa itaingiza mapato kwenye daftari la miamala kiotomatiki. PDF inazalishwa bila hitaji la internet.')}
              </div>
            </div>
          </div>
        </div>

        {/* Modal Foot Actions */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-6 py-3.5 border-t border-[#e2dccd] bg-[#fcfaf4]/75 shrink-0">
          <div className="text-[11px] text-[#746e7d] hidden sm:block">
            {t('Namba:')} <strong className="text-[#14231c]">{invoiceNumber || '—'}</strong>
          </div>

          <div className="flex items-center gap-2 ml-auto">
            <button
              type="button"
              onClick={closeInvoiceBuilder}
              className="px-3.5 py-2 rounded-[10px] text-[12px] font-semibold text-[#746e7d] hover:bg-[#ece7da] hover:text-[#14231c] transition-colors"
            >
              {t('Ghairi')}
            </button>

            <button
              type="button"
              onClick={() => handleSave(true, false)}
              className="px-3.5 py-2 rounded-[10px] bg-[#fcfaf4] border border-[#d2cab7] text-[12px] font-semibold text-[#14231c] hover:bg-white transition-colors"
            >
              {t('Hifadhi rasimu')}
            </button>

            <button
              type="button"
              onClick={() => handleSave(false, true)}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white font-semibold text-[12px] hover:bg-[#123a2c] shadow-[0_7px_17px_rgba(12,43,34,0.16)] transition-all"
            >
              <Download className="w-4 h-4" />
              <span>{t('Hifadhi & Pakua PDF')}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
