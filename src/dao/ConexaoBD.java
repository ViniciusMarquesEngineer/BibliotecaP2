package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
                Connection nova = DriverManager.getConnection(URL);
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
            throw new PersistenciaException("Não foi possível abrir o banco de dados: " + e.getMessage(), e);
        }
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
            throw new PersistenciaException("Erro ao fechar o banco de dados: " + e.getMessage(), e);
        }
    }
}
