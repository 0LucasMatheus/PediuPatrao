package com.umc.pediupatrao.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuditoriaService auditoriaService;

    // ========================
    // LOGIN E HOME
    // ========================
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    // ========================
    // USUÁRIOS
    // ========================
    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(required = false) String username,
            @RequestParam(required = false) String perfil,
            @RequestParam(required = false) String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {
        if (pagina < 0) {
            pagina = 0;
        }

        model.addAttribute("usuarios", usuarioService.buscar(username, perfil, ordem, pagina));

        // devolve os filtros pra tela continuar preenchida
        model.addAttribute("username", username);
        model.addAttribute("perfil", perfil);
        model.addAttribute("ordem", ordem);

        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = usuarioService.buscarPorId(id).map(u -> {
            model.addAttribute("usuario", u);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        usuarioService.salvarUsuario(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    public String atualizarUsuario(@PathVariable String id,
            @ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        usuarioService.atualizarUsuario(id, usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    public String deletarUsuario(@PathVariable String id,
            RedirectAttributes redirectAttributes) {
        usuarioService.deletarUsuario(id);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        return "redirect:/usuarios";
    }

    // ========================
    // PEDIDOS
    // ========================
    @GetMapping("/pedidos")
    public String pedidos(@RequestParam(required = false) String cliente,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {
        // sem dia escolhido mostra os de hoje
        if (dia == null) {
            dia = LocalDate.now();
        }
        if (pagina < 0) {
            pagina = 0;
        }

        model.addAttribute("pedidos", pedidoService.buscar(cliente, status, dia, ordem, pagina));

        // devolve os filtros pra tela continuar preenchida
        model.addAttribute("cliente", cliente);
        model.addAttribute("status", status);
        model.addAttribute("dia", dia);
        model.addAttribute("ordem", ordem);
        model.addAttribute("todosStatus", StatusPedido.values());

        model.addAttribute("content", "pedidos/lista :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @GetMapping("/pedidos/novo")
    public String novoPedidoForm(Model model) {
        model.addAttribute("content", "pedidos/form :: content");
        return "layout";
    }

    // o form manda duas listas na mesma ordem, uma de produto e uma de quantidade
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PostMapping("/pedidos/salvar")
    public String salvarPedido(@RequestParam String clienteId,
            @RequestParam(required = false) List<String> produtoId,
            @RequestParam(required = false) List<Integer> quantidade,
            RedirectAttributes redirectAttributes) {
        List<ItemPedidoRequest> itens = new ArrayList<>();

        if (produtoId != null && quantidade != null) {
            for (int i = 0; i < produtoId.size(); i++) {
                // campo vazio vem null, vira 0 e o service barra
                Integer qtd = quantidade.get(i);
                if (qtd == null) {
                    qtd = 0;
                }
                itens.add(new ItemPedidoRequest(produtoId.get(i), qtd));
            }
        }

        pedidoService.criarPedido(new PedidoRequest(clienteId, itens));
        redirectAttributes.addFlashAttribute("sucesso", "Pedido registrado com sucesso!");
        return "redirect:/pedidos";
    }

    // detalhe do pedido, todo mundo logado pode ver
    @GetMapping("/pedidos/{id}")
    public String detalhePedido(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);
        if (pedido == null) {
            redirectAttributes.addFlashAttribute("erro", "Pedido não encontrado.");
            return "redirect:/pedidos";
        }

        model.addAttribute("pedido", pedido);

        // busca o cliente pra mostrar telefone e endereco, se foi excluido vem null
        model.addAttribute("cliente", clienteService.buscarPorId(pedido.getClienteId()).orElse(null));

        model.addAttribute("content", "pedidos/detalhe :: content");
        return "layout";
    }

    // so gerente, nem o admin pode
    @PreAuthorize("hasRole('GERENTE')")
    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelarPedido(@PathVariable String id,
            @RequestParam String justificativa,
            RedirectAttributes redirectAttributes) {
        pedidoService.cancelar(id, justificativa);
        redirectAttributes.addFlashAttribute("sucesso", "Pedido cancelado.");
        return "redirect:/pedidos/" + id;
    }

    // so gerente, nem o admin pode
    @PreAuthorize("hasRole('GERENTE')")
    @PostMapping("/pedidos/{id}/desconto")
    public String descontoPedido(@PathVariable String id,
            @RequestParam int percentual,
            RedirectAttributes redirectAttributes) {
        pedidoService.aplicarDesconto(id, percentual);
        redirectAttributes.addFlashAttribute("sucesso", "Desconto de " + percentual + "% aplicado.");
        return "redirect:/pedidos/" + id;
    }

    // botoes de avancar status na lista
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PostMapping("/pedidos/{id}/status")
    public String atualizarStatusPedido(@PathVariable String id,
            @RequestParam StatusPedido status,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        pedidoService.atualizarStatus(id, status);
        redirectAttributes.addFlashAttribute("sucesso", "Status do pedido atualizado!");

        // volta pra mesma lista com os mesmos filtros
        String voltar = request.getHeader("Referer");
        if (voltar == null) {
            voltar = "/pedidos";
        }
        return "redirect:" + voltar;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PostMapping("/pedidos/{id}/voltar")
    public String voltarStatusPedido(@PathVariable String id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        pedidoService.voltarStatus(id);
        redirectAttributes.addFlashAttribute("sucesso", "Pedido voltou para o status anterior.");

        String voltar = request.getHeader("Referer");
        if (voltar == null) {
            voltar = "/pedidos";
        }
        return "redirect:" + voltar;
    }

    // ========================
    // PRODUTOS
    // ========================
    @GetMapping("/produtos")
    public String produtos(@RequestParam(required = false) String nome,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String ativo,
            @RequestParam(required = false) String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {
        if (pagina < 0) {
            pagina = 0;
        }

        model.addAttribute("produtos", produtoService.buscar(nome, tipo, ativo, ordem, pagina));

        // devolve os filtros pra tela continuar preenchida
        model.addAttribute("nome", nome);
        model.addAttribute("tipo", tipo);
        model.addAttribute("ativo", ativo);
        model.addAttribute("ordem", ordem);

        model.addAttribute("content", "produtos/lista :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @GetMapping("/produtos/novo")
    public String novoProdutoForm(Model model) {
        Produto produto = new Produto();
        produto.setAtivo(true); // já vem marcado
        model.addAttribute("produto", produto);
        model.addAttribute("content", "produtos/form :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @PostMapping("/produtos/salvar")
    public String salvarProduto(@ModelAttribute Produto produto,
            RedirectAttributes redirectAttributes) {
        produtoService.salvar(produto);
        redirectAttributes.addFlashAttribute("sucesso", "Produto criado com sucesso!");
        return "redirect:/produtos";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @GetMapping("/produtos/editar/{id}")
    public String editarProdutoForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = produtoService.buscarPorId(id).map(p -> {
            model.addAttribute("produto", p);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Produto não encontrado.");
            return "redirect:/produtos";
        }

        model.addAttribute("content", "produtos/form :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @PostMapping("/produtos/editar/{id}")
    public String atualizarProduto(@PathVariable String id,
            @ModelAttribute Produto produto,
            RedirectAttributes redirectAttributes) {
        produto.setId(id);
        produtoService.salvar(produto);
        redirectAttributes.addFlashAttribute("sucesso", "Produto atualizado com sucesso!");
        return "redirect:/produtos";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @PostMapping("/produtos/excluir/{id}")
    public String excluirProduto(@PathVariable String id,
            RedirectAttributes redirectAttributes) {
        produtoService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Produto removido com sucesso!");
        return "redirect:/produtos";
    }

    // ========================
    // CLIENTES
    // ========================
    @GetMapping("/clientes")
    public String clientes(@RequestParam(required = false) String busca,
            @RequestParam(required = false) String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {
        if (pagina < 0) {
            pagina = 0;
        }

        model.addAttribute("clientes", clienteService.buscar(busca, ordem, pagina));

        // devolve os filtros pra tela continuar preenchida
        model.addAttribute("busca", busca);
        model.addAttribute("ordem", ordem);

        model.addAttribute("content", "clientes/lista :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @GetMapping("/clientes/novo")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PostMapping("/clientes/salvar")
    public String salvarCliente(@ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        clienteService.salvar(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model model,
            RedirectAttributes redirectAttributes) {
        boolean encontrado = clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return true;
        }).orElse(false);

        if (!encontrado) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }

        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    @PostMapping("/clientes/editar/{id}")
    public String atualizarCliente(@PathVariable String id,
            @ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        cliente.setId(id);
        clienteService.salvar(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    @PreAuthorize("hasRole('GERENTE')")
    @PostMapping("/clientes/excluir/{id}")
    public String excluirCliente(@PathVariable String id,
            RedirectAttributes redirectAttributes) {
        clienteService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente removido com sucesso!");
        return "redirect:/clientes";
    }

    // ========================
    // AUDITORIA
    // ========================
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @GetMapping("/auditoria")
    public String auditoria(@RequestParam(required = false) String usuario,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) String ordem,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {
        if (pagina < 0) {
            pagina = 0;
        }

        model.addAttribute("registros", auditoriaService.buscar(usuario, entidade, acao, campo, ordem, pagina));
        model.addAttribute("ordem", ordem);

        // devolve os filtros pra tela continuar preenchida
        model.addAttribute("usuario", usuario);
        model.addAttribute("entidade", entidade);
        model.addAttribute("acao", acao);
        model.addAttribute("campo", campo);

        model.addAttribute("content", "auditoria/lista :: content");
        return "layout";
    }

    // ========================
    // ERROS
    // ========================
    // quando o service barra alguma coisa volta pra tela anterior com a mensagem
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String erroDeRegra(RuntimeException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("erro", e.getMessage());

        String voltar = request.getHeader("Referer");
        if (voltar == null) {
            voltar = "/";
        }
        return "redirect:" + voltar;
    }
}
