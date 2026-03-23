package wordbook.backend.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailVerifyRequestDTO {
    public String email;
    public String code;
}
