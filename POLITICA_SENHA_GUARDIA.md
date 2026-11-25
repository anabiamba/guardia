# Política de senha do Guardiã°

Decisão aprovada em 23/11/2026: novas senhas devem ter no mínimo oito caracteres, pelo menos uma letra, um número e um caractere especial. Não é obrigatório misturar maiúsculas e minúsculas. A regra vale no cadastro e na troca de senha, com validação no Android e obrigatória no backend, inclusive em chamadas diretas aos serviços.

Caracteres são contados por pontos de código Unicode. Letras acentuadas são aceitas. Pontuação e símbolos contam como especiais; espaços, quebras de linha e caracteres de controle não substituem um símbolo. Espaços podem fazer parte da senha quando os demais requisitos são cumpridos. Controles e sequências Unicode malformadas são rejeitados. Não há conversão de maiúsculas/minúsculas, normalização, remoção de espaços ou truncamento.

O limite permanece 72 bytes em UTF-8 por compatibilidade com BCrypt. Acentos e emojis podem consumir mais de um byte. Entrada acima do limite é rejeitada antes de criar o novo hash. O algoritmo e custo BCrypt existentes foram preservados. A política de composição é uma decisão do projeto, não uma afirmação de conformidade integral com padrões de segurança.

## Compatibilidade

- Login e senha atual não recebem a nova exigência de composição: contas existentes com BCrypt continuam autenticando com a senha que já possuíam.
- A nova senha escolhida na troca deve atender à regra e ser diferente da atual.
- Troca válida continua revogando as sessões da conta e limpando seus vínculos FCM. Troca rejeitada não altera senha nem revoga sessões.
- Contas antigas em texto simples continuam recusadas pelo fluxo existente; não foi feita migração, redefinição ou atualização em massa.
- Campos, rotas e códigos de sucesso permanecem: cadastro 201, troca 204. Senha nova inválida retorna 400. Mensagens não incluem o valor informado.
- Removido do Android o log da resposta completa do cadastro. Senhas não devem ser incluídas em logs, relatórios ou mensagens de teste.

## Validação manual pendente

No ambiente teste/guardia_teste, com notificações simuladas e ddl-auto=validate, reiniciar a API e executar o Android atualizado. Sem informar senhas no chat:

1. Confirmar que uma conta fictícia já existente continua entrando.
2. No cadastro, verificar rejeição de entradas curtas ou sem cada categoria; espaço sozinho não vale como símbolo. Não compartilhar os valores usados.
3. Criar outra conta fictícia com senha conforme a regra e verificar o login.
4. Trocar a senha de uma conta fictícia: erro não deve encerrar sua sessão; sucesso deve voltar ao login, rejeitar a senha antiga e aceitar a nova.
5. Conferir fila offline/vínculos e múltiplas sessões nas regressões pendentes, conforme autorização específica para integração real. Testes com mocks não comprovam bloqueios/rollback MySQL.

Resultados reais e comandos automatizados ficam no PROGRESSO_GUARDIA.md. Nenhuma integração com banco ou senha real foi executada para implementar esta política.
