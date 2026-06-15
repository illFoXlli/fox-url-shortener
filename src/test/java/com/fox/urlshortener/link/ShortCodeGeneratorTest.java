package com.fox.urlshortener.link;

import static org.assertj.core.api.Assertions.assertThat;

import com.fox.urlshortener.TestFixtures;

import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    @Test
    void generatesAlphaNumericCodeWithConfiguredLength() {
        ShortCodeGenerator generator = new ShortCodeGenerator(TestFixtures.properties());

        for (int index = 0; index < 50; index++) {
            String code = generator.generate();
            assertThat(code).matches("[A-Za-z0-9]{6,8}");
        }
    }
}
