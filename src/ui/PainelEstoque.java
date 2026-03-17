package ui;

import model.Produto;
import service.ProdutoServico;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Painel dedicado ao controle de estoque.
 * Mostra todos os produtos com cores indicando nível crítico,
 * e permite entradas rápidas de mercadoria.
 */
public class PainelEstoque extends JPanel {
    private static final String[] COLUNAS = {"ID", "Nome", "Categoria", "Preço (R$)", "Estoque", "Situação"};
    private static final String[] CATEGORIAS = {"Todas", "Ração", "Brinquedo", "Higiene", "Medicamento", "Acessório", "Outro"};
    private static final int CRITICO = 3;
    private static final int BAIXO = 10;

    private final ProdutoServico servico;
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private final JComboBox<String> cbCategoria;
    private final JLabel lblTotal;
    private final JLabel lblCritico;
    private final JLabel lblBaixo;

    public PainelEstoque(ProdutoServico servico) {
        this.servico = servico;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Resumo no topo ──
        lblTotal   = new JLabel("0");
        lblCritico = new JLabel("0");
        lblBaixo   = new JLabel("0");
        JPanel painelResumo = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 4));
        painelResumo.setBorder(BorderFactory.createTitledBorder("Resumo"));
        painelResumo.add(cartao("Produtos cadastrados", lblTotal,   new Color(60, 100, 200)));
        painelResumo.add(cartao("Estoque crítico (≤ " + CRITICO + ")", lblCritico, new Color(200, 40, 40)));
        painelResumo.add(cartao("Estoque baixo (≤ " + BAIXO + ")",  lblBaixo,   new Color(200, 130, 0)));

        // ── Filtro ──
        cbCategoria = new JComboBox<>(CATEGORIAS);
        JButton btnAtualizar = new JButton("⟳  Atualizar");
        JPanel painelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        painelFiltro.add(new JLabel("Filtrar categoria:"));
        painelFiltro.add(cbCategoria);
        painelFiltro.add(btnAtualizar);

        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.add(painelResumo, BorderLayout.NORTH);
        painelTopo.add(painelFiltro, BorderLayout.SOUTH);

        // ── Tabela ──
        modelo = new DefaultTableModel(COLUNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(26);
        tabela.getTableHeader().setFont(tabela.getTableHeader().getFont().deriveFont(Font.BOLD));
        tabela.getColumnModel().getColumn(0).setMaxWidth(45);
        tabela.getColumnModel().getColumn(3).setMaxWidth(110);
        tabela.getColumnModel().getColumn(4).setMaxWidth(80);
        tabela.getColumnModel().getColumn(5).setMinWidth(100);

        // Renderer de cores
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    int est = (int) modelo.getValueAt(row, 4);
                    if (est == 0)        c.setBackground(new Color(255, 160, 160));
                    else if (est <= CRITICO) c.setBackground(new Color(255, 190, 190));
                    else if (est <= BAIXO)   c.setBackground(new Color(255, 235, 150));
                    else                 c.setBackground(new Color(230, 255, 230));
                }
                return c;
            }
        };
        for (int i = 0; i < COLUNAS.length; i++) tabela.getColumnModel().getColumn(i).setCellRenderer(renderer);

        // ── Botões de ação ──
        JButton btnEntrada  = criarBotao("↑  Entrada de Estoque", new Color(0, 120, 60));
        JButton btnAjustar  = criarBotao("✎  Ajustar Quantidade", new Color(0, 100, 200));
        btnEntrada.setEnabled(false);
        btnAjustar.setEnabled(false);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        // Legenda
        JPanel legenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legenda.add(quadrado(new Color(255, 160, 160)));  legenda.add(new JLabel("Zerado"));
        legenda.add(quadrado(new Color(255, 190, 190)));  legenda.add(new JLabel("Crítico (≤ " + CRITICO + ")"));
        legenda.add(quadrado(new Color(255, 235, 150)));  legenda.add(new JLabel("Baixo (≤ " + BAIXO + ")"));
        legenda.add(quadrado(new Color(230, 255, 230)));  legenda.add(new JLabel("OK"));

        painelBotoes.add(btnEntrada);
        painelBotoes.add(btnAjustar);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(legenda, BorderLayout.WEST);
        rodape.add(painelBotoes, BorderLayout.EAST);

        add(painelTopo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);

        // ── Eventos ──
        tabela.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = tabela.getSelectedRow() >= 0;
            btnEntrada.setEnabled(sel);
            btnAjustar.setEnabled(sel);
        });

        cbCategoria.addActionListener(e -> carregarTabela());
        btnAtualizar.addActionListener(e -> carregarTabela());

        btnEntrada.addActionListener(e -> {
            Produto p = getProdutoSelecionado();
            if (p != null) abrirEntradaEstoque(p);
        });

        btnAjustar.addActionListener(e -> {
            Produto p = getProdutoSelecionado();
            if (p != null) abrirAjusteEstoque(p);
        });

        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        String catFiltro = cbCategoria.getSelectedIndex() == 0 ? "" : (String) cbCategoria.getSelectedItem();
        List<Produto> lista = servico.listarTodos();

        int criticos = 0, baixos = 0;
        for (Produto p : lista) {
            if (catFiltro.isEmpty() || p.getCategoria().equals(catFiltro)) {
                String situacao;
                int est = p.getQuantidadeEstoque();
                if (est == 0)          situacao = "⛔ ZERADO";
                else if (est <= CRITICO) situacao = "🔴 Crítico";
                else if (est <= BAIXO)   situacao = "🟡 Baixo";
                else                   situacao = "🟢 OK";
                modelo.addRow(new Object[]{p.getId(), p.getNome(), p.getCategoria(),
                        String.format("%.2f", p.getPreco()), est, situacao});
            }
            if (p.getQuantidadeEstoque() <= CRITICO) criticos++;
            if (p.getQuantidadeEstoque() <= BAIXO)   baixos++;
        }
        lblTotal.setText(String.valueOf(lista.size()));
        lblCritico.setText(String.valueOf(criticos));
        lblBaixo.setText(String.valueOf(baixos));
    }

    private Produto getProdutoSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) return null;
        return servico.buscarPorId((int) modelo.getValueAt(linha, 0)).orElse(null);
    }

    private void abrirEntradaEstoque(Produto p) {
        JTextField fQtd = new JTextField("1", 6);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5); gc.anchor = GridBagConstraints.WEST;

        addLinha(form, gc, 0, "Produto:", lbl(p.getNome()));
        addLinha(form, gc, 1, "Estoque atual:", lbl(String.valueOf(p.getQuantidadeEstoque())));
        addLinha(form, gc, 2, "Unidades a adicionar:", fQtd);

        int res = JOptionPane.showConfirmDialog(this, form, "Entrada de Estoque",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            int qtd = Integer.parseInt(fQtd.getText().trim());
            if (qtd <= 0) throw new NumberFormatException();
            int novoEstoque = p.getQuantidadeEstoque() + qtd;
            servico.atualizarEstoque(p.getId(), novoEstoque);
            JOptionPane.showMessageDialog(this,
                    String.format("✔  %s\nEstoque anterior: %d\nAdicionado: +%d\nNovo total: %d",
                            p.getNome(), p.getQuantidadeEstoque() - qtd, qtd, novoEstoque),
                    "Entrada Registrada", JOptionPane.INFORMATION_MESSAGE);
            carregarTabela();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirAjusteEstoque(Produto p) {
        JTextField fQtd = new JTextField(String.valueOf(p.getQuantidadeEstoque()), 6);
        JTextField fMotivo = new JTextField(20);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5); gc.anchor = GridBagConstraints.WEST;

        addLinha(form, gc, 0, "Produto:", lbl(p.getNome()));
        addLinha(form, gc, 1, "Estoque atual:", lbl(String.valueOf(p.getQuantidadeEstoque())));
        addLinha(form, gc, 2, "Novo valor de estoque:", fQtd);
        addLinha(form, gc, 3, "Motivo do ajuste:", fMotivo);

        int res = JOptionPane.showConfirmDialog(this, form, "Ajuste de Estoque",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            int novoEst = Integer.parseInt(fQtd.getText().trim());
            if (novoEst < 0) throw new NumberFormatException();
            servico.atualizarEstoque(p.getId(), novoEst);
            carregarTabela();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers de UI ──

    private JPanel cartao(String titulo, JLabel valor, Color cor) {
        valor.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valor.setForeground(cor);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor.darker(), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        lblTitulo.setAlignmentX(CENTER_ALIGNMENT);
        valor.setAlignmentX(CENTER_ALIGNMENT);
        c.add(lblTitulo);
        c.add(valor);
        return c;
    }

    private void addLinha(JPanel p, GridBagConstraints gc, int row, String label, JComponent campo) {
        gc.gridx = 0; gc.gridy = row; gc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        p.add(campo, gc);
    }

    private JLabel lbl(String t) { JLabel l = new JLabel(t); l.setFont(l.getFont().deriveFont(Font.BOLD)); return l; }

    private JButton criarBotao(String t, Color cor) {
        JButton b = new JButton(t); b.setBackground(cor); b.setForeground(Color.WHITE); b.setFocusPainted(false); return b;
    }

    private JLabel quadrado(Color cor) {
        JLabel l = new JLabel("  "); l.setOpaque(true); l.setBackground(cor);
        l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        l.setPreferredSize(new Dimension(14, 14)); return l;
    }

    public void atualizar() { carregarTabela(); }
}
