package database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cria as tabelas na primeira execução.
 * Seguro chamar sempre ao iniciar — usa IF NOT EXISTS.
 */
public class InicializadorBanco {

    public static void inicializar() {
        try (Statement stmt = ConexaoBanco.get().createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS clientes (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome      TEXT    NOT NULL,
                    telefone  TEXT    NOT NULL,
                    email     TEXT    NOT NULL DEFAULT ''
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS produtos (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome                TEXT    NOT NULL,
                    preco               REAL    NOT NULL,
                    quantidade_estoque  INTEGER NOT NULL DEFAULT 0,
                    categoria           TEXT    NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vendas (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    cliente_id       INTEGER,
                    forma_pagamento  TEXT    NOT NULL,
                    data_hora        TEXT    NOT NULL,
                    subtotal         REAL    NOT NULL,
                    desconto         REAL    NOT NULL,
                    total            REAL    NOT NULL,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS itens_venda (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    venda_id        INTEGER NOT NULL,
                    produto_id      INTEGER NOT NULL,
                    nome_produto    TEXT    NOT NULL,
                    quantidade      INTEGER NOT NULL,
                    preco_unitario  REAL    NOT NULL,
                    categoria       TEXT    NOT NULL DEFAULT '',
                    FOREIGN KEY (venda_id)   REFERENCES vendas(id),
                    FOREIGN KEY (produto_id) REFERENCES produtos(id)
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inicializar banco de dados: " + e.getMessage(), e);
        }
    }
}
