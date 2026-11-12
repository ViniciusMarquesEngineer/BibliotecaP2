package app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import modelo.Aluno;
import modelo.Biblioteca;
import modelo.Emprestimo;
import modelo.Livro;
import modelo.Professor;
import modelo.Usuario;
import modelo.excecao.LimiteEmprestimosException;
import modelo.excecao.LivroIndisponivelException;

public class Main {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) {
        List<Livro> livros = new ArrayList<>();
        livros.add(new Livro(1, "Dom Casmurro", "Machado de Assis", "978-85-359-0277-5", 3));
        livros.add(new Livro(2, "Java: Como Programar", "Deitel", "978-85-430-0479-6", 1));
        livros.add(new Livro(3, "Head First Java", "Sierra e Bates", "978-85-508-0099-3", 5));
        livros.add(new Livro(4, "Estruturas de Dados", "Cormen", "978-85-352-3699-6", 5));

        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(new Aluno(1, "Maria Silva", "20250011111"));
        usuarios.add(new Professor(2, "Ana Souza", "P-0033"));
        usuarios.add(new Aluno(3, "João Pereira", "20240022222"));

        Biblioteca biblioteca = new Biblioteca(livros, usuarios);
        Usuario maria = usuarios.get(0);
        Usuario ana = usuarios.get(1);
        Usuario joao = usuarios.get(2);

        System.out.println("=== Empréstimos e prazos ===");
        try {
            Emprestimo e1 = biblioteca.emprestar(maria, livros.get(0));
            mostrar(e1);
            Emprestimo e2 = biblioteca.emprestar(ana, livros.get(2));
            mostrar(e2);

            System.out.println("\n=== Devoluções e multas ===");
            LocalDate daqui10 = LocalDate.now().plusDays(10);
            double multaAluno = biblioteca.devolver(e1, daqui10);
            double multaProf = biblioteca.devolver(e2, daqui10);
            System.out.printf("Devolução em %s%n", daqui10.format(FMT));
            System.out.printf("  %s (aluno): multa R$ %.2f%n", maria.getNome(), multaAluno);
            System.out.printf("  %s (professor): multa R$ %.2f%n", ana.getNome(), multaProf);

            LocalDate daqui18 = LocalDate.now().plusDays(18);
            Emprestimo e3 = biblioteca.emprestar(ana, livros.get(3));
            System.out.printf("  %s devolve em %s (3 dias de atraso): multa R$ %.2f%n",
                    ana.getNome(), daqui18.format(FMT), biblioteca.devolver(e3, daqui18));
        } catch (LimiteEmprestimosException | LivroIndisponivelException ex) {
            System.out.println("Erro inesperado: " + ex.getMessage());
        }

        System.out.println("\n=== Livro sem exemplar livre ===");
        try {
            biblioteca.emprestar(maria, livros.get(1));
            biblioteca.emprestar(joao, livros.get(1));
        } catch (LivroIndisponivelException ex) {
            System.out.println("Exceção tratada: " + ex.getMessage());
        } catch (LimiteEmprestimosException ex) {
            System.out.println("Erro inesperado: " + ex.getMessage());
        }
        System.out.println("Exemplares livres de \"" + livros.get(1).getTitulo() + "\": "
                + livros.get(1).getQuantidadeExemplares());

        System.out.println("\n=== Limite de empréstimos ===");
        try {
            biblioteca.emprestar(maria, livros.get(0));
            biblioteca.emprestar(maria, livros.get(2));
            System.out.println("Maria tem 3 empréstimos em aberto.");
            biblioteca.emprestar(maria, livros.get(3));
        } catch (LimiteEmprestimosException ex) {
            System.out.println("Exceção tratada: " + ex.getMessage());
        } catch (LivroIndisponivelException ex) {
            System.out.println("Erro inesperado: " + ex.getMessage());
        }

        System.out.println("\n=== Regras por tipo de usuário ===");
        for (Usuario u : biblioteca.getUsuarios()) {
            System.out.printf("%-14s prazo=%2d dias | limite=%d | multa de 3 dias = R$ %.2f%n",
                    u.getNome(), u.prazoDias(), u.limiteEmprestimos(), u.calcularMulta(3));
        }
    }

    private static void mostrar(Emprestimo e) {
        System.out.printf("%s pegou \"%s\" em %s -> devolução prevista: %s%n",
                e.getUsuario().getNome(), e.getLivro().getTitulo(),
                e.getDataEmprestimo().format(FMT), e.getDataPrevista().format(FMT));
    }
}
