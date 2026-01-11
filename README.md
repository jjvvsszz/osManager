# OS Manager API

**OS Manager** é uma API REST robusta desenvolvida em **Java 25** com **Spring Boot 4**, projetada para o gerenciamento híbrido de Ordens de Serviço (OS). O sistema atua como uma ponte entre um controle interno moderno e um sistema legado (Demandanet), utilizando Inteligência Artificial (Google Gemini) para auxiliar na geração de relatórios técnicos e Oracle Cloud Infrastructure (OCI) para segurança de credenciais.

## 🚀 Tecnologias

-   **Java 25**
-   **Spring Boot 4.0.1**
-   **Oracle Database** (Autonomous DB na Oracle Cloud)
-   **OCI SDK** (Oracle Vault & Secrets)
-   **Google Gemini AI** (Generative AI)
-   **Spring Security + JWT**
-   **WebClient** (Integração Reativa)

---

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter instalado:

1.  **JDK 25**: O projeto utiliza recursos modernos do Java.
2.  **Conta Oracle Cloud**: Para acesso ao Banco de Dados Autônomo e OCI Vault.
3.  **Conta Google AI Studio**: Para obter a chave da API do Gemini.

---

## ⚙️ Configuração do Ambiente

Por razões de segurança, este projeto não inclui credenciais ou arquivos de configuração sensíveis no repositório. Você deve configurá-los manualmente.

### 1. Variáveis de Ambiente

O projeto depende das seguintes variáveis de ambiente para rodar. Você pode configurá-las no seu sistema operacional, na sua IDE (IntelliJ/Eclipse) ou em um arquivo `.env` (se usar Docker).

| Variável | Descrição | Exemplo |
| :--- | :--- | :--- |
| `DB_USERNAME` | Usuário do banco Oracle | `ADMIN` |
| `DB_PASSWORD` | Senha do banco Oracle | `SuaSenhaForte123` |
| `JWT_SECRET` | Chave secreta para assinar tokens (HS512) | `uma_hash_sha512_muito_longa...` |
| `VAULT_COMPARTMENT_ID` | OCID do Compartment no OCI | `ocid1.compartment.oc1..aaaa...` |
| `VAULT_ID` | OCID do Vault (Cofre) | `ocid1.vault.oc1.sa-saopaulo-1...` |
| `VAULT_ENCRYPTION_KEY_ID` | OCID da Master Encryption Key | `ocid1.key.oc1.sa-saopaulo-1...` |
| `GEMINI_API_KEY` | Chave de API do Google AI Studio | `AIzaSy...` |
| `GEMINI_MODEL` | Modelo do Gemini a ser usado | `gemini-flash-lite-latest` |

### 2. Configuração do Oracle Wallet

Para conectar ao Oracle Autonomous Database, é necessário o **Wallet**.

1.  Baixe o Wallet do seu banco de dados no console da Oracle Cloud.
2.  Descompacte os arquivos (`cwallet.sso`, `ewallet.p12`, `tnsnames.ora`, etc.).
3.  Crie a pasta `wallet` dentro dos resources do projeto:
    ```
    osManager/src/main/resources/wallet/
    ```
4.  Cole os arquivos descompactados dentro desta pasta.

> **⚠️ IMPORTANTE:** Certifique-se de que a pasta `src/main/resources/wallet/` esteja listada no seu `.gitignore` para não vazar credenciais de banco de dados.

### 3. Configuração do OCI CLI (`.oci`)

Para que o `OciSecretsService` funcione (autenticação com a Oracle Cloud para ler segredos), você precisa configurar o arquivo de configuração local do OCI SDK.

1.  Na sua máquina, crie a pasta `.oci` no diretório do usuário:
    *   **Windows:** `C:\Users\SeuUsuario\.oci\`
    *   **Linux/Mac:** `~/.oci/`
2.  Gere um par de chaves API no console da Oracle (User Settings -> API Keys).
3.  Baixe a chave privada (`*.pem`) e coloque na pasta `.oci`.
4.  Crie um arquivo chamado `config` dentro da pasta `.oci` com o seguinte conteúdo (os valores são fornecidos pelo console da Oracle ao gerar a chave):

```ini
[DEFAULT]
user=ocid1.user.oc1..aaaa...
fingerprint=xx:xx:xx...
key_file=C:\Users\SeuUsuario\.oci\oci_api_key.pem
tenancy=ocid1.tenancy.oc1..aaaa...
region=sa-saopaulo-1
