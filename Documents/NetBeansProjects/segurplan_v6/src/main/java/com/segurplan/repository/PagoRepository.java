package com.segurplan.repository;

import com.segurplan.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByCotizacion_IdCotizacionAndEstado(
            Long idCotizacion, String estado);

    List<Pago> findByUsuario_IdUsuarioOrderByFechaPagoDesc(Long idUsuario);
}
