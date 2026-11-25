# Instalação limpa do ambiente de demonstração

**Proposta preparada em 24/11/2026. Não executada no MySQL.** Instalação limpa é diferente de atualização de ambiente existente. O banco local em uso durante o desenvolvimento já possui dados e não deve receber este arquivo.

## Escopo

`instalacao-limpa-guardia-teste.sql` define sete tabelas do mapeamento JPA atual: usuarios, sessoes, alertas, vinculos_fcm, mensagens_alerta, eventos_push e entregas_push. Não copia dados ou cria contas, senhas, usuários MySQL ou schemas. Não substitui arquivos de configuração nem altera o perfil Spring. Não executa scripts automaticamente.

Todas as instruções apontam explicitamente para `guardia_teste`. O script não é uma proteção contra uso de credenciais administrativas: a barreira de acesso deve ser o usuário `guardia_teste_app`, restrito a esse schema. Uma pessoa responsável pelo ambiente deve preparar esse schema vazio e o usuário restrito separadamente, com senha definida fora de arquivos públicos. Esta preparação administrativa não foi executada nem está autorizada por este documento.

## Antes de qualquer execução futura

1. Revisar o SQL e obter autorização para executar DDL em uma instalação nova. Não usar o banco local de desenvolvimento preenchido.
2. Confirmar MySQL compatível com InnoDB, utf8mb4 e collation utf8mb4_0900_ai_ci (MySQL 8+). Compatibilidade da proposta ainda depende de validação real. Não mudar collation de um banco existente com este arquivo.
3. Conectar localmente ao schema guardia_teste usando guardia_teste_app, nunca root. Não colocar senha no comando ou relatório.
4. Executar apenas as consultas de leitura abaixo e conferir os resultados antes de autorizar o DDL:

```sql
SELECT DATABASE() AS banco, CURRENT_USER() AS usuario;
SHOW GRANTS FOR CURRENT_USER;
SELECT COUNT(*) AS tabelas_existentes
FROM information_schema.tables
WHERE table_schema = 'guardia_teste';
```

Esperado: banco guardia_teste, usuário guardia_teste_app e privilégios somente nesse schema, sem privilégios globais de dados/DDL; zero tabelas. Se qualquer condição divergir, interromper. `USE` muda o schema, não muda o usuário ou seus privilégios.

## Execução futura — ainda não autorizada

Somente após revisão dos resultados acima, aplicar uma vez o arquivo **instalacao-limpa-guardia-teste.sql**. O arquivo não contém guard procedural que torne segura uma execução sem essa verificação prévia. Não usar opção de continuar ignorando erros, não executar repetidamente e não remover erros com IF NOT EXISTS. A ausência de IF NOT EXISTS é proposital para não ocultar estrutura incompatível.

Não aplicar depois os scripts aditivos de fila/localização sobre a estrutura nova: ela já inclui essas tabelas e coluna. Eles são históricos/propostas de atualização para contextos específicos e não uma sequência de instalação.

Se houver falha, parar e identificar o que já foi criado. DDL MySQL pode ter commits implícitos: o arquivo inteiro não é uma transação com rollback garantido. Não apagar tabelas, repetir automaticamente ou usar ddl-auto=update/create como correção. Um plano de recuperação precisa de autorização própria, avaliando se há dados ou uso concorrente.

## Verificação futura após criação

Conferir estrutura e iniciar a API somente com perfil teste, notificações simuladas e ddl-auto=validate. A primeira inicialização valida tipos/colunas pelo Hibernate; se falhar, interromper e revisar a proposta, sem alterar o esquema automaticamente. Depois, criar contas fictícias pela API/Android, nunca inserir senha em texto simples por SQL. Casos de cadastro/login, sessão, vínculo FCM, SOS, ajuda e chat devem ser executados no ambiente isolado.

**Não afirmar compatibilidade homologada com Hibernate/MySQL antes dessa etapa.** Conferência estática não substitui execução. Scripts completos preparados não significam instalação nova já comprovada.

## Correspondência com o código

- UUID de Sessao.instalacaoId usa BINARY(16); token_hash e hashes textuais usam VARCHAR(64).
- Instant/LocalDateTime usam DATETIME(6), seguindo os scripts já usados no projeto. Interpretar horários conforme configuração do backend; não converter registros existentes.
- versao de Sessao permanece BIGINT, sem aplicar valores a sessões existentes.
- Unicidade: e-mail, revogacao_hash, sessão por instalação FCM e hash do token FCM. Hashes nulos podem se repetir no MySQL; não representam um token registrado.
- Evento push é único por alerta; entrega é única por evento/instalação/sessão. Índices de mensagens e fila seguem o JPA/script existente.
- Somente EntregaPush.evento é relação JPA e possui chave estrangeira para eventos_push. Outros IDs são campos escalares no modelo atual; não acrescentadas cascatas ou novas relações.
- Campos FCM antigos continuam presentes para compatibilidade, sem voltar a selecionar destinatários por eles.

Nenhum dado, segredo ou credencial consta do arquivo. Licenças/autoria/publicação e demais pendências continuam separadas.
