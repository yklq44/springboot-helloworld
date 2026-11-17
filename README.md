# 后端笔试题 · Spring Boot 3.2.0 + Spring Security

> 岗位：后端（杭州五维数据有限责任公司）
> 题目原文：
> *Implement a project using SpringBoot 3.2.0. With a GET method HelloWorld API that requires SpringSecurity authentication, which returns a string saying "Hello World". And a username-password login API (could use username `test` and password `123456`). Submit a GitHub link.*

## 1. 交付内容对照

| 题目要求 | 实现位置 | 状态 |
|---|---|---|
| 使用 Spring Boot **3.2.0** | `pom.xml` → `spring-boot-starter-parent:3.2.0` | ✅ |
| **GET HelloWorld** 接口，返回 `Hello World` | `HelloController#hello` → `GET /api/hello` | ✅ |
| 该接口**必须经过 Spring Security 认证** | `SecurityConfig`：除登录/健康检查外 `anyRequest().authenticated()` | ✅ |
| **用户名密码登录接口**（test / 123456） | `AuthController#login` → `POST /api/auth/login` | ✅ |
| 可提交 GitHub 链接 | 本仓库 | ✅ |

## 2. 技术栈

| 项 | 版本 / 选型 | 说明 |
|---|---|---|
| JDK | 17 | Spring Boot 3.x 最低要求 |
| Spring Boot | 3.2.0（锁定） | starter-web / starter-security / starter-validation |
| Spring Security | 6.2.0（由 Boot 托管） | 无状态过滤链 |
| 令牌 | JWT HS256（jjwt 0.12.5） | 登录成功后签发，后续请求带 Bearer |
| 构建 | Maven 3.9.9 | `mvn spring-boot:run` / `mvn test` |

## 3. 目录结构

```
springboot-helloworld/
├── pom.xml
├── Dockerfile
├── api.http                     # IDEA / VS Code REST Client 可直接点开调试
└── src
    ├── main
    │   ├── java/com/wuwei/helloworld
    │   │   ├── HelloWorldApplication.java
    │   │   ├── config/SecurityConfig.java            # 安全过滤链、内存用户、密码编码器
    │   │   ├── security/JwtService.java              # 签发 / 校验 JWT
    │   │   ├── security/JwtAuthenticationFilter.java # Bearer → SecurityContext
    │   │   ├── security/RestAuthenticationEntryPoint.java
    │   │   ├── security/RestAccessDeniedHandler.java
    │   │   └── web
    │   │       ├── AuthController.java               # POST /api/auth/login
    │   │       ├── HelloController.java              # GET /api/hello, /api/me, /api/health
    │   │       ├── GlobalExceptionHandler.java
    │   │       └── dto/{LoginRequest,LoginResponse}.java
    │   └── resources/application.yml
    └── test/java/com/wuwei/helloworld/HelloWorldApiTest.java
```

## 4. 快速开始

```bash
# 只需要 JDK 17+；仓库自带 Maven Wrapper，无需预装 Maven
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
# 默认监听 http://localhost:8080
```

若已装 Maven，`mvn spring-boot:run` 等价。

打包运行：

```bash
mvn clean package
java -jar target/springboot-helloworld-1.0.0.jar
```

## 5. 接口说明

### 5.1 登录（公开）

```
POST /api/auth/login
Content-Type: application/json

{"username": "test", "password": "123456"}
```

成功 `200`：

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "test",
  "roles": ["ROLE_USER"]
}
```

失败 `401`：`{"status":401,"error":"Unauthorized","message":"Invalid username or password", ...}`

### 5.2 HelloWorld（受保护）

```
GET /api/hello
Authorization: Bearer <accessToken>
```

成功 `200`，响应体就是纯文本：`Hello World`

未携带 / 令牌无效或过期 → `401` + JSON 错误体。

### 5.3 辅助接口

| 接口 | 鉴权 | 用途 |
|---|---|---|
| `GET /api/me` | 需要 | 返回当前用户名与权限，便于验证令牌 |
| `GET /api/health` | 公开 | 存活探针 |

## 6. 命令行验证（复制即用）

bash（Linux / macOS / Git Bash）：

```bash
# 1) 未认证调用，应返回 401
curl -i http://localhost:8080/api/hello

# 2) 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# 3) 带 token 调用，应返回 Hello World
curl -i http://localhost:8080/api/hello -H "Authorization: Bearer $TOKEN"
```

Windows PowerShell 版：

```powershell
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType 'application/json' -Body '{"username":"test","password":"123456"}').accessToken

Invoke-RestMethod -Uri http://localhost:8080/api/hello -Headers @{ Authorization = "Bearer $token" }
```

同时保留 **HTTP Basic** 作为第二种标准认证方式（`Spring Security` 默认能力）：

```bash
curl -u test:123456 http://localhost:8080/api/hello
```

## 7. 设计说明（为什么这样做）

1. **无状态 + JWT**：题目要求"登录接口 + 受保护接口"，前后端分离场景下用 JWT 比 Session 更贴合；服务端不保存会话，`SessionCreationPolicy.STATELESS`，多实例水平扩展无需共享会话。
2. **认证与授权分离**：`AuthenticationManager` 负责校验用户名密码（`DaoAuthenticationProvider` + `BCryptPasswordEncoder`），`JwtAuthenticationFilter` 只负责把 Bearer 令牌还原成 `Authentication`，二者都在 Spring Security 过滤链内，没有自造鉴权。
3. **默认拒绝**：`anyRequest().authenticated()`，白名单只有登录与健康检查，避免新增接口时忘记加保护。
4. **密码不落明文**：演示账号 `test/123456` 在内存中也是 BCrypt 哈希；生产应替换为数据库 + 更高的 BCrypt 强度。
5. **错误响应统一为 JSON**：默认的 401/403 是 HTML 或空响应，对前端不友好，因此自定义了 `AuthenticationEntryPoint` 与 `AccessDeniedHandler`。
6. **密钥外置**：`app.jwt.secret` 支持环境变量 `APP_JWT_SECRET` 覆盖，仓库里只放演示值。

## 8. 测试

```bash
mvn test
```

`HelloWorldApiTest` 覆盖 7 个场景：未带凭证 401、带 JWT 返回 Hello World、登录发令牌、错误密码 401、被篡改令牌 401、HTTP Basic 通过、健康检查公开。

## 9. Docker

```bash
docker build -t springboot-helloworld .
docker run --rm -p 8080:8080 springboot-helloworld
```

## 10. 可继续演进（未在本题范围内）

- 令牌刷新（refresh token）与登出黑名单
- 用户表 + Flyway 迁移，替代内存用户
- 登录限流 / 失败次数锁定，防爆破
- OpenAPI（springdoc）自动生成接口文档
