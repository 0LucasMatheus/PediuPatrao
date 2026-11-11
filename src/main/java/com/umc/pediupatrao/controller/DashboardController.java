package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.ProdutoVendido;
import com.umc.pediupatrao.dto.QuantidadeStatus;
import com.umc.pediupatrao.dto.ResumoDashboard;
import com.umc.pediupatrao.dto.VendaHora;
import com.umc.pediupatrao.dto.VendaPeriodo;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// dados do dashboard, o preauthorize em cima da classe vale pra todos os endpoints
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/resumo")
    public ResumoDashboard resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        // sem data pega os ultimos 30 dias contando hoje
        if (fim == null) {
            fim = LocalDate.now();
        }
        if (inicio == null) {
            inicio = fim.minusDays(29);
        }
        return dashboardService.resumo(inicio, fim);
    }

    @GetMapping("/vendas")
    public List<VendaPeriodo> vendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "dia") String agrupar) {
        if (fim == null) {
            fim = LocalDate.now();
        }
        if (inicio == null) {
            inicio = fim.minusDays(29);
        }
        return dashboardService.vendas(inicio, fim, agrupar);
    }

    @GetMapping("/vendas-por-hora")
    public List<VendaHora> vendasPorHora(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        if (fim == null) {
            fim = LocalDate.now();
        }
        if (inicio == null) {
            inicio = fim.minusDays(29);
        }
        return dashboardService.vendasPorHora(inicio, fim);
    }

    @GetMapping("/status")
    public List<QuantidadeStatus> status(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        if (fim == null) {
            fim = LocalDate.now();
        }
        if (inicio == null) {
            inicio = fim.minusDays(29);
        }
        return dashboardService.status(inicio, fim);
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoVendido> produtosMaisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(required = false) String tipo) {
        if (fim == null) {
            fim = LocalDate.now();
        }
        if (inicio == null) {
            inicio = fim.minusDays(29);
        }
        return dashboardService.produtosMaisVendidos(inicio, fim, limite, tipo);
    }
}
