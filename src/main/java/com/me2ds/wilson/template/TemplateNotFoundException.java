package com.me2ds.wilson.template;

/**
 * Created by w3kim on 15-06-28.
 */
public class TemplateNotFoundException extends TemplateException {
    public TemplateNotFoundException(String templatePath) {
        super("Template not found: " + templatePath);
    }
}
