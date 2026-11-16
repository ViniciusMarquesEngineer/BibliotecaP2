package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modelo.Emprestimo;
import modelo.Livro;
import modelo.Usuario;
import modelo.excecao.PersistenciaException;
import modelo.excecao.RegistroNaoEncontradoException;

/**
 * DAO (acesso a dados) da tabela emprestimo.
 *
 * Gravar um empréstimo (ou uma devolução) muda duas tabelas: emprestimo e livro
 * (quantidade de exemplares livres). Por isso os dois passos são feitos numa
 * transação: ou os dois ficam gravados, ou nenhum.
 * Erros de SQL (SQLException) são convertidos em PersistenciaException.
 */
public class EmprestimoDAO {

    // Usado para gravar a nova quantidade de exemplares do livro na mesma transação.
    private final LivroDAO livroDAO = new LivroDAO();

    /**
     * Grava um empréstimo novo e a nova quantidade de exemplares do livro (RF1.3).
     * As datas são gravadas como texto no formato ISO (AAAA-MM-DD).
     * O empréstimo já deve ter sido criado pela Biblioteca, que valida limite e exemplares.
     *
     * @return o id gerado pelo banco para o empréstimo
     * @throws PersistenciaException se der erro no banco (nesse caso nada fica gravado)
     */
    public int inserir(Emprestimo emprestimo) throws PersistenciaException {
        Connection c = ConexaoBD.obter();
        String sql = "INSERT INTO emprestimo (usuario_id, livro_id, data_emprestimo, data_prevista) "
                + "VALUES (?, ?, ?, ?)";
        // Fica true só se a transação terminar com commit; o finally usa isso para decidir o rollback.
        boolean gravou = false;
        try {
            // Começa a transação: nada vale até o commit.
            c.setAutoCommit(false);
            int id;
            // Passo 1: grava o empréstimo.
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, emprestimo.getUsuario().getId());
                ps.setInt(2, emprestimo.getLivro().getId());
                ps.setString(3, emprestimo.getDataEmprestimo().toString());
                ps.setString(4, emprestimo.getDataPrevista().toString());
                ps.executeUpdate();
                // Lê o id que o banco gerou.
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    chaves.next();
                    id = chaves.getInt(1);
                }
            }
            // Passo 2: grava a nova quantidade de exemplares livres do livro (já diminuída no modelo).
            livroDAO.atualizarExemplares(c, emprestimo.getLivro());
            // Os dois passos deram certo: confirma tudo de uma vez.
            c.commit();
            gravou = true;
            return id;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao gravar o empréstimo: " + e.getMessage(), e);
        } finally {
            encerrarTransacao(c, gravou);
        }
    }

    /**
     * Grava a devolução (data e multa) e a nova quantidade de exemplares do livro (RF1.4).
     * O empréstimo já deve ter sido devolvido no modelo (Biblioteca.devolver),
     * que calcula a multa.
     *
     * @param emprestimo empréstimo já devolvido no modelo
     * @param multa      valor da multa calculado pela Biblioteca
     * @throws RegistroNaoEncontradoException se não existir empréstimo com esse id
     * @throws PersistenciaException se der erro no banco (nesse caso nada fica gravado)
     */
    public void registrarDevolucao(Emprestimo emprestimo, double multa)
            throws RegistroNaoEncontradoException, PersistenciaException {
        Connection c = ConexaoBD.obter();
        // Fica true só se a transação terminar com commit; o finally usa isso para decidir o rollback.
        boolean gravou = false;
        try {
            // Começa a transação: nada vale até o commit.
            c.setAutoCommit(false);
            // Passo 1: grava a data da devolução e a multa cobrada no empréstimo.
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE emprestimo SET data_devolucao = ?, multa_cobrada = ? WHERE id = ?")) {
                ps.setString(1, emprestimo.getDataDevolucao().toString());
                ps.setDouble(2, multa);
                ps.setInt(3, emprestimo.getId());
                // 0 linhas alteradas quer dizer que o id não existe (o finally desfaz a transação).
                if (ps.executeUpdate() == 0) {
                    throw new RegistroNaoEncontradoException("Empréstimo " + emprestimo.getId() + " não encontrado.");
                }
            }
            // Passo 2: grava a nova quantidade de exemplares livres do livro (já aumentada no modelo).
            livroDAO.atualizarExemplares(c, emprestimo.getLivro());
            // Os dois passos deram certo: confirma tudo de uma vez.
            c.commit();
            gravou = true;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao gravar a devolução: " + e.getMessage(), e);
        } finally {
            encerrarTransacao(c, gravou);
        }
    }

    /**
     * Lê todos os empréstimos gravados, em ordem de id.
     *
     * Recebe os usuários e livros já carregados para que cada empréstimo aponte para
     * os mesmos objetos que estão nessas listas. Isso é importante porque a Biblioteca
     * conta os empréstimos de um usuário comparando objetos.
     *
     * @param usuarios usuários já lidos do banco
     * @param livros   livros já lidos do banco
     * @throws PersistenciaException se der erro no banco ou se um empréstimo apontar
     *                               para usuário ou livro que não existe
     */
    public List<Emprestimo> listar(List<Usuario> usuarios, List<Livro> livros) throws PersistenciaException {
        // Mapas id -> objeto, para achar rápido o usuário e o livro de cada empréstimo.
        Map<Integer, Usuario> usuarioPorId = new HashMap<>();
        for (Usuario u : usuarios) {
            usuarioPorId.put(u.getId(), u);
        }
        Map<Integer, Livro> livroPorId = new HashMap<>();
        for (Livro l : livros) {
            livroPorId.put(l.getId(), l);
        }

        List<Emprestimo> emprestimos = new ArrayList<>();
        String sql = "SELECT id, usuario_id, livro_id, data_emprestimo, data_prevista, data_devolucao "
                + "FROM emprestimo ORDER BY id";
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                // Troca os ids gravados pelos objetos já carregados.
                Usuario usuario = usuarioPorId.get(rs.getInt("usuario_id"));
                Livro livro = livroPorId.get(rs.getInt("livro_id"));
                if (usuario == null || livro == null) {
                    throw new PersistenciaException(
                            "O empréstimo " + id + " aponta para usuário ou livro inexistente.", null);
                }
                // data_devolucao é vazia (null) enquanto o livro não foi devolvido.
                String devolucao = rs.getString("data_devolucao");
                emprestimos.add(new Emprestimo(id, usuario, livro,
                        LocalDate.parse(rs.getString("data_emprestimo")),
                        LocalDate.parse(rs.getString("data_prevista")),
                        devolucao == null ? null : LocalDate.parse(devolucao)));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao ler os empréstimos: " + e.getMessage(), e);
        }
        return emprestimos;
    }

    /**
     * Termina a transação: se algo falhou, desfaz (rollback); em qualquer caso volta
     * ao modo normal, em que cada comando é gravado na hora.
     *
     * @param gravou true se o commit foi feito
     */
    private void encerrarTransacao(Connection c, boolean gravou) {
        try {
            if (!gravou) {
                c.rollback();
            }
            c.setAutoCommit(true);
        } catch (SQLException ignorada) {
            // Nada a fazer: a exceção original já está sendo propagada.
        }
    }
}
