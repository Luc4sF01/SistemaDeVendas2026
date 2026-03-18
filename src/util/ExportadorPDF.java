package util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import model.Venda;
import service.ProdutoServico;
import service.VendaServico;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gera um relatório financeiro completo em PDF.
 * Usa OpenPDF (LGPL) — livre para uso em produtos comerciais.
 */
public class ExportadorPDF {

    private static final Color COR_HEADER      = new Color(35, 70, 150);
    private static final Color COR_LINHA_PAR   = new Color(240, 245, 255);
    private static final Color COR_BORDA       = new Color(180, 200, 230);
    private static final Color COR_CINZA       = new Color(100, 100, 100);

    private static final Font F_TITULO     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  18, new Color(20, 50, 120));
    private static final Font F_SUBTITULO  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  12, new Color(35, 70, 150));
    private static final Font F_NORMAL     = FontFactory.getFont(FontFactory.HELVETICA,        9,  Color.BLACK);
    private static final Font F_NEGRITO    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9,  Color.BLACK);
    private static final Font F_HEADER_TAB = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9,  Color.WHITE);
    private static final Font F_RODAPE     = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, COR_CINZA);

    public static void exportarRelatorio(VendaServico vendaServico,
                                         ProdutoServico produtoServico,
                                         File destino) throws Exception {
        Document doc = new Document(PageSize.A4, 45, 45, 60, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        List<Venda> todas  = vendaServico.listarTodas();
        double totalGeral  = todas.stream().mapToDouble(Venda::getTotal).sum();
        String dataHoje    = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // ══════════════════════════════════════════
        // CABEÇALHO
        // ══════════════════════════════════════════
        Paragraph titulo = new Paragraph("VendasPET", F_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph subtitulo = new Paragraph("Relatório Financeiro — Gerado em " + dataHoje, F_RODAPE);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(12);
        doc.add(subtitulo);

        LineSeparator linha = new LineSeparator(1, 100, COR_HEADER, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(linha));
        doc.add(Chunk.NEWLINE);

        // ══════════════════════════════════════════
        // RESUMO FINANCEIRO
        // ══════════════════════════════════════════
        doc.add(secao("Resumo Financeiro"));

        PdfPTable tResumo = new PdfPTable(2);
        tResumo.setWidthPercentage(55);
        tResumo.setHorizontalAlignment(Element.ALIGN_LEFT);
        tResumo.setWidths(new float[]{55, 45});
        linhaResumo(tResumo, "Total Geral",     String.format("R$ %.2f", totalGeral),            false);
        linhaResumo(tResumo, "Total Hoje",      String.format("R$ %.2f", vendaServico.totalVendasHoje()), true);
        linhaResumo(tResumo, "Qtd. de Vendas",  String.valueOf(todas.size()),                     false);
        linhaResumo(tResumo, "Ticket Médio",    String.format("R$ %.2f", vendaServico.ticketMedio()), true);
        doc.add(tResumo);
        espaco(doc);

        // ══════════════════════════════════════════
        // RECEITA POR FORMA DE PAGAMENTO
        // ══════════════════════════════════════════
        doc.add(secao("Receita por Forma de Pagamento"));
        PdfPTable tPag = tabela(new String[]{"Forma de Pagamento", "Total (R$)", "% do Total"},
                                new float[]{55, 25, 20});
        boolean alt = false;
        for (Map.Entry<String, Double> e : vendaServico.totalPorFormaPagamento().entrySet()) {
            double pct = totalGeral > 0 ? e.getValue() / totalGeral * 100 : 0;
            linhaDados(tPag, alt, e.getKey(),
                    String.format("R$ %.2f", e.getValue()),
                    String.format("%.1f%%", pct));
            alt = !alt;
        }
        doc.add(tPag);
        espaco(doc);

        // ══════════════════════════════════════════
        // PRODUTOS MAIS VENDIDOS (top 10)
        // ══════════════════════════════════════════
        doc.add(secao("Produtos Mais Vendidos — Top 10"));

        Map<String, Double> receitaPorProduto = new LinkedHashMap<>();
        todas.stream().flatMap(v -> v.getItens().stream())
                .forEach(i -> receitaPorProduto.merge(
                        i.getProduto().getNome(), i.getSubtotal(), Double::sum));

        PdfPTable tMV = tabela(
                new String[]{"#", "Produto", "Qtd. Vendida", "Receita (R$)"},
                new float[]{8, 54, 18, 20});
        alt = false;
        int pos = 1;
        for (Map.Entry<String, Integer> e : vendaServico.produtosMaisVendidos().entrySet()) {
            if (pos > 10) break;
            linhaDados(tMV, alt,
                    String.valueOf(pos++), e.getKey(),
                    String.valueOf(e.getValue()),
                    String.format("R$ %.2f", receitaPorProduto.getOrDefault(e.getKey(), 0.0)));
            alt = !alt;
        }
        doc.add(tMV);
        espaco(doc);

        // ══════════════════════════════════════════
        // RECEITA POR CATEGORIA
        // ══════════════════════════════════════════
        doc.add(secao("Receita por Categoria"));

        Map<String, Long> qtdCat = new LinkedHashMap<>();
        todas.stream().flatMap(v -> v.getItens().stream())
                .forEach(i -> qtdCat.merge(i.getProduto().getCategoria(),
                        (long) i.getQuantidade(), Long::sum));

        PdfPTable tCat = tabela(
                new String[]{"Categoria", "Qtd. Itens Vendidos", "Receita (R$)", "% da Receita"},
                new float[]{30, 25, 25, 20});
        alt = false;
        for (Map.Entry<String, Double> e : vendaServico.receitaPorCategoria().entrySet()) {
            double pct = totalGeral > 0 ? e.getValue() / totalGeral * 100 : 0;
            linhaDados(tCat, alt,
                    e.getKey(),
                    String.valueOf(qtdCat.getOrDefault(e.getKey(), 0L)),
                    String.format("R$ %.2f", e.getValue()),
                    String.format("%.1f%%", pct));
            alt = !alt;
        }
        doc.add(tCat);
        espaco(doc);

        // ══════════════════════════════════════════
        // ESTOQUE ATUAL
        // ══════════════════════════════════════════
        doc.add(secao("Estoque Atual"));

        PdfPTable tEst = tabela(
                new String[]{"Produto", "Categoria", "Preço (R$)", "Qtd.", "Situação"},
                new float[]{38, 22, 16, 10, 14});
        alt = false;
        for (model.Produto p : produtoServico.listarTodos()) {
            int q = p.getQuantidadeEstoque();
            String sit = q == 0 ? "ZERADO" : q <= 3 ? "Crítico" : q <= 10 ? "Baixo" : "OK";
            Color bgLinha = alt ? COR_LINHA_PAR : Color.WHITE;

            // Coloração especial para situação crítica
            PdfPCell cSit = new PdfPCell(new Phrase(sit, q == 0 ? F_NEGRITO : F_NORMAL));
            cSit.setBackgroundColor(q == 0 ? new Color(255, 180, 180)
                    : q <= 3  ? new Color(255, 200, 200)
                    : q <= 10 ? new Color(255, 240, 170)
                    :           bgLinha);
            cSit.setPadding(4); cSit.setBorderColor(COR_BORDA);

            PdfPCell[] celulas = {
                    celula(p.getNome(), F_NORMAL, bgLinha),
                    celula(p.getCategoria(), F_NORMAL, bgLinha),
                    celula(String.format("%.2f", p.getPreco()), F_NORMAL, bgLinha),
                    celula(String.valueOf(q), F_NORMAL, bgLinha),
                    cSit
            };
            for (PdfPCell c : celulas) tEst.addCell(c);
            alt = !alt;
        }
        doc.add(tEst);

        doc.close();
    }

    // ── Helpers privados ──

    private static Paragraph secao(String texto) {
        Paragraph p = new Paragraph(texto, F_SUBTITULO);
        p.setSpacingBefore(6);
        p.setSpacingAfter(4);
        return p;
    }

    private static void espaco(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
    }

    private static PdfPTable tabela(String[] colunas, float[] larguras) throws DocumentException {
        PdfPTable t = new PdfPTable(colunas.length);
        t.setWidthPercentage(100);
        t.setWidths(larguras);
        for (String col : colunas) {
            PdfPCell c = new PdfPCell(new Phrase(col, F_HEADER_TAB));
            c.setBackgroundColor(COR_HEADER);
            c.setPadding(5);
            c.setBorderColor(COR_HEADER);
            t.addCell(c);
        }
        return t;
    }

    private static void linhaDados(PdfPTable t, boolean alt, String... valores) {
        Color bg = alt ? COR_LINHA_PAR : Color.WHITE;
        for (String v : valores) t.addCell(celula(v, F_NORMAL, bg));
    }

    private static void linhaResumo(PdfPTable t, String chave, String valor, boolean alt) {
        t.addCell(celula(chave, F_NORMAL,  alt ? COR_LINHA_PAR : Color.WHITE));
        t.addCell(celula(valor, F_NEGRITO, alt ? COR_LINHA_PAR : Color.WHITE));
    }

    private static PdfPCell celula(String texto, Font fonte, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setBorderColor(COR_BORDA);
        return c;
    }
}
