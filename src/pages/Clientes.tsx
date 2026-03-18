import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Search, Edit2, Trash2 } from 'lucide-react';
import { clientesService } from '../services/clientesService';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import type { Cliente, ClienteForm } from '../types';

const EMPTY: ClienteForm = { nome: '', telefone: '', email: '' };

export function Clientes() {
  const qc = useQueryClient();
  const [busca, setBusca] = useState('');
  const [modal, setModal] = useState<'add' | 'edit' | null>(null);
  const [selected, setSelected] = useState<Cliente | null>(null);
  const [form, setForm] = useState<ClienteForm>(EMPTY);
  const [errors, setErrors] = useState<Partial<ClienteForm>>({});
  const [deleteId, setDeleteId] = useState<number | null>(null);

  const { data: clientes, isLoading, isError, refetch } = useQuery({
    queryKey: ['clientes'],
    queryFn: clientesService.listar,
  });

  const criarMutation = useMutation({
    mutationFn: clientesService.criar,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['clientes'] }); closeModal(); },
  });

  const editarMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ClienteForm }) => clientesService.atualizar(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['clientes'] }); closeModal(); },
  });

  const deletarMutation = useMutation({
    mutationFn: clientesService.remover,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['clientes'] }); setDeleteId(null); },
  });

  const filtrados = useMemo(() => {
    if (!clientes) return [];
    if (!busca.trim()) return clientes;
    return clientes.filter((c) => c.nome.toLowerCase().includes(busca.toLowerCase()));
  }, [clientes, busca]);

  function openAdd() {
    setForm(EMPTY);
    setErrors({});
    setModal('add');
  }

  function openEdit(c: Cliente) {
    setSelected(c);
    setForm({ nome: c.nome, telefone: c.telefone, email: c.email });
    setErrors({});
    setModal('edit');
  }

  function closeModal() {
    setModal(null);
    setSelected(null);
    setForm(EMPTY);
  }

  function validate(): boolean {
    const e: Partial<ClienteForm> = {};
    if (!form.nome.trim()) e.nome = 'Nome é obrigatório';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function handleSubmit() {
    if (!validate()) return;
    if (modal === 'add') criarMutation.mutate(form);
    else if (modal === 'edit' && selected) editarMutation.mutate({ id: selected.id, data: form });
  }

  if (isLoading) return <LoadingSpinner />;
  if (isError) return <ErrorMessage onRetry={refetch} />;

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex gap-3 items-center">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={15} />
          <input
            type="text"
            placeholder="Buscar por nome..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className="form-input pl-9"
          />
        </div>
        <div className="ml-auto">
          <Button onClick={openAdd}>
            <Plus size={15} /> Novo Cliente
          </Button>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase border-b border-gray-100">
                <th className="px-4 py-3 text-left font-medium">Nome</th>
                <th className="px-4 py-3 text-left font-medium">Telefone</th>
                <th className="px-4 py-3 text-left font-medium">E-mail</th>
                <th className="px-4 py-3 text-center font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((c, i) => (
                <tr
                  key={c.id}
                  className={`border-t border-gray-50 ${i % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}`}
                >
                  <td className="px-4 py-3 font-medium text-gray-800">{c.nome}</td>
                  <td className="px-4 py-3 text-gray-600">{c.telefone || '—'}</td>
                  <td className="px-4 py-3 text-gray-600">{c.email || '—'}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-center gap-1">
                      <button
                        onClick={() => openEdit(c)}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-primary hover:bg-primary-50 transition-colors"
                      >
                        <Edit2 size={15} />
                      </button>
                      <button
                        onClick={() => setDeleteId(c.id)}
                        className="p-1.5 rounded-lg text-gray-400 hover:text-danger hover:bg-danger-light transition-colors"
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filtrados.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-gray-400 text-sm">
                    Nenhum cliente encontrado.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal */}
      <Modal
        open={modal !== null}
        onClose={closeModal}
        title={modal === 'add' ? 'Novo Cliente' : 'Editar Cliente'}
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
          <Field label="Nome *" error={errors.nome}>
            <input
              type="text"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              className="form-input"
              placeholder="Nome completo"
              autoFocus
            />
          </Field>
          <Field label="Telefone" error={errors.telefone}>
            <input
              type="tel"
              value={form.telefone}
              onChange={(e) => setForm({ ...form, telefone: e.target.value })}
              className="form-input"
              placeholder="(00) 00000-0000"
            />
          </Field>
          <Field label="E-mail" error={errors.email}>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="form-input"
              placeholder="email@exemplo.com"
            />
          </Field>
          {(criarMutation.isError || editarMutation.isError) && (
            <p className="text-xs text-danger">
              {criarMutation.error?.message || editarMutation.error?.message}
            </p>
          )}
        </div>
      </Modal>

      <ConfirmDialog
        open={deleteId !== null}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && deletarMutation.mutate(deleteId)}
        title="Remover Cliente"
        message="Tem certeza que deseja remover este cliente?"
        loading={deletarMutation.isPending}
      />
    </div>
  );
}

function Field({
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
