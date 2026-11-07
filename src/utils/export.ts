import { Transaction, Invoice, BusinessSettings } from '../types';
import { todayISO, dateLabel, dateLong, fmt, num, sortTxNewest, invoiceStatus, invoiceMath } from './format';
import { saveFile, printDocument } from '../native/bridge';

export function csvEscape(value: unknown): string {
  const s = String(value == null ? '' : value);
  return '"' + s.replace(/"/g, '""') + '"';
}

/**
 * Saves a generated file. Inside the Android app this writes to
 * Download/Mengidata; in a browser it downloads normally.
 */
export function downloadBlob(blob: Blob, filename: string): void {
  saveFile(blob, filename).catch(err => {
    console.error('Could not save file', err);
    alert('Imeshindwa kuhifadhi faili. Tafadhali jaribu tena.');
  });
}

export function exportTransactionsCSV(transactions: Transaction[]): void {
  const header = [
    'Tarehe',
    'Aina',
    'Maelezo',
    'Mteja/Msambazaji',
    'Kundi',
    'Njia ya Malipo',
    'Kiasi (TZS)',
    'Kumbukumbu',
    'Chanzo'
  ];
  const rows = transactions
    .slice()
    .sort(sortTxNewest)
    .map(t => [
      t.date,
      t.type === 'income' ? 'Fedha Ndani' : 'Fedha Nje',
      t.description,
      t.party,
      t.category,
      t.method,
      t.amount,
      t.reference,
      t.source
    ]);
  const csv = '\uFEFF' + [header, ...rows].map(r => r.map(csvEscape).join(',')).join('\r\n');
  downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), 'mengidata-miamala-' + todayISO() + '.csv');
}

export function exportBackupJSON(state: unknown): void {
  const data = JSON.stringify(state, null, 2);
  downloadBlob(new Blob([data], { type: 'application/json' }), 'mengidata-backup-' + todayISO() + '.json');
}

export function printHTML(html: string, title: string): void {
  printDocument(html, title);
}
export function generateInvoiceHTML(inv: Invoice, settings: BusinessSettings): string {
  const m = invoiceMath(inv);
  const st = invoiceStatus(inv);
  const rows = inv.items.map((item, idx) => `
    <tr>
      <td style="width:28px;color:#98a39b">${idx + 1}</td>
      <td><strong>${item.description}</strong></td>
      <td class="r">${item.qty}</td>
      <td class="r">${fmt(item.price)}</td>
      <td class="r">${fmt(num(item.qty) * num(item.price))}</td>
    </tr>
  `).join('');

  return `
    <div class="sheet-top">
      <div class="sheet-brand">
        <div class="sheet-mark">M</div>
        <div>
          <div class="sheet-biz-name">${settings.businessName}</div>
          <div style="font-size:11px;color:#6d7b72">${settings.tagline}</div>
        </div>
      </div>
      <div style="text-align:right">
        <div style="font-size:10px;letter-spacing:1px;text-transform:uppercase;color:#6d7b72;font-weight:700">ANKARA</div>
        <div style="font-size:22px;font-weight:700;color:#123a2c">${inv.number}</div>
        <div style="display:inline-block;padding:2px 8px;border-radius:99px;background:#f3efe5;color:#123a2c;font-weight:700;font-size:10px;margin-top:4px">${st.label}</div>
      </div>
    </div>

    <div class="sheet-meta">
      <div>
        <div style="font-size:10px;letter-spacing:1px;text-transform:uppercase;color:#98a39b;font-weight:700;margin-bottom:4px">IMETOLEWA KWA</div>
        <div style="font-size:15px;font-weight:700;color:#14231c">${inv.customerName}</div>
        ${inv.customerPhone ? `<div>${inv.customerPhone}</div>` : ''}
        ${inv.customerEmail ? `<div>${inv.customerEmail}</div>` : ''}
        ${inv.customerAddress ? `<div>${inv.customerAddress}</div>` : ''}
      </div>
      <div style="text-align:right">
        <div><strong>Tarehe ya kutolewa:</strong> ${dateLabel(inv.issueDate)}</div>
        <div><strong>Tarehe ya malipo:</strong> ${dateLabel(inv.dueDate)}</div>
        ${inv.paidDate ? `<div><strong>Tarehe iliyolipwa:</strong> ${dateLabel(inv.paidDate)}</div>` : ''}
        ${settings.tin ? `<div><strong>TIN:</strong> ${settings.tin}</div>` : ''}
      </div>
    </div>

    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Maelezo</th>
          <th class="r">Idadi</th>
          <th class="r">Bei (TZS)</th>
          <th class="r">Jumla (TZS)</th>
        </tr>
      </thead>
      <tbody>
        ${rows}
      </tbody>
    </table>

    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-top:20px">
      <div style="max-width:320px;font-size:11px;color:#6d7b72">
        ${inv.notes ? `<p><strong>Maelezo:</strong><br>${inv.notes.replace(/\n/g, '<br>')}</p>` : ''}
        ${settings.paymentDetails ? `<p><strong>Maelezo ya Malipo:</strong><br>${settings.paymentDetails.replace(/\n/g, '<br>')}</p>` : ''}
        <p style="font-style:italic;color:#1b5540;margin-top:12px">Asante kwa kufanya biashara nasi.</p>
      </div>

      <div class="totals-box">
        <div class="total-row"><span>Jumla ndogo</span><span>TZS ${fmt(m.subtotal)}</span></div>
        <div class="total-row"><span>Punguzo</span><span>− TZS ${fmt(m.discount)}</span></div>
        <div class="total-row"><span>VAT (${inv.vatRate}%)</span><span>TZS ${fmt(m.vat)}</span></div>
        <div class="total-row grand"><span>Jumla kuu</span><span>TZS ${fmt(m.total)}</span></div>
        <div class="total-row"><span>Iliyolipwa</span><span>TZS ${fmt(m.paid)}</span></div>
        <div class="total-row" style="color:#a3462c;font-weight:700"><span>Salio</span><span>TZS ${fmt(m.balance)}</span></div>
      </div>
    </div>

    <div class="footer">
      <div>${settings.businessName} · ${settings.phone} · ${settings.email}</div>
      <div>Imetengenezwa na Mengidata</div>
    </div>
  `;
}
