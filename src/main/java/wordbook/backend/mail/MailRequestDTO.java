package wordbook.backend.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MailRequestDTO {
    @NotBlank(message="no email")
    @Email(message="invalid email")
    private String email;
}
