package Interface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import arquivo.RelatorioCSV;
import arquivo.RelatorioExcel;
import dao.EmprestimoDAO;
import dao.LivroDAO;
import dao.UsuarioDAO;
import modelo.Biblioteca;
import modelo.Emprestimo;
import modelo.Livro;
import modelo.Usuario;
import modelo.excecao.PersistenciaException;
import modelo.excecao.RegistroNaoEncontradoException;

/**
 * Aba "Empréstimos": escolher usuário e livro para emprestar, ver os empréstimos em aberto
 * (os atrasados ficam em vermelho), devolver mostrando a multa e gerar o relatório CSV.
 */
class PainelEmprestimos extends JPanel {

    // Cor de fundo das linhas atrasadas.
    private static final Color COR_ATRASADO = new Color(255, 205, 210);
    // Texto da coluna "Atraso" quando o empréstimo está no prazo.
    private static final String SEM_ATRASO = "—";
    private static final int COLUNA_ATRASO = 5;

    private final LivroDAO livroDAO = new LivroDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmprestimoDAO emprestimoDAO = new EmprestimoDAO();

    private final JComboBox<Usuario> comboUsuario = new JComboBox<>();
    private final JComboBox<Livro> comboLivro = new JComboBox<>();
    private final JLabel rotuloPrevisao = new JLabel("Devolução prevista: —");
    private final JLabel rotuloMulta = new JLabel(" ");

    private final DefaultTableModel modelo =
            Apoio.modeloTabela("ID", "Usuário", "Livro", "Empréstimo", "Prevista", "Atraso");
    private final JTable tabela = new JTable(modelo);

    // Empréstimos em aberto mostrados na tabela, na mesma ordem das linhas.
    private List<Emprestimo> abertos = new ArrayList<>();

    PainelEmprestimos() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        configurarCombos();
        configurarTabela();

        add(montarTopo(), BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
    }

