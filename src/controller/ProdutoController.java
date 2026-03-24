package controller;

import model.Produto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ProdutoServico;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoServico produtoServico;

    public ProdutoController(ProdutoServico produtoServico) {
        this.produtoServico = produtoServico;
    }

    @GetMapping
    public List<Produto> listar(@RequestParam(required = false) String nome) {
        if (nome != null && !nome.isBlank()) return produtoServico.buscarPorNome(nome);
        return produtoServico.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(@PathVariable int id) {
        return produtoServico.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Produto> cadastrar(@RequestBody Map<String, Object> body) {
        Produto p = produtoServico.cadastrar(
                (String) body.get("nome"),
                Double.parseDouble(body.get("preco").toString()),
                Integer.parseInt(body.get("quantidadeEstoque").toString()),
                (String) body.get("categoria")
        );
        return ResponseEntity.status(201).body(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizar(@PathVariable int id, @RequestBody Map<String, Object> body) {
        if (body.containsKey("nome"))              produtoServico.atualizarNome(id, (String) body.get("nome"));
        if (body.containsKey("preco"))             produtoServico.atualizarPreco(id, Double.parseDouble(body.get("preco").toString()));
        if (body.containsKey("quantidadeEstoque")) produtoServico.atualizarEstoque(id, Integer.parseInt(body.get("quantidadeEstoque").toString()));
        return produtoServico.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/estoque")
    public ResponseEntity<Produto> ajustarEstoque(@PathVariable int id, @RequestBody Map<String, Object> body) {
        int quantidade = Integer.parseInt(body.get("quantidade").toString());
        String tipo = (String) body.get("tipo");

        Produto produto = produtoServico.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto com ID " + id + " não encontrado."));

        int novoEstoque = "ENTRADA".equalsIgnoreCase(tipo)
                ? produto.getQuantidadeEstoque() + quantidade
                : quantidade; // AJUSTE = define o valor absoluto

        produtoServico.atualizarEstoque(id, novoEstoque);
        return produtoServico.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable int id) {
        boolean removido = produtoServico.remover(id);
        return removido ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
