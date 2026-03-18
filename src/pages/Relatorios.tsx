import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell,
} from 'recharts';
import { Download, FileText, Filter, FileDown } from 'lucide-react';
import { relatoriosService } from '../services/relatoriosService';
import { vendasService } from '../services/vendasService';
import { StatCard } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { formatCurrency, formatDateTime, formaPagamentoLabel, exportarCSV, exportarPDF } from '../utils';

type Tab = 'resumo' | 'mais-vendidos' | 'por-categoria' | 'por-periodo';

const TABS: { id: Tab; label: string }[] = [
  { id: 'resumo', label: 'Resumo' },
  { id: 'mais-vendidos', label: 'Mais Vendidos' },
  { id: 'por-categoria', label: 'Por Categoria' },
  { id: 'por-periodo', label: 'Por Período' },
];

const CHART_COLORS = ['#7C3AED', '#16A34A', '#C82828', '#C88200', '#8B5CF6', '#0EA5E9'];

export function Relatorios() {
  const [tab, setTab] = useState<Tab>('resumo');
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [buscandoPeriodo, setBuscandoPeriodo] = useState(false);

  const resumoQ = useQuery({ queryKey: ['resumo'], queryFn: relatoriosService.resumo });
  const maisVendQ = useQuery({ queryKey: ['mais-vendidos'], queryFn: relatoriosService.maisVendidos });
  const catQ = useQuery({ queryKey: ['por-categoria'], queryFn: relatoriosService.porCategoria });
  const pagQ = useQuery({ queryKey: ['por-pagamento'], queryFn: relatoriosService.porPagamento });
  const periodoQ = useQuery({
    queryKey: ['por-periodo', inicio, fim],
    queryFn: () => vendasService.listarPorPeriodo(inicio, fim),
    enabled: buscandoPeriodo && !!inicio && !!fim,
  });

  return (
    <div className="space-y-5">
      {/* Tabs */}
      <div className="flex gap-1 bg-white border border-purple-100 rounded-xl p-1 w-fit shadow-sm">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              tab === t.id
                ? 'bg-primary text-white shadow-sm'
                : 'text-gray-600 hover:bg-primary-50 hover:text-primary'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'resumo' && (
        <ResumoTab resumoQ={resumoQ} pagQ={pagQ} />
      )}
      {tab === 'mais-vendidos' && (
        <MaisVendidosTab q={maisVendQ} />
      )}
      {tab === 'por-categoria' && (
        <CategoriaTab q={catQ} />
      )}
      {tab === 'por-periodo' && (
        <PeriodoTab
          q={periodoQ}
          inicio={inicio}
          fim={fim}
          setInicio={setInicio}
          setFim={setFim}
          onBuscar={() => setBuscandoPeriodo(true)}
          onLimpar={() => { setBuscandoPeriodo(false); setInicio(''); setFim(''); }}
        />
      )}
    </div>
  );
}

// ─── Sub-tabs ────────────────────────────────────────────────────────────────

