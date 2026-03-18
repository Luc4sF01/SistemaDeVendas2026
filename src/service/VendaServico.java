package service;

import model.*;
import org.springframework.stereotype.Service;
import repository.VendaRepositorio;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VendaServico {
    private final VendaRepositorio repositorio;
    private final ProdutoServico produtoServico;
    private final ClienteServico clienteServico;

    public VendaServico(VendaRepositorio repositorio, ProdutoServico produtoServico, ClienteServico clienteServico) {
        this.repositorio = repositorio;
        this.produtoServico = produtoServico;
        this.clienteServico = clienteServico;
    }

    /** 5% de desconto para Dinheiro e PIX. */
    public static double calcularDesconto(FormaPagamento pagamento, double subtotal) {
        if (pagamento == FormaPagamento.DINHEIRO || pagamento == FormaPagamento.PIX)
            return subtotal * 0.05;
        return 0;
    }

    /** Finaliza uma venda: valida estoque, desconta e persiste. clienteId=0 para avulso. */
    public Venda finalizar(int clienteId, List<ItemVenda> itens, FormaPagamento formaPagamento) {
        double subtotal = itens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        return finalizar(clienteId, itens, formaPagamento, calcularDesconto(formaPagamento, subtotal));
    }

    /** Versão com desconto explícito (valor em R$). */
    public Venda finalizar(int clienteId, List<ItemVenda> itens, FormaPagamento formaPagamento, double desconto) {
        if (itens == null || itens.isEmpty())
            throw new IllegalArgumentException("A venda deve ter ao menos um item.");

        for (ItemVenda item : itens) {
            int estoque = item.getProduto().getQuantidadeEstoque();
            if (estoque < item.getQuantidade())
                throw new IllegalStateException(
                        "Estoque insuficiente para '" + item.getProduto().getNome() +
                        "'. Disponível: " + estoque + ", solicitado: " + item.getQuantidade());
        }

        for (ItemVenda item : itens)
            produtoServico.descontarEstoque(item.getProduto().getId(), item.getQuantidade());

        Cliente cliente = clienteId > 0 ? clienteServico.buscarPorId(clienteId).orElse(null) : null;
        Venda venda = new Venda(repositorio.gerarId(), cliente, itens, formaPagamento, desconto);
        return repositorio.salvar(venda);
    }

    public List<Venda> listarTodas() { return repositorio.listarTodas(); }

    public Optional<Venda> buscarPorId(int id) { return repositorio.buscarPorId(id); }

    public List<Venda> listarPorCliente(int clienteId) { return repositorio.listarPorCliente(clienteId); }

    public List<Venda> vendasDeHoje() {
        LocalDate hoje = LocalDate.now();
        return repositorio.listarTodas().stream()
                .filter(v -> v.getDataHora().toLocalDate().equals(hoje))
                .toList();
    }

    public double totalVendas() {
        return repositorio.listarTodas().stream().mapToDouble(Venda::getTotal).sum();
    }

    public double totalVendasHoje() {
        return vendasDeHoje().stream().mapToDouble(Venda::getTotal).sum();
    }

    public double ticketMedio() {
        List<Venda> todas = repositorio.listarTodas();
        return todas.isEmpty() ? 0 : totalVendas() / todas.size();
    }

    /** Retorna mapa {nomeProduto -> quantidadeVendida} ordenado do mais para o menos vendido. */
    public Map<String, Integer> produtosMaisVendidos() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        repositorio.listarTodas().stream()
                .flatMap(v -> v.getItens().stream())
                .forEach(i -> mapa.merge(i.getProduto().getNome(), i.getQuantidade(), Integer::sum));
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** Retorna mapa {categoria -> receita} ordenado pela maior receita. */
    public Map<String, Double> receitaPorCategoria() {
        Map<String, Double> mapa = new LinkedHashMap<>();
        repositorio.listarTodas().stream()
                .flatMap(v -> v.getItens().stream())
                .forEach(i -> mapa.merge(i.getProduto().getCategoria(), i.getSubtotal(), Double::sum));
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** Retorna mapa {formaPagamento -> total} */
    public Map<String, Double> totalPorFormaPagamento() {
        Map<String, Double> mapa = new LinkedHashMap<>();
        repositorio.listarTodas()
                .forEach(v -> mapa.merge(v.getFormaPagamento().getDescricao(), v.getTotal(), Double::sum));
        return mapa;
    }

    public List<Produto> relatorioEstoqueBaixo(int limite) {
        return produtoServico.listarEstoqueBaixo(limite);
    }
}
