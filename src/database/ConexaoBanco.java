package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerencia a conexão única com o banco SQLite.
 * O arquivo vendasPET.db é criado automaticamente na primeira execução,
 * na mesma pasta do programa.
 */
public class ConexaoBanco {

    private static final String URL = "jdbc:sqlite:vendasPET.db";
    private static Connection instancia;

    private ConexaoBanco() {}

    public static Connection get() {
        try {
            if (instancia == null || instancia.isClosed()) {
                instancia = DriverManager.getConnection(URL);
                try (Statement stmt = instancia.createStatement()) {
                    // WAL = melhor desempenho em escrita
                    stmt.execute("PRAGMA journal_mode=WAL");
                    // Ativa validação de chaves estrangeiras
                    stmt.execute("PRAGMA foreign_keys=ON");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao conectar ao banco de dados: " + e.getMessage(), e);
        }
        return instancia;
    }

    public static void fechar() {
        try {
            if (instancia != null && !instancia.isClosed()) {
                instancia.close();
            }
        } catch (SQLException ignored) {}
    }
}
