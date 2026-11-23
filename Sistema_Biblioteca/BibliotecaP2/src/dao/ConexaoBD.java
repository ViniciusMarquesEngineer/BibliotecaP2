package dao;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import modelo.excecao.PersistenciaException;

/**
 * Cuida da conexão com o banco SQLite (arquivo biblioteca.db).
 *
 * Responsabilidades:
 *  - abrir a conexão uma única vez e reaproveitá-la em todos os DAOs;
 *  - criar as tabelas na primeira execução do programa;
 *  - fechar a conexão quando o programa termina.
 *
 * É uma classe utilitária: não se cria objeto dela, só se chamam os métodos estáticos.
 */
public final class ConexaoBD {

    // Endereço do banco. O arquivo biblioteca.db é criado na pasta onde o programa é executado.
    private static final String URL = "jdbc:sqlite:biblioteca.db";

    // Conexão compartilhada por todos os DAOs. Fica nula até a primeira chamada de obter().
    private static Connection conexao;

    // Tabela de usuários (alunos e professores). A coluna tipo guarda qual é a classe do objeto.
    private static final String TABELA_USUARIO = """
            CREATE TABLE IF NOT EXISTS usuario (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                nome      TEXT NOT NULL,
                matricula TEXT NOT NULL UNIQUE,
                tipo      TEXT NOT NULL CHECK (tipo IN ('ALUNO', 'PROFESSOR'))
            )""";

    // Tabela de livros. Guarda o total de exemplares e quantos estão livres.
    // O CHECK final impede ter mais exemplares livres do que o total.
    private static final String TABELA_LIVRO = """
            CREATE TABLE IF NOT EXISTS livro (
                id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo                 TEXT NOT NULL,
                autor                  TEXT NOT NULL,
                isbn                   TEXT,
                quantidade_total       INTEGER NOT NULL CHECK (quantidade_total >= 0),
                exemplares_disponiveis INTEGER NOT NULL CHECK (exemplares_disponiveis >= 0),
                CHECK (exemplares_disponiveis <= quantidade_total)
            )""";

    // Tabela de empréstimos. data_devolucao e multa_cobrada ficam vazias até o livro voltar.
    // ON DELETE CASCADE: ao apagar um usuário ou livro, o histórico dele é apagado junto.
    private static final String TABELA_EMPRESTIMO = """
            CREATE TABLE IF NOT EXISTS emprestimo (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id      INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                livro_id        INTEGER NOT NULL REFERENCES livro(id) ON DELETE CASCADE,
                data_emprestimo TEXT NOT NULL,
                data_prevista   TEXT NOT NULL,
                data_devolucao  TEXT,
                multa_cobrada   REAL
            )""";

    // Construtor privado: impede criar objetos desta classe.
    private ConexaoBD() {
    }

    /**
     * Devolve a conexão com o banco. Na primeira chamada abre o arquivo e cria as tabelas
     * (se ainda não existirem). Nas chamadas seguintes reaproveita a mesma conexão.
     *
     * @throws PersistenciaException se o banco não puder ser aberto (por exemplo, driver ausente)
     */
    public static synchronized Connection obter() throws PersistenciaException {
        try {
            // Só abre uma conexão nova se ainda não existe ou se foi fechada.
            if (conexao == null || conexao.isClosed()) {
                Connection nova = abrirConexao();
                try (Statement st = nova.createStatement()) {
                    // O SQLite só respeita chaves estrangeiras (e o CASCADE) se isto estiver ligado.
                    st.execute("PRAGMA foreign_keys = ON");
                    // CREATE TABLE IF NOT EXISTS: cria só na primeira execução.
                    st.execute(TABELA_USUARIO);
                    st.execute(TABELA_LIVRO);
                    st.execute(TABELA_EMPRESTIMO);
                }
                // Só guarda a conexão depois que tudo deu certo, para não ficar meio configurada.
                conexao = nova;
            }
            return conexao;
        } catch (SQLException e) {
            // O resto do sistema não precisa conhecer SQLException: converte para erro do sistema.
            throw new PersistenciaException(mensagemDeAbertura(e), e);
        }
    }