    /** Relê usuários, livros e empréstimos do banco e atualiza a tela (chamado ao abrir a aba). */
    void atualizar() {
        try {
            Biblioteca biblioteca = carregar();
            preencherCombos(biblioteca);
            preencherTabela(biblioteca);
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // ---------- montagem da tela ----------

    // Mostra nos combos o nome (e o tipo) do usuário e o título do livro.
    private void configurarCombos() {
        comboUsuario.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                if (valor instanceof Usuario u) {
                    setText(u.getNome() + " (" + Apoio.tipoDe(u) + ")");
                }
                return this;
            }
        });
        comboLivro.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                if (valor instanceof Livro l) {
                    setText(l.getTitulo());
                }
                return this;
            }
        });
        // Ao trocar o usuário, a data prevista muda (cada tipo tem um prazo).
        comboUsuario.addActionListener(e -> atualizarPrevisao());
    }

    // Tabela de uma linha selecionável; linhas atrasadas ganham fundo vermelho.
    private void configurarTabela() {
        Apoio.estilizarCabecalho(tabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionada,
                                                           boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(t, valor, selecionada, foco, linha, coluna);
                if (!selecionada) {
                    boolean atrasado = !SEM_ATRASO.equals(t.getValueAt(linha, COLUNA_ATRASO));
                    setBackground(atrasado ? COR_ATRASADO : t.getBackground());
                }
                return this;
            }
        });
    }

    // Parte de cima: escolha do usuário e do livro, botão Emprestar e a data prevista.
    private JPanel montarTopo() {
        JButton emprestar = new JButton("Emprestar");
        Apoio.igualarTamanho(emprestar);
        emprestar.addActionListener(e -> emprestar());

        JPanel escolha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        escolha.add(new JLabel("Usuário:"));
        escolha.add(comboUsuario);
        escolha.add(Box.createHorizontalStrut(12));
        escolha.add(new JLabel("Livro:"));
        escolha.add(comboLivro);

        JPanel acao = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        acao.add(emprestar);
        acao.add(rotuloPrevisao);

        JPanel coluna = new JPanel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.add(escolha);
        coluna.add(acao);
        return coluna;
    }

    // Parte do meio: título da lista, tabela e os botões de devolver e gerar relatório.
    private JPanel montarCentro() {
        JLabel titulo = new JLabel("Empréstimos em aberto (atrasados em vermelho)");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD));

        JButton devolver = new JButton("Devolver selecionado");
        devolver.addActionListener(e -> devolver());
        rotuloMulta.setFont(rotuloMulta.getFont().deriveFont(Font.BOLD, 14f));
        JButton relatorio = Apoio.botaoColorido("Gerar relatório CSV", Apoio.VERDE);
        relatorio.addActionListener(e -> gerarRelatorio());

        Apoio.igualarTamanho(devolver, relatorio);
        JPanel linhaDevolver = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        linhaDevolver.add(devolver);
        linhaDevolver.add(rotuloMulta);
        JPanel linhaRelatorio = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        linhaRelatorio.add(relatorio);

        JPanel rodape = new JPanel(new GridLayout(2, 1));
        rodape.add(linhaDevolver);
        rodape.add(linhaRelatorio);

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.add(titulo, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabela), BorderLayout.CENTER);
        centro.add(rodape, BorderLayout.SOUTH);
        return centro;
    }

    // ---------- ações dos botões ----------

    // Registra o empréstimo do livro escolhido para o usuário escolhido.
    private void emprestar() {
        try {
            Usuario escolhido = (Usuario) comboUsuario.getSelectedItem();
            Livro livroEscolhido = (Livro) comboLivro.getSelectedItem();
            if (escolhido == null || livroEscolhido == null) {
                throw new IllegalArgumentException("Escolha um usuário e um livro.");
            }
            // Monta a Biblioteca de novo com os dados do banco, para nunca usar dados velhos.
            Biblioteca biblioteca = carregar();
            Usuario usuario = acharUsuario(biblioteca, escolhido.getId());
            Livro livro = acharLivro(biblioteca, livroEscolhido.getId());

            Emprestimo emprestimo = biblioteca.emprestar(usuario, livro);
            emprestimoDAO.inserir(emprestimo);

            rotuloMulta.setText(" ");
            atualizar();
            rotuloPrevisao.setText("Devolução prevista: " + emprestimo.getDataPrevista().format(Apoio.DATA));
        } catch (Exception ex) {
            Apoio.erro(this, ex);
            atualizar();
        }
    }

    // Devolve o empréstimo selecionado na tabela e mostra a multa.
    private void devolver() {
        try {
            int linha = tabela.getSelectedRow();
            if (linha < 0) {
                throw new IllegalArgumentException("Selecione um empréstimo na tabela.");
            }
            Biblioteca biblioteca = carregar();
            Emprestimo emprestimo = acharEmprestimo(biblioteca, abertos.get(linha).getId());

            double multa = biblioteca.devolver(emprestimo);
            emprestimoDAO.registrarDevolucao(emprestimo, multa);

            atualizar();
            rotuloMulta.setText("Multa: " + Apoio.moeda(multa));
            rotuloMulta.setForeground(multa > 0 ? Color.RED : new Color(0, 130, 0));
        } catch (Exception ex) {
            Apoio.erro(this, ex);
            atualizar();
        }
    }

    // Pede o período e grava o relatório CSV na pasta Downloads.
    private void gerarRelatorio() {
        JTextField campoInicio = new JTextField(LocalDate.now().withDayOfMonth(1).format(Apoio.DATA), 10);
        JTextField campoFim = new JTextField(LocalDate.now().format(Apoio.DATA), 10);
        JPanel pedido = new JPanel(new GridLayout(2, 2, 6, 6));
        pedido.add(new JLabel("Data inicial (dd/MM/aaaa):"));
        pedido.add(campoInicio);
        pedido.add(new JLabel("Data final (dd/MM/aaaa):"));
        pedido.add(campoFim);

        int resposta = JOptionPane.showConfirmDialog(this, pedido, "Relatório de empréstimos",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resposta != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LocalDate inicio = lerData(campoInicio.getText(), "inicial");
            LocalDate fim = lerData(campoFim.getText(), "final");
            Biblioteca biblioteca = carregar();
            // O arquivo é salvo na pasta Downloads do usuário (criada se ainda não existir).
            Path pastaDownloads = Path.of(System.getProperty("user.home"), "Downloads");
            Files.createDirectories(pastaDownloads);
            String nome = "relatorio_emprestimos_" + inicio + "_a_" + fim;
            Path destinoCsv = pastaDownloads.resolve(nome + ".csv");
            Path destinoExcel = pastaDownloads.resolve(nome + ".xlsx");
            List<Emprestimo> todos = biblioteca.getEmprestimos();
            int total = RelatorioCSV.gerar(todos, inicio, fim, destinoCsv);
            RelatorioExcel.gerar(todos, inicio, fim, destinoExcel);
            Apoio.info(this, "Relatório salvo na pasta Downloads com " + total + " empréstimo(s):\n"
                    + destinoExcel.getFileName() + "  (Excel, com títulos em negrito)\n"
                    + destinoCsv.getFileName() + "  (CSV)");
        } catch (Exception ex) {
            Apoio.erro(this, ex);
        }
    }

    // ---------- preenchimento da tela ----------

    // Recarrega os combos mantendo, se possível, o usuário e o livro que já estavam escolhidos.
    private void preencherCombos(Biblioteca biblioteca) {
        Usuario usuarioAnterior = (Usuario) comboUsuario.getSelectedItem();
        Livro livroAnterior = (Livro) comboLivro.getSelectedItem();

        comboUsuario.removeAllItems();
        for (Usuario u : biblioteca.getUsuarios()) {
            comboUsuario.addItem(u);
            if (usuarioAnterior != null && u.getId() == usuarioAnterior.getId()) {
                comboUsuario.setSelectedItem(u);
            }
        }
        comboLivro.removeAllItems();
        for (Livro l : biblioteca.getLivros()) {
            comboLivro.addItem(l);
            if (livroAnterior != null && l.getId() == livroAnterior.getId()) {
                comboLivro.setSelectedItem(l);
            }
        }
        atualizarPrevisao();
    }

    // Lista só os empréstimos em aberto e calcula os dias de atraso de cada um.
    private void preencherTabela(Biblioteca biblioteca) {
        abertos = new ArrayList<>();
        modelo.setRowCount(0);
        for (Emprestimo e : biblioteca.getEmprestimos()) {
            if (!e.estaEmAberto()) {
                continue;
            }
            abertos.add(e);
            long atraso = ChronoUnit.DAYS.between(e.getDataPrevista(), LocalDate.now());
            String textoAtraso = atraso <= 0 ? SEM_ATRASO : atraso + (atraso == 1 ? " dia" : " dias");
            modelo.addRow(new Object[] {e.getId(), e.getUsuario().getNome(), e.getLivro().getTitulo(),
                    e.getDataEmprestimo().format(Apoio.DATA), e.getDataPrevista().format(Apoio.DATA), textoAtraso});
        }
    }

    // Mostra a data em que o usuário escolhido teria de devolver um livro emprestado hoje.
    private void atualizarPrevisao() {
        Usuario usuario = (Usuario) comboUsuario.getSelectedItem();
        if (usuario == null) {
            rotuloPrevisao.setText("Devolução prevista: —");
            return;
        }
        LocalDate previsao = LocalDate.now().plusDays(usuario.prazoDias());
        rotuloPrevisao.setText("Devolução prevista: " + previsao.format(Apoio.DATA));
    }

    // ---------- apoio ----------

    // Monta a Biblioteca com tudo o que está gravado no banco.
    private Biblioteca carregar() throws PersistenciaException {
        List<Livro> livros = livroDAO.listar();
        List<Usuario> usuarios = usuarioDAO.listar();
        List<Emprestimo> emprestimos = emprestimoDAO.listar(usuarios, livros);
        return new Biblioteca(livros, usuarios, emprestimos);
    }

    private Usuario acharUsuario(Biblioteca biblioteca, int id) throws RegistroNaoEncontradoException {
        for (Usuario u : biblioteca.getUsuarios()) {
            if (u.getId() == id) {
                return u;
            }
        }
        throw new RegistroNaoEncontradoException("Usuário " + id + " não encontrado.");
    }

    private Livro acharLivro(Biblioteca biblioteca, int id) throws RegistroNaoEncontradoException {
        for (Livro l : biblioteca.getLivros()) {
            if (l.getId() == id) {
                return l;
            }
        }
        throw new RegistroNaoEncontradoException("Livro " + id + " não encontrado.");
    }

    private Emprestimo acharEmprestimo(Biblioteca biblioteca, int id) throws RegistroNaoEncontradoException {
        for (Emprestimo e : biblioteca.getEmprestimos()) {
            if (e.getId() == id) {
                return e;
            }
        }
        throw new RegistroNaoEncontradoException("Empréstimo " + id + " não encontrado.");
    }

    // Converte o texto dd/MM/aaaa em data, com mensagem clara se estiver errado.
    private LocalDate lerData(String texto, String nome) {
        try {
            return LocalDate.parse(texto.trim(), Apoio.DATA);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data " + nome + " inválida. Use dd/MM/aaaa.");
        }
    }
}
