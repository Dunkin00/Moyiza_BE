package com.example.moyiza_be.common.utils;

public interface BadWords {
    String[] badWords = { "나쁜말","너정말나쁜아이구나","슈발", "미친", "뻐큐", "썅", "섹스", "ㅅㅂ", "ㅆㅂ",
            "시발", "씨발", "ㅗ", "fuck", "개새끼", "개색기", "씹", "ㅁㅊ", "ㅈㄹ",
            "ㄷㅊ", "꺼져", "닥쳐", "지랄", "개새기" , "좆", "존나", "놈", "년", "련",
            "개새", "스벌", "씨벌", "쓰벌", "병신", "ㅂㅅ", "븅신", "ㅅ1발","개자식",
            "좆까", "좇", "sex", "썅간나"};
    String[] delimiters = { " ", "1", ",", ".", "!", "?", "2", "3", "@", "4", "5",
            "6", "7", "8", "9", "0", "ㅡ", "_", "~", "/", "$"};
    String substituteValue = "*";
}