    /**
     * Abre a conexão com o arquivo do banco. Se o driver do SQLite não estiver no classpath
     * (comum ao rodar pela IDE), procura o arquivo sqlite-jdbc*.jar na pasta lib e o usa.
     */
    // Texto curto para o usuário: o que houve, sem detalhes técnicos do SQL.
    private static String mensagemDeAbertura(SQLException e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.startsWith("Driver") || m.startsWith("O driver")) {
            return "Não foi possível abrir o banco de dados. " + m;
        }
        if (m.contains("unable to open") || m.contains("readonly") || m.contains("CANTOPEN")) {
            return "Não foi possível abrir o banco de dados. Sem permissão para gravar nesta pasta.";
        }
        return "Não foi possível abrir o banco de dados.";
    }

    private static Connection abrirConexao() throws SQLException {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException semDriver) {
            Driver driver = carregarDriverDaPastaLib();
            if (driver == null) {
                throw new SQLException("Driver do SQLite não encontrado. Coloque o .jar na pasta lib.", semDriver);
            }
            Connection c = driver.connect(URL, new Properties());
            if (c == null) {
                throw new SQLException("O driver da pasta lib não é compatível.");
            }
            return c;
        }
    }

    // Procura o .jar do driver em volta do programa e carrega a classe do driver dele.
    private static Driver carregarDriverDaPastaLib() {
        List<File> pastas = new ArrayList<>();
        // Sobe algumas pastas a partir do diretório atual e do local do programa (a IDE costuma
        // rodar de pastas como out/production/..., longe da pasta lib do projeto).
        adicionarPastasAcima(pastas, new File("").getAbsoluteFile());
        try {
            File local = new File(ConexaoBD.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            adicionarPastasAcima(pastas, local.isFile() ? local.getParentFile() : local);
        } catch (Exception ignorada) {
            // Sem o local do programa, só o diretório atual é usado.
        }
        // Cópia do driver que o Maven ou o Gradle possam ter baixado.
        String casa = System.getProperty("user.home");
        if (casa != null) {
            pastas.add(new File(casa, ".m2/repository/org/xerial/sqlite-jdbc"));
        }
        for (File pasta : pastas) {
            for (File jar : acharJars(pasta, 3)) {
                try {
                    URLClassLoader carregador = new URLClassLoader(new URL[] {jar.toURI().toURL()},
                            ConexaoBD.class.getClassLoader());
                    Class<?> classe = Class.forName("org.sqlite.JDBC", true, carregador);
                    return (Driver) classe.getDeclaredConstructor().newInstance();
                } catch (Exception ignorada) {
                    // Este jar não serviu; tenta o próximo.
                }
            }
        }
        return null;
    }

    // Acrescenta a pasta, a pasta lib dela e as 6 pastas acima (e suas lib) à lista de busca.
    private static void adicionarPastasAcima(List<File> pastas, File inicio) {
        File atual = inicio;
        for (int i = 0; i < 7 && atual != null; i++) {
            pastas.add(new File(atual, "lib"));
            pastas.add(atual);
            atual = atual.getParentFile();
        }
    }

    // Lista os arquivos sqlite-jdbc*.jar da pasta (e de subpastas, até a profundidade dada).
    private static List<File> acharJars(File pasta, int profundidade) {
        List<File> achados = new ArrayList<>();
        File[] itens = pasta.listFiles();
        if (itens == null) {
            return achados;
        }
        for (File f : itens) {
            if (f.isFile() && f.getName().startsWith("sqlite-jdbc") && f.getName().endsWith(".jar")) {
                achados.add(f);
            } else if (f.isDirectory() && profundidade > 0 && pasta.getName().startsWith("sqlite-jdbc")) {
                achados.addAll(acharJars(f, profundidade - 1));
            }
        }
        return achados;
    }

    /**
     * Fecha a conexão, se estiver aberta. É chamado quando o programa termina.
     *
     * @throws PersistenciaException se der erro ao fechar
     */
    public static synchronized void fechar() throws PersistenciaException {
        try {
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível fechar o banco de dados.", e);
        }
    }
}
