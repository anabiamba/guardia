package br.com.guardia.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import br.com.guardia.model.EntregaPush;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EntregaPushRepository extends JpaRepository<EntregaPush, Long> {
    @Query(value = """
            SELECT d.* FROM entregas_push d JOIN eventos_push e ON e.id = d.evento_id
            WHERE e.modo = :modo AND
              ((d.estado = 'PENDENTE' AND d.proxima_tentativa <= :agora) OR
               (d.estado = 'PROCESSANDO' AND d.reservada_ate <= :agora))
            ORDER BY d.id LIMIT :lote FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EntregaPush> reservarPendentes(@Param("modo") String modo, @Param("agora") Instant agora, @Param("lote") int lote);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from EntregaPush d where d.id = :id")
    Optional<EntregaPush> bloquear(@Param("id") Long id);
}
