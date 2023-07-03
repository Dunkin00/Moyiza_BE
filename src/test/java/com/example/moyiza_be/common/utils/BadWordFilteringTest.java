package com.example.moyiza_be.common.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BadWordFilteringTest {

    @InjectMocks
    BadWordFiltering badWordFiltering;

    @BeforeEach
    public void setup() {
        badWordFiltering.compileBadWordPatterns();
    }

    @Nested
    @DisplayName("checkBadWord() Test")
    class CheckBadWord {
        @Test
        @DisplayName("Success Case_badWord")
        void testCheckBadWord_badWord() {
            //given
            String input = "나쁜말";
            //when then
            assertTrue(badWordFiltering.checkBadWord(input));
        }
        @Test
        @DisplayName("Success Case_kindWord")
        void testCheckBadWord_kindWord() {
            //given
            String input = "착한말";
            //when then
            assertFalse(badWordFiltering.checkBadWord(input));
        }
    }

    @Nested
    @DisplayName("change() Test")
    class ChangeBadWord {
        @Test
        @DisplayName("Success Case_Change Bad words")
        void testChange_badWord() {
            //given
            String text = "나쁜말";
            //when
            String changedText = badWordFiltering.change(text);
            //then
            assertEquals("***", changedText);
        }
        @Test
        @DisplayName("Success Case_Kind words should not be changed")
        void testChange_kindWord() {
            //given
            String text = "착한말";
            //when
            String changedText = badWordFiltering.change(text);
            //then
            assertEquals(text, changedText);
        }
    }

}