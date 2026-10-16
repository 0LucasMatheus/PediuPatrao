package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
