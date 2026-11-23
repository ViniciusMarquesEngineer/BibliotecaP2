package arquivo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import modelo.Emprestimo;

/** Gera o relatório CSV (RF1.7) dos empréstimos feitos em um período. */
public class RelatorioCSV {
    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private RelatorioCSV() {
    }

    /**
     * Grava em {@code destino} os empréstimos cuja data de empréstimo está entre
     * {@code inicio} e {@code fim} (inclusive). Devolve quantos empréstimos entraram no arquivo.
     */
    public static int gerar(List<Emprestimo> emprestimos, LocalDate inicio, LocalDate fim, Path destino)
            throws IOException {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }
        int total = 0;
        try (BufferedWriter w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // marca UTF-8 para o Excel mostrar os acentos
            w.write("id;usuario;matricula;livro;data_emprestimo;data_prevista;data_devolucao;situacao;multa");
            w.newLine();
            for (Emprestimo e : emprestimos) {
                if (e.getDataEmprestimo().isBefore(inicio) || e.getDataEmprestimo().isAfter(fim)) {
                    continue;
                }
                w.write(linha(e));
                w.newLine();
                total++;
            }
        }
        return total;
    }

    /**
     * Valores de uma linha do relatório, na ordem das colunas: id, usuário, matrícula, livro,
     * data do empréstimo, data prevista, data da devolução (vazia se não devolveu), situação e multa.
     * Também é usado pelo relatório em Excel, para os dois arquivos terem o mesmo conteúdo.
     */
    static String[] valores(Emprestimo e) {
        String devolucao = "";
        String multa = "";
        String situacao;
        if (e.estaEmAberto()) {
            situacao = LocalDate.now().isAfter(e.getDataPrevista()) ? "Atrasado" : "Em aberto";
        } else {
            situacao = "Devolvido";
            devolucao = e.getDataDevolucao().format(FMT);
            long atraso = ChronoUnit.DAYS.between(e.getDataPrevista(), e.getDataDevolucao());
            double valor = e.getUsuario().calcularMulta((int) Math.max(0, atraso));
            multa = String.format(PT_BR, "%.2f", valor);
        }
        return new String[] {
                String.valueOf(e.getId()),
                e.getUsuario().getNome(),
                e.getUsuario().getMatricula(),
                e.getLivro().getTitulo(),
                e.getDataEmprestimo().format(FMT),
                e.getDataPrevista().format(FMT),
                devolucao,
                situacao,
                multa};
    }

    private static String linha(Emprestimo e) {
        String[] v = valores(e);
        for (int i = 0; i < v.length; i++) {
            v[i] = campo(v[i]);
        }
        return String.join(SEPARADOR, v);
    }

    // Coloca o texto entre aspas quando ele tem separador, aspas ou quebra de linha.
    private static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
