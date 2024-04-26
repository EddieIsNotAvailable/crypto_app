package org.example.crypto_app.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.crypto_app.model.BaseUser;
import org.example.crypto_app.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    public final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/login")
    public String loginForm() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof UserDetails) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(HttpServletRequest request, Model model) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            request.login(username, password);
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }


    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(HttpServletRequest request, Model model) {
        String name = request.getParameter("name");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        BaseUser user = new BaseUser(name,username,password);

        try {
            userService.createUser(user);
            model.addAttribute("message", "Signup successful");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/account")
    public String account(Model model) {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        model.addAttribute("name", user.getName());
        model.addAttribute("username", user.getUsername());
        return "account";
    }

    @PostMapping("/account/changePassword")
    public String changePassword(HttpServletRequest request, Model model) {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String currPass = request.getParameter("currentPassword");
        String newPass = request.getParameter("newPassword");

        try {
            userService.changePassword(user, currPass, newPass);
            return "redirect:/logout";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "account";
        }

    }

    @PostMapping("/account/delete")
    public String deleteAccount(HttpServletRequest request, Model model) {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String passwordConfirmation = request.getParameter("passwordConfirmation");

        try {
            userService.deleteUser(user, passwordConfirmation);
            model.addAttribute("message", "Account deleted successfully");
            return "redirect:/logout";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "account";
        }
    }
}