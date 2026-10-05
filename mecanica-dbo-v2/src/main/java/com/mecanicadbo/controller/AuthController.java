package com.mecanicadbo.controller;

import com.mecanicadbo.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MS = 5 * 60 * 1000L;

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();

    /** Falhas recentes por IP: [contagem, início da janela]. Em memória — suficiente para uma única instância. */
    private final Map<String, long[]> falhas = new ConcurrentHashMap<>();

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest body,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        if (bloqueado(ip)) {
            return erro(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde alguns minutos e tente novamente.");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username().trim(), body.password()));
        } catch (BadCredentialsException e) {
            registrarFalha(ip);
            return erro(HttpStatus.UNAUTHORIZED, "Usuário ou senha incorretos.");
        } catch (AuthenticationException e) {
            registrarFalha(ip);
            log.warn("Falha de autenticação: {}", e.getClass().getSimpleName());
            return erro(HttpStatus.UNAUTHORIZED, "Não foi possível autenticar.");
        }

        falhas.remove(ip);

        // Evita session fixation: se já existe sessão (anônima), troca o id.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(auth);
        holder.setContext(context);
        securityContextRepository.saveContext(context, request, response); // grava na sessão HTTP

        return ResponseEntity.ok(estadoLogado(auth));
    }

    /** Público: informa se há sessão ativa. O frontend usa no guard de rotas. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("autenticado", false);
            return m;
        }
        return estadoLogado(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        holder.clearContext();
        return ResponseEntity.noContent().build();
    }

    // ---------- helpers ----------

    private Map<String, Object> estadoLogado(Authentication auth) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("autenticado", true);
        m.put("usuario", auth.getName());
        return m;
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", LocalDateTime.now().toString());
        m.put("status", status.value());
        m.put("mensagem", mensagem);
        return ResponseEntity.status(status).body(m);
    }

    private boolean bloqueado(String ip) {
        long[] f = falhas.get(ip);
        if (f == null) return false;
        if (System.currentTimeMillis() - f[1] > JANELA_MS) {
            falhas.remove(ip);
            return false;
        }
        return f[0] >= MAX_TENTATIVAS;
    }

    private void registrarFalha(String ip) {
        long agora = System.currentTimeMillis();
        falhas.merge(ip, new long[]{1, agora}, (atual, novo) ->
                agora - atual[1] > JANELA_MS ? new long[]{1, agora} : new long[]{atual[0] + 1, atual[1]});
    }
}
