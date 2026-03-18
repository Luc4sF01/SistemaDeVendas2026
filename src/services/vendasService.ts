import api from './api';
import type { Venda, VendaRequest } from '../types';

export const vendasService = {
  listar: () => api.get<Venda[]>('/vendas').then((r) => r.data),

  buscarPorId: (id: number) =>
    api.get<Venda>(`/vendas/${id}`).then((r) => r.data),

  listarPorCliente: (clienteId: number) =>
    api.get<Venda[]>('/vendas', { params: { clienteId } }).then((r) => r.data),

  listarPorPeriodo: (inicio: string, fim: string) =>
    api
      .get<Venda[]>('/relatorios/por-periodo', { params: { inicio, fim } })
      .then((r) => r.data),

  finalizar: (data: VendaRequest) =>
    api.post<Venda>('/vendas', data).then((r) => r.data),
};
