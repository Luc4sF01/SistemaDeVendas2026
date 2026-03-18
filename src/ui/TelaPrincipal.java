package ui;

import service.ClienteServico;
import service.ProdutoServico;
import service.VendaServico;

import javax.swing.*;
import java.awt.*;

public class TelaPrincipal extends JFrame {

    public TelaPrincipal(ProdutoServico produtoServico, ClienteServico clienteServico, VendaServico vendaServico) {
        setTitle("PetShop — Sistema de Vendas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        PainelDashboard  dashboard  = new PainelDashboard(vendaServico, produtoServico, clienteServico);
        PainelProdutos   produtos   = new PainelProdutos(produtoServico);
        PainelEstoque    estoque    = new PainelEstoque(produtoServico);
        PainelClientes   clientes   = new PainelClientes(clienteServico);
        PainelVendas     vendas     = new PainelVendas(vendaServico, produtoServico, clienteServico);
        PainelRelatorios relatorios = new PainelRelatorios(vendaServico, produtoServico);

        JTabbedPane abas = new JTabbedPane(JTabbedPane.LEFT);
        abas.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        abas.addTab("  🏠  Início     ", dashboard);
        abas.addTab("  📦  Produtos   ", produtos);
        abas.addTab("  📊  Estoque    ", estoque);
        abas.addTab("  👥  Clientes   ", clientes);
        abas.addTab("  🛒  Vendas     ", vendas);
        abas.addTab("  📈  Relatórios ", relatorios);

        // Atualiza dashboad e relatórios ao trocar para essas abas
        abas.addChangeListener(e -> {
            Component selecionado = abas.getSelectedComponent();
            if (selecionado == dashboard)  dashboard.atualizar();
            if (selecionado == estoque)    estoque.atualizar();
            if (selecionado == relatorios) relatorios.atualizar();
        });

        add(abas);
        setVisible(true);
    }
}
