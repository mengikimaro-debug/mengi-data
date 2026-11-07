import React from 'react';
import { X, Printer, Download, Edit2 } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import {
  fmt,
  money,
  dateLabel,
  invoiceMath,
  invoiceStatus,
  num
} from '../../utils/format';
import { buildInvoicePDF } from '../../utils/pdfGenerator';
import { downloadBlob, printHTML, generateInvoiceHTML } from '../../utils/export';

export const InvoiceViewModal: React.FC = () => {
  const {
    state,
    invoiceViewOpen,
    viewingInvoice,
    closeInvoiceView,
    openInvoiceBuilder,
    addToast
  } = useApp();

  if (!invoiceViewOpen || !viewingInvoice) return null;

  const inv = viewingInvoice;
  const s = state.settings;
  const m = invoiceMath(inv);
  const st = invoiceStatus(inv);

  const handleDownload = () => {
    try {
      const blob = buildInvoicePDF(inv, s);
      const safeName = (`Ankara-${inv.number}-${inv.customerName}`)
        .replace(/[^a-z0-9\-_]+/gi, '-')
        .replace(/-+/g, '-');
      downloadBlob(blob, `${safeName}.pdf`);
      addToast(`Ankara ${inv.number} imepakuliwa kama PDF.`);
    } catch (e) {
      console.error(e);
      addToast('Imeshindwa kupakua PDF.', 'error');
    }
  };

  const handlePrint = () => {
    const html = generateInvoiceHTML(inv, s);
    printHTML(html, `Ankara ${inv.number} — ${inv.customerName}`);
  };

  const handleEdit = () => {
    closeInvoiceView();
    openInvoiceBuilder(inv);
  };

  return (
    <div className="premium-backdrop fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-[#081e16]/60 backdrop-blur-xs overflow-y-auto">
      <div className="premium-enter relative w-full max-w-4xl max-h-[94vh] flex flex-col bg-[#f3efe5] border border-white/20 rounded-[22px] shadow-[0_24px_70px_rgba(8,30,22,0.22)] overflow-hidden">
        {/* Modal Head */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#e2dccd] bg-[#fcfaf4]/75 shrink-0">
          <div>
            <div className="text-[9px] font-bold tracking-[0.18em] uppercase text-[#1b5540]">
              Hakiki ankara
            </div>
            <h2 className="font-display text-[20px] sm:text-[22px] font-semibold text-[#14231c] tracking-tight">
              Ankara {inv.number}
            </h2>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleEdit}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-[9px] bg-[#fcfaf4] border border-[#d2cab7] text-[#14231c] text-[11px] font-semibold hover:bg-white transition-colors"
            >
              <Edit2 className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Hariri</span>
            </button>

            <button
              onClick={handlePrint}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-[9px] bg-[#fcfaf4] border border-[#d2cab7] text-[#14231c] text-[11px] font-semibold hover:bg-white transition-colors"
            >
              <Printer className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Chapisha</span>
            </button>

            <button
              onClick={handleDownload}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-[9px] bg-[#0c2b22] text-white text-[11px] font-semibold hover:bg-[#123a2c] transition-colors shadow-xs"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Pakua PDF</span>
            </button>

            <button
              onClick={closeInvoiceView}
              className="w-8 h-8 rounded-[9px] border border-[#e2dccd] bg-[#fcfaf4] text-[#746e7d] hover:text-[#14231c] hover:bg-white grid place-items-center transition-colors ml-1"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Modal Sheet Body */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-8 bg-[#e9e4d8]/70">
          <div className="max-w-[780px] mx-auto bg-white text-[#0c2b22] p-6 sm:p-12 rounded-[4px] shadow-[0_12px_40px_rgba(8,30,22,0.12)] space-y-6 text-[12px]">
            {/* Sheet Top Banner */}
            <div className="flex flex-col sm:flex-row justify-between items-start gap-4 pb-6 border-b-2 border-[#123a2c]">
              <div className="flex items-center gap-3">
                <div className="w-[42px] h-[42px] rounded-[10px] bg-[#123a2c] text-[#d8f36b] grid place-items-center font-display text-[26px] font-bold shrink-0">
                  M
                </div>
                <div>
                  <div className="font-display text-[20px] font-bold text-[#123a2c] tracking-tight leading-tight">
                    {s.businessName}
                  </div>
                  <div className="text-[9px] uppercase tracking-wider text-[#6d7b72] font-bold mt-0.5">
                    {s.tagline}
                  </div>
                </div>
              </div>

              <div className="text-left sm:text-right">
                <div className="text-[9px] uppercase tracking-[0.2em] text-[#6d7b72] font-bold">
                  ANKARA
                </div>
                <div className="font-display text-[22px] font-bold text-[#123a2c] font-mono-numbers">
                  {inv.number}
                </div>
                <div className="mt-1">
                  <span
                    className={`inline-block px-2.5 py-0.5 rounded-full text-[9.5px] font-bold uppercase tracking-wider ${
                      st.key === 'paid'
                        ? 'bg-[#e7f1e7] text-[#4b6f8b]'
                        : st.key === 'partial'
                        ? 'bg-[#f9eed5] text-[#8f6213]'
                        : st.key === 'overdue'
                        ? 'bg-[#f8e4dc] text-[#a3462c]'
                        : 'bg-[#f3efe5] text-[#6d7b72]'
                    }`}
                  >
                    {st.label}
                  </span>
                </div>
              </div>
            </div>

            {/* Recipient and Meta Info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 py-2">
              <div>
                <div className="text-[9px] font-bold uppercase tracking-[0.15em] text-[#98a39b] mb-1.5">
                  Imetolewa kwa
                </div>
                <div className="text-[15px] font-bold text-[#14231c]">
                  {inv.customerName}
                </div>
                {inv.customerPhone && <div className="text-[11px] text-[#6d7b72] mt-0.5">{inv.customerPhone}</div>}
                {inv.customerEmail && <div className="text-[11px] text-[#6d7b72] mt-0.5">{inv.customerEmail}</div>}
                {inv.customerAddress && <div className="text-[11px] text-[#6d7b72] mt-0.5">{inv.customerAddress}</div>}
              </div>

              <div className="grid grid-cols-2 gap-3 text-[11px] sm:text-right">
                <div>
                  <div className="text-[9px] uppercase tracking-wider text-[#98a39b] font-bold">
                    Tarehe ya kutolewa
                  </div>
                  <div className="font-semibold text-[#0c2b22] mt-0.5">{dateLabel(inv.issueDate)}</div>
                </div>

                <div>
                  <div className="text-[9px] uppercase tracking-wider text-[#98a39b] font-bold">
                    Tarehe ya malipo
                  </div>
                  <div className="font-semibold text-[#0c2b22] mt-0.5">{dateLabel(inv.dueDate)}</div>
                </div>

                {inv.paidDate && (
                  <div>
                    <div className="text-[9px] uppercase tracking-wider text-[#98a39b] font-bold">
                      Tarehe iliyolipwa
                    </div>
                    <div className="font-semibold text-[#0c2b22] mt-0.5">{dateLabel(inv.paidDate)}</div>
                  </div>
                )}

                {s.tin && (
                  <div>
                    <div className="text-[9px] uppercase tracking-wider text-[#98a39b] font-bold">
                      TIN ya Biashara
                    </div>
                    <div className="font-semibold text-[#0c2b22] mt-0.5 font-mono">{s.tin}</div>
                  </div>
                )}
              </div>
            </div>

            {/* Items Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-[#eef9bd] text-[#1b5540] text-[9.5px] uppercase tracking-wider border-b border-[#ece7da]">
                    <th className="py-2.5 px-3 w-8">#</th>
                    <th className="py-2.5 px-3">Maelezo</th>
                    <th className="py-2.5 px-3 text-right">Idadi</th>
                    <th className="py-2.5 px-3 text-right">Bei (TZS)</th>
                    <th className="py-2.5 px-3 text-right">Jumla (TZS)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#eef9bd] text-[11.5px]">
                  {inv.items.map((item, idx) => {
                    const rowTotal = num(item.qty) * num(item.price);
                    return (
                      <tr key={idx}>
                        <td className="py-2.5 px-3 text-[#98a39b] font-mono text-[10px]">{idx + 1}</td>
                        <td className="py-2.5 px-3 font-semibold text-[#182820]">{item.description}</td>
                        <td className="py-2.5 px-3 text-right font-mono">{item.qty}</td>
                        <td className="py-2.5 px-3 text-right font-mono">{fmt(item.price)}</td>
                        <td className="py-2.5 px-3 text-right font-mono font-bold text-[#14231c]">
                          {fmt(rowTotal)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Bottom Breakdown & Notes */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-4 items-start">
              <div className="space-y-3 text-[11px] text-[#6d7b72]">
                {inv.notes && (
                  <div>
                    <strong className="block text-[#123a2c] text-[11px] mb-1">Maelezo:</strong>
                    <p className="whitespace-pre-line leading-relaxed">{inv.notes}</p>
                  </div>
                )}

                {s.paymentDetails && (
                  <div>
                    <strong className="block text-[#123a2c] text-[11px] mb-1">Maelezo ya Malipo:</strong>
                    <p className="whitespace-pre-line leading-relaxed">{s.paymentDetails}</p>
                  </div>
                )}

                <div className="font-display italic text-[#123a2c] text-[13px] pt-2">
                  Asante kwa kufanya biashara nasi.
                </div>
              </div>

              {/* Totals Table */}
              <div className="border border-[#ece7da] rounded-[8px] overflow-hidden text-[11px]">
                <div className="flex justify-between p-2 px-3 border-b border-[#eef9bd]">
                  <span className="text-[#6d7b72]">Jumla ndogo</span>
                  <span className="font-mono font-semibold">{money(m.subtotal)}</span>
                </div>

                <div className="flex justify-between p-2 px-3 border-b border-[#eef9bd] text-[#a3462c]">
                  <span>Punguzo</span>
                  <span className="font-mono font-semibold">− {money(m.discount)}</span>
                </div>

                <div className="flex justify-between p-2 px-3 border-b border-[#eef9bd]">
                  <span className="text-[#6d7b72]">VAT ({inv.vatRate}%)</span>
                  <span className="font-mono font-semibold">{money(m.vat)}</span>
                </div>

                <div className="flex justify-between p-2.5 px-3 bg-[#123a2c] text-white font-bold text-[12px]">
                  <span>JUMLA KUU</span>
                  <span className="font-mono text-[#d8f36b]">{money(m.total)}</span>
                </div>

                <div className="flex justify-between p-2 px-3 border-b border-[#eef9bd]">
                  <span className="text-[#6d7b72]">Iliyolipwa</span>
                  <span className="font-mono font-semibold">{money(m.paid)}</span>
                </div>

                <div className="flex justify-between p-2.5 px-3 font-bold text-[12px] bg-[#faf6f2]">
                  <span className="text-[#a3462c]">SALIO LINILODAIWA</span>
                  <span className="font-mono text-[#a3462c]">{money(m.balance)}</span>
                </div>
              </div>
            </div>

            {/* Sheet Footer */}
            <div className="pt-6 border-t border-[#ece7da] flex flex-col sm:flex-row justify-between gap-3 text-[10px] text-[#6d7b72]">
              <div>
                <strong>{s.businessName}</strong> · {s.address} · {s.phone}
              </div>
              <div className="sm:text-right">
                Ankara {inv.number} · Imetengenezwa na Mengidata
              </div>
            </div>
          </div>
        </div>

        {/* Modal Foot */}
        <div className="flex items-center justify-between px-6 py-3 border-t border-[#e2dccd] bg-[#fcfaf4]/75 shrink-0">
          <div className="text-[11px] text-[#746e7d]">
            Hali: <strong className="text-[#14231c]">{st.label}</strong>
          </div>
          <button
            onClick={closeInvoiceView}
            className="px-4 py-2 rounded-[10px] bg-[#fcfaf4] border border-[#d2cab7] text-[12px] font-semibold text-[#14231c] hover:bg-white"
          >
            Funga
          </button>
        </div>
      </div>
    </div>
  );
};
