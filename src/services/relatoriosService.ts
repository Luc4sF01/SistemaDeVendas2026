import api from './api';
import type {
  ResumoRelatorio,
  ProdutoMaisVendido,
  ReceitaPorCategoria,
  ReceitaPorPagamento,
  Produto,
  Venda,
} from '../types';

export const relatoriosService = {
  resumo: () =>
    api.get<ResumoRelatorio>('/relatorios/resumo').then((r) => r.data),

  maisVendidos: () =>
    api.get<ProdutoMaisVendido[]>('/relatorios/mais-vendidos').then((r) => r.data),

  porCategoria: () =>
    api.get<ReceitaPorCategoria[]>('/relatorios/por-categoria').then((r) => r.data),

  porPagamento: () =>
    api.get<ReceitaPorPagamento[]>('/relatorios/por-pagamento').then((r) => r.data),

  porPeriodo: (inicio: string, fim: string) =>
    api
      .get<Venda[]>('/relatorios/por-periodo', { params: { inicio, fim } })
      .then((r) => r.data),

  estoqueBaixo: () =>
    api.get<Produto[]>('/relatorios/estoque-baixo').then((r) => r.data),
};
