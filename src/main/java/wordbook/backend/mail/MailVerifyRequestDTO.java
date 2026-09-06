package wordbook.backend.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailVerifyRequestDTO {

    @NotBlank(message="no email")
    @Email(message="invalid email")
    public String email;

    @NotBlank(message="no code")
    @Size(min=4,max=4)
    public String code;
}
