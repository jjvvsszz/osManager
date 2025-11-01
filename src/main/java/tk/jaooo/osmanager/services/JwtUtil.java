package tk.jaooo.osmanager.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private static final String SESSION_COOKIE_CLAIM = "session_cookie";
    private static final String ID_ESCOLA_CLAIM = "id_escola";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractSessionCookie(String token) {
        return extractClaim(token, claims -> claims.get(SESSION_COOKIE_CLAIM, String.class));
    }

    public String extractIdEscola(String token) {
        return extractClaim(token, claims -> claims.get(ID_ESCOLA_CLAIM, String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateTokenForDemandanetSession(String sessionCookie, String idEscola) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claim(SESSION_COOKIE_CLAIM, sessionCookie)
                .claim(ID_ESCOLA_CLAIM, idEscola)
                .subject(idEscola)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(this.key)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.warn("Validação do token JWT falhou: {}", e.getMessage());
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
