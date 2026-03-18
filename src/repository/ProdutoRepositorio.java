package repository;

import database.ConexaoBanco;
import model.Produto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositorio {

    /** ID temporário — será substituído pelo gerado no banco após salvar(). */
    public int gerarId() {
        return 0;
    }

    public Produto salvar(Produto produto) {
        String sql = "INSERT INTO produtos (nome, preco, quantidade_estoque, categoria) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = ConexaoBanco.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, produto.getNome());
            ps.setDouble(2, produto.getPreco());
            ps.setInt(3, produto.getQuantidadeEstoque());
            ps.setString(4, produto.getCategoria());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) produto.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar produto: " + e.getMessage(), e);
        }
        return produto;
    }

    public void atualizar(Produto produto) {
        String sql = "UPDATE produtos SET nome=?, preco=?, quantidade_estoque=?, categoria=? WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setString(1, produto.getNome());
            ps.setDouble(2, produto.getPreco());
            ps.setInt(3, produto.getQuantidadeEstoque());
            ps.setString(4, produto.getCategoria());
            ps.setInt(5, produto.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar produto: " + e.getMessage(), e);
        }
    }

    public Optional<Produto> buscarPorId(int id) {
        String sql = "SELECT * FROM produtos WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produto: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Produto> listarTodos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos ORDER BY nome";
        try (Statement stmt = ConexaoBanco.get().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<Produto> buscarPorNome(String nome) {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos WHERE LOWER(nome) LIKE ? ORDER BY nome";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setString(1, "%" + nome.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage(), e);
        }
        return lista;
    }

    public boolean remover(int id) {
        String sql = "DELETE FROM produtos WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover produto: " + e.getMessage(), e);
        }
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getDouble("preco"),
                rs.getInt("quantidade_estoque"),
                rs.getString("categoria"));
    }
}
