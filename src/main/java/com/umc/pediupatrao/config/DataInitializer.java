package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    // vem do .env
    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.gerente.username}")
    private String gerenteUsername;

    @Value("${app.gerente.password}")
    private String gerentePassword;

    @Value("${app.atendente.username}")
    private String atendenteUsername;

    @Value("${app.atendente.password}")
    private String atendentePassword;

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            criarSeNaoExistir(repo, encoder, adminUsername, adminPassword, Perfil.ADMIN);
            criarSeNaoExistir(repo, encoder, gerenteUsername, gerentePassword, Perfil.GERENTE);
            criarSeNaoExistir(repo, encoder, atendenteUsername, atendentePassword, Perfil.ATENDENTE);
        };
    }

    private void criarSeNaoExistir(UsuarioRepository repo, PasswordEncoder encoder,
            String username, String senha, Perfil perfil) {
        if (repo.findByUsername(username).isPresent()) {
            return;
        }

        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPassword(encoder.encode(senha));
        user.setRole(perfil);
        repo.save(user);

        System.out.println("Usuário " + username + " criado como " + perfil);
    }
}
