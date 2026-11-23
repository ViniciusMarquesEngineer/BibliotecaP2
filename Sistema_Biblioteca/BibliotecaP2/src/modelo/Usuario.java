package modelo;

public abstract class Usuario {
    private int id;
    private String nome;
    private String matricula;

    public Usuario(int id, String nome, String matricula) {
        this.id = id;
        this.nome = nome;
        this.matricula = matricula;
    }

    public abstract int prazoDias();

    public abstract int limiteEmprestimos();

    public abstract double calcularMulta(int dias);

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
}
