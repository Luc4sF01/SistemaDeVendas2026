// ─── Enums ────────────────────────────────────────────────────────────────────

export type Categoria =
  | 'Ração'
  | 'Brinquedo'
  | 'Higiene'
  | 'Medicamento'
  | 'Acessório'
  | 'Outro';

export type FormaPagamento =
  | 'DINHEIRO'
  | 'CARTAO_DEBITO'
  | 'CARTAO_CREDITO'
  | 'PIX';

// ─── Models ───────────────────────────────────────────────────────────────────

export interface Cliente {
  id: number;
  nome: string;
  telefone: string;
  email: string;
}

export interface Produto {
  id: number;
  nome: string;
  preco: number;
  quantidadeEstoque: number;
  categoria: Categoria;
}

export interface ItemVenda {
  produto: Produto;
  quantidade: number;
  precoUnitario: number;
}

export interface Venda {
  id: number;
  cliente: Cliente | null;
  itens: ItemVenda[];
  formaPagamento: FormaPagamento;
  dataHora: string;
  subtotal: number;
  desconto: number;
  total: number;
}

// ─── DTOs / Request Bodies ────────────────────────────────────────────────────

export interface ClienteForm {
  nome: string;
  telefone: string;
  email: string;
}

export interface ProdutoForm {
  nome: string;
  preco: number | string;
  quantidadeEstoque: number | string;
  categoria: Categoria | '';
}

export interface ItemVendaRequest {
  produtoId: number;
  quantidade: number;
}

export interface VendaRequest {
  clienteId: number;
  itens: ItemVendaRequest[];
  formaPagamento: FormaPagamento;
  desconto: number;
}

// ─── Relatórios ───────────────────────────────────────────────────────────────

export interface ResumoRelatorio {
  totalGeral: number;
  totalHoje: number;
  qtdVendas: number;
  ticketMedio: number;
}

export interface ProdutoMaisVendido {
  nome: string;
  qtd: number;
  receita: number;
}

export interface ReceitaPorCategoria {
  categoria: string;
  qtd: number;
  receita: number;
}

export interface ReceitaPorPagamento {
  forma: string;
  total: number;
  percentual: number;
}

// ─── Carrinho (client-side only) ──────────────────────────────────────────────

export interface ItemCarrinho {
  produto: Produto;
  quantidade: number;
}
