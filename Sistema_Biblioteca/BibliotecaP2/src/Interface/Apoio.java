package Interface;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import modelo.Usuario;

/**
 * Funções pequenas usadas por todas as telas: mensagens de erro, formatos de data e
 * dinheiro e criação de tabelas. Fica separada para não repetir código em cada painel.
 */
final class Apoio {

    // Formato de data mostrado ao usuário (dd/MM/aaaa).
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Cores da interface.
    static final Color AZUL = new Color(120, 180, 240);
    static final Color VERDE = new Color(110, 200, 120);
    static final Color VERMELHO = new Color(235, 90, 90);

    // Fonte de toda a interface: Arial tamanho 13 (negrito nos títulos, botões e abas).
    private static final Font FONTE = new Font("Arial", Font.BOLD, 13);
    private static final Font FONTE_NORMAL = new Font("Arial", Font.PLAIN, 13);

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    // Classe utilitária: não se cria objeto dela.
    private Apoio() {
    }

    /** Mostra o erro numa caixa de mensagem e o programa continua funcionando. */
    static void erro(Component tela, Exception ex) {
        String mensagem = ex.getMessage() != null ? ex.getMessage() : "Ocorreu um erro inesperado.";
        JOptionPane.showMessageDialog(tela, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /** Mostra um aviso simples (operação concluída). */
    static void info(Component tela, String mensagem) {
        JOptionPane.showMessageDialog(tela, mensagem, "Biblioteca", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Lê um campo de texto obrigatório; se estiver vazio, avisa qual campo falta. */
    static String obrigatorio(String texto, String campo) {
        String limpo = texto.trim();
        if (limpo.isEmpty()) {
            throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        }
        return limpo;
    }

    /** Converte o texto em número inteiro, com uma mensagem clara se não for número. */
    static int inteiro(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("O campo " + campo + " precisa ser um número inteiro.");
        }
    }

    /** Valor em reais no formato brasileiro, por exemplo "R$ 8,00". */
    static String moeda(double valor) {
        return String.format(PT_BR, "R$ %.2f", valor);
    }

    /** Nome do tipo do usuário para mostrar nas telas ("Aluno" ou "Professor"). */
    static String tipoDe(Usuario usuario) {
        return usuario.getClass().getSimpleName();
    }

    /** Cria o modelo de uma tabela só de leitura: o usuário seleciona, mas não digita nas células. */
    static DefaultTableModel modeloTabela(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    /** Coloca todos os componentes (rótulos, campos, tabelas, botões, abas) na mesma fonte e tamanho. */
    static void padronizarFonte() {
        for (Object chave : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(chave) instanceof FontUIResource) {
                UIManager.put(chave, new FontUIResource(FONTE_NORMAL));
            }
        }
        for (String chave : new String[] {"Button.font", "ToggleButton.font", "TabbedPane.font"}) {
            UIManager.put(chave, new FontUIResource(FONTE));
        }
    }

    /** Dá aos botões o mesmo tamanho (o maior deles, com altura mínima de 30), para a tela ficar alinhada. */
    static void igualarTamanho(JButton... botoes) {
        int largura = 0;
        int altura = 30;
        for (JButton b : botoes) {
            largura = Math.max(largura, b.getPreferredSize().width);
            altura = Math.max(altura, b.getPreferredSize().height);
        }
        for (JButton b : botoes) {
            b.setPreferredSize(new Dimension(largura, altura));
        }
    }

    /** Pinta o cabeçalho da tabela (ID, Título, Autor...) de azul, com texto Arial preto em negrito. */
    static void estilizarCabecalho(JTable tabela) {
        JTableHeader cabecalho = tabela.getTableHeader();
        cabecalho.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionada,
                                                           boolean foco, int linha, int coluna) {
                JLabel rotulo = (JLabel) super.getTableCellRendererComponent(t, valor, false, false, linha, coluna);
                rotulo.setOpaque(true);
                rotulo.setBackground(AZUL);
                rotulo.setForeground(Color.BLACK);
                rotulo.setFont(FONTE);
                rotulo.setHorizontalAlignment(SwingConstants.CENTER);
                rotulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(70, 120, 180)));
                return rotulo;
            }
        });
        cabecalho.setPreferredSize(new Dimension(cabecalho.getPreferredSize().width, 30));
        tabela.setRowHeight(24);
    }

    /** Cria um botão com fundo da cor dada e texto Arial preto em negrito (aparece igual em qualquer sistema). */
    static JButton botaoColorido(String texto, Color cor) {
        return new BotaoColorido(texto, cor);
    }

    // Botão que desenha o próprio fundo, porque em alguns sistemas o Swing ignora a cor de fundo dos botões.
    private static class BotaoColorido extends JButton {
        private final Color cor;

        BotaoColorido(String texto, Color cor) {
            super(texto);
            this.cor = cor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(FONTE);
            setForeground(Color.BLACK);
            setMargin(new Insets(2, 14, 2, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Fica um pouco mais escuro ao passar o mouse e mais ainda ao clicar.
            Color fundo = getModel().isPressed() ? cor.darker().darker()
                    : getModel().isRollover() ? cor.darker() : cor;
            g2.setColor(fundo);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(fundo.darker());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
