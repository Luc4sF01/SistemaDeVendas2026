package service;

import model.Cliente;
import repository.ClienteRepositorio;

import java.util.List;
import java.util.Optional;

public class ClienteServico {
    private final ClienteRepositorio repositorio;

    public ClienteServico(ClienteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Cliente cadastrar(String nome, String telefone, String email) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do cliente é obrigatório.");
        if (telefone == null || telefone.isBlank()) throw new IllegalArgumentException("Telefone é obrigatório.");
        Cliente cliente = new Cliente(repositorio.gerarId(), nome.trim(), telefone.trim(), email == null ? "" : email.trim());
        return repositorio.salvar(cliente);
    }

    public Optional<Cliente> buscarPorId(int id) {
        return repositorio.buscarPorId(id);
    }

    public List<Cliente> listarTodos() {
        return repositorio.listarTodos();
    }

    public List<Cliente> buscarPorNome(String nome) {
        return repositorio.buscarPorNome(nome);
    }

    public void atualizarNome(int id, String novoNome) {
        Cliente c = buscarOuErro(id);
        if (novoNome == null || novoNome.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio.");
        c.setNome(novoNome.trim());
        repositorio.atualizar(c);
    }

    public void atualizarTelefone(int id, String novoTelefone) {
        Cliente c = buscarOuErro(id);
        if (novoTelefone == null || novoTelefone.isBlank()) throw new IllegalArgumentException("Telefone não pode ser vazio.");
        c.setTelefone(novoTelefone.trim());
        repositorio.atualizar(c);
    }

    public void atualizarEmail(int id, String novoEmail) {
        Cliente c = buscarOuErro(id);
        c.setEmail(novoEmail == null ? "" : novoEmail.trim());
        repositorio.atualizar(c);
    }

    public boolean remover(int id) {
        return repositorio.remover(id);
    }

    private Cliente buscarOuErro(int id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente com ID " + id + " não encontrado."));
    }
}
