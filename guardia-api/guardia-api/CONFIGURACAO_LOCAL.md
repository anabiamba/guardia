# Configuração local sem credenciais no repositório

Use o perfil teste para a demonstração acadêmica: MySQL local guardia_teste, usuário guardia_teste_app com acesso somente a esse schema e Firebase simulado. Não utilizar contas ou dados reais.

1. Em uma cópia nova, copiar src/main/resources/application-teste.properties.example para src/main/resources/application-teste.properties. Não sobrescrever um arquivo local existente sem revisá-lo. O arquivo real é ignorado pelo Git.
2. Definir GUARDIA_TEST_DB_PASSWORD na mesma sessão do PowerShell, por entrada mascarada. Arquivos .env não são carregados automaticamente pelo Spring Boot.
3. Usar um schema previamente preparado. Não habilitar update/create para resolver erros de validação. Esta preparação não cria tabelas nem executa migrações.
4. Iniciar explicitamente com perfil teste e validate. O perfil teste mantém Firebase Admin real desativado e notificações simuladas. Não habilitar teste-push para esta demonstração sem autorização específica.

~~~powershell
$env:GUARDIA_TEST_DB_PASSWORD = Read-Host 'Senha do banco de teste' -MaskInput
if ([string]::IsNullOrWhiteSpace($env:GUARDIA_TEST_DB_PASSWORD)) { throw 'Senha ausente.' }
.\mvnw.cmd '-Dspring-boot.run.profiles=teste' '-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=validate --guardia.sessao.duracao=PT24H' spring-boot:run
~~~

Maven requer JDK compatível com o projeto; Java 17 é o alvo de compilação. No ambiente de desenvolvimento atual utiliza-se o JBR do Android Studio. Não versionar distribuições locais do JDK.

## Outros perfis

A configuração principal exige GUARDIA_DB_URL, GUARDIA_DB_USERNAME e GUARDIA_DB_PASSWORD sem valores padrão. Não há mais senha literal ou banco selecionado por padrão nesse arquivo. A ausência de configuração deve bloquear a inicialização, em vez de escolher um ambiente real automaticamente. O Hibernate usa validate por padrão; alterações de esquema exigem planejamento separado.

Os demais perfis preservam seu comportamento Firebase. Quando necessário fora do perfil simulado, GOOGLE_APPLICATION_CREDENTIALS deve apontar para um arquivo de conta de serviço mantido fora do repositório. Não copiar o conteúdo desse arquivo para código, documentação, issues ou logs. A configuração não torna o perfil principal um ambiente de teste nem uma implantação pronta para produção.

## Antes de publicar

Revisar arquivos candidatos e eventuais históricos existentes. .gitignore não remove segredos de arquivos já versionados ou do histórico. Se alguma credencial já tiver sido publicada/compartilhada indevidamente, revogá-la e substituí-la; exclusão do arquivo não basta. Esta preparação não altera senhas no MySQL nem prova ausência de exposição anterior.

## Coluna de atualização da localização

A nova entidade Usuario exige localizacao_atualizada_em. Consulte sql/LOCALIZACAO.md e a proposta sql/2026-11-22-localizacao-teste.sql. No ambiente local de testes, a coluna foi preparada pela responsável e validada posteriormente pelo Hibernate. Isso não prepara o banco de uma cópia nova. Com validate, é necessário aplicar a mudança aditiva autorizada exclusivamente em guardia_teste antes de iniciar a nova versão. Não habilitar update/create nem tratar posições legadas como recentes por backfill automático. Validade configurável guardia.localizacao.validade, padrão PT15M.

