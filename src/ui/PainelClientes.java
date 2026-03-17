package ui;

import model.Cliente;
import service.ClienteServico;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PainelClientes extends JPanel {
    private static final String[] COLUNAS = {"ID", "Nome", "Telefone", "E-mail"};

    private final ClienteServico servico;
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private final JTextField campoBusca;

    public PainelClientes(ClienteServico servico) {
        this.servico = servico;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Barra superior ──
        campoBusca = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnNovo = criarBotao("+ Novo Cliente", new Color(34, 139, 34));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barraTop.add(new JLabel("Buscar:"));
        barraTop.add(campoBusca);
        barraTop.add(btnBuscar);
        barraTop.add(Box.createHorizontalStrut(20));
        barraTop.add(btnNovo);

        // ── Tabela ──
        modelo = new DefaultTableModel(COLUNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(24);
        tabela.getTableHeader().setFont(tabela.getTableHeader().getFont().deriveFont(Font.BOLD));
        tabela.getColumnModel().getColumn(0).setMaxWidth(50);

        // ── Barra inferior ──
        JButton btnEditar = new JButton("Editar");
        JButton btnRemover = criarBotao("Remover", new Color(180, 30, 30));
        btnEditar.setEnabled(false);
        btnRemover.setEnabled(false);

        JPanel barraBot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        barraBot.add(btnEditar);
        barraBot.add(btnRemover);

        add(barraTop, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(barraBot, BorderLayout.SOUTH);

        // ── Eventos ──
        tabela.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = tabela.getSelectedRow() >= 0;
            btnEditar.setEnabled(sel);
            btnRemover.setEnabled(sel);
        });

        btnBuscar.addActionListener(e -> carregarTabela(campoBusca.getText().trim()));
        campoBusca.addActionListener(e -> carregarTabela(campoBusca.getText().trim()));

        btnNovo.addActionListener(e -> {
            if (abrirDialogCliente(null)) carregarTabela("");
        });

        btnEditar.addActionListener(e -> {
            Cliente c = getClienteSelecionado();
            if (c != null && abrirDialogCliente(c)) carregarTabela(campoBusca.getText().trim());
        });

        btnRemover.addActionListener(e -> {
            Cliente c = getClienteSelecionado();
            if (c == null) return;
            int r = JOptionPane.showConfirmDialog(this,
                    "Remover o cliente \"" + c.getNome() + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                servico.remover(c.getId());
                carregarTabela(campoBusca.getText().trim());
            }
        });

        carregarTabela("");
    }

    private void carregarTabela(String filtro) {
        modelo.setRowCount(0);
        List<Cliente> lista = filtro.isEmpty() ? servico.listarTodos() : servico.buscarPorNome(filtro);
        for (Cliente c : lista) {
            modelo.addRow(new Object[]{c.getId(), c.getNome(), c.getTelefone(), c.getEmail()});
        }
    }

    private Cliente getClienteSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) return null;
        int id = (int) modelo.getValueAt(linha, 0);
        return servico.buscarPorId(id).orElse(null);
    }

    private boolean abrirDialogCliente(Cliente c) {
        JTextField fNome = new JTextField(c != null ? c.getNome() : "", 22);
        JTextField fTelefone = new JTextField(c != null ? c.getTelefone() : "", 15);
        JTextField fEmail = new JTextField(c != null ? c.getEmail() : "", 22);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        adicionarLinha(form, gc, 0, "Nome:", fNome);
        adicionarLinha(form, gc, 1, "Telefone:", fTelefone);
        adicionarLinha(form, gc, 2, "E-mail:", fEmail);

        String titulo = c == null ? "Novo Cliente" : "Editar Cliente";
        int res = JOptionPane.showConfirmDialog(this, form, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return false;

        try {
            if (c == null) {
                servico.cadastrar(fNome.getText(), fTelefone.getText(), fEmail.getText());
            } else {
                servico.atualizarNome(c.getId(), fNome.getText());
                servico.atualizarTelefone(c.getId(), fTelefone.getText());
                servico.atualizarEmail(c.getId(), fEmail.getText());
            }
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void adicionarLinha(JPanel p, GridBagConstraints gc, int linha, String label, JComponent campo) {
        gc.gridx = 0; gc.gridy = linha; gc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        p.add(campo, gc);
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton b = new JButton(texto);
        b.setBackground(cor);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}
