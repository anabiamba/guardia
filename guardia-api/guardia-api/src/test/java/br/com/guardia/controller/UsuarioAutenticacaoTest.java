package br.com.guardia.controller;

import java.util.Optional;

import br.com.guardia.config.PasswordConfig;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.UsuarioRepository;
import br.com.guardia.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioAutenticacaoTest {

    private static final String EMAIL = "conta@example.test";
    private static final String SENHA_FICTICIA = "exemplo-teste-26";

    private final UsuarioRepository repository =
            mock(UsuarioRepository.class, withSettings().mockMaker(MockMakers.PROXY));
    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(
                new UsuarioController(new UsuarioService(repository, encoder))).build();
    }

    @Test
    void cadastraComBcryptESemRetornarCredencial() throws Exception {
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(chamada -> {
            Usuario usuario = chamada.getArgument(0);
            assertTrue(encoder.matches(SENHA_FICTICIA, usuario.getSenha()));
            usuario.setId(1L);
            return usuario;
        });

        mvc.perform(post("/api/usuarios").contentType("application/json")
                        .content(cadastro("Pessoa de teste", EMAIL, SENHA_FICTICIA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.senha").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.hash").doesNotHaveJsonPath());
    }

    @Test
    void autenticaContaComSenhaValidaSemRetornarCredencial() throws Exception {
        cadastrarNoRepositorio(encoder.encode(SENHA_FICTICIA));

        login(EMAIL, SENHA_FICTICIA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.senha").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.hash").doesNotHaveJsonPath());
    }

    @Test
    void rejeitaSenhaIncorretaEContaInexistenteComMesmoRetorno() throws Exception {
        cadastrarNoRepositorio(encoder.encode(SENHA_FICTICIA));

        String resposta = login(EMAIL, "entrada-incorreta")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        login("ausente@example.test", SENHA_FICTICIA)
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(resposta));
    }

    @Test
    void rejeitaSenhaLegadaSemAlterarConta() throws Exception {
        cadastrarNoRepositorio(SENHA_FICTICIA);

        login(EMAIL, SENHA_FICTICIA).andExpect(status().isUnauthorized());
        verify(repository).findByEmail(EMAIL);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        when(repository.existsByEmail(EMAIL)).thenReturn(true);

        mvc.perform(post("/api/usuarios").contentType("application/json")
                        .content(cadastro("Pessoa de teste", EMAIL, SENHA_FICTICIA)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("E-mail já cadastrado."));
    }

    @Test
    void validaCadastroELoginAntesDeConsultarRepositorio() throws Exception {
        for (String corpo : new String[]{
                cadastro(" ", EMAIL, SENHA_FICTICIA),
                cadastro("Pessoa de teste", "invalido", SENHA_FICTICIA),
                cadastro("Pessoa de teste", EMAIL, "curta"),
                cadastro("Pessoa de teste", EMAIL, "á".repeat(37))}) {
            mvc.perform(post("/api/usuarios").contentType("application/json").content(corpo))
                    .andExpect(status().isBadRequest());
        }
        login("invalido", SENHA_FICTICIA).andExpect(status().isBadRequest());
        login(EMAIL, "").andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    private void cadastrarNoRepositorio(String senhaArmazenada) {
        Usuario usuario = new Usuario("Pessoa de teste", EMAIL, senhaArmazenada, null, null);
        usuario.setId(1L);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    private ResultActions login(String email, String senha)
            throws Exception {
        return mvc.perform(post("/api/usuarios/login").contentType("application/json")
                .content("""
                        {"email":"%s","senha":"%s"}
                        """.formatted(email, senha)));
    }

    private static String cadastro(String nome, String email, String senha) {
        return """
                {"nome":"%s","email":"%s","senha":"%s"}
                """.formatted(nome, email, senha);
    }

    @Test void rejeitaSenhaFracaMesmoSemAndroidESemConsultarRepositorio() throws Exception {
        for (String entrada : new String[]{"12345678", "somenteletras", "Abcd123 ", "1234567!"}) {
            mvc.perform(post("/api/usuarios").contentType("application/json")
                .content(cadastro("Fictícia", EMAIL, entrada))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }
    @Test void contaBcryptAntigaAindaAutenticaSemRegraDeComposicao() throws Exception {
        cadastrarNoRepositorio(encoder.encode("somenteletras"));
        login(EMAIL, "somenteletras").andExpect(status().isOk());
    }
    @Test void servicoDeCadastroTambemRejeitaCredencialFraca() {
        var service = new UsuarioService(repository, encoder);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.cadastrar(new br.com.guardia.dto.CadastroRequest("Fictícia", EMAIL, "12345678")));
        verifyNoInteractions(repository);
    }

}
