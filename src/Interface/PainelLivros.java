package Interface;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import dao.LivroDAO;
import modelo.Livro;
import modelo.excecao.OperacaoInvalidaException;

/**
 * Aba "Livros": formulário de cadastro, busca por título ou autor e tabela com a lista.
 * Ao clicar numa linha da tabela, os dados do livro vão para o formulário para edição.
 */
class PainelLivros extends JPanel {

    private final LivroDAO livroDAO = new LivroDAO();

    // Campos do formulário.
    private final JTextField campoTitulo = new JTextField(22);
    private final JTextField campoAutor = new JTextField(18);
    private final JTextField campoIsbn = new JTextField(22);
    private final JTextField campoExemplares = new JTextField(6);
    private final JTextField campoBusca = new JTextField(24);

    // Tabela de livros. Cada linha guarda os dados já formatados; o id fica na coluna 0.
    private final DefaultTableModel modelo =
            Apoio.modeloTabela("ID", "Título", "Autor", "ISBN", "Total", "Livres");
    private final JTable tabela = new JTable(modelo);

    // Livros mostrados na tabela, na mesma ordem das linhas (para recuperar o livro clicado).
    private List<Livro> mostrados = List.of();

    PainelLivros() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(montarTopo(), BorderLayout.NORTH);
        Apoio.estilizarCabecalho(tabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                preencherFormulario();
            }
        });
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    /** Relê os livros do banco e mostra na tabela (chamado ao abrir a aba). */
    void atualizar() {
        try {
            mostrar(campoBusca.getText().trim().isEmpty()
                    ? livroDAO.listar()
                    : livroDAO.buscar(campoBusca.getText().trim()));
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // ---------- montagem da tela ----------

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout(0, 8));
        JPanel alinhado = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        alinhado.add(montarFormulario());
        topo.add(alinhado, BorderLayout.NORTH);
        topo.add(montarBusca(), BorderLayout.SOUTH);
        return topo;
    }

    private JPanel montarFormulario() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 4);
        g.anchor = GridBagConstraints.WEST;

        adicionar(form, g, 0, 0, new JLabel("Título:"), campoTitulo);
        adicionar(form, g, 2, 0, new JLabel("Autor:"), campoAutor);
        adicionar(form, g, 0, 1, new JLabel("ISBN:"), campoIsbn);
        adicionar(form, g, 2, 1, new JLabel("Exemplares:"), campoExemplares);

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

    private JPanel montarBusca() {
        JPanel busca = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton buscar = new JButton("Buscar");
        Apoio.igualarTamanho(buscar);
        buscar.addActionListener(e -> atualizar());
        campoBusca.addActionListener(e -> atualizar()); // Enter também busca
        busca.add(new JLabel("Buscar (título ou autor):"));
        busca.add(campoBusca);
        busca.add(buscar);
        return busca;
    }

    // Coloca um rótulo e o campo dele lado a lado, na posição (coluna, linha) da grade.
    private void adicionar(JPanel painel, GridBagConstraints g, int coluna, int linha,
                           JLabel rotulo, JTextField campo) {
        g.gridx = coluna;
        g.gridy = linha;
        g.gridwidth = 1;
        painel.add(rotulo, g);
        g.gridx = coluna + 1;
        painel.add(campo, g);
    }

    // ---------- ações dos botões ----------

    // Cadastra um livro novo com os dados do formulário.
    private void salvar() {
        try {
            String titulo = Apoio.obrigatorio(campoTitulo.getText(), "título");
            String autor = Apoio.obrigatorio(campoAutor.getText(), "autor");
            int exemplares = Apoio.inteiro(campoExemplares.getText(), "exemplares");
            Livro livro = livroDAO.inserir(new Livro(0, titulo, autor, isbnOuNulo(), exemplares));
            limpar();
            atualizar();
            Apoio.info(this, "Livro cadastrado com o id " + livro.getId() + ".");
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // Grava as alterações do livro selecionado na tabela.
    private void editar() {
        try {
            Livro atual = livroSelecionado();
            String titulo = Apoio.obrigatorio(campoTitulo.getText(), "título");
            String autor = Apoio.obrigatorio(campoAutor.getText(), "autor");
            int total = Apoio.inteiro(campoExemplares.getText(), "exemplares");

            // Os exemplares emprestados continuam emprestados: só o total muda.
            int emprestados = atual.getQuantidadeTotal() - atual.getQuantidadeExemplares();
            int livres = total - emprestados;
            if (total >= 0 && livres < 0) {
                throw new OperacaoInvalidaException(
                        "O total não pode ser menor que os " + emprestados + " emprestados.");
            }
            // Se nada foi alterado, avisa em vez de gravar à toa.
            String isbnAtual = atual.getIsbn() == null ? "" : atual.getIsbn();
            if (titulo.equals(atual.getTitulo()) && autor.equals(atual.getAutor())
                    && campoIsbn.getText().trim().equals(isbnAtual) && total == atual.getQuantidadeTotal()) {
                Apoio.info(this, "Nenhuma alteração para salvar.");
                return;
            }
            livroDAO.atualizar(new Livro(atual.getId(), titulo, autor, isbnOuNulo(), total, livres));
            limpar();
            atualizar();
            Apoio.info(this, "Livro atualizado.");
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // Remove o livro selecionado, depois de pedir confirmação.
    private void remover() {
        try {
            Livro livro = livroSelecionado();
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Remover o livro \"" + livro.getTitulo() + "\"?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            livroDAO.remover(livro.getId());
            limpar();
            atualizar();
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // Esvazia o formulário e tira a seleção da tabela.
    private void limpar() {
        campoTitulo.setText("");
        campoAutor.setText("");
        campoIsbn.setText("");
        campoExemplares.setText("");
        tabela.clearSelection();
    }

    // ---------- apoio ----------

    private void mostrar(List<Livro> livros) {
        mostrados = livros;
        modelo.setRowCount(0);
        for (Livro l : livros) {
            modelo.addRow(new Object[] {l.getId(), l.getTitulo(), l.getAutor(),
                    l.getIsbn() == null ? "" : l.getIsbn(), l.getQuantidadeTotal(), l.getQuantidadeExemplares()});
        }
    }

    // Livro da linha selecionada; avisa o usuário se nenhuma linha foi escolhida.
    private Livro livroSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            throw new IllegalArgumentException("Selecione um livro na tabela.");
        }
        return mostrados.get(linha);
    }

    // Copia os dados da linha clicada para o formulário. O campo "Exemplares" mostra o total.
    private void preencherFormulario() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            return;
        }
        Livro l = mostrados.get(linha);
        campoTitulo.setText(l.getTitulo());
        campoAutor.setText(l.getAutor());
        campoIsbn.setText(l.getIsbn() == null ? "" : l.getIsbn());
        campoExemplares.setText(String.valueOf(l.getQuantidadeTotal()));
    }

    // O ISBN é opcional: campo vazio vira null no banco.
    private String isbnOuNulo() {
        String isbn = campoIsbn.getText().trim();
        return isbn.isEmpty() ? null : isbn;
    }
}
