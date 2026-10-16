package com.umc.pediupatrao.security;

import com.umc.pediupatrao.entity.Perfil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

// pega quem ta logado agora, usado nas regras e na auditoria
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "sistema";
        }
        return auth.getName();
    }

    public static Perfil perfil() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }

        // o spring guarda como ROLE_ADMIN, tira o ROLE_ e vira o enum
        for (GrantedAuthority autoridade : auth.getAuthorities()) {
            String nome = autoridade.getAuthority().replace("ROLE_", "");
            return Perfil.valueOf(nome);
        }
        return null;
    }
}
