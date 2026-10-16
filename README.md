<h1 align="center"> 
	🍃 Javacamus - Sistema base com SpringBoot 🍃
</h1>


---

## 🌳 Sobre o projeto

Javacamus é um sistema generico que serve de base para outros construído com **Java + Spring Boot**, **Thymeleaf** e **MongoDB Atlas**. O objetivo é entregar um **template genérico de autenticação e autorização**, pronto para ser usado como base de outros projetos.

O sistema cobre o ciclo completo de gerenciamento de usuários: cadastro (restrito a administradores), login, logout, recuperação de senha e controle de acesso por perfil (role) usando **Spring Security** para toda a parte de autenticação/autorização e **MongoDB Atlas** como banco de dados.

A interface com Thymeleaf foi montada em cima de um layout único (`layout.html`) com fragments reaproveitáveis (sidebar, conteúdo), o que permite trocar o tema visual sem mexer em muitos arquivos.

---


## 🛡️ Perfis de acesso (Roles)

O sistema vem com 3 perfis genéricos, pensados para servir de ponto de partida para qualquer domínio de aplicação:

| Perfil       | Pode acessar                                                                 |
|--------------|-------------------------------------------------------------------------------|
| `ADMIN`      | Tudo: área administrativa (`/admin/**`), moderação (`/moderator/**`), área comum (`/usuario/**`) e a API REST (`/api/usuarios`) |
| `MODERATOR`  | Moderação (`/moderator/**`) e área comum (`/usuario/**`)                     |
| `USER`       | Apenas a área comum (`/usuario/**`)                                          |

A autorização é configurada centralmente em `SecurityConfig` (via `authorizeHttpRequests`), e as telas (Thymeleaf) usam `sec:authorize` para exibir ou esconder menus e blocos de conteúdo conforme o perfil do usuário logado. Quem tenta acessar uma rota sem permissão cai na página `/acesso-negado` (HTTP 403).

Para adaptar os perfis ao tema do seu projeto, basta:
1. Alterar os valores do enum `Role` (`src/main/java/com/umc/javacamus/entity/Role.java`);
2. Ajustar os `requestMatchers(...).hasRole(...)` em `SecurityConfig`;
3. Ajustar os `sec:authorize` nas views (`sidebar.html`, `home.html`, etc).

E começar a fazer suas própias telas novas seguindo a estrutura definida.

---

## 🍃 MongoDB Atlas

O projeto usa **Spring Data MongoDB** para persistir os usuários em um cluster do **MongoDB Atlas** (serviço de banco de dados gerenciado na nuvem).

### Como a conexão é feita

A string de conexão é lida de uma variável de ambiente (`MONGODB_URI`) dentro de `application.properties`:

```properties
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=${MONGODB_DATABASE}
spring.data.mongodb.auto-index-creation=true
```

O valor real de `MONGODB_URI` fica em um arquivo `.env` não versionado por segurança.

```
MONGODB_URI=mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/?appName=Cluster0
MONGODB_DATABASE=javacamus
```

### Como criar seu próprio cluster no Atlas

1. Crie uma conta gratuita em [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas).
2. Crie um cluster (o tier gratuito M0 é suficiente).
3. Em **Database Access**, crie um usuário de banco com senha.
4. Em **Network Access**, libere o IP da sua máquina (ou `0.0.0.0/0` apenas para desenvolvimento).
5. Em **Connect → Drivers**, copie a connection string (`mongodb+srv://...`) e cole em `MONGODB_URI` no seu `.env`.

### O que é armazenado

Toda a persistência do sistema gira em torno de uma única coleção, `usuarios`, com documentos no formato:

```json
{
  "_id": "ObjectId(...)",
  "username": "admin",
  "email": "admin@exemplo.com",
  "password": "$2a$10$... (hash BCrypt, nunca texto puro)",
  "role": "ADMIN",
  "resetCode": null,
  "resetCodeExpiresAt": null
}
```

`resetCode` e `resetCodeExpiresAt` só são preenchidos durante uma solicitação de recuperação de senha, e são limpos assim que a senha é redefinida ou quando o código expira.

A coleção `usuarios` é criada automaticamente na primeira execução por `MongoInitConfig`, e o índice único de `username` é criado automaticamente pelo Spring Data (`auto-index-creation=true`), evitando usernames duplicados.

---

## 🖼️ Layout

---

## 🦈 Como executar o projeto

### Pré-requisitos

- [Git](https://git-scm.com)
- [Nix + devenv](https://devenv.sh/getting-started/) (gerencia o JDK 21 e o Maven automaticamente)
- Uma conta no [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) (gratuita) com um cluster criado
- Uma conta de e-mail com senha de app habilitada, para o envio do código de recuperação de senha

---

**1 - Instalar o Nix e o devenv**, seguindo o guia oficial: [devenv.sh/getting-started](https://devenv.sh/getting-started/)

**2 - Clonar o projeto e entrar na pasta**
```bash
git clone https://github.com/SEU_USUARIO/javacamus.git
cd javacamus
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

**5 - Subir a aplicação**
```bash
devenv up
```
O processo `app` já sobe o Spring Boot (via Maven) na porta `8080`, lendo as variáveis do `.env`.

Alternativamente, dentro do `devenv shell`, dá pra rodar manualmente:
```bash
mvn spring-boot:run
```

**6 - Acessar o projeto**
- Aplicação: http://localhost:8080/login
- Usuário ADMIN padrão: o que você definiu em `ADMIN_USERNAME` / `ADMIN_PASSWORD` no `.env` (criado automaticamente na primeira execução, caso ainda não exista)

---

## 🧩 Decisões de design

---

## 🎣 Tecnologias

As seguintes ferramentas estão sendo usadas na construção do projeto:

#### **Ambiente de desenvolvimento**
- **[devenv](https://devenv.sh)** (Nix) — JDK 21 e Maven gerenciados automaticamente, com processo dedicado para subir a aplicação

#### **Back-End**
- [Java 21](https://www.oracle.com/java/) + [Spring Boot 3](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security) — autenticação e autorização
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb) — persistência no MongoDB Atlas
- [Spring Mail](https://docs.spring.io/spring-framework/reference/integration/email.html) — envio do código de recuperação de senha por e-mail

#### **Front-End**
- [Thymeleaf](https://www.thymeleaf.org/) + [thymeleaf-extras-springsecurity6](https://github.com/thymeleaf/thymeleaf-extras-springsecurity) (`sec:authorize` nas views)
- [thymeleaf-layout-dialect](https://github.com/ultraq/thymeleaf-layout-dialect)
- [Bootstrap 5](https://getbootstrap.com/) + [Bootstrap Icons](https://icons.getbootstrap.com/)

#### **Banco de dados**
- [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)

---
