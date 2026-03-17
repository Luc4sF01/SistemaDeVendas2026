package repository;

import model.Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteRepositorio {
    private final List<Cliente> clientes = new ArrayList<>();
    private int proximoId = 1;

    public Cliente salvar(Cliente cliente) {
        clientes.add(cliente);
        return cliente;
    }

    public int gerarId() {
        return proximoId++;
    }

    public Optional<Cliente> buscarPorId(int id) {
        return clientes.stream().filter(c -> c.getId() == id).findFirst();
    }

    public List<Cliente> listarTodos() {
        return new ArrayList<>(clientes);
    }

    public List<Cliente> buscarPorNome(String nome) {
        String busca = nome.toLowerCase();
        return clientes.stream()
                .filter(c -> c.getNome().toLowerCase().contains(busca))
                .toList();
    }

    public boolean remover(int id) {
        return clientes.removeIf(c -> c.getId() == id);
    }
}
