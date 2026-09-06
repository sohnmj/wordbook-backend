package wordbook.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
@AllArgsConstructor

public class UserCreateDTO {

    @NotBlank(message="no id")
    @Pattern(regexp = "^[a-zA-Z0-9]{5,10}$", message = "영문 및 숫자로 5~10자 이내여야 합니다.")
    private String username;

    @NotBlank(message="no ps")
    @Size(min=8,max=16,message="invalid ps")
    private String password;
    @NotBlank(message="no email")
    @Email()
    private String email;
}
