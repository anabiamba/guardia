package br.com.guardia.repository;

import java.util.List;
import br.com.guardia.model.MensagemAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemAlertaRepository extends JpaRepository<MensagemAlerta, Long> {
    List<MensagemAlerta> findTop50ByAlertaIdAndIdGreaterThanOrderByIdAsc(Long alertaId, Long depois);
}
