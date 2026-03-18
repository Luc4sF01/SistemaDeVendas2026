import database.InicializadorBanco;
import repository.ClienteRepositorio;
import repository.ProdutoRepositorio;
import repository.VendaRepositorio;
import service.ClienteServico;
import service.ProdutoServico;
import service.VendaServico;
import ui.TelaPrincipal;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Inicializa o banco de dados (cria o arquivo e as tabelas se não existirem)
        InicializadorBanco.inicializar();

        // Nimbus: L&F moderno, suporta componentes customizados corretamente
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    // Ajustes de cores globais do Nimbus
                    UIManager.put("control",          new java.awt.Color(245, 247, 250));
                    UIManager.put("nimbusBase",       new java.awt.Color(50, 90, 160));
                    UIManager.put("nimbusBlueGrey",   new java.awt.Color(100, 115, 140));
                    UIManager.put("Table.alternateRowColor", new java.awt.Color(248, 249, 252));
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Repositórios
        ProdutoRepositorio produtoRepo = new ProdutoRepositorio();
        ClienteRepositorio clienteRepo = new ClienteRepositorio();
        VendaRepositorio   vendaRepo   = new VendaRepositorio();

        // Serviços
        ProdutoServico produtoServico = new ProdutoServico(produtoRepo);
        ClienteServico clienteServico = new ClienteServico(clienteRepo);
        VendaServico   vendaServico   = new VendaServico(vendaRepo, produtoServico, clienteServico);

        SwingUtilities.invokeLater(() ->
                new TelaPrincipal(produtoServico, clienteServico, vendaServico)
        );
    }
}
