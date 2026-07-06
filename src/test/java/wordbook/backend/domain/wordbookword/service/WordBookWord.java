package wordbook.backend.domain.wordbookword.service;

import wordbook.backend.domain.user.entity.UserEntity;
import wordbook.backend.domain.word.entity.WordEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class WordBookWord {
    List<WordEntity> testWords = new ArrayList<>();
public void init() {
    UserEntity testUser=UserEntity.builder().
            id(1l).build();
    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Algorithm").meaning("알고리즘, 문제를 해결하기 위한 절차").lang("en")
            .example("This algorithm solves the puzzle in seconds.").topic("IT").translation("이 알고리즘은 몇 초 만에 퍼즐을 풀어낸다.")
            .useword("algorithm").exampleStartIndex(5).exampleLastIndex(14).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Database").meaning("데이터베이스, 데이터의 집합").lang("en")
            .example("We store all user records in a secure database.").topic("IT").translation("우리는 모든 사용자 기록을 안전한 데이터베이스에 저장한다.")
            .useword("database").exampleStartIndex(41).exampleLastIndex(49).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Authentication").meaning("인증, 신원 확인").lang("en")
            .example("The system requires biometrics for authentication.").topic("Security").translation("시스템은 인증을 위해 생체 인식을 요구한다.")
            .useword("authentication").exampleStartIndex(35).exampleLastIndex(49).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Repository").meaning("저장소, 레포지토리").lang("en")
            .example("Please push your commits to the remote repository.").topic("Development").translation("커밋을 원격 저장소에 푸시해 주세요.")
            .useword("repository").exampleStartIndex(41).exampleLastIndex(51).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Redundant").meaning("불필요한, 중복의").lang("en")
            .example("Remove redundant code to optimize performance.").topic("Refactoring").translation("성능을 최적화하기 위해 중복된 코드를 제거하라.")
            .useword("redundant").exampleStartIndex(7).exampleLastIndex(16).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Meticulous").meaning("꼼꼼한, 세심한").lang("en")
            .example("She is meticulous when writing unit tests.").topic("TOEIC").translation("그녀는 단위 테스트를 작성할 때 매우 꼼꼼하다.")
            .useword("meticulous").exampleStartIndex(7).exampleLastIndex(17).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Postpone").meaning("연기하다, 미루다").lang("en")
            .example("We had to postpone the sprint planning meeting.").topic("Business").translation("우리는 스프린트 계획 회의를 연기해야 했다.")
            .useword("postpone").exampleStartIndex(10).exampleLastIndex(18).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Collaborate").meaning("협력하다, 공동으로 작업하다").lang("en")
            .example("Developers and designers collaborate on this project.").topic("Business").translation("개발자들과 디자이너들은 이 프로젝트에서 협력한다.")
            .useword("collaborate").exampleStartIndex(25).exampleLastIndex(36).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Ambiguous").meaning("애매모호한, 여러 가지로 해석되는").lang("en")
            .example("The requirements doc is too ambiguous for the team.").topic("Development").translation("요구사항 문서가 팀에게 너무 모호하다.")
            .useword("ambiguous").exampleStartIndex(25).exampleLastIndex(34).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Simultaneous").meaning("동시의, 일제히 일어나는").lang("en")
            .example("The server handles thousands of simultaneous connections.").topic("Network").translation("서버는 수천 개의 동시 연결을 처리한다.")
            .useword("simultaneous").exampleStartIndex(30).exampleLastIndex(43).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Comprehensive").meaning("종합적인, 포괄적인").lang("en")
            .example("This book provides a comprehensive guide to JPA.").topic("TOEIC").translation("이 책은 JPA에 대한 종합적인 가이드를 제공한다.")
            .useword("comprehensive").exampleStartIndex(20).exampleLastIndex(33).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Feasible").meaning("실행 가능한, 실현 가능한").lang("en")
            .example("Building this feature in two days is not feasible.").topic("Business").translation("이 기능을 이틀 만에 만드는 것은 불가능하다.")
            .useword("feasible").exampleStartIndex(41).exampleLastIndex(49).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Persistent").meaning("지속적인, 영속성의").lang("en")
            .example("JPA helps manage persistent data easily.").topic("Database").translation("JPA는 영속성 데이터를 쉽게 관리하도록 도와준다.")
            .useword("persistent").exampleStartIndex(18).exampleLastIndex(28).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Encapsulation").meaning("캡슐화, 정보 은닉").lang("en")
            .example("Encapsulation hides the internal state of an object.").topic("OOP").translation("캡슐화는 객체의 내부 상태를 숨긴다.")
            .useword("Encapsulation").exampleStartIndex(0).exampleLastIndex(13).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("Innovative").meaning("혁신적인").lang("en")
            .example("The startup introduced an innovative solution for cache.").topic("Business").translation("그 스타트업은 캐시를 위한 혁신적인 솔루션을 선보였다.")
            .useword("innovative").exampleStartIndex(27).exampleLastIndex(37).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("り해 (이해)").meaning("이해, 사리를 분별하여 해석함").lang("ja")
            .example("日本語の文法を理解することは難しい。").topic("Daily").translation("일본어 문법을 이해하는 것은 어렵다.")
            .useword("理解").exampleStartIndex(8).exampleLastIndex(10).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("かいはつ (개발)").meaning("개발, 새로운 것을 만들어냄").lang("ja")
            .example("新しいシステムの開発が始まりました。").topic("IT").translation("새로운 시스템의 개발이 시작되었습니다.")
            .useword("開発").exampleStartIndex(8).exampleLastIndex(10).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("かんり (관리)").meaning("관리, 시설이나 일을 맡아 다스림").lang("ja")
            .example("データベースの管理は非常に重要です。").topic("Database").translation("데이터베이스 관리는 매우 중요합니다.")
            .useword("管理").exampleStartIndex(8).exampleLastIndex(10).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("정규표현식").meaning("특정 규칙을 가진 문자열 집합을 표현하는 식").lang("ko")
            .example("이메일 형식을 검증할 때는 정규표현식을 사용하면 편리합니다.").topic("IT").translation("It is convenient to use regular expressions when validating email formats.")
            .useword("정규표현식").exampleStartIndex(14).exampleLastIndex(20).build());

    testWords.add(WordEntity.builder()
            .userEntity(testUser).word("비동기").meaning("요청과 결과가 동시에 일어나지 않는 방식").lang("ko")
            .example("화면을 새로고침하지 않고 데이터를 받아오기 위해 비동기 통신을 썼다.").topic("Web").translation("Asynchronous communication was used to fetch data without refreshing the page.")
            .useword("비동기").exampleStartIndex(26).exampleLastIndex(29).build());
}
}
