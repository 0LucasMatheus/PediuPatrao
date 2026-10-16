package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    private static final int TAMANHO_PAGINA = 20;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            MongoTemplate mongoTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    // lista da tela com busca, filtro de perfil, ordenacao e paginacao
    public Page<Usuario> buscar(String username, String perfil, String ordem, int pagina) {
        Query query = new Query();

        if (username != null && !username.isBlank()) {
            // tipo um ILIKE '%texto%' do sql
            query.addCriteria(Criteria.where("username").regex(Pattern.quote(username), "i"));
        }
        if (perfil != null && !perfil.isBlank()) {
            query.addCriteria(Criteria.where("role").is(perfil));
        }

        // ordena igual portugues, sem separar maiuscula de minuscula
        query.collation(Collation.of("pt"));

        long total = mongoTemplate.count(query, Usuario.class);

        Sort ordenacao = Sort.by("username").ascending();
        if ("username_desc".equals(ordem)) {
            ordenacao = Sort.by("username").descending();
        }

        Pageable paginacao = PageRequest.of(pagina, TAMANHO_PAGINA, ordenacao);
        query.with(paginacao);
        List<Usuario> usuarios = mongoTemplate.find(query, Usuario.class);

        return new PageImpl<>(usuarios, paginacao, total);
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public void salvarUsuario(Usuario usuario) {
        // garante que e um novo e n sobrescreve outro usuario pelo id
        usuario.setId(null);

        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("Informe o username.");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        if (usuario.getRole() == null) {
            throw new IllegalArgumentException("Selecione um perfil.");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // n deixa o admin se excluir e ficar sem acesso
        if (usuario.getUsername().equals(UsuarioLogado.username())) {
            throw new IllegalStateException("Você não pode excluir o seu próprio usuário.");
        }

        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (usuarioAtualizado.getRole() == null) {
            throw new IllegalArgumentException("Selecione um perfil.");
        }

        // ninguem muda o proprio perfil
        boolean ehEleMesmo = usuarioExistente.getUsername().equals(UsuarioLogado.username());
        if (ehEleMesmo && usuarioAtualizado.getRole() != usuarioExistente.getRole()) {
            throw new IllegalStateException("Você não pode alterar o seu próprio perfil.");
        }

        // se trocou o username n pode ser um que ja existe
        String novoUsername = usuarioAtualizado.getUsername();
        if (!novoUsername.equals(usuarioExistente.getUsername())
                && usuarioRepository.findByUsername(novoUsername).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }

        usuarioExistente.setUsername(novoUsername);
        usuarioExistente.setRole(usuarioAtualizado.getRole());

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        usuarioRepository.save(usuarioExistente);
    }
}
