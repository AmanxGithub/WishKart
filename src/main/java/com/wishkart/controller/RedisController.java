package com.wishkart.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisController {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisController(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/set")
    public String set() {
        redisTemplate.opsForValue().set("test:key", "hello redis");
        return "saved";
    }

    @GetMapping("/get")
    public String get() {
        return redisTemplate.opsForValue().get("test:key");
    }


}
