package com.me2ds.wilson.template;

/**
 * Created by w3kim on 15-06-28.
 */
public class InvalidTemplateException extends TemplateException {
    public InvalidTemplateException(String templateName) {
        super("'" + templateName + "' is in invalid JSON format");
    }
}
