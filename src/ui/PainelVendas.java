package ui;

import model.ItemVenda;
import model.Venda;
import service.ClienteServico;
import service.ProdutoServico;
import service.VendaServico;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PainelVendas extends JPanel {
    private static final String[] COLUNAS = {"ID", "Data/Hora", "Cliente", "Total (R$)", "Pagamento"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VendaServico vendaServico;
    private final ProdutoServico produtoServico;
    private final ClienteServico clienteServico;
    private final DefaultTableModel modelo;
    private final JTable tabela;

    public PainelVendas(VendaServico vendaServico, ProdutoServico produtoServico, ClienteServico clienteServico) {
        this.vendaServico = vendaServico;
        this.produtoServico = produtoServico;
        this.clienteServico = clienteServico;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Barra superior ──
        JButton btnNova = criarBotao("+ Nova Venda", new Color(34, 139, 34));
        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barraTop.add(btnNova);

        // ── Tabela ──
        modelo = new DefaultTableModel(COLUNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(24);
        tabela.getTableHeader().setFont(tabela.getTableHeader().getFont().deriveFont(Font.BOLD));
        tabela.getColumnModel().getColumn(0).setMaxWidth(50);
        tabela.getColumnModel().getColumn(1).setMinWidth(130);
        tabela.getColumnModel().getColumn(3).setMaxWidth(110);

        // ── Barra inferior ──
        JButton btnDetalhes = new JButton("Ver detalhes");
        btnDetalhes.setEnabled(false);

        JPanel barraBot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        barraBot.add(btnDetalhes);

        add(barraTop, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(barraBot, BorderLayout.SOUTH);

        // ── Eventos ──
        tabela.getSelectionModel().addListSelectionListener(e -> btnDetalhes.setEnabled(tabela.getSelectedRow() >= 0));

        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) exibirDetalhes();
            }
        });

        btnNova.addActionListener(e -> {
            DialogNovaVenda dialog = new DialogNovaVenda(
                    SwingUtilities.getWindowAncestor(this), vendaServico, produtoServico, clienteServico);
            dialog.setVisible(true);
            if (dialog.isVendaFinalizada()) carregarTabela();
        });

        btnDetalhes.addActionListener(e -> exibirDetalhes());

        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        List<Venda> vendas = vendaServico.listarTodas();
        for (Venda v : vendas) {
            String nomeCliente = v.getCliente() != null ? v.getCliente().getNome() : "Avulso";
            modelo.addRow(new Object[]{
                    v.getId(),
                    v.getDataHora().format(FMT),
                    nomeCliente,
                    String.format("%.2f", v.getTotal()),
                    v.getFormaPagamento().getDescricao()
            });
        }
    }

    private void exibirDetalhes() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) return;
        int id = (int) modelo.getValueAt(linha, 0);
        Venda venda = vendaServico.listarTodas().stream()
                .filter(v -> v.getId() == id).findFirst().orElse(null);
        if (venda == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Venda #%d  —  %s%n", venda.getId(), venda.getDataHora().format(FMT)));
        sb.append(String.format("Cliente : %s%n", venda.getCliente() != null ? venda.getCliente().getNome() : "Avulso"));
        sb.append(String.format("Pagamento: %s%n%n", venda.getFormaPagamento().getDescricao()));
        sb.append("Itens:\n");
        for (ItemVenda item : venda.getItens()) {
            sb.append(String.format("  %-22s x%2d  R$ %.2f  =  R$ %.2f%n",
                    item.getProduto().getNome(), item.getQuantidade(),
                    item.getPrecoUnitario(), item.getSubtotal()));
        }
        sb.append(String.format("%n  Subtotal : R$ %.2f", venda.getSubtotal()));
        if (venda.getDesconto() > 0)
            sb.append(String.format("%n  Desconto : -R$ %.2f (5%%)", venda.getDesconto()));
        sb.append(String.format("%n  TOTAL    : R$ %.2f", venda.getTotal()));

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(480, 300));

        JOptionPane.showMessageDialog(this, scroll, "Detalhes da Venda", JOptionPane.PLAIN_MESSAGE);
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton b = new JButton(texto);
        b.setBackground(cor);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}
