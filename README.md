<<<<<<< Updated upstream
# BibliotecaP2
=======
# Sistema de Controle de Biblioteca

Projeto da disciplina de Modelagem e Programação Orientada a Objetos (UFRPE/UABJ, 2026.2).

O sistema controla empréstimos e devoluções de livros para alunos e professores. Cada tipo de usuário tem prazo, limite de empréstimos e multa por atraso diferentes. Na Entrega 3 os dados passam a ser gravados em banco **SQLite**, acessado por **JDBC**, e o programa roda no console por um menu. O relatório de empréstimos em **CSV** é gerado quando o usuário pede.

## Regras

| | Aluno | Professor |
|---|---|---|
| Prazo de devolução | 7 dias | 15 dias |
| Máximo de empréstimos em aberto | 3 | 5 |
| Multa por dia de atraso | R$ 1,00 | R$ 0,50 |

- A multa conta a partir do dia seguinte à data prevista.
- Livro sem exemplar livre não pode ser emprestado (`LivroIndisponivelException`).
- Usuário no limite de empréstimos não pode pegar outro livro (`LimiteEmprestimosException`).
- Livro com empréstimo em aberto não pode ser removido (`OperacaoInvalidaException`). O mesmo vale para usuário com empréstimo em aberto.
- Não podem existir dois usuários com a mesma matrícula (`OperacaoInvalidaException`).
- Id que não existe no banco gera `RegistroNaoEncontradoException`.
- Erro de SQL vira `PersistenciaException`, então o menu não precisa saber nada de SQL.

## Estrutura

```
BibliotecaP2/
├── lib/
│   └── sqlite-jdbc-<versão>.jar       driver JDBC do SQLite (baixar, veja abaixo)
├── src/
│   ├── app/
│   │   ├── MenuConsole.java           menu de console (Entrega 3)
│   │   └── Main.java                  demonstração em memória (Entrega 2)
│   ├── modelo/
│   │   ├── Biblioteca.java
│   │   ├── Usuario.java               classe abstrata
│   │   ├── Aluno.java
│   │   ├── Professor.java
│   │   ├── Livro.java
│   │   ├── Emprestimo.java
│   │   └── excecao/
│   │       ├── LivroIndisponivelException.java
│   │       ├── LimiteEmprestimosException.java
│   │       ├── RegistroNaoEncontradoException.java
│   │       ├── OperacaoInvalidaException.java
│   │       └── PersistenciaException.java
│   ├── dao/
│   │   ├── ConexaoBD.java             abre o banco e cria as tabelas
│   │   ├── LivroDAO.java
│   │   ├── UsuarioDAO.java
│   │   └── EmprestimoDAO.java
│   └── arquivo/
│       └── RelatorioCSV.java          gera o relatório CSV
├── uml_atualizado.png                 diagrama de classes da Entrega 2
├── uml_entrega3.png                   diagrama de classes atualizado (Entrega 3)
├── saida_main.txt                     saída do Main (Entrega 2)
└── README.md
```

O arquivo `biblioteca.db` e os relatórios `relatorio_emprestimos_*.csv` são criados na pasta em que o programa é executado.

## Banco de dados

SQLite, no arquivo `biblioteca.db`. As tabelas são criadas sozinhas na primeira execução. Datas são gravadas como texto no formato `AAAA-MM-DD`. As chaves estrangeiras ficam ligadas (`PRAGMA foreign_keys = ON`).

| Tabela | Colunas |
|---|---|
| `usuario` | `id` (PK), `nome`, `matricula` (única), `tipo` (`ALUNO` ou `PROFESSOR`) |
| `livro` | `id` (PK), `titulo`, `autor`, `isbn`, `quantidade_total`, `exemplares_disponiveis` |
| `emprestimo` | `id` (PK), `usuario_id` (FK), `livro_id` (FK), `data_emprestimo`, `data_prevista`, `data_devolucao` (vazia até devolver), `multa_cobrada` |

- Cada tabela tem um DAO: `UsuarioDAO`, `LivroDAO` e `EmprestimoDAO`.
- Gravar um empréstimo e baixar o exemplar do livro acontece numa única transação. Na devolução também. Se um passo falhar, nada fica gravado.
- Ao remover um livro ou usuário sem empréstimo em aberto, o histórico de empréstimos já devolvidos dele é removido junto (`ON DELETE CASCADE`).
- Os dados continuam lá depois que o programa fecha.

## Como executar

Requisitos: JDK 17 ou mais novo (o `javac` precisa estar disponível) e o driver `sqlite-jdbc`.

**1. Baixar o driver.** Baixe o arquivo `sqlite-jdbc-<versão>.jar` em https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/ (uma versão recente) e coloque na pasta `lib` do projeto. Se o programa reclamar de `org.slf4j`, coloque também o jar `slf4j-api` em `lib`.

**2. Compilar e rodar.** Abra o terminal **na pasta `BibliotecaP2`** (a que contém `src`).

Windows:

```
mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\app\MenuConsole.java
java -cp "out;lib/*" app.MenuConsole
```

Linux ou macOS:

```
mkdir -p out
javac -encoding UTF-8 -sourcepath src -d out src/app/MenuConsole.java
java -cp "out:lib/*" app.MenuConsole
```

Se os acentos saírem errados no Windows, rode `chcp 65001` e use:

```
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out;lib/*" app.MenuConsole
```

A demonstração em memória da Entrega 2 continua disponível e não usa banco nem driver:

```
javac -encoding UTF-8 -sourcepath src -d out src/app/Main.java
java -cp out app.Main
```

## O que o menu faz

1. **Livros**: cadastrar, listar, buscar por título ou autor, editar e remover (RF1.1, RF1.5, RN1.5).
2. **Usuários**: cadastrar, listar, editar e remover, escolhendo entre aluno e professor (RF1.2).
3. **Empréstimos**: emprestar (mostra a data prevista de devolução), devolver (mostra a multa) e listar os empréstimos em aberto, para saber o id na hora de devolver (RF1.3, RF1.4).
4. **Gerar relatório CSV**: pede a data inicial e a data final (`dd/MM/aaaa`) e grava o arquivo `relatorio_emprestimos_AAAA-MM-DD_a_AAAA-MM-DD.csv` com os empréstimos feitos no período (RF1.7).

Erros de regra (livro sem exemplar, limite de empréstimos, id inexistente, remoção não permitida, matrícula repetida) aparecem como mensagem e o programa volta ao menu.

## Relatório CSV

- Separador `;`, codificação UTF-8 com BOM (abre direto no Excel com acentos).
- Colunas: `id`, `usuario`, `matricula`, `livro`, `data_emprestimo`, `data_prevista`, `data_devolucao`, `situacao`, `multa`.
- `situacao` é `Devolvido`, `Em aberto` ou `Atrasado` (em aberto e já passou da data prevista).
- `multa` só é preenchida nos empréstimos devolvidos, com vírgula decimal (exemplo: `3,00`).
- O período filtra pela data do empréstimo, com as duas datas incluídas.

## Equipe

- Luiz Willian Soares Sobral
- Vinícius Cavalcante da Silva Marques
- Rodolfo Heleno Tenório Freitas

Professor: Michael Oliveira da Cruz
>>>>>>> Stashed changes
