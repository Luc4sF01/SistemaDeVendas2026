package model;

public class ItemVenda {
    private final Produto produto;
    private final int quantidade;
    private final double precoUnitario; // snapshot do preço no momento da venda

    public ItemVenda(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public double getPrecoUnitario() { return precoUnitario; }
    public double getSubtotal() { return precoUnitario * quantidade; }

    @Override
    public String toString() {
        return String.format("  %-25s x%2d  R$ %7.2f cada  = R$ %8.2f",
                produto.getNome(), quantidade, precoUnitario, getSubtotal());
    }
}
