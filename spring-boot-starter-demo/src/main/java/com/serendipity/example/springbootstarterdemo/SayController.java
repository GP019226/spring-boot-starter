package com.serendipity.example.springbootstarterdemo;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SayController {

    @Autowired
    RedissonClient redissonClient;

    @GetMapping("/say")
    public String say(){
        RBucket bucket = redissonClient.getBucket("key");
        if (bucket.get() == null){
            bucket.set("serendipity.com");
        }
        return bucket.get().toString();
    }
}
