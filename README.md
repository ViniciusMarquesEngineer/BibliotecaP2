# Sistema de Controle de Biblioteca

Projeto da disciplina de Modelagem e Programação Orientada a Objetos (UFRPE/UABJ, 2026.2).

O sistema controla empréstimos e devoluções de livros para alunos e professores. Cada tipo de usuário tem prazo, limite de empréstimos e multa por atraso diferentes. Nesta etapa os dados ficam em listas (`ArrayList`) e o programa roda no console.

## Regras

| | Aluno | Professor |
|---|---|---|
| Prazo de devolução | 7 dias | 15 dias |
| Máximo de empréstimos em aberto | 3 | 5 |
| Multa por dia de atraso | R$ 1,00 | R$ 0,50 |

- A multa conta a partir do dia seguinte à data prevista.
- Livro sem exemplar livre não pode ser emprestado (`LivroIndisponivelException`).
- Usuário no limite de empréstimos não pode pegar outro livro (`LimiteEmprestimosException`).

## Estrutura

```
BibliotecaP2/
├── src/
│   ├── app/
│   │   └── Main.java                  demonstração no console
│   └── modelo/
│       ├── Biblioteca.java
│       ├── Usuario.java               classe abstrata
│       ├── Aluno.java
│       ├── Professor.java
│       ├── Livro.java
│       ├── Emprestimo.java
│       └── excecao/
│           ├── LivroIndisponivelException.java
│           └── LimiteEmprestimosException.java
├── uml_atualizado.png                 diagrama de classes
├── saida_main.txt                     saída do Main
└── README.md
```

## Como executar

Requisito: JDK 17 ou mais novo (o `javac` precisa estar disponível).

Abra o terminal **na pasta `BibliotecaP2`** (a que contém `src`).

Windows:

```
mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\app\Main.java
java -cp out app.Main
```

Linux ou macOS:

```
mkdir -p out
javac -encoding UTF-8 -sourcepath src -d out src/app/Main.java
java -cp out app.Main
```

Se os acentos saírem errados no Windows, rode `chcp 65001` e use:

```
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out app.Main
```

## O que o Main mostra

1. Empréstimos com a data prevista de devolução de cada tipo de usuário.
2. Devoluções com atraso e o cálculo da multa.
3. Tentativa de emprestar um livro sem exemplar livre.
4. Tentativa de passar do limite de empréstimos.
5. As regras de cada tipo de usuário, percorrendo a lista de usuários.

## Equipe

- Luiz Willian Soares Sobral
- Vinícius Cavalcante da Silva Marques
- Rodolfo Heleno Tenório Freitas

Professor: Michael Oliveira da Cruz
