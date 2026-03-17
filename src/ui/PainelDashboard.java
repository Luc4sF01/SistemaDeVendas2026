package ui;

import model.Produto;
import model.Venda;
import service.ClienteServico;
import service.ProdutoServico;
import service.VendaServico;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PainelDashboard extends JPanel {

    private static final DateTimeFormatter FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy",
            new java.util.Locale("pt", "BR"));
    private static final int LIMITE_ALERTA = 10;
    private static final String[] CATEGORIAS = {"Ração","Brinquedo","Higiene","Medicamento","Acessório","Outro"};

    private final VendaServico    vendaServico;
    private final ProdutoServico  produtoServico;
    private final ClienteServico  clienteServico;

    private final JLabel lblTotalHoje;
    private final JLabel lblTotalGeral;
    private final JLabel lblQtdVendas;
    private final JLabel lblTicketMedio;
    private final DefaultTableModel modeloVendas;
    private final DefaultTableModel modeloEstoque;

    public PainelDashboard(VendaServico vendaServico, ProdutoServico produtoServico, ClienteServico clienteServico) {
        this.vendaServico   = vendaServico;
        this.produtoServico = produtoServico;
        this.clienteServico = clienteServico;

        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        setBackground(new Color(245, 247, 250));

        // ══════════════════════════════════════════
        // CABEÇALHO
        // ══════════════════════════════════════════
        JLabel lblNome = new JLabel("🐾  PetShop");
        lblNome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblNome.setForeground(new Color(30, 60, 140));

        JLabel lblData = new JLabel(LocalDate.now().format(FMT_DT));
        lblData.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblData.setForeground(new Color(120, 120, 120));

        JButton btnAtualizar = UiUtil.botao("⟳  Atualizar", new Color(90, 110, 150));

        JPanel cabLeft = new JPanel(new BorderLayout(0, 2));
        cabLeft.setOpaque(false);
        cabLeft.add(lblNome, BorderLayout.NORTH);
        cabLeft.add(lblData, BorderLayout.SOUTH);

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 215, 225)));
        cabecalho.add(cabLeft, BorderLayout.WEST);
        cabecalho.add(btnAtualizar, BorderLayout.EAST);

        // ══════════════════════════════════════════
        // AÇÕES RÁPIDAS
        // ══════════════════════════════════════════
        JPanel painelAcoes = new JPanel(new GridLayout(1, 3, 12, 0));
        painelAcoes.setOpaque(false);
        painelAcoes.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(0,0,0,0), "  Ações Rápidas",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(80, 80, 80)));

        painelAcoes.add(UiUtil.cardAcao("🛒", "NOVA VENDA",
                "Registrar uma venda", new Color(34, 130, 60), this::abrirNovaVenda));
        painelAcoes.add(UiUtil.cardAcao("📦", "NOVO PRODUTO",
                "Cadastrar produto no sistema", new Color(0, 100, 200), this::abrirNovoProduto));
        painelAcoes.add(UiUtil.cardAcao("📥", "ENTRADA DE ESTOQUE",
                "Repor unidades de um produto", new Color(160, 90, 0), this::abrirEntradaEstoque));

        // ══════════════════════════════════════════
        // MÉTRICAS
        // ══════════════════════════════════════════
        lblTotalHoje   = new JLabel("R$ 0,00");
        lblTotalGeral  = new JLabel("R$ 0,00");
        lblQtdVendas   = new JLabel("0");
        lblTicketMedio = new JLabel("R$ 0,00");

        JPanel painelMetricas = new JPanel(new GridLayout(1, 4, 12, 0));
        painelMetricas.setOpaque(false);
        painelMetricas.add(UiUtil.cardMetrica("Total Hoje",        lblTotalHoje,   new Color(34, 130, 60)));
        painelMetricas.add(UiUtil.cardMetrica("Total Geral",       lblTotalGeral,  new Color(0, 100, 200)));
        painelMetricas.add(UiUtil.cardMetrica("Vendas Realizadas", lblQtdVendas,   new Color(120, 50, 180)));
        painelMetricas.add(UiUtil.cardMetrica("Ticket Médio",      lblTicketMedio, new Color(180, 100, 0)));

        // ══════════════════════════════════════════
        // TABELAS
        // ══════════════════════════════════════════
        String[] colV = {"#", "Data/Hora", "Cliente", "Total (R$)", "Pagamento"};
        modeloVendas = new DefaultTableModel(colV, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tVendas = novaTabela(modeloVendas);
        tVendas.getColumnModel().getColumn(0).setMaxWidth(40);
        tVendas.getColumnModel().getColumn(3).setMaxWidth(100);
        JPanel pVendas = painelTabela("📋  Últimas Vendas", tVendas);

        String[] colE = {"ID", "Produto", "Estoque", "Categoria"};
        modeloEstoque = new DefaultTableModel(colE, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tEstoque = novaTabela(modeloEstoque);
        tEstoque.getColumnModel().getColumn(0).setMaxWidth(40);
        tEstoque.getColumnModel().getColumn(2).setMaxWidth(65);
        JPanel pEstoque = painelTabela("⚠️  Estoque Baixo (≤ " + LIMITE_ALERTA + ")", tEstoque);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pVendas, pEstoque);
        split.setResizeWeight(0.6);
        split.setDividerLocation(560);
        split.setBorder(null);
        split.setOpaque(false);

        // ══════════════════════════════════════════
        // MONTAGEM
        // ══════════════════════════════════════════
        JPanel topSection = new JPanel(new GridLayout(2, 1, 0, 12));
        topSection.setOpaque(false);
        topSection.add(painelAcoes);
        topSection.add(painelMetricas);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(topSection, BorderLayout.NORTH);
        centro.add(split,      BorderLayout.CENTER);

        add(cabecalho, BorderLayout.NORTH);
        add(centro,    BorderLayout.CENTER);

        btnAtualizar.addActionListener(e -> atualizar());
        atualizar();
    }

    // ══════════════════════════════════════════
    // AÇÕES
    // ══════════════════════════════════════════

    private void abrirNovaVenda() {
        DialogNovaVenda d = new DialogNovaVenda(
                SwingUtilities.getWindowAncestor(this), vendaServico, produtoServico, clienteServico);
        d.setVisible(true);
        if (d.isVendaFinalizada()) atualizar();
    }

    private void abrirNovoProduto() {
        JTextField fNome    = new JTextField(22);
        JTextField fPreco   = new JTextField(10);
        JTextField fEstoque = new JTextField("0", 6);
        JComboBox<String> cbCat = new JComboBox<>(CATEGORIAS);

        JPanel form = form(
                new String[]{"Nome:", "Preço (R$):", "Estoque inicial:", "Categoria:"},
                new JComponent[]{fNome, fPreco, fEstoque, cbCat});

        int res = JOptionPane.showConfirmDialog(this, form, "Novo Produto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            produtoServico.cadastrar(
                    fNome.getText().trim(),
                    Double.parseDouble(fPreco.getText().trim().replace(",", ".")),
                    Integer.parseInt(fEstoque.getText().trim()),
                    (String) cbCat.getSelectedItem());
            JOptionPane.showMessageDialog(this, "Produto cadastrado com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            atualizar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirEntradaEstoque() {
        List<Produto> produtos = produtoServico.listarTodos();
        if (produtos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum produto cadastrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] nomes = produtos.stream()
                .map(p -> p.getId() + " — " + p.getNome() + " (estoque: " + p.getQuantidadeEstoque() + ")")
                .toArray(String[]::new);
        JComboBox<String> cbProduto = new JComboBox<>(nomes);
        JTextField fQtd = new JTextField("1", 6);

        JPanel form = form(new String[]{"Produto:", "Quantidade a adicionar:"},
                new JComponent[]{cbProduto, fQtd});

        int res = JOptionPane.showConfirmDialog(this, form, "Entrada de Estoque",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            int qtd = Integer.parseInt(fQtd.getText().trim());
            if (qtd <= 0) throw new NumberFormatException();
            Produto p = produtos.get(cbProduto.getSelectedIndex());
            int novo = p.getQuantidadeEstoque() + qtd;
            produtoServico.atualizarEstoque(p.getId(), novo);
            JOptionPane.showMessageDialog(this,
                    String.format("%s\nAdicionado: +%d  →  Novo estoque: %d", p.getNome(), qtd, novo),
                    "Estoque Atualizado", JOptionPane.INFORMATION_MESSAGE);
            atualizar();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════
    // ATUALIZAÇÃO DE DADOS
    // ══════════════════════════════════════════

    public void atualizar() {
        lblTotalHoje.setText(String.format("R$ %.2f", vendaServico.totalVendasHoje()));
        lblTotalGeral.setText(String.format("R$ %.2f", vendaServico.totalVendas()));
        lblQtdVendas.setText(String.valueOf(vendaServico.listarTodas().size()));
        lblTicketMedio.setText(String.format("R$ %.2f", vendaServico.ticketMedio()));

        modeloVendas.setRowCount(0);
        List<Venda> todas = vendaServico.listarTodas();
        int de = Math.max(0, todas.size() - 10);
        for (int i = todas.size() - 1; i >= de; i--) {
            Venda v = todas.get(i);
            modeloVendas.addRow(new Object[]{
                    v.getId(), v.getDataHora().format(FMT),
                    v.getCliente() != null ? v.getCliente().getNome() : "Avulso",
                    String.format("%.2f", v.getTotal()),
                    v.getFormaPagamento().getDescricao()
            });
        }

        modeloEstoque.setRowCount(0);
        for (Produto p : vendaServico.relatorioEstoqueBaixo(LIMITE_ALERTA)) {
            modeloEstoque.addRow(new Object[]{
                    p.getId(), p.getNome(), p.getQuantidadeEstoque(), p.getCategoria()
            });
        }
    }

    // ══════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════

    private JTable novaTabela(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(220, 220, 220));
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    private JPanel painelTabela(String titulo, JTable tabela) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(60, 60, 80));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 215, 225)));
        p.add(lbl, BorderLayout.NORTH);
        p.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return p;
    }

    private JPanel form(String[] labels, JComponent[] campos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.fill = GridBagConstraints.NONE;
            p.add(new JLabel(labels[i]), gc);
            gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            p.add(campos[i], gc);
        }
        return p;
    }
}
