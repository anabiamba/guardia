package br.com.guardia.config;

import org.springframework.core.env.Environment;

public record LimitesPushTeste(long usuarioId, String instalacaoHash, String projetoId) {
    public LimitesPushTeste {
        if (usuarioId <= 0 || instalacaoHash == null || !instalacaoHash.matches("[a-f0-9]{64}") ||
                projetoId == null || !projetoId.matches("[a-z][a-z0-9-]{4,28}[a-z0-9]")) {
            throw new IllegalStateException("Configure o projeto Firebase, a conta e a instalação do ensaio.");
        }
    }

    public static LimitesPushTeste carregar(Environment ambiente) {
        try {
            return new LimitesPushTeste(Long.parseLong(ambiente.getProperty("GUARDIA_PUSH_TEST_USER_ID", "")),
                    ambiente.getProperty("GUARDIA_PUSH_TEST_INSTALLATION_HASH"),
                    ambiente.getProperty("GUARDIA_PUSH_TEST_PROJECT_ID"));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Defina a conta fictícia destinatária do ensaio.");
        }
    }

    @Override
    public String toString() {
        return "LimitesPushTeste[configuração restrita]";
    }
}
