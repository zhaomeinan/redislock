package com.example.redislock.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author: zhaomeinan
 * @Description: 测试分布式锁
 * @Date: Create in 13:09 2018/8/13
 * @Modificd By:
 */
@Component
public class LockTest {

  @Autowired
  private RedisDistributedLock redisLock;

  @Scheduled(cron = "0 35 17 * * ?")
  public void test() {
    for (int i = 0; i < 50; i++) {
      try {
        redisLock.seckill(i);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
