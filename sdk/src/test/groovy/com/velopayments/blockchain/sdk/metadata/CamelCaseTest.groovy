package com.velopayments.blockchain.sdk.metadata

import org.junit.Test

import static CamelCase.upperUnderscoreToLowerCamelCase
import static CamelCase.upperUnderscoreToUpperCamelCase

class CamelCaseTest {

    @Test
    void 'test case transformation'() {
        assert upperUnderscoreToLowerCamelCase("A") == 'a'
        assert upperUnderscoreToLowerCamelCase("BOB") == 'bob'
        assert upperUnderscoreToLowerCamelCase("BOB_SMITH") == 'bobSmith'
        assert upperUnderscoreToUpperCamelCase("BOB_SMITH") == 'BobSmith'
        assert upperUnderscoreToLowerCamelCase("BOB_SMITH_1") == 'bobSmith1'
        assert upperUnderscoreToLowerCamelCase("") == ''
        assert upperUnderscoreToLowerCamelCase(null) == null
        assert upperUnderscoreToLowerCamelCase("_VOID") == 'void'
        assert upperUnderscoreToLowerCamelCase("_CREATED") == 'created'
    }
}
