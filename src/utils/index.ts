import type { FormaPagamento } from '../types';

export function formatCurrency(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR');
}

export function formaPagamentoLabel(forma: FormaPagamento): string {
  const map: Record<FormaPagamento, string> = {
    DINHEIRO: 'Dinheiro',
    CARTAO_DEBITO: 'Cartão Débito',
    CARTAO_CREDITO: 'Cartão Crédito',
    PIX: 'PIX',
  };
  return map[forma] ?? forma;
}

export type EstoqueBadgeColor = 'red' | 'red-light' | 'yellow' | 'green';

export function getEstoqueBadge(qty: number): {
  label: string;
  color: EstoqueBadgeColor;
  rowClass: string;
} {
  if (qty === 0)
    return { label: 'Zerado', color: 'red', rowClass: 'bg-red-50 text-red-700' };
  if (qty <= 3)
    return { label: `Crítico (${qty})`, color: 'red-light', rowClass: 'bg-red-50/60 text-red-600' };
  if (qty <= 10)
    return { label: `Baixo (${qty})`, color: 'yellow', rowClass: 'bg-yellow-50 text-yellow-700' };
  return { label: `OK (${qty})`, color: 'green', rowClass: 'bg-green-50/40 text-green-700' };
}

export function calcularDesconto(forma: FormaPagamento, subtotal: number): number {
  if (forma === 'DINHEIRO' || forma === 'PIX') return subtotal * 0.05;
  return 0;
}

export function exportarCSV(data: object[], filename: string): void {
  if (!data.length) return;
  const headers = Object.keys(data[0]);
  const rows = data.map((row) =>
    headers.map((h) => {
      const val = (row as Record<string, unknown>)[h];
      return typeof val === 'string' && val.includes(',') ? `"${val}"` : String(val ?? '');
    }).join(',')
  );
  const csv = [headers.join(','), ...rows].join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
