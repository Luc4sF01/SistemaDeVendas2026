package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gerencia a conexão única com o banco PostgreSQL.
 * Credenciais lidas das variáveis de ambiente DB_URL, DB_USER e DB_PASSWORD.
 */
public class ConexaoBanco {

    private static final String URL = System.getenv("DB_URL");

    private static Connection instancia;

    private ConexaoBanco() {}

    public static Connection get() {
        try {
            if (instancia == null || instancia.isClosed()) {
                instancia = DriverManager.getConnection(URL);
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
