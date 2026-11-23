package Interface;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dao.ConexaoBD;
import modelo.excecao.PersistenciaException;

/**
 * Janela principal do sistema (Entrega 4). Tem três abas: Livros, Usuários e Empréstimos.
 * Para abrir o programa, execute o main desta classe.
 */
public class JanelaPrincipal extends JFrame {

    private final PainelLivros painelLivros = new PainelLivros();
    private final PainelUsuarios painelUsuarios = new PainelUsuarios();
    private final PainelEmprestimos painelEmprestimos = new PainelEmprestimos();

    public JanelaPrincipal() {
        super("Sistema de Biblioteca");

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Livros", painelLivros);
        abas.addTab("Usuários", painelUsuarios);
        abas.addTab("Empréstimos", painelEmprestimos);
        // Ao trocar de aba, a lista é lida de novo do banco, para mostrar sempre dados atuais.
        abas.addChangeListener(e -> atualizarAba(abas.getSelectedIndex()));
        add(abas);

        // Fecha a conexão com o banco quando a janela é fechada.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    ConexaoBD.fechar();
                } catch (PersistenciaException ex) {
                    JOptionPane.showMessageDialog(JanelaPrincipal.this, ex.getMessage(), "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
                dispose();
            }
        });

        setMinimumSize(new Dimension(820, 560));
        setSize(900, 600);
        setLocationRelativeTo(null);
        atualizarAba(0);
    }

    // Manda a aba escolhida reler seus dados.
    private void atualizarAba(int indice) {
        switch (indice) {
            case 0 -> painelLivros.atualizar();
            case 1 -> painelUsuarios.atualizar();
            case 2 -> painelEmprestimos.atualizar();
            default -> { }
        }
    }

    public static void main(String[] args) {
        // Usa a aparência do sistema operacional (Windows, macOS ou Linux).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // Se não der, o Swing usa a aparência padrão dele.
        }
        Apoio.padronizarFonte();
        SwingUtilities.invokeLater(() -> new JanelaPrincipal().setVisible(true));
    }
}
