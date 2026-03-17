package ui;

import javax.swing.*;
import java.awt.*;

/** Utilitários de UI reutilizáveis em todos os painéis. */
public final class UiUtil {

    private UiUtil() {}

    /**
     * Cria um JButton colorido que funciona em qualquer Look & Feel,
     * incluindo Nimbus (que ignora setBackground por padrão).
     */
    public static JButton botao(String texto, Color cor) {
        JButton b = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? cor.darker().darker()
                         : getModel().isRollover() ? cor.darker()
                         : cor;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                // pinta só o texto/ícone por cima
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 16, 30));
        return b;
    }

    /**
     * Cria um card de ação grande e clicável (para o Dashboard).
     * Pintado com arredondamento e efeito hover.
     */
    public static JPanel cardAcao(String emoji, String titulo, String subtitulo,
                                   Color cor, Runnable acao) {
        JPanel card = new JPanel() {
            private boolean hover = false;

            {
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true;  repaint(); }
                    @Override public void mouseExited (java.awt.event.MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (isEnabled()) acao.run(); }
                    @Override public void mousePressed(java.awt.event.MouseEvent e) { repaint(); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e) { repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = hover ? cor.darker() : cor;
                // Sombra suave
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(3, 5, getWidth() - 3, getHeight() - 3, 14, 14);
                // Corpo
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 4, 14, 14);
                g2.dispose();
            }

        };

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel lblEmoji = centrado(emoji, new Font("Segoe UI Emoji", Font.PLAIN, 28), Color.WHITE);
        JLabel lblTitulo = centrado(titulo, new Font("Segoe UI", Font.BOLD, 13), Color.WHITE);
        JLabel lblSub = centrado(subtitulo, new Font("Segoe UI", Font.PLAIN, 11),
                new Color(255, 255, 255, 190));

        card.add(Box.createVerticalGlue());
        card.add(lblEmoji);
        card.add(Box.createVerticalStrut(5));
        card.add(lblTitulo);
        card.add(Box.createVerticalStrut(2));
        card.add(lblSub);
        card.add(Box.createVerticalGlue());

        return card;
    }

    /** Card de métrica (fundo claro com borda colorida). */
    public static JPanel cardMetrica(String titulo, JLabel valor, Color cor) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        valor.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valor.setForeground(cor);
        valor.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        JLabel lblT = new JLabel(titulo, SwingConstants.CENTER);
        lblT.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblT.setForeground(new Color(80, 80, 80));
        lblT.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(lblT);
        p.add(Box.createVerticalStrut(4));
        p.add(valor);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JLabel centrado(String texto, Font fonte, Color cor) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setFont(fonte);
        l.setForeground(cor);
        l.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        return l;
    }
}
