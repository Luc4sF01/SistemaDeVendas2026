import api from './api';
import type { Cliente, ClienteForm } from '../types';

export const clientesService = {
  listar: () => api.get<Cliente[]>('/clientes').then((r) => r.data),

  buscarPorNome: (nome: string) =>
    api.get<Cliente[]>('/clientes', { params: { nome } }).then((r) => r.data),

  buscarPorId: (id: number) =>
    api.get<Cliente>(`/clientes/${id}`).then((r) => r.data),

  criar: (data: ClienteForm) =>
    api.post<Cliente>('/clientes', data).then((r) => r.data),

  atualizar: (id: number, data: ClienteForm) =>
    api.put<Cliente>(`/clientes/${id}`, data).then((r) => r.data),

  remover: (id: number) => api.delete(`/clientes/${id}`),
};
