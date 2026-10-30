package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import xlike.top.kn_ai_chat.domain.EnterpriseProfile;
import xlike.top.kn_ai_chat.service.EnterpriseProfileService;

@Controller
@RequestMapping("/admin")
public class EnterpriseProfileController {

    private final EnterpriseProfileService service;

    public EnterpriseProfileController(EnterpriseProfileService service) {
        this.service = service;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    @GetMapping("/enterprise")
    public String page(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("profile", service.get());
        return "enterprise";
    }

    @GetMapping("/api/enterprise")
    @ResponseBody
    public ResponseEntity<EnterpriseProfile> get(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.get());
    }

    @PostMapping("/api/enterprise")
    @ResponseBody
    public ResponseEntity<EnterpriseProfile> save(@RequestBody EnterpriseProfile profile, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.save(profile));
    }
}
