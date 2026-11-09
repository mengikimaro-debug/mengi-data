import React, { useState, useEffect } from 'react';
import { PiggyBank, Plus, ArrowDownLeft, ArrowUpRight, History, Edit2, Trash2, Calendar, Eye, EyeOff, Lock, Fingerprint } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import { money, dateLabel } from '../../utils/format';
import { SavingsGoal } from '../../types';

const SAVER_UNLOCKED_KEY = 'mengidata_saver_unlocked';

export const SaverPage: React.FC = () => {
  const { state, t, openSaverModal, openSaverTxnModal, deleteSavingsGoal, addToast } = useApp();
  const [historyGoal, setHistoryGoal] = useState<SavingsGoal | null>(null);
  const [hideMoney, setHideMoney] = useState(false);
  const [isUnlocked, setIsUnlocked] = useState(() => {
    try {
      return localStorage.getItem(SAVER_UNLOCKED_KEY) === '1';
    } catch {
      return false;
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(SAVER_UNLOCKED_KEY, isUnlocked ? '1' : '0');
    } catch {
      // ignore
    }
  }, [isUnlocked]);

  const savings = state.savings || [];
  const logs = state.savingsLogs || [];

  const totalSaved = savings.reduce((s, g) => s + (g.currentAmount || 0), 0);
  const totalTarget = savings.reduce((s, g) => s + (g.targetAmount || 0), 0);
  const overallRatio = totalTarget > 0 ? Math.min(Math.max(totalSaved / totalTarget, 0), 1) : 0;
  const overallPercent = Math.round(overallRatio * 100);

  const formatAmount = (val: number) => {
    if (hideMoney) return 'TZS ••••••';
    return money(val);
  };

  const handleBiometricUnlock = () => {
    const bridge = (window as any).MengiNative;
    if (bridge && typeof bridge.requestBiometricUnlock === 'function') {
      (window as any).__biometricUnlocked = (success: boolean) => {
        if (success) {
          setIsUnlocked(true);
          addToast(t('Akiba imefunguliwa kwa mafanikio.'), 'success');
        } else {
          addToast(t('Imeshindwa kuthibitisha alama ya kidole au usalama wa simu.'), 'error');
        }
      };
      bridge.requestBiometricUnlock();
    } else {
      setIsUnlocked(true);
      addToast(t('Akiba imefunguliwa kwa mafanikio.'), 'success');
    }
  };

  if (!isUnlocked) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] p-6 text-center">
        <div className="w-20 h-20 rounded-full bg-[#1b5540]/10 text-[#1b5540] grid place-items-center mb-4 shadow-inner">
          <Fingerprint className="w-10 h-10 animate-pulse" />
        </div>
        <h2 className="font-display text-[24px] font-bold text-[#14231c]">
          {t('Sehemu ya Akiba Imefungwa')}
        </h2>
        <p className="text-[13px] text-[#746e7d] mt-1 max-w-sm">
          {t('Weka alama ya kidole au tumia usalama wa simu ili kufungua na kutazama akiba yako.')}
        </p>
        <button
          onClick={handleBiometricUnlock}
          className="mt-6 inline-flex items-center gap-2 px-6 py-3 rounded-[14px] bg-[#1b5540] text-[#d8f36b] text-[13.5px] font-semibold shadow-[0_8px_24px_rgba(27,85,64,0.3)] hover:bg-[#123d2e] transition-all"
        >
          <Lock className="w-4 h-4" />
          <span>{t('Fungua kwa Alama ya Kidole / PIN')}</span>
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Heading */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-[#1b5540]/10 text-[#1b5540] text-[11px] font-bold tracking-wide uppercase">
            <PiggyBank className="w-3.5 h-3.5" />
            <span>{t('Akiba na Malengo')}</span>
          </div>
          <h1 className="font-display text-[32px] sm:text-[38px] font-semibold text-[#14231c] tracking-tight mt-1 leading-tight">
            {t('Mfuko wa Akiba')}
          </h1>
          <p className="text-[12.5px] text-[#746e7d] mt-1">
            {t('Tenga na fuatilia malengo yako ya kifedha kwa urahisi na usalama.')}
          </p>
        </div>
        <div className="flex items-center gap-2.5 self-start sm:self-auto">
          <button
            onClick={() => setHideMoney(!hideMoney)}
            title={hideMoney ? t('Onyesha pesa') : t('Ficha pesa')}
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-[12px] bg-[#e2dccd] text-[#14231c] text-[12.5px] font-semibold hover:bg-[#d5d0c0] transition-all"
          >
            {hideMoney ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            <span>{hideMoney ? t('Ficha') : t('Onyesha')}</span>
          </button>
          <button
            onClick={() => openSaverModal(null)}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-[12px] bg-[#1b5540] text-[#d8f36b] text-[13px] font-semibold shadow-[0_6px_20px_rgba(27,85,64,0.25)] hover:bg-[#123d2e] transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>{t('+ Ongeza Lengo la Akiba')}</span>
          </button>
        </div>
      </div>

      {/* Summary Card */}
      <div className="p-6 rounded-[22px] bg-gradient-to-br from-[#123d2e] to-[#081e16] text-white shadow-[0_16px_40px_rgba(8,30,22,0.2)] border border-[#1b5540]/30 relative overflow-hidden">
        <div className="absolute right-0 top-0 w-64 h-64 bg-[#d8f36b]/10 rounded-full blur-3xl pointer-events-none" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10">
          <div>
            <div className="text-[10px] tracking-[0.2em] uppercase text-[#92aa9c] font-bold">
              {t('Jumla ya Akiba Iliyotunzwa')}
            </div>
            <div className="font-display text-[36px] sm:text-[42px] font-bold tracking-tight text-[#d8f36b] mt-1">
              {formatAmount(totalSaved)}
            </div>
            <div className="text-[12px] text-[#dbe7dc] mt-1">
              {t('Lengo la Jumla:')} <strong className="text-white">{formatAmount(totalTarget)}</strong>
            </div>
          </div>
          <div className="flex flex-col justify-center">
            <div className="flex justify-between text-[12px] font-bold mb-1.5 text-[#dbe7dc]">
              <span>{t('Maendeleo ya Jumla')}</span>
              <span className="text-[#d8f36b]">{overallPercent}%</span>
            </div>
            <div className="w-full h-3 rounded-full bg-white/10 overflow-hidden p-0.5">
              <div
                className="h-full rounded-full bg-gradient-to-r from-[#d8f36b] to-[#a3e635] transition-all duration-500"
                style={{ width: `${overallPercent}%` }}
              />
            </div>
            <div className="text-[10.5px] text-[#92aa9c] mt-2">
              {savings.length} {t('malengo ya akiba yamesajiliwa')}
            </div>
          </div>
        </div>
      </div>

      {/* Savings Goals Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {savings.length === 0 ? (
          <div className="col-span-full p-12 text-center rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd]">
            <PiggyBank className="w-12 h-12 text-[#92aa9c] mx-auto mb-3 opacity-60" />
            <h3 className="font-display text-[16px] font-bold text-[#14231c]">
              {t('Hakuna malengo ya akiba bado')}
            </h3>
            <p className="text-[12px] text-[#746e7d] mt-1 max-w-md mx-auto">
              {t('Bonyeza kitufe cha kuongeza lengo hapo juu ili kuanza kutunza akiba yako ya biashara.')}
            </p>
          </div>
        ) : (
          savings.map(goal => {
            const ratio = goal.targetAmount > 0 ? Math.min(Math.max(goal.currentAmount / goal.targetAmount, 0), 1) : 0;
            const pct = Math.round(ratio * 100);
            const remaining = Math.max(0, goal.targetAmount - goal.currentAmount);

            return (
              <div
                key={goal.id}
                className="flex flex-col justify-between p-5 rounded-[20px] bg-[#fcfaf4] border border-[#e2dccd] shadow-[0_8px_24px_rgba(8,30,22,0.05)] hover:border-[#1b5540]/40 transition-all"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-display text-[16px] font-bold text-[#14231c] leading-snug">
                      {goal.title}
                    </h3>
                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        onClick={() => openSaverModal(goal)}
                        title={t('Hariri')}
                        className="p-1.5 rounded-lg text-[#6d7b72] hover:bg-[#e2dccd]/50 hover:text-[#14231c] transition-colors"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => deleteSavingsGoal(goal.id)}
                        title={t('Futa')}
                        className="p-1.5 rounded-lg text-[#b94a39] hover:bg-[#f7e4db] transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {goal.targetDate && (
                    <div className="flex items-center gap-1.5 text-[11px] text-[#6d7b72] mt-1">
                      <Calendar className="w-3.5 h-3.5 text-[#1b5540]" />
                      <span>{t('Lengo:')} {goal.targetDate}</span>
                    </div>
                  )}

                  <div className="mt-4 space-y-1.5">
                    <div className="flex justify-between text-[12px]">
                      <span className="text-[#6d7b72]">{t('Akiba sasa:')}</span>
                      <strong className="text-[#1b5540]">{formatAmount(goal.currentAmount)}</strong>
                    </div>
                    <div className="flex justify-between text-[12px]">
                      <span className="text-[#6d7b72]">{t('Lengo:')}</span>
                      <strong className="text-[#14231c]">{formatAmount(goal.targetAmount)}</strong>
                    </div>
                    {remaining > 0 && (
                      <div className="flex justify-between text-[11px]">
                        <span className="text-[#746e7d]">{t('Imebaki:')}</span>
                        <span className="text-[#746e7d]">{formatAmount(remaining)}</span>
                      </div>
                    )}
                  </div>

                  <div className="mt-3.5">
                    <div className="flex justify-between text-[11px] font-bold mb-1 text-[#6d7b72]">
                      <span>{t('Maendeleo')}</span>
                      <span>{pct}%</span>
                    </div>
                    <div className="w-full h-2.5 rounded-full bg-[#e2dccd] overflow-hidden">
                      <div
                        className="h-full rounded-full bg-[#1b5540]"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>

                  {goal.notes && (
                    <p className="text-[11.5px] text-[#746e7d] mt-3 bg-[#f5f2e9] p-2.5 rounded-[12px] border border-[#e2dccd]/60">
                      {goal.notes}
                    </p>
                  )}
                </div>

                <div className="mt-5 pt-3 border-t border-[#e2dccd] flex items-center gap-2">
                  <button
                    onClick={() => openSaverTxnModal(goal, 'deposit')}
                    className="flex-1 inline-flex items-center justify-center gap-1.5 py-2 px-3 rounded-[12px] bg-[#1b5540] text-[#d8f36b] text-[12px] font-semibold hover:bg-[#123d2e] transition-colors"
                  >
                    <ArrowDownLeft className="w-3.5 h-3.5" />
                    <span>{t('+ Weka')}</span>
                  </button>
                  <button
                    onClick={() => openSaverTxnModal(goal, 'withdraw')}
                    className="flex-1 inline-flex items-center justify-center gap-1.5 py-2 px-3 rounded-[12px] bg-[#b94a39]/10 text-[#b94a39] text-[12px] font-semibold hover:bg-[#b94a39]/20 transition-colors"
                  >
                    <ArrowUpRight className="w-3.5 h-3.5" />
                    <span>{t('- Toa')}</span>
                  </button>
                  <button
                    onClick={() => setHistoryGoal(goal)}
                    title={t('Historia')}
                    className="p-2 rounded-[12px] bg-[#e2dccd]/60 text-[#14231c] hover:bg-[#e2dccd] transition-colors"
                  >
                    <History className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* History Modal */}
      {historyGoal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#081e16]/60 backdrop-blur-xs">
          <div className="w-full max-w-lg bg-[#fcfaf4] rounded-[24px] border border-[#e2dccd] shadow-[0_24px_60px_rgba(8,30,22,0.25)] overflow-hidden flex flex-col max-h-[85vh]">
            <div className="px-6 py-4 border-b border-[#e2dccd] flex items-center justify-between bg-[#f5f2e9]">
              <div>
                <h3 className="font-display text-[18px] font-bold text-[#14231c]">
                  {t('Historia ya Miamala')}
                </h3>
                <p className="text-[12px] text-[#746e7d]">{historyGoal.title}</p>
              </div>
              <button
                onClick={() => setHistoryGoal(null)}
                className="w-8 h-8 rounded-full bg-[#e2dccd] text-[#14231c] grid place-items-center hover:bg-[#d5d0c0] font-bold"
              >
                ✕
              </button>
            </div>
            <div className="p-6 overflow-y-auto space-y-3 flex-1">
              {logs.filter(l => l.savingsId === historyGoal.id).length === 0 ? (
                <div className="text-center py-10 text-[#746e7d] text-[13px]">
                  {t('Hakuna miamala iliyorekodiwa bado kwenye akiba hii.')}
                </div>
              ) : (
                logs
                  .filter(l => l.savingsId === historyGoal.id)
                  .map(log => {
                    const isDeposit = log.type === 'deposit';
                    return (
                      <div key={log.id} className="p-3.5 rounded-[16px] bg-white border border-[#e2dccd] flex items-center justify-between gap-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold ${isDeposit ? 'bg-[#1b5540]/10 text-[#1b5540]' : 'bg-[#b94a39]/10 text-[#b94a39]'}`}>
                              {isDeposit ? t('Kuweka') : t('Kutoa')}
                            </span>
                            <span className="text-[11.5px] text-[#746e7d]">{dateLabel(log.date)}</span>
                          </div>
                          {log.note && <p className="text-[12px] text-[#14231c] mt-1">{log.note}</p>}
                        </div>
                        <div className={`font-display text-[15px] font-bold ${isDeposit ? 'text-[#1b5540]' : 'text-[#b94a39]'}`}>
                          {isDeposit ? '+' : '-'}{formatAmount(log.amount)}
                        </div>
                      </div>
                    );
                  })
              )}
            </div>
            <div className="px-6 py-3 border-t border-[#e2dccd] bg-[#f5f2e9] flex justify-end">
              <button
                onClick={() => setHistoryGoal(null)}
                className="px-4 py-2 rounded-[12px] bg-[#14231c] text-white text-[12.5px] font-semibold"
              >
                {t('Funga')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
