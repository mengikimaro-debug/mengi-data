import { Invoice, BusinessSettings } from '../types';
import { invoiceMath, invoiceStatus, dateLabel, fmt, fmt2, num, pad } from './format';

let _pdfMeasureCtx: CanvasRenderingContext2D | null = null;

function pdfMeasure(text: string, size: number, font: string): number {
  if (!_pdfMeasureCtx) {
    const canvas = document.createElement('canvas');
    _pdfMeasureCtx = canvas.getContext('2d');
  }
  if (!_pdfMeasureCtx) return text.length * size * 0.55;
  _pdfMeasureCtx.font = (font === 'F2' ? '700 ' : font === 'F3' ? 'italic 400 ' : '400 ') + size + 'px Arial, Helvetica, sans-serif';
  return _pdfMeasureCtx.measureText(String(text)).width;
}

function pdfSafe(value: unknown): string {
  return String(value == null ? '' : value)
    .replace(/\r/g, '')
    .replace(/[\u2018\u2019]/g, "'")
    .replace(/[\u201C\u201D]/g, '"')
    .replace(/[\u2013\u2014\u2212]/g, '-')
    .replace(/\u2026/g, '...')
    .replace(/\u00A0/g, ' ')
    .replace(/[\u2022\u25CF]/g, '-')
    .replace(/[^\x20-\x7E\xA0-\xFF\n]/g, '?');
}

function escapePdfString(value: string): string {
  return String(value).replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)');
}

function fitPdfText(value: string, size: number, font: string, maxWidth: number): string {
  let text = pdfSafe(value);
  if (!maxWidth || pdfMeasure(text, size, font) <= maxWidth) return text;
  while (text.length > 1 && pdfMeasure(text + '...', size, font) > maxWidth) {
    text = text.slice(0, -1);
  }
  return text.replace(/\s+$/, '') + '...';
}

function wrapPdfText(value: string, size: number, font: string, maxWidth: number, maxLines: number): string[] {
  const words = pdfSafe(value).replace(/\n/g, ' ').split(/\s+/).filter(Boolean);
  const lines: string[] = [];
  let line = '';
  for (const word of words) {
    const candidate = line ? line + ' ' + word : word;
    if (pdfMeasure(candidate, size, font) <= maxWidth) {
      line = candidate;
    } else {
      if (line) lines.push(line);
      line = word;
      if (pdfMeasure(line, size, font) > maxWidth) {
        while (line.length > 1 && pdfMeasure(line + '...', size, font) > maxWidth) {
          line = line.slice(0, -1);
        }
        line = line + '...';
      }
    }
  }
  if (line) lines.push(line);
  if (!lines.length) lines.push('');
  if (maxLines && lines.length > maxLines) {
    const cut = lines.slice(0, maxLines);
    cut[maxLines - 1] = fitPdfText(cut[maxLines - 1] + '...', size, font, maxWidth);
    return cut;
  }
  return lines;
}

export class PDFBuilder {
  W: number;
  H: number;
  pages: string[][];
  page: string[];

  constructor() {
    this.W = 595.28; // A4 width
    this.H = 841.89; // A4 height
    this.pages = [];
    this.page = [];
    this.newPage();
  }

  newPage() {
    this.page = [];
    this.pages.push(this.page);
  }

  rgb(hex: string): string {
    const h = String(hex || '#000000').replace('#', '');
    const full = h.length === 3 ? h.split('').map(c => c + c).join('') : h.padEnd(6, '0').slice(0, 6);
    const r = parseInt(full.slice(0, 2), 16) / 255;
    const g = parseInt(full.slice(2, 4), 16) / 255;
    const b = parseInt(full.slice(4, 6), 16) / 255;
    return r.toFixed(3) + ' ' + g.toFixed(3) + ' ' + b.toFixed(3);
  }

