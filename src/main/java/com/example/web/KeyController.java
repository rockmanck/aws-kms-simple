package com.example.web;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.CreateKeyResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class KeyController {

    private final AWSKMS kms;

    public KeyController(AWSKMS kms) {
        this.kms = kms;
    }

    @GetMapping("/create")
    public void create() {
        final CreateKeyResult key = kms.createKey();
        System.out.println(key.getKeyMetadata());
    }
}
