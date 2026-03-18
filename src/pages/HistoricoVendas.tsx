import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Eye, Filter } from 'lucide-react';
import { vendasService } from '../services/vendasService';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import type { Venda } from '../types';
import { formatCurrency, formatDateTime, formaPagamentoLabel } from '../utils';

export function HistoricoVendas() {
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [filtrandoPeriodo, setFiltrandoPeriodo] = useState(false);
  const [vendaDetalhe, setVendaDetalhe] = useState<Venda | null>(null);

  const vendasQuery = useQuery({
    queryKey: ['vendas'],
    queryFn: vendasService.listar,
  });

  const periodoQuery = useQuery({
    queryKey: ['vendas-periodo', inicio, fim],
    queryFn: () => vendasService.listarPorPeriodo(inicio, fim),
    enabled: filtrandoPeriodo && !!inicio && !!fim,
  });

  const vendas = useMemo(() => {
    if (filtrandoPeriodo && periodoQuery.data) return periodoQuery.data;
    return vendasQuery.data ?? [];
  }, [vendasQuery.data, periodoQuery.data, filtrandoPeriodo]);

  const isLoading = vendasQuery.isLoading || (filtrandoPeriodo && periodoQuery.isLoading);
  const isError = vendasQuery.isError;

  function aplicarFiltro() {
    if (inicio && fim) setFiltrandoPeriodo(true);
  }

  function limparFiltro() {
    setInicio('');
    setFim('');
    setFiltrandoPeriodo(false);
  }

  if (isLoading) return <LoadingSpinner />;
  if (isError) return <ErrorMessage onRetry={() => vendasQuery.refetch()} />;

  const vendaOrdenada = [...vendas].reverse();

  return (
    <div className="space-y-4">
      {/* Period filter */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
        <div className="flex flex-wrap gap-3 items-end">
          <div>
            <label className="form-label">Data Inicial</label>
            <input
              type="date"
              value={inicio}
              onChange={(e) => setInicio(e.target.value)}
              className="form-input"
            />
          </div>
          <div>
            <label className="form-label">Data Final</label>
            <input
              type="date"
              value={fim}
              onChange={(e) => setFim(e.target.value)}
              className="form-input"
            />
          </div>
          <Button onClick={aplicarFiltro} disabled={!inicio || !fim}>
            <Filter size={14} /> Filtrar
          </Button>
          {filtrandoPeriodo && (
            <Button variant="ghost" onClick={limparFiltro}>
              Limpar
            </Button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="px-5 py-3 border-b border-gray-100 flex items-center justify-between">
          <span className="text-sm font-medium text-gray-700">
            {vendaOrdenada.length} venda{vendaOrdenada.length !== 1 ? 's' : ''}
            {filtrandoPeriodo && ' no período'}
          </span>
          {filtrandoPeriodo && (
            <span className="text-sm font-semibold text-primary">
              Total: {formatCurrency(vendaOrdenada.reduce((s, v) => s + v.total, 0))}
            </span>
          )}
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b border-gray-100">
                <th className="px-4 py-3 text-left font-medium">#</th>
                <th className="px-4 py-3 text-left font-medium">Data / Hora</th>
                <th className="px-4 py-3 text-left font-medium">Cliente</th>
                <th className="px-4 py-3 text-left font-medium">Pagamento</th>
                <th className="px-4 py-3 text-right font-medium">Subtotal</th>
                <th className="px-4 py-3 text-right font-medium">Desconto</th>
                <th className="px-4 py-3 text-right font-medium">Total</th>
                <th className="px-4 py-3 text-center font-medium">Detalhe</th>
              </tr>
            </thead>
            <tbody>
              {vendaOrdenada.map((v, i) => (
                <tr
                  key={v.id}
                  className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}
                >
                  <td className="px-4 py-3 text-gray-400">#{v.id}</td>
                  <td className="px-4 py-3 text-gray-600">{formatDateTime(v.dataHora)}</td>
                  <td className="px-4 py-3 font-medium text-gray-800">
                    {v.cliente?.nome ?? 'Avulso'}
                  </td>
                  <td className="px-4 py-3">
                    <Badge color="blue">{formaPagamentoLabel(v.formaPagamento)}</Badge>
                  </td>
                  <td className="px-4 py-3 text-right text-gray-600">{formatCurrency(v.subtotal)}</td>
                  <td className="px-4 py-3 text-right text-success">
                    {v.desconto > 0 ? `- ${formatCurrency(v.desconto)}` : '—'}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold text-gray-800">
                    {formatCurrency(v.total)}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => setVendaDetalhe(v)}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary-50 transition-colors"
                    >
                      <Eye size={15} />
                    </button>
                  </td>
                </tr>
              ))}
              {vendaOrdenada.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-4 py-8 text-center text-gray-400 text-sm">
                    Nenhuma venda encontrada.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Detalhe Modal */}
      <Modal
        open={vendaDetalhe !== null}
        onClose={() => setVendaDetalhe(null)}
        title={`Venda #${vendaDetalhe?.id}`}
        size="lg"
        footer={
          <Button variant="ghost" onClick={() => setVendaDetalhe(null)}>Fechar</Button>
        }
      >
        {vendaDetalhe && (
          <div className="space-y-4">
            {/* Info */}
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-gray-500">Data:</span>{' '}
                <strong>{formatDateTime(vendaDetalhe.dataHora)}</strong>
              </div>
              <div>
                <span className="text-gray-500">Cliente:</span>{' '}
                <strong>{vendaDetalhe.cliente?.nome ?? 'Avulso'}</strong>
              </div>
              <div>
                <span className="text-gray-500">Pagamento:</span>{' '}
                <Badge color="blue">{formaPagamentoLabel(vendaDetalhe.formaPagamento)}</Badge>
              </div>
            </div>

            {/* Items */}
            <div className="border border-gray-100 rounded-xl overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 text-gray-500 text-xs uppercase">
                    <th className="px-4 py-2.5 text-left font-medium">Produto</th>
                    <th className="px-4 py-2.5 text-center font-medium">Qtd</th>
                    <th className="px-4 py-2.5 text-right font-medium">Unit.</th>
                    <th className="px-4 py-2.5 text-right font-medium">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {vendaDetalhe.itens.map((item, i) => (
                    <tr key={i} className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}>
                      <td className="px-4 py-2.5 font-medium text-gray-800">{item.produto.nome}</td>
                      <td className="px-4 py-2.5 text-center">{item.quantidade}</td>
                      <td className="px-4 py-2.5 text-right">{formatCurrency(item.precoUnitario)}</td>
                      <td className="px-4 py-2.5 text-right font-semibold">
                        {formatCurrency(item.precoUnitario * item.quantidade)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Totals */}
            <div className="bg-gray-50 rounded-xl p-4 space-y-2 text-sm">
              <div className="flex justify-between text-gray-600">
                <span>Subtotal</span>
                <span>{formatCurrency(vendaDetalhe.subtotal)}</span>
              </div>
              {vendaDetalhe.desconto > 0 && (
                <div className="flex justify-between text-success">
                  <span>Desconto</span>
                  <span>- {formatCurrency(vendaDetalhe.desconto)}</span>
                </div>
              )}
              <div className="flex justify-between font-bold text-gray-800 text-base border-t border-gray-200 pt-2">
                <span>Total</span>
                <span>{formatCurrency(vendaDetalhe.total)}</span>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
