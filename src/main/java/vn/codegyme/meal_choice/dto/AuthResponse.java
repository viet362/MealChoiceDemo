package vn.codegyme.meal_choice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private UUID id;

    private String email;

    private String displayName;

    private String phoneNumber;

    private String avatarUrl;

    private Set<String> roles;
}
