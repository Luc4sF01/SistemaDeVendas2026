package controller;

import model.Cliente;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ClienteServico;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteServico clienteServico;

    public ClienteController(ClienteServico clienteServico) {
        this.clienteServico = clienteServico;
    }

    @GetMapping
    public List<Cliente> listar(@RequestParam(required = false) String nome) {
        if (nome != null && !nome.isBlank()) return clienteServico.buscarPorNome(nome);
        return clienteServico.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable int id) {
        return clienteServico.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Cliente> cadastrar(@RequestBody Map<String, String> body) {
        Cliente c = clienteServico.cadastrar(
                body.get("nome"),
                body.get("telefone"),
                body.getOrDefault("email", "")
        );
        return ResponseEntity.status(201).body(c);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(@PathVariable int id, @RequestBody Map<String, String> body) {
        if (body.containsKey("nome"))     clienteServico.atualizarNome(id, body.get("nome"));
        if (body.containsKey("telefone")) clienteServico.atualizarTelefone(id, body.get("telefone"));
        if (body.containsKey("email"))    clienteServico.atualizarEmail(id, body.get("email"));
        return clienteServico.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable int id) {
        boolean removido = clienteServico.remover(id);
        return removido ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
