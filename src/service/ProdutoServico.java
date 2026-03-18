package service;

import model.Produto;
import repository.ProdutoRepositorio;

import java.util.List;
import java.util.Optional;

public class ProdutoServico {
    private final ProdutoRepositorio repositorio;

    public ProdutoServico(ProdutoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Produto cadastrar(String nome, double preco, int estoque, String categoria) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do produto é obrigatório.");
        if (preco <= 0) throw new IllegalArgumentException("Preço deve ser maior que zero.");
        if (estoque < 0) throw new IllegalArgumentException("Estoque não pode ser negativo.");
        Produto produto = new Produto(repositorio.gerarId(), nome.trim(), preco, estoque, categoria);
        return repositorio.salvar(produto);
    }

    public Optional<Produto> buscarPorId(int id) {
        return repositorio.buscarPorId(id);
    }

    public List<Produto> listarTodos() {
        return repositorio.listarTodos();
    }

    public List<Produto> buscarPorNome(String nome) {
        return repositorio.buscarPorNome(nome);
    }

    public void atualizarNome(int id, String novoNome) {
        Produto p = buscarOuErro(id);
        if (novoNome == null || novoNome.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio.");
        p.setNome(novoNome.trim());
        repositorio.atualizar(p);
    }

    public void atualizarPreco(int id, double novoPreco) {
        Produto p = buscarOuErro(id);
        if (novoPreco <= 0) throw new IllegalArgumentException("Preço deve ser maior que zero.");
        p.setPreco(novoPreco);
        repositorio.atualizar(p);
    }

    public void atualizarEstoque(int id, int novoEstoque) {
        Produto p = buscarOuErro(id);
        if (novoEstoque < 0) throw new IllegalArgumentException("Estoque não pode ser negativo.");
        p.setQuantidadeEstoque(novoEstoque);
        repositorio.atualizar(p);
    }

    public void descontarEstoque(int id, int quantidade) {
        Produto p = buscarOuErro(id);
        if (p.getQuantidadeEstoque() < quantidade) {
            throw new IllegalStateException(
                    "Estoque insuficiente para '" + p.getNome() + "'. Disponível: " + p.getQuantidadeEstoque());
        }
        p.setQuantidadeEstoque(p.getQuantidadeEstoque() - quantidade);
        repositorio.atualizar(p);
    }

    public boolean remover(int id) {
        return repositorio.remover(id);
    }

    public List<Produto> listarEstoqueBaixo(int limite) {
        return repositorio.listarTodos().stream()
                .filter(p -> p.getQuantidadeEstoque() <= limite)
                .toList();
    }

    private Produto buscarOuErro(int id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto com ID " + id + " não encontrado."));
    }
}
