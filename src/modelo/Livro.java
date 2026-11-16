package modelo;

import modelo.excecao.LivroIndisponivelException;

public class Livro {
    private int id;
    private String titulo;
    private String autor;
    private String isbn;
    private int quantidadeTotal;
    private int quantidadeExemplares;

    public Livro(int id, String titulo, String autor, String isbn, int quantidadeExemplares) {
        this(id, titulo, autor, isbn, quantidadeExemplares, quantidadeExemplares);
    }

    public Livro(int id, String titulo, String autor, String isbn,
                 int quantidadeTotal, int quantidadeExemplares) {
        if (quantidadeTotal < 0 || quantidadeExemplares < 0) {
            throw new IllegalArgumentException("Quantidade de exemplares não pode ser negativa.");
        }
        if (quantidadeExemplares > quantidadeTotal) {
            throw new IllegalArgumentException(
                    "Exemplares livres não podem ser mais que o total de exemplares.");
        }
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeExemplares = quantidadeExemplares;
    }

    public void emprestarExemplar() throws LivroIndisponivelException {
        if (quantidadeExemplares <= 0) {
            throw new LivroIndisponivelException(
                    "O livro \"" + titulo + "\" não tem exemplar livre.");
        }
        quantidadeExemplares--;
    }

    public void devolverExemplar() {
        quantidadeExemplares++;
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public String getIsbn() { return isbn; }
    public int getQuantidadeTotal() { return quantidadeTotal; }
    public int getQuantidadeExemplares() { return quantidadeExemplares; }
}
