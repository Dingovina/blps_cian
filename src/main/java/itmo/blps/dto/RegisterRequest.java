package itmo.blps.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import itmo.blps.entity.UserRole;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$",
           message = "Invalid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
