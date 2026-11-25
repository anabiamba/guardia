package br.com.guardia.service;

import java.time.LocalDateTime;
import java.util.List;
import br.com.guardia.model.MensagemAlerta;
import br.com.guardia.repository.MensagemAlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatAlertaService {
    private final AlertaService alertas;
    private final MensagemAlertaRepository mensagens;

    public ChatAlertaService(AlertaService alertas, MensagemAlertaRepository mensagens) {
        this.alertas = alertas;
        this.mensagens = mensagens;
    }

    @Transactional
    public List<Mensagem> listar(Long alertaId, Long usuariaId, long depois) {
        alertas.exigirParticipanteDoChat(alertaId, usuariaId);
        return mensagens.findTop50ByAlertaIdAndIdGreaterThanOrderByIdAsc(alertaId, depois)
                .stream().map(Mensagem::de).toList();
    }

    @Transactional
    public Mensagem enviar(Long alertaId, Long usuariaId, String texto) {
        var agora = alertas.exigirParticipanteDoChat(alertaId, usuariaId);
        return Mensagem.de(mensagens.save(new MensagemAlerta(alertaId, usuariaId, texto.strip(), agora)));
    }

    public record Mensagem(Long id, Long remetenteId, String texto, LocalDateTime enviadaEm) {
        static Mensagem de(MensagemAlerta mensagem) {
            return new Mensagem(mensagem.getId(), mensagem.getRemetenteId(),
                    mensagem.getTexto(), mensagem.getEnviadaEm());
        }

        @Override
        public String toString() { return "Mensagem"; }
    }
}
