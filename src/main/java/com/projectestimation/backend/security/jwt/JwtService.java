package com.projectestimation.backend.security.jwt;

import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.constant.ProjectEstimationConstantDev;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final String jwtSecret;

	private final long jwtExpirationSeconds;

	public JwtService(
			@org.springframework.beans.factory.annotation.Value(ProjectEstimationConstantDev.APP_SECURITY_JWT_SECRET) String jwtSecret,
			@org.springframework.beans.factory.annotation.Value(ProjectEstimationConstantDev.APP_SECURITY_JWT_EXPIRATION_SECONDS) long jwtExpirationSeconds) {
		this.jwtSecret = jwtSecret;
		this.jwtExpirationSeconds = jwtExpirationSeconds;
	}

	public String generateToken(UserDetails userDetails) {
		Instant now = Instant.now();
		Instant expiration = now.plusSeconds(jwtExpirationSeconds);
		return Jwts.builder().subject(userDetails.getUsername()).issuedAt(Date.from(now))
				.expiration(Date.from(expiration)).signWith(getSignInKey()).compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
