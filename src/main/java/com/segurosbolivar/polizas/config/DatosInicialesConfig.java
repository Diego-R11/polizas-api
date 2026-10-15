package com.segurosbolivar.polizas.config;

import com.segurosbolivar.polizas.domain.Poliza;
import com.segurosbolivar.polizas.domain.Riesgo;
import com.segurosbolivar.polizas.domain.TipoPoliza;
import com.segurosbolivar.polizas.repository.PolizaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Carga pólizas de ejemplo para poder probar la API apenas arranca (no se ejecuta en tests). */
@Configuration
@Profile("!test")
public class DatosInicialesConfig {

    @Bean
    CommandLineRunner cargarDatos(PolizaRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            Poliza individual = new Poliza(TipoPoliza.INDIVIDUAL, "Laura Gómez", "Carlos Pérez",
                    LocalDate.of(2026, 1, 1), 12, new BigDecimal("1800000"));
            individual.agregarRiesgo(new Riesgo("Calle 80 # 15-20 Apto 301, Bogotá", "Laura Gómez"));

            Poliza colectiva = new Poliza(TipoPoliza.COLECTIVA, "Inmobiliaria Andes S.A.S.", "Arrendadores asociados",
                    LocalDate.of(2026, 3, 1), 6, new BigDecimal("2500000"));
            colectiva.agregarRiesgo(new Riesgo("Carrera 7 # 72-10 Apto 502, Bogotá", "Andrés Ruiz"));
            colectiva.agregarRiesgo(new Riesgo("Calle 100 # 19-45 Apto 1203, Bogotá", "María Torres"));

            Poliza cancelada = new Poliza(TipoPoliza.INDIVIDUAL, "Jorge Díaz", "Ana Rincón",
                    LocalDate.of(2025, 6, 1), 12, new BigDecimal("1200000"));
            cancelada.agregarRiesgo(new Riesgo("Avenida 68 # 22-31, Bogotá", "Jorge Díaz"));
            cancelada.cancelar();

            repo.save(individual);
            repo.save(colectiva);
            repo.save(cancelada);
        };
    }
}
