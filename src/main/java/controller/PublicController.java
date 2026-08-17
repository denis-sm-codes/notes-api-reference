package controller;

import dto.request.AuthRequest;
import dto.request.RefreshTokenRequest;
import dto.request.RegisterRequest;
import dto.response.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import security.AuthService;

@RestController
@RequestMapping("public/")
@RequiredArgsConstructor
public class PublicController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody AuthRequest request){
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.updateTokens(request.refreshToken());
    }

}
