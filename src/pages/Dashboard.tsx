import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  DollarSign,
  TrendingUp,
  ShoppingBag,
  Receipt,
  AlertTriangle,
  ShoppingCart,
  Clock,
} from 'lucide-react';
import { relatoriosService } from '../services/relatoriosService';
import { vendasService } from '../services/vendasService';
import { StatCard } from '../components/ui/Card';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { formatCurrency, formatDateTime, getEstoqueBadge, formaPagamentoLabel } from '../utils';

export function Dashboard() {
  const navigate = useNavigate();

  const resumoQuery = useQuery({
    queryKey: ['resumo'],
    queryFn: relatoriosService.resumo,
    refetchInterval: 60_000,
  });

  const vendasQuery = useQuery({
    queryKey: ['vendas-recentes'],
    queryFn: vendasService.listar,
    refetchInterval: 60_000,
    select: (data) => data.slice().reverse().slice(0, 10),
  });

  const estoqueBaixoQuery = useQuery({
    queryKey: ['estoque-baixo'],
    queryFn: relatoriosService.estoqueBaixo,
    refetchInterval: 120_000,
  });

  const isLoading = resumoQuery.isLoading || vendasQuery.isLoading || estoqueBaixoQuery.isLoading;
  const isError = resumoQuery.isError || vendasQuery.isError || estoqueBaixoQuery.isError;

  if (isLoading) return <LoadingSpinner />;
  if (isError) return <ErrorMessage message="Erro ao carregar o dashboard." />;

  const resumo = resumoQuery.data!;
  const vendas = vendasQuery.data ?? [];
  const estoqueBaixo = estoqueBaixoQuery.data ?? [];

  return (
    <div className="space-y-6">
      {/* Quick action */}
      <div className="flex justify-end">
        <Button variant="success" size="lg" onClick={() => navigate('/nova-venda')}>
          <ShoppingCart size={18} />
          Nova Venda
        </Button>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Total Vendido Hoje"
          value={formatCurrency(resumo.totalHoje)}
          icon={<DollarSign className="text-success" size={22} />}
          iconBg="bg-success-light"
        />
        <StatCard
          title="Total Geral"
          value={formatCurrency(resumo.totalGeral)}
          icon={<TrendingUp className="text-primary" size={22} />}
          iconBg="bg-primary-50"
        />
        <StatCard
          title="Total de Vendas"
          value={resumo.qtdVendas.toString()}
          icon={<ShoppingBag className="text-blue-500" size={22} />}
          iconBg="bg-blue-50"
        />
        <StatCard
          title="Ticket Médio"
          value={formatCurrency(resumo.ticketMedio)}
          icon={<Receipt className="text-purple-500" size={22} />}
          iconBg="bg-purple-50"
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        {/* Últimas vendas */}
        <div className="xl:col-span-2 bg-white rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center gap-2 px-5 py-4 border-b border-gray-100">
            <Clock size={16} className="text-primary" />
            <h2 className="font-semibold text-gray-800">Últimas Vendas</h2>
          </div>
          {vendas.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Nenhuma venda registrada.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 text-gray-500 text-xs uppercase">
                    <th className="px-4 py-2.5 text-left font-medium">#</th>
                    <th className="px-4 py-2.5 text-left font-medium">Data / Hora</th>
                    <th className="px-4 py-2.5 text-left font-medium">Cliente</th>
                    <th className="px-4 py-2.5 text-left font-medium">Pagamento</th>
                    <th className="px-4 py-2.5 text-right font-medium">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {vendas.map((v, i) => (
                    <tr
                      key={v.id}
                      className={`border-t border-gray-50 hover:bg-gray-50 transition-colors cursor-pointer ${
                        i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'
                      }`}
                      onClick={() => navigate('/historico')}
                    >
                      <td className="px-4 py-3 text-gray-400">#{v.id}</td>
                      <td className="px-4 py-3 text-gray-600">{formatDateTime(v.dataHora)}</td>
                      <td className="px-4 py-3 font-medium text-gray-800">
                        {v.cliente?.nome ?? 'Avulso'}
                      </td>
                      <td className="px-4 py-3">
                        <Badge color="blue">{formaPagamentoLabel(v.formaPagamento)}</Badge>
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-gray-800">
                        {formatCurrency(v.total)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Estoque baixo */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center gap-2 px-5 py-4 border-b border-gray-100">
            <AlertTriangle size={16} className="text-warning" />
            <h2 className="font-semibold text-gray-800">Estoque Baixo</h2>
            {estoqueBaixo.length > 0 && (
              <Badge color="yellow" className="ml-auto">{estoqueBaixo.length}</Badge>
            )}
          </div>
          {estoqueBaixo.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Todos os estoques OK.</p>
          ) : (
            <ul className="divide-y divide-gray-50 max-h-72 overflow-y-auto">
              {estoqueBaixo.map((p) => {
                const { label, color } = getEstoqueBadge(p.quantidadeEstoque);
                return (
                  <li
                    key={p.id}
                    className="flex items-center justify-between px-5 py-3 hover:bg-gray-50 cursor-pointer"
                    onClick={() => navigate('/estoque')}
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-800 truncate">{p.nome}</p>
                      <p className="text-xs text-gray-400">{p.categoria}</p>
                    </div>
                    <Badge color={color}>{label}</Badge>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
