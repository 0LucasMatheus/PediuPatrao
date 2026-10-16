package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int TAMANHO_PAGINA = 20;

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    // lista da tela com busca, filtros, ordenacao e paginacao
    public Page<Produto> buscar(String nome, String tipo, String ativo, String ordem, int pagina) {
        Query query = new Query();

        if (nome != null && !nome.isBlank()) {
            // tipo um ILIKE '%texto%' do sql
            query.addCriteria(Criteria.where("nome").regex(Pattern.quote(nome), "i"));
        }
        if (tipo != null && !tipo.isBlank()) {
            query.addCriteria(Criteria.where("tipo").is(tipo));
        }
        // vem "sim", "nao" ou vazio do select
        if ("sim".equals(ativo)) {
            query.addCriteria(Criteria.where("ativo").is(true));
        }
        if ("nao".equals(ativo)) {
            query.addCriteria(Criteria.where("ativo").ne(true));
        }

        // ordena igual portugues, sem separar maiuscula de minuscula
        query.collation(Collation.of("pt"));

        long total = mongoTemplate.count(query, Produto.class);

        Pageable paginacao = PageRequest.of(pagina, TAMANHO_PAGINA, ordenacao(ordem));
        query.with(paginacao);
        List<Produto> produtos = mongoTemplate.find(query, Produto.class);

        return new PageImpl<>(produtos, paginacao, total);
    }

    // o padrao e nome A-Z
    private Sort ordenacao(String ordem) {
        if ("nome_desc".equals(ordem)) {
            return Sort.by("nome").descending();
        }
        if ("menor_preco".equals(ordem)) {
            return Sort.by("preco").ascending();
        }
        if ("maior_preco".equals(ordem)) {
            return Sort.by("preco").descending();
        }
        return Sort.by("nome").ascending();
    }

    // usado na busca do adicionar item do pedido
    public List<Produto> buscarAtivosPorNome(String nome) {
        return produtoRepository.findTop10ByNomeContainingIgnoreCaseAndAtivoTrue(nome);
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    // Método para excluir produto
    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }

    // Método para salvar um novo produto ou atualizar um existente
    public Produto salvar(Produto produto) {
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new IllegalArgumentException("Preço do produto deve ser maior que zero.");
        }
        // Se o produto não tem ID (novo produto), salva como novo
        if (produto.getId() == null) {
            return produtoRepository.save(produto);
        } // Se já tem ID (produto existente), atualiza
        else {
            // Verifica se o produto existe antes de atualizar
            if (produtoRepository.existsById(produto.getId())) {
                return produtoRepository.save(produto);
            } else {
                throw new IllegalArgumentException("Produto não encontrado para atualização.");
            }
        }
    }
}
