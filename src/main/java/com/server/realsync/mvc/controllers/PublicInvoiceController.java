package com.server.realsync.mvc.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Serves the public invoice view HTML page.
 * URL pattern: /{businessSlug}/invoice/{token}
 *
 * The slug is for readability only; the token is used for the actual data fetch.
 * This endpoint is accessible without authentication (see SecurityConfig).
 *
 * URL is mapped via Spring's wildcard pattern but routed here.
 * We use /i/{token} as the canonical public path for simplicity;
 * the {slug}/invoice/{token} variant is handled by the catch-all below.
 */
@Controller
public class PublicInvoiceController {

    /**
     * Short canonical URL: /i/{token}
     */
    @GetMapping("/i/{token}")
    public String publicInvoiceShort(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "remindmeui/public-invoice";
    }

    /**
     * Slug-based URL: /{slug}/invoice/{token}
     * Note: AntPathMatcher handles this with a single wildcard path variable.
     */
    @GetMapping("/{slug}/invoice/{token}")
    public String publicInvoiceWithSlug(
            @PathVariable String slug,
            @PathVariable String token,
            Model model) {
        model.addAttribute("token", token);
        model.addAttribute("slug", slug);
        return "remindmeui/public-invoice";
    }
}
