package repository;

import database.ConexaoBanco;
import model.Cliente;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepositorio {

    /** ID temporário — será substituído pelo gerado no banco após salvar(). */
    public int gerarId() {
        return 0;
    }

    public Cliente salvar(Cliente cliente) {
        String sql = "INSERT INTO clientes (nome, telefone, email) VALUES (?, ?, ?)";
        try (PreparedStatement ps = ConexaoBanco.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cliente.getNome());
            ps.setString(2, cliente.getTelefone());
            ps.setString(3, cliente.getEmail());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) cliente.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar cliente: " + e.getMessage(), e);
        }
        return cliente;
    }

    public void atualizar(Cliente cliente) {
        String sql = "UPDATE clientes SET nome=?, telefone=?, email=? WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setString(1, cliente.getNome());
            ps.setString(2, cliente.getTelefone());
            ps.setString(3, cliente.getEmail());
            ps.setInt(4, cliente.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar cliente: " + e.getMessage(), e);
        }
    }

    public Optional<Cliente> buscarPorId(int id) {
        String sql = "SELECT * FROM clientes WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar cliente: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes ORDER BY nome";
        try (Statement stmt = ConexaoBanco.get().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar clientes: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<Cliente> buscarPorNome(String nome) {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE LOWER(nome) LIKE ? ORDER BY nome";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setString(1, "%" + nome.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar clientes: " + e.getMessage(), e);
        }
        return lista;
    }

    public boolean remover(int id) {
        String sql = "DELETE FROM clientes WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover cliente: " + e.getMessage(), e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("telefone"),
                rs.getString("email"));
    }
}
