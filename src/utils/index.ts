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

// ── CSV Export ────────────────────────────────────────────────────────────────

export function exportarCSV(data: object[], filename: string): void {
  if (!data.length) return;
  const headers = Object.keys(data[0]);
  const rows = data.map((row) =>
    headers.map((h) => {
      const val = (row as Record<string, unknown>)[h];
      return typeof val === 'string' && val.includes(',') ? `"${val}"` : String(val ?? '');
    }).join(';')
  );
  const csv = [headers.join(';'), ...rows].join('\n');
  downloadBlob('\uFEFF' + csv, filename, 'text/csv;charset=utf-8;');
}

export function exportarEstoqueCSV(produtos: { id: number; nome: string; preco: number; quantidadeEstoque: number; categoria: string }[]): void {
  const rows = produtos.map((p) => `${p.nome};${p.preco.toFixed(2)};${p.quantidadeEstoque};${p.categoria}`);
  const csv = 'nome;preco;estoque;categoria\n' + rows.join('\n');
  downloadBlob('\uFEFF' + csv, 'estoque-petstop.csv', 'text/csv;charset=utf-8;');
}

export function gerarModeloCSV(): void {
  const modelo = [
    'nome;preco;estoque;categoria',
    'Ração Premium 15kg;89.90;50;Ração',
    'Shampoo para Cães;25.50;30;Higiene',
    'Bola de Borracha;12.00;100;Brinquedo',
  ].join('\n');
  downloadBlob('\uFEFF' + modelo, 'modelo-importacao-estoque.csv', 'text/csv;charset=utf-8;');
}

// ── CSV Import ────────────────────────────────────────────────────────────────

export interface LinhaCSV {
  nome: string;
  preco: number;
  quantidade: number;
  categoria: string;
}

export function parsearCSVEstoque(texto: string): { linhas: LinhaCSV[]; erros: string[] } {
  const linhas: LinhaCSV[] = [];
  const erros: string[] = [];
  const linhasTexto = texto.split(/\r?\n/).filter((l) => l.trim());

  // Skip header if present
  const startIndex = linhasTexto[0]?.toLowerCase().includes('nome') ? 1 : 0;

  linhasTexto.slice(startIndex).forEach((linha, idx) => {
    const num = idx + startIndex + 1;
    const partes = linha.split(';').map((p) => p.trim());

    if (partes.length < 2) {
      erros.push(`Linha ${num}: formato inválido (mínimo: nome;quantidade)`);
      return;
    }

    const nome = partes[0];
    const quantidade = Number(partes[2] !== undefined ? partes[2] : partes[1]);
    const preco = partes[2] !== undefined ? Number(partes[1]) : 0;
    const categoria = partes[3] ?? '';

    if (!nome) { erros.push(`Linha ${num}: nome vazio`); return; }
    if (isNaN(quantidade) || quantidade < 0) { erros.push(`Linha ${num}: quantidade inválida`); return; }

    linhas.push({ nome, preco, quantidade, categoria });
  });

  return { linhas, erros };
}

// ── PDF Export ────────────────────────────────────────────────────────────────

export function exportarPDF(titulo: string, htmlContent: string): void {
  const estilos = `
    <style>
      * { box-sizing: border-box; margin: 0; padding: 0; }
      body { font-family: 'Segoe UI', Arial, sans-serif; color: #1a1a2e; padding: 24px; font-size: 13px; }
      h1 { color: #6D28D9; font-size: 20px; margin-bottom: 4px; }
      .subtitle { color: #888; font-size: 11px; margin-bottom: 20px; }
      h2 { color: #4C1D95; font-size: 14px; margin: 20px 0 10px; border-bottom: 2px solid #EDE9FE; padding-bottom: 4px; }
      table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
      th { background: #EDE9FE; color: #4C1D95; text-align: left; padding: 8px 10px; font-size: 11px; text-transform: uppercase; }
      td { padding: 7px 10px; border-bottom: 1px solid #f0f0f0; }
      tr:nth-child(even) td { background: #F5F3FF; }
      .badge-red { background: #fdeaea; color: #C82828; border-radius: 99px; padding: 2px 8px; font-size: 11px; }
      .badge-yellow { background: #fdf3e0; color: #C88200; border-radius: 99px; padding: 2px 8px; font-size: 11px; }
      .badge-green { background: #DCFCE7; color: #16A34A; border-radius: 99px; padding: 2px 8px; font-size: 11px; }
      .footer { margin-top: 32px; text-align: center; color: #aaa; font-size: 10px; border-top: 1px solid #eee; padding-top: 12px; }
      @media print { body { padding: 16px; } }
    </style>
  `;

  const agora = new Date().toLocaleString('pt-BR');
  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>${titulo}</title>${estilos}</head><body>
    <h1>🐾 PetsTop · ${titulo}</h1>
    <p class="subtitle">Gerado em ${agora}</p>
    ${htmlContent}
    <div class="footer">PetsTop Pet Shop · Sistema de Vendas</div>
  </body></html>`;

  const win = window.open('', '_blank');
  if (!win) return;
  win.document.write(html);
  win.document.close();
  win.focus();
  setTimeout(() => { win.print(); }, 500);
}

// ── Internal helpers ──────────────────────────────────────────────────────────

function downloadBlob(content: string, filename: string, type: string): void {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
