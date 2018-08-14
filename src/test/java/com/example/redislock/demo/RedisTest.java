package com.example.redislock.demo;

import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author: zhaomeinan
 * @Description:
 * @Date: Create in 12:57 2018/8/13
 * @Modificd By:
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableAsync
public class RedisTest {

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  @Resource
  private RedisTemplate redisTemplate;

  @Autowired
  private LockTest service;

  @Autowired
  private RedisDistributedLock redisLock2;

  @Test
  public void test() throws Exception {
    // 保存字符串
    stringRedisTemplate.opsForValue().set("bbbs", "111");
    redisTemplate.opsForValue().setIfAbsent("ssdfsf", "舒肤佳舒肤佳");
    Assert.assertEquals("111", stringRedisTemplate.opsForValue().get("aaa"));
  }

  @Test
  public void test2() throws InterruptedException {
    for (int i = 0; i < 1; i++) {
      //service.seckill(i);
      redisLock2.seckill(i);
    }
  }
}
