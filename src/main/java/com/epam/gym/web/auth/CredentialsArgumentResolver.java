package com.epam.gym.web.auth;

import com.epam.gym.dto.Credentials;
import com.epam.gym.exception.AuthenticationFailedException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CredentialsArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BASIC_PREFIX = "Basic ";
    private static final String INVALID_HEADER = "Missing or invalid Authorization header";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Credentials.class.equals(parameter.getParameterType());
    }

    @Override
    public Credentials resolveArgument(MethodParameter parameter,
                                       ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest,
                                       WebDataBinderFactory binderFactory) {
        return parse(webRequest.getHeader(HttpHeaders.AUTHORIZATION));
    }

    Credentials parse(String header) {
        if (header == null || !header.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            throw new AuthenticationFailedException(INVALID_HEADER);
        }
        String decoded;
        try {
            byte[] bytes = Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length()).trim());
            decoded = new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationFailedException(INVALID_HEADER);
        }
        int separator = decoded.indexOf(':');
        if (separator <= 0) {
            throw new AuthenticationFailedException(INVALID_HEADER);
        }
        return new Credentials(decoded.substring(0, separator), decoded.substring(separator + 1));
    }
}
