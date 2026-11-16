package app;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import arquivo.RelatorioCSV;
import dao.ConexaoBD;
import dao.EmprestimoDAO;
import dao.LivroDAO;
import dao.UsuarioDAO;
import modelo.Aluno;
import modelo.Biblioteca;
import modelo.Emprestimo;
import modelo.Livro;
import modelo.Professor;
import modelo.Usuario;
import modelo.excecao.OperacaoInvalidaException;
import modelo.excecao.PersistenciaException;
import modelo.excecao.RegistroNaoEncontradoException;

/** Menu de console da Entrega 3: usa o banco SQLite pelos DAOs. */
public class MenuConsole {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FunctionalInterface
    private interface Acao {
        void executar() throws Exception;
    }

    private final Scanner entrada = new Scanner(System.in);
    private final LivroDAO livroDAO = new LivroDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmprestimoDAO emprestimoDAO = new EmprestimoDAO();

    public static void main(String[] args) {
        try {
            new MenuConsole().principal();
        } catch (NoSuchElementException ex) {
            System.out.println("\nEntrada encerrada.");
        } finally {
            try {
                ConexaoBD.fechar();
            } catch (PersistenciaException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    // ---------- menus ----------

    private void principal() {
        while (true) {
            System.out.println("\n=== Sistema de Biblioteca ===");
            System.out.println("1. Livros");
            System.out.println("2. Usuários");
            System.out.println("3. Empréstimos");
            System.out.println("4. Gerar relatório CSV");
            System.out.println("0. Sair");
            switch (lerInt("Opção: ")) {
                case 1 -> menuLivros();
                case 2 -> menuUsuarios();
                case 3 -> menuEmprestimos();
                case 4 -> executar(this::gerarRelatorio);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void menuLivros() {
        while (true) {
            System.out.println("\n--- Livros ---");
            System.out.println("1. Cadastrar");
            System.out.println("2. Listar");
            System.out.println("3. Buscar por título ou autor");
            System.out.println("4. Editar");
            System.out.println("5. Remover");
            System.out.println("0. Voltar");
            switch (lerInt("Opção: ")) {
                case 1 -> executar(this::cadastrarLivro);
                case 2 -> executar(this::listarLivros);
                case 3 -> executar(this::buscarLivros);
                case 4 -> executar(this::editarLivro);
                case 5 -> executar(this::removerLivro);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void menuUsuarios() {
        while (true) {
            System.out.println("\n--- Usuários ---");
            System.out.println("1. Cadastrar");
            System.out.println("2. Listar");
            System.out.println("3. Editar");
            System.out.println("4. Remover");
            System.out.println("0. Voltar");
            switch (lerInt("Opção: ")) {
                case 1 -> executar(this::cadastrarUsuario);
                case 2 -> executar(this::listarUsuarios);
                case 3 -> executar(this::editarUsuario);
                case 4 -> executar(this::removerUsuario);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private void menuEmprestimos() {
        while (true) {
            System.out.println("\n--- Empréstimos ---");
            System.out.println("1. Emprestar");
            System.out.println("2. Devolver");
            System.out.println("3. Listar empréstimos em aberto");
            System.out.println("0. Voltar");
            switch (lerInt("Opção: ")) {
                case 1 -> executar(this::emprestar);
                case 2 -> executar(this::devolver);
                case 3 -> executar(this::listarEmAberto);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    // Mostra o erro e volta ao menu, sem fechar o programa.
    private void executar(Acao acao) {
        try {
            acao.executar();
        } catch (NoSuchElementException ex) {
            throw ex; // entrada encerrada: quem trata é o main
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.out.println("Erro: " + msg);
        }
    }

    // ---------- livros ----------

    private void cadastrarLivro() throws Exception {
        String titulo = lerObrigatorio("Título: ", "título");
        String autor = lerObrigatorio("Autor: ", "autor");
        String isbn = lerTexto("ISBN (opcional): ");
        int quantidade = lerInt("Quantidade de exemplares: ");
        Livro livro = livroDAO.inserir(new Livro(0, titulo, autor, isbn.isEmpty() ? null : isbn, quantidade));
        System.out.println("Livro cadastrado com o id " + livro.getId() + ".");
    }

    private void listarLivros() throws Exception {
        mostrarLivros(livroDAO.listar());
    }

    private void buscarLivros() throws Exception {
        String termo = lerObrigatorio("Título ou autor: ", "busca");
        mostrarLivros(livroDAO.buscar(termo));
    }

    private void editarLivro() throws Exception {
        int id = lerInt("ID do livro: ");
        Livro atual = livroDAO.buscarPorId(id);
        System.out.println("(Enter mantém o valor atual)");
        String titulo = lerOuManter("Título [" + atual.getTitulo() + "]: ", atual.getTitulo());
        String autor = lerOuManter("Autor [" + atual.getAutor() + "]: ", atual.getAutor());
        String isbnAtual = atual.getIsbn() == null ? "" : atual.getIsbn();
        String isbn = lerOuManter("ISBN [" + isbnAtual + "]: ", isbnAtual);
        int total = lerIntOuManter("Total de exemplares [" + atual.getQuantidadeTotal() + "]: ",
                atual.getQuantidadeTotal());

        int emprestados = atual.getQuantidadeTotal() - atual.getQuantidadeExemplares();
        int livres = total - emprestados;
        if (total >= 0 && livres < 0) {
            throw new OperacaoInvalidaException(
                    "O total não pode ser menor que os " + emprestados + " exemplares emprestados.");
        }
        livroDAO.atualizar(new Livro(id, titulo, autor, isbn.isEmpty() ? null : isbn, total, livres));
        System.out.println("Livro atualizado.");
    }

    private void removerLivro() throws Exception {
        livroDAO.remover(lerInt("ID do livro: "));
        System.out.println("Livro removido.");
    }

    private void mostrarLivros(List<Livro> livros) {
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado.");
            return;
        }
        System.out.printf("%-4s %-30s %-22s %-18s %5s %5s%n", "ID", "Título", "Autor", "ISBN", "Total", "Livres");
        for (Livro l : livros) {
            System.out.printf("%-4d %-30s %-22s %-18s %5d %5d%n", l.getId(), l.getTitulo(), l.getAutor(),
                    l.getIsbn() == null ? "-" : l.getIsbn(), l.getQuantidadeTotal(), l.getQuantidadeExemplares());
        }
    }

    // ---------- usuários ----------

    private void cadastrarUsuario() throws Exception {
        String nome = lerObrigatorio("Nome: ", "nome");
        String matricula = lerObrigatorio("Matrícula: ", "matrícula");
        int tipo = lerTipo("Tipo (1-Aluno, 2-Professor): ");
        Usuario usuario = usuarioDAO.inserir(criarUsuario(tipo, 0, nome, matricula));
        System.out.println("Usuário cadastrado com o id " + usuario.getId() + ".");
    }

    private void listarUsuarios() throws Exception {
        List<Usuario> usuarios = usuarioDAO.listar();
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
            return;
        }
        System.out.printf("%-4s %-30s %-16s %-10s%n", "ID", "Nome", "Matrícula", "Tipo");
        for (Usuario u : usuarios) {
            System.out.printf("%-4d %-30s %-16s %-10s%n", u.getId(), u.getNome(), u.getMatricula(),
                    u.getClass().getSimpleName());
        }
    }

    private void editarUsuario() throws Exception {
        int id = lerInt("ID do usuário: ");
        Usuario atual = usuarioDAO.buscarPorId(id);
        System.out.println("(Enter mantém nome e matrícula atuais)");
        String nome = lerOuManter("Nome [" + atual.getNome() + "]: ", atual.getNome());
        String matricula = lerOuManter("Matrícula [" + atual.getMatricula() + "]: ", atual.getMatricula());
        int tipo = lerTipo("Tipo (atual: " + atual.getClass().getSimpleName() + ") - 1-Aluno, 2-Professor: ");
        usuarioDAO.atualizar(criarUsuario(tipo, id, nome, matricula));
        System.out.println("Usuário atualizado.");
    }

    private void removerUsuario() throws Exception {
        usuarioDAO.remover(lerInt("ID do usuário: "));
        System.out.println("Usuário removido.");
    }

    private Usuario criarUsuario(int tipo, int id, String nome, String matricula) {
        return tipo == 1 ? new Aluno(id, nome, matricula) : new Professor(id, nome, matricula);
    }

    // ---------- empréstimos ----------

    private void emprestar() throws Exception {
        Biblioteca biblioteca = carregar();
        Usuario usuario = acharUsuario(biblioteca, lerInt("ID do usuário: "));
        Livro livro = acharLivro(biblioteca, lerInt("ID do livro: "));
        Emprestimo emprestimo = biblioteca.emprestar(usuario, livro);
        int id = emprestimoDAO.inserir(emprestimo);
        System.out.printf("Empréstimo %d registrado: %s pegou \"%s\" em %s -> devolução prevista: %s%n",
                id, usuario.getNome(), livro.getTitulo(),
                emprestimo.getDataEmprestimo().format(FMT), emprestimo.getDataPrevista().format(FMT));
    }

    private void devolver() throws Exception {
        Biblioteca biblioteca = carregar();
        int id = lerInt("ID do empréstimo: ");
        Emprestimo emprestimo = acharEmprestimo(biblioteca, id);
        double multa = biblioteca.devolver(emprestimo);
        emprestimoDAO.registrarDevolucao(emprestimo, multa);
        System.out.printf("Devolução registrada. Multa: R$ %.2f%n", multa);
    }

    private void listarEmAberto() throws Exception {
        Biblioteca biblioteca = carregar();
        List<Emprestimo> abertos = new ArrayList<>();
        for (Emprestimo e : biblioteca.getEmprestimos()) {
            if (e.estaEmAberto()) {
                abertos.add(e);
            }
        }
        if (abertos.isEmpty()) {
            System.out.println("Nenhum empréstimo em aberto.");
            return;
        }
        System.out.printf("%-4s %-22s %-28s %-12s %-12s%n", "ID", "Usuário", "Livro", "Empréstimo", "Prevista");
        for (Emprestimo e : abertos) {
            System.out.printf("%-4d %-22s %-28s %-12s %-12s%n", e.getId(), e.getUsuario().getNome(),
                    e.getLivro().getTitulo(), e.getDataEmprestimo().format(FMT), e.getDataPrevista().format(FMT));
        }
    }

    // ---------- relatório ----------

    private void gerarRelatorio() throws Exception {
        LocalDate inicio = lerData("Data inicial (dd/MM/aaaa): ");
        LocalDate fim = lerData("Data final (dd/MM/aaaa): ");
        Biblioteca biblioteca = carregar();
        Path destino = Path.of("relatorio_emprestimos_" + inicio + "_a_" + fim + ".csv");
        int total = RelatorioCSV.gerar(biblioteca.getEmprestimos(), inicio, fim, destino);
        System.out.println("Relatório gerado com " + total + " empréstimo(s): " + destino.toAbsolutePath());
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

    private String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return entrada.nextLine().trim();
    }

    private String lerObrigatorio(String mensagem, String campo) {
        String texto = lerTexto(mensagem);
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("O campo " + campo + " é obrigatório.");
        }
        return texto;
    }

    private String lerOuManter(String mensagem, String atual) {
        String texto = lerTexto(mensagem);
        return texto.isEmpty() ? atual : texto;
    }

    private int lerInt(String mensagem) {
        while (true) {
            try {
                return Integer.parseInt(lerTexto(mensagem));
            } catch (NumberFormatException ex) {
                System.out.println("Digite um número inteiro.");
            }
        }
    }

    private int lerIntOuManter(String mensagem, int atual) {
        while (true) {
            String texto = lerTexto(mensagem);
            if (texto.isEmpty()) {
                return atual;
            }
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException ex) {
                System.out.println("Digite um número inteiro.");
            }
        }
    }

    private int lerTipo(String mensagem) {
        while (true) {
            int tipo = lerInt(mensagem);
            if (tipo == 1 || tipo == 2) {
                return tipo;
            }
            System.out.println("Digite 1 para Aluno ou 2 para Professor.");
        }
    }

    private LocalDate lerData(String mensagem) {
        while (true) {
            try {
                return LocalDate.parse(lerTexto(mensagem), FMT);
            } catch (DateTimeParseException ex) {
                System.out.println("Data inválida. Use o formato dd/MM/aaaa.");
            }
        }
    }
}