  rect(x: number, y: number, w: number, h: number, opts: { fill?: string; stroke?: string; lw?: number; r?: number } = {}) {
    const Y = this.H - (y + h);
    const r = Math.min(opts.r || 0, w / 2, h / 2);
    let ops = 'q ';
    if (opts.fill) ops += this.rgb(opts.fill) + ' rg ';
    if (opts.stroke) ops += this.rgb(opts.stroke) + ' RG ' + (opts.lw || 1) + ' w ';
    if (r > 0) {
      const k = 0.5522847498 * r;
      ops += (x + r) + ' ' + Y + ' m ' + (x + w - r) + ' ' + Y + ' l ';
      ops += (x + w - r + k) + ' ' + Y + ' ' + (x + w) + ' ' + (Y + r - k) + ' ' + (x + w) + ' ' + (Y + r) + ' c ';
      ops += (x + w) + ' ' + (Y + h - r) + ' l ';
      ops += (x + w) + ' ' + (Y + h - r + k) + ' ' + (x + w - r + k) + ' ' + (Y + h) + ' ' + (x + w - r) + ' ' + (Y + h) + ' c ';
      ops += (x + r) + ' ' + (Y + h) + ' l ';
      ops += (x + r - k) + ' ' + (Y + h) + ' ' + x + ' ' + (Y + h - r + k) + ' ' + x + ' ' + (Y + h - r) + ' c ';
      ops += x + ' ' + (Y + r) + ' l ';
      ops += x + ' ' + (Y + r - k) + ' ' + (x + r - k) + ' ' + Y + ' ' + (x + r) + ' ' + Y + ' c h';
    } else {
      ops += x + ' ' + Y + ' ' + w + ' ' + h + ' re';
    }
    ops += opts.fill && opts.stroke ? ' B' : opts.fill ? ' f' : opts.stroke ? ' S' : ' n';
    ops += ' Q';
    this.page.push(ops);
  }

  line(x1: number, y1: number, x2: number, y2: number, opts: { color?: string; lw?: number; dash?: string } = {}) {
    const dash = opts.dash ? '[' + opts.dash + '] 0 d ' : '';
    this.page.push('q ' + this.rgb(opts.color || '#ece7da') + ' RG ' + (opts.lw || 1) + ' w ' + dash + x1 + ' ' + (this.H - y1) + ' m ' + x2 + ' ' + (this.H - y2) + ' l S Q');
  }

  text(value: string, x: number, y: number, opts: { size?: number; font?: string; color?: string; align?: 'left' | 'right' | 'center'; letter?: number; maxWidth?: number } = {}) {
    const size = opts.size || 9;
    const font = opts.font || 'F1';
    const color = opts.color || '#000000';
    const align = opts.align || 'left';
    const letter = opts.letter || 0;
    const maxWidth = opts.maxWidth || 0;
    const lines = String(value == null ? '' : value).split('\n');
    lines.forEach((raw, idx) => {
      let line = pdfSafe(raw);
      if (maxWidth) line = fitPdfText(line, size, font, maxWidth - Math.max(0, letter * Math.max(0, line.length - 1)));
      if (!line) return;
      const width = pdfMeasure(line, size, font) + Math.max(0, line.length - 1) * letter;
      let tx = x;
      if (align === 'right') tx = x - width;
      else if (align === 'center') tx = x - width / 2;
      const yy = y + idx * size * 1.36;
      const base = this.H - (yy + size * 0.82);
      this.page.push('BT /' + font + ' ' + size + ' Tf ' + letter + ' Tc ' + this.rgb(color) + ' rg 1 0 0 1 ' + tx.toFixed(2) + ' ' + base.toFixed(2) + ' Tm (' + escapePdfString(line) + ') Tj ET');
    });
  }

  paragraph(value: string, x: number, y: number, opts: { size?: number; font?: string; color?: string; maxWidth?: number; maxLines?: number } = {}): number {
    const size = opts.size || 8;
    const font = opts.font || 'F1';
    const maxWidth = opts.maxWidth || 250;
    const maxLines = opts.maxLines || 99;
    const lines = wrapPdfText(value, size, font, maxWidth, maxLines);
    this.text(lines.join('\n'), x, y, { ...opts, size, font, maxWidth: 0 });
    return lines.length * size * 1.36;
  }

