package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Venda {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final int id;
    private final Cliente cliente;
    private final List<ItemVenda> itens;
    private final FormaPagamento formaPagamento;
    private final LocalDateTime dataHora;
    private final double subtotal;
    private final double desconto;   // valor absoluto do desconto (R$)
    private final double total;      // subtotal - desconto

    /** Construtor usado ao registrar uma nova venda. */
    public Venda(int id, Cliente cliente, List<ItemVenda> itens,
                 FormaPagamento formaPagamento, double desconto) {
        this.id = id;
        this.cliente = cliente;
        this.itens = itens;
        this.formaPagamento = formaPagamento;
        this.desconto = desconto;
        this.dataHora = LocalDateTime.now();
        this.subtotal = itens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        this.total    = subtotal - desconto;
    }

    /** Construtor usado ao carregar uma venda do banco de dados. */
    public Venda(int id, Cliente cliente, List<ItemVenda> itens,
                 FormaPagamento formaPagamento, double desconto, LocalDateTime dataHora) {
        this.id = id;
        this.cliente = cliente;
        this.itens = itens;
        this.formaPagamento = formaPagamento;
        this.desconto = desconto;
        this.dataHora = dataHora;
        this.subtotal = itens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        this.total    = subtotal - desconto;
    }

    public int getId()                    { return id; }
    public Cliente getCliente()           { return cliente; }
    public List<ItemVenda> getItens()     { return itens; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public LocalDateTime getDataHora()    { return dataHora; }
    public double getSubtotal()           { return subtotal; }
    public double getDesconto()           { return desconto; }
    public double getTotal()              { return total; }
}
