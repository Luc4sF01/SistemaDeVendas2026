import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Search, Edit2, Trash2, PlusCircle } from 'lucide-react';
import { produtosService } from '../services/produtosService';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import type { Produto, ProdutoForm, Categoria } from '../types';
import { formatCurrency, getEstoqueBadge } from '../utils';

const CATEGORIAS: Categoria[] = ['Ração', 'Brinquedo', 'Higiene', 'Medicamento', 'Acessório', 'Outro'];

const EMPTY_FORM: ProdutoForm = { nome: '', preco: '', quantidadeEstoque: '', categoria: '' };

export function Produtos() {
  const qc = useQueryClient();
  const [busca, setBusca] = useState('');
  const [catFiltro, setCatFiltro] = useState('');
  const [modal, setModal] = useState<'add' | 'edit' | 'estoque' | null>(null);
  const [selected, setSelected] = useState<Produto | null>(null);
  const [form, setForm] = useState<ProdutoForm>(EMPTY_FORM);
  const [estoqueQty, setEstoqueQty] = useState('');
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { data: produtos, isLoading, isError, refetch } = useQuery({
    queryKey: ['produtos'],
    queryFn: produtosService.listar,
  });

  const criarMutation = useMutation({
    mutationFn: produtosService.criar,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['produtos'] }); closeModal(); },
  });

  const editarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ProdutoForm }) => produtosService.atualizar(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['produtos'] }); closeModal(); },
  });

  const estoqueMutation = useMutation({
    mutationFn: ({ id, qty }: { id: number; qty: number }) => produtosService.atualizarEstoque(id, qty),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['produtos'] }); closeModal(); },
  });

  const deletarMutation = useMutation({
    mutationFn: produtosService.remover,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['produtos'] }); setDeleteId(null); },
  });

  const filtrados = useMemo(() => {
    if (!produtos) return [];
    return produtos.filter((p) => {
      const matchNome = !busca || p.nome.toLowerCase().includes(busca.toLowerCase());
      const matchCat = !catFiltro || p.categoria === catFiltro;
      return matchNome && matchCat;
    });
  }, [produtos, busca, catFiltro]);

  function openAdd() {
    setForm(EMPTY_FORM);
    setErrors({});
    setModal('add');
  }

  function openEdit(p: Produto) {
    setSelected(p);
    setForm({ nome: p.nome, preco: p.preco, quantidadeEstoque: p.quantidadeEstoque, categoria: p.categoria });
    setErrors({});
    setModal('edit');
  }

  function openEstoque(p: Produto) {
    setSelected(p);
    setEstoqueQty('');
    setModal('estoque');
  }

  function closeModal() {
    setModal(null);
    setSelected(null);
    setForm(EMPTY_FORM);
  }

  function validateForm(): boolean {
    const e: Record<string, string> = {};
    if (!form.nome.trim()) e.nome = 'Nome é obrigatório';
    if (!form.preco || Number(form.preco) <= 0) e.preco = 'Preço inválido';
    if (form.quantidadeEstoque === '' || Number(form.quantidadeEstoque) < 0) e.quantidadeEstoque = 'Estoque inválido';
    if (!form.categoria) e.categoria = 'Categoria é obrigatória';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function handleSubmit() {
    if (!validateForm()) return;
    if (modal === 'add') criarMutation.mutate(form);
    else if (modal === 'edit' && selected) editarMutation.mutate({ id: selected.id, data: form });
  }

  function handleEstoque() {
    if (!selected || !estoqueQty) return;
    estoqueMutation.mutate({ id: selected.id, qty: Number(estoqueQty) });
  }

  if (isLoading) return <LoadingSpinner />;
  if (isError) return <ErrorMessage onRetry={refetch} />;

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={15} />
          <input
            type="text"
            placeholder="Buscar produto..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className="form-input pl-9"
          />
        </div>
        <select
          value={catFiltro}
          onChange={(e) => setCatFiltro(e.target.value)}
          className="form-select w-40"
        >
          <option value="">Todas as categorias</option>
          {CATEGORIAS.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <div className="ml-auto">
          <Button onClick={openAdd}>
            <Plus size={15} /> Novo Produto
          </Button>
        </div>
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
                <th className="px-4 py-3 text-center font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((p, i) => {
                const { label, color } = getEstoqueBadge(p.quantidadeEstoque);
                return (
                  <tr
                    key={p.id}
                    className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}
                  >
                    <td className="px-4 py-3 text-gray-400">#{p.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-800">{p.nome}</td>
                    <td className="px-4 py-3">
                      <span className="text-xs bg-gray-100 text-gray-600 rounded px-2 py-0.5">
                        {p.categoria}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-semibold text-primary">
                      {formatCurrency(p.preco)}
                    </td>
                    <td className="px-4 py-3 text-center font-medium">{p.quantidadeEstoque}</td>
                    <td className="px-4 py-3 text-center">
                      <Badge color={color}>{label}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-center gap-1">
                        <button
                          onClick={() => openEstoque(p)}
                          title="Entrada de estoque"
                          className="p-1.5 rounded-lg text-gray-400 hover:text-success hover:bg-success-light transition-colors"
                        >
                          <PlusCircle size={15} />
                        </button>
                        <button
                          onClick={() => openEdit(p)}
                          title="Editar"
                          className="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary-50 transition-colors"
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          onClick={() => setDeleteId(p.id)}
                          title="Remover"
                          className="p-1.5 rounded-lg text-gray-400 hover:text-danger hover:bg-danger-light transition-colors"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {filtrados.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-gray-400 text-sm">
                    Nenhum produto encontrado.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add/Edit Modal */}
      <Modal
        open={modal === 'add' || modal === 'edit'}
        onClose={closeModal}
        title={modal === 'add' ? 'Novo Produto' : 'Editar Produto'}
        footer={
          <>
            <Button variant="ghost" onClick={closeModal}>Cancelar</Button>
            <Button
              onClick={handleSubmit}
              loading={criarMutation.isPending || editarMutation.isPending}
            >
              {modal === 'add' ? 'Cadastrar' : 'Salvar'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <FormField label="Nome" error={errors.nome as string}>
            <input
              type="text"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              className="form-input"
              placeholder="Nome do produto"
            />
          </FormField>
          <div className="grid grid-cols-2 gap-4">
            <FormField label="Preço (R$)" error={errors.preco as string}>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.preco}
                onChange={(e) => setForm({ ...form, preco: e.target.value })}
                className="form-input"
                placeholder="0.00"
              />
            </FormField>
            <FormField label="Estoque" error={errors.quantidadeEstoque as string}>
              <input
                type="number"
                min="0"
                value={form.quantidadeEstoque}
                onChange={(e) => setForm({ ...form, quantidadeEstoque: e.target.value })}
                className="form-input"
                placeholder="0"
              />
            </FormField>
          </div>
          <FormField label="Categoria" error={errors.categoria as string}>
            <select
              value={form.categoria}
              onChange={(e) => setForm({ ...form, categoria: e.target.value as Categoria })}
              className="form-select"
            >
              <option value="">Selecione...</option>
              {CATEGORIAS.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </FormField>
          {(criarMutation.isError || editarMutation.isError) && (
            <p className="text-xs text-danger">
              {criarMutation.error?.message || editarMutation.error?.message}
            </p>
          )}
        </div>
      </Modal>

      {/* Estoque Modal */}
      <Modal
        open={modal === 'estoque'}
        onClose={closeModal}
        title="Entrada de Estoque"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={closeModal}>Cancelar</Button>
            <Button
              variant="success"
              onClick={handleEstoque}
              loading={estoqueMutation.isPending}
              disabled={!estoqueQty}
            >
              Confirmar
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <p className="text-sm text-gray-700">
            Produto: <strong>{selected?.nome}</strong>
          </p>
          <p className="text-sm text-gray-500">Estoque atual: {selected?.quantidadeEstoque}</p>
          <FormField label="Quantidade a adicionar">
            <input
              type="number"
              min="1"
              value={estoqueQty}
              onChange={(e) => setEstoqueQty(e.target.value)}
              className="form-input"
              placeholder="Ex: 10"
              autoFocus
            />
          </FormField>
          {estoqueQty && (
            <p className="text-sm text-success">
              Novo estoque: {(selected?.quantidadeEstoque ?? 0) + Number(estoqueQty)}
            </p>
          )}
        </div>
      </Modal>

      {/* Confirm delete */}
      <ConfirmDialog
        open={deleteId !== null}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && deletarMutation.mutate(deleteId)}
        title="Remover Produto"
        message="Tem certeza que deseja remover este produto? Esta ação não pode ser desfeita."
        loading={deletarMutation.isPending}
      />
    </div>
  );
}

function FormField({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="form-label">{label}</label>
      {children}
      {error && <p className="text-xs text-danger mt-1">{error}</p>}
    </div>
  );
}