function ResumoTab({ resumoQ, pagQ }: { resumoQ: any; pagQ: any }) {
  if (resumoQ.isLoading || pagQ.isLoading) return <LoadingSpinner />;
  if (resumoQ.isError) return <ErrorMessage />;

  const r = resumoQ.data;
  const pag = pagQ.data ?? [];

  function handlePDF() {
    const linhas = pag.map((p: any) => `
      <tr>
        <td>${formaPagamentoLabel(p.forma)}</td>
        <td>${formatCurrency(p.total)}</td>
        <td>${p.percentual.toFixed(1)}%</td>
      </tr>`).join('');

    const html = `
      <h2>Resumo Financeiro</h2>
      <table>
        <thead><tr><th>Métrica</th><th>Valor</th></tr></thead>
        <tbody>
          <tr><td>Total Hoje</td><td>${formatCurrency(r.totalHoje)}</td></tr>
          <tr><td>Total Geral</td><td>${formatCurrency(r.totalGeral)}</td></tr>
          <tr><td>Qtd. Vendas</td><td>${r.qtdVendas}</td></tr>
          <tr><td>Ticket Médio</td><td>${formatCurrency(r.ticketMedio)}</td></tr>
        </tbody>
      </table>
      <h2>Receita por Forma de Pagamento</h2>
      <table>
        <thead><tr><th>Forma</th><th>Total</th><th>%</th></tr></thead>
        <tbody>${linhas}</tbody>
      </table>`;
    exportarPDF('Resumo Financeiro', html);
  }

  return (
    <div className="space-y-5">
      <div className="flex justify-end">
        <Button size="sm" variant="ghost" onClick={handlePDF}>
          <FileDown size={13} /> Exportar PDF
        </Button>
      </div>

      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard title="Total Hoje" value={formatCurrency(r.totalHoje)} icon={<span className="text-xl">📅</span>} iconBg="bg-primary-50" />
        <StatCard title="Total Geral" value={formatCurrency(r.totalGeral)} icon={<span className="text-xl">💰</span>} iconBg="bg-success-light" />
        <StatCard title="Qtd. Vendas" value={String(r.qtdVendas)} icon={<span className="text-xl">🛍️</span>} iconBg="bg-primary-100" />
        <StatCard title="Ticket Médio" value={formatCurrency(r.ticketMedio)} icon={<span className="text-xl">📊</span>} iconBg="bg-warning-light" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-800">Receita por Forma de Pagamento</h3>
            <Button
              size="sm"
              variant="ghost"
              onClick={() =>
                exportarCSV(
                  pag.map((p: any) => ({ Forma: formaPagamentoLabel(p.forma), Total: p.total, Percentual: p.percentual })),
                  'pagamento.csv'
                )
              }
            >
              <Download size={13} /> CSV
            </Button>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase">
                <th className="px-4 py-2.5 text-left font-medium">Forma</th>
                <th className="px-4 py-2.5 text-right font-medium">Total</th>
                <th className="px-4 py-2.5 text-right font-medium">%</th>
              </tr>
            </thead>
            <tbody>
              {pag.map((p: any, i: number) => (
                <tr key={i} className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}>
                  <td className="px-4 py-3 font-medium">{formaPagamentoLabel(p.forma)}</td>
                  <td className="px-4 py-3 text-right font-semibold text-success">{formatCurrency(p.total)}</td>
                  <td className="px-4 py-3 text-right">
                    <Badge color="blue">{p.percentual.toFixed(1)}%</Badge>
                  </td>
                </tr>
              ))}
              {pag.length === 0 && (
                <tr><td colSpan={3} className="py-6 text-center text-gray-400 text-sm">Sem dados.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {pag.length > 0 && (
          <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
            <h3 className="font-semibold text-gray-800 mb-4">Distribuição por Pagamento</h3>
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie
                  data={pag.map((p: any) => ({ name: formaPagamentoLabel(p.forma), value: p.total }))}
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                  labelLine={false}
                >
                  {pag.map((_: any, i: number) => (
                    <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => formatCurrency(Number(v))} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}

function MaisVendidosTab({ q }: { q: any }) {
  if (q.isLoading) return <LoadingSpinner />;
  if (q.isError) return <ErrorMessage />;
  const data = q.data ?? [];

  function handlePDF() {
    const linhas = data.map((item: any, i: number) => `
      <tr>
        <td>#${i + 1}</td>
        <td>${item.nome}</td>
        <td>${item.qtd}</td>
        <td>${formatCurrency(item.receita)}</td>
      </tr>`).join('');
    const html = `
      <h2>Produtos Mais Vendidos</h2>
      <table>
        <thead><tr><th>Pos.</th><th>Produto</th><th>Qtd. Vendida</th><th>Receita</th></tr></thead>
        <tbody>${linhas}</tbody>
      </table>`;
    exportarPDF('Produtos Mais Vendidos', html);
  }

  return (
    <div className="space-y-5">
      <div className="flex justify-end gap-2">
        <Button size="sm" variant="ghost" onClick={() => exportarCSV(data, 'mais-vendidos.csv')}>
          <Download size={13} /> CSV
        </Button>
        <Button size="sm" variant="ghost" onClick={handlePDF}>
          <FileDown size={13} /> PDF
        </Button>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b border-gray-100">
                <th className="px-4 py-3 text-left font-medium">Pos.</th>
                <th className="px-4 py-3 text-left font-medium">Produto</th>
                <th className="px-4 py-3 text-right font-medium">Qtd. Vendida</th>
                <th className="px-4 py-3 text-right font-medium">Receita</th>
              </tr>
            </thead>
            <tbody>
              {data.map((item: any, i: number) => (
                <tr key={i} className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}>
                  <td className="px-4 py-3">
                    <span className={`font-bold ${i < 3 ? 'text-warning' : 'text-gray-400'}`}>
                      #{i + 1}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-gray-800">{item.nome}</td>
                  <td className="px-4 py-3 text-right">{item.qtd}</td>
                  <td className="px-4 py-3 text-right font-semibold text-success">{formatCurrency(item.receita)}</td>
                </tr>
              ))}
              {data.length === 0 && (
                <tr><td colSpan={4} className="py-8 text-center text-gray-400 text-sm">Sem dados.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {data.length > 0 && (
          <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
            <h3 className="font-semibold text-gray-800 mb-4">Top Produtos por Quantidade</h3>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={data.slice(0, 8)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="nome" tick={{ fontSize: 11 }} width={100} />
                <Tooltip />
                <Bar dataKey="qtd" fill="#7C3AED" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}

function CategoriaTab({ q }: { q: any }) {
  if (q.isLoading) return <LoadingSpinner />;
  if (q.isError) return <ErrorMessage />;
  const data = q.data ?? [];

  function handlePDF() {
    const linhas = data.map((item: any) => `
      <tr><td>${item.categoria}</td><td>${item.qtd}</td><td>${formatCurrency(item.receita)}</td></tr>`).join('');
    const html = `
      <h2>Receita por Categoria</h2>
      <table>
        <thead><tr><th>Categoria</th><th>Qtd.</th><th>Receita</th></tr></thead>
        <tbody>${linhas}</tbody>
      </table>`;
    exportarPDF('Receita por Categoria', html);
  }

  return (
    <div className="space-y-5">
      <div className="flex justify-end gap-2">
        <Button size="sm" variant="ghost" onClick={() => exportarCSV(data, 'por-categoria.csv')}>
          <Download size={13} /> CSV
        </Button>
        <Button size="sm" variant="ghost" onClick={handlePDF}>
          <FileDown size={13} /> PDF
        </Button>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-5">
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b">
                <th className="px-4 py-3 text-left font-medium">Categoria</th>
                <th className="px-4 py-3 text-right font-medium">Qtd.</th>
                <th className="px-4 py-3 text-right font-medium">Receita</th>
              </tr>
            </thead>
            <tbody>
              {data.map((item: any, i: number) => (
                <tr key={i} className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}>
                  <td className="px-4 py-3 font-medium text-gray-800">{item.categoria}</td>
                  <td className="px-4 py-3 text-right">{item.qtd}</td>
                  <td className="px-4 py-3 text-right font-semibold text-success">{formatCurrency(item.receita)}</td>
                </tr>
              ))}
              {data.length === 0 && (
                <tr><td colSpan={3} className="py-8 text-center text-gray-400 text-sm">Sem dados.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {data.length > 0 && (
          <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
            <h3 className="font-semibold text-gray-800 mb-4">Receita por Categoria</h3>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="categoria" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => `R$${(v / 1000).toFixed(0)}k`} />
                <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                <Bar dataKey="receita" fill="#16A34A" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}

function PeriodoTab({
  q, inicio, fim, setInicio, setFim, onBuscar, onLimpar,
}: {
  q: any; inicio: string; fim: string;
  setInicio: (v: string) => void;
  setFim: (v: string) => void;
  onBuscar: () => void;
  onLimpar: () => void;
}) {
  const vendas = q.data ?? [];

  function handlePDF() {
    const linhas = vendas.map((v: any) => `
      <tr>
        <td>#${v.id}</td>
        <td>${formatDateTime(v.dataHora)}</td>
        <td>${v.cliente?.nome ?? 'Avulso'}</td>
        <td>${formaPagamentoLabel(v.formaPagamento)}</td>
        <td>${formatCurrency(v.total)}</td>
      </tr>`).join('');
    const total = vendas.reduce((s: number, v: any) => s + v.total, 0);
    const html = `
      <h2>Vendas no Período: ${inicio} a ${fim}</h2>
      <p style="margin-bottom:12px;color:#555">${vendas.length} vendas · Total: ${formatCurrency(total)}</p>
      <table>
        <thead><tr><th>#</th><th>Data/Hora</th><th>Cliente</th><th>Pagamento</th><th>Total</th></tr></thead>
        <tbody>${linhas}</tbody>
      </table>`;
    exportarPDF(`Vendas ${inicio} a ${fim}`, html);
  }

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
        <div className="flex flex-wrap gap-3 items-end">
          <div>
            <label className="form-label">Início</label>
            <input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} className="form-input" />
          </div>
          <div>
            <label className="form-label">Fim</label>
            <input type="date" value={fim} onChange={(e) => setFim(e.target.value)} className="form-input" />
          </div>
          <Button onClick={onBuscar} disabled={!inicio || !fim}>
            <Filter size={14} /> Buscar
          </Button>
          {q.data && (
            <>
              <Button variant="ghost" onClick={onLimpar}>Limpar</Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() =>
                  exportarCSV(
                    vendas.map((v: any) => ({
                      ID: v.id,
                      Data: formatDateTime(v.dataHora),
                      Cliente: v.cliente?.nome ?? 'Avulso',
                      Pagamento: formaPagamentoLabel(v.formaPagamento),
                      Subtotal: v.subtotal,
                      Desconto: v.desconto,
                      Total: v.total,
                    })),
                    `vendas-${inicio}-${fim}.csv`
                  )
                }
              >
                <Download size={13} /> CSV
              </Button>
              <Button variant="ghost" size="sm" onClick={handlePDF}>
                <FileDown size={13} /> PDF
              </Button>
            </>
          )}
        </div>
      </div>

      {q.isLoading && <LoadingSpinner />}
      {q.isError && <ErrorMessage />}

      {q.data && (
        <>
          <div className="bg-primary-50 border border-primary-100 rounded-xl p-4 flex items-center justify-between">
            <span className="text-sm font-medium text-primary">
              {vendas.length} venda{vendas.length !== 1 ? 's' : ''} no período
            </span>
            <span className="text-base font-bold text-primary">
              Total: {formatCurrency(vendas.reduce((s: number, v: any) => s + v.total, 0))}
            </span>
          </div>

          <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b border-gray-100">
                  <th className="px-4 py-3 text-left font-medium">#</th>
                  <th className="px-4 py-3 text-left font-medium">Data/Hora</th>
                  <th className="px-4 py-3 text-left font-medium">Cliente</th>
                  <th className="px-4 py-3 text-left font-medium">Pagamento</th>
                  <th className="px-4 py-3 text-right font-medium">Total</th>
                </tr>
              </thead>
              <tbody>
                {vendas.map((v: any, i: number) => (
                  <tr key={v.id} className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}>
                    <td className="px-4 py-3 text-gray-400">#{v.id}</td>
                    <td className="px-4 py-3 text-gray-600">{formatDateTime(v.dataHora)}</td>
                    <td className="px-4 py-3 font-medium">{v.cliente?.nome ?? 'Avulso'}</td>
                    <td className="px-4 py-3">
                      <Badge color="blue">{formaPagamentoLabel(v.formaPagamento)}</Badge>
                    </td>
                    <td className="px-4 py-3 text-right font-semibold">{formatCurrency(v.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {vendas.length > 1 && (
            <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
              <h3 className="font-semibold text-gray-800 mb-4">Vendas no Período</h3>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={vendas.map((v: any) => ({ data: formatDateTime(v.dataHora).split(' ')[0], total: v.total }))}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="data" tick={{ fontSize: 10 }} />
                  <YAxis tickFormatter={(v) => `R$${v}`} tick={{ fontSize: 10 }} />
                  <Tooltip formatter={(v) => formatCurrency(Number(v))} />
                  <Bar dataKey="total" fill="#7C3AED" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </>
      )}

      {!q.data && !q.isLoading && (
        <div className="bg-white rounded-xl border border-dashed border-purple-200 p-12 flex flex-col items-center gap-3 text-gray-400">
          <FileText size={32} className="text-primary-200" />
          <p className="text-sm">Selecione um período para gerar o relatório.</p>
        </div>
      )}
    </div>
  );
}
