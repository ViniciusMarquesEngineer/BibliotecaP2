package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import modelo.Livro;
import modelo.excecao.OperacaoInvalidaException;
import modelo.excecao.PersistenciaException;
import modelo.excecao.RegistroNaoEncontradoException;

/**
 * DAO (acesso a dados) da tabela livro.
 *
 * Todo o SQL de livros fica aqui. O resto do sistema só chama estes métodos
 * e nunca precisa saber nada de SQL. Erros de SQL (SQLException) são convertidos
 * em PersistenciaException.
 */
public class LivroDAO {

    /**
     * Grava um livro novo no banco (RF1.1 - cadastrar).
     * O id é gerado pelo banco, por isso o método devolve uma cópia do livro já com o id.
     *
     * @param livro livro a gravar (o id dele é ignorado)
     * @return o livro gravado, com o id gerado
     * @throws PersistenciaException se der erro no banco
     */
    public Livro inserir(Livro livro) throws PersistenciaException {
        // Os "?" são preenchidos depois; isso evita erro de digitação e SQL injection.
        String sql = "INSERT INTO livro (titulo, autor, isbn, quantidade_total, exemplares_disponiveis) "
                + "VALUES (?, ?, ?, ?, ?)";
        // RETURN_GENERATED_KEYS pede ao banco o id que ele criou.
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, livro.getTitulo());
            ps.setString(2, livro.getAutor());
            ps.setString(3, livro.getIsbn());
            ps.setInt(4, livro.getQuantidadeTotal());
            ps.setInt(5, livro.getQuantidadeExemplares());
            ps.executeUpdate();
            // Lê o id gerado e monta um Livro novo com ele (Livro não tem setId).
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                chaves.next();
                return new Livro(chaves.getInt(1), livro.getTitulo(), livro.getAutor(), livro.getIsbn(),
                        livro.getQuantidadeTotal(), livro.getQuantidadeExemplares());
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao gravar o livro: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos os livros, em ordem de id (RF1.1 - listar).
     *
     * @throws PersistenciaException se der erro no banco
     */
    public List<Livro> listar() throws PersistenciaException {
        // termo null significa "sem filtro".
        return consultar("SELECT * FROM livro ORDER BY id", null);
    }

    /**
     * Busca livros cujo título ou autor contém o texto informado (RF1.5).
     *
     * @param termo parte do título ou do autor
     * @throws PersistenciaException se der erro no banco
     */
    public List<Livro> buscar(String termo) throws PersistenciaException {
        // LIKE com % dos dois lados procura o texto em qualquer parte do título ou do autor.
        return consultar("SELECT * FROM livro WHERE titulo LIKE ? OR autor LIKE ? ORDER BY id", termo);
    }

    /**
     * Procura um livro pelo id.
     *
     * @throws RegistroNaoEncontradoException se não existir livro com esse id
     * @throws PersistenciaException se der erro no banco
     */
    public Livro buscarPorId(int id) throws RegistroNaoEncontradoException, PersistenciaException {
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement("SELECT * FROM livro WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Se não veio nenhuma linha, o id não existe.
                if (!rs.next()) {
                    throw new RegistroNaoEncontradoException("Livro " + id + " não encontrado.");
                }
                return montar(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao ler o livro: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza todos os dados de um livro já gravado (RF1.1 - editar).
     * O livro é identificado pelo id que ele carrega.
     *
     * @throws RegistroNaoEncontradoException se não existir livro com esse id
     * @throws PersistenciaException se der erro no banco
     */
    public void atualizar(Livro livro) throws RegistroNaoEncontradoException, PersistenciaException {
        String sql = "UPDATE livro SET titulo = ?, autor = ?, isbn = ?, quantidade_total = ?, "
                + "exemplares_disponiveis = ? WHERE id = ?";
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement(sql)) {
            ps.setString(1, livro.getTitulo());
            ps.setString(2, livro.getAutor());
            ps.setString(3, livro.getIsbn());
            ps.setInt(4, livro.getQuantidadeTotal());
            ps.setInt(5, livro.getQuantidadeExemplares());
            ps.setInt(6, livro.getId());
            // executeUpdate devolve quantas linhas mudaram; 0 quer dizer que o id não existe.
            if (ps.executeUpdate() == 0) {
                throw new RegistroNaoEncontradoException("Livro " + livro.getId() + " não encontrado.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar o livro: " + e.getMessage(), e);
        }
    }

    /**
     * Remove um livro (RF1.1 - remover).
     * Regra RN1.5: livro com empréstimo em aberto não pode ser removido.
     *
     * @throws RegistroNaoEncontradoException se não existir livro com esse id
     * @throws OperacaoInvalidaException se o livro tiver empréstimo em aberto
     * @throws PersistenciaException se der erro no banco
     */
    public void remover(int id)
            throws RegistroNaoEncontradoException, OperacaoInvalidaException, PersistenciaException {
        Connection c = ConexaoBD.obter();
        try {
            // 1) Conta os empréstimos em aberto (sem data de devolução) desse livro.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM emprestimo WHERE livro_id = ? AND data_devolucao IS NULL")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new OperacaoInvalidaException(
                                "O livro " + id + " tem empréstimo em aberto e não pode ser removido.");
                    }
                }
            }
            // 2) Sem empréstimo em aberto, apaga o livro.
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM livro WHERE id = ?")) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) {
                    throw new RegistroNaoEncontradoException("Livro " + id + " não encontrado.");
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao remover o livro: " + e.getMessage(), e);
        }
    }

    /**
     * Grava só a quantidade de exemplares livres de um livro.
     * É usado pelo EmprestimoDAO para gravar o empréstimo (ou a devolução) e o estoque
     * na mesma transação. Por isso recebe a conexão já aberta e não tem "public":
     * só as classes do pacote dao enxergam este método.
     */
    void atualizarExemplares(Connection c, Livro livro) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE livro SET exemplares_disponiveis = ? WHERE id = ?")) {
            ps.setInt(1, livro.getQuantidadeExemplares());
            ps.setInt(2, livro.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Executa uma consulta que devolve livros e transforma cada linha em um objeto Livro.
     * Serve para listar() e buscar(), que só mudam o SQL.
     *
     * @param sql   consulta a executar
     * @param termo texto da busca; se for null, a consulta não usa parâmetros
     */
    private List<Livro> consultar(String sql, String termo) throws PersistenciaException {
        List<Livro> livros = new ArrayList<>();
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement(sql)) {
            if (termo != null) {
                // O mesmo texto vale para o título (?1) e para o autor (?2).
                ps.setString(1, "%" + termo + "%");
                ps.setString(2, "%" + termo + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    livros.add(montar(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao ler os livros: " + e.getMessage(), e);
        }
        return livros;
    }

    /** Transforma a linha atual do ResultSet em um objeto Livro. */
    private Livro montar(ResultSet rs) throws SQLException {
        return new Livro(rs.getInt("id"), rs.getString("titulo"), rs.getString("autor"),
                rs.getString("isbn"), rs.getInt("quantidade_total"), rs.getInt("exemplares_disponiveis"));
    }
}
