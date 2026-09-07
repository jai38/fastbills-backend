package com.fastbills.api.invoice;

import java.security.SecureRandom;

/**
 * Generates human-readable, unambiguous collection references as specified in DOMAIN.md Section 7.
 * Alphabet excludes ambiguous characters: 'O', '0', 'I', '1', 'L'.
 * Shape: {sequence}-{4 random chars}, e.g. 42-7K2M, 1-9PXT.
 */
public final class CollectionReferenceGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private CollectionReferenceGenerator() {}

    public static String generate(int sequenceNumber) {
        char[] randomPart = new char[4];
        for (int i = 0; i < 4; i++) {
            randomPart[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return sequenceNumber + "-" + new String(randomPart);
    }
}
