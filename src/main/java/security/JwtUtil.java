package security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@AllArgsConstructor
public class JwtUtil { //READY
    //СОЗДАНИЕ ACCESS ТОКЕНА
    //Проверка Валидации Токена
    //Достать Ключ
    //Достать UserName и Срок Завершения Токена
    private final JwtProperties jwtProperties;

    //Метод дающий Ключ как SecretKey
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //3.Метод достающий из токена UserName
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    //3.Метод достающий из токена срок завершения
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //2.Вспомогательный метод
    public <R> R extractClaim(String token, Function<Claims, R> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    //1.Достать все клеймы и поместить их в Claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //Метод проверки срока токена
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    //ГЕНЕРАЦИЯ ТОКЕНА
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername());
    }
    //СОЗДАНИЕ ТОКЕНА
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    //Проверка валидация токена
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false; // Если токен изменен, подделан или истек
        }
    }
}