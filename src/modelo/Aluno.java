package modelo;

public class Aluno extends Usuario {
    private static final int PRAZO_DIAS = 7;
    private static final int LIMITE = 3;
    private static final double MULTA_POR_DIA = 1.00;

    public Aluno(int id, String nome, String matricula) {
        super(id, nome, matricula);
    }

    @Override
    public int prazoDias() {
        return PRAZO_DIAS;
    }

    @Override
    public int limiteEmprestimos() {
        return LIMITE;
    }

    @Override
    public double calcularMulta(int dias) {
        return dias > 0 ? dias * MULTA_POR_DIA : 0.0;
    }
}
