import api from './api';
import type { Produto, ProdutoForm } from '../types';

export const produtosService = {
  listar: () => api.get<Produto[]>('/produtos').then((r) => r.data),

  buscarPorNome: (nome: string) =>
    api.get<Produto[]>('/produtos', { params: { nome } }).then((r) => r.data),

  buscarPorId: (id: number) =>
    api.get<Produto>(`/produtos/${id}`).then((r) => r.data),

  criar: (data: ProdutoForm) =>
    api.post<Produto>('/produtos', data).then((r) => r.data),

  atualizar: (id: number, data: ProdutoForm) =>
    api.put<Produto>(`/produtos/${id}`, data).then((r) => r.data),

  atualizarEstoque: (id: number, quantidade: number) =>
    api.patch<Produto>(`/produtos/${id}/estoque`, { quantidade }).then((r) => r.data),

  remover: (id: number) => api.delete(`/produtos/${id}`),
};
