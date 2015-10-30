package net.isucon;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@Controller
@Slf4j
public class Isucon5fApplication {

    public static void main(String[] args) {
        SpringApplication.run(Isucon5fApplication.class, args);
    }

    @Bean
    ServletContextInitializer servletContextInitializer() {
        // keep session only in cookie so that jsessionid is not appended to URL.
        return servletContext -> servletContext.setSessionTrackingModes(
                Collections.singleton(SessionTrackingMode.COOKIE));
    }

    @Autowired
    HttpSession session;

    @Autowired
    HttpServletRequest request;

    @Autowired
    JdbcTemplate db;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${initialize-script-path:classpath:/initialize.sql}")
    Resource initializeScript;

    @Autowired
    RestTemplate restTemplate;

    @RequestMapping(method = RequestMethod.GET, path = "/signup")
    String signup() {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {

        }
        return "signup";
    }

    @RequestMapping(method = RequestMethod.POST, path = "/signup")
    String signup(@RequestParam String email, @RequestParam String password,
            @RequestParam User.Grade grade) {
        String salt = RandomStringUtils.randomAlphanumeric(32);
        tx.execute(status -> {
            Long userId = db.queryForObject(
                    "INSERT INTO users (email,salt,passhash,grade) VALUES (?,?,digest(? || ?, 'sha512'),?::grades) RETURNING id",
                    Long.class, email, salt, salt, password, grade.name());
            db.update("INSERT INTO subscriptions (user_id,arg) VALUES (?,?)",
                    userId, "{}");
            return null;
        });

        return "redirect:/login";
    }

