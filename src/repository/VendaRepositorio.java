package repository;

import database.ConexaoBanco;
import model.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class VendaRepositorio {

    /** ID temporário — o banco gera o ID real no INSERT. */
    public int gerarId() {
        return 0;
    }

    /**
     * Persiste a venda e todos os seus itens numa transação única.
     * Retorna uma nova instância de Venda com o ID gerado pelo banco.
     */
    public Venda salvar(Venda venda) {
        Connection conn = ConexaoBanco.get();
        try {
            conn.setAutoCommit(false);

            // 1. Insere a venda principal
            int vendaId;
            String sqlVenda = """
                INSERT INTO vendas (cliente_id, forma_pagamento, data_hora, subtotal, desconto, total)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                if (venda.getCliente() != null) {
                    ps.setInt(1, venda.getCliente().getId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setString(2, venda.getFormaPagamento().name());
                ps.setTimestamp(3, Timestamp.valueOf(venda.getDataHora()));
                ps.setDouble(4, venda.getSubtotal());
                ps.setDouble(5, venda.getDesconto());
                ps.setDouble(6, venda.getTotal());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    vendaId = rs.getInt(1);
                }
            }

            // 2. Insere cada item (snapshot do produto no momento da venda)
            String sqlItem = """
                INSERT INTO itens_venda (venda_id, produto_id, nome_produto, quantidade, preco_unitario, categoria)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                for (ItemVenda item : venda.getItens()) {
                    ps.setInt(1, vendaId);
                    ps.setInt(2, item.getProduto().getId());
                    ps.setString(3, item.getProduto().getNome());
                    ps.setInt(4, item.getQuantidade());
                    ps.setDouble(5, item.getPrecoUnitario());
                    ps.setString(6, item.getProduto().getCategoria());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();

            // Retorna uma nova instância com o ID real e a data original
            return new Venda(vendaId, venda.getCliente(), venda.getItens(),
                    venda.getFormaPagamento(), venda.getDesconto(), venda.getDataHora());

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Erro ao salvar venda: " + e.getMessage(), e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public Optional<Venda> buscarPorId(int id) {
        return listarTodas().stream().filter(v -> v.getId() == id).findFirst();
    }

    public List<Venda> listarTodas() {
        List<Venda> vendas = new ArrayList<>();
        String sql = "SELECT * FROM vendas ORDER BY data_hora DESC";
        try (Statement stmt = ConexaoBanco.get().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                vendas.add(montarVenda(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar vendas: " + e.getMessage(), e);
        }
        return vendas;
    }

    public List<Venda> listarPorCliente(int clienteId) {
        List<Venda> vendas = new ArrayList<>();
        String sql = "SELECT * FROM vendas WHERE cliente_id=? ORDER BY data_hora DESC";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) vendas.add(montarVenda(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar vendas do cliente: " + e.getMessage(), e);
        }
        return vendas;
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private Venda montarVenda(ResultSet rsVenda) throws SQLException {
        int vendaId = rsVenda.getInt("id");

        // Carrega o cliente (pode ser nulo para vendas avulsas)
        Cliente cliente = null;
        int clienteId = rsVenda.getInt("cliente_id");
        if (!rsVenda.wasNull()) {
            cliente = buscarClientePorId(clienteId);
        }

        List<ItemVenda> itens = carregarItens(vendaId);
        FormaPagamento formaPagamento = FormaPagamento.valueOf(rsVenda.getString("forma_pagamento"));
        double desconto = rsVenda.getDouble("desconto");
        LocalDateTime dataHora = rsVenda.getTimestamp("data_hora").toLocalDateTime();

        return new Venda(vendaId, cliente, itens, formaPagamento, desconto, dataHora);
    }

    private List<ItemVenda> carregarItens(int vendaId) throws SQLException {
        List<ItemVenda> itens = new ArrayList<>();
        String sql = "SELECT * FROM itens_venda WHERE venda_id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Reconstrói o produto com os dados do momento da venda (snapshot)
                    Produto produto = new Produto(
                            rs.getInt("produto_id"),
                            rs.getString("nome_produto"),
                            rs.getDouble("preco_unitario"),
                            0,
                            rs.getString("categoria"));
                    itens.add(new ItemVenda(produto, rs.getInt("quantidade")));
                }
            }
        }
        return itens;
    }

    private Cliente buscarClientePorId(int id) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE id=?";
        try (PreparedStatement ps = ConexaoBanco.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(rs.getInt("id"), rs.getString("nome"),
                            rs.getString("telefone"), rs.getString("email"));
                }
            }
        }
        return null;
    }
}
