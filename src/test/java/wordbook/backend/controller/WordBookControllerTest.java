package wordbook.backend.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(WordbookLikeController.class)
@AutoConfigureMockMvc(addFilters = true)
class WordBookControllerTest {


}