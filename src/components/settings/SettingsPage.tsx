import React, { useState, useRef, useEffect } from 'react';
import {
  Download,
  Upload,
  FileSpreadsheet,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  Building,
  CheckCircle2,
  WifiOff,
  Smartphone,
  Monitor,
  Sun,
  Moon,
  RefreshCw,
  CircleCheck,
  CircleAlert,
  LoaderCircle,
  ShieldCheck,
  ExternalLink
} from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { relTime } from '../../utils/format';
import { exportBackupJSON, exportTransactionsCSV } from '../../utils/export';
import { getNativeBridge, isNativeApp, nativeAppVersion, UpdateStatusEvent } from '../../native/bridge';

type UpdateStatus = {
  state: UpdateStatusEvent['detail']['state'];
  version?: string;
  notes?: string;
  message?: string;
};

const RELEASES_URL = 'https://github.com/mengikimaro-debug/MengiData/releases/latest';
const UPDATE_STATES: UpdateStatus['state'][] = [
  'idle',
  'checking',
  'available',
  'up-to-date',
  'error',
  'downloading',
  'installing',
  'permission-required'
];

function isUpdateStatus(value: unknown): value is UpdateStatus {
  if (!value || typeof value !== 'object' || !('state' in value)) return false;
  return typeof value.state === 'string'
    && UPDATE_STATES.some(state => state === value.state)
    && (!('version' in value) || typeof value.version === 'string')
    && (!('notes' in value) || typeof value.notes === 'string')
    && (!('message' in value) || typeof value.message === 'string');
}

