package controller;

import model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ProdutoServico;
import service.VendaServico;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaServico vendaServico;
    private final ProdutoServico produtoServico;

    public VendaController(VendaServico vendaServico, ProdutoServico produtoServico) {
        this.vendaServico = vendaServico;
        this.produtoServico = produtoServico;
    }

    @GetMapping
    public List<Venda> listar(@RequestParam(required = false) Integer clienteId) {
        if (clienteId != null) return vendaServico.listarPorCliente(clienteId);
        return vendaServico.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venda> buscarPorId(@PathVariable int id) {
        return vendaServico.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Venda> finalizar(@RequestBody VendaRequest req) {
        List<ItemVenda> itens = req.itens.stream().map(item -> {
            Produto produto = produtoServico.buscarPorId(item.produtoId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto " + item.produtoId + " não encontrado."));
            return new ItemVenda(produto, item.quantidade);
        }).toList();

        FormaPagamento forma = FormaPagamento.valueOf(req.formaPagamento);
        int clienteId = req.clienteId != null ? req.clienteId : 0;

        double desconto = req.desconto >= 0
                ? req.desconto
                : VendaServico.calcularDesconto(forma, itens.stream().mapToDouble(ItemVenda::getSubtotal).sum());

        Venda venda = vendaServico.finalizar(clienteId, itens, forma, desconto);
        return ResponseEntity.status(201).body(venda);
    }

    // DTO de entrada
    public static class VendaRequest {
        public Integer clienteId;
        public List<ItemRequest> itens;
        public String formaPagamento;
        public double desconto = -1;

        public static class ItemRequest {
            public int produtoId;
            public int quantidade;
        }
    }
}
