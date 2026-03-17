package ui;

import model.*;
import service.ClienteServico;
import service.ProdutoServico;
import service.VendaServico;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DialogNovaVenda extends JDialog {

    private static final double PCT_DESCONTO = 0.05; // 5 %

    private final VendaServico   vendaServico;
    private final ProdutoServico produtoServico;

    private final DefaultTableModel modeloProdutos;
    private final DefaultTableModel modeloCarrinho;
    private final JTable            tabelaProdutos;
    private final JTextField        campoQtd;
    private final JComboBox<String>        cbCliente;
    private final JComboBox<FormaPagamento> cbPagamento;

    // Painel financeiro
    private final JLabel lblSubtotal   = new JLabel("R$ 0,00");
    private final JLabel lblDesconto   = new JLabel("R$ 0,00");
    private final JLabel lblTotal      = new JLabel("R$ 0,00");
    private final JLabel lblTagDesconto = new JLabel("Desconto (5%): −");
    private final JPanel painelDesconto;        // visível só em Dinheiro/PIX

    // Painel troco (só Dinheiro)
    private final JPanel     painelTroco;
    private final JTextField campoRecebido = new JTextField("0,00", 8);
    private final JLabel     lblTroco      = new JLabel("R$ 0,00");

    private final List<ItemVenda> carrinho = new ArrayList<>();
    private boolean vendaFinalizada = false;

    public DialogNovaVenda(Window pai, VendaServico vendaServico,
                           ProdutoServico produtoServico, ClienteServico clienteServico) {
        super(pai, "Nova Venda", ModalityType.APPLICATION_MODAL);
        this.vendaServico   = vendaServico;
        this.produtoServico = produtoServico;

        setSize(900, 600);
        setMinimumSize(new Dimension(720, 500));
        setLocationRelativeTo(pai);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        // ══════════════════════════════════════════
        // TOPO: cliente + pagamento
        // ══════════════════════════════════════════
        cbCliente = new JComboBox<>();
        cbCliente.addItem("— Avulso (sem cadastro) —");
        clienteServico.listarTodos().forEach(c -> cbCliente.addItem(c.getId() + " — " + c.getNome()));
        cbCliente.setPreferredSize(new Dimension(230, 26));

        cbPagamento = new JComboBox<>(FormaPagamento.values());

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        topo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        topo.add(new JLabel("Cliente (opcional):"));
        topo.add(cbCliente);
        topo.add(Box.createHorizontalStrut(20));
        topo.add(new JLabel("Forma de pagamento:"));
        topo.add(cbPagamento);

        // ══════════════════════════════════════════
        // CENTRO: produtos | carrinho
        // ══════════════════════════════════════════
        String[] colProd = {"ID", "Nome", "Preço", "Estoque"};
        modeloProdutos = new DefaultTableModel(colProd, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaProdutos = new JTable(modeloProdutos);
        tabelaProdutos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaProdutos.setRowHeight(22);
        tabelaProdutos.getColumnModel().getColumn(0).setMaxWidth(40);
        tabelaProdutos.getColumnModel().getColumn(2).setMaxWidth(90);
        tabelaProdutos.getColumnModel().getColumn(3).setMaxWidth(80);
        carregarProdutos();

        campoQtd = new JTextField("1", 4);
        JButton btnAdd = UiUtil.botao("Adicionar →", new Color(0, 100, 200));

        JPanel esqSul = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        esqSul.add(new JLabel("Quantidade:"));
        esqSul.add(campoQtd);
        esqSul.add(btnAdd);

        JPanel painelEsq = new JPanel(new BorderLayout(4, 4));
        painelEsq.setBorder(BorderFactory.createTitledBorder("Produtos disponíveis  (duplo-clique para adicionar)"));
        painelEsq.add(new JScrollPane(tabelaProdutos), BorderLayout.CENTER);
        painelEsq.add(esqSul, BorderLayout.SOUTH);

        String[] colCarrinho = {"Produto", "Qtd", "Preço Unit.", "Subtotal"};
        modeloCarrinho = new DefaultTableModel(colCarrinho, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaCarrinho = new JTable(modeloCarrinho);
        tabelaCarrinho.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaCarrinho.setRowHeight(22);

        JButton btnRemover = UiUtil.botao("✕  Remover item", new Color(180, 30, 30));

        JPanel painelDir = new JPanel(new BorderLayout(4, 4));
        painelDir.setBorder(BorderFactory.createTitledBorder("Carrinho"));
        painelDir.add(new JScrollPane(tabelaCarrinho), BorderLayout.CENTER);
        painelDir.add(btnRemover, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelEsq, painelDir);
        split.setDividerLocation(420);
        split.setResizeWeight(0.5);
        split.setBorder(null);

        // ══════════════════════════════════════════
        // SUL: resumo financeiro + botões
        // ══════════════════════════════════════════

        // — Bloco de valores —
        estilizarValor(lblSubtotal, Color.DARK_GRAY, 13f);
        estilizarValor(lblDesconto, new Color(0, 130, 60), 13f);
        estilizarValor(lblTotal,    new Color(180, 0, 0),  15f);
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 15f));

        painelDesconto = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        painelDesconto.setOpaque(false);
        lblTagDesconto.setForeground(new Color(0, 130, 60));
        painelDesconto.add(lblTagDesconto);
        painelDesconto.add(lblDesconto);

        JPanel colValores = new JPanel();
        colValores.setLayout(new BoxLayout(colValores, BoxLayout.Y_AXIS));
        colValores.setOpaque(false);
        colValores.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 16));
        colValores.add(linha("Subtotal:", lblSubtotal));
        colValores.add(painelDesconto);
        colValores.add(separador());
        colValores.add(linha("TOTAL A PAGAR:", lblTotal));

        // — Bloco de troco (só aparece para Dinheiro) —
        campoRecebido.setPreferredSize(new Dimension(90, 24));
        lblTroco.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTroco.setForeground(new Color(0, 100, 200));

        JPanel linhaTroco = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        linhaTroco.setOpaque(false);
        linhaTroco.add(new JLabel("Valor recebido:  R$"));
        linhaTroco.add(campoRecebido);
        JPanel linhaTrocoResult = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        linhaTrocoResult.setOpaque(false);
        linhaTrocoResult.add(new JLabel("Troco:"));
        linhaTrocoResult.add(lblTroco);

        painelTroco = new JPanel();
        painelTroco.setLayout(new BoxLayout(painelTroco, BoxLayout.Y_AXIS));
        painelTroco.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 1, new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(4, 16, 4, 16)));
        painelTroco.setOpaque(false);
        painelTroco.add(linhaTroco);
        painelTroco.add(linhaTrocoResult);

        // — Botões —
        JButton btnFinalizar = UiUtil.botao("✔  Finalizar Venda", new Color(34, 130, 60));
        btnFinalizar.setFont(btnFinalizar.getFont().deriveFont(Font.BOLD, 13f));
        btnFinalizar.setPreferredSize(new Dimension(170, 34));
        JButton btnCancelar = new JButton("Cancelar");

        JPanel colBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        colBotoes.setOpaque(false);
        colBotoes.add(btnCancelar);
        colBotoes.add(btnFinalizar);

        JPanel rodape = new JPanel(new BorderLayout(0, 0));
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        rodape.add(colValores,  BorderLayout.WEST);
        rodape.add(painelTroco, BorderLayout.CENTER);
        rodape.add(colBotoes,   BorderLayout.EAST);

        add(topo,   BorderLayout.NORTH);
        add(split,  BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);

        // ══════════════════════════════════════════
        // EVENTOS
        // ══════════════════════════════════════════
        btnAdd.addActionListener(e -> adicionarAoCarrinho(tabelaCarrinho));
        tabelaProdutos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) adicionarAoCarrinho(tabelaCarrinho);
            }
        });

        btnRemover.addActionListener(e -> {
            int row = tabelaCarrinho.getSelectedRow();
            if (row >= 0) { carrinho.remove(row); atualizarResumo(tabelaCarrinho); }
        });

        cbPagamento.addActionListener(e -> atualizarResumo(tabelaCarrinho));

        campoRecebido.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { recalcularTroco(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { recalcularTroco(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalcularTroco(); }
        });

        btnFinalizar.addActionListener(e -> finalizar());
        btnCancelar.addActionListener(e -> dispose());

        atualizarResumo(tabelaCarrinho); // estado inicial
    }

    // ══════════════════════════════════════════
    // LÓGICA
    // ══════════════════════════════════════════

    private void carregarProdutos() {
        modeloProdutos.setRowCount(0);
        produtoServico.listarTodos().stream()
                .filter(p -> p.getQuantidadeEstoque() > 0)
                .forEach(p -> modeloProdutos.addRow(new Object[]{
                        p.getId(), p.getNome(),
                        String.format("R$ %.2f", p.getPreco()),
                        p.getQuantidadeEstoque()
                }));
    }

    private void adicionarAoCarrinho(JTable tabelaCarrinho) {
        int linha = tabelaProdutos.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um produto.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int produtoId = (int) modeloProdutos.getValueAt(linha, 0);
        Produto produto = produtoServico.buscarPorId(produtoId).orElse(null);
        if (produto == null) return;

        int qtd;
        try {
            qtd = Integer.parseInt(campoQtd.getText().trim());
            if (qtd <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int jaNoCarrinho = carrinho.stream()
                .filter(i -> i.getProduto().getId() == produtoId)
                .mapToInt(ItemVenda::getQuantidade).sum();

        if (jaNoCarrinho + qtd > produto.getQuantidadeEstoque()) {
            JOptionPane.showMessageDialog(this,
                    "Estoque insuficiente. Disponível: " + produto.getQuantidadeEstoque()
                    + ", já no carrinho: " + jaNoCarrinho, "Estoque", JOptionPane.WARNING_MESSAGE);
            return;
        }

        carrinho.add(new ItemVenda(produto, qtd));
        atualizarResumo(tabelaCarrinho);
        campoQtd.setText("1");
    }

    private void atualizarResumo(JTable tabelaCarrinho) {
        modeloCarrinho.setRowCount(0);
        double subtotal = 0;
        for (ItemVenda item : carrinho) {
            modeloCarrinho.addRow(new Object[]{
                    item.getProduto().getNome(), item.getQuantidade(),
                    String.format("R$ %.2f", item.getPrecoUnitario()),
                    String.format("R$ %.2f", item.getSubtotal())
            });
            subtotal += item.getSubtotal();
        }

        FormaPagamento pag = (FormaPagamento) cbPagamento.getSelectedItem();
        boolean temDesconto = pag == FormaPagamento.DINHEIRO || pag == FormaPagamento.PIX;
        double desconto = temDesconto ? subtotal * PCT_DESCONTO : 0;
        double total    = subtotal - desconto;

        lblSubtotal.setText(String.format("R$ %.2f", subtotal));
        lblDesconto.setText(String.format("R$ %.2f", desconto));
        lblTotal.setText(String.format("R$ %.2f", total));

        painelDesconto.setVisible(temDesconto);
        painelTroco.setVisible(pag == FormaPagamento.DINHEIRO);

        if (pag == FormaPagamento.DINHEIRO) recalcularTroco();
    }

    private void recalcularTroco() {
        try {
            double recebido = Double.parseDouble(campoRecebido.getText().trim().replace(",", "."));
            double total    = valorTotalAtual();
            double troco    = recebido - total;
            lblTroco.setText(String.format("R$ %.2f", Math.max(troco, 0)));
            lblTroco.setForeground(troco >= 0 ? new Color(0, 100, 200) : new Color(180, 0, 0));
        } catch (NumberFormatException ignored) {
            lblTroco.setText("—");
        }
    }

    private double valorTotalAtual() {
        double subtotal = carrinho.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        FormaPagamento pag = (FormaPagamento) cbPagamento.getSelectedItem();
        boolean temDesconto = pag == FormaPagamento.DINHEIRO || pag == FormaPagamento.PIX;
        return temDesconto ? subtotal * (1 - PCT_DESCONTO) : subtotal;
    }

    private void finalizar() {
        if (carrinho.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Adicione ao menos um item.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Troco insuficiente
        FormaPagamento pag = (FormaPagamento) cbPagamento.getSelectedItem();
        if (pag == FormaPagamento.DINHEIRO) {
            try {
                double recebido = Double.parseDouble(campoRecebido.getText().trim().replace(",", "."));
                if (recebido < valorTotalAtual()) {
                    JOptionPane.showMessageDialog(this,
                            String.format("Valor recebido (R$ %.2f) é menor que o total (R$ %.2f).",
                                    recebido, valorTotalAtual()),
                            "Valor insuficiente", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        int clienteId = 0;
        String sel = (String) cbCliente.getSelectedItem();
        if (sel != null && sel.contains(" — ")) {
            try { clienteId = Integer.parseInt(sel.split(" — ")[0].trim()); }
            catch (NumberFormatException ignored) {}
        }

        double subtotal = carrinho.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        double desconto = VendaServico.calcularDesconto(pag, subtotal);

        try {
            Venda venda = vendaServico.finalizar(clienteId, new ArrayList<>(carrinho), pag, desconto);

            String msg = String.format(
                    "Venda #%d registrada!\n\nSubtotal:  R$ %.2f\nDesconto:  R$ %.2f\nTotal:     R$ %.2f\n",
                    venda.getId(), venda.getSubtotal(), venda.getDesconto(), venda.getTotal());

            if (pag == FormaPagamento.DINHEIRO) {
                try {
                    double recebido = Double.parseDouble(campoRecebido.getText().trim().replace(",", "."));
                    msg += String.format("\nRecebido:  R$ %.2f\nTroco:     R$ %.2f", recebido, recebido - venda.getTotal());
                } catch (NumberFormatException ignored) {}
            }

            JOptionPane.showMessageDialog(this, msg, "Venda Concluída", JOptionPane.INFORMATION_MESSAGE);
            vendaFinalizada = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════
    // HELPERS DE UI
    // ══════════════════════════════════════════

    private JPanel linha(String label, JLabel valor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(130, 20));
        p.add(lbl);
        p.add(valor);
        return p;
    }

    private JSeparator separador() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private void estilizarValor(JLabel l, Color cor, float tamanho) {
        l.setFont(new Font("Segoe UI", Font.BOLD, (int) tamanho));
        l.setForeground(cor);
    }

    public boolean isVendaFinalizada() { return vendaFinalizada; }
}
