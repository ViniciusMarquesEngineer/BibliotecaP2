package Interface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

import dao.UsuarioDAO;
import modelo.Aluno;
import modelo.Professor;
import modelo.Usuario;

/**
 * Aba "Usuários": formulário com a escolha entre aluno e professor e tabela com a lista.
 * Ao clicar numa linha da tabela, os dados do usuário vão para o formulário para edição.
 */
class PainelUsuarios extends JPanel {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // Campos do formulário.
    private final JTextField campoNome = new JTextField(26);
    private final JTextField campoMatricula = new JTextField(16);
    private final JRadioButton opcaoAluno = new JRadioButton("Aluno", true);
    private final JRadioButton opcaoProfessor = new JRadioButton("Professor");

    private final DefaultTableModel modelo = Apoio.modeloTabela("ID", "Nome", "Matrícula", "Tipo");
    private final JTable tabela = new JTable(modelo);

    // Usuários mostrados na tabela, na mesma ordem das linhas.
    private List<Usuario> mostrados = List.of();

    PainelUsuarios() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // O campo da matrícula só aceita dígitos: letras e símbolos digitados ou colados são ignorados.
        ((PlainDocument) campoMatricula.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int posicao, String texto, AttributeSet atributos)
                    throws BadLocationException {
                if (texto != null) {
                    super.insertString(fb, posicao, somenteDigitos(texto), atributos);
                }
            }

            @Override
            public void replace(FilterBypass fb, int posicao, int tamanho, String texto, AttributeSet atributos)
                    throws BadLocationException {
                super.replace(fb, posicao, tamanho, texto == null ? null : somenteDigitos(texto), atributos);
            }
        });

        // Os dois botões de rádio formam um grupo: só um fica marcado por vez.
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(opcaoAluno);
        grupo.add(opcaoProfessor);

        // O formulário fica encostado à esquerda, como no esboço.
        JPanel alinhado = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        alinhado.add(montarFormulario());
        add(alinhado, BorderLayout.NORTH);
        Apoio.estilizarCabecalho(tabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                preencherFormulario();
            }
        });
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    /** Relê os usuários do banco e mostra na tabela (chamado ao abrir a aba). */
    void atualizar() {
        try {
            mostrados = usuarioDAO.listar();
            modelo.setRowCount(0);
            for (Usuario u : mostrados) {
                modelo.addRow(new Object[] {u.getId(), u.getNome(), u.getMatricula(), Apoio.tipoDe(u)});
            }
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // ---------- montagem da tela ----------

    private JPanel montarFormulario() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        form.add(new JLabel("Nome:"), g);
        g.gridx = 1;
        form.add(campoNome, g);
        g.gridx = 2;
        form.add(new JLabel("Matrícula:"), g);
        g.gridx = 3;
        form.add(campoMatricula, g);

        JPanel tipo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tipo.add(opcaoAluno);
        tipo.add(opcaoProfessor);
        g.gridx = 0;
        g.gridy = 1;
        form.add(new JLabel("Tipo:"), g);
        g.gridx = 1;
        g.gridwidth = 3;
        form.add(tipo, g);

        JButton salvar = new JButton("Salvar");
        JButton editar = new JButton("Editar");
        JButton remover = Apoio.botaoColorido("Remover", Apoio.VERMELHO);
        salvar.addActionListener(e -> salvar());
        editar.addActionListener(e -> editar());
        remover.addActionListener(e -> remover());

        Apoio.igualarTamanho(salvar, editar, remover);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botoes.add(salvar);
        botoes.add(editar);
        botoes.add(remover);
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 4;
        form.add(botoes, g);
        return form;
    }

    // ---------- ações dos botões ----------

    // Cadastra um usuário novo com os dados do formulário.
    private void salvar() {
        try {
            String nome = Apoio.obrigatorio(campoNome.getText(), "nome");
            String matricula = lerMatricula();
            Usuario usuario = usuarioDAO.inserir(criarUsuario(0, nome, matricula));
            limpar();
            atualizar();
            Apoio.info(this, "Usuário cadastrado com o id " + usuario.getId() + ".");
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // Grava as alterações do usuário selecionado na tabela.
    private void editar() {
        try {
            Usuario atual = usuarioSelecionado();
            String nome = Apoio.obrigatorio(campoNome.getText(), "nome");
            String matricula = lerMatricula();
            // Se nada foi alterado, avisa em vez de gravar à toa.
            String tipoEscolhido = opcaoProfessor.isSelected() ? "Professor" : "Aluno";
            if (nome.equals(atual.getNome()) && matricula.equals(atual.getMatricula())
                    && tipoEscolhido.equals(Apoio.tipoDe(atual))) {
                Apoio.info(this, "Nenhuma alteração para salvar.");
                return;
            }
            usuarioDAO.atualizar(criarUsuario(atual.getId(), nome, matricula));
            limpar();
            atualizar();
            Apoio.info(this, "Usuário atualizado.");
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // Remove o usuário selecionado, depois de pedir confirmação.
    private void remover() {
        try {
            Usuario usuario = usuarioSelecionado();
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Remover o usuário \"" + usuario.getNome() + "\"?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            usuarioDAO.remover(usuario.getId());
            limpar();
            atualizar();
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // Esvazia o formulário e tira a seleção da tabela.
    private void limpar() {
        campoNome.setText("");
        campoMatricula.setText("");
        opcaoAluno.setSelected(true);
        tabela.clearSelection();
    }

    // ---------- apoio ----------

    // Mantém só os dígitos de um texto.
    private static String somenteDigitos(String texto) {
        return texto.replaceAll("\\D", "");
    }

    // Lê a matrícula do formulário: obrigatória e formada apenas por números.
    private String lerMatricula() {
        String matricula = Apoio.obrigatorio(campoMatricula.getText(), "matrícula");
        if (!matricula.matches("\\d+")) {
            throw new IllegalArgumentException("A matrícula deve conter apenas números.");
        }
        return matricula;
    }

    // Cria um Aluno ou um Professor conforme a opção marcada no formulário.
    private Usuario criarUsuario(int id, String nome, String matricula) {
        return opcaoProfessor.isSelected()
                ? new Professor(id, nome, matricula)
                : new Aluno(id, nome, matricula);
    }

    // Usuário da linha selecionada; avisa se nenhuma linha foi escolhida.
    private Usuario usuarioSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            throw new IllegalArgumentException("Selecione um usuário na tabela.");
        }
        return mostrados.get(linha);
    }

    // Copia os dados da linha clicada para o formulário.
    private void preencherFormulario() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        Usuario u = mostrados.get(linha);
        campoNome.setText(u.getNome());
        campoMatricula.setText(u.getMatricula());
        if (Apoio.tipoDe(u).equals("Professor")) {
            opcaoProfessor.setSelected(true);
        } else {
            opcaoAluno.setSelected(true);
        }
    }
}
