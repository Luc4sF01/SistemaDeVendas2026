package model;

public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartão Débito"),
    CARTAO_CREDITO("Cartão Crédito"),
    PIX("Pix");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
