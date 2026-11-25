package br.com.guardia.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.guardia.model.Alerta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(Long usuarioId);

    List<Alerta> findByAtivoTrueAndDataHoraCriacaoAfter(LocalDateTime limite);

    Optional<Alerta> findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(
            Long usuarioId, LocalDateTime limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alerta a where a.id = :id")
    Optional<Alerta> bloquear(@Param("id") Long id);

    @Modifying
    @Query("""
            update Alerta a set a.ativo = false, a.dataHoraEncerramento = :agora
            where a.ativo = true and a.dataHoraCriacao <= :limite
            """)
    int encerrarExpirados(@Param("limite") LocalDateTime limite, @Param("agora") LocalDateTime agora);
}
