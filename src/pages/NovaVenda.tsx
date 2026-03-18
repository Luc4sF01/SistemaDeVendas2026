import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Plus, Minus, Trash2, ShoppingCart, User, CheckCircle2 } from 'lucide-react';
import { produtosService } from '../services/produtosService';
import { clientesService } from '../services/clientesService';
import { vendasService } from '../services/vendasService';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Badge } from '../components/ui/Badge';
import type { ItemCarrinho, FormaPagamento, Cliente, Produto } from '../types';
import { formatCurrency, calcularDesconto, formaPagamentoLabel } from '../utils';

const FORMAS: FormaPagamento[] = ['DINHEIRO', 'CARTAO_DEBITO', 'CARTAO_CREDITO', 'PIX'];

export function NovaVenda() {
  const queryClient = useQueryClient();

  const [busca, setBusca] = useState('');
  const [carrinho, setCarrinho] = useState<ItemCarrinho[]>([]);
  const [clienteSelecionado, setClienteSelecionado] = useState<Cliente | null>(null);
  const [buscaCliente, setBuscaCliente] = useState('');
  const [forma, setForma] = useState<FormaPagamento>('DINHEIRO');
  const [valorRecebido, setValorRecebido] = useState('');
  const [modalCliente, setModalCliente] = useState(false);
  const [successModal, setSuccessModal] = useState(false);
  const [vendaId, setVendaId] = useState<number | null>(null);

  const produtosQuery = useQuery({
    queryKey: ['produtos'],
    queryFn: produtosService.listar,
    select: (data) => data.filter((p) => p.quantidadeEstoque > 0),
  });

  const clientesQuery = useQuery({
    queryKey: ['clientes'],
    queryFn: clientesService.listar,
    enabled: modalCliente,
  });

  const mutation = useMutation({
    mutationFn: vendasService.finalizar,
    onSuccess: (venda) => {
      setVendaId(venda.id);
      setSuccessModal(true);
      setCarrinho([]);
      setClienteSelecionado(null);
      setForma('DINHEIRO');
      setValorRecebido('');
      queryClient.invalidateQueries({ queryKey: ['produtos'] });
      queryClient.invalidateQueries({ queryKey: ['vendas-recentes'] });
      queryClient.invalidateQueries({ queryKey: ['resumo'] });
      queryClient.invalidateQueries({ queryKey: ['estoque-baixo'] });
    },
  });

  const produtosFiltrados = useMemo(() => {
    if (!produtosQuery.data) return [];
    if (!busca.trim()) return produtosQuery.data;
    const lower = busca.toLowerCase();
    return produtosQuery.data.filter(
      (p) => p.nome.toLowerCase().includes(lower) || p.categoria.toLowerCase().includes(lower)
    );
  }, [produtosQuery.data, busca]);

  const clientesFiltrados = useMemo(() => {
    if (!clientesQuery.data) return [];
    if (!buscaCliente.trim()) return clientesQuery.data;
    return clientesQuery.data.filter((c) =>
      c.nome.toLowerCase().includes(buscaCliente.toLowerCase())
    );
  }, [clientesQuery.data, buscaCliente]);

  const subtotal = carrinho.reduce((s, i) => s + i.produto.preco * i.quantidade, 0);
  const desconto = calcularDesconto(forma, subtotal);
  const total = subtotal - desconto;
  const troco = forma === 'DINHEIRO' ? (parseFloat(valorRecebido) || 0) - total : 0;

  function addItem(produto: Produto) {
    setCarrinho((prev) => {
      const existing = prev.find((i) => i.produto.id === produto.id);
      if (existing) {
        const maxQty = produto.quantidadeEstoque;
        if (existing.quantidade >= maxQty) return prev;
        return prev.map((i) =>
          i.produto.id === produto.id ? { ...i, quantidade: i.quantidade + 1 } : i
        );
      }
      return [...prev, { produto, quantidade: 1 }];
    });
  }

  function changeQty(produtoId: number, delta: number) {
    setCarrinho((prev) =>
      prev
        .map((i) =>
          i.produto.id === produtoId ? { ...i, quantidade: i.quantidade + delta } : i
        )
        .filter((i) => i.quantidade > 0)
    );
  }

  function removeItem(produtoId: number) {
    setCarrinho((prev) => prev.filter((i) => i.produto.id !== produtoId));
  }

  function handleFinalizar() {
    if (carrinho.length === 0) return;
    mutation.mutate({
      clienteId: clienteSelecionado?.id ?? 0,
      itens: carrinho.map((i) => ({ produtoId: i.produto.id, quantidade: i.quantidade })),
      formaPagamento: forma,
      desconto,
    });
  }

  if (produtosQuery.isLoading) return <LoadingSpinner />;
  if (produtosQuery.isError)
    return <ErrorMessage message="Erro ao carregar produtos." onRetry={() => produtosQuery.refetch()} />;

  return (
    <div className="flex gap-5 h-full min-h-0">
      {/* ── Left: product catalog ── */}
      <div className="flex-1 min-w-0 flex flex-col gap-4">
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
          <input
            type="text"
            placeholder="Buscar produto por nome ou categoria..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className="form-input pl-9"
          />
        </div>

        {/* Products grid */}
        <div className="grid grid-cols-2 xl:grid-cols-3 gap-3 overflow-y-auto max-h-[calc(100vh-220px)]">
          {produtosFiltrados.map((p) => {
            const inCart = carrinho.find((i) => i.produto.id === p.id);
            return (
              <button
                key={p.id}
                onClick={() => addItem(p)}
                className="bg-white rounded-xl border border-gray-200 p-4 text-left hover:border-primary hover:shadow-md transition-all group"
              >
                <div className="flex items-start justify-between mb-2">
                  <span className="text-xs text-gray-400 bg-gray-100 rounded px-1.5 py-0.5">
                    {p.categoria}
                  </span>
                  {inCart && (
                    <span className="text-xs bg-primary text-white rounded-full w-5 h-5 flex items-center justify-center font-bold">
                      {inCart.quantidade}
                    </span>
                  )}
                </div>
                <p className="text-sm font-medium text-gray-800 line-clamp-2 group-hover:text-primary">
                  {p.nome}
                </p>
                <p className="text-base font-bold text-primary mt-1">{formatCurrency(p.preco)}</p>
                <p className="text-xs text-gray-400 mt-1">Estoque: {p.quantidadeEstoque}</p>
              </button>
            );
          })}
          {produtosFiltrados.length === 0 && (
            <p className="col-span-full text-sm text-gray-400 text-center py-8">
              Nenhum produto encontrado.
            </p>
          )}
        </div>
      </div>

      {/* ── Right: Cart & checkout ── */}
      <div className="w-80 xl:w-96 flex flex-col gap-3 flex-shrink-0">
        {/* Client */}
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-700 flex items-center gap-1">
              <User size={14} /> Cliente
            </span>
            <button
              className="text-xs text-primary hover:underline"
              onClick={() => setModalCliente(true)}
            >
              {clienteSelecionado ? 'Trocar' : 'Selecionar'}
            </button>
          </div>
          <p className="text-sm font-semibold text-gray-800">
            {clienteSelecionado?.nome ?? 'Venda Avulsa'}
          </p>
          {clienteSelecionado && (
            <button
              className="text-xs text-gray-400 hover:text-danger mt-0.5"
              onClick={() => setClienteSelecionado(null)}
            >
              Remover cliente
            </button>
          )}
        </div>

        {/* Cart */}
        <div className="bg-white rounded-xl border border-gray-200 flex-1 flex flex-col min-h-0">
          <div className="flex items-center gap-2 px-4 py-3 border-b border-gray-100">
            <ShoppingCart size={15} className="text-primary" />
            <span className="text-sm font-semibold text-gray-800">
              Carrinho ({carrinho.length})
            </span>
          </div>

          <div className="flex-1 overflow-y-auto divide-y divide-gray-50 max-h-48">
            {carrinho.length === 0 ? (
              <p className="text-xs text-gray-400 text-center py-8">Carrinho vazio</p>
            ) : (
              carrinho.map((item) => (
                <div key={item.produto.id} className="flex items-center gap-2 px-4 py-2.5">
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium text-gray-800 truncate">{item.produto.nome}</p>
                    <p className="text-xs text-gray-400">{formatCurrency(item.produto.preco)}</p>
                  </div>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => changeQty(item.produto.id, -1)}
                      className="w-6 h-6 flex items-center justify-center rounded bg-gray-100 hover:bg-gray-200 text-gray-600"
                    >
                      <Minus size={12} />
                    </button>
                    <span className="w-6 text-center text-xs font-semibold">{item.quantidade}</span>
                    <button
                      onClick={() => changeQty(item.produto.id, 1)}
                      disabled={item.quantidade >= item.produto.quantidadeEstoque}
                      className="w-6 h-6 flex items-center justify-center rounded bg-gray-100 hover:bg-gray-200 text-gray-600 disabled:opacity-40"
                    >
                      <Plus size={12} />
                    </button>
                    <button
                      onClick={() => removeItem(item.produto.id)}
                      className="w-6 h-6 flex items-center justify-center rounded hover:bg-red-50 text-gray-400 hover:text-danger ml-1"
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>
                  <span className="text-xs font-semibold text-gray-700 w-16 text-right">
                    {formatCurrency(item.produto.preco * item.quantidade)}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Payment */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 space-y-3">
          <label className="form-label">Forma de Pagamento</label>
          <div className="grid grid-cols-2 gap-2">
            {FORMAS.map((f) => (
              <button
                key={f}
                onClick={() => setForma(f)}
                className={`text-xs py-2 px-2 rounded-lg border font-medium transition-all ${
                  forma === f
                    ? 'bg-primary text-white border-primary'
                    : 'bg-white text-gray-600 border-gray-200 hover:border-primary'
                }`}
              >
                {formaPagamentoLabel(f)}
              </button>
            ))}
          </div>

          {forma === 'DINHEIRO' && (
            <div>
              <label className="form-label">Valor Recebido</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={valorRecebido}
                onChange={(e) => setValorRecebido(e.target.value)}
                className="form-input"
                placeholder="R$ 0,00"
              />
              {valorRecebido && (
                <p className={`text-sm font-semibold mt-1 ${troco >= 0 ? 'text-success' : 'text-danger'}`}>
                  Troco: {formatCurrency(Math.max(0, troco))}
                  {troco < 0 && ' (insuficiente)'}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Totals */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 space-y-2">
          <div className="flex justify-between text-sm text-gray-500">
            <span>Subtotal</span>
            <span>{formatCurrency(subtotal)}</span>
          </div>
          {desconto > 0 && (
            <div className="flex justify-between text-sm text-success">
              <span>Desconto (5%)</span>
              <span>- {formatCurrency(desconto)}</span>
            </div>
          )}
          <div className="flex justify-between text-base font-bold text-gray-800 border-t pt-2">
            <span>Total</span>
            <span>{formatCurrency(total)}</span>
          </div>
          {(forma === 'DINHEIRO' || forma === 'PIX') && (
            <Badge color="green" className="w-full justify-center py-1">
              5% de desconto aplicado
            </Badge>
          )}
        </div>

        <Button
          variant="success"
          size="lg"
          className="w-full"
          disabled={carrinho.length === 0}
          loading={mutation.isPending}
          onClick={handleFinalizar}
        >
          <CheckCircle2 size={18} />
          Finalizar Venda
        </Button>

        {mutation.isError && (
          <p className="text-xs text-danger text-center">{mutation.error?.message}</p>
        )}
      </div>

      {/* Modal: Select client */}
      <Modal
        open={modalCliente}
        onClose={() => setModalCliente(false)}
        title="Selecionar Cliente"
        size="md"
      >
        <div className="space-y-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
            <input
              type="text"
              placeholder="Buscar cliente..."
              value={buscaCliente}
              onChange={(e) => setBuscaCliente(e.target.value)}
              className="form-input pl-9"
            />
          </div>
          {clientesQuery.isLoading ? (
            <LoadingSpinner text="Buscando clientes..." />
          ) : (
            <ul className="divide-y divide-gray-100 max-h-64 overflow-y-auto">
              {clientesFiltrados.map((c) => (
                <li
                  key={c.id}
                  className="flex items-center justify-between px-2 py-3 hover:bg-gray-50 cursor-pointer rounded-lg"
                  onClick={() => {
                    setClienteSelecionado(c);
                    setModalCliente(false);
                    setBuscaCliente('');
                  }}
                >
                  <div>
                    <p className="text-sm font-medium text-gray-800">{c.nome}</p>
                    <p className="text-xs text-gray-400">{c.telefone}</p>
                  </div>
                  {clienteSelecionado?.id === c.id && (
                    <CheckCircle2 className="text-success" size={16} />
                  )}
                </li>
              ))}
              {clientesFiltrados.length === 0 && (
                <p className="text-sm text-gray-400 text-center py-4">Nenhum cliente encontrado.</p>
              )}
            </ul>
          )}
        </div>
      </Modal>

      {/* Modal: success */}
      <Modal
        open={successModal}
        onClose={() => setSuccessModal(false)}
        title="Venda Finalizada!"
        size="sm"
        footer={
          <Button variant="success" onClick={() => setSuccessModal(false)}>
            OK
          </Button>
        }
      >
        <div className="flex flex-col items-center gap-3 py-4">
          <CheckCircle2 className="text-success" size={48} />
          <p className="text-base font-semibold text-gray-800">
            Venda #{vendaId} registrada com sucesso!
          </p>
        </div>
      </Modal>
    </div>
  );
}