export const SettingsPage: React.FC = () => {
  const {
    state,
    t,
    theme,
    setTheme,
    updateSettings,
    resetToCleanData,
    loadDemoData,
    restoreFromJSON,
    showConfirm,
    addToast
  } = useApp();

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Business form state
  const [businessName, setBusinessName] = useState(state.settings.businessName);
  const [ownerName, setOwnerName] = useState(state.settings.ownerName);
  const [tagline, setTagline] = useState(state.settings.tagline);
  const [address, setAddress] = useState(state.settings.address);
  const [phone, setPhone] = useState(state.settings.phone);
  const [email, setEmail] = useState(state.settings.email);
  const [tin, setTin] = useState(state.settings.tin);
  const [website, setWebsite] = useState(state.settings.website);

  // Invoice form state
  const [invoicePrefix, setInvoicePrefix] = useState(state.settings.invoicePrefix);
  const [vatRate, setVatRate] = useState(state.settings.vatRate);
  const [paymentDetails, setPaymentDetails] = useState(state.settings.paymentDetails);
  const [invoiceNote, setInvoiceNote] = useState(state.settings.invoiceNote);
  const [autoUpdatesEnabled, setAutoUpdatesEnabled] = useState(() => {
    try {
      return getNativeBridge()?.getAutoUpdatesEnabled() ?? false;
    } catch (error) {
      console.warn('Failed to read automatic update preference', error);
      return false;
    }
  });
  const [updateStatus, setUpdateStatus] = useState<UpdateStatus>({ state: 'idle' });

  useEffect(() => {
    const bridge = getNativeBridge();
    if (!bridge) return;

    try {
      const savedStatus: unknown = JSON.parse(bridge.getUpdateStatus());
      if (isUpdateStatus(savedStatus)) setUpdateStatus(savedStatus);
    } catch (error) {
      console.warn('Failed to read update status', error);
    }

    const handleUpdateStatus = (event: UpdateStatusEvent) => setUpdateStatus(event.detail);
    window.addEventListener('mengi-update-status', handleUpdateStatus);
    return () => window.removeEventListener('mengi-update-status', handleUpdateStatus);
  }, []);

  const handleCheckForUpdates = () => {
    const bridge = getNativeBridge();
    if (!bridge) return;
    setUpdateStatus({ state: 'checking' });
    try {
      bridge.checkForUpdate();
    } catch (error) {
      console.error('Failed to start update check', error);
      setUpdateStatus({ state: 'error', message: t('Imeshindwa kuanzisha ukaguzi wa update.') });
    }
  };

  const handleToggleAutoUpdates = () => {
    const bridge = getNativeBridge();
    if (!bridge) return;
    const enabled = !autoUpdatesEnabled;
    setAutoUpdatesEnabled(enabled);
    try {
      bridge.setAutoUpdatesEnabled(enabled);
      if (enabled) handleCheckForUpdates();
    } catch (error) {
      console.error('Failed to save automatic update preference', error);
      setAutoUpdatesEnabled(!enabled);
      setUpdateStatus({ state: 'error', message: t('Imeshindwa kuhifadhi chaguo la update.') });
    }
  };

  const handleDownloadUpdate = () => {
    const bridge = getNativeBridge();
    if (!bridge) return;
    setUpdateStatus(current => ({ ...current, state: 'downloading' }));
    try {
      bridge.downloadAndInstallUpdate();
    } catch (error) {
      console.error('Failed to start app update', error);
      setUpdateStatus({ state: 'error', message: t('Imeshindwa kuanzisha upakuaji wa update.') });
    }
  };

  const handleSaveBusiness = (e: React.FormEvent) => {
    e.preventDefault();
    if (!businessName.trim()) {
      addToast(t('Jina la biashara linahitajika.'), 'error');
      return;
    }
    updateSettings({
      businessName: businessName.trim(),
      ownerName: ownerName.trim(),
      tagline: tagline.trim(),
      address: address.trim(),
      phone: phone.trim(),
      email: email.trim(),
      tin: tin.trim(),
      website: website.trim()
    });
  };

  const handleSaveInvoiceSettings = (e: React.FormEvent) => {
    e.preventDefault();
    updateSettings({
      invoicePrefix: invoicePrefix.trim().toUpperCase() || 'MD',
      vatRate: Number(vatRate) || 0,
      paymentDetails: paymentDetails.trim(),
      invoiceNote: invoiceNote.trim()
    });
  };

  const handleFileImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = event => {
      try {
        const text = event.target?.result as string;
        const parsed = JSON.parse(text);
        if (!parsed || !Array.isArray(parsed.transactions) || !Array.isArray(parsed.invoices)) {
          throw new Error('Invalid schema');
        }

        showConfirm({
          title: t('Rejesha data kutoka kwenye faili?'),
          message: t('Data zote zilizopo sasa zitabadilishwa na data iliyomo kwenye faili hili. Hakikisha umehifadhi nakala rudufu ya sasa kwanza.'),
          okLabel: t('Leta data'),
          danger: false,
          onConfirm: () => {
            restoreFromJSON(parsed);
          }
        });
      } catch (err) {
        console.error(err);
        addToast(t('Faili si sahihi. Tafadhali chagua faili sahihi la Mengidata (.json).'), 'error');
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const handleClearAll = () => {
    showConfirm({
      title: t('Futa data zote za kifedha?'),
      message: t('Miamala yote, ankara zote na bili zote zitaondolewa kabisa. Taarifa za biashara yako zitabaki. Kitendo hiki hakiwezi kutenduliwa.'),
      okLabel: t('Futa data zote'),
      danger: true,
      onConfirm: () => {
        resetToCleanData();
      }
    });
  };

  const handleLoadDemo = () => {
    showConfirm({
      title: t('Pakia data ya mfano?'),
      message: t('Mfumo utapakiwa miamala, ankara na bili za mfano ili kuona jinsi Mengidata inavyofanya kazi.'),
      okLabel: t('Pakia data ya mfano'),
      danger: false,
      onConfirm: () => {
        loadDemoData();
      }
    });
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Page Heading */}
      <div className="page-heading page-heading-stack">
        <div className="flex items-center gap-2 text-[9px] font-bold tracking-[0.19em] uppercase text-[#1b5540]">
          <span className="w-4 h-[1px] bg-[#1b5540]/50" />
          {t('Sanidi mfumo')}
        </div>
        <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
          {t('Mipangilio')}
        </h1>
        <p className="text-[12.5px] text-[#746e7d] mt-1 max-w-xl">
          {t('Sanidi taarifa za biashara yako, mfumo wa ankara na usimamizi wa data na nakala rudufu.')}
        </p>
      </div>

      <section className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between p-4 sm:px-5 rounded-[16px] bg-[#fcfaf4] border border-[#e2dccd]">
        <div>
          <h2 className="text-[13px] font-bold text-[#14231c]">{t('Muonekano wa mfumo')}</h2>
          <p className="text-[10.5px] text-[#746e7d]">{t('Chagua mandhari unayopendelea.')}</p>
        </div>
        <div className="grid w-full max-w-[390px] grid-cols-3 gap-2" role="group" aria-label={t('Chagua mandhari')}>
          {([
            { value: 'system', label: t('Mfumo'), detail: t('Fuata mandhari ya simu'), Icon: Monitor },
            { value: 'light', label: t('Mwanga'), detail: t('Muonekano angavu'), Icon: Sun },
            { value: 'dark', label: t('Giza Premium'), detail: t('Kijani cha kina na lime'), Icon: Moon }
          ] as const).map(({ value, label, detail, Icon }) => (
            <button
              key={value}
              type="button"
              onClick={() => setTheme(value)}
              aria-pressed={theme === value}
              className="theme-choice"
            >
              <span className="theme-choice-icon">
                <Icon className="h-4 w-4" aria-hidden="true" />
              </span>
              <span>
                <span className="block text-[11px] font-bold leading-tight">{label}</span>
                <span className="mt-1 block text-[9px] leading-tight opacity-75">{detail}</span>
              </span>
            </button>
          ))}
        </div>
      </section>

      <section className="premium-glow-card rounded-[20px] p-4 sm:p-5 space-y-4">
        <div className="flex items-start gap-3">
          <div className="theme-choice-icon shrink-0">
            <Smartphone className="h-4 w-4" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-[13px] font-bold text-[#14231c]">{t('Updates za programu')}</h2>
            <p className="text-[10.5px] text-[#746e7d]">
              {isNativeApp()
                ? `${t('Toleo lililosakinishwa')}: ${nativeAppVersion() || '—'}`
                : t('Tovuti husasishwa kiotomatiki unapofungua toleo jipya.')}
            </p>
          </div>
          <span className="premium-badge shrink-0">
            <ShieldCheck className="h-3 w-3" aria-hidden="true" />
            {t('Salama')}
          </span>
        </div>

        {isNativeApp() ? (
          <>
            <button
              type="button"
              role="switch"
              aria-checked={autoUpdatesEnabled}
              onClick={handleToggleAutoUpdates}
              className="flex w-full items-center justify-between gap-4 rounded-[13px] border border-[#e2dccd] bg-[#f8f4ea]/55 px-3.5 py-3 text-left"
            >
              <span>
                <span className="block text-[11px] font-bold text-[#14231c]">
                  {t('Angalia updates kiotomatiki')}
                </span>
                <span className="mt-0.5 block text-[9.5px] text-[#746e7d]">
                  {t('Huangalia hadi mara moja kila baada ya saa 12 unapofungua app.')}
                </span>
              </span>
              <span
                aria-hidden="true"
                className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${
                  autoUpdatesEnabled ? 'bg-[#1b5540]' : 'bg-[#c6c1b6]'
                }`}
              >
                <span
                  className={`absolute top-[3px] h-[18px] w-[18px] rounded-full bg-white shadow transition-transform ${
                    autoUpdatesEnabled ? 'translate-x-[22px]' : 'translate-x-[3px]'
                  }`}
                />
              </span>
            </button>

            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={handleCheckForUpdates}
                disabled={updateStatus.state === 'checking' || updateStatus.state === 'downloading' || updateStatus.state === 'installing'}
                className="inline-flex items-center gap-2 rounded-[10px] bg-[#0c2b22] px-3.5 py-2 text-[11px] font-bold text-white shadow-[0_7px_17px_rgba(12,43,34,0.16)] disabled:cursor-not-allowed disabled:opacity-60"
              >
                <RefreshCw className={`h-3.5 w-3.5 ${updateStatus.state === 'checking' ? 'animate-spin' : ''}`} aria-hidden="true" />
                {t('Angalia update sasa')}
              </button>
              {updateStatus.state === 'available' && (
                <button
                  type="button"
                  onClick={handleDownloadUpdate}
                  className="inline-flex items-center gap-2 rounded-[10px] border border-[#1b5540]/20 bg-[#eef9bd] px-3.5 py-2 text-[11px] font-bold text-[#274000]"
                >
                  <Download className="h-3.5 w-3.5" aria-hidden="true" />
                  {t('Pakua na sakinisha')}
                </button>
              )}
              {updateStatus.state === 'permission-required' && (
                <button
                  type="button"
                  onClick={() => {
                    try {
                      getNativeBridge()?.openUpdateInstallSettings();
                    } catch (error) {
                      console.error('Failed to open update install settings', error);
                      setUpdateStatus({ state: 'error', message: t('Imeshindwa kufungua ruhusa ya kusakinisha update.') });
                    }
                  }}
                  className="rounded-[10px] border border-[#d2cab7] bg-[#fcfaf4] px-3.5 py-2 text-[11px] font-bold text-[#14231c]"
                >
                  {t('Ruhusu kusakinisha update')}
                </button>
              )}
            </div>

            {updateStatus.state === 'available' && (
              <div className="flex items-start gap-2 rounded-[12px] border border-[#b8d95b]/40 bg-[#eef9bd]/55 p-3 text-[#274000]">
                <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                <div className="min-w-0">
                  <p className="text-[11px] font-bold">
                    {t('Toleo jipya linapatikana')}: {updateStatus.version}
                  </p>
                  {updateStatus.notes && (
                    <p className="mt-1 whitespace-pre-wrap text-[10px] leading-relaxed">{updateStatus.notes}</p>
                  )}
                  <p className="mt-1 text-[9.5px]">{t('Data yako itabaki baada ya kusasisha.')}</p>
                </div>
              </div>
            )}

            {(updateStatus.state === 'up-to-date' || updateStatus.state === 'downloading' || updateStatus.state === 'installing' || updateStatus.state === 'permission-required' || updateStatus.state === 'error') && (
              <div
                role="status"
                className={`flex items-start gap-2 rounded-[12px] border p-3 text-[10.5px] ${
                  updateStatus.state === 'error' || updateStatus.state === 'permission-required'
                    ? 'border-[#eccbbc] bg-[#fdf5f1] text-[#9e432b]'
                    : 'border-[#e2dccd] bg-[#f8f4ea]/65 text-[#34483e]'
                }`}
              >
                {updateStatus.state === 'up-to-date' ? (
                  <CircleCheck className="h-4 w-4 shrink-0" aria-hidden="true" />
                ) : updateStatus.state === 'downloading' || updateStatus.state === 'installing' ? (
                  <LoaderCircle className="h-4 w-4 shrink-0 animate-spin" aria-hidden="true" />
                ) : (
                  <CircleAlert className="h-4 w-4 shrink-0" aria-hidden="true" />
                )}
                <span>
                  {updateStatus.message ||
                    (updateStatus.state === 'downloading'
                      ? t('Inapakua update salama...')
                      : updateStatus.state === 'installing'
                        ? t('Pakua imekamilika. Thibitisha dirisha la Android.')
                        : '')}
                </span>
              </div>
            )}
          </>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-[13px] border border-[#e2dccd] bg-[#f8f4ea]/55 px-3.5 py-3">
            <p className="text-[10.5px] text-[#746e7d]">{t('Tumia toleo la hivi karibuni la tovuti.')}</p>
            <a
              href={RELEASES_URL}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 rounded-[10px] bg-[#0c2b22] px-3 py-2 text-[10.5px] font-bold text-white"
            >
              {t('Pakua app ya Android')}
              <ExternalLink className="h-3 w-3" aria-hidden="true" />
            </a>
          </div>
        )}
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left 2 Columns: Business Profile & Invoice Settings */}
        <div className="lg:col-span-2 space-y-6">
          {/* Business Profile Form */}
          <form
            onSubmit={handleSaveBusiness}
            className="p-5 sm:p-6 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] space-y-4"
          >
            <div>
              <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">{t('Taarifa za biashara')}</h3>
              <p className="text-[11px] text-[#746e7d]">{t('Hizi zinaonekana kwenye ankara zako na nyaraka za PDF.')}</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Jina la biashara *')}
                </label>
                <input
                  type="text"
                  value={businessName}
                  onChange={e => setBusinessName(e.target.value)}
                  placeholder={t('Mfano: Mengidata')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                  required
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Jina la mmiliki / Msimamizi')}
                </label>
                <input
                  type="text"
                  value={ownerName}
                  onChange={e => setOwnerName(e.target.value)}
                  placeholder={t('Mfano: Mengi')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Maelezo mafupi ya biashara (Tagline)')}
                </label>
                <input
                  type="text"
                  value={tagline}
                  onChange={e => setTagline(e.target.value)}
                  placeholder={t('Mfano: Ushauri, mauzo & huduma za kidijitali')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Anwani ya ofisi')}
                </label>
                <input
                  type="text"
                  value={address}
                  onChange={e => setAddress(e.target.value)}
                  placeholder={t('Mfano: Kimara Mwisho, Ubungo, Dar es Salaam')}
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Namba ya simu')}
                </label>
                <input
                  type="text"
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                  placeholder="+255 754 000 000"
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Barua pepe')}
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="hello@biashara.co.tz"
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('TIN / Namba ya Kodi ya Mlipaji')}
                </label>
                <input
                  type="text"
                  value={tin}
                  onChange={e => setTin(e.target.value)}
                  placeholder="123-456-789 (Hiari)"
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Tovuti au ukurasa wa mtandao')}
                </label>
                <input
                  type="text"
                  value={website}
                  onChange={e => setWebsite(e.target.value)}
                  placeholder="www.biashara.co.tz au @jina"
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>
            </div>

            <div className="pt-3 border-t border-[#e2dccd] flex justify-end">
              <button
                type="submit"
                className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold hover:bg-[#123a2c] transition-all shadow-[0_7px_17px_rgba(12,43,34,0.16)]"
              >
                {t('Hifadhi taarifa za biashara')}
              </button>
            </div>
          </form>

          {/* Invoice & Tax Defaults */}
          <form
            onSubmit={handleSaveInvoiceSettings}
            className="p-5 sm:p-6 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] space-y-4"
          >
            <div>
              <h3 className="text-[14px] font-bold text-[#14231c] tracking-tight">{t('Ankara & maelezo ya malipo')}</h3>
              <p className="text-[11px] text-[#746e7d]">{t('Mipangilio hii itatumika moja kwa moja unapotengeneza ankara mpya.')}</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Mwanzo wa namba ya ankara (Prefix)')}
                </label>
                <input
                  type="text"
                  value={invoicePrefix}
                  maxLength={6}
                  onChange={e => setInvoicePrefix(e.target.value)}
                  placeholder="MD"
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono text-[#14231c] font-bold focus:outline-none focus:border-[#1b5540]"
                />
                <div className="text-[9.5px] text-[#98a39b] mt-1">{t('Mfano:')} {invoicePrefix || 'MD'}-2026-001</div>
              </div>

              <div>
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('VAT ya kawaida (%)')}
                </label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="0.5"
                  value={vatRate}
                  onChange={e => setVatRate(Number(e.target.value))}
                  placeholder="0"
                  className="w-full h-[38px] px-3 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] font-mono text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
                <div className="text-[9.5px] text-[#98a39b] mt-1">{t('Weka 0 ikiwa haujasajiliwa kulipa VAT.')}</div>
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Maelezo ya malipo (M-Pesa, Benki, n.k.)')}
                </label>
                <textarea
                  rows={3}
                  value={paymentDetails}
                  onChange={e => setPaymentDetails(e.target.value)}
                  placeholder="Mfano: M-Pesa Lipa Namba: 5521400 (Mengi Data)&#10;Benki: CRDB Bank — A/C 0150 0000 0000"
                  className="w-full p-2.5 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[9.5px] uppercase tracking-wider text-[#746e7d] font-bold mb-1">
                  {t('Maelezo ya chini ya ankara')}
                </label>
                <textarea
                  rows={2}
                  value={invoiceNote}
                  onChange={e => setInvoiceNote(e.target.value)}
                  placeholder="Mfano: Asante kwa kufanya biashara nasi. Malipo yafanyike kabla ya tarehe ya mwisho."
                  className="w-full p-2.5 rounded-[10px] bg-[#f8f4ea] border border-[#d2cab7] text-[12px] text-[#14231c] focus:outline-none focus:border-[#1b5540]"
                />
              </div>
            </div>

            <div className="pt-3 border-t border-[#e2dccd] flex justify-end">
              <button
                type="submit"
                className="px-4 py-2 rounded-[10px] bg-[#0c2b22] text-white text-[12px] font-semibold hover:bg-[#123a2c] transition-all shadow-[0_7px_17px_rgba(12,43,34,0.16)]"
              >
                {t('Hifadhi mipangilio ya ankara')}
              </button>
            </div>
          </form>
        </div>

        {/* Right 1 Column: Data, Backups, and Danger Zone */}
        <div className="space-y-5">
          {/* Backups and Exports */}
          <div className="p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] space-y-4">
            <div>
              <h3 className="text-[13.5px] font-bold text-[#14231c] tracking-tight">{t('Data & nakala rudufu')}</h3>
              <p className="text-[10.5px] text-[#746e7d]">{t('Data yako inahifadhiwa kwenye kifaa hiki. Pakua nakala rudufu mara kwa mara.')}</p>
            </div>

            <div className="space-y-2.5">
              <div className="p-3 rounded-[12px] border border-[#e2dccd] bg-[#f8f4ea]/40 flex items-start gap-3">
                <Download className="w-4 h-4 text-[#1b5540] shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="text-[11.5px] font-bold text-[#14231c]">{t('Pakua nakala rudufu')}</div>
                  <p className="text-[10px] text-[#746e7d] mt-0.5 mb-2">{t('Faili la JSON lenye miamala, ankara, bili na mipangilio yote.')}</p>
                  <button
                    onClick={() => exportBackupJSON(state)}
                    className="px-2.5 py-1 rounded-[7px] bg-[#fcfaf4] border border-[#d2cab7] text-[10.5px] font-bold text-[#14231c] hover:bg-white"
                  >
                    {t('Pakua Backup (.json)')}
                  </button>
                </div>
              </div>

              <div className="p-3 rounded-[12px] border border-[#e2dccd] bg-[#f8f4ea]/40 flex items-start gap-3">
                <Upload className="w-4 h-4 text-[#1b5540] shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="text-[11.5px] font-bold text-[#14231c]">{t('Leta data kutoka faili')}</div>
                  <p className="text-[10px] text-[#746e7d] mt-0.5 mb-2">{t('Rejesha nakala ya awali iliyopakuliwa kutoka Mengidata.')}</p>
                  <input
                    type="file"
                    ref={fileInputRef}
                    accept=".json,application/json"
                    onChange={handleFileImport}
                    className="hidden"
                  />
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="px-2.5 py-1 rounded-[7px] bg-[#fcfaf4] border border-[#d2cab7] text-[10.5px] font-bold text-[#14231c] hover:bg-white"
                  >
                    {t('Chagua faili la JSON')}
                  </button>
                </div>
              </div>

              <div className="p-3 rounded-[12px] border border-[#e2dccd] bg-[#f8f4ea]/40 flex items-start gap-3">
                <FileSpreadsheet className="w-4 h-4 text-[#1b5540] shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="text-[11.5px] font-bold text-[#14231c]">{t('Pakua miamala (CSV)')}</div>
                  <p className="text-[10px] text-[#746e7d] mt-0.5 mb-2">{t('Fungua kwenye Microsoft Excel au Google Sheets kwa uchambuzi zaidi.')}</p>
                  <button
                    onClick={() => exportTransactionsCSV(state.transactions)}
                    className="px-2.5 py-1 rounded-[7px] bg-[#fcfaf4] border border-[#d2cab7] text-[10.5px] font-bold text-[#14231c] hover:bg-white"
                  >
                    {t('Pakua CSV ya Miamala')}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* System Status and Summary */}
          <div className="p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_1px_2px_rgba(20,35,28,.035)] space-y-3">
            <h3 className="text-[13.5px] font-bold text-[#14231c] tracking-tight">{t('Hali ya data')}</h3>

            <div className="p-3.5 rounded-[12px] bg-[#f8f4ea]/70 border border-[#e2dccd] flex items-center gap-3">
              <div className="w-9 h-9 rounded-[10px] bg-[#0c2b22] text-[#d8f36b] grid place-items-center font-display text-[20px] font-bold shrink-0">
                M
              </div>
              <div>
                <div className="text-[11.5px] font-bold text-[#14231c]">
                  {state.transactions.length} {t('miamala')} · {state.invoices.length} {t('ankara')} · {state.bills.length} {t('bili')}
                </div>
                <div className="text-[10px] text-[#746e7d] mt-0.5">
                  {t('Imesasishwa')} {relTime(state.meta.updatedAt)}
                </div>
              </div>
            </div>

            <button
              onClick={handleLoadDemo}
              className="w-full py-2 px-3 rounded-[9px] bg-[#fcfaf4] border border-[#d2cab7] text-[11px] font-semibold text-[#14231c] hover:bg-white inline-flex items-center justify-center gap-1.5"
            >
              <Sparkles className="w-3.5 h-3.5 text-[#c58a21]" />
              <span>{t('Pakia data ya mfano')}</span>
            </button>
          </div>

          {/* Offline / About */}
          <div className="p-5 rounded-[20px] bg-[#0c2b22] text-white shadow-[0_1px_2px_rgba(20,35,28,.035)] space-y-2.5">
            <div className="flex items-center gap-2 text-[#d8f36b]">
              <WifiOff className="w-4 h-4" />
              <h3 className="text-[13.5px] font-bold tracking-tight">{t('Inafanya kazi bila intaneti')}</h3>
            </div>
            <p className="text-[10.5px] text-[#d8d2df] leading-relaxed">
              {t('Programu hii na data yako zote ziko kwenye simu hii. Hakuna taarifa inayotumwa mtandaoni — unaweza kuandika miamala, kutoa ankara na kupakua PDF bila kuwa na bando.')}
            </p>
            <div className="pt-2.5 border-t border-white/10 flex items-center justify-between text-[9.5px] text-white/60">
              <span className="inline-flex items-center gap-1.5">
                <Smartphone className="w-3 h-3" />
                Mengidata {isNativeApp() ? '· Android' : '· Tovuti'}
              </span>
              <span>Toleo {nativeAppVersion() || '1.0.0'}</span>
            </div>
            <p className="text-[9.5px] text-[#a3baac] leading-relaxed">
              {t('Kwenye Android, chagua mahali pa kuhifadhi faili. Kwenye tovuti, faili hupakuliwa moja kwa moja.')}
            </p>
          </div>

          {/* Danger Zone */}
          <div className="p-5 rounded-[20px] bg-[#fdf5f1] border border-[#eccbbc] shadow-[0_1px_2px_rgba(20,35,28,.035)] space-y-3">
            <div className="flex items-center gap-2 text-[#9e432b]">
              <AlertTriangle className="w-4 h-4" />
              <h3 className="text-[13.5px] font-bold tracking-tight">{t('Eneo la hatari')}</h3>
            </div>
            <p className="text-[10.5px] text-[#746e7d] leading-relaxed">
              {t('Kitendo hiki kitafuta miamala, ankara na bili zote kwenye hifadhi ya ndani ya kifaa hiki. Hakikisha umepakua backup kwanza.')}
            </p>
            <button
              onClick={handleClearAll}
              className="w-full py-2 px-3 rounded-[9px] bg-[#bd5a3d] text-white text-[11.5px] font-bold hover:bg-[#9e432b] transition-colors"
            >
              {t('Futa data zote')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