    @RequestMapping(method = RequestMethod.POST, path = "/cancel")
    String cancel() {
        return "redirect:/signup";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/login")
    String login() {
        return "login";
    }

    @RequestMapping(method = RequestMethod.POST, path = "/login")
    String login(@RequestParam String email, @RequestParam String password) {
        authenticate(email, password).orElseThrow(AccessDeniedException::new);
        request.changeSessionId();
        return "redirect:/";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/logout")
    String logout() {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {

        }
        return "redirect:/login";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/")
    String index(Model model) {
        User user = currentUser().orElseThrow(AccessDeniedException::new);
        model.addAttribute("user", user);
        return "main";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/user.js")
    String userjs(Model model) {
        User.Grade grade = currentUser().orElseThrow(AccessDeniedException::new)
                .getGrade();
        int interval = 0;
        switch (grade) {
        case micro:
            interval = 30000;
            break;
        case small:
            interval = 30000;
            break;
        case standard:
            interval = 20000;
            break;
        case premium:
            interval = 10000;
            break;
        }
        model.addAttribute("interval", interval);
        return "userjs";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/modify")
    String modify(Model model) {
        User user = currentUser().orElseThrow(AccessDeniedException::new);
        db.queryForList("SELECT arg FROM subscriptions WHERE user_id=?",
                String.class, user.getId()).stream().findAny().ifPresent(
                        args -> model.addAttribute("args", args));
        model.addAttribute("user", user);
        return "modify";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, path = "/modify")
    String modify(@RequestParam String service,
            @RequestParam Optional<String> token,
            @RequestParam Optional<String> keys,
            @RequestParam(name = "param_name") Optional<String> paramName,
            @RequestParam(name = "param_value") Optional<String> paramValue) {
        User user = currentUser().orElseThrow(AccessDeniedException::new);

        tx.execute(status -> {
            db.queryForList(
                    "SELECT arg FROM subscriptions WHERE user_id=? FOR UPDATE",
                    String.class, user.getId()).stream().findAny().ifPresent(
                            json -> {
                Map<String, Object> args = parseJson(json);
                args.computeIfAbsent(service, k -> new HashMap<>());
                Map<String, Object> s = (Map<String, Object>) args.get(service);
                token.ifPresent(x -> s.put("token", x));
                keys.ifPresent(x -> s.put("keys", x));
                paramName.ifPresent(name -> paramValue.ifPresent(value -> {
                    s.computeIfAbsent("params", k -> new HashMap<>());
                    Map<String, Object> p = (Map<String, Object>) s.get(
                            "params");
                    p.put(name, value);
                }));
                db.update("UPDATE subscriptions SET arg=? WHERE user_id=?",
                        toJson(args), user.getId());
            });
            return null;
        });
        return "redirect:/modify";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.GET, path = "/data")
    @ResponseBody
    List<Map<String, Object>> data() {
        User user = currentUser().orElseThrow(AccessDeniedException::new);
        String json = db.queryForObject(
                "SELECT arg FROM subscriptions WHERE user_id=?", String.class,
                user.getId());
        Map<String, Object> arg = parseJson(json);
        return arg.entrySet().stream().map(e -> {
            String service = e.getKey();
            Endpoint row = db.queryForObject(
                    "SELECT meth, token_type, token_key, uri FROM endpoints WHERE service=?",
                    (rs, i) -> {
                Optional<String> tokenType = Optional.ofNullable(rs.getString(
                        "token_type"));
                return new Endpoint(rs.getString("meth"), tokenType.map(
                        Endpoint.TokenType::valueOf).orElse(null), rs.getString(
                                "token_key"), rs.getString("uri"));
            } , service);
            Map<String, Object> conf = (Map<String, Object>) e.getValue();
            Map<String, Object> params = (Map<String, Object>) conf
                    .getOrDefault("params", new HashMap<>());
            Map<String, Object> headers = new HashMap<>();
            if (row.getTokenType() != null) {
                switch (row.getTokenType()) {
                case header:
                    headers.put(row.getTokenKey(), conf.get("token"));
                    break;
                case param:
                    params.put(row.getTokenKey(), conf.get("token"));
                    break;
                }
            }
            try {
                List keys = null;
                Object v = conf.getOrDefault("keys", Collections.emptyList());
                if (v instanceof List) {
                    keys = (List) v;
                } else {
                    keys = Arrays.asList(v);
                }
                String uri = String.format(row.getUri(), List.class.cast(keys)
                        .toArray());

                Map<String, Object> data = new HashMap<>();
                data.put("service", service);
                data.put("data", fetchApi(row.getMeth(), uri, headers, params));
                return data;
            } catch (Exception ex) {
                log.warn("!! " + conf, ex);
                return Collections.<String, Object> emptyMap();
            }
        }).collect(Collectors.toList());
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/initialize")
    String initialize() throws SQLException {
        try (Connection con = DataSourceUtils.getConnection(db
                .getDataSource())) {
            ScriptUtils.executeSqlScript(con,
                    new EncodedResource(initializeScript, StandardCharsets.UTF_8));
        }
        return "OK";
    }

    @ExceptionHandler(AccessDeniedException.class)
    String error() {
        return "redirect:/login";
    }

    MultiValueMap<String, String> toMultiValueMap(Map<String, Object> map) {
        MultiValueMap<String, String> p = new LinkedMultiValueMap<>();
        map.forEach((k, v) -> p.add(k, v.toString()));
        return p;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> fetchApi(String method, String uri,
            Map<String, Object> headers, Map<String, Object> params) {
        try {
            switch (method) {
            case "GET": {
                return restTemplate.exchange(UriComponentsBuilder.fromUriString(
                        uri).queryParams(toMultiValueMap(params)).build()
                        .toUri(), HttpMethod.GET,
                        new HttpEntity(toMultiValueMap(headers)),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }).getBody();
            }
            case "POST": {
                return restTemplate.exchange(URI.create(uri), HttpMethod.POST,
                        new HttpEntity(toMultiValueMap(params), toMultiValueMap(
                                headers)),
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }).getBody();
            }
            default:
                throw new IllegalStateException("unknown method " + method);
            }
        } catch (RestClientException e) {
            log.warn("call failure uri=" + uri + ",params=" + params, e);
            return Collections.emptyMap();
        }
    }

    Optional<User> authenticate(String email, String password) {
        return db.query(
                "SELECT id, email, grade FROM users WHERE email=? AND passhash=digest(salt || ?, 'sha512')",
                (rs, i) -> {
                    return new User(rs.getLong("id"), rs.getString(
                            "email"), User.Grade.valueOf(rs.getString(
                                    "grade")));
                } , email, password).stream().findAny().map(u -> {
                    session.setAttribute("user", u);
                    return u;
                });
    }

    Optional<User> currentUser() {
        return Optional.ofNullable((User) session.getAttribute("user"));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}

@Data
@AllArgsConstructor
class User implements Serializable {
    private long id;

    private String email;

    private Grade grade;

    public enum Grade {
        micro, small, standard, premium
    }
}

@Data
@AllArgsConstructor
class Endpoint implements Serializable {
    private String meth;

    private TokenType tokenType;

    private String tokenKey;

    private String uri;

    public enum TokenType {
        header, param
    }
}

@ResponseStatus(HttpStatus.FORBIDDEN)
class AccessDeniedException extends RuntimeException {

}
