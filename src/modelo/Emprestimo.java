package modelo;

import java.time.LocalDate;

public class Emprestimo {
    private int id;
    private Usuario usuario;
    private Livro livro;
    private LocalDate dataEmprestimo;
    private LocalDate dataPrevista;
    private LocalDate dataDevolucao;

    // Criação de um empréstimo novo: só a Biblioteca faz isso (pacote modelo).
    Emprestimo(Usuario usuario, Livro livro, LocalDate dataEmprestimo) {
        this.usuario = usuario;
        this.livro = livro;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevista = dataEmprestimo.plusDays(usuario.prazoDias());
    }

    // Recria um empréstimo já gravado (usado pelo DAO ao ler do banco).
    public Emprestimo(int id, Usuario usuario, Livro livro, LocalDate dataEmprestimo,
                      LocalDate dataPrevista, LocalDate dataDevolucao) {
        this.id = id;
        this.usuario = usuario;
        this.livro = livro;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevista = dataPrevista;
        this.dataDevolucao = dataDevolucao;
    }

    void registrarDevolucao(LocalDate data) {
        this.dataDevolucao = data;
    }

    public boolean estaEmAberto() {
        return dataDevolucao == null;
    }

    public int getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Livro getLivro() { return livro; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataPrevista() { return dataPrevista; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
}
