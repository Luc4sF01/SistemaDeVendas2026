package repository;

import model.Produto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositorio {
    private final List<Produto> produtos = new ArrayList<>();
    private int proximoId = 1;

    public Produto salvar(Produto produto) {
        produtos.add(produto);
        return produto;
    }

    public int gerarId() {
        return proximoId++;
    }

    public Optional<Produto> buscarPorId(int id) {
        return produtos.stream().filter(p -> p.getId() == id).findFirst();
    }

    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos);
    }

    public List<Produto> buscarPorNome(String nome) {
        String busca = nome.toLowerCase();
        return produtos.stream()
                .filter(p -> p.getNome().toLowerCase().contains(busca))
                .toList();
    }

    public boolean remover(int id) {
        return produtos.removeIf(p -> p.getId() == id);
    }
}
