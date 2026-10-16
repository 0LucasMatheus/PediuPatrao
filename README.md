<h1 align="center"> 
	🍕 PediuPatrão - Sistema de pedidos de pizzaria 🍕
</h1>


---

## 🍕 Sobre o projeto

PediuPatrão é um sistema de pedidos para pizzaria feito com **Java + Spring Boot**, **Thymeleaf** e **MongoDB Atlas**, usado para cadastrar clientes, gerenciar produtos e registrar pedidos.

Esse projeto é a evolução do sistema base da **Atividade Prática M1 de Engenharia de Software**. A pizzaria cresceu e precisou separar a responsabilidade de cada funcionário, controlar a entrada e a saída dos pedidos e saber quem alterou o que nas informações de clientes e pedidos. Pra isso foram adicionados três perfis de acesso com as regras no backend, um fluxo de status obrigatório pros pedidos, cancelamento e desconto exclusivos do gerente e uma auditoria de todas as alterações.

---

## 🧀 Funcionalidades

#### RF01 — Usuários e permissões
- [x] Três perfis de acesso: `ADMIN`, `GERENTE` e `ATENDENTE`
- [x] Login usa o perfil real do usuário salvo no banco
- [x] Os três usuários iniciais são criados pelo `.env` na primeira execução
- [x] Autorização no backend com `@PreAuthorize` em cada endpoint de tela e de API, esconder botão é só visual
- [x] Ninguém altera o próprio perfil nem exclui o próprio usuário (proteção contra autoelevação)
- [x] API de usuários não devolve a senha
- [x] CSRF ligado em todos os formulários e operações sensíveis

#### RF02 — Cadastro de pedidos
- [x] Pedido com itens, quantidades, preços, desconto, valor total, responsável e data/hora de entrada
- [x] Valor total calculado no servidor com o preço do banco, a requisição não consegue definir o total
- [x] Preço do produto fica congelado no item, se o produto mudar de preço o pedido antigo não muda
- [x] Todos os valores em centavos (`int`) pra não ter erro de conta
- [x] Tela de novo pedido com busca de cliente e de produto pelo nome

#### RF03 — Entrada, saída e transições
- [x] Fluxo obrigatório: `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA / RETIRADO → FINALIZADO`
- [x] Transições inválidas bloqueadas no service (ex: finalizar um pedido em preparação)
- [x] Saída registra data/hora e responsável obrigatoriamente
- [x] Voltar um status só antes da saída, pra corrigir clique errado
- [x] Pedido cancelado ou finalizado não muda mais

#### RF04 — Cancelamento e desconto
- [x] Só o `GERENTE` cancela e dá desconto, nem o `ADMIN` consegue
- [x] Cancelamento exige justificativa, só antes da saída, guarda quem e quando e não apaga nada
- [x] Desconto percentual de 1% a 20%, só antes da saída, aplicar de novo substitui e não soma

#### RF05 — Auditoria
- [x] Auditoria persistente no MongoDB de inclusões, alterações e exclusões de clientes e pedidos
- [x] Guarda usuário, perfil, data/hora, ação, entidade, id e os campos alterados com valor anterior e novo
- [x] Justificativa do cancelamento vai junto no registro
- [x] Consulta só para `ADMIN` e `GERENTE`, sem editar ou excluir
- [x] Senha nunca vai pro log e telefone aparece mascarado

#### Extras
- [x] Busca, filtros, ordenação e paginação em todas as listas
- [x] Lista de pedidos mostra os pedidos do dia por padrão
- [x] Tela de detalhe do pedido
- [ ] Telas de funcionários, entregadores e horário de funcionamento

---

## 🛡️ Perfis de acesso

| Ação | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Usuários (criar, editar, perfis) | ✅ | ❌ | ❌ |
| Clientes: consultar | ✅ | ✅ | ✅ |
| Clientes: criar / editar | ❌ | ✅ | ✅ |
| Clientes: excluir | ❌ | ✅ | ❌ |
| Produtos: consultar | ✅ | ✅ | ✅ |
| Produtos: criar / editar / excluir | ✅ | ✅ | ❌ |
| Pedidos: consultar | ✅ | ✅ | ✅ |
| Pedidos: registrar e mudar status | ❌ | ✅ | ✅ |
| Pedidos: cancelar e dar desconto | ❌ | ✅ | ❌ |
| Auditoria | ✅ | ✅ | ❌ |

A proteção é feita em duas partes:
1. `SecurityConfig` — exige login em tudo e bloqueia áreas inteiras, tipo `/usuarios/**` só pra `ADMIN`;
2. `@PreAuthorize` em cada endpoint, tanto das telas (`HomeController`) quanto da API (`/api/...`).

Quem tenta acessar sem permissão recebe **403**, mesmo chamando a API direto sem passar pela tela. API sem login responde **401**.

---

## 🍅 Auditoria

