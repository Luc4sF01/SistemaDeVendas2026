package ui;

import model.Venda;
import service.VendaServico;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Relatórios completos em abas:
 *  1. Resumo financeiro
 *  2. Produtos mais vendidos
 *  3. Receita por categoria
 *  4. Vendas por período
 */
public class PainelRelatorios extends JPanel {
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_D  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VendaServico vendaServico;

    // Aba 1 – Resumo
    private final JLabel lblTotalGeral;
    private final JLabel lblTotalHoje;
    private final JLabel lblQtdVendas;
    private final JLabel lblTicketMedio;
    private final DefaultTableModel modeloPagamentos;

    // Aba 2 – Mais vendidos
    private final DefaultTableModel modeloMaisVendidos;

    // Aba 3 – Por categoria
    private final DefaultTableModel modeloCategoria;

    // Aba 4 – Por período
    private final JSpinner spinnerInicio;
    private final JSpinner spinnerFim;
    private final DefaultTableModel modeloPeriodo;
    private final JLabel lblTotalPeriodo;

    public PainelRelatorios(VendaServico vendaServico) {
        this.vendaServico = vendaServico;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane abas = new JTabbedPane();
        abas.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // ═══════════════════════════════════════════
        // ABA 1 — RESUMO
        // ═══════════════════════════════════════════
        lblTotalGeral  = new JLabel("R$ 0,00");
        lblTotalHoje   = new JLabel("R$ 0,00");
        lblQtdVendas   = new JLabel("0");
        lblTicketMedio = new JLabel("R$ 0,00");

        JPanel abaResumo = new JPanel(new BorderLayout(10, 10));
        abaResumo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel cards = new JPanel(new GridLayout(2, 2, 12, 12));
        cards.add(cardMetrica("💰  Total Geral",      lblTotalGeral,  new Color(0, 130, 60)));
        cards.add(cardMetrica("📅  Total Hoje",        lblTotalHoje,   new Color(0, 100, 200)));
        cards.add(cardMetrica("🛒  Qtd. de Vendas",    lblQtdVendas,   new Color(120, 60, 180)));
        cards.add(cardMetrica("📊  Ticket Médio",      lblTicketMedio, new Color(180, 100, 0)));

        String[] colPag = {"Forma de Pagamento", "Qtd. Vendas", "Total (R$)", "% do Total"};
        modeloPagamentos = new DefaultTableModel(colPag, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaPag = novaTabela(modeloPagamentos);
        JPanel painelPag = new JPanel(new BorderLayout(4, 4));
        painelPag.setBorder(BorderFactory.createTitledBorder("Receita por forma de pagamento"));
        painelPag.add(new JScrollPane(tabelaPag));

        abaResumo.add(cards, BorderLayout.NORTH);
        abaResumo.add(painelPag, BorderLayout.CENTER);

        // ═══════════════════════════════════════════
        // ABA 2 — PRODUTOS MAIS VENDIDOS
        // ═══════════════════════════════════════════
        String[] colMV = {"Pos.", "Produto", "Qtd. Vendida", "Receita (R$)"};
        modeloMaisVendidos = new DefaultTableModel(colMV, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaMV = novaTabela(modeloMaisVendidos);
        tabelaMV.getColumnModel().getColumn(0).setMaxWidth(50);
        tabelaMV.getColumnModel().getColumn(2).setMaxWidth(120);

        JPanel abaMV = new JPanel(new BorderLayout(8, 8));
        abaMV.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel subtituloMV = new JLabel("Ranking de produtos por quantidade vendida");
        subtituloMV.setFont(subtituloMV.getFont().deriveFont(Font.ITALIC));
        subtituloMV.setForeground(Color.GRAY);
        abaMV.add(subtituloMV, BorderLayout.NORTH);
        abaMV.add(new JScrollPane(tabelaMV), BorderLayout.CENTER);

        // ═══════════════════════════════════════════
        // ABA 3 — POR CATEGORIA
        // ═══════════════════════════════════════════
        String[] colCat = {"Categoria", "Qtd. Itens Vendidos", "Receita (R$)", "% da Receita"};
        modeloCategoria = new DefaultTableModel(colCat, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaCat = novaTabela(modeloCategoria);

        JPanel abaCat = new JPanel(new BorderLayout(8, 8));
        abaCat.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel subtituloCat = new JLabel("Receita total agrupada por categoria de produto");
        subtituloCat.setFont(subtituloCat.getFont().deriveFont(Font.ITALIC));
        subtituloCat.setForeground(Color.GRAY);
        abaCat.add(subtituloCat, BorderLayout.NORTH);
        abaCat.add(new JScrollPane(tabelaCat), BorderLayout.CENTER);

        // ═══════════════════════════════════════════
        // ABA 4 — POR PERÍODO
        // ═══════════════════════════════════════════
        spinnerInicio = criarSpinnerData(LocalDate.now().withDayOfMonth(1));
        spinnerFim    = criarSpinnerData(LocalDate.now());
        lblTotalPeriodo = new JLabel("R$ 0,00");
        lblTotalPeriodo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalPeriodo.setForeground(new Color(0, 120, 60));

        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.setBackground(new Color(0, 100, 200));
        btnFiltrar.setForeground(Color.WHITE);
        btnFiltrar.setFocusPainted(false);
        btnFiltrar.addActionListener(e -> atualizarPeriodo());

        JPanel painelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        painelFiltro.add(new JLabel("De:"));
        painelFiltro.add(spinnerInicio);
        painelFiltro.add(new JLabel("Até:"));
        painelFiltro.add(spinnerFim);
        painelFiltro.add(btnFiltrar);
        painelFiltro.add(Box.createHorizontalStrut(20));
        painelFiltro.add(new JLabel("Total no período:"));
        painelFiltro.add(lblTotalPeriodo);

        String[] colPeriodo = {"#", "Data/Hora", "Cliente", "Pagamento", "Total (R$)"};
        modeloPeriodo = new DefaultTableModel(colPeriodo, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaPeriodo = novaTabela(modeloPeriodo);
        tabelaPeriodo.getColumnModel().getColumn(0).setMaxWidth(50);
        tabelaPeriodo.getColumnModel().getColumn(4).setMaxWidth(110);

        JPanel abaPeriodo = new JPanel(new BorderLayout(8, 8));
        abaPeriodo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        abaPeriodo.add(painelFiltro, BorderLayout.NORTH);
        abaPeriodo.add(new JScrollPane(tabelaPeriodo), BorderLayout.CENTER);

        // ═══════════════════════════════════════════
        // Botão global Atualizar
        // ═══════════════════════════════════════════
        JButton btnAtualizar = new JButton("⟳  Atualizar Tudo");
        btnAtualizar.addActionListener(e -> atualizar());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnAtualizar);

        abas.addTab("  Resumo  ", abaResumo);
        abas.addTab("  Mais Vendidos  ", abaMV);
        abas.addTab("  Por Categoria  ", abaCat);
        abas.addTab("  Por Período  ", abaPeriodo);

        add(abas, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    public void atualizar() {
        List<Venda> todas = vendaServico.listarTodas();
        double totalGeral = todas.stream().mapToDouble(Venda::getTotal).sum();

        // ── Aba 1: Resumo ──
        lblTotalGeral.setText(String.format("R$ %.2f", totalGeral));
        lblTotalHoje.setText(String.format("R$ %.2f", vendaServico.totalVendasHoje()));
        lblQtdVendas.setText(String.valueOf(todas.size()));
        lblTicketMedio.setText(String.format("R$ %.2f", vendaServico.ticketMedio()));

        modeloPagamentos.setRowCount(0);
        Map<String, List<Venda>> porPag = todas.stream()
                .collect(Collectors.groupingBy(v -> v.getFormaPagamento().getDescricao()));
        porPag.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, List<Venda>> e) ->
                        e.getValue().stream().mapToDouble(Venda::getTotal).sum()).reversed())
                .forEach(e -> {
                    double sub = e.getValue().stream().mapToDouble(Venda::getTotal).sum();
                    double pct = totalGeral > 0 ? (sub / totalGeral * 100) : 0;
                    modeloPagamentos.addRow(new Object[]{
                            e.getKey(), e.getValue().size(),
                            String.format("%.2f", sub),
                            String.format("%.1f%%", pct)
                    });
                });

        // ── Aba 2: Mais vendidos ──
        modeloMaisVendidos.setRowCount(0);
        Map<String, Integer> qtdPorProduto = vendaServico.produtosMaisVendidos();
        Map<String, Double> receitaPorProduto = new LinkedHashMap<>();
        todas.stream().flatMap(v -> v.getItens().stream())
                .forEach(i -> receitaPorProduto.merge(i.getProduto().getNome(), i.getSubtotal(), Double::sum));
        int pos = 1;
        for (Map.Entry<String, Integer> e : qtdPorProduto.entrySet()) {
            modeloMaisVendidos.addRow(new Object[]{
                    pos++, e.getKey(), e.getValue(),
                    String.format("%.2f", receitaPorProduto.getOrDefault(e.getKey(), 0.0))
            });
        }

        // ── Aba 3: Por categoria ──
        modeloCategoria.setRowCount(0);
        Map<String, Double> recCat = vendaServico.receitaPorCategoria();
        Map<String, Long> qtdCat = new LinkedHashMap<>();
        todas.stream().flatMap(v -> v.getItens().stream())
                .forEach(i -> qtdCat.merge(i.getProduto().getCategoria(), (long) i.getQuantidade(), Long::sum));
        recCat.forEach((cat, receita) -> {
            double pct = totalGeral > 0 ? (receita / totalGeral * 100) : 0;
            modeloCategoria.addRow(new Object[]{
                    cat, qtdCat.getOrDefault(cat, 0L),
                    String.format("%.2f", receita),
                    String.format("%.1f%%", pct)
            });
        });

        // Aba 4: atualizar com datas atuais
        atualizarPeriodo();
    }

    private void atualizarPeriodo() {
        LocalDate inicio = ((SpinnerDateModel) spinnerInicio.getModel())
                .getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate fim = ((SpinnerDateModel) spinnerFim.getModel())
                .getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        modeloPeriodo.setRowCount(0);
        List<Venda> filtradas = vendaServico.listarTodas().stream()
                .filter(v -> {
                    LocalDate d = v.getDataHora().toLocalDate();
                    return !d.isBefore(inicio) && !d.isAfter(fim);
                })
                .sorted(Comparator.comparing(Venda::getDataHora).reversed())
                .toList();

        double totalPeriodo = 0;
        for (Venda v : filtradas) {
            String cliente = v.getCliente() != null ? v.getCliente().getNome() : "Avulso";
            modeloPeriodo.addRow(new Object[]{
                    v.getId(), v.getDataHora().format(FMT_DT), cliente,
                    v.getFormaPagamento().getDescricao(),
                    String.format("%.2f", v.getTotal())
            });
            totalPeriodo += v.getTotal();
        }
        lblTotalPeriodo.setText(String.format("R$ %.2f", totalPeriodo));
    }

    // ── Helpers ──

    private JTable novaTabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(24);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    private JPanel cardMetrica(String titulo, JLabel valor, Color cor) {
        valor.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valor.setForeground(cor);
        valor.setAlignmentX(CENTER_ALIGNMENT);
        JLabel lblT = new JLabel(titulo, SwingConstants.CENTER);
        lblT.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblT.setAlignmentX(CENTER_ALIGNMENT);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 2, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        p.add(Box.createVerticalGlue());
        p.add(lblT);
        p.add(Box.createVerticalStrut(6));
        p.add(valor);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JSpinner criarSpinnerData(LocalDate data) {
        java.util.Date d = java.util.Date.from(data.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        SpinnerDateModel model = new SpinnerDateModel(d, null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(110, 26));
        return spinner;
    }
}
