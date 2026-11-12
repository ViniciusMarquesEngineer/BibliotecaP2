package modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import modelo.excecao.LimiteEmprestimosException;
import modelo.excecao.LivroIndisponivelException;

public class Biblioteca {
    private final List<Livro> livros;
    private final List<Usuario> usuarios;
    private final List<Emprestimo> emprestimos = new ArrayList<>();

    public Biblioteca(List<Livro> livros, List<Usuario> usuarios) {
        this.livros = livros;
        this.usuarios = usuarios;
    }

    public Emprestimo emprestar(Usuario usuario, Livro livro)
            throws LimiteEmprestimosException, LivroIndisponivelException {
        int emAberto = 0;
        for (Emprestimo e : emprestimos) {
            if (e.getUsuario() == usuario && e.estaEmAberto()) {
                emAberto++;
            }
        }
        if (emAberto >= usuario.limiteEmprestimos()) {
            throw new LimiteEmprestimosException(usuario.getNome()
                    + " já atingiu o limite de " + usuario.limiteEmprestimos() + " empréstimos.");
        }
        livro.emprestarExemplar();
        Emprestimo emprestimo = new Emprestimo(usuario, livro, LocalDate.now());
        emprestimos.add(emprestimo);
        return emprestimo;
    }

    public double devolver(Emprestimo emprestimo) {
        return devolver(emprestimo, LocalDate.now());
    }

    public double devolver(Emprestimo emprestimo, LocalDate dataDevolucao) {
        if (!emprestimo.estaEmAberto()) {
            throw new IllegalStateException("Este empréstimo já foi devolvido.");
        }
        emprestimo.registrarDevolucao(dataDevolucao);
        emprestimo.getLivro().devolverExemplar();
        long atraso = ChronoUnit.DAYS.between(emprestimo.getDataPrevista(), dataDevolucao);
        return emprestimo.getUsuario().calcularMulta((int) Math.max(0, atraso));
    }

    public List<Livro> getLivros() { return Collections.unmodifiableList(livros); }
    public List<Usuario> getUsuarios() { return Collections.unmodifiableList(usuarios); }
    public List<Emprestimo> getEmprestimos() { return Collections.unmodifiableList(emprestimos); }
}
