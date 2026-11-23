package arquivo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import modelo.Emprestimo;

/**
 * Gera o mesmo relatório em uma planilha do Excel (.xlsx), com os títulos em negrito
 * e colunas largas o bastante para mostrar datas e textos por inteiro.
 * O arquivo .xlsx é um zip de arquivos XML; aqui ele é montado direto, sem bibliotecas extras.
 */
public class RelatorioExcel {

    private static final String[] TITULOS = {"ID", "Usuário", "Matrícula", "Livro", "Data do empréstimo",
            "Data prevista de devolução", "Data da devolução", "Situação", "Multa (R$)"};

    // Largura de cada coluna, em caracteres.
    private static final int[] LARGURAS = {8, 30, 16, 38, 22, 30, 22, 16, 14};

    private RelatorioExcel() {
    }

    /**
     * Grava em {@code destino} os empréstimos cuja data de empréstimo está entre
     * {@code inicio} e {@code fim} (inclusive). Devolve quantos empréstimos entraram na planilha.
     */
    public static int gerar(List<Emprestimo> emprestimos, LocalDate inicio, LocalDate fim, Path destino)
            throws IOException {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }
        StringBuilder linhas = new StringBuilder();
        linhas.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        for (int c = 0; c < TITULOS.length; c++) {
            linhas.append(celulaTexto(c, 1, TITULOS[c], 1));
        }
        linhas.append("</row>");

        int total = 0;
        for (Emprestimo e : emprestimos) {
            if (e.getDataEmprestimo().isBefore(inicio) || e.getDataEmprestimo().isAfter(fim)) {
                continue;
            }
            total++;
            int linha = total + 1;
            String[] v = RelatorioCSV.valores(e);
            linhas.append("<row r=\"").append(linha).append("\">");
            linhas.append(celulaNumero(0, linha, v[0], 0));
            for (int c = 1; c <= 7; c++) {
                linhas.append(celulaTexto(c, linha, v[c], 0));
            }
            // A multa vai como número (com vírgula decimal trocada por ponto no arquivo).
            if (v[8].isEmpty()) {
                linhas.append(celulaTexto(8, linha, "", 0));
            } else {
                linhas.append(celulaNumero(8, linha, v[8].replace(',', '.'), 2));
            }
            linhas.append("</row>");
        }

        try (OutputStream saida = Files.newOutputStream(destino);
             ZipOutputStream zip = new ZipOutputStream(saida, StandardCharsets.UTF_8)) {
            escrever(zip, "[Content_Types].xml", CONTENT_TYPES);
            escrever(zip, "_rels/.rels", RELS);
            escrever(zip, "xl/workbook.xml", WORKBOOK);
            escrever(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            escrever(zip, "xl/styles.xml", ESTILOS);
            escrever(zip, "xl/worksheets/sheet1.xml", planilha(linhas.toString()));
        }
        return total;
    }

    // ---------- montagem dos XMLs ----------

    private static String planilha(String linhas) {
        StringBuilder colunas = new StringBuilder("<cols>");
        for (int c = 0; c < LARGURAS.length; c++) {
            colunas.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(LARGURAS[c]).append("\" customWidth=\"1\"/>");
        }
        colunas.append("</cols>");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                // Congela a primeira linha (os títulos) ao rolar a planilha.
                + "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" "
                + "activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>"
                + colunas + "<sheetData>" + linhas + "</sheetData></worksheet>";
    }

    // Letra da coluna (0 = A, 1 = B ...). O relatório tem menos de 26 colunas.
    private static String referencia(int coluna, int linha) {
        return (char) ('A' + coluna) + String.valueOf(linha);
    }

    private static String celulaTexto(int coluna, int linha, String texto, int estilo) {
        return "<c r=\"" + referencia(coluna, linha) + "\" s=\"" + estilo + "\" t=\"inlineStr\"><is><t>"
                + escapar(texto) + "</t></is></c>";
    }

    private static String celulaNumero(int coluna, int linha, String numero, int estilo) {
        return "<c r=\"" + referencia(coluna, linha) + "\" s=\"" + estilo + "\"><v>" + numero + "</v></c>";
    }

    // Troca os caracteres que têm significado especial no XML.
    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void escrever(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ---------- arquivos fixos do formato .xlsx ----------

    private static final String CABECALHO_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    private static final String CONTENT_TYPES = CABECALHO_XML
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" "
            + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" "
            + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" "
            + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String RELS = CABECALHO_XML
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" "
            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" "
            + "Target=\"xl/workbook.xml\"/></Relationships>";

    private static final String WORKBOOK = CABECALHO_XML
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"Empréstimos\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";

    private static final String WORKBOOK_RELS = CABECALHO_XML
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" "
            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" "
            + "Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" "
            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" "
            + "Target=\"styles.xml\"/></Relationships>";

    // Estilo 0: normal. Estilo 1: título (negrito, fundo cinza, centralizado). Estilo 2: número com 2 casas.
    private static final String ESTILOS = CABECALHO_XML
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9E1F2\"/><bgColor indexed=\"64\"/>"
            + "</patternFill></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"3\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" "
            + "applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
