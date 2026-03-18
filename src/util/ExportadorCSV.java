package util;

import model.Produto;
import service.ProdutoServico;

import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exporta e importa dados em formato CSV compatível com Excel.
 * Usa ponto-e-vírgula como separador (padrão Excel pt-BR).
 */
public class ExportadorCSV {

    /**
     * Exporta qualquer DefaultTableModel para CSV.
     * Útil para exportar qualquer aba de relatório diretamente.
     */
    public static void exportarTabela(DefaultTableModel modelo, File destino) throws IOException {
        try (PrintWriter pw = nova(destino)) {
            int cols = modelo.getColumnCount();

            // Cabeçalho
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                if (c > 0) sb.append(';');
                sb.append(escapar(modelo.getColumnName(c)));
            }
            pw.println(sb);

            // Dados
            for (int r = 0; r < modelo.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < cols; c++) {
                    if (c > 0) sb.append(';');
                    Object v = modelo.getValueAt(r, c);
                    sb.append(escapar(v != null ? v.toString() : ""));
                }
                pw.println(sb);
            }
        }
    }

    /**
     * Exporta a lista completa de produtos para CSV.
     * Formato: nome;preco;estoque;categoria
     */
    public static void exportarEstoque(List<Produto> produtos, File destino) throws IOException {
        try (PrintWriter pw = nova(destino)) {
            pw.println("nome;preco;estoque;categoria");
            for (Produto p : produtos) {
                pw.printf("%s;%.2f;%d;%s%n",
                        escapar(p.getNome()),
                        p.getPreco(),
                        p.getQuantidadeEstoque(),
                        escapar(p.getCategoria()));
            }
        }
    }

    /**
     * Gera um CSV modelo para o usuário preencher e importar depois.
     */
    public static void gerarModelo(File destino) throws IOException {
        try (PrintWriter pw = nova(destino)) {
            pw.println("nome;preco;estoque;categoria");
            pw.println("Ração Premium 15kg;89.90;20;Ração");
            pw.println("Coleira Ajustável P;25.00;15;Acessório");
            pw.println("Shampoo Antipulgas;35.50;10;Higiene");
        }
    }

    /**
     * Importa produtos de um CSV.
     * - Se o produto (pelo nome exato) já existe: soma o estoque importado ao atual.
     * - Se não existe: cadastra como novo produto.
     * Retorna mensagem com o resultado da operação.
     */
    public static String importarEstoque(File origem, ProdutoServico servico) throws IOException {
        int criados = 0, atualizados = 0, erros = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(origem), StandardCharsets.UTF_8))) {

            String linha = br.readLine(); // pula cabeçalho
            if (linha == null) return "Arquivo vazio.";
            if (linha.startsWith("\uFEFF")) linha = linha.substring(1); // remove BOM

            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] p = linha.split(";", -1);
                if (p.length < 4) { erros++; continue; }

                try {
                    String nome      = p[0].trim().replace("\"", "");
                    double preco     = Double.parseDouble(p[1].trim().replace(',', '.'));
                    int    estoque   = Integer.parseInt(p[2].trim());
                    String categoria = p[3].trim().replace("\"", "");

                    if (nome.isEmpty()) { erros++; continue; }

                    // Verifica se já existe pelo nome exato
                    Produto existente = servico.buscarPorNome(nome).stream()
                            .filter(x -> x.getNome().equalsIgnoreCase(nome))
                            .findFirst().orElse(null);

                    if (existente != null) {
                        servico.atualizarEstoque(existente.getId(),
                                existente.getQuantidadeEstoque() + estoque);
                        atualizados++;
                    } else {
                        servico.cadastrar(nome, preco, estoque, categoria);
                        criados++;
                    }
                } catch (Exception e) {
                    erros++;
                }
            }
        }

        return String.format(
                "Importação concluída!\n\n" +
                "✔  %d produto(s) criado(s)\n" +
                "↑  %d estoque(s) atualizado(s)\n" +
                "✘  %d linha(s) ignorada(s) por erro",
                criados, atualizados, erros);
    }

    // ── Helpers ──

    private static PrintWriter nova(File destino) throws IOException {
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8));
        pw.print('\uFEFF'); // BOM para Excel reconhecer UTF-8
        return pw;
    }

    private static String escapar(String valor) {
        if (valor == null) return "";
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n"))
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        return valor;
    }
}
