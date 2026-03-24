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
                    id        SERIAL PRIMARY KEY,
                    nome      VARCHAR(255) NOT NULL,
                    telefone  VARCHAR(255) NOT NULL,
                    email     VARCHAR(255) NOT NULL DEFAULT ''
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS produtos (
                    id                  SERIAL PRIMARY KEY,
                    nome                VARCHAR(255) NOT NULL,
                    preco               NUMERIC(10,2) NOT NULL,
                    quantidade_estoque  INTEGER       NOT NULL DEFAULT 0,
                    categoria           VARCHAR(255)  NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vendas (
                    id               SERIAL PRIMARY KEY,
                    cliente_id       INTEGER,
                    forma_pagamento  VARCHAR(255)  NOT NULL,
                    data_hora        TIMESTAMP     NOT NULL,
                    subtotal         NUMERIC(10,2) NOT NULL,
                    desconto         NUMERIC(10,2) NOT NULL,
                    total            NUMERIC(10,2) NOT NULL,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS itens_venda (
                    id              SERIAL PRIMARY KEY,
                    venda_id        INTEGER       NOT NULL,
                    produto_id      INTEGER       NOT NULL,
                    nome_produto    VARCHAR(255)  NOT NULL,
                    quantidade      INTEGER       NOT NULL,
                    preco_unitario  NUMERIC(10,2) NOT NULL,
                    categoria       VARCHAR(255)  NOT NULL DEFAULT '',
                    FOREIGN KEY (venda_id)   REFERENCES vendas(id),
                    FOREIGN KEY (produto_id) REFERENCES produtos(id)
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inicializar banco de dados: " + e.getMessage(), e);
        }
    }
}