A auditoria funciona igual a um trigger de banco. O `AuditoriaListener` é um listener de eventos do Spring Data MongoDB que roda sozinho antes de todo `save` e `delete` nas collections `clientes` e `pedidos`:

- **sem id** → é uma inclusão;
- **com id** → busca como o documento tá no banco, compara com o novo e é uma alteração;
- **delete** → busca o que vai ser apagado e é uma exclusão.

Só os campos que mudaram são guardados. Mudança de status, desconto e cancelamento entram como alteração de pedido, mostrando os campos `status`, `descontoPercentual`, `valorTotal`, etc.

Exemplo de registro na collection `auditoria`:

```json
{
  "usuario": "atendente",
  "perfil": "ATENDENTE",
  "acao": "ALTERACAO",
  "entidade": "clientes",
  "entidadeId": "6710f3...",
  "alteracoes": [
    { "campo": "logradouro", "valorAnterior": "Rua das Flores", "valorNovo": "Av. Brasil" },
    { "campo": "telefone", "valorAnterior": "*****-1234", "valorNovo": "*****-5678" }
  ],
  "justificativa": null,
  "dataHora": "2026-09-23T14:35:00"
}
```

O `AuditoriaRepository` estende o `Repository` vazio do Spring e só declara o `save`, então não existe update nem delete de auditoria pela aplicação.

---

## 🖼️ Layout

---

## 🧑‍🍳 Como executar o projeto

### Pré-requisitos

- [Git](https://git-scm.com)
- [Nix + devenv](https://devenv.sh/getting-started/) (gerencia o JDK 21 e o Maven automaticamente)
- Uma conta no [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) (gratuita) com um cluster criado

---

**1 - Instalar o Nix e o devenv**, seguindo o guia oficial: [devenv.sh/getting-started](https://devenv.sh/getting-started/)

**2 - Clonar o projeto e entrar na pasta**
```bash
git clone https://github.com/0LucasMatheus/PediuPatrao.git
cd PediuPatrao
```

**3 - Entrar no ambiente**
```bash
devenv shell
# ou, se você usa direnv:
direnv allow
```

**4 - Configurar as variáveis de ambiente**
```bash
cp .env.example .env
# preencha com os valores reais
```

| Variável | Pra que serve |
|---|---|
| `MONGODB_URI` | string de conexão do MongoDB Atlas |
| `MONGODB_DATABASE` | nome do banco (padrão `pediupatrao`) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | usuário ADMIN criado na primeira execução |
| `GERENTE_USERNAME` / `GERENTE_PASSWORD` | usuário GERENTE criado na primeira execução |
| `ATENDENTE_USERNAME` / `ATENDENTE_PASSWORD` | usuário ATENDENTE criado na primeira execução |

**5 - Subir a aplicação**
```bash
mvn spring-boot:run
```

**6 - Acessar o projeto**
- Aplicação: http://localhost:8080/login
- Usuários: os três definidos no `.env`, criados automaticamente na primeira execução caso ainda não existam

---

## 🌶️ Principais alterações

Em relação ao projeto base:

- Credenciais tiradas do `application.properties` e movidas pro `.env`
- Telas que estavam quebradas arrumadas (produtos, sidebar) e páginas de exemplo sem uso removidas
- Entidades com Lombok e dependência de JPA (`@Entity`) removida, já que o banco é Mongo
- `UsuarioDetailsService` usando o perfil do usuário em vez de `USER` fixo
- `SecurityConfig` reescrito com CSRF ligado, rotas por perfil e `@EnableMethodSecurity`
- Modelo de `Pedido` refeito (itens, valores, entrada, saída, cancelamento) e `PedidoService` com as regras de negócio
- Estrutura nova de auditoria: `Auditoria`, `CampoAlterado`, `AcaoAuditoria`, `AuditoriaRepository`, `AuditoriaService` e `AuditoriaListener`
- Busca, filtros, ordenação e paginação em pedidos, clientes, produtos, usuários e auditoria
- Tratamento de erros: na API volta JSON com 400/404, nas telas volta pra página anterior com a mensagem

---

## 🎣 Tecnologias

As seguintes ferramentas estão sendo usadas na construção do projeto:

#### **Ambiente de desenvolvimento**
- **[devenv](https://devenv.sh)** (Nix) — JDK 21 e Maven gerenciados automaticamente

#### **Back-End**
- [Java 21](https://www.oracle.com/java/) + [Spring Boot 3](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security) — autenticação e autorização
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb) — persistência no MongoDB Atlas
- [Lombok](https://projectlombok.org/)

#### **Front-End**
- [Thymeleaf](https://www.thymeleaf.org/) + [thymeleaf-extras-springsecurity6](https://github.com/thymeleaf/thymeleaf-extras-springsecurity) (`sec:authorize` nas views)
- [thymeleaf-layout-dialect](https://github.com/ultraq/thymeleaf-layout-dialect)
- [Bootstrap 5](https://getbootstrap.com/) + [Bootstrap Icons](https://icons.getbootstrap.com/)

#### **Banco de dados**
- [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)

---
