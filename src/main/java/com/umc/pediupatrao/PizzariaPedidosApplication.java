package com.umc.pediupatrao;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PizzariaPedidosApplication {

    public static void main(String[] args) {
        // horario de brasilia sempre, n importa o fuso da maquina
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(PizzariaPedidosApplication.class, args);
    }
}
