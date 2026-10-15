# Swordmaster-Server

## 프로젝트 소개
2026 내일배움캠프 단기심화 최종프로젝트 서버

9조(강화이구조) 팀프로젝트 서버

[클라이언트 주소]
https://github.com/upgrade-29/swordmaster-client

## 개발기간
26.09.23 ~ 26.10.21 (기획기간 포함)

## 개발환경 및 기술 스택

- Intellij

    Spring Boot(4.1.1) (Java, JPA, MySQL, RDS)
  
    JPK : 21.amazon

- Docker

- Postman

- AWS

    123123

    123123
    

## 개발자 소개
- 김관영( https://github.com/UR-KGY ) : 상점 및 CICD
- 오수호( https://github.com/OHSUHO ) : 전투
- 이훈( https://github.com/Lee-Hun97 ) : 로그인 및 채팅
- 허준혁 ( https://github.com/slshan0597 ) : 아이템 및 아티팩트

## 주요 기능
<details>
  <summary>유지보수 및 배포</summary>
  <div markdown="1">

여기에 숨겨둘 텍스트, 이미지, 코드 등을 작성합니다.(첫줄을 남겨놓아야 줄바꿈이 가능함 <- 이유는 모름)
  </div>
</details>

<details>
  <summary>인증과 인가</summary>
  <div markdown="1">
    
...  
회원가입과 로그인, WebSocket의 통신을 제외한 모든 요청에는 JWT인증이 필요하다.

로그인 성공 시 서버는 클라이언트에게 **JWT**(AccessToken + RefreshToken)를 반환해주고, (RefreshToken 미구현)

클라이언트가 서버의 api를 호출하는 경우 **filter기능**을 통해서 JWT를 통한 인증을 먼저 거치게된다.

AccessToken 만료 직전 클라이언트는 서버에 재연결을 요청하여 새로운 JWT를 전달받아야한다. (재연결 미구현)

모바일 환경이기에 만료 직전 클라이언트가 가동을 안하는 경우를 대비해, 재연결 시 만료 직전 시간을 비교하여 재연결을 요청해야한다.

**WebSocket** 통신의 경우 처음 **STOMP connect**에만 JWT가 필요하고, AccessToken 만료 직전 재발급 시 자동으로 재연결을 요청해야한다.
  </div>
</details>

<details>
  <summary>강화</summary>
  <div markdown="1">

  여기에 숨겨둘 텍스트, 이미지, 코드 등을 작성합니다.(첫줄을 남겨놓아야 줄바꿈이 가능함 <- 이유는 모름)

  </div>
</details>

<details>
  <summary>전투 및 보상</summary>
  <div markdown="1">

  여기에 숨겨둘 텍스트, 이미지, 코드 등을 작성합니다.(첫줄을 남겨놓아야 줄바꿈이 가능함 <- 이유는 모름)

  </div>
</details>
