package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
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
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int TAMANHO_PAGINA = 20;

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // lista da tela, busca no nome ou no telefone
    public Page<Cliente> buscar(String busca, String ordem, int pagina) {
        Query query = new Query();

        if (busca != null && !busca.isBlank()) {
            // tipo um nome ILIKE '%texto%' OR telefone ILIKE '%texto%'
            String texto = Pattern.quote(busca);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("nome").regex(texto, "i"),
                    Criteria.where("telefone").regex(texto, "i")));
        }

        // ordena igual portugues, sem separar maiuscula de minuscula
        query.collation(Collation.of("pt"));

        long total = mongoTemplate.count(query, Cliente.class);

        Sort ordenacao = Sort.by("nome").ascending();
        if ("nome_desc".equals(ordem)) {
            ordenacao = Sort.by("nome").descending();
        }

        Pageable paginacao = PageRequest.of(pagina, TAMANHO_PAGINA, ordenacao);
        query.with(paginacao);
        List<Cliente> clientes = mongoTemplate.find(query, Cliente.class);

        return new PageImpl<>(clientes, paginacao, total);
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.deleteById(id);
    }

    // usado na busca de cliente do novo pedido
    public List<Cliente> buscarPorNome(String nome) {
        return clienteRepository.findTop10ByNomeContainingIgnoreCase(nome);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null) {
            return clienteRepository.save(cliente);  // Cria um novo cliente
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            if (clienteRepository.existsById(cliente.getId())) {
                return clienteRepository.save(cliente);  // Atualiza o cliente existente
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }

}
