package com.alpha.marketplace.controllers;

import com.alpha.marketplace.models.Extension;
import com.alpha.marketplace.models.binding.UserBindingModel;
import com.alpha.marketplace.services.base.ExtensionService;
import com.alpha.marketplace.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.github.api.GitHub;
import org.springframework.social.github.api.GitHubCommit;
import org.springframework.social.github.api.GitHubRepo;
import org.springframework.social.github.api.GitHubUserProfile;
import org.springframework.social.github.api.impl.GitHubTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private ExtensionService extensionService;

    @Autowired
    public HomeController(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Extension> newestExtensions = extensionService.getLatest();
        List<Extension> selectedByAdmin = extensionService.getAdminSelection();
        List<Extension> mostPopular = extensionService.getMostPopular();
        test();

        model.addAttribute("view", "index");
        model.addAttribute("newest", newestExtensions);
        model.addAttribute("adminSelection", selectedByAdmin);
        model.addAttribute("mostPopular", mostPopular);

        return "base-layout";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!Utils.isUserNotAnonymous()) {
            return "redirect:/";
        }
        model.addAttribute("view", "register");
        model.addAttribute("user", new UserBindingModel());

        return "base-layout";
    }

    @GetMapping(value = "/login")
    public String login(Model model) {
        if (!Utils.isUserNotAnonymous()) {
            return "redirect:/";
        }
        model.addAttribute("view", "login");

        return "base-layout";
    }

    @GetMapping("/unauthorized")
    public String unauthorized(Model model) {
        model.addAttribute("view", "unauthorized");

        return "base-layout";
    }

    public void test() {
        GitHub gitHub = new GitHubTemplate();

        GitHubRepo repo = gitHub.repoOperations().getRepo("Petroslav", "telerik-alpha-final-project");
        gitHub.repoOperations();
    }
}
