package com.example.web;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.KeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
public class KeyController {

    private final AWSKMS kms;
    private final AwsCrypto crypto = AwsCrypto.standard();
    private final KmsMasterKeyProvider prov;
    private final Map<String, String> context = Map.of(
            "Purpose", "Test",
            "Service", "demo-service"
    );
    private final String keyArn;

    public KeyController(AWSKMS kms, KmsMasterKeyProvider.Builder builder) {
        this.kms = kms;
        CreateKeyRequest createAsymmetricRequest = new CreateKeyRequest();
        createAsymmetricRequest.setKeySpec(KeySpec.SYMMETRIC_DEFAULT);
        final CreateKeyResult keyResult = kms.createKey(createAsymmetricRequest);
        keyArn = keyResult.getKeyMetadata().getArn();
        prov = builder
                .buildStrict(keyArn);
    }

    @GetMapping("/encrypt")
    @ResponseStatus(HttpStatus.OK)
    public void create(@RequestParam String value) {
        // Encrypt the data
        //
        final CryptoResult<byte[], KmsMasterKey> encryptResult = crypto.encryptData(
                prov,
                value.getBytes(StandardCharsets.UTF_8),
                context
        );
        final byte[] ciphertext = encryptResult.getResult();
        System.out.println("Ciphertext: " + new String(ciphertext));

        // Decrypt the data
        final CryptoResult<byte[], KmsMasterKey> decryptResult = crypto.decryptData(prov, ciphertext);
        // Your application should verify the encryption context and the KMS key to
        // ensure this is the expected ciphertext before returning the plaintext
        if (!decryptResult.getMasterKeyIds().get(0).equals(keyArn)) {
            throw new IllegalStateException("Wrong key id!");
        }

        // The AWS Encryption SDK may add information to the encryption context, so check to
        // ensure all of the values that you specified when encrypting are *included* in the returned encryption context.
        if (!context.entrySet().stream()
                .allMatch(e -> e.getValue().equals(decryptResult.getEncryptionContext().get(e.getKey())))) {
            throw new IllegalStateException("Wrong Encryption Context!");
        }

//        assert Arrays.equals(decryptResult.getResult(), value.getBytes(StandardCharsets.UTF_8));

        // The data is correct, so return it.
        System.out.println("Decrypted: " + new String(decryptResult.getResult(), StandardCharsets.UTF_8));
    }
}
