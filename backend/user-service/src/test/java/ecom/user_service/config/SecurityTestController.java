package ecom.user_service.config;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityTestController {

    @RequestMapping(
            path = {"/auth/register", "/auth/login", "/users/me", "/actuator/health", "/unknown"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH})
    void endpoint() {
    }
}
