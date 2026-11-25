package br.com.guardia.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import br.com.guardia.controller.AlertaController;
import br.com.guardia.controller.NotificacaoController;
import br.com.guardia.controller.UsuarioController;
import br.com.guardia.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AcessoApiConfig implements WebMvcConfigurer {

    public static final String IDENTIDADE = "br.com.guardia.identidade";
    private static final List<String> USUARIOS_PUBLICOS = List.of("cadastrar", "login", "teste");

    private final SessaoService sessoes;
    private final boolean legadoLocal;

    public AcessoApiConfig(SessaoService sessoes, Environment ambiente,
                           @Value("${guardia.legado-local.habilitado:false}") boolean habilitado) {
        this.sessoes = sessoes;
        this.legadoLocal = habilitado
                && ambiente.acceptsProfiles(Profiles.of("dev-local"))
                && "127.0.0.1".equals(ambiente.getProperty("server.address"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) {
                return protegerLegado(request, response, handler);
            }
        });
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                     Object handler) {
                return autenticar(request, response);
            }
        }).addPathPatterns("/api/v2/**").excludePathPatterns("/api/v2/sessoes/login", "/api/v2/sessoes/revogacoes");
    }

    private boolean protegerLegado(HttpServletRequest request, HttpServletResponse response,
                                   Object handler) {
        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        Class<?> controller = metodo.getBeanType();
        if (controller == NotificacaoController.class
                || ((controller == AlertaController.class
                        || (controller == UsuarioController.class
                            && !USUARIOS_PUBLICOS.contains(metodo.getMethod().getName())))
                    && !(legadoLocal && requisicaoLocal(request)))) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        return true;
    }

    private static boolean requisicaoLocal(HttpServletRequest request) {
        boolean encaminhada = Collections.list(request.getHeaderNames()).stream()
                .anyMatch(nome -> nome.equalsIgnoreCase("Forwarded")
                        || nome.regionMatches(true, 0, "X-Forwarded-", 0, 12));
        return !encaminhada && loopback(request.getRemoteAddr()) && loopback(request.getLocalAddr());
    }

    private static boolean loopback(String endereco) {
        try {
            return endereco != null && InetAddress.getByName(endereco).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean autenticar(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.size() == 1) {
            String header = headers.get(0);
            if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                var identidade = sessoes.autenticar(header.substring(7));
                if (identidade.isPresent()) {
                    request.setAttribute(IDENTIDADE, identidade.get());
                    return true;
                }
            }
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
