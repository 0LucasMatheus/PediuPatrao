package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AcaoAuditoria;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    // campos que nunca vao pro log
    private static final List<String> CAMPOS_IGNORADOS = List.of("_id", "_class", "password", "senha");

    private static final int TAMANHO_PAGINA = 20;

    private final AuditoriaRepository auditoriaRepository;
    private final MongoTemplate mongoTemplate;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, MongoTemplate mongoTemplate) {
        this.auditoriaRepository = auditoriaRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // monta a busca so com os filtros que vieram preenchidos
    public Page<Auditoria> buscar(String usuario, String entidade, String acao, String campo, String ordem, int pagina) {
        Query query = new Query();

        if (usuario != null && !usuario.isBlank()) {
            query.addCriteria(Criteria.where("usuario").regex(Pattern.quote(usuario), "i"));
        }
        if (entidade != null && !entidade.isBlank()) {
            query.addCriteria(Criteria.where("entidade").is(entidade));
        }
        if (acao != null && !acao.isBlank()) {
            query.addCriteria(Criteria.where("acao").is(acao));
        }
        if (campo != null && !campo.isBlank()) {
            // procura o campo dentro da lista de alteracoes tipo ILIKE
            query.addCriteria(Criteria.where("alteracoes.campo").regex(Pattern.quote(campo), "i"));
        }

        long total = mongoTemplate.count(query, Auditoria.class);

        // o padrao e mais novos primeiro
        Sort ordenacao = Sort.by("dataHora").descending();
        if ("antigos".equals(ordem)) {
            ordenacao = Sort.by("dataHora").ascending();
        }

        Pageable paginacao = PageRequest.of(pagina, TAMANHO_PAGINA, ordenacao);
        query.with(paginacao);
        List<Auditoria> registros = mongoTemplate.find(query, Auditoria.class);

        return new PageImpl<>(registros, paginacao, total);
    }

    // chamado pelo listener, antigo e novo sao o documento do mongo inteiro
    public void registrar(AcaoAuditoria acao, String entidade, String entidadeId, Document antigo, Document novo) {
        List<CampoAlterado> alteracoes = compararDocumentos(antigo, novo);

        // alterou mas nenhum campo mudou de verdade, n precisa logar
        if (acao == AcaoAuditoria.ALTERACAO && alteracoes.isEmpty()) {
            return;
        }

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(UsuarioLogado.username());
        auditoria.setPerfil(UsuarioLogado.perfil());
        auditoria.setAcao(acao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setAlteracoes(alteracoes);
        auditoria.setDataHora(LocalDateTime.now());

        // se mudou algum campo de justificativa copia pro log
        for (CampoAlterado campo : alteracoes) {
            if (campo.getCampo().toLowerCase().contains("justificativa")) {
                auditoria.setJustificativa(campo.getValorNovo());
            }
        }

        auditoriaRepository.save(auditoria);
    }

    // olha todos os campos dos dois e guarda so os que tao diferentes
    private List<CampoAlterado> compararDocumentos(Document antigo, Document novo) {
        Set<String> campos = new LinkedHashSet<>();
        campos.addAll(antigo.keySet());
        campos.addAll(novo.keySet());

        List<CampoAlterado> alteracoes = new ArrayList<>();
        for (String campo : campos) {
            if (CAMPOS_IGNORADOS.contains(campo)) {
                continue;
            }

            String valorAnterior = paraTexto(antigo.get(campo));
            String valorNovo = paraTexto(novo.get(campo));

            if (!Objects.equals(valorAnterior, valorNovo)) {
                // telefone n aparece inteiro no log
                if (campo.equals("telefone")) {
                    valorAnterior = mascararTelefone(valorAnterior);
                    valorNovo = mascararTelefone(valorNovo);
                }
                alteracoes.add(new CampoAlterado(campo, valorAnterior, valorNovo));
            }
        }
        return alteracoes;
    }

    private String paraTexto(Object valor) {
        if (valor == null) {
            return null;
        }

        // data vem do mongo como Date, deixa no formato br
        if (valor instanceof Date) {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm").format((Date) valor);
        }

        // lista tipo os itens do pedido, transforma cada um e junta
        if (valor instanceof List) {
            List<String> textos = new ArrayList<>();
            for (Object item : (List<?>) valor) {
                textos.add(paraTexto(item));
            }
            return String.join(" | ", textos);
        }

        // documento dentro de documento, tipo um item, vira json
        if (valor instanceof Document) {
            return ((Document) valor).toJson();
        }

        return valor.toString();
    }

    // (11) 99999-1234 vira *****-1234
    private String mascararTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String numeros = telefone.replaceAll("\\D", "");
        if (numeros.length() < 4) {
            return "****";
        }
        return "*****-" + numeros.substring(numeros.length() - 4);
    }
}
