[README.md](https://github.com/user-attachments/files/35291298/README.md)
# Sistema de Controle de Biblioteca

**Equipe:** Luiz Willian Soares Sobral, Vinícius Cavalcante da Silva Marques e Rodolfo Heleno Tenório Freitas.

## Como executar

Requisito: **Java 17 ou superior**.

- **Mais simples:** dar dois cliques em `Biblioteca.jar` (ou `java -jar Biblioteca.jar`).
  Esse `.jar` já traz o driver do SQLite dentro dele e funciona sozinho, em qualquer pasta que aceite gravação.
- **Pela IDE (IntelliJ, Eclipse, VS Code):** abra a pasta do projeto como projeto **Maven** (`pom.xml`).
  A IDE baixa o driver sozinha. Depois rode `app.Main`.
- **Sem Maven:** adicione `lib/sqlite-jdbc-3.47.0.0.jar` às bibliotecas do projeto e rode `app.Main`.
- **Pelo terminal:** `executar.bat` (Windows) ou `./executar.sh` (Linux/macOS) compilam e abrem o sistema.

O banco `biblioteca.db` é criado sozinho na pasta de onde o programa é executado, na primeira vez,
e os dados continuam lá depois que o programa fecha. Menu de console (mesmo banco): `app.MenuConsole`.

## O que cada parte do código faz

```
BibliotecaP2/
│
├── Biblioteca.jar ........ programa pronto para abrir (traz o driver SQLite dentro)
├── pom.xml ............... projeto Maven: a IDE baixa o driver sozinha
├── executar.bat / .sh .... compilam o código e abrem o sistema
├── lib/ .................. driver sqlite-jdbc (.jar)
├── docs/telas/ ........... capturas das telas
├── uml_entrega4.png ...... diagrama de classes final
│
└── src/
    │
    ├── app/ ................................ ponto de entrada
    │   ├── Main ............................ abre a janela principal (é a classe que se executa)
    │   └── MenuConsole ..................... menu de texto: livros, usuários, empréstimos e relatório
    │
    ├── Interface/ .......................... interface gráfica Swing
    │   ├── JanelaPrincipal ................. janela com as 3 abas; relê os dados ao trocar de aba
    │   │                                     e fecha a conexão com o banco ao sair
    │   ├── PainelLivros .................... aba Livros
    │   │   ├── salvar() .................... cadastra um livro novo com os dados do formulário
    │   │   ├── editar() .................... grava as alterações do livro selecionado
    │   │   ├── remover() ................... remove o livro selecionado (pede confirmação)
    │   │   └── atualizar() ................. lista os livros ou o resultado da busca por título/autor
    │   ├── PainelUsuarios .................. aba Usuários
    │   │   ├── salvar() / editar() ......... cadastra ou altera aluno/professor
    │   │   ├── remover() ................... remove o usuário selecionado (pede confirmação)
    │   │   └── campo matrícula ............. aceita apenas números
    │   ├── PainelEmprestimos ............... aba Empréstimos
    │   │   ├── emprestar() ................. registra o empréstimo e mostra a data prevista de devolução
    │   │   ├── devolver() .................. registra a devolução e mostra a multa
    │   │   ├── preencherTabela() ........... lista os empréstimos em aberto; atrasados ficam em vermelho
    │   │   └── gerarRelatorio() ............ salva o relatório (.xlsx e .csv) na pasta Downloads
    │   └── Apoio ........................... funções usadas por todas as telas
    │       ├── erro() / info() ............. caixas de mensagem
    │       ├── obrigatorio() / inteiro() ... conferem campos de texto e números
    │       ├── moeda() / DATA .............. formatos de dinheiro (R$ 8,00) e de data (dd/MM/aaaa)
    │       ├── modeloTabela() .............. tabela só de leitura
    │       ├── estilizarCabecalho() / botaoColorido() ... cores azul, verde e vermelho da interface
    │       └── padronizarFonte() / igualarTamanho() ..... Arial 13 em toda a tela e botões do mesmo tamanho
    │
    ├── modelo/ ............................. classes do problema e regras (não usa banco nem tela)
    │   ├── Biblioteca ...................... guarda livros, usuários e empréstimos
    │   │   ├── emprestar() ................. confere limite e exemplar livre e cria o Empréstimo
    │   │   └── devolver() .................. fecha o empréstimo e devolve o valor da multa
    │   ├── Usuario (abstrata) .............. id, nome, matrícula; define prazo, limite e multa
    │   │   ├── Aluno ....................... 7 dias, limite 3, multa R$ 1,00 por dia
    │   │   └── Professor ................... 15 dias, limite 5, multa R$ 0,50 por dia
    │   ├── Livro ........................... dados do livro e controle de exemplares
    │   │   ├── emprestarExemplar() ......... tira um exemplar livre (erro se não houver)
    │   │   └── devolverExemplar() .......... devolve um exemplar
    │   ├── Emprestimo ...................... liga usuário e livro; datas do empréstimo, prevista e da devolução
    │   └── excecao/ ........................ erros do sistema
    │       ├── LivroIndisponivelException .. livro sem exemplar livre
    │       ├── LimiteEmprestimosException .. usuário no limite de empréstimos
    │       ├── RegistroNaoEncontradoException .. id que não existe
    │       ├── OperacaoInvalidaException ... remoção não permitida ou matrícula repetida
    │       └── PersistenciaException ....... erro de banco de dados (SQL)
    │
    ├── dao/ ................................ persistência em SQLite via JDBC
    │   ├── ConexaoBD ....................... abre a conexão, cria as tabelas na 1ª execução e procura o driver
    │   ├── UsuarioDAO ...................... inserir, listar, buscarPorId, atualizar e remover usuários
    │   ├── LivroDAO ........................ inserir, listar, buscar (título/autor), atualizar e remover livros
    │   └── EmprestimoDAO ................... inserir, listar e registrarDevolucao (empréstimo e exemplares na mesma transação)
    │
    └── arquivo/ ............................ relatórios
        ├── RelatorioCSV .................... relatório em CSV (separador ;, UTF-8 com BOM)
        └── RelatorioExcel .................. o mesmo relatório em .xlsx, com títulos em negrito e colunas largas
```

Divisão em camadas: a **Interface** (e o `MenuConsole`) chama o **modelo** e os **DAOs**;
os DAOs gravam e leem no banco; o **modelo** não conhece banco nem tela.

### Como um empréstimo acontece (aba Empréstimos)

```
Clique em "Emprestar"
└── PainelEmprestimos.emprestar()
    ├── carregar() ............... monta a Biblioteca com o que está no banco
    ├── Biblioteca.emprestar() ... confere limite do usuário e exemplar livre; cria o Emprestimo
    │   ├── erro de limite ....... LimiteEmprestimosException  → caixa de mensagem
    │   └── erro de exemplar ..... LivroIndisponivelException → caixa de mensagem
    ├── EmprestimoDAO.inserir() .. grava o empréstimo e baixa o exemplar (mesma transação)
    └── atualizar() .............. recarrega a lista de empréstimos em aberto
```

## Regras do sistema

| | Aluno | Professor |
|---|---|---|
| Prazo | 7 dias | 15 dias |
| Limite de empréstimos em aberto | 3 | 5 |
| Multa por dia de atraso | R$ 1,00 | R$ 0,50 |

- Livro sem exemplar livre não pode ser emprestado.
- Livro com empréstimo em aberto não pode ser removido.
- Gravar empréstimo/devolução e atualizar os exemplares acontece na mesma transação.

## Relatório de empréstimos

Gerado pela aba Empréstimos (botão "Gerar relatório CSV") e salvo na pasta **Downloads** do usuário, com os
empréstimos feitos no período informado. São criados dois arquivos com o mesmo conteúdo:

- `relatorio_emprestimos_AAAA-MM-DD_a_AAAA-MM-DD.xlsx`: planilha do Excel, com títulos em negrito e colunas
  largas, para as datas aparecerem por inteiro.
- `relatorio_emprestimos_AAAA-MM-DD_a_AAAA-MM-DD.csv`: texto com separador `;` e UTF-8 com BOM. O CSV não guarda
  formatação, então no Excel as colunas abrem estreitas e as datas podem aparecer como `#####` até alargar a coluna.
