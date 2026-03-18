import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Package, AlertTriangle, XCircle, CheckCircle, Edit3 } from 'lucide-react';
import { produtosService } from '../services/produtosService';
import { StatCard } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import type { Produto } from '../types';
import { formatCurrency, getEstoqueBadge } from '../utils';

export function Estoque() {
  const qc = useQueryClient();
  const [selected, setSelected] = useState<Produto | null>(null);
  const [novaQty, setNovaQty] = useState('');
  const [modal, setModal] = useState(false);

  const { data: produtos, isLoading, isError, refetch } = useQuery({
    queryKey: ['produtos'],
    queryFn: produtosService.listar,
  });

  const ajusteMutation = useMutation({
    mutationFn: ({ id, qty }: { id: number; qty: number }) =>
      produtosService.atualizarEstoque(id, qty),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['produtos'] });
      setModal(false);
      setSelected(null);
    },
  });

  if (isLoading) return <LoadingSpinner />;
  if (isError) return <ErrorMessage onRetry={refetch} />;

  const total = produtos?.length ?? 0;
  const zerados = produtos?.filter((p) => p.quantidadeEstoque === 0).length ?? 0;
  const criticos = produtos?.filter((p) => p.quantidadeEstoque > 0 && p.quantidadeEstoque <= 3).length ?? 0;
  const baixos = produtos?.filter((p) => p.quantidadeEstoque > 3 && p.quantidadeEstoque <= 10).length ?? 0;

  function openAjuste(p: Produto) {
    setSelected(p);
    setNovaQty(String(p.quantidadeEstoque));
    setModal(true);
  }

  return (
    <div className="space-y-5">
      {/* Summary cards */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Total de Produtos"
          value={String(total)}
          icon={<Package className="text-primary" size={22} />}
          iconBg="bg-primary-50"
        />
        <StatCard
          title="Estoque Zerado"
          value={String(zerados)}
          icon={<XCircle className="text-danger" size={22} />}
          iconBg="bg-danger-light"
        />
        <StatCard
          title="Crítico (≤ 3)"
          value={String(criticos)}
          icon={<AlertTriangle className="text-warning" size={22} />}
          iconBg="bg-warning-light"
        />
        <StatCard
          title="OK (> 10)"
          value={String(total - zerados - criticos - baixos)}
          icon={<CheckCircle className="text-success" size={22} />}
          iconBg="bg-success-light"
        />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b border-gray-100">
                <th className="px-4 py-3 text-left font-medium">ID</th>
                <th className="px-4 py-3 text-left font-medium">Nome</th>
                <th className="px-4 py-3 text-left font-medium">Categoria</th>
                <th className="px-4 py-3 text-right font-medium">Preço</th>
                <th className="px-4 py-3 text-center font-medium">Estoque</th>
                <th className="px-4 py-3 text-center font-medium">Situação</th>
                <th className="px-4 py-3 text-center font-medium">Ajustar</th>
              </tr>
            </thead>
            <tbody>
              {produtos?.map((p, i) => {
                const { label, color, rowClass } = getEstoqueBadge(p.quantidadeEstoque);
                const highlight = p.quantidadeEstoque <= 10;
                return (
                  <tr
                    key={p.id}
                    className={`border-t border-gray-50 ${
                      highlight ? rowClass.replace('text-', 'bg-').split(' ')[0] + '/20' : i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
                    }`}
                  >
                    <td className="px-4 py-3 text-gray-400">#{p.id}</td>
                    <td className={`px-4 py-3 font-medium ${highlight ? rowClass.split(' ')[1] : 'text-gray-800'}`}>
                      {p.nome}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{p.categoria}</td>
                    <td className="px-4 py-3 text-right font-semibold text-gray-700">
                      {formatCurrency(p.preco)}
                    </td>
                    <td className={`px-4 py-3 text-center font-bold text-lg ${highlight ? rowClass.split(' ')[1] : 'text-gray-700'}`}>
                      {p.quantidadeEstoque}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Badge color={color}>{label}</Badge>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => openAjuste(p)}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary-50 transition-colors"
                        title="Ajustar estoque"
                      >
                        <Edit3 size={15} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Ajuste Modal */}
      <Modal
        open={modal}
        onClose={() => setModal(false)}
        title="Ajustar Estoque"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancelar</Button>
            <Button
              onClick={() => {
                if (selected) ajusteMutation.mutate({ id: selected.id, qty: Number(novaQty) });
              }}
              loading={ajusteMutation.isPending}
            >
              Salvar
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <p className="text-sm font-medium text-gray-800">{selected?.nome}</p>
          <p className="text-sm text-gray-500">Estoque atual: <strong>{selected?.quantidadeEstoque}</strong></p>
          <div>
            <label className="form-label">Novo valor de estoque (absoluto)</label>
            <input
              type="number"
              min="0"
              value={novaQty}
              onChange={(e) => setNovaQty(e.target.value)}
              className="form-input"
              autoFocus
            />
          </div>
          {ajusteMutation.isError && (
            <p className="text-xs text-danger">{ajusteMutation.error?.message}</p>
          )}
        </div>
      </Modal>
    </div>
  );
}
