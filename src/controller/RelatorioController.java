package controller;

import model.Produto;
import model.Venda;
import org.springframework.web.bind.annotation.*;
import service.ProdutoServico;
import service.VendaServico;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final VendaServico vendaServico;
    private final ProdutoServico produtoServico;

    public RelatorioController(VendaServico vendaServico, ProdutoServico produtoServico) {
        this.vendaServico = vendaServico;
        this.produtoServico = produtoServico;
    }

    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        List<Venda> todas = vendaServico.listarTodas();
        double totalGeral = todas.stream().mapToDouble(Venda::getTotal).sum();
        double totalHoje  = vendaServico.totalVendasHoje();
        double ticketMedio = todas.isEmpty() ? 0 : totalGeral / todas.size();

        return Map.of(
                "totalGeral",  totalGeral,
                "totalHoje",   totalHoje,
                "qtdVendas",   todas.size(),
                "ticketMedio", ticketMedio
        );
    }

    @GetMapping("/mais-vendidos")
    public List<Map<String, Object>> maisVendidos() {
        Map<String, double[]> mapa = new LinkedHashMap<>(); // nome -> [qtd, receita]
        vendaServico.listarTodas().stream()
                .flatMap(v -> v.getItens().stream())
                .forEach(i -> {
                    mapa.computeIfAbsent(i.getProduto().getNome(), k -> new double[]{0, 0});
                    double[] arr = mapa.get(i.getProduto().getNome());
                    arr[0] += i.getQuantidade();
                    arr[1] += i.getSubtotal();
                });

        return mapa.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nome", e.getKey());
                    m.put("qtd", (int) e.getValue()[0]);
                    m.put("receita", e.getValue()[1]);
                    return m;
                })
                .toList();
    }

    @GetMapping("/por-categoria")
    public List<Map<String, Object>> porCategoria() {
        Map<String, double[]> mapa = new LinkedHashMap<>(); // categoria -> [qtd, receita]
        vendaServico.listarTodas().stream()
                .flatMap(v -> v.getItens().stream())
                .forEach(i -> {
                    String cat = i.getProduto().getCategoria();
                    mapa.computeIfAbsent(cat, k -> new double[]{0, 0});
                    double[] arr = mapa.get(cat);
                    arr[0] += i.getQuantidade();
                    arr[1] += i.getSubtotal();
                });

        return mapa.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("categoria", e.getKey());
                    m.put("qtd", (int) e.getValue()[0]);
                    m.put("receita", e.getValue()[1]);
                    return m;
                })
                .toList();
    }

    @GetMapping("/por-pagamento")
    public List<Map<String, Object>> porPagamento() {
        Map<String, Double> totais = vendaServico.totalPorFormaPagamento();
        double totalGeral = totais.values().stream().mapToDouble(Double::doubleValue).sum();

        return totais.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("forma", e.getKey());
                    m.put("total", e.getValue());
                    m.put("percentual", totalGeral > 0 ? (e.getValue() / totalGeral * 100) : 0);
                    return m;
                })
                .toList();
    }

    @GetMapping("/por-periodo")
    public List<Venda> porPeriodo(
            @RequestParam String inicio,
            @RequestParam String fim) {
        LocalDate dataInicio = LocalDate.parse(inicio);
        LocalDate dataFim    = LocalDate.parse(fim);
        return vendaServico.listarTodas().stream()
                .filter(v -> {
                    LocalDate data = v.getDataHora().toLocalDate();
                    return !data.isBefore(dataInicio) && !data.isAfter(dataFim);
                })
                .toList();
    }

    @GetMapping("/estoque-baixo")
    public List<Produto> estoqueBaixo() {
        return produtoServico.listarEstoqueBaixo(10);
    }
}
