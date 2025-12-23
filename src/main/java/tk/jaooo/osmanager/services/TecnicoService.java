package tk.jaooo.osmanager.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.jaooo.osmanager.exception.ResourceNotFoundException;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.model.dto.TecnicoRegisterDTO;
import tk.jaooo.osmanager.repository.ReparoRepository;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.List;

@Service
public class TecnicoService {

    private final TecnicoRepository tecnicoRepository;
    private final ReparoRepository reparoRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemandanetClientService demandanetClientService;
    private final OciSecretsService ociSecretsService;

    public TecnicoService(TecnicoRepository tecnicoRepository,
                          ReparoRepository reparoRepository,
                          PasswordEncoder passwordEncoder,
                          DemandanetClientService demandanetClientService,
                          OciSecretsService ociSecretsService) {
        this.tecnicoRepository = tecnicoRepository;
        this.reparoRepository = reparoRepository;
        this.passwordEncoder = passwordEncoder;
        this.demandanetClientService = demandanetClientService;
        this.ociSecretsService = ociSecretsService;
    }

    /**
     * FLUXO 1: Auto-cadastro Público.
     * Exige credenciais válidas do Demandanet.
     */
    @Transactional
    public Tecnico registrarTecnicoComDemandanet(TecnicoRegisterDTO dto) {
        validarUsernameUnico(dto.username());

        if (dto.demandanetUser() == null || dto.demandanetUser().isBlank() ||
                dto.demandanetPassword() == null || dto.demandanetPassword().isBlank()) {
            throw new IllegalArgumentException("Para cadastro público, usuário e senha do Demandanet são obrigatórios.");
        }

        // 1. Valida se as credenciais do legado funcionam
        try {
            String sessionTest = demandanetClientService.loginAndGetSessionCookie(dto.demandanetUser(), dto.demandanetPassword())
                    .block();
            if (sessionTest == null) throw new RuntimeException("Login falhou");
        } catch (Exception e) {
            throw new IllegalArgumentException("Credenciais do Demandanet inválidas. Não foi possível autenticar no sistema legado.");
        }

        // 2. Salva as credenciais no cofre da OCI
        String vaultOcid = ociSecretsService.createSecretForUser(
                dto.username(),
                dto.demandanetUser(),
                dto.demandanetPassword()
        );

        // 3. Cria o usuário localmente
        Tecnico novoTecnico = Tecnico.builder()
                .nome(dto.nome())
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password())) // Hash da senha do app
                .estagiario(dto.estagiario())
                .credentialKey(vaultOcid) // Vincula ao segredo na nuvem
                .responsavel(null) // É autônomo pois tem credencial própria
                .removido(false)
                .build();

        return tecnicoRepository.save(novoTecnico);
    }

    /**
     * FLUXO 2: Cadastro Interno.
     * Criado por um usuário logado (pai/responsável).
     */
    @Transactional
    public Tecnico criarTecnicoInterno(TecnicoRegisterDTO dto, Tecnico responsavel) {
        validarUsernameUnico(dto.username());

        Tecnico novoTecnico = Tecnico.builder()
                .nome(dto.nome())
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .estagiario(dto.estagiario())
                .credentialKey(null) // Não tem acesso direto ao legado
                .responsavel(responsavel) // Herda permissão de quem criou
                .removido(false)
                .build();

        return tecnicoRepository.save(novoTecnico);
    }

    private void validarUsernameUnico(String username) {
        if (tecnicoRepository.findByUsernameAndRemovidoIsFalse(username).isPresent()) {
            throw new IllegalArgumentException("Já existe um técnico ativo com este usuário.");
        }
    }

    public List<Tecnico> listarTodos() {
        return tecnicoRepository.findAll();
    }

    public Tecnico buscarPorId(Long id) {
        return tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado com o ID: " + id));
    }

    public Tecnico atualizarTecnico(Long id, Tecnico dadosTecnico) {
        Tecnico tecnicoExistente = buscarPorId(id);
        tecnicoExistente.setNome(dadosTecnico.getNome());
        tecnicoExistente.setEstagiario(dadosTecnico.isEstagiario());
        return tecnicoRepository.save(tecnicoExistente);
    }

    @Transactional
    public void deletarTecnico(Long id) {
        Tecnico tecnico = buscarPorId(id);
        boolean hasReparos = !reparoRepository.findByTecnico_Id(id).isEmpty();

        if (hasReparos) {
            tecnico.setRemovido(true);
            tecnicoRepository.save(tecnico);
        } else {
            tecnicoRepository.delete(tecnico);
        }
    }

    public Tecnico restaurarTecnico(Long id) {
        Tecnico tecnicoRemovido = buscarPorId(id);
        if (!tecnicoRemovido.isRemovido()) {
            throw new IllegalStateException("O técnico não está marcado como removido.");
        }
        tecnicoRemovido.setRemovido(false);
        return tecnicoRepository.save(tecnicoRemovido);
    }
}
