package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import modelo.Aluno;
import modelo.Professor;
import modelo.Usuario;
import modelo.excecao.OperacaoInvalidaException;
import modelo.excecao.PersistenciaException;
import modelo.excecao.RegistroNaoEncontradoException;

/**
 * DAO (acesso a dados) da tabela usuario.
 *
 * Usuario é abstrata, então o banco guarda na coluna "tipo" se a linha é um
 * ALUNO ou um PROFESSOR. Ao ler, este DAO recria o objeto da classe certa.
 * Erros de SQL (SQLException) são convertidos em PersistenciaException.
 */
public class UsuarioDAO {

    /**
     * Grava um usuário novo (RF1.2 - cadastrar).
     * O id é gerado pelo banco, por isso o método devolve uma cópia já com o id.
     *
     * @param usuario aluno ou professor a gravar (o id dele é ignorado)
     * @return o usuário gravado, com o id gerado
     * @throws OperacaoInvalidaException se já existir usuário com a mesma matrícula
     * @throws PersistenciaException se der erro no banco
     */
    public Usuario inserir(Usuario usuario) throws OperacaoInvalidaException, PersistenciaException {
        Connection c = ConexaoBD.obter();
        String sql = "INSERT INTO usuario (nome, matricula, tipo) VALUES (?, ?, ?)";
        try {
            // Confere antes se a matrícula já existe, para dar uma mensagem clara.
            // Id 0 nunca existe no banco, então nenhum usuário é ignorado na conferência.
            verificarMatriculaLivre(c, usuario.getMatricula(), 0);
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getMatricula());
                ps.setString(3, tipoDe(usuario));
                ps.executeUpdate();
                // Lê o id gerado e cria um Aluno ou Professor novo com ele.
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    chaves.next();
                    return criar(tipoDe(usuario), chaves.getInt(1), usuario.getNome(), usuario.getMatricula());
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao gravar o usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todos os usuários, em ordem de id (RF1.2 - listar).
     *
     * @throws PersistenciaException se der erro no banco
     */
    public List<Usuario> listar() throws PersistenciaException {
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement("SELECT * FROM usuario ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(montar(rs));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao ler os usuários: " + e.getMessage(), e);
        }
        return usuarios;
    }

    /**
     * Procura um usuário pelo id.
     *
     * @throws RegistroNaoEncontradoException se não existir usuário com esse id
     * @throws PersistenciaException se der erro no banco
     */
    public Usuario buscarPorId(int id) throws RegistroNaoEncontradoException, PersistenciaException {
        try (PreparedStatement ps = ConexaoBD.obter().prepareStatement("SELECT * FROM usuario WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Se não veio nenhuma linha, o id não existe.
                if (!rs.next()) {
                    throw new RegistroNaoEncontradoException("Usuário " + id + " não encontrado.");
                }
                return montar(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao ler o usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza nome, matrícula e tipo de um usuário já gravado (RF1.2 - editar).
     * O usuário é identificado pelo id que ele carrega.
     *
     * @throws RegistroNaoEncontradoException se não existir usuário com esse id
     * @throws OperacaoInvalidaException se a matrícula já for de outro usuário
     * @throws PersistenciaException se der erro no banco
     */
    public void atualizar(Usuario usuario)
            throws RegistroNaoEncontradoException, OperacaoInvalidaException, PersistenciaException {
        Connection c = ConexaoBD.obter();
        try {
            // A matrícula pode continuar a mesma; por isso o próprio usuário é ignorado na conferência.
            verificarMatriculaLivre(c, usuario.getMatricula(), usuario.getId());
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE usuario SET nome = ?, matricula = ?, tipo = ? WHERE id = ?")) {
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getMatricula());
                ps.setString(3, tipoDe(usuario));
                ps.setInt(4, usuario.getId());
                // 0 linhas alteradas quer dizer que o id não existe.
                if (ps.executeUpdate() == 0) {
                    throw new RegistroNaoEncontradoException("Usuário " + usuario.getId() + " não encontrado.");
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar o usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Remove um usuário (RF1.2 - remover).
     * Usuário com empréstimo em aberto não pode ser removido.
     *
     * @throws RegistroNaoEncontradoException se não existir usuário com esse id
     * @throws OperacaoInvalidaException se o usuário tiver empréstimo em aberto
     * @throws PersistenciaException se der erro no banco
     */
    public void remover(int id)
            throws RegistroNaoEncontradoException, OperacaoInvalidaException, PersistenciaException {
        Connection c = ConexaoBD.obter();
        try {
            // 1) Conta os empréstimos em aberto (sem data de devolução) desse usuário.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM emprestimo WHERE usuario_id = ? AND data_devolucao IS NULL")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new OperacaoInvalidaException(
                                "O usuário " + id + " tem empréstimo em aberto e não pode ser removido.");
                    }
                }
            }
            // 2) Sem empréstimo em aberto, apaga o usuário.
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) {
                    throw new RegistroNaoEncontradoException("Usuário " + id + " não encontrado.");
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao remover o usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Confere se a matrícula ainda não pertence a outro usuário.
     *
     * @param idIgnorado id do usuário que está sendo editado (ele pode manter a própria matrícula);
     *                   use 0 quando for um cadastro novo
     * @throws OperacaoInvalidaException se outro usuário já usa a matrícula
     */
    private void verificarMatriculaLivre(Connection c, String matricula, int idIgnorado)
            throws SQLException, OperacaoInvalidaException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM usuario WHERE matricula = ? AND id <> ?")) {
            ps.setString(1, matricula);
            ps.setInt(2, idIgnorado);
            try (ResultSet rs = ps.executeQuery()) {
                // Se veio alguma linha, outro usuário já tem essa matrícula.
                if (rs.next()) {
                    throw new OperacaoInvalidaException("Já existe um usuário com a matrícula " + matricula + ".");
                }
            }
        }
    }

    /** Objeto -> texto gravado na coluna tipo ("ALUNO" ou "PROFESSOR"). */
    private String tipoDe(Usuario usuario) {
        if (usuario instanceof Aluno) {
            return "ALUNO";
        }
        if (usuario instanceof Professor) {
            return "PROFESSOR";
        }
        throw new IllegalArgumentException("Tipo de usuário não suportado: " + usuario.getClass().getSimpleName());
    }

    /** Texto gravado na coluna tipo -> objeto da classe certa (Aluno ou Professor). */
    private Usuario criar(String tipo, int id, String nome, String matricula) {
        return switch (tipo) {
            case "ALUNO" -> new Aluno(id, nome, matricula);
            case "PROFESSOR" -> new Professor(id, nome, matricula);
            default -> throw new IllegalStateException("Tipo de usuário desconhecido no banco: " + tipo);
        };
    }

    /** Transforma a linha atual do ResultSet em um Aluno ou Professor. */
    private Usuario montar(ResultSet rs) throws SQLException {
        return criar(rs.getString("tipo"), rs.getInt("id"), rs.getString("nome"), rs.getString("matricula"));
    }
}