  toPDF(title: string, author: string): string {
    const objects: string[] = [];
    const add = (body: string) => { objects.push(body); return objects.length; };
    const catalogId = add('');
    const pagesId = add('');
    const f1 = add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>');
    const f2 = add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>');
    const f3 = add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique /Encoding /WinAnsiEncoding >>');
    const pageIds: number[] = [];
    this.pages.forEach(ops => {
      const pid = add('');
      const content = ops.join('\n');
      const cid = add('<< /Length ' + content.length + ' >>\nstream\n' + content + '\nendstream');
      objects[pid - 1] = '<< /Type /Page /Parent ' + pagesId + ' 0 R /MediaBox [0 0 ' + this.W.toFixed(2) + ' ' + this.H.toFixed(2) + '] /Resources << /Font << /F1 ' + f1 + ' 0 R /F2 ' + f2 + ' 0 R /F3 ' + f3 + ' 0 R >> >> /Contents ' + cid + ' 0 R >>';
      pageIds.push(pid);
    });
    objects[pagesId - 1] = '<< /Type /Pages /Kids [' + pageIds.map(id => id + ' 0 R').join(' ') + '] /Count ' + pageIds.length + ' >>';
    objects[catalogId - 1] = '<< /Type /Catalog /Pages ' + pagesId + ' 0 R >>';
    const date = new Date();
    const pdfDate = 'D:' + date.getFullYear() + pad(date.getMonth() + 1) + pad(date.getDate()) + pad(date.getHours()) + pad(date.getMinutes()) + pad(date.getSeconds());
    const infoId = add('<< /Title (' + escapePdfString(pdfSafe(title || 'Mengidata')) + ') /Author (' + escapePdfString(pdfSafe(author || 'Mengidata')) + ') /Creator (Mengidata) /Producer (Mengidata) /CreationDate (' + pdfDate + ') >>');

    let pdf = '%PDF-1.4\n%\xE2\xE3\xCF\xD3\n';
    const offsets = new Array(objects.length + 1).fill(0);
    objects.forEach((body, i) => {
      offsets[i + 1] = pdf.length;
      pdf += (i + 1) + ' 0 obj\n' + body + '\nendobj\n';
    });
    const xref = pdf.length;
    pdf += 'xref\n0 ' + (objects.length + 1) + '\n0000000000 65535 f \n';
    for (let i = 1; i <= objects.length; i++) {
      pdf += String(offsets[i]).padStart(10, '0') + ' 00000 n \n';
    }
    pdf += 'trailer\n<< /Size ' + (objects.length + 1) + ' /Root ' + catalogId + ' 0 R /Info ' + infoId + ' 0 R >>\nstartxref\n' + xref + '\n%%EOF';
    return pdf;
  }
}

function pdfBlobFromDoc(doc: PDFBuilder, title: string, author: string): Blob {
  const pdf = doc.toPDF(title, author);
  const bytes = new Uint8Array(pdf.length);
  for (let i = 0; i < pdf.length; i++) {
    bytes[i] = pdf.charCodeAt(i) & 0xff;
  }
  return new Blob([bytes], { type: 'application/pdf' });
}

function statusPdfColors(key: string) {
  if (key === 'paid') return { bg: '#dbe7dc', fg: '#4b6f8b' };
  if (key === 'partial') return { bg: '#f8edd3', fg: '#946510' };
  if (key === 'overdue') return { bg: '#f7e4db', fg: '#a3462c' };
  if (key === 'draft') return { bg: '#efede5', fg: '#6f6a5e' };
  return { bg: '#f0eee7', fg: '#6f6a5e' };
}

