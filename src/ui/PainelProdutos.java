package ui;

import model.Produto;
import service.ProdutoServico;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PainelProdutos extends JPanel {
    private static final String[] COLUNAS    = {"ID", "Nome", "Preço (R$)", "Estoque", "Categoria"};
    private static final String[] CATEGORIAS = {"Ração","Brinquedo","Higiene","Medicamento","Acessório","Outro"};
    private static final int ESTOQUE_BAIXO   = 5;

    private final ProdutoServico   servico;
    private final DefaultTableModel modelo;
    private final JTable            tabela;
    private final JTextField        campoBusca;
    private final JComboBox<String> cbCategoria;

    public PainelProdutos(ProdutoServico servico) {
        this.servico = servico;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Linha 1: botão Novo bem visível ──
        JButton btnNovo = UiUtil.botao("＋  Novo Produto", new Color(34, 139, 34));
        btnNovo.setFont(btnNovo.getFont().deriveFont(Font.BOLD, 13f));
        btnNovo.setPreferredSize(new Dimension(170, 32));

        JPanel linhaNovo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        linhaNovo.add(btnNovo);

        // ── Linha 2: busca + filtro ──
        campoBusca  = new JTextField(18);
        cbCategoria = new JComboBox<>();
        cbCategoria.addItem("Todas as categorias");
        for (String c : CATEGORIAS) cbCategoria.addItem(c);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnLimpar = new JButton("Limpar");

        JPanel linhaBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        linhaBusca.add(new JLabel("Buscar:"));
        linhaBusca.add(campoBusca);
        linhaBusca.add(btnBuscar);
        linhaBusca.add(Box.createHorizontalStrut(10));
        linhaBusca.add(new JLabel("Categoria:"));
        linhaBusca.add(cbCategoria);
        linhaBusca.add(btnLimpar);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(linhaNovo,  BorderLayout.NORTH);
        topo.add(linhaBusca, BorderLayout.SOUTH);

        // ── Tabela ──
        modelo = new DefaultTableModel(COLUNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(26);
        tabela.getTableHeader().setFont(tabela.getTableHeader().getFont().deriveFont(Font.BOLD));
        tabela.getColumnModel().getColumn(0).setMaxWidth(50);
        tabela.getColumnModel().getColumn(2).setMaxWidth(110);
        tabela.getColumnModel().getColumn(3).setMaxWidth(90);

        // Cores por nível de estoque
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    int est = (int) modelo.getValueAt(row, 3);
                    c.setBackground(est == 0         ? new Color(255, 180, 180)
                                  : est <= ESTOQUE_BAIXO ? new Color(255, 235, 170)
                                  : Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        // ── Botões de ação (habilitados ao selecionar linha) ──
        JButton btnEditar  = new JButton("✏  Editar");
        JButton btnEntrada = UiUtil.botao("↑  Entrada de Estoque", new Color(0, 100, 200));
        JButton btnRemover = UiUtil.botao("✕  Remover", new Color(180, 30, 30));
        btnEditar.setEnabled(false);
        btnEntrada.setEnabled(false);
        btnRemover.setEnabled(false);

        // Legenda
        JPanel legenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        legenda.add(quad(new Color(255, 180, 180))); legenda.add(new JLabel("Sem estoque"));
        legenda.add(quad(new Color(255, 235, 170))); legenda.add(new JLabel("Baixo (≤ " + ESTOQUE_BAIXO + ")"));
        legenda.add(quad(Color.WHITE));              legenda.add(new JLabel("Normal"));

        JPanel botoesAcao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        botoesAcao.add(btnEntrada);
        botoesAcao.add(btnEditar);
        botoesAcao.add(btnRemover);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(legenda,    BorderLayout.WEST);
        rodape.add(botoesAcao, BorderLayout.EAST);

        add(topo,                    BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(rodape,                  BorderLayout.SOUTH);

        // ── Eventos ──
        tabela.getSelectionModel().addListSelectionListener(e -> {
            boolean s = tabela.getSelectedRow() >= 0;
            btnEditar.setEnabled(s); btnEntrada.setEnabled(s); btnRemover.setEnabled(s);
        });

        btnBuscar.addActionListener(e -> filtrar());
        campoBusca.addActionListener(e -> filtrar());
        cbCategoria.addActionListener(e -> filtrar());
        btnLimpar.addActionListener(e -> { campoBusca.setText(""); cbCategoria.setSelectedIndex(0); filtrar(); });
        btnNovo.addActionListener(e -> { if (abrirDialogProduto(null)) filtrar(); });
        btnEditar.addActionListener(e -> { Produto p = selecionado(); if (p != null && abrirDialogProduto(p)) filtrar(); });
        btnEntrada.addActionListener(e -> { Produto p = selecionado(); if (p != null) abrirEntradaEstoque(p); });
        btnRemover.addActionListener(e -> {
            Produto p = selecionado();
            if (p == null) return;
            int r = JOptionPane.showConfirmDialog(this,
                    "Remover \"" + p.getNome() + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION) { servico.remover(p.getId()); filtrar(); }
        });

        filtrar();
    }

    private void filtrar() {
        String nome = campoBusca.getText().trim();
        String cat  = cbCategoria.getSelectedIndex() == 0 ? "" : (String) cbCategoria.getSelectedItem();
        modelo.setRowCount(0);
        servico.listarTodos().stream()
                .filter(p -> nome.isEmpty() || p.getNome().toLowerCase().contains(nome.toLowerCase()))
                .filter(p -> cat.isEmpty()  || p.getCategoria().equals(cat))
                .forEach(p -> modelo.addRow(new Object[]{
                        p.getId(), p.getNome(),
                        String.format("%.2f", p.getPreco()),
                        p.getQuantidadeEstoque(),
                        p.getCategoria()
                }));
    }

    private Produto selecionado() {
        int row = tabela.getSelectedRow();
        return row < 0 ? null : servico.buscarPorId((int) modelo.getValueAt(row, 0)).orElse(null);
    }

    private boolean abrirDialogProduto(Produto p) {
        JTextField fNome    = new JTextField(p != null ? p.getNome() : "", 22);
        JTextField fPreco   = new JTextField(p != null ? String.format("%.2f", p.getPreco()) : "", 10);
        JTextField fEstoque = new JTextField(p != null ? String.valueOf(p.getQuantidadeEstoque()) : "0", 6);
        JComboBox<String> cbCat = new JComboBox<>(CATEGORIAS);
        if (p != null) cbCat.setSelectedItem(p.getCategoria());

        JPanel form = form(new String[]{"Nome:", "Preço (R$):", "Estoque:", "Categoria:"},
                new JComponent[]{fNome, fPreco, fEstoque, cbCat});

        if (JOptionPane.showConfirmDialog(this, form, p == null ? "Novo Produto" : "Editar Produto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return false;
        try {
            String nome = fNome.getText().trim();
            double preco = Double.parseDouble(fPreco.getText().trim().replace(",", "."));
            int est = Integer.parseInt(fEstoque.getText().trim());
            String cat = (String) cbCat.getSelectedItem();
            if (p == null) { servico.cadastrar(nome, preco, est, cat); }
            else { servico.atualizarNome(p.getId(), nome); servico.atualizarPreco(p.getId(), preco);
                   servico.atualizarEstoque(p.getId(), est); p.setCategoria(cat); }
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void abrirEntradaEstoque(Produto p) {
        JTextField fQtd = new JTextField("1", 6);
        JPanel form = form(new String[]{"Produto:", "Estoque atual:", "Quantidade a adicionar:"},
                new JComponent[]{lbl(p.getNome()), lbl(String.valueOf(p.getQuantidadeEstoque())), fQtd});
        if (JOptionPane.showConfirmDialog(this, form, "Entrada de Estoque",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            int qtd = Integer.parseInt(fQtd.getText().trim());
            if (qtd <= 0) throw new NumberFormatException();
            servico.atualizarEstoque(p.getId(), p.getQuantidadeEstoque() + qtd);
            JOptionPane.showMessageDialog(this,
                    String.format("Novo estoque de \"%s\": %d", p.getNome(), p.getQuantidadeEstoque()),
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            filtrar();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel form(String[] labels, JComponent[] campos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5,5,5,5); gc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.fill = GridBagConstraints.NONE; p.add(new JLabel(labels[i]), gc);
            gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL;         p.add(campos[i], gc);
        }
        return p;
    }

    private JLabel lbl(String t) { JLabel l = new JLabel(t); l.setFont(l.getFont().deriveFont(Font.BOLD)); return l; }

    private JLabel quad(Color cor) {
        JLabel l = new JLabel("  "); l.setOpaque(true); l.setBackground(cor);
        l.setBorder(BorderFactory.createLineBorder(Color.GRAY)); l.setPreferredSize(new Dimension(14,14)); return l;
    }

    public void atualizar() { filtrar(); }
}
