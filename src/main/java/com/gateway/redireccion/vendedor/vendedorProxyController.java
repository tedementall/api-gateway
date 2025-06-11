package com.gateway.redireccion.vendedor;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.gateway.jwt.service.JwtService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;


@RestController
@RequestMapping("/api/proxy/vendedor")
@RequiredArgsConstructor

public class vendedorProxyController {

    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    @RequestMapping(value = "/**",method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyVendedor(HttpServletRequest request, 
                                                @RequestBody(required = false) String body,
                                                @RequestHeader HttpHeaders headers) {
        String originalPath = request.getRequestURI().replace("/api/proxy/vendedor", "");
        String targetUrl = "http://localhost:8086/api/vendedor" + originalPath;
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        if (method == HttpMethod.DELETE) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if(authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Token no presente o invalido\"}");
            }

            String token = authHeader.replace("Bearer ", "");
            String rol = jwtService.extractClaim(token, claims -> claims.get("rol", String.class));
            
            if (!"admin".equals(rol)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"No tienes permiso para realizar esta accion\"}");
            }
            if (!"vendedor".equals(rol)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"No tienes permiso para realizar esta accion\"}");
            }
        }

        HttpHeaders cleanHeaders = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (!key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                cleanHeaders.put(key, value);
            }
        });
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, cleanHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, method, entity, String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Error inesperado en el API Gateway\", \"detalle\": \"" + ex.getMessage() + "\"}");
        }


    }
}
    