export function buildInvoicePDF(inv: Invoice, settings: BusinessSettings): Blob {
  const s = settings;
  const m = invoiceMath(inv);
  const st = invoiceStatus(inv);
  const doc = new PDFBuilder();
  const M = 40;
  const R = doc.W - M;

  // Header Banner
  doc.rect(0, 0, doc.W, 132, { fill: '#0c2b22' });
  doc.rect(M, 28, 38, 38, { fill: '#d8f36b', r: 9 });
  doc.text('M', M + 9.5, 34, { size: 24, font: 'F2', color: '#0c2b22' });
  doc.text(fitPdfText(s.businessName || 'Mengi Data', 15, 'F2', 250), M + 50, 30, { size: 15, font: 'F2', color: '#ffffff' });
  doc.text(fitPdfText(s.tagline || '', 6.5, 'F1', 250), M + 50, 51, { size: 6.5, color: '#b9d0c1', letter: 0.7 });
  doc.text(fitPdfText(s.address || '', 7, 'F1', 255), M + 50, 70, { size: 7, color: '#d8d2df' });
  const contact = [s.phone, s.email].filter(Boolean).join('  ·  ');
  doc.text(fitPdfText(contact, 7, 'F1', 255), M + 50, 82, { size: 7, color: '#d8d2df' });
  if (s.tin) doc.text('TIN: ' + s.tin, M + 50, 94, { size: 6.3, color: '#a3baac' });

  doc.text('ANKARA', R, 26, { size: 6.8, font: 'F2', color: '#d8f36b', align: 'right', letter: 1.8 });
  doc.text(fitPdfText(inv.number, 15, 'F2', 180), R, 37, { size: 15, font: 'F2', color: '#ffffff', align: 'right' });
  const pill = st.label.toUpperCase();
  const pw = pdfMeasure(pill, 6.2, 'F2') + 16;
  const pc = statusPdfColors(st.key);
  doc.rect(R - pw, 64, pw, 16, { fill: pc.bg, r: 8 });
  doc.text(pill, R - pw / 2, 68.2, { size: 6.2, font: 'F2', color: pc.fg, align: 'center' });
  doc.text('Tarehe: ' + dateLabel(inv.issueDate), R, 92, { size: 7, color: '#d8d2df', align: 'right' });
  doc.text('Malipo: ' + dateLabel(inv.dueDate), R, 104, { size: 7, color: '#d8d2df', align: 'right' });

  // Client Details & Invoice Meta
  let y = 154;
  doc.text('IMETOLEWA KWA', M, y, { size: 6.3, font: 'F2', color: '#2a6d53', letter: 0.8 });
  doc.text(fitPdfText(inv.customerName, 11, 'F2', 280), M, y + 10, { size: 11, font: 'F2', color: '#14231c' });
  let cy = y + 25;
  const contactLines = [inv.customerPhone, inv.customerEmail, inv.customerAddress].filter(Boolean);
  contactLines.forEach(line => {
    doc.text(fitPdfText(line, 7.5, 'F1', 280), M, cy, { size: 7.5, color: '#6d7b72' });
    cy += 11;
  });

  const infoX = 468;
  doc.text('TAARIFA ZA ANKARA', infoX, y, { size: 6.3, font: 'F2', color: '#2a6d53', letter: 0.8, align: 'right' });
  doc.text('Namba', infoX, y + 13, { size: 7, color: '#6d7b72', align: 'right' });
  doc.text(fitPdfText(inv.number, 7.5, 'F2', 120), R, y + 13, { size: 7.5, font: 'F2', color: '#123a2c', align: 'right' });
  doc.text('Tarehe', infoX, y + 25, { size: 7, color: '#6d7b72', align: 'right' });
  doc.text(dateLabel(inv.issueDate), R, y + 25, { size: 7.5, color: '#123a2c', align: 'right' });
  doc.text('Malipo', infoX, y + 37, { size: 7, color: '#6d7b72', align: 'right' });
  doc.text(dateLabel(inv.dueDate), R, y + 37, { size: 7.5, color: '#123a2c', align: 'right' });
  doc.text('Hali', infoX, y + 49, { size: 7, color: '#6d7b72', align: 'right' });
  doc.text(st.label, R, y + 49, { size: 7.5, font: 'F2', color: st.key === 'overdue' ? '#a3462c' : st.key === 'paid' ? '#4b6f8b' : '#946510', align: 'right' });

  // Items Table
  let tableY = Math.max(cy + 12, y + 74);
  tableY = Math.max(tableY, 238);
  doc.rect(M, tableY, 515, 24, { fill: '#123a2c' });
  doc.text('#', M + 8, tableY + 8, { size: 6.6, font: 'F2', color: '#ffffff' });
  doc.text('MAELEZO', M + 32, tableY + 8, { size: 6.6, font: 'F2', color: '#ffffff', letter: 0.4 });
  doc.text('IDADI', M + 364, tableY + 8, { size: 6.6, font: 'F2', color: '#ffffff', align: 'right' });
  doc.text('BEI (TZS)', M + 438, tableY + 8, { size: 6.6, font: 'F2', color: '#ffffff', align: 'right' });
  doc.text('JUMLA', M + 507, tableY + 8, { size: 6.6, font: 'F2', color: '#ffffff', align: 'right' });
  let ty = tableY + 24;

  const items = inv.items.length ? inv.items : [{ description: '—', qty: 1, price: 0 }];
  items.forEach((item, idx) => {
    const descLines = wrapPdfText(item.description || '—', 8.1, 'F1', 292, 2);
    const rowH = Math.max(27, 10 + descLines.length * 10.2);
    if (ty + rowH > 718) {
      doc.newPage();
      // Continuation top banner
      doc.rect(0, 0, doc.W, 58, { fill: '#0c2b22' });
      doc.rect(M, 16, 26, 26, { fill: '#d8f36b', r: 6 });
      doc.text('M', M + 6.5, 21, { size: 16, font: 'F2', color: '#0c2b22' });
      doc.text(s.businessName || 'Mengi Data', M + 36, 20, { size: 10, font: 'F2', color: '#ffffff' });
      doc.text('Ankara ' + inv.number, R, 23, { size: 8, color: '#d8d2df', align: 'right' });

      ty = 86;
      doc.rect(M, ty, 515, 24, { fill: '#123a2c' });
      doc.text('#', M + 8, ty + 8, { size: 6.6, font: 'F2', color: '#ffffff' });
      doc.text('MAELEZO', M + 32, ty + 8, { size: 6.6, font: 'F2', color: '#ffffff', letter: 0.4 });
      doc.text('IDADI', M + 364, ty + 8, { size: 6.6, font: 'F2', color: '#ffffff', align: 'right' });
      doc.text('BEI (TZS)', M + 438, ty + 8, { size: 6.6, font: 'F2', color: '#ffffff', align: 'right' });
      doc.text('JUMLA', M + 507, ty + 8, { size: 6.6, font: 'F2', color: '#ffffff', align: 'right' });
      ty += 24;
    }
    if (idx % 2 === 0) doc.rect(M, ty, 515, rowH, { fill: '#f8f4ea' });
    doc.text(String(idx + 1), M + 8, ty + 8, { size: 7.5, color: '#98a39b' });
    doc.text(descLines.join('\n'), M + 32, ty + 8, { size: 8.1, color: '#123a2c' });
    const qty = Number.isInteger(num(item.qty)) ? fmt(item.qty) : fmt2(item.qty);
    doc.text(qty, M + 364, ty + 8, { size: 8, color: '#6d7b72', align: 'right' });
    doc.text(fmt(item.price), M + 438, ty + 8, { size: 8, color: '#6d7b72', align: 'right' });
    doc.text(fmt(num(item.qty) * num(item.price)), M + 507, ty + 8, { size: 8, font: 'F2', color: '#123a2c', align: 'right' });
    doc.line(M, ty + rowH, M + 515, ty + rowH, { color: '#e6ece2', lw: 0.6 });
    ty += rowH;
  });

  // Totals Box
  let totalsY = ty + 16;
  if (totalsY + 132 > 735) {
    doc.newPage();
    totalsY = 96;
  }
  const boxX = 350;
  const boxW = 205;
  doc.rect(boxX, totalsY, boxW, 130, { fill: '#f8f4ea', stroke: '#ece7da', lw: 0.7 });

  const row = (label: string, value: string, dy: number, opts: { size?: number; color?: string; bold?: boolean } = {}) => {
    doc.text(label, boxX + 10, totalsY + dy, { size: opts.size || 7.4, color: opts.color || '#6d7b72', font: opts.bold ? 'F2' : 'F1' });
    doc.text(value, boxX + boxW - 10, totalsY + dy, { size: opts.size || 7.6, color: opts.color || '#123a2c', font: opts.bold ? 'F2' : 'F1', align: 'right' });
  };

  row('Jumla ndogo', 'TZS ' + fmt(m.subtotal), 8);
  row('Punguzo', '− TZS ' + fmt(m.discount), 24);
  row('VAT (' + fmt2(inv.vatRate) + '%)', 'TZS ' + fmt(m.vat), 40);
  doc.rect(boxX, totalsY + 54, boxW, 26, { fill: '#123a2c' });
  doc.text('JUMLA KUU', boxX + 10, totalsY + 62, { size: 7.2, font: 'F2', color: '#ffffff', letter: 0.4 });
  doc.text('TZS ' + fmt(m.total), boxX + boxW - 10, totalsY + 61, { size: 9.2, font: 'F2', color: '#d8f36b', align: 'right' });
  row('Iliyolipwa', 'TZS ' + fmt(m.paid), 90);
  row('SALIO', 'TZS ' + fmt(m.balance), 108, { bold: true, size: 8, color: '#a3462c' });
  doc.line(boxX + 10, totalsY + 84, boxX + boxW - 10, totalsY + 84, { color: '#e2e9dd', lw: 0.6 });

  // Notes & Payment instructions
  const note = inv.notes || s.invoiceNote || '';
  let notesY = totalsY;
  doc.text('MAELEZO', M, notesY, { size: 6.3, font: 'F2', color: '#2a6d53', letter: 0.8 });
  notesY += 10;
  if (note) {
    notesY += doc.paragraph(note, M, notesY, { size: 7.2, color: '#6d7b72', maxWidth: 270, maxLines: 5 }) + 6;
  }
  if (s.paymentDetails) {
    doc.text('MALIPO', M, notesY, { size: 6.3, font: 'F2', color: '#2a6d53', letter: 0.8 });
    notesY += 10;
    notesY += doc.paragraph(s.paymentDetails, M, notesY, { size: 7.2, color: '#6d7b72', maxWidth: 270, maxLines: 5 });
  }
  if (s.tin) doc.text('TIN: ' + s.tin, M, Math.max(notesY + 8, totalsY + 118), { size: 6.5, color: '#98a39b' });
  doc.text('Asante kwa kufanya biashara nasi.', M, Math.max(notesY + 20, totalsY + 130), { size: 8, font: 'F3', color: '#2a6d53' });

  // Footers on all pages
  const totalPages = doc.pages.length;
  for (let i = 0; i < totalPages; i++) {
    doc.page = doc.pages[i];
    doc.line(M, 792, R, 792, { color: '#ece7da', lw: 0.7 });
    doc.text(fitPdfText(s.businessName + '  ·  ' + [s.phone, s.email].filter(Boolean).join('  ·  '), 6.2, 'F1', 360), M, 799, { size: 6.2, color: '#6d7b72' });
    doc.text('Ukurasa ' + (i + 1) + ' / ' + totalPages, R, 799, { size: 6.2, color: '#6d7b72', align: 'right' });
    doc.text('Imetengenezwa na Mengidata', R, 808, { size: 5.4, color: '#a1aba4', align: 'right' });
  }

  return pdfBlobFromDoc(doc, 'Ankara ' + inv.number + ' — ' + inv.customerName, s.businessName);
}